# LittleMaidNeo — システム統合における技術的困難と重要維持項目 (TODO_System)

NeoForge 移行および 2MOD 統合プロセスで、クリーンアップ・削除が困難な部分、互換性保護のために
維持すべき部分、リファクタリング時の設計上の罠をここに記録します。

---

## 🎯 リファクタリング方針（2026-05-31 更新・最重要）

**絶対に壊してはならないベース Mod 機能は次の 2 つのみ:**
1. **外部からモデル／テクスチャを読み込む機能**（外部モデルパック `.class` の ASM リマップ読み込み・描画を含む）
2. **外部からボイスパック（サウンドパック）を読み込む機能**

> この 2 機能が担保され、かつ **Mod 全体の挙動（マルチプレイ同期・セーブ互換を含む）に問題が出ない限り**、
> 従来「現状維持境界」としていた箇所（後述 §3 / §4 等）も **破壊・再設計して構わない**。
> 逆に、上記 2 機能の load-bearing 部分（後述「🛡️ 保護コア」）は**シグネチャ・データ形式・命名規則・探索パスを不変**に保つこと。

---

## 🛡️ 保護コア（上記 2 機能の load-bearing）

> **【2026-05-31 追記・モデル側の方針緩和】**
> 保護コア A（モデル/テクスチャ/描画）は「該当ファイルを触るな」ではなく、
> **次の 2 つの観測可能な保証を満たす限り、内部実装は書き換えてよい**：
> 1. **メイドさんが正常に描画される**（既存モデル SR2/AC/RX0/Steve 等・防具・手持ち・頭部装飾を含む）。
> 2. **既存の外部モデルパックを、ユーザー側を改変させずにそのままロードできる**
>    （外部 `.class` パックが現状のまま読み込めること）。
>
> → ASM リマップ・描画ラッパー型・`maidmodel/` のシグネチャを変える場合でも、
>   **ローダー/トランスフォーマ側でブリッジを用意し、既存パックが無改変でロード＆描画できる**なら可。
>   ただしこの 2 保証は **実機で必ず検証**（runClient で実在の外部パックをロードして描画確認）。
>   保証を割れない確証が持てない変更は行わない。
> 保護コア B（ボイス）は従来どおり外部契約（`.cfg` 形式・`LMSounds` 定数・命名規則・パケット形式）を維持する。

### A. 外部モデル／テクスチャ読み込み・描画（上記 2 保証を満たせば内部書換可）
* **ASM リマップ基盤**: `resource/classloader/MultiModelClassLoader`・`MultiModelClassTransformer`、リマップ先スタブ `entity/EntityLittleMaid`、リマップ先パッケージ `work.nemonet.littlemaidneo.maidmodel`（Transformer にハードコード）。
* **描画ラッパー層**: `maidmodel/` 全般（`ModelMultiBase`・`EntityCaps`・`ModelRenderer` 等）、`maidmodel/compat/GLCompat`、`multimodel/layer/`（`MMMatrixStack`/`MMVertexConsumer`/`MMPose` 等）、`multimodel/IMultiModel`・`maidmodel/IModelCaps`。
  → 外部モデルパックがこれらの型・メソッドを override するため、**シグネチャ変更は `LinkageError`/`AbstractMethodError` を招く**。
* **リソース探索／登録**: `resource/loader/LMFileLoader`（`LMMLResources/` 探索順）、`LMMultiModelLoader`/`LMTextureLoader`、`resource/manager/LMModelManager`/`LMTextureManager`（**登録モデル名＝パック探索キー不変**）、`resource/holder/TextureHolder`、`resource/util/ResourceHelper`（命名・Identifier 生成ロジック）。

### B. 外部ボイスパック読み込み・再生
不変厳守なのは主に「外部パック作者が依存する契約」と「探索・再生フォーマット」:
* **探索パス／形式**: `LMMLResources/` フォルダ、`.ogg` 探索（`client/resource/loader/LMSoundLoader`）、`.cfg` パース形式（`resource/loader/LMConfigLoader`、`key=value` 例: `se_hurt=pack.parent.file`）、`LMFileLoader`／`LMLoader` 契約。
* **命名規則**: `resource/util/ResourceHelper`（pack/parent/file 抽出・`getLocation()` の Identifier 生成）、`ConfigHolder`（`packName`/`parentName`/`fileName`/`settings` と `getSoundFileName()` のキー形式）。
* **サウンド名定数**: `resource/util/LMSounds`（`se_hurt`・`se_attack` 等の文字列＝エンティティ ↔ `.cfg` の契約）。**定数文字列の変更は全既存ボイスパックを壊す**。
* **再生・橋渡し**: `entity/compound/SoundPlayable`／`SoundPlayableCompound`、`client/resource/manager/LMSoundManager`、`client/resource/LMSoundInstance`、`client/resource/ResourceWrapper`・`LMPackProvider`（MC リソース系への橋渡し）、`resource/manager/LMConfigManager`。
* **ネットワーク形式**: `network/LMSoundPayload`・`SyncSoundPackPayload`・`SyncSoundConfigPayload` の**パケットフォーマット**（codec）。
* **音量**: `config/LMMLConfig#getVoiceVolume()`（キー名・範囲）。

> 注: 上記クラスでも「外部契約に関わらない内部実装」は整理可。
> 例えば `SoundPlayableCompound` の内部リファクタは可だが、`SoundPlayable.play(String)` のシグネチャと
> `LMSounds.*` 文字列・`.cfg` キー解決の振る舞いは不変に保つ。

---

## 🗂️ 旧「現状維持境界」の再分類

### §1. 外部モデルパック（.class）の動的 ASM リマップ — 🟡 **2 保証を満たせば内部書換可**
`MultiModelClassLoader` / `MultiModelClassTransformer` / `EntityLittleMaid`（スタブ）。
→ 保護機能①の中核だが、**既存外部パックが無改変でロードでき、メイドさんが正常描画される**なら内部刷新可。
  例: トランスフォーマのマッピング表拡張やブリッジ追加で、内部構造を変えつつ旧パックの互換を維持する等。
  ⚠️ 互換が割れると全外部パックが読めなくなる高リスク領域。実機ロード検証必須。

### §2. 描画ラッパー層（`MMMatrixStack`/`MMVertexConsumer` 等）— 🟡 **2 保証を満たせば内部書換可**
`maidmodel/` 全般・`GLCompat`。外部モデルパックがこれらの型・メソッドを override する。
→ 標準型（`PoseStack`/`VertexConsumer`）への統一等も、**ローダー/トランスフォーマ側で旧シグネチャを
  ブリッジし、既存パックが無改変でロード＆描画できる**なら可。
  ⚠️ ブリッジなしのシグネチャ変更は `LinkageError`/`AbstractMethodError` を招く。実機描画検証必須。

### §3. Mixin による Vanilla 注入 interface と Impl — 🔓 **解禁（保護2機能と無関係）**
`entity/mode/HasMode`、`entity/targeting/TargetTagManager`（`MixinPlayerEntity`）、`entity/util/MaidManager`（`MixinServerPlayerEntity`）。
**確認済み: `resource`/`multimodel`/`maidmodel` はこれらに一切依存していない** → モデル/テクスチャ/ボイス読み込みと無関係。
従来は「Mixin が Vanilla `Player`/`ServerPlayer` へ多態性契約を注入する境界」として現状維持としていたが、
保護対象外のため**再設計可**。ただし下記の挙動は維持すること:
* `instanceof MaidManager` / `instanceof TargetTagManager` による Vanilla プレイヤーへのキャスト（`NetworkHandler`・`MaidSoulEntity`・`LittleMaidEntity` 等）。
* マルチプレイ同期、ディメンション移動時のデータ引き継ぎ（`MixinServerPlayerEntity#restoreFrom` 等）、NBT セーブ互換。
→ 詳細な解禁タスクは R-2 / R-3 参照。

### §4. 多階層 Goal 継承チェーンの縮約 — 🔓 **解禁（保護2機能と無関係）**
`StareAtHeldItemGoal`（旧基底）/ `TameableStareAtHeldItemGoal`（旧中間）/ `FollowAtHeldItemGoal` / `LMStareAtHeldItemGoal`。
中間抽象の削除はドミノ倒しを招くため従来は慎重対応としていたが、保護機能とは無関係。
→ サブクラスを自己完結化しつつ継承段数を縮約する整理を**進めてよい**（R-5 参照）。挙動（AI 目標の動作）は不変に。

---

## ✅ リファクタリング候補（統合・記述量削減）

> 大前提: 🛡️ 保護コアのシグネチャ・データ形式・命名規則・探索パスは不変。
> 検証: 各項目で `./gradlew compileJava` を通し、可能なら `runClient`/`runServer` で
> 外部モデル読み込み・ボイス再生・マルチ同期を実機確認しながら小さく進める。

### 🟥 R-1. `Impl` 内部の「インスタンス／static 二重実装」解消
`MaidManagerImpl` / `TargetTagManagerImpl` は同一シリアライズ処理を
インスタンスメソッドと static メソッドの両方で重複保持。単一ソース化する。
* `MaidManagerImpl#writeMaidManager/readMaidManager`(L35-51) ⇔ `MaidManagerImpl.write/read`(static, L90-104)
* `TargetTagManagerImpl#writeTargetTags/readTargetTags`(L127-153) ⇔ `TargetTagManagerImpl.write/read`(static, L135-168)
* ⚠️ `MaidManager.LMInfo`（sealed）の NBT キー・フィールド構成は不変（セーブ互換）。

### 🟥 R-2. §3 Mixin 注入の再設計（interface + Impl の薄い二重構造の解消）
**【解禁】** 保護機能と無関係になったため、以下いずれかで整理可:
* (a) **NeoForge Data Attachment（`AttachmentType`）への移行** — Vanilla `Player`/`ServerPlayer` への
  状態付与を Mixin+interface+Impl から Data Attachment に置換し、`MixinPlayerEntity`/`MixinServerPlayerEntity` と
  `MaidManager`/`TargetTagManager` interface を撤廃。`instanceof` キャスト箇所は Attachment 取得に置換。
  ※現状 Data Attachment は未使用（新規導入）。**NBT セーブ互換・マルチ同期・ディメンション移動引き継ぎを要検証**。
* (b) Data Attachment 導入が重い場合は、最低限 R-1 の内部重複解消に留める。
* `HasMode`/`HasModeImpl` は **Mixin 非依存**（`LittleMaidEntity` のフィールド合成のみ・`instanceof HasMode` 未使用、
  `MixinLivingEntity` も存在しない）。→ interface を畳んで具象へ統合し記述量削減してよい。

### 🟥 R-3. `LittleMaidEntity`（2763 行）の機能分割
`CLAUDE.md` 記載の `LMGoalInitializer`/`LMSafeMovement`/`LMInteractionHandler`/`MaidResurrection`/`MaidSoul` は
**実在しない**（実在の委譲先は `LMHasInventory`/`LMItemContractable` のみ）。実際に抽出して乖離を解消する。
`super` を含む override 本体はクラスに残し、**ロジックのみ委譲**。
* 復活演出 `resurrectionMaid()`（static, L266-415）→ `MaidResurrection` へ全移動（障壁なし・着手容易）。
* 本パラメータ適用 `applyParametersFromBook()`（L2678-2724）→ `BookParameterParser` 等へ（障壁なし）。
* Goal 登録 `registerGoals()`（L420-669）→ Goal 生成を `LMGoalInitializer.init(...)` へ委譲（override 本体は残す）。
  ⚠️ `initGoals()` は `Mob` コンストラクタ内で呼ばれフィールド未初期化 → ラムダ遅延参照。
* 右クリック `mobInteract()`（L1639-1870）→ アイテム別分岐を `LMInteractionHandler` へ委譲。
* 安全移動 `maybeBackOffFromEdge()` 等（L1428-1623）→ 危険判定を `LMSafeMovement` へ抽出。
* ⚠️ ボイス再生に関わる `play()/playForce()/setConfigHolder()/getConfigHolder()` のシグネチャは不変（保護コア B）。

### 🟧 R-4. モード具象クラス間の重複ボイラープレート共通化
* ブロックエンティティ探索＋キャスト: `CookingMode`(L107-132)/`PharmcistMode`(L279-304)/`TorcherMode` → `Optional<T> getBlockEntity(level,pos,Class<T>)`。
* インベントリ走査でスロット検索: `CookingMode`/`HealerMode`(L65-89)/`PharmcistMode` → `OptionalInt findSlot(Container, Predicate<ItemStack>)`。
* tick ベース経路再計算タイマー: `CookingMode`(L254-261)/`PharmcistMode`(L87-89)/`RipperMode`(L89-99)/`TorcherMode`(L159-178) → `PathRecalcTimer`。
* コンテナ間アイテム移送: `CookingMode`(L296-362)/`PharmcistMode`(L159-227) → 「空き/一致スロット探索→検証→移送」共通化。

### 🟧 R-5. §4 Goal 継承チェーンの整理
**【解禁】** 中間抽象（`StareAtHeldItemGoal`/`TameableStareAtHeldItemGoal`）の縮約。
サブクラス（`FollowAtHeldItemGoal`・`LMStareAtHeldItemGoal` 等）を自己完結化しつつ継承段数を減らす。
⚠️ AI 目標の動作（追従・注視の挙動）は不変に保つこと。

### 🟧 R-6. `Modes.java` のモード登録をテーブル駆動化
「`buildXxxMode()`×6」＋「static 代入」＋「`init()` で `register()`×6」の三重定義を 1 テーブルの一括ループ登録へ。
**登録 ModeType・ItemMatcher・Priority・登録順は完全維持**。約30行削減。

### 🟧 R-7. `HasMode` ⇔ `Mode` の NBT API 不整合の解消
`Mode` は `CompoundTag` 直接、`HasModeImpl` は `ValueOutput/ValueInput` のためラッパ発生（`HasModeImpl` L73-75）。
どちらかへ統一しラッパ除去。**NBT キー（`ModeID`/`ModeData`）・格納形式は不変**。

### 🟩 R-8. モデル／ボイスローダー系の内部重複整理（保護コアの命名・探索パス・登録名は不可触）
* `resource/manager/`（Model/Texture/Config）: `get()` ごとの `toLowerCase()` を登録時 1 回へ集約（**キー文字列自体は不変**）。
* `resource/util/ResourceHelper`: `getFileName()`/`getParentFolderName()`/`getTexturePackName()` の `replace("\\","/")` を `normalizePath()` へ共通化（**命名結果は不変**）。
* `resource/loader/LMFileLoader`: `loadArchive()`(L89-90)/`loadFile()`(L112-113) の同一ローダ適用ループを `applyLoaders(...)` へ（**実行順不変**）。
* `resource/manager/LMModelManager` の内部クラス `ModelHolder`（L65-86）のインライン化／record 化。

### 🟩 R-9. 薄い DTO／補助構造・フラグの整理（低優先・効果小）
* `resource/util/TexturePair`・`ArmorPart.Builder`（L51-76）の簡素化。
* `LittleMaidEntity#setLMMFlag/getLMMFlag`（L1933-1946）ビット操作の enum ラッパー化（**ビット位置・同期値は不変厳守**）。

---

## 🧠 R-10. AI システムを Goal 型から Brain（BehaviorControl）型へ書き換え

**【大規模アーキテクチャ刷新】** 現状のメイドさん AI は `GoalSelector` ベース（`registerGoals()` で登録、
`entity/goal/` に多数の Goal、§4 の継承チェーン）。これをバニラ新世代の **Brain / `BehaviorControl`（Activity・
Memory・Sensor）ベース**へ移行する。
* R-3（`registerGoals()` の委譲）・R-5（Goal 継承縮約）は本書き換えの**前段または一部置換**として位置づける。
  Goal を整理してから Brain へ移すか、機能単位で順次 Behavior へ移植するかは要設計判断。
* 移行対象の洗い出し: `entity/goal/` 配下の全 Goal（追従・戦闘・サルベージ・給料回収・待機・注視等）、
  `entity/mode/ModeWrapperGoal`（モードを Goal でラップしている箇所）、ターゲティング（`entity/targeting/`）。
* 設計メモ: `Brain<LittleMaidEntity>` の `MemoryModuleType` / `SensorType` / `Activity` を新規定義。
  モード（`HasMode`）とターゲティングを Memory/Activity にどうマッピングするか、`ModeWrapperGoal` の置換方針を要検討。
* ⚠️ 保護コアとは独立だが大規模。挙動（AI の振る舞い）の等価性を実機で検証しながら段階移行する。
* ⚠️ NBT セーブ互換: Brain の Memory 永続化と既存セーブの読み込み互換に注意（必要ならマイグレーション）。
* 調査は `mc-api-research` エージェントで Vanilla の Brain/Behavior API（`Villager`/`Piglin`/`Axolotl` 等の実装）を参照。
* **応用（データ駆動型 AI）**: AI パラメータ（「臆病」「好戦的」等の性格＝重み付けプロファイル）を `Codec` で JSON 定義し、
  エンティティ生成時に読み込んで Brain の挙動（優先スケジュール・重み）を変化させる設計も視野。DataGen（R-13）と連携可能。
* **具体設計**:
  - **`SensorType` 自作**: 周囲のアイテム/敵/主人/地形をスキャンし結果を Brain のメモリ（`MemoryModuleType`）へ書き込む。
  - **`Activity` でモード管理**: `IDLE`（待機）/`FOLLOW`（追従）/`FIGHT`（戦闘）/`WORK`（作業）等を定義し、
    各 Activity に実行可能な Behavior を優先度付きで割り当てる。
  - 記憶の揮発（一定時間で忘れる処理）と Activity 切替はバニラ `Brain` に委譲し、Mod 側は純粋な Behavior ロジックに集中。
  - 既存モード（`HasMode`/`entity/mode/`）は WORK 系 Activity の Behavior へマッピングし `ModeWrapperGoal` を置換。

## 🙂 R-11. メイドさんの首（頭部）の動きを `LookControl` で制御

メイドさんの首・頭部の向きを `LookControl`（`Mob#getLookControl()`）経由で制御するよう統一する。
* 現状の頭部向き制御の実装箇所を洗い出し（注視 Goal `LMStareAtHeldItemGoal`・begging の `tickInterestedAngle()`/
  `getInterestedAngle()`（L2200-2215 付近）・モードでの `getLookControl().setLookAt(...)` 呼び出し等）、
  `LookControl` ベースへ寄せて重複・不整合を解消する。
* R-10（Brain 化）と整合させる: Brain 移行時は `LookAtTargetSink` 相当の Behavior と `LookControl` の連携で
  首の向きを制御する形が自然。R-10 とセットで設計する。
* まずは **エンティティ側の向き値（yHeadRot 等）の制御に留める**のが低リスク。
  描画側に手を入れる場合は保護コア A の 2 保証（メイドさん正常描画／外部パック無改変ロード）を実機検証する。
* **視線判定（Raytrace）との同期**: `getDefaultDimensions(Pose)` が返す eyeHeight・ポーズを首振り/視線判定と一致させ、
  バニラ Raytrace と同期させる（既に Pose 対応の `getDefaultDimensions` は実装済み）。
  ⚠️ サイズ算出は `maidmodel` のモデル寸法に依存（**読み取りのみ・保護コア A は不変**）。L1084 のコメントどおり
  毎 tick の `EntityDimensions` 生成はキャッシュ最適化の余地あり。

### 🟧 R-12. 描画ラッパー層のモダン化（§2 の内部刷新・2 保証前提）

**【解禁】** `MMMatrixStack`/`MMVertexConsumer`/`MMPose` 等の独自描画ラッパーを、
バニラ標準型（`PoseStack`/`VertexConsumer`/`MultiBufferSource`）へ寄せて簡素化する。
* 必須条件: **既存外部モデルパックが無改変でロード＆描画できること**＋**メイドさん本体が正常描画されること**（保護コア A の 2 保証）。
* 現実的アプローチ: 外部パックは旧ラッパー型のメソッドを override しているため、
  - (a) ローダー/`MultiModelClassTransformer` 側で旧シグネチャ → 新標準型へのブリッジ（アダプタ）を生成・注入する、
  - (b) もしくは旧ラッパー型を「標準型への薄いアダプタ」として残しつつ内部実装だけ標準型に委譲する、
  のいずれかで互換を保つ。
* ⚠️ 高リスク。まず実在の外部パック数種で PoC を行い、`runClient` で描画一致を確認してから本適用。
* ⚠️ `GLCompat`（旧 GL11 → 現代描画）への ASM リダイレクトも、ブリッジ方針と整合させる。
* 背景: 1.20.1→1.21 で `RenderType`/`VertexFormat` 等に破壊的変更。旧テッセレーター直叩き/手動 GL は不可
  （現在は `GLCompat`＋`multimodel/layer` が現行パイプラインへブリッジして動作中）。
* **フル再構築（`EntityRenderer`/`LayerDefinition`/`ModelPart`/`AgeableListModel` ベース）の注意**:
  バニラ `ModelPart`/`LayerDefinition` へ完全移行すると、**外部 `.class` モデルパックが依存する `maidmodel/` 独自ジオメトリと
  非互換**になり保護機能①が壊れる。→ フル再構築は「外部パック用に旧描画パスを並行維持する」前提でのみ可。
  資産（テクスチャ/モデル定義データ）は流用可だが、描画フック一本化は 2 保証を割らない範囲に限定する。

---

## 🧱 現代 NeoForge アーキテクチャ採用状況（ギャップ分析）

本 Mod は **既に NeoForge 26.1.2 上**にあり、1.6.4 → 現代の移行は大部分が完了済み。
以下は「現代化機能の採用状況」と、残ギャップに対する移行タスク。

| 機能 | 状況 | メモ |
|---|---|---|
| Modern Payload Networking（`RegisterPayloadHandlersEvent`/`StreamCodec`） | ✅ 導入済 | `network/NetworkHandler`。サウンド/同期パケットも保護コア B として運用中 |
| Deferred Register / DeferredHolder | ✅ 導入済 | `setup/ModRegistration`（Entity/Block/Item/Menu/CreativeTab 等） |
| EntityAttributeCreationEvent | ✅ 導入済 | `onEntityAttributeCreation`（CLAUDE.md 記載どおり） |
| EntityRenderersEvent（RegisterRenderers/LayerDefinitions） | ✅ 導入済 | `LittleMaidNeoClient#onRegisterRenderers`。⚠️ メイドさん本体の描画は外部モデルパック互換（保護コア A）に依存 |
| Data Components（ItemStack） | ✅ 概ね現代化 | `item/LittleMaidSpawnEggItem` が `DataComponents` 使用。**ItemStack 旧 NBT（`getOrCreateTag` 等）は不使用** |
| **Data Attachments（`AttachmentType`）** | ❌ **未採用** | → R-14。プレイヤー状態(R-2)/メイド AI ステート(R-10)/魂データ引き継ぎ(R-3) で活用 |
| **DataGen（`GatherDataEvent`）** | ❌ **未導入** | → R-13。loot/tags/recipes/advancements/lang を手動 JSON で保守中。`runData` 未配線 |
| **Brain / `MemoryModuleType` / `SensorType`** | ❌ 未導入（Goal ベース） | → R-10。MemoryModuleType は DeferredRegister で登録 |
| GeckoLib / AzureLib | 🟡 条件付き可 | メイドさん本体に導入する場合、**既存外部モデルパックが無改変でロード＆描画できる経路を別途維持**することが条件（保護コア A の 2 保証）。両立が困難なら新規補助エンティティ限定。要 PoC・実機検証 |
| データ駆動スポーン（`RegisterSpawnPlacementsEvent`/Biome Modifiers） | ✅ 導入済 | `setup/ModSetup` で登録、`data/littlemaidneo/neoforge/biome_modifier/maid_spawn.json`。⚠️ JSON は手書き → R-13 で `BiomeModifierProvider` 自動生成へ寄せられる |
| ModConfigSpec（TOML） | ✅ 導入済 | `config/LMRBConfig`/`LMMLConfig`。⚠️ 全て `ModConfig.Type.COMMON` ＝**サーバー自動同期なし** → R-16 |
| EntityDimensions / Pose 動的サイズ | ✅ 導入済 | `LittleMaidEntity#getDefaultDimensions(Pose)` がモデル連動で幅/高さを返す。⚠️ 毎回生成のキャッシュ最適化余地（L1084 コメント）、視線高さ同期は R-11 と関連 |
| **Entity タグ（`TagKey<EntityType>`）による AI 抽象化** | ❌ **未使用** | `TargetTagManagerImpl` に `instanceof` ハードコード **19 箇所** → R-15 |
| ICondition（条件付き DataGen） | ❌ 未使用 | DataGen 未導入のため。→ R-13 の一部（他 Mod 連携ドロップ/レシピ） |
| Codec データ駆動型 AI プロファイル | ❌ 未使用 | → R-10 の応用（性格/重み付け JSON） |
| 手動シリアライズ → `Codec`/`StreamCodec` | ✅ **概ね完了** | パケットは全 15 ペイロードが `StreamCodec` 採用、**手動 `FriendlyByteBuf` インデックス追跡型は無し**。NBT も `ValueInput`/`ValueOutput`＋`Codec`。残課題は `addAdditionalSaveData` の per-field 手動 put/get を record+Codec へ集約（→ R-14） |
| ItemStack 独自データ（旧 NBT 直書き） | ✅ 廃止済 | `getOrCreateTag` 等不使用。独自データは `DataComponentType`＋Record で（`LittleMaidSpawnEggItem` 実績） |
| レンダリングパイプライン（`RenderType`/`VertexFormat`、1.20.1→1.21 破壊的変更） | 🟢 現行 26.1 で動作中 | 旧 GL 直叩きは `GLCompat`＋`multimodel/layer` が現行パイプラインへブリッジ済み。さらなる現代化は R-12（2 保証前提） |

---

## 🏗️ R-13. DataGen（`GatherDataEvent`）の導入

膨大なインフラ JSON を手書き保守している状態を解消し、Java コードから自動生成する。
* 対象（現状すべて手動 JSON）: loot table（`salary_box`・`little_maid_mob`）、tags（11 ファイル — モード用 `{mode}_mode.json` 含む）、
  recipes・advancements（計 6）、lang（`en_us`/`ja_jp`）。
* 実装: `GatherDataEvent` で `DataProvider`（`LootTableProvider`/`TagsProvider`/`RecipeProvider`/`AdvancementProvider`/
  言語は `LanguageProvider`）を登録。出力先は `src/generated/resources/`（CLAUDE.md 既出）。`build.gradle` の `runData` を配線。
* ⚠️ モード用タグ（`tags/items/{mode}_mode.json`）は `api/mode/Modes` の登録と整合させ、生成元を Java 側に一本化すると
  R-6（Modes テーブル駆動化）と相性が良い。
* **Biome Modifier も `BiomeModifierProvider` で生成**し、手書き `maid_spawn.json` を Java 側へ一本化
  （他 Mod 追加バイオームへの対応が容易になる）。スポーン条件は `RegisterSpawnPlacementsEvent` 側で型安全に定義（既存登録を踏襲）。
* **`ICondition` の活用**: 他 Mod 導入時のみ有効なドロップ（連携アイテム）やレシピ、相互作用プロファイルを
  条件付きで生成（`data/.../conditions`）。連携先 Mod 非導入時は無効化。
* ⚠️ 保護コア（外部モデル/ボイスの探索・命名）には JSON が絡まないため影響なし。生成結果が既存 JSON と一致することを差分確認。

## 🔌 R-14. Data Attachments（`AttachmentType`）の全面活用

現在 `AttachmentType` は未使用。旧 Capability/`IExtendedEntityProperties` 相当の独自ステート付与を Data Attachment へ。
* **プレイヤー状態（R-2 と統合）**: `MixinServerPlayerEntity`/`MixinPlayerEntity` が注入する `MaidManager`/`TargetTagManager`
  のステートを `AttachmentType`＋`Codec` に置換。Mixin+interface+Impl を撤廃でき、`instanceof` キャストは Attachment 取得へ。
* **メイドさんの AI ステート（R-10 と統合）**: 警戒度・各種パラメータなど Brain 化で必要になる永続データを `AttachmentType` で保持。
* **魂データ引き継ぎ（R-3 と統合）**: `copyOnDeath()` を利用し、メイドさん死亡 → 魂化/復活時のデータ引き継ぎを簡潔化
  （現状 `MaidSoul` の手動 NBT 受け渡しを置換可能か検討）。
* メリット: `Codec` 指定だけで NBT 自動保存/読込が完結。手動 `write/read` の記述量削減（R-1 とも連動）。
* ⚠️ **既存セーブ互換**: 旧 NBT キーからの移行（マイグレーション）と、マルチ同期（必要な Attachment は同期設定）を要検証。
* **状態の 2 レイヤー分離（設計指針）**:
  - 永続データ（モード/主人情報/内部インベントリ/AI 記憶コンテキスト）→ **Data Attachments＋Codec**（自動セーブ）。
  - 描画・モーションに必要な最小限のステートのみ → **`SynchedEntityData`（`EntityDataAccessor`）**（DataWatcher の正統進化）。
  現状 `addAdditionalSaveData`（L690-）の per-field 手動 put/get は record+Codec に集約し、手動 NBT の温床を排除する。

---

## 🏷️ R-15. Entity タグ（`TagKey<EntityType>`）による AI ターゲティングのデータ駆動化

現状 `TargetTagManagerImpl` は **`instanceof` ハードコード 19 箇所**（`Creeper`/`Warden`/`Enemy`/`Piglin`/
`ZombifiedPiglin`/`EnderMan`/`TamableAnimal`/`Npc`/`Merchant`/`ArmorStand`/家畜系 等）でターゲット可否
（先制攻撃禁止・接近禁止・攻撃禁止・遠近武器禁止）を決めている。これをタグ駆動へ。
* 独自タグ `TagKey<EntityType<?>>` を定義（例: `#littlemaidneo:attack_prohibited` / `approach_prohibited` /
  `preemptive_attack_prohibited` / `ranged_weapon_prohibited` / `melee_weapon_prohibited`）。
  AI 側は `entity.getType().is(tag)` で判定し、ハードコードを排除。
* 分類は **DataGen の `TagsProvider` で生成**（R-13 と連携）。バニラタグ（`minecraft:undead`/`arthropod` 等）も活用、
  他 Mod モブも JSON 側で対象に含められる。
* §3／R-2 のターゲティング再設計（`TargetTagManager` の Data Attachment 化）と**統合して進める**。
* ⚠️ 既存のターゲティング挙動（現行の判定結果）を**等価に保つ**こと。既存セーブの `targetTagMap` 互換に注意
  （ユーザーが GUI で個別設定したタグの読み込み互換）。

## ⚙️ R-16. サーバーコンフィグの自動同期（`ModConfig.Type.SERVER` 移行）

現状 `LMRBConfig`/`LMMLConfig` は **`ModConfig.Type.COMMON`** 登録（`LittleMaidNeo` L59-60）。
クライアント・サーバーで別管理になり、マルチでは手動コピーが必要（`TODO.md` バックログ「鯖蔵コンフィグの同期」）。
* サーバー権威であるべき設定（索敵範囲・攻撃力・各機能 ON/OFF 等）を **`ModConfig.Type.SERVER`** へ移し、
  接続時にクライアントへ自動同期させる。
* クライアント専用設定（音量 `getVoiceVolume` 等）は `Type.CLIENT`、両者にまたがるものは整理して再分類。
* ⚠️ 設定分類の変更で TOML ファイル構成が変わる。既存ユーザー設定の移行に配慮。
  `getVoiceVolume`（保護コア B）は**キー名・範囲を不変**に保つ。

---

## 📝 R-17. `CLAUDE.md` の書き直し（重要）

`CLAUDE.md` は**旧ソースから引っ張ってきた記述が多く、現行実装と乖離している**。実装に合わせて書き直すこと。
判明済みの乖離（最低限ここは直す）:
* 「`LittleMaidEntity` は `LMGoalInitializer`/`LMSafeMovement`/`LMInteractionHandler`/`MaidResurrection`/`MaidSoul` への
  分割パターンを採用」とあるが **これらのクラスは実在しない**（実在は `LMHasInventory`/`LMItemContractable` のみ）。
  → R-3 で実際に抽出後、記述を実態に合わせる。
* 「現状維持境界」前提の記述（Mixin 注入 interface は統合不可 等）は、本ファイルの新方針（保護2機能以外は解禁）に合わせて更新。
* リファクタ（R-1〜R-16）の進行に応じて、該当する CLAUDE.md の Architecture / Notes 節を随時更新する。
* 現代化ギャップ（Data Attachments / DataGen / Brain）の採用状況も反映する。
* 作業はリファクタと同期させる（コードを変えたら CLAUDE.md も同コミットで更新するのが望ましい）。

---

## 🧭 リプレース推奨実施順序（フェーズ別ロードマップ）

> 既に現代化済みの基盤（StreamCodec パケット・`ValueInput/Output`＋Codec NBT・DataComponents・DeferredRegister・
> Biome Modifiers・Payload Networking）の上に、未解消ギャップと記述量削減を段階適用する。

1. **基盤整備**: `DeferredRegister` に `MemoryModuleType`/`SensorType`（/必要なら `ArgumentType`）を追加し 26.1 ライフサイクルで定義（R-10 の前提）。
2. **DataGen 構築**: loot/tags/lang/biome modifier を Java 出力化し手動 JSON を即廃止（R-13）。R-15 のターゲティング用 Entity タグ生成基盤もここで整える。
3. **データ構造の決定**: モブ/アイテムのデータを Record＋Codec で定義し、Data Attachments / Data Components へ寄せる（R-14・状態 2 レイヤー分離）。`addAdditionalSaveData` の手動 put/get もここで集約。
4. **AI（Brain）構築**: 最小の Sensor/Behavior から段階実装（R-10）。R-5 で Goal を整理してから移植、R-15 のタグでターゲティングをデータ駆動化、R-7 でモードの Behavior 化を整合。
5. **描画/アニメ再結合**: `maidmodel` の資産取得を最新 `ResourceManager` 経由に整理しつつ、保護コア A の 2 保証を守って現行パイプラインへ結合（R-12）。

付随作業（各フェーズで並行可）: R-1〜R-4・R-6・R-8・R-9（記述量削減）、R-11（首の LookControl）、R-16（サーバー Config 同期）。
仕上げに R-17（`CLAUDE.md` を実態へ更新）。

---

## ⚠️ 本タスクの対象外（挙動が変わるため別途）

* 一部モードの状態 NBT 未永続化（Archer/Fencer の cooldown、Healer の index 等）は**リロード時の挙動を変える修正**のため、
  記述量削減リファクタとは分けて `TODO.md` のバックログで扱う。
