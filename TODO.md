# LittleMaidNeo — 実装状況 & TODO

> 更新日: 2026-05-23
> ベース: `LittleMaidModelLoader-Architectury` + `LittleMaidReBirth-Architectury`（旧 `net.sistr.*` / Architectury）
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合

---

## 現状サマリ

2MODの NeoForge への統合移植は**ひと通り完了**している。

- Javaソース **219ファイル**（`work.nemonet.littlemaidneo` 配下、28パッケージ）。
- 旧Architectury構成からの主な対応関係:
  - Architectury `*Packet` → NeoForge `CustomPacketPayload`（`network/` に C2S/S2C 全実装）
  - `Networking` → `NetworkHandler`
  - `LMMLMod` / `LMRBMod` → `LittleMaidNeo` / `LittleMaidNeoClient` に統合
  - プラットフォーム別 `*Impl`（`EPEntityUtilImpl` 等）→ NeoForge専用本体クラスに統合
- `build.gradle`: Java 25 ツールチェーン + ASM / asm-tree / commons-io 定義済み。
- `LittleMaidNeo#initModelLoader()`: AC / RX0 を含む全モデル登録済み。
- リソース: `assets`（lang ja/en, GUI/skin テクスチャ群）、`data`（advancement / loot / recipe / tag /
  biome_modifier）、`littlemaidneo.mixins.json` 配置済み。プレースホルダー（"Example…"）は残っていない。

---

## 🔍 要検証（実機・ビルド未確認）

- [ ] `./gradlew build` の成功確認（CI 環境）。※ローカルsandboxは JDK25 不在 + NeoForge maven が
      ネットワーク非許可のためフルビルド不可。CI（temurin JDK25）での確認が必要。
- [ ] クライアント起動でのモデル選択GUI / メイド描画 / サウンドパック動作確認。
- [ ] メイドのスポーン同期（NeoForge標準のスポーンデータで足りるか。旧 `SpawnLittleMaidPacket` 相当が
      不要かを実機確認）。

---

## 📋 機能バックログ（原作 LittleMaidReBirth 由来 / 新規開発）

移植とは別に、原作で未実装だった機能。優先度・タグは旧 ReBirth TODO 準拠。

| 優先度 | タグ | 項目 |
|---|---|---|
| 高 | feature | 醸造モード（醸造台を使うモード） |
| 高 | bug | 赤石検知中に迷子になる |
| 高 | other | ドキュメント整備 |
| 中 | feature | インベントリを開いている間は動きを止める（QOL） |
| 中 | feature | 装飾品スロットの拡張（現状は頭のみ） |
| 中 | feature | 鯖蔵コンフィグの同期（手動コピー不要に） |
| 中 | feature | ModelCaps 未実装箇所の実装 |
| 中 | feature | LivingVoiceRate 実装 |
| 中 | feature | 潜水能力 / 好感度 / メイドのグループ分け |
| 中 | problem | 連続発声問題（射手・明かりモード等での重複発声） |
| 中 | problem | 大量Modマルチ環境での安定性改善 |
| 中 | problem | 経験値瓶にガラスが大量に必要 |
| 低 | feature | 利き手設定 / 本で一括設定 / 体力増加 / 成長要素 / 農業モード |
| 低 | feature/original | Ripper隠し機能 / 糸 / ポーション等付与 / TNT / 弓と火打ち石 |

---

## 🛠 Java 8+ 近代化（完了）

下記は実装済み（このコミット群で対応）。

- [x] **record化** — `util/Tuple`, `resource/util/TexturePair`, `client/screen/component/RangeChecker`
      （呼び出し側のアクセサ追随済み）。
- [x] **instanceof パターンマッチング** — `network/NetworkHandler`（2箇所）, `block/SalaryBoxBlock`,
      `client/renderer/LMHeadFeatureRenderer`（2箇所）, `resource/classloader/MultiModelClassTransformer`。
- [x] **switch式 / arrow-form switch化** — `maidmodel/ModelPlate`, `ModelMultiMMMBase`,
      `ModelMultiBase`, `ModelRenderer`, `ModelLittleMaid_RX0`, `EntityCaps`。

### 対象外（意図的に変更せず）
- `resource/util/ArmorPart`: コンストラクタが引数→フィールド変換を行い Builder も持つため record 化しない。
- **並列ストリーム（parallelStream）は導入しない**: tick/レンダリングは Minecraft メインスレッド前提で、
  ForkJoinPool 競合・スレッド安全性破壊のリスクが高い。起動時リソース読み込み等のオフスレッド処理に限り
  将来候補だが、プロファイルで効果を確認できるまで逐次のまま据え置く。
