# LittleMaidNeo — 総合開発状況 ＆ TODO / 開発ロードマップ

> 更新日: 2026-05-29
> ベース: `LittleMaidModelLoader-Architectury` + `LittleMaidReBirth-Architectury`
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合

---

## 📌 開発状況サマリー

2つのレガシーMod系譜（LML/MMM系 と ReBirth系）の統合、および MC 26.1.2 / NeoForge 26.1.2.64-beta / Java 25 への移行作業における API 追従コンパイル修正はすべて完了しています。
GUI レンダリング API 移行も完了し、コンパイルを妨げていた `Registration` カスケードおよび GUI の残作業（`LittleMaidScreen` の `imageHeight` / 可視性）も修正済みです。

現在の開発フェーズは、**「NeoForge クリーンアップ・クラス統合（Step 6〜8）」** および **「ビルド・実機動作確認」** です。

*   Javaソースファイル数: **219ファイル** ([work.nemonet.littlemaidneo](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/) 配下)
*   旧Architecturyからの移行構造:
    *   `*Packet` → NeoForge `CustomPacketPayload` ([network/](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/network/) 配下に C2S/S2C を集約)
    *   `Networking` → [NetworkHandler.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/network/NetworkHandler.java)
    *   `LMMLMod` / `LMRBMod` → [LittleMaidNeo.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/LittleMaidNeo.java) / [LittleMaidNeoClient.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/LittleMaidNeoClient.java)
    *   プラットフォーム別 `*Impl` → NeoForge専用本体クラスへ一本化

---

## 1. 🧹 NeoForge クリーンアップ・クラス統合（進行中）

レガシー系譜の融合に伴い発生したデッドコードの削除、および過剰な抽象化の整理を段階的に実施しています。

### 統合/クリーンアップ進捗
*   **Step 1: デッドコード9クラス削除** — ✅ **完了** (`f5a88b8`)
    *   未使用のユーティリティ（`Pos2d`, `SightUtil`, `ColorConverter`）
    *   未登録の Mixin アクセサ 4種 (`ItemEntityAccessor`, `PersistentProjectileEntityAccessor`, `MeleeAttackGoalAccessor`, `ProjectileEntityAccessor`)
    *   完全なデッドコードGUI（`ListGUI`, `ScrollBar`）
*   **Step 2: GUIクラスの統合** — ✅ **完了** (`e17fd3b`)
    *   `MutableListGUI` → [ListGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ListGUI.java) に統合
    *   `MutableScrollBar` → [ScrollBar.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ScrollBar.java) に統合
*   **Step 3: 薄いインターフェースの整理** — ✅ **一部完了** (`e47c9b7`)
    *   `AimingPoseable` を削除し、[LittleMaidEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java) へ直接記述。
    *   ※ `HasMovingMode`, `SalaryBoxPosListener`, `SoundPlayable` は多態性契約で必要なため保持。
*   **Step 4: Config二重登録の衝突解消** — ✅ **完了** (`e47c9b7`)
    *   [LMMLConfig.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/config/LMMLConfig.java) の config ファイル名を独自のもの（`littlemaidneo-lmml-common.toml`）に変更し、[LMRBConfig.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/config/LMRBConfig.java) との競合を回避。
*   **Step 5: Interface+Impl 統合** — ❌ **中止（現状維持）**
    *   `HasMode`, `TargetTagManager`, `MaidManager` は Mixin で vanilla の `Player` / `ServerPlayer` に注入されるクロスカッティングな多態性契約のため、統合不可と判断。
*   **Step 6: リソースサブシステムの統合** — ✅ **完了**
    *   `ConfigHolder` を record 化（`packName()`, `parentName()`, `fileName()`, `settings()` アクセサで旧 getter 名を置き換え）。
    *   呼び出し箇所（`SoundPackSelectScreen`, `LMConfigManager`, `LittleMaidEntity`）を record アクセサ名に追従修正。
*   **Step 7: 単一サブクラス Goal 基底の統合** — ✅ **完了（3ペア統合）**
    *   `HealMyselfGoal<LittleMaidEntity>` → `LMHealMyselfGoal` に吸収、`HealMyselfGoal.java` 削除。heal() でバニラSEとメイドさんボイスを統合。
    *   `TeleportTameOwnerGoal<LittleMaidEntity>` → `LMTeleportTameOwnerGoal` に吸収。MovingMode.ESCORT チェックを canUse/canContinueToUse 冒頭に統合。`TeleportTameOwnerGoal.java` 削除。
    *   `FollowTameOwnerGoal<T>` → `HasMMFollowTameOwnerGoal<T>` に吸収。ジェネリクス境界 `T extends TamableAnimal & HasMovingMode` を保持。MovingMode.ESCORT チェックを canUse 冒頭に統合。`FollowTameOwnerGoal.java` 削除。
*   **Step 8: MM* 描画ラッパー層の簡素化** — ❌ **中止（外部互換性保護のため現状維持）**
    *   `MMMatrixStack` / `MMVertexConsumer` は `maidmodel/ModelMultiBase.java` のシグネチャで直接使われており、外部モデルパック（.class）が override するメソッドの引数型として機能している。変更すると外部互換性が壊れるため統合不可と判断。

### 1-A. 不要クラス調査結果 (Obsolete Classes)

#### 確実に削除可能なデッドコード（0参照を検証済み）→ **Step 1 で削除済み**

| クラス / パッケージ | 種別 | 検証結果 | 削除理由 |
|---|---|---|---|
| `util/Pos2d` | ユーティリティ | 0参照 | 2D座標の薄いラッパー。どこからも未使用。必要なら `Vec2`/`BlockPos` 等で代替可。 |
| `util/SightUtil` (269行) | ユーティリティ | 0参照 | `rotate()` が `throw new AssertionError()` の**未完成実装**。統合されないまま放置されたデッドコード。 |
| `resource/util/ColorConverter` | ユーティリティ | 0参照 | 染料→`TextureColors` 変換。呼び出し元なし。 |
| `mixin/ItemEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ。設定に登録されておらずビルドにも乗らない。 |
| `mixin/PersistentProjectileEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ（旧 Yarn 名 `PersistentProjectileEntity`＝NeoForge の `AbstractArrow`）。Fabric時代の残骸。 |
| `mixin/MeleeAttackGoalAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ。未使用。 |
| `mixin/ProjectileEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ（追加検証で発見）。未使用。 |
| `client/screen/component/ListGUI` (200行) | GUI | 0参照 | `new ListGUI` / `extends ListGUI` ともに0件。`MutableListGUI` が上位互換。完全なデッドコード。 |
| `client/screen/component/ScrollBar` (119行) | GUI | 0参照 | `MutableScrollBar` のみが実際に使われ、こちらは未インスタンス化。`MutableScrollBar` が上位互換（防御的 null チェック・`elemSize` 可変）。 |

#### 保持すべきクラス（削除・統合対象外）
*   **外部モデルパック互換インフラ (load-bearing)**:
    *   [MultiModelClassLoader.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/resource/classloader/MultiModelClassLoader.java) / [MultiModelClassTransformer.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/resource/classloader/MultiModelClassTransformer.java): 外部の旧 LMM/MMM モデルパックを ASM 経由でリマップ読み込みする互換インフラのため削除厳禁。
    *   `maidmodel/` パッケージ全体 / `maidmodel/compat/GLCompat`: 上記の外部互換機能のための GL11 シムレイヤーなどを含んでおり load-bearing。
    *   [EntityLittleMaid.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/EntityLittleMaid.java): Transformerのリマップ先スタブとして必要なため残す。

### 1-B. 統合推奨クラス調査結果 (Classes to Merge)

#### デッド上位互換ペアの一本化 → **Step 2 で実施済み**
*   `MutableListGUI` → **`ListGUI` にリネーム統合**。4階層ピラミッド（`ListGUI`→`MutableListGUI`→`ScrollableListGUI`→`FilterableListGUI`）を3階層へ。実利用は `FilterableListGUI`（4スクリーン全て）と `ScrollableListGUI` のみ。
*   `MutableScrollBar` → **`ScrollBar` にリネーム統合**。

#### 価値の低い単一実装インターフェースの畳み込み
*   ✅ `entity/util/AimingPoseable`（11行）→ **除去済み**。多態消費者ゼロ（`LittleMaidEntity` のみ実装、`AbstractArcherMode.mob` は `LittleMaidEntity` 直接型）。2メソッドを `LittleMaidEntity` に直接保持。
*   ❌ `entity/util/HasMovingMode` → **保持**。`HasMMFollowTameOwnerGoal<T extends TamableAnimal & HasMovingMode>` のジェネリック境界として実利用。
*   ❌ `entity/util/SalaryBoxPosListener` → **保持**。`SalaryBoxBlockEntity` が `instanceof SalaryBoxPosListener` + キャストで通知する observer 境界（block→entity の脱結合）。
*   ❌ `entity/compound/SoundPlayable` → **保持**。`LittleMaidEntity` と `MultiModelEntity` の2実装があり、`NetworkHandler`/`MoveToDropItemGoal` 等で型として利用される多態契約。

#### Interface + Impl ペアの統合 → **中止（統合不可）**
Mixin で vanilla クラスに注入されるクロスカッティング契約であり、impl はエンティティが内部 compose する委譲先、interface は vanilla 側にもスタプルされるため、統合すると mixin 注入実装が壊れる。
*   ❌ `entity/mode/HasMode` → 保持。`ModeWrapperGoal<T extends LivingEntity & HasMode>` の境界。
*   ❌ `entity/targeting/TargetTagManager` → 保持。`MixinPlayerEntity implements TargetTagManager`（vanilla Player に注入）。
*   ❌ `entity/util/MaidManager` → 保持。`MixinServerPlayerEntity implements MaidManager`（vanilla ServerPlayer に注入）。

#### 単一サブクラス Goal 基底クラスの統合（積極方針・Step 7 予定）
非LM基底クラスはいずれも対応する唯一のLMサブクラスからのみ参照される（検証済み）。

| 基底（唯一の利用者） | サブクラス | 方針 |
|---|---|---|
| `FollowTameOwnerGoal` | `HasMMFollowTameOwnerGoal` | 統合 or 基底のジェネリクスを残しつつ畳み込み |
| `TeleportTameOwnerGoal` | `LMTeleportTameOwnerGoal` | 同上 |
| `HealMyselfGoal` | `LMHealMyselfGoal` | 同上 |
| `MoveToDropItemGoal`(abstract) | `LMMoveToDropItemGoal` | abstract を具象へ畳み込み |
| `StoreItemToContainerGoal`(abstract) | `LMStoreItemToContainerGoal` | 同上 |
| `CollectItemFromContainerGoal`(abstract) | `LMCollectSalaryFromContainerGoal` | 同上 |
| `StareAtHeldItemGoal` → `TameableStareAtHeldItemGoal` → `LittleMaidEntity.LMStareAtHeldItemGoal`(inner) | 3段 → 1〜2段へ縮約 |

> **注**: クラス数削減と将来の汎用再利用性／可読性のトレードオフがあるため、まず1ペア（例: `HealMyselfGoal` + `LMHealMyselfGoal`）を試行して影響を確認してから横展開する。

#### resource サブシステムの統合（Step 6 予定）
*   `resource/manager/`（`LMModelManager`/`LMTextureManager`/`LMConfigManager`）＋ `client/resource/manager/LMSoundManager` を共通の登録API（共通基底 or 汎用 `ResourceRegistry`）へ統合。
*   `resource/loader/`（`LMConfigLoader`/`LMTextureLoader`/`LMMultiModelLoader`）＋ `client/resource/loader/LMSoundLoader` のローダー共通基底を抽出し、各ローダーを薄いアダプタ化。
*   `resource/holder/ConfigHolder` の record 化、`TextureHolder` の検索ロジック簡素化。

#### MM* 描画ラッパー層の簡素化（Step 8 予定・要事前検証）
*   `MMVertexConsumer`, `MMMatrixStack`, `MMPose`, `MMRenderContext` を整理し、`IMultiModel` のシグネチャを vanilla 型（`VertexConsumer`, `PoseStack`）に寄せて層を削減。
*   `MultiModel` と `LMMultiModel` を統合。
*   ※ 外部モデルパック互換性を破壊しないことを事前に確認した上で着手する。

---

## 2. 🎨 GUI / レンダリング API 移行（完了）

Minecraft 26.1.2 に追従する形で、GUI 描画を行っている全13ファイルの移行を完了しました。
`grep "GuiGraphics\b"`（旧型）= **0 件**。すべて `GuiGraphicsExtractor`（新型）に書き換わっています。

### 移行完了したファイル
*   **画面 (screen/)**:
    *   [LittleMaidScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java) (可視性 public 化および 5引数 super コンストラクタ修正完了)
    *   [TargetTagScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/TargetTagScreen.java)
    *   [SoundPackSelectScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/SoundPackSelectScreen.java)
    *   [ModelSelectScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/ModelSelectScreen.java)
    *   [MaidManagerScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/MaidManagerScreen.java)
*   **コンポーネント (screen/component/)**:
    *   [ScrollableListGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ScrollableListGUI.java) / [FilterableListGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/FilterableListGUI.java) / [ListGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ListGUI.java)
    *   [TextInputGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/TextInputGUI.java) / [ArmorModelGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ArmorModelGUI.java) / [MultiModelGUI.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/MultiModelGUI.java)
    *   [ScrollBar.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/ScrollBar.java) / [MultiModelGUIUtil.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/component/MultiModelGUIUtil.java)

### 適用済み GUI API 対応表

| 旧 API（MC 1.20.1 / Architectury） | 新 API（MC 26.1.2） |
| --- | --- |
| `GuiGraphics`（型） | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `render(GuiGraphics, int, int, float)` | `extractRenderState(GuiGraphicsExtractor, int, int, float)` |
| `renderBg(GuiGraphics, float, int, int)` | `extractContents(GuiGraphicsExtractor, int, int, float)` |
| `renderLabels(GuiGraphics, int, int)` | `extractLabels(GuiGraphicsExtractor, int, int)` |
| `renderWidget(GuiGraphics, int, int, float)` | `extractWidgetRenderState(GuiGraphicsExtractor, int, int, float)` |
| `blit(tex, x, y, u, v, w, h)`（int u/v） | `blit(tex, x, y, w, h, (float)u, (float)v, (float)w, (float)h)` |
| `drawString(font, text, x, y, color, shadow)` | `text(font, text, x, y, color, shadow)` |
| `renderItem(stack, x, y)` | `item(stack, x, y)` |
| `InventoryScreen.renderEntityInInventoryFollowsMouse(...)` | `InventoryScreen.extractEntityInInventoryFollowsMouse(...)` |
| `mouseClicked(double, double, int)` | `mouseClicked(MouseButtonEvent, boolean)` |

---

## 3. ✅ MC 26.1.2 API 追従状況（完了）

*   **エンティティセーブ API**: `CompoundTag` → `ValueOutput` / `ValueInput` (ブリッジ用 `TagValueOutput` / `TagValueInput` を使用)
    *   対象: `LittleMaidEntity`, `MultiModelEntity`, `MaidSoulEntity`, `SalaryBoxBlockEntity`, Mixin (`MixinPlayerEntity`, `MixinServerPlayerEntity`), 各種マネージャー実装など。
*   **エンティティメソッド**: `customServerAiStep(ServerLevel)`, `doHurtTarget(ServerLevel, Entity)`, `hurtServer(ServerLevel, DamageSource, float)`, `killedEntity(ServerLevel, LivingEntity, DamageSource)`, `getBaseExperienceReward(ServerLevel)`, `dropEquipment(ServerLevel)`, `spawnAtLocation(ServerLevel, ItemStack)` への追従。
*   **他 API 変更**: ネットワーク周り、属性定義、キーマッピング、スロット列挙などの NeoForge 標準 API への完全な移行。
*   **`Registration` カスケード解消**: `ModRegistration` への置換・整理を完了。
*   **その他コンパイル修正**: `EntityType#create` 引数追加対応など。

---

## 4. 🔍 検証・テスト用チェックリスト

ビルドおよび実機での検証時、特に以下の項目を確認してください。

### ⚙️ ビルド検証
- [x] `./gradlew build` がエラーなしで成功すること。

### 🎮 実機検証 (runClient / runServer)
- [ ] `./gradlew runClient` が起動し、クラッシュしないこと。
- [ ] メイドさんを右クリックして `LittleMaidScreen` (インベントリ、防具、手持ちスロット等) が正常に表示され、GUI高さ(208)がズレていないこと。
- [ ] 各種ボタンの動作:
    - [ ] ターゲットタグ設定ボタン
    - [ ] サウンドパック選択ボタン
    - [ ] モデル選択ボタン
    - [ ] 移動モード切替 / 吸血トグルの切替
    - [ ] メイド管理ボタン
    - [ ] お仕事スロット数設定
- [ ] `ModelSelectScreen` / `SoundPackSelectScreen` でのリストスクロール、テキストフィルタ検索が正常動作すること。
- [ ] GUI内のメイドさんプレビューがマウス追従して描画されること。
- [ ] マウスクリック判定のズレ（`mouseClicked` 移行による座標系への影響）がないこと。
- [ ] モデル描画 (SR2, AC, RX0, Steve等含む全モデル) および防具、手持ち、頭部装飾が正常に表示されること.
- [ ] `config/` フォルダ以下に `littlemaidneo-common.toml` および `littlemaidneo-lmml-common.toml` が競合せず生成され、各設定項目が反映されること。

### 📦 互換性・ネットワーク検証
- [ ] 既存セーブデータのロード時に NBT 読み込みエラーが起きないこと（NBTキー名の互換性維持）。
- [ ] `LMMLResources` 等に配置した外部 LMM/MMM モデルパック (.class 形式) が ASM リマップにより正常に読み込めること。
- [ ] マルチプレイ接続時に、メイドさんのスポーン同期パケット等が正常に動作し同期されること。
- [ ] `mixins.json` 未登録の `CrossbowItemInvoker` が `LittleMaidEntity` から参照されている件について、クロスボウ発射動作に不具合がないか。必要があれば Mixin への追加を行う。

---

## 📋 機能バックログ (原作未実装・新規開発要素)

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

## 🛠️ ソースコード中のTODO

ソースコード内に残されている `TODO` コメントのリストです。

### 💻 クライアント / 画面関連
- [LittleMaidScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java)
  - [ ] [L29](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java#L29): モード名表示/移動状態をアイコンで表記する
  - [ ] [L30](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java#L30): ストライキ時の表示改善

### 🧠 目標 / AI (Goal)
- [LMStoreItemToContainerGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java)
  - [ ] [L52](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java#L52): チェストに仕舞うときの演出を強化する
  - [ ] [L53](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java#L53): チェストに仕舞わない条件を追加する
- [LMTargetGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMTargetGoal.java)
  - [ ] [L37](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMTargetGoal.java#L37): コンフィグ化
- [RedstoneTraceGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java)
  - [ ] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L21): 180度ターン時に首がグリッとなるのがこわいので挙動を修正
  - [ ] [L22](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L22): この状態では自由行動の起点が最後に検知した赤石動力付近に再設定されます。
  - [ ] [L23](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L23): 処理のリファクタリング
- [WaitWhenOpenGUIGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/WaitWhenOpenGUIGoal.java)
  - [ ] [L11](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/WaitWhenOpenGUIGoal.java#L11): 実装する

### 👤 メイド本体 (Entity)
- [LittleMaidEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java)
  - [ ] [L135](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L135): 声タイミング調整
  - [ ] [L136](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L136): ドロップアイテム
  - [ ] [L137](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L137): 契約期間の残りと砂糖をあげた時の音符の色を対応させる。
  - [ ] [L138](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L138): 雪バイオームで雪合戦させる、日が暮れると終わるように
  - [ ] [L139](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L139): モードトリガーアイテム指定
  - [ ] [L140](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L140): 署名済みではない書き込み可能な本にパラメータを記述して、メイドさんに右クリックで使用すると値が反映されるように
  - [ ] [L141](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L141): メイドさんも金リンゴや牛乳を飲めるように
  - [ ] [L142](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L142): つまみ食い
  - [ ] [L143](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L143): ダメージ/水没待機解除 実装済みだっけ？
  - [ ] [L144](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L144): トランザム機能追加
  - [ ] [L145](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L145): 経験値追加
  - [ ] [L146](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L146): 座ったメイドでも追従時に立つようにする
  - [ ] [L147](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L147): スト時砂糖ドカ食い機能
  - [ ] [L148](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L148): GUIを開いている時に動きを止める
  - [ ] [L149](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L149): リスポーン機能
  - [ ] [L150](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L150): 死亡メッセ追加
  - [ ] [L151](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L151): はしごを使えるように
  - [ ] [L152](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L152): おさわり厳禁：他人のメイドに触ると殴られる
  - [ ] [L153](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L153): 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
  - [ ] [L180](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L180): enumにまとめる
  - [ ] [L244](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L244): クラス化検討
  - [ ] [L259](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L259): クライアント側のこの値は信用ならない
  - [ ] [L298](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L298): メイドさんに付与する属性の再考
  - [ ] [L310](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L310): コンフィグでスポーン条件を設定可能にする
  - [ ] [L382](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L382): パーティクル演出の強化
  - [ ] [L639](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L639): 頭の装飾品をチェストに仕舞わないようにする
  - [ ] [L889](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L889): IdFactorが確実にセットされたタイミングで実行されるようにする
  - [ ] [L1169](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1169): スポーン条件をコンフィグで設定可能にする
  - [ ] [L1190](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1190): マウント系の位置を調整
  - [ ] [L1239](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1239): ボイス周りの調整、コンフィグ化
  - [ ] [L1312](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1312): 強制再生メソッドを生やす
  - [ ] [L1357](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1357): 処理の改善
  - [ ] [L1398](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1398): 処理の改善
  - [ ] [L1507](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1507): try/catchを挟む。処理の改善
  - [ ] [L1514](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1514): `Holder<Enchantment>` を取得してチェックする
  - [ ] [L1573](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1573): 弾道調整(archerShootVelocityFactor)が必要な場合 performCrossbowAttack をオーバーライドする
  - [ ] [L1578](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1578): コメントを差す
  - [ ] [L1811](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1811): 複数モデルで問題ないかチェックする
  - [ ] [L1823](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1823): 処理の見直し、処理を追加可能に
  - [ ] [L1824](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1824): 使用アイテムをコンフィグから追加可能に
  - [ ] [L2101](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2101): 計算式の改善
  - [ ] [L2142](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2142): どこで使われるメソッドかわからん、使われてない or 代替可能なら消す
  - [ ] [L2156](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2156): 処理の改善
  - [ ] [L2228](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2228): IdFactorの仕様の改善
  - [ ] [L2490](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2490): 暫定でテイム済みのモブは攻撃対象から外す
  - [ ] [L2491](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2491): TODO
  - [ ] [L2623](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2623): 強制再生メソッドを生やす
  - [ ] [L2624](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2624): 再生クールダウンをコンフィグ化
  - [ ] [L2686](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2686): このクラス置く場所がここでいいのかチェック、間違っているなら代替可能なら削除、そうでない場合は正しい位置に移動
- [LittleMaidModelCaps.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidModelCaps.java)
  - [ ] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidModelCaps.java#L21): インベントリの挙動を修正
- [MaidSoulEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java)
  - [ ] [L170](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java#L170): エフェクト調整
  - [ ] [L183](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java#L183): 憑依ステータス効果

### ⚔️ モード (Mode)
- [ArcherMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/ArcherMode.java)
  - [ ] [L33](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/ArcherMode.java#L33): 処理の見直し
- [FencerMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/FencerMode.java)
  - [ ] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/FencerMode.java#L21): 相手が無敵時間中は殴らない
- [HealerMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/HealerMode.java)
  - [ ] [L25](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/HealerMode.java#L25): 処理のリファクタリング
- [PharmcistMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/PharmcistMode.java)
  - [ ] [L15](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/PharmcistMode.java#L15): 実装する
- [TorcherMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java)
  - [ ] [L29](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java#L29): 処理の改善
  - [ ] [L60](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java#L60): blockFinder of TorcherMode 共通化

### 🔀 その他 / Mixin / タグ
- [MixinPlayerEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/mixin/MixinPlayerEntity.java)
  - [ ] [L32](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/mixin/MixinPlayerEntity.java#L32): TargetTagManagerはServerPlayer側で実装するべきかチェック
- [LMTags.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/tags/LMTags.java)
  - [ ] [L14](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/tags/LMTags.java#L14): 判定をタグとコンフィグで行えるように仕様を調整

---

## 5. 🛠️ Java 8+ 近代化（完了）

*   **record化**: DTOクラス等のイミュータブル化。
    *   `util/Tuple`, `resource/util/TexturePair`, `client/screen/component/RangeChecker`
*   **instanceof パターンマッチング**: キャストの冗長性削減。
    *   `network/NetworkHandler` (2箇所), `block/SalaryBoxBlock`, `client/renderer/LMHeadFeatureRenderer` (2箇所), `resource/classloader/MultiModelClassTransformer`
*   **switch式 (arrow-form switch)**: 可読性向上と簡素化。
    *   `maidmodel/ModelPlate`, `ModelMultiMMMBase`, `ModelMultiBase`, `ModelRenderer`, `ModelLittleMaid_RX0`, `EntityCaps`
