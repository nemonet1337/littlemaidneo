# LittleMaidNeo 実装 HOWTO（モダン化リファクタリング）

> 対象読者: 本リポジトリを実装する開発者／AI。
> 前提環境: **Java 25**。`./gradlew build` / `runClient` / `runServer` / `runData` / `runGameTestServer`。
>   ※ ローカルに JDK25/foojay 解決が無い環境では、検証は **push 後の CI（Java 25）** が唯一の手段（ADR-0002 と同方針）。
> 関連ドキュメント: 全体プラン `docs/plan/2026-06-01_統合リファクタリングプラン.md`、設計判断 `docs/adr/`、バックログ `TODO.md`。
> 本書は旧 `HOWTO.md`（Phase 4〜10 ガイド）と旧 `TODO_System.md`（保護コア技術詳細）を統合・刷新したもの。

---

## 0. 共通ルール（全作業で厳守）

### 0.1 絶対に壊してはならないベース Mod 機能は 2 つのみ
1. **外部モデル／テクスチャ読み込み・描画**（外部モデルパック `.class` の ASM リマップ読み込み・描画）
2. **外部ボイスパック（サウンドパック）読み込み・再生**

この 2 機能が担保され、かつ **Mod 全体の挙動（マルチ同期・セーブ互換を含む）に問題が出ない限り**、それ以外は破壊・再設計してよい。逆に、下記「🛡️ 保護コア」の load-bearing 部分は **シグネチャ・データ形式・命名規則・探索パスを不変** に保つこと。

### 0.2 🛡️ 保護コア A: 外部モデル／テクスチャ読み込み・描画

> 方針: 「該当ファイルを触るな」ではなく、**次の 2 つの観測可能な保証を満たす限り内部実装は書き換えてよい**。
> 1. **メイドさんが正常描画される**（SR2/AC/RX0/Steve 等・防具・手持ち・頭部装飾を含む）。
> 2. **既存の外部モデルパックを、ユーザー側を改変させずにそのままロードできる**。
> → シグネチャを変える場合でもローダー/トランスフォーマ側でブリッジを用意し、上記 2 保証を **実機（runClient）で必ず検証**。割れない確証が持てない変更はしない。

- **ASM リマップ基盤**: `resource/classloader/MultiModelClassLoader`・`MultiModelClassTransformer`（リマップ表 `CODE_REPLACE_MAP`、GL11→`GLCompat` リダイレクト）。リマップ先スタブ `entity/EntityLittleMaid`、リマップ先パッケージ `work.nemonet.littlemaidneo.maidmodel`（Transformer にハードコード）。
- **描画ラッパー層**: `maidmodel/` 全般（`ModelMultiBase`/`EntityCaps`/`ModelRenderer`/`IModelCaps`）、`maidmodel/compat/GLCompat`、`multimodel/layer/`（`MMMatrixStack`/`MMVertexConsumer`/`MMPose`/`MMRenderContext`）、`multimodel/IMultiModel`。
  → 外部パックがこれらの型・メソッドを override するため、**シグネチャ変更は `LinkageError`/`AbstractMethodError` に直結**。
- **リソース探索／登録**: `resource/loader/LMFileLoader`（`LMMLResources/` 探索順）、`LMMultiModelLoader`/`LMTextureLoader`、`resource/manager/LMModelManager`/`LMTextureManager`（**登録モデル名＝パック探索キー不変**）、`resource/holder/TextureHolder`、`resource/util/ResourceHelper`（命名・Identifier 生成）。
- **描画モダン化は ADR-0001 で見送り決定**: 本体の GeckoLib/`ModelPart` 移行はしない。ホットパス最適化（P-1〜P-6）は実施済み。これ以上の描画刷新は本リファクタの対象外。

### 0.3 🛡️ 保護コア B: 外部ボイスパック読み込み・再生

不変厳守は「外部パック作者が依存する契約」と「探索・再生フォーマット」:
- **探索パス／形式**: `LMMLResources/` フォルダ、`.ogg` 探索（`client/resource/loader/LMSoundLoader`）、`.cfg` パース（`resource/loader/LMConfigLoader`、`key=value` 例 `se_hurt=pack.parent.file`）、`LMFileLoader`/`LMLoader` 契約。
- **命名規則**: `resource/util/ResourceHelper`（pack/parent/file 抽出・`getLocation()` の Identifier 生成）、`ConfigHolder`（`packName`/`parentName`/`fileName`/`settings`・`getSoundFileName()` のキー形式）。
- **サウンド名定数**: `resource/util/LMSounds` の **定数文字列**（`se_hurt`/`se_attack` 等＝エンティティ↔`.cfg` 契約）。**変更は全既存ボイスパックを壊す**。
- **再生・橋渡し**: `entity/compound/SoundPlayable`/`SoundPlayableCompound`、`client/resource/manager/LMSoundManager`、`client/resource/LMSoundInstance`、`client/resource/ResourceWrapper`・`LMPackProvider`、`resource/manager/LMConfigManager`。
- **ネットワーク形式**: `network/LMSoundPayload`・`SyncSoundPackPayload`・`SyncSoundConfigPayload` のパケットフォーマット（codec）。
- **音量**: `config/LMMLConfig#getVoiceVolume()`（キー名・範囲不変）。
  > 注: 上記クラスでも「外部契約に関わらない内部実装」は整理可。`SoundPlayable.play(String)` のシグネチャと `LMSounds.*` 文字列・`.cfg` キー解決の振る舞いは不変に保つこと。

### 0.4 既存実装の事実（推測しないこと）
- `LittleMaidEntity` の委譲先: `MaidResurrection`/`BookParameterParser`/`LMInteractionHandler`/`LMSafeMovement`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MultiModelCompound`/`SoundPlayableCompound`/`TargetTagManagerImpl`/`TargetingSystem`/`MaidLookControl`。
- NBT 入出力は **`ValueOutput`/`ValueInput`＋Codec**（旧 `CompoundTag` 直書きは原則不使用）。
- パケットは全 15 種が `StreamCodec`（`network/`）。登録は `network/NetworkHandler.register(RegisterPayloadHandlersEvent)`。
- `DeferredRegister` は **`setup/ModRegistration.java`** に集約、`LittleMaidNeo` コンストラクタで `register(modEventBus)`。
- **AI 現状（ADR-0002）**: 移動軸 `MaidMode`（FREEDOM/ESCORT/TRACER）は **Brain Behavior 化済み**（`entity/ai/behavior/Maid*Behavior`）＋ Codec/StreamCodec。作業軸 `Mode`（`api/mode/`）は **`ModeWrapperGoal` で Goal ラップ**、`ModeManager.CODEC` で永続化。戦闘は `CombatMode` に統合。
- API 調査は `mc-api-research` エージェント必須。NeoForge/Mojang マッピングのメソッド名はバージョンで変わる。ライブラリ Doc は Context7 MCP 優先。

### 0.5 検証（コミット前に必ず）
1. `./gradlew compileJava`（全変更）／`./gradlew build`（区切り、CI 相当）
2. 保護コア A に触れたら `runClient` で **実在の外部モデルパック(.class)** を `LMMLResources/` に置き描画確認
3. 保護コア B に触れたら 外部ボイスパック(.cfg+.ogg) 再生確認
4. セーブ互換に触れたら **既存ワールドをロードして NBT エラーが出ないこと** ＋ `runServer` でマルチ接続・ディメンション移動引き継ぎ
5. DataGen は `runData` 後 `src/generated/resources/` が既存 JSON と **差分ゼロ（意味的一致）**
6. `runGameTestServer`（namespace `littlemaidneo`）で回帰確認

### 0.6 推奨実施順（依存関係）
**WS3 → WS2 → WS5 → WS4 → WS1**（低リスク基盤を先に、最高リスクの AI を最後に集中）。各 WS 完了＝1 コミット。重要な設計判断は `/doc` で `docs/adr/` に記録。CLAUDE.md の該当節も同コミットで更新。

---

## WS1 — AI 完全 Brain 化（作業モード含む・ADR-0002 改訂・最高リスク）

**目的**: 残存 Goal（補助 Goal ＋ 作業モードブリッジ `ModeWrapperGoal`）を Brain の Activity/Behavior へ移行し、移動・戦闘・作業・補助を Brain に一元化。ADR-0002 の「作業=Goal」2軸方針を改訂する（新 ADR を起こす）。

**現状（`entity/LittleMaidEntity.java`）**:
- Brain `CORE` Activity: `MaidWait/FollowOwner/Stare/Freedom/Trace` + `MoveToTargetSink`。`customServerAiStep` で `getBrain().tick()` 済み。
- `registerGoals()` 残存: `LMTeleportTameOwnerGoal`, `FloatGoal`, `OpenDoorGoal`, `LMHealMyselfGoal`, **`ModeWrapperGoal`**, `LMCollectSalaryFromContainerGoal`, `LMStoreItemToContainerGoal`, `LMMoveToDropItemGoal`, `PlaySnowGoal`, `LookAtPlayerGoal`×2, `RandomLookAroundGoal`、`targetSelector` に `LMTargetGoal`。

**手順（機能単位・1つ移すごとに runClient/GameTest で等価確認）**:
1. **Activity 体系化**: `CORE`（生存・移動・緊急TP）/ `IDLE`（見回り・PlaySnow）/ `WORK`（作業モード・治癒・給料/収納/搬送）/ `FIGHT`（ターゲティング）。`customServerAiStep` で `setActiveActivityToFirstValid([FIGHT, WORK, IDLE])`。
2. **作業モードの Brain 化（中核）**: `ModeWrapperGoal` を廃し `MaidWorkModeBehavior`（WORK の `BehaviorControl`）を新設。`Mode#shouldExecute()/tick()` 契約を `checkExtraStartConditions`/`canStillUse`/`tick` にマップ。**`ModeManager`/`HasModeImpl`/`CombatMode`/各 `Mode`・`ItemMatcher` 判定（Priority 降順）は再利用**（選択・実体は不変、駆動基盤のみ差し替え）。`CombatMode#getBattleModeType()/getJobName()`（caps 契約）は不変。
3. **補助 Goal → Behavior**:
   - `LMTeleportTameOwnerGoal` → `MaidEmergencyTeleportBehavior`（CORE）
   - `LMHealMyselfGoal` → `MaidHealSelfBehavior`（WORK）
   - `LMCollectSalary`/`LMStoreItem`/`LMMoveToDropItem` → WORK 系 Behavior（**既存 `BlockSearch`/`BlockReservationManager`/`WorkStrategy` 委譲を再利用**）
   - `PlaySnowGoal` → IDLE Behavior
   - `LMTargetGoal`（targetSelector）→ FIGHT の `MaidStartAttacking`/`MaidSetWalkTargetFromAttackTarget` 相当（**`TargetingSystem`/`TargetTagManagerImpl` を再利用**）
4. **バニラ Goal の置換/残置**:
   - `LookAtPlayerGoal`/`RandomLookAroundGoal` → `SetEntityLookTarget`+`LookAtTargetSink`+`RandomLookAround`（`LOOK_TARGET` メモリを provider に追加。現状 no-op 回避で未登録だった経緯を解禁）
   - `FloatGoal`/`OpenDoorGoal` は Brain 等価が薄いため CORE の薄い Behavior ラッパー化、困難なら最小限 Goal 残置を新 ADR で明記。
5. **メモリ/Sensor 拡張**: `ATTACK_TARGET`/`LOOK_TARGET`/`NEAREST_VISIBLE_*` 等を `BRAIN_PROVIDER`・`ModRegistration.MEMORY_MODULES` に追加。検出ロジックは `LittleMaidSensor` へ集約。

**影響ファイル**: `entity/LittleMaidEntity.java`（provider 拡張・`registerGoals()` 縮小撤去・Activity 切替）、新規 `entity/ai/behavior/MaidWorkModeBehavior.java` ほか `Maid*Behavior`、`entity/ai/sensor/LittleMaidSensor.java`、`setup/ModRegistration.java`。撤去: `entity/goal/`（移植完了分）, `entity/mode/ModeWrapperGoal.java`。新 ADR を `docs/adr/`。

**調査**: `mc-api-research` で `Villager`/`Piglin`/`Axolotl` の Brain/Behavior 実装を参照。

**検証**: 追従/待機/料理/治癒/サルベージ/給料回収/ターゲティングの GameTest（`createWorldPlayer()`/`cleanupWorldPlayers()`）を追加し移行前後で挙動同値。`runClient` 目視（首振りクランプ `MaidLookControl` 維持）。永続 Memory はセーブ互換に注意。

---

## WS2 — MaidSoul の Codec 化 + カスタム Data Components

**目的**: モード軸 Codec は ADR-0002 で完了済み。残る生 `CompoundTag` の **MaidSoul** を Codec 化し、メイドさん関連アイテムデータをカスタム `DataComponentType` で型安全化。

**手順**:
1. **MaidSoul の Codec 化**: `LittleMaidEntity$MaidSoul`（`fromNbt`/`getNbt` 手書き）・`MaidSoulEntity`・`world/WorldMaidSoulState`。`record MaidSoulData` を抽出し `RecordCodecBuilder` の `Codec` ＋ `StreamCodec`（保存／同期で再利用）を定義。**既存記法を踏襲**: `network/SyncMultiModelPayload.java`・`advancement/criterion/*Criterion.java`。
2. **カスタム DataComponentType 登録**: `setup/ModRegistration.java` に `DeferredRegister<DataComponentType<?>>`（`Registries.DATA_COMPONENT_TYPE`）を新設。`MAID_SOUL` component（`MaidSoulData` の Codec/StreamCodec）を登録し、`item/LittleMaidSpawnEggItem.java` の `DataComponents.ENTITY_DATA` 生 blob 依存のうち適切な部分を component 経由へ。

**影響ファイル**: 新規 `entity/soul/MaidSoulData.java`、`setup/ModRegistration.java`、`world/WorldMaidSoulState.java`・`entity/MaidSoulEntity.java`・`item/LittleMaidSpawnEggItem.java`。

**検証**: メイドさん→ソウル→復活の往復で UUID/名前/在庫が保存される GameTest。save→load 永続化。

---

## WS3 — DataGen で model / blockstate 生成

**目的**: 手書き JSON（`assets/littlemaidneo/{blockstates,models,items}/` の `salary_box`/`little_maid_spawn_egg`）を DataGen 化し Java を単一ソースに。

**現状の手書き JSON**: `blockstates/salary_box.json`、`models/block/salary_box.json`・`salary_box_open.json`、`models/item/salary_box.json`・`little_maid_spawn_egg.json`、`items/salary_box.json`・`little_maid_spawn_egg.json`。

**手順**:
1. `data/LMBlockStateProvider.java`（`BlockStateProvider`）で `salary_box`（open/closed）の blockstate＋block model＋item model 生成。
2. `data/LMItemModelProvider.java` で spawn egg・salary_box の item model / `items/` クライアント定義生成（WS2 のカスタム component と整合）。
3. `data/LMDataGenerator.java` の `GatherDataEvent.Client` に登録。出力は `src/generated/resources/`（git 追跡）。**`runData` 出力と既存 JSON が diff ゼロ**を確認後に手書き JSON を撤去。

**検証**: `./gradlew runData`（or `mergeData`）→ 生成差分が従来 JSON と等価。`runClient` で open/closed・spawn egg 表示目視。

---

## WS4 — Brigadier 管理コマンド

**目的**: 運用・デバッグ用コマンドを `RegisterCommandsEvent` + Brigadier で追加（現状コマンド皆無）。

**コマンド案（`/littlemaidneo`｜`/lmn`）**:
- `reload` — `LMMLResources/` 外部モデル/ボイスパック再走査（保護コア B は**読取のみ**・形式不変）
- `models list` — 読込済みモデルパック一覧（`resource/holder`/`manager` 参照）
- `maid count|tp|dismiss` — 近傍メイドさん管理（`requires(src -> src.hasPermission(2))`）
- `debug dump` — モデル/ボイス読込状態ダンプ

**影響ファイル**: 新規 `command/LMCommands.java`（`Commands.literal`）、`setup/ModSetup.java` か `LittleMaidNeo.java` で `RegisterCommandsEvent` 購読、lang `assets/littlemaidneo/lang/{en_us,ja_jp}.json` に応答キー追加。

**検証**: `runServer --nogui` で実行・補完・権限分岐。保護コア B に副作用なし（読取専用）を確認。

---

## WS5 — DataFixerUpper 導入（MaidSoul / エンティティ NBT 限定）

**目的**: メイドさん本体 NBT と MaidSoul のスキーマ版管理＋旧 LMRB/LMML キー互換に範囲限定して導入。**移動モード byte 形式の旧互換は ADR-0002 が非対応宣言済みのため対象外**。

**手順（最小実装 → 拡張）**:
1. **DataVersion**: メイドさん/MaidSoul 永続データに `dataVersion`(int) を埋め、読込時に段階アップグレード可能な土台を作る。
2. **バージョン付き Codec**: WS2 の `MaidSoulData` Codec に旧キー（旧 `Owner`/`UUID` 配列表現、旧モード ID 等）→ 新スキーマのフォールバック分岐（`xmap`/optional）を実装する `entity/soul/MaidDataFixer`。
3. **完全 DFU スキーマ**（`Schema`/`DataFix` 登録）の可否は NeoForge 制約調査が必要なため、**ADR で方針確定後に着手**（`mc-api-research` で NeoForge の DataFixer 登録可否を調査）。

**影響ファイル**: 新規 `entity/soul/MaidDataFixer.java`、WS2 の `MaidSoulData` Codec フォールバック分岐、`docs/adr/` に DFU 方針 ADR。

**検証**: 旧形式 NBT サンプル→新形式読込の GameTest。既存セーブの読込回帰。

---

## 付録: 現代化採用状況（ギャップ分析・main 時点）

| 機能 | 状況 | メモ |
|---|---|---|
| Payload Networking（`StreamCodec`） / Deferred Register / Data Attachments / Mixin / TOML / Mojang mappings / moddev / 並行ロード / BlockState | ✅ 採用済 | `network/`, `setup/ModRegistration`, `build.gradle.kts` 等 |
| `ValueInput/Output`＋Codec 永続化 | ✅ 採用済 | 旧 `getOrCreateTag` 等は不使用 |
| Codec（モード軸 `MaidMode`/`ModeManager`） | ✅ 採用済 | ADR-0002（AI-2） |
| Brain AI（移動軸・戦闘） | ✅ 採用済 | 移動 3 モード Brain 化＋`CombatMode` 統合（AI-3/4） |
| Brain AI（作業・補助 Goal） | ⚠️ 未移行 | → **WS1** |
| Codec（MaidSoul）/ カスタム Data Components | ⚠️ 未 | → **WS2** |
| DataGen（model/blockstate） | ❌ 未 | → **WS3**（loot/tag/recipe/advancement/lang は採用済） |
| Brigadier コマンド | ❌ 未 | → **WS4** |
| DataFixerUpper | ❌ 未 | → **WS5** |
| 描画 Blaze3D 本体移行 | 🛡️ 見送り決定 | ADR-0001。P-1〜P-6 最適化のみ実施 |
| Forge Energy | ⛔ 非該当 | 電力概念なし |
