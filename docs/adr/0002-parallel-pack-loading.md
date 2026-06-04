# ADR 0002: 外部パックローディングの並列化（パース並列 / 登録直列）

## ステータス

承認済み (Accepted) — 2026-06-04

## コンテキスト

外部パック（モデル `.class`・テクスチャ `.png`・設定 `.cfg`・ボイス `.ogg`）のロードは、起動時に
`FMLCommonSetupEvent` → `enqueueWork` 内で `LMFileLoader.load()` が **完全同期・単一スレッド** で実行していた。
`LMMLResources/` を `Files.walk` で走査し、各ファイルを 1 件ずつ各ローダへ分配する逐次処理である。

支配的コストは **モデルの読み込み**（`Class.forName` によるクラスロード＋ASM リマップ＋
skin/inner/outer の 3×`newInstance`＝ジオメトリ構築）で、パック数・モデル数に比例して起動が伸びる。

重要な事実として、**このロード段階では GL リソース生成・テクスチャ GL アップロードは一切ない**
（実デコード/アップロードは後段のリソースリロード・遅延ロードで行う）。したがってパースと構築は
メインスレッド制約がなく、ワーカースレッドへオフロードできる。

## 意思決定

ロードを **3 フェーズ** に再構成する。並行プリミティブは **virtual thread**
（`Executors.newVirtualThreadPerTaskExecutor`、Java 21+ 安定。`StructuredTaskScope` は Java 25 でも
プレビュー（JEP 505・`--enable-preview` 必要）のため不採用）。

1. **収集（単一スレッド・安定順）**: 全フォルダを `Files.walk` で走査しトップレベルファイルを列挙。
2. **解析（並列）**: 各ファイルを仮想スレッドで並列にパース・構築し、登録アクション (`Runnable`) を返す。
   共有 Manager には触れない。アーカイブは **1 ZIP = 1 タスク**（`ZipInputStream` の逐次制約をタスク内に閉じ込め、
   ZIP 同士は並列）。
3. **登録（単一スレッド・決定的）**: 収集順（=従来の走査順）に登録アクションを実行。HashMap を保護し、
   同一キーの「後勝ち」順序を従来と完全一致させる。

### 設計の要点

- **`LMLoader` の契約拡張**: `parse(...) -> @Nullable Runnable`（重い処理＋登録アクション生成）を主とし、
  `load(...)` は `parse` を即時実行する逐次互換のデフォルトメソッドに。4 ローダ
  （Config/Texture/MultiModel/Sound）が `parse` を実装。ストリームを読むのは `LMConfigLoader` のみで、
  読み取りは `parse` 内（ストリーム生存中）で完了させる。
- **`LMModelManager` の分割**: 重い構築 `buildHolder()`（ワーカーで実行可・共有状態に触れない）と
  軽い登録 `putModel()`（登録フェーズで実行）に分離。`addModel()` はその合成として温存。
- **クラスロード並列化**: `MultiModelClassLoader` に `registerAsParallelCapable()` を追加し、
  クラス名ごとのロックで複数モデルクラスの並列ロードを許可。
- **ネスト並列の回避**: 外側でクラスを並列ロードするため、`MultiModelClassTransformer.transform` 内の
  細粒度 `parallelStream`（fields/methods/localVariables）を `stream`（直列）に戻し、common ForkJoinPool の
  競合を排除。**リマップ規則・GL11→GLCompat 置換は完全不変**。
- **堅牢性**: 旧 `applyLoaders` の `forEach` は例外無保護で 1 失敗が後続全停止だった。`collectParses` で
  ローダ単位に try/catch を入れ、1 パックの破損が他を止めないようにした。

### 決定性

登録順 = 収集順（`Files.walk` 順、アーカイブ内は ZIP エントリ順）に固定したため、
同名モデル・同 index テクスチャ・同名設定・同 location ボイスの「後勝ち」結果が **従来と完全一致**。
HashMap は ConcurrentHashMap 化せず単一スレッド登録で安全・決定的。

## 結果

- **保護コア A 非侵襲**: 変更は `MultiModelClassLoader` の static ブロック 1 行と
  `MultiModelClassTransformer` の `parallelStream→stream`（意味同値）のみ。リマップ表・`findClass`/`loadClass`
  の署名と継承・`maidmodel`・`GLCompat` は不変。
- **保護コア B 非侵襲**: `LMSoundLoader`/`LMSoundManager` の `.cfg+.ogg` 形式・`LMSounds`・命名規則・
  探索パス・キー生成・ネットワーク同期パケットは不変。登録を遅延しただけ。
- **高速化**: 最重コスト（モデルのクラスロード＋ASM＋3×newInstance）と ZIP 展開・I/O が並列化され、
  起動時間の短縮を見込む（CPU コア数に応じてスケール）。

### 既知の制約・検証事項

- **デフォルトパッケージのモデル**（走査ルート直下の `.class`）を複数スレッドが同名でロードすると、
  `loadClass` の override が `getClassLoadingLock` 外で `findClass` する経路により稀に `LinkageError` の
  可能性。その場合も `collectParses` の try/catch で当該モデルのみスキップ（クラッシュしない）。実運用では
  モデル名はフォルダ構造由来のドット付きが通常で、該当は稀。
- ビルド検証は Java 25 環境が必要。`./gradlew compileJava` と `runClient` で、起動ログの
  `Loading … ms` 比較、全モデル/テクスチャ/ボイスの登録数一致、同名衝突キーの勝者が毎回同一であることを確認する。
