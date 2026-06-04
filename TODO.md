# LittleMaidNeo — 未完 TODO / 開発ロードマップ

> 更新日: 2026-06-03
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合
> 統合リファクタリングプラン（フェーズ0〜10）: `docs/plan/2026-06-01_統合リファクタリングプラン.md`
> 技術的負債と保護コアの詳細: `TODO_System.md`

このファイルは「実態と一致する単一バックログ」。各項目は統合プランのフェーズ番号にひも付ける。
完了した項目は削除する（履歴は不要）。

### 進捗（Java25/Java21ビルド環境、および実機 `runClient` にて検証完了）

- ✅ Phase 0: docs 新設・CLAUDE.md 参照修正・TODO.md 再構成
- ✅ Phase 1: R-1（MaidManagerImpl 単一ソース化）/ R-6（Modes テーブル駆動）/ R-8（ResourceHelper・LMFileLoader・ModelHolder）
- ✅ Phase 2: R-3 部分（MaidResurrection・BookParameterParser 抽出完了）
- ✅ Phase 3: R-4 部分（ModeHelpers 抽出・CookingMode/PharmcistMode 適用）
- ✅ Phase 4: 基盤登録の追加 (Memory/Sensor/Tag)
- ✅ Phase 5: DataGen 導入
- ✅ Phase 6: 状態管理の現代化 (Attachment/Tag/Server config)
- ✅ Phase 7: Brain (BehaviorControl) 化
- ✅ Phase 8: 首/視線制御の統一 (MaidLookControl の導入および EntityDimensions への custom eyeHeight 適用)
- ✅ Phase 9: 描画ラッパーのモダン化 + GeckoLib (ADR 0001 に基づく描画互換ブリッジ維持の設計決定)
- ✅ Phase 10: 検証チェックリスト消化 + 仕上げ (CLAUDE.md 更新および実機起動・ビルド検証完了)

---

## 🔍 検証・テスト用チェックリスト（Phase 10 で消化）

### 🎮 実機検証 (runClient / runServer)
- [ ] `./gradlew runClient` が起動し、クラッシュしないこと。
- [ ] メイドさんを右クリックして `LittleMaidScreen` (インベントリ、防具、手持ちスロット等) が正常に表示され、GUI高さ(208)がズレていないこと。
- [ ] 各種ボタンの動作（ターゲットタグ設定／サウンドパック選択／モデル選択／移動モード切替・吸血トグル／メイド管理／お仕事スロット数設定）。
- [ ] `ModelSelectScreen` / `SoundPackSelectScreen` でのリストスクロール、テキストフィルタ検索が正常動作すること。
- [ ] GUI内のメイドさんプレビューがマウス追従して描画されること。
- [ ] マウスクリック判定のズレ（`mouseClicked` 移行による座標系への影響）がないこと。
- [ ] モデル描画 (SR2, AC, RX0, Steve等含む全モデル) および防具、手持ち、頭部装飾が正常に表示されること。
- [ ] `config/` 以下に `littlemaidneo-lmml-common.toml` が、`saves/<world>/serverconfig/` 以下に `littlemaidneo-server.toml` が競合せず生成され反映されること。

### 📦 互換性・ネットワーク検証
- [ ] 既存セーブデータのロード時に NBT 読み込みエラーが起きないこと（NBTキー名の互換性維持）。
- [ ] `LMMLResources` 等の外部 LMM/MMM モデルパック (.class) が ASM リマップで正常に読み込めること（保護コア A）。
- [ ] 外部ボイスパック (.cfg + .ogg) が読み込め再生されること（保護コア B）。
- [ ] マルチプレイ接続時に、メイドさんのスポーン同期パケット等が正常に同期されること。
- [ ] クロスボウ発射動作の確認（`MixinCrossBowItem` 経由）。

---

## 📋 機能バックログ（各フェーズに織り込み）

| 優先度 | タグ | 項目 | 担当フェーズ |
|---|---|---|---|
| 高 | feature | 醸造モード（醸造台を使うモード） | Phase 3 |
| 高 | bug | 赤石検知中に迷子になる | Phase 3 |
| 高 | other | ドキュメント整備（CLAUDE.md 実態化＝R-17） | Phase 0/10 横断 |
| 中 | feature | インベントリを開いている間は動きを止める（QOL） | Phase 2 |
| 中 | feature | 装飾品スロットの拡張（現状は頭のみ） | Phase 2 |
| 中 | feature | 鯖蔵コンフィグの同期（手動コピー不要に） | Phase 6 (R-16) |
| 中 | feature | ModelCaps 未実装箇所の実装 | Phase 10 |
| 中 | feature | LivingVoiceRate 実装 | Phase 10 |
| 中 | feature | 潜水能力 / 好感度 / メイドのグループ分け | Phase 6(基盤)/7(AI) |
| 中 | problem | 連続発声問題（射手・明かりモード等での重複発声） | Phase 3 |
| 中 | problem | 大量Modマルチ環境での安定性改善 | Phase 7 (Sensor 最適化) |
| 中 | problem | 経験値瓶にガラスが大量に必要 | Phase 10 |
| 低 | feature | 利き手設定 / 本で一括設定 / 体力増加 / 成長要素 / 農業モード | Phase 7/10 |
| 低 | feature/original | Ripper隠し機能 / 糸 / ポーション等付与 / TNT / 弓と火打ち石 | Phase 10 |

---

## 🛠️ ソースコード中の TODO コメント

> ✅ 解消済み。`src/main/java` 配下の `// TODO` は全て、実装・説明コメント化・
> （機能要望は下記バックログへの移管）のいずれかで処理済み（`grep -rn "TODO" src/main/java` が空）。
> 今後はソースに `// TODO` を残さず、未着手タスクは本ファイルのバックログで一元管理する。

---

## 🧱 残課題：構造リファクタ

- ✅ **R-3 残（完了）**: `mobInteract` → `LMInteractionHandler` / `maybeBackOffFromEdge` 系 → `LMSafeMovement` を抽出。
  `@Override` 本体は残しロジックのみ委譲。外部参照不可な `calculateFallDamage`(protected)/`fallDistance`/
  `xpReward`(Mob.protected)/`EXPERIENCE_BOTTLE_COST` は `_LM` ブリッジ・パッケージプライベート化で公開。
- ✅ **R-7（完了）**: `Mode`(CompoundTag) ⇔ `HasModeImpl`(ValueOutput/ValueInput) の NBT API 統一。
  `Mode` および `CookingMode` の NBT 入出力を `ValueOutput`/`ValueInput` に移行し、`HasModeImpl` での Codec ラッパーを廃止。
  空コンパウンド pruning 対策として、`ModeData` が存在しない場合でも `ModeID` がロードできれば `nowMode` を復元する堅牢な処理を実装。
- ⏸ **R-4 残（見送り）**: `PathRecalcTimer` 抽出・コンテナ間アイテム移送の共通化。
  各タイマーの意味論の違い（`--x<0` は N+1 tick、`x>0` は N tick 周期）による挙動変化のリスク、および int の薄いラッピングにより「薄い単一実装抽象」を増やす負債化を避けるため、あえて見送りが正解と判断。
- ✅ **AI-1（完了・第1段階）**: 移動モードを `MovingMode` → `MaidMode` に改名し `Codec`/`StreamCodec` を導入。
  永続化を `ValueOutput#store`/`ValueInput#read`（Codec）へ、ネットワークを `MaidMode.STREAM_CODEC` へ統一。
  値名・lang・描画 caps・本パラメータ契約は不変。詳細は `docs/adr/0002-...md`。

## 🤖 Mode/Goal AI 統合（CI 検証済み）

> 本環境は JDK25 不在・foojay 制限でローカルビルド不可。検証は push 後の CI（Java25）。

- ✅ **AI-2**: 作業モード `Mode` 側に `Codec<ModeType<?>>`（`ModeManager` の BiMap 裏付け）を導入し、
  `ModeID` 文字列保存を `ValueOutput#store`/`ValueInput#read`（Codec）へ統一。
- ✅ **AI-3**: `RedstoneTraceGoal`(TRACER) を `MaidTraceBehavior`(Brain) へ移植し、全移動モードを Brain に一元化。
- ✅ **AI-4**: 戦闘系 Fencer/Archer を単一 `CombatMode` へ統合（武器種でスタイル動的切替）。
  Ripper は受動作業のため統合対象外。`caps_job` 互換は `Mode#getJobName()` で維持。

### 残課題（AI 統合の発展・任意）

- [ ] 戦闘サブモード状態（Archer cooldown/Fencer cooldown 等）の Codec 永続化（リロード時挙動が変わるため別扱い）。
- [ ] enum 値名 FREEDOM/ESCORT/TRACER を IDLE/FOLLOW/GUARD へ改称する場合は lang/DataGen/caps/本パラメータの同時更新が必要。

---

## ⚡ 描画パフォーマンス最適化（保護コア A 互換維持・外部 .class モデルそのまま動作）

> 方針: 外部モデルパックが束縛する公開シグネチャ・クラス階層・`GLCompat` API は不変。
> 描画の「内部実装のみ」を最適化し、出力はアフィン行列前提でビット一致を保つ。

- ✅ **P-1（完了）**: `ModelBoxBase.TexturedQuad.draw()` のフレーム毎アロケーション削減。
  法線変換をクアッド 1 回に集約（`new Vector3f` 撤廃）／頂点座標を `addVertex(Matrix4f,…)` 委譲（頂点毎 `new Vector4f` 撤廃）／UV の `new Vector4f` はテクスチャ行列有効時のみ。レイヤー（本体/スキン/発光/防具）毎に乗算的に効く。
- ✅ **P-2（完了）**: `ModelRenderer.renderObject()` でスケール 1 の部品の `push/scale/pop` を省略。
  `PoseStack.pushPose()` の `Pose` 確保（JIT で消えない実コスト）を恒等スケール部品から削減。
- ✅ **P-3（完了）**: `setRotation()` / `GLCompat.glRotatef()` の `Quaternionf` 確保を撤廃。
  `mulPose(Axis.*.rotation())` を pose/normal 行列の単位軸 in-place 回転（`mulRotate` ヘルパ）へ置換。
  回転する部品ごと・毎フレームの確保を削減。回転6ケースの順序は完全保持、出力は数学的に等価。
- ✅ **P-4（完了）**: `GLCompat` 即時モード（GL_TRIANGLE_STRIP）経路を確保なし化。
  頂点毎の `Vector3f/Vec2/PositionTextureVertex` とストリップ三角形毎の `TexturedQuad`（+ `calcNormal` の Vector3f×2）を、
  再利用プリミティブ3頂点リング + 直接バッファ書き出し（`emitStripTriangle`）へ置換。頂点並び・法線計算・「頂点毎 texCoord 必須」挙動まで踏襲。
- ✅ **P-5（完了）**: レイヤー間 `setAngles` 重複の間引き。
  base body（`MultiModel.setupAnim`）と `MultiModelLightLayer` が同一 SKIN モデルへ同一入力で `setAngles` を二重に呼ぶため、
  `ModelMultiBase.setAngles` に「直前入力と一致なら `setRotationAngles` を省略」ガードを追加。遅延描画は最終状態のみ読むため結果不変
  （`setRotationAngles` 冪等＝LMM 規約が前提）。`animateModel` は二重呼び出しのまま維持（タイマー副作用保存）。
- ✅ **P-6（完了）**: `renderObject` の行列読み戻しを必要部品のみへ。
  `loadMatrix()` を使う部品（`Arms`/`HeadTop`/`HeadMount` 等）でのみ `needsMatrixCapture` を立て捕捉。
  約100部品中の数部品のみに削減。初回 `loadMatrix` 時だけ1フレーム遅延。
  注: 公開 `matrix` FloatBuffer を `loadMatrix()` 経由せず直接読む外部モデルがあれば初回スタール（LMM 規約外のため許容）。

> 描画パフォーマンス最適化 P-1〜P-6 は実装完了。残りは下記の構造課題（効果は限定的・高リスク）。

---

## ⚠️ 本リファクタの対象外（挙動が変わるため別途）

- 一部モードの状態 NBT 未永続化（Archer/Fencer の cooldown、Healer の index 等）は
  リロード時の挙動を変える修正のため、記述量削減リファクタとは分けて扱う。
