# LittleMaidNeo リファクタリング実装 HOWTO（Phase 4〜10）

> 対象読者: 本リポジトリの Phase 4〜10 を実装する開発者／AI。
> 前提: **Java 25 が使え、`./gradlew build` / `runClient` / `runServer` / `runData` / `runGameTestServer` が実行できる環境**。
> 既完了: Phase 0〜3 部分（ブランチ `claude/todo-refactoring-plan-C2L1d` / PR #7）。本書はその続き。
> 全体プラン: `docs/plan/2026-06-01_統合リファクタリングプラン.md`、技術背景: `TODO_System.md`、バックログ: `TODO.md`。

---

## 0. 共通ルール（全 Phase 厳守）

### 0.1 絶対不変の「保護コア」
壊すと既存ユーザー資産（外部パック・セーブ）が死ぬ。**シグネチャ・データ形式・命名規則・探索パスを変えない。**

- **A. 外部モデル/テクスチャ読み込み・描画**
  - `resource/classloader/MultiModelClassLoader`・`MultiModelClassTransformer`（ASM リマップ表は `CODE_REPLACE_MAP` ＝ `MultiModelClassTransformer.java` L21〜216）
  - リマップ先スタブ `entity/EntityLittleMaid`、リマップ先パッケージ `work.nemonet.littlemaidneo.maidmodel`
  - `maidmodel/` 全般（`ModelMultiBase`/`EntityCaps`/`ModelRenderer`/`IModelCaps`）、`multimodel/layer/`（`MMMatrixStack`/`MMVertexConsumer`/`MMPose`/`MMRenderContext`）、`multimodel/IMultiModel`
  - 2 保証: ①メイドさんが正常描画される（SR2/AC/RX0/Steve・防具・手持ち・頭部装飾）②既存外部 `.class` パックが無改変でロード＆描画できる
- **B. 外部ボイスパック読み込み・再生**
  - 探索パス `LMMLResources/`、`.cfg` パース（`key=value` 例 `se_hurt=pack.parent.file`）、`.ogg` 探索
  - `resource/util/LMSounds` の**定数文字列**（`se_hurt`/`se_attack` 等＝エンティティ↔`.cfg` 契約）
  - `resource/util/ResourceHelper` の命名規則・`getLocation()` の Identifier 生成
  - パケット形式: `network/LMSoundPayload`・`SyncSoundPackPayload`・`SyncSoundConfigPayload` の codec
  - 音量: `config/LMMLConfig#getVoiceVolume()`（キー名・範囲不変）

### 0.2 検証（コミット前に必ず）
1. `./gradlew compileJava` — 全変更で必須
2. `./gradlew build` — フェーズ区切り
3. 保護コア A に触れたら: `./gradlew runClient` で `LMMLResources/` に**実在の外部モデルパック(.class)**を置き描画確認
4. 保護コア B に触れたら: 外部ボイスパック(.cfg+.ogg)再生確認
5. セーブ互換に触れたら（Phase 6/7）: **既存ワールドをロードして NBT エラーが出ないこと**＋`runServer` でマルチ接続・ディメンション移動引き継ぎ確認
6. DataGen（Phase 5）: `./gradlew runData` 後、`src/generated/resources/` が既存手動 JSON と**差分ゼロ**であること
7. `./gradlew runGameTestServer`（namespace `littlemaidneo`）で回帰確認

### 0.3 既存実装の事実（重要・推測しないこと）
- `LittleMaidEntity`（`entity/LittleMaidEntity.java`）の委譲先は **`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MultiModelCompound`/`SoundPlayableCompound`/`TargetTagManagerImpl`**。
- NBT 入出力は **`ValueOutput`/`ValueInput`＋Codec**（旧 `CompoundTag` 直書きは原則不使用）。例: `output.putByte/putInt/putBoolean/putString/store(key,codec,v)`、`input.getXxxOr(key,default)/read(key,codec)/child(key)/childrenList(key)`。
- `addAdditionalSaveData`＝`LittleMaidEntity.java` L546〜568、`readAdditionalSaveData`＝L571〜617。
- パケットは全 15 種が `StreamCodec`（`network/`）。登録は `network/NetworkHandler.register(RegisterPayloadHandlersEvent)`。
- `DeferredRegister` は **`setup/ModRegistration.java`** に集約、`LittleMaidNeo` コンストラクタで `register(modEventBus)`。
- Config 登録は `LittleMaidNeo` コンストラクタ L59〜60（`ModConfig.Type.COMMON`×2）。
- **`build.gradle.kts` の `data` run config（L105〜113）は既に存在**。＝`runData` 自体は配線済みで、**`GatherDataEvent` リスナーを Java 側に追加するだけ**で DataGen が動く。
- API 調査は必ず実機 jar/`mc-api-research` 相当で確認。NeoForge/Mojang マッピングのメソッド名はバージョンで変わる。ドキュメントは Context7 MCP 優先。

### 0.4 推奨実施順（依存関係）
**Phase 4 → 5 → 6 → 7 → 8 → 9 → 10**。
Phase 4（登録基盤）は 7 の前提、Phase 5（DataGen/タグ生成）は 6 の R-15 の前提。各フェーズ完了＝1 コミット。CLAUDE.md の該当節も同コミットで更新（R-17 横断）。

---

## Phase 4 — 基盤登録の追加（Memory/Sensor/Entity タグ）

**目的**: Phase 7(Brain)・Phase 6(R-15) の前提となる登録物を 26.1 ライフサイクルで定義する。

**影響ファイル**: `setup/ModRegistration.java`（追記）、新規 `tags/LMEntityTags.java`、`LittleMaidNeo`（register 呼び出し確認）。

### 手順
1. **Entity タグ（R-15 用）** — 新規 `tags/LMEntityTags.java`。`LMTags`（`tags/LMTags.java`）の `register` パターンに倣う:
   ```java
   public final class LMEntityTags {
       public static final TagKey<EntityType<?>> ATTACK_PROHIBITED        = register("attack_prohibited");
       public static final TagKey<EntityType<?>> APPROACH_PROHIBITED      = register("approach_prohibited");
       public static final TagKey<EntityType<?>> PREEMPTIVE_ATTACK_PROHIBITED = register("preemptive_attack_prohibited");
       public static final TagKey<EntityType<?>> RANGED_WEAPON_PROHIBITED = register("ranged_weapon_prohibited");
       public static final TagKey<EntityType<?>> MELEE_WEAPON_PROHIBITED  = register("melee_weapon_prohibited");
       private static TagKey<EntityType<?>> register(String id) {
           return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, id));
       }
   }
   ```
   ※この時点ではタグの中身（JSON）は Phase 5 で生成。タグ定義だけ先に置く。
2. **MemoryModuleType / SensorType（R-10 用）** — Phase 7 で実際に使う分だけ `ModRegistration` に `DeferredRegister` を追加:
   ```java
   public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULES =
       DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, MODID);
   public static final DeferredRegister<SensorType<?>> SENSORS =
       DeferredRegister.create(Registries.SENSOR_TYPE, MODID);
   ```
   - `MemoryModuleType` は `MEMORY_MODULES.register("xxx", () -> new MemoryModuleType<>(Optional.empty()))`。永続化する Memory は `Optional.of(codec)` を渡す。
   - `SensorType` は `SENSORS.register("xxx", () -> new SensorType<>(MySensor::new))`。
   - **`LittleMaidNeo` コンストラクタの既存 `ModRegistration.register(modEventBus)` で両 DeferredRegister も `register(modEventBus)` 呼ぶこと**（漏れると登録されない）。
   - ※Phase 7 を始めるまで具体的な Memory/Sensor が決まらないなら、**Phase 4 ではタグ定義のみ**に留め、Memory/Sensor は Phase 7 と同時に追加してよい（空登録はデッドコードになるため）。

**検証**: `./gradlew runClient` 起動でレジストリ登録エラーが出ないこと（ログの `REGISTRIES` マーカー確認）。

---

## Phase 5 — DataGen 導入（R-13）

**目的**: 手動 JSON を Java から自動生成し、保守を一本化。**生成結果が既存 JSON と一致**することが受け入れ条件。

**現状の手動 JSON（`src/main/resources/`配下・全て生成対象）**:
- loot: `data/littlemaidneo/loot_tables/`（`salary_box`・`little_maid_mob` 計2）
- tags: `data/littlemaidneo/tags/`（items 7＝モード6+`maids_employable`等、blocks 1、biomes 2、neoforge/biome_modifier 1 等 計11前後）
- recipes: `data/littlemaidneo/recipes/`（`little_maid_spawn_egg`・`salary_box` 計2）
- advancements: `data/littlemaidneo/advancements/`（`contract_maid`・`resurrect_maid` 等 計4前後）
- lang: `assets/littlemaidneo/lang/`（`en_us`・`ja_jp`）
- biome modifier: `data/littlemaidneo/neoforge/biome_modifier/maid_spawn.json`

**影響ファイル**: 新規 `data/`（DataGen プロバイダ群、例 `data/LMDataGenerator.java` ＋ 各 Provider）、`LittleMaidNeoClient` か専用クラスで `GatherDataEvent` を購読。

### 手順
1. `GatherDataEvent`（NeoForge）リスナーを mod イベントバスに登録。`event.getGenerator()`・`event.getPackOutput()`・`event.getLookupProvider()`・`event.getExistingFileHelper()` を使う。
2. 各 Provider を `generator.addProvider(event.includeServer()/includeClient(), provider)` で登録:
   - **`LanguageProvider`**（client）: 既存 `en_us.json`/`ja_jp.json` の全キーを `add(key, value)` で再現。**モード名キー `mode.littlemaidneo.{Name}` を漏らさない**（現在: Fencer/Archer/Cooking/Pharmcist/Ripper/Torcher/Healer）。
   - **`ItemTagsProvider` / `TagsProvider<Item>`**: モード用タグ `{mode}_mode`（例 `cooking_mode`=`minecraft:bowl`、`pharmcist_mode`=`minecraft:glass_bottle`）と `maids_employable`/`maids_salary`。**`api/mode/Modes.ENTRIES` と整合**させ、生成元を Java に一本化すると R-6 と相性良い。
   - **`TagsProvider<EntityType<?>>`**: **R-15 の肝**。Phase 4 の `LMEntityTags` を、現行 `TargetTagManagerImpl`（後述 §Phase6-R15）の `instanceof` 判定と**等価**になるよう列挙。バニラタグ（`minecraft:undead` 等）も活用可。
   - **`LootTableProvider`**: `salary_box`・`little_maid_mob` を再現。
   - **`RecipeProvider`**: `little_maid_spawn_egg`・`salary_box`。
   - **`AdvancementProvider`**: 既存 advancement を再現。
   - **`BiomeModifierProvider`（`DatapackBuiltinEntriesProvider` 経由）**: `maid_spawn.json` を Java 化。スポーン条件自体は `RegisterSpawnPlacementsEvent`（`setup/ModSetup`）側の既存登録を踏襲。
3. `ICondition`（任意）: 他 Mod 連携時のみ有効なドロップ/レシピを条件付き生成。
4. 生成後、**既存手動 JSON を削除**するのは「`runData` 出力と diff ゼロ」を確認してから。出力先は `src/generated/resources/`（`build.gradle.kts` の `data` run config L112 で設定済み）。

**保護コア**: モデル/ボイスの探索・命名に JSON は絡まないので影響なし。ただし `tags/items/{mode}_mode.json` の中身を変えるとモード判定が変わるので**現在値を厳守**。

**検証**: `./gradlew runData` → `git diff src/generated/` と既存 `src/main/resources/` の該当 JSON を比較し**意味的に一致**。`runClient` でモードトリガー・スポーン・レシピが従来通り。

---

## Phase 6 — 状態管理の現代化（R-2 / R-14 / R-15 / R-16）

> ⚠️ **本フェーズはセーブ互換・マルチ同期が核心**。CI ビルドでは回帰を検出できない。
> **既存ワールドのロード**と **runServer マルチ接続**で必ず検証。必要なら旧 NBT からのマイグレーションを実装。

### R-15: ターゲティングの `instanceof` → Entity タグ駆動
**現状（`entity/targeting/TargetTagManagerImpl.java`）**: `init()`（L49〜112）が全 EntityType を生成し `instanceof` で判定（**L56〜100 に下記ルール**）。これを `entity.getType().is(tag)` 判定へ置換する。**判定結果を等価に保つ**こと。

現行ルール（必ず再現）:
- `!(e instanceof Enemy) || Piglin || ZombifiedPiglin || EnderMan` → `PREEMPTIVE_ATTACK_PROHIBITED`
- `Creeper || Warden` → `APPROACH_PROHIBITED` ＋ `MELEE_WEAPON_PROHIBITED`
- `EnderMan` → `RANGED_WEAPON_PROHIBITED`
- `TamableAnimal || Npc || Merchant || ArmorStand` → `ATTACK_PROHIBITED` ＋ `PREEMPTIVE_ATTACK_PROHIBITED`
- `PREEMPTIVE_ATTACK_PROHIBITED かつ !(Enemy)` → `ATTACK_PROHIBITED`
- `Cow||Chicken||Sheep||Pig||PolarBear||Rabbit` → `ATTACK_PROHIBITED` 除去
- `Warden` → `ATTACK_PROHIBITED` 追加

**注意**: `instanceof` はクラス階層で**任意 Mod のモブも自動分類**する（例 `Enemy` 実装は全部敵）。タグ JSON は静的列挙なので**バニラ＋既知 Mod しかカバーできない**。等価性を保つには、(a) タグに**バニラの該当 EntityType を網羅列挙**（`#minecraft:` 既存タグ活用）し、(b) **タグ未登録のモブは従来 `instanceof` でフォールバック**するハイブリッドが安全。完全置換は modded entity の挙動を変えるため非推奨。
- 実装: `getTargetTag(id)` で「タグ判定 → 無ければ `instanceof` フォールバック」。`init()` の重い全 EntityType 走査は撤廃可。
- 既存セーブ互換: ユーザーが GUI で個別設定した `targetTagMap`（NBT キー `targetTagMap`、`id`/`tags(byte ordinal list)`、`TargetTagManagerImpl.read/write` 参照）は**そのまま読めること**。`TargetingSystem.TargetTag` の **enum ordinal を変えない**（NBT は ordinal で保存）。

### R-2 + R-14: Mixin+interface+Impl → Data Attachment
**現状**:
- `MixinPlayerEntity`（→ `TargetTagManager`、impl 生成 L47）、`MixinServerPlayerEntity`（→ `MaidManager`、impl 生成 L43）。
- `instanceof MaidManager` / `instanceof TargetTagManager` キャストが **`NetworkHandler`・`MaidSoulEntity`・`LittleMaidEntity`** にある（grep して全置換）。
- `MaidManager.LMInfo`（sealed・`entity/util/MaidManager.java`）NBT キー: **`name`/`status`/`id`/`lastPos`/`worldId`/`entityId`/`soul`**（不変）。

**手順**:
1. `AttachmentType<T>` を `DeferredRegister`（`Registries.ATTACHMENT_TYPE`）で定義。状態型は **record＋Codec**（自動 NBT セーブ）。`AttachmentType.builder(...).serialize(codec).copyOnDeath()`（魂引き継ぎ＝R-3 連動）等。
   - `MaidManagerState`（旧 `MaidManagerImpl` の `maidMap`）と `TargetTagState`（旧 `targetTagMap`）を Attachment 化。
2. `((MaidManager) player)` → `player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT)` に置換。同期が必要な Attachment は `.sync()` 系設定 or 既存パケット（`SyncSoundConfigPayload` 等の隣にある同期 payload）を流用。
3. `Mixin`（`MixinPlayerEntity`/`MixinServerPlayerEntity`）と interface（`MaidManager`/`TargetTagManager`）を撤廃。**`littlemaidneo.mixins.json` から両 Mixin の登録も削除**（孤立 Mixin はデッドコード化する）。
4. ディメンション移動引き継ぎ: 旧 `MixinServerPlayerEntity#restoreFrom` 相当の挙動を Attachment の `copyOnDeath`/`copyOnRespawn` で再現。`LittleMaidEntity#restoreFrom`（L956 付近）の freedomPos クリアは維持。
5. **`HasMode`/`HasModeImpl`（Mixin 非依存）**: `instanceof HasMode` 未使用・`MixinLivingEntity` 無し。interface を畳んで具象（`HasModeImpl`）へ統合し記述量削減可。
6. `addAdditionalSaveData`（L546〜568）の per-field 手動 put/get を **record+Codec に集約**（手動 NBT の温床排除）。
   - 状態 2 レイヤー分離: 永続データ→Data Attachment+Codec、描画/モーション最小ステート→`SynchedEntityData`（既存 `LMM_FLAGS`/`MOVING_MODE` 等は維持）。

**マイグレーション**: 旧 NBT（プレイヤーの Mixin が書いていた `maidList`/`targetTagMap`）→ 新 Attachment への一回限り変換を読み込み時に実装（旧キーがあれば Attachment に移し替え）。

### R-16: サーバーコンフィグ自動同期（`ModConfig.Type.SERVER`）
**現状**: `LMRBConfig`/`LMMLConfig` とも `ModConfig.Type.COMMON`（`LittleMaidNeo` L59〜60）。
- サーバー権威設定（索敵範囲・攻撃力・各機能 ON/OFF＝`LMRBConfig` の Spawn/Health/Movement/Work/Contract/Target 等）→ `Type.SERVER` へ。接続時にクライアントへ自動同期。
- クライアント専用（音量 `LMMLConfig#getVoiceVolume`＝**保護コア B・キー名/範囲不変**、`LMRBConfig.Client`）→ `Type.CLIENT`。
- ⚠️ TOML ファイル構成が変わる（`littlemaidneo-common.toml` → server/client 分割）。既存ユーザー設定の移行に配慮（ファイル名変更の周知 or 旧ファイル読み替え）。
- **バックログ「鯖蔵コンフィグの同期（手動コピー不要）」はここで達成**。
- 消費側は `LMRBConfig.get()`/`TargetingConfig.getXxx()` 経由参照を維持（ゲッターの中身だけ差し替え）。

**検証**: 既存ワールドロード（NBT エラー無し・ターゲティング個別設定が残る）、`runServer` でマルチ接続しメイドさん同期・config 自動同期・ディメンション移動引き継ぎ。

---

## Phase 7 — AI を Goal 型 → Brain（BehaviorControl）型へ（R-10）

> 【大規模・段階移行】挙動の等価性を runClient で確認しながら**機能単位**で移す。一気にやらない。

**現状**: `GoalSelector` ベース。`LittleMaidEntity#registerGoals()`（L420〜525）で多数の Goal 登録（`entity/goal/`）。モードは `entity/mode/ModeWrapperGoal` で Goal ラップ。ターゲティングは `entity/targeting/`。
- ⚠️ `registerGoals()` は**匿名内部クラス＋`super.canUse()`** を多用（`LMMoveToDropItemGoal`・`PanicGoal`・`WaterAvoidingRandomStrollGoal` 等を匿名サブクラス化）。Goal をそのまま外部委譲はできない。Brain 化＝Behavior へ**書き換え**が必要。

**手順（推奨順）**:
1. **基盤**（Phase 4）: `MemoryModuleType`/`SensorType`/`Activity` を定義。`Activity`＝`IDLE`/`FOLLOW`/`FIGHT`/`WORK`。
2. `LittleMaidEntity` を Brain 化: `brainProvider()` と `makeBrain(Dynamic)` を override。`Brain.provider(MEMORY_TYPES, SENSOR_TYPES)`。`Mob#registerGoals` は最小化し、`customServerAiStep()` で `brain.tick()`。
3. **Sensor 自作**: 周囲のアイテム/敵/主人/地形をスキャンしメモリへ書く（バニラ `NearestLivingEntitySensor`/`NearestItemSensor` を参考）。
4. **Behavior 移植**（機能単位・1つ移すたび runClient 等価確認）:
   - 追従（`HasMMFollowTameOwnerGoal`）→ `FOLLOW` Activity の Behavior
   - 戦闘（`LMTargetGoal`・`PredicateRevengeGoal`・モードの戦闘）→ `FIGHT`
   - サルベージ/給料回収/しまう（`LMMoveToDropItemGoal`/`LMCollectSalaryFromContainerGoal`/`LMStoreItemToContainerGoal`）→ `WORK`
   - 待機/注視（`WaitGoal`/`WaitWhenOpenGUIGoal`/`LMStareAtHeldItemGoal`/`FollowAtHeldItemGoal`）
   - 赤石トレース（`RedstoneTraceGoal`）、自由行動（`FreedomGoal`）、雪遊び（`PlaySnowGoal`）
   - **モード（`HasMode`/`entity/mode/`）→ `WORK` 系 Activity の Behavior にマッピングし `ModeWrapperGoal` を置換**（R-7 と整合）。
5. 記憶の揮発・Activity 切替はバニラ `Brain` に委譲。Mod 側は純粋な Behavior ロジックに集中。
6. **応用（データ駆動 AI・任意）**: 性格プロファイル（臆病/好戦的＝重み）を `Codec` で JSON 定義、生成時に読み込み Brain の優先度/重みに反映（Phase 5 の DataGen と連携）。
7. **R-11 と統合**（Phase 8）: 首/視線は `LookAtTargetSink` 相当 Behavior ＋ `LookControl` で制御。

**セーブ互換**: Brain の Memory 永続化と既存セーブの読み込み互換に注意。永続 Memory はマイグレーション要。`isFriend()`（`LittleMaidEntity` L2225 付近）に残る「TargetingSystem にフレンドタグ復活」課題もここで対応。

**調査**: `mc-api-research` で `Villager`/`Piglin`/`Axolotl` の Brain/Behavior 実装を参照。

**検証**: 各 Behavior 移植ごとに runClient で**旧 Goal 挙動と等価**か確認。`runGameTestServer` で回帰。マルチでの同期。

---

## Phase 8 — 首/視線制御を `LookControl` で統一（R-11）

**目的**: 頭部向きを `Mob#getLookControl()` 経由に寄せ、重複・不整合を解消。

**現状の頭部向き制御**（洗い出して統一）:
- 注視 Goal `LMStareAtHeldItemGoal`（`LittleMaidEntity` 内部クラス付近）
- begging の `tickInterestedAngle()`/`getInterestedAngle()`（`LittleMaidEntity` L2200 前後・クライアント側）
- 各モードの `getLookControl().setLookAt(...)`（例 `CookingMode.tick` L244、`PharmcistMode.tick` L93）

**手順**:
1. まず**エンティティ側の向き値（`yHeadRot` 等）**の制御に寄せる（低リスク）。`RedstoneTraceGoal.tick`（首振り制限 L101〜110）の手書きクランプも `LookControl` 連携に整合させる。
2. Phase 7 と整合: Brain では `LookAtTargetSink` 相当 Behavior ＋ `LookControl`。
3. 視線判定 Raytrace 同期: `getDefaultDimensions(Pose)`（L944 付近）が返す eyeHeight/ポーズを首振り/視線と一致させる（Pose 対応の `getDefaultDimensions` は実装済み）。**サイズ算出は `maidmodel` 寸法依存＝読み取りのみ・保護コア A 不変**。
4. ソース TODO: マウント位置調整（`getMountedYOffset`/`getRidingYOffset` 付近、旧 L1064）もここで。

**検証**: runClient で首振り・視線追従が自然か、Raytrace（プレイヤーがメイドを見る/メイドが対象を見る）が一致するか。描画に手を入れたら保護コア A の 2 保証を実機確認。

---

## Phase 9 — 描画ラッパーのモダン化 ＋ GeckoLib（R-12）

> 【最高リスク・保護コア A】**PoC 必須**。実在の外部パック数種で `runClient` 描画一致を確認してから本適用。
> ⚠️ `maidmodel/`・`EntityCaps`・`IModelCaps`・`ModelRenderer` の**シグネチャ変更は `LinkageError`/`AbstractMethodError` に直結**（外部 `.class` パックが override しているため）。ブリッジ無しの変更は禁止。

**目的**: 独自描画ラッパー（`MMMatrixStack`/`MMVertexConsumer`/`MMPose`/`MMRenderContext`）をバニラ標準型（`PoseStack`/`VertexConsumer`/`MultiBufferSource`）へ寄せて簡素化。

**互換維持の方針（どちらか）**:
- **(a) Transformer ブリッジ**: `MultiModelClassTransformer`（`CODE_REPLACE_MAP` L21〜216、GL11→GLCompat リダイレクト L88〜124）側で**旧シグネチャ→新標準型へのアダプタを生成/注入**し、既存パックを無改変ロード。
- **(b) アダプタ保持**: 旧ラッパー型を「標準型への薄いアダプタ」として**残し**、内部実装だけ標準型に委譲。

**GeckoLib/AzureLib**:
- メイドさん本体へ導入する場合、**既存外部パックが無改変でロード＆描画できる経路を別途維持**することが条件。両立困難なら**新規補助エンティティ限定**。
- フル再構築（`EntityRenderer`/`LayerDefinition`/`ModelPart`/`AgeableListModel`）は、**外部パック用に旧描画パスを並行維持する前提でのみ可**。バニラ `ModelPart` へ完全移行すると `maidmodel/` 独自ジオメトリと非互換になり保護機能①が壊れる。

**背景**: 1.20.1→1.21 で `RenderType`/`VertexFormat` 破壊的変更。旧 GL 直叩きは不可（現在 `GLCompat`＋`multimodel/layer` が現行パイプラインへブリッジ）。`MultiModelRenderLayer`（`RenderType` 継承）、発光シェーダー `lmml_emissive`（`RegisterShadersEvent`、`assets/minecraft/shaders/core/`）に注意。

**検証**: PoC →（実在 SR2/AC/RX0/Steve＋外部 .class パック数種）→ `runClient` で**描画完全一致**を目視。割れない確証が持てない変更は行わない。

---

## Phase 10 — 検証チェックリスト消化 ＋ 仕上げ（R-17 完了）

**目的**: `TODO.md` の実機検証チェックリスト全消化、文書最終整合、残バックログ処理。

**手順**:
1. `runClient`/`runServer` で `TODO.md`「実機検証チェックリスト」を全消化（GUI 表示・各ボタン・モデル/防具/頭部装飾描画・config 競合なし生成・マウスクリック判定・既存セーブ NBT ロード・外部 .class ASM ロード・マルチ同期・クロスボウ＝`MixinCrossBowItem`）。
2. 残バックログ実装 or `TODO.md` 明示再掲:
   - 中: ModelCaps 未実装箇所、LivingVoiceRate、経験値瓶ガラス問題、潜水/好感度/グループ分け（Phase 6 基盤＋ Phase 7 AI）
   - 低: 利き手設定/本一括設定/体力増加/成長/農業モード、Ripper 隠し機能/糸/ポーション付与/TNT/弓と火打ち石
   - 連続発声問題（射手・明かりモード重複発声）: `ArcherMode`/`TorcherMode`/ボイス再生経路
3. **R-17（CLAUDE.md 全面更新）**:
   - 「`LittleMaidEntity` は LMGoalInitializer/… 分割」記述を実態へ（実在は `MaidResurrection`/`BookParameterParser`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl` 等）。
   - 「現状維持境界」前提の旧記述を「保護2機能以外は解禁」方針へ更新。
   - 現代化採用状況（Data Attachment/DataGen/Brain）を反映。
4. **対象外（別途）**: モード状態 NBT 未永続化（Archer/Fencer cooldown・Healer index）はリロード挙動が変わるため本リファクタと分離。

---

## 付録: 既知のソース TODO 対応表（残）

| 箇所 | 内容 | 担当 Phase |
|---|---|---|
| `LittleMaidEntity` L215 | client 側 accelerationTicks 不信頼（同期方針） | 6 |
| `LittleMaidEntity` L244 | 付与属性の再考 | 6/10 |
| `LittleMaidEntity` 旧 L256/L1043 | スポーン条件コンフィグ化 | 6 |
| `LittleMaidEntity` 旧 L1109 | ボイス調整・コンフィグ化 | 6 |
| `LittleMaidEntity` 旧 L1218/1257/1359 | 処理改善・try/catch | 2/6 |
| `LittleMaidEntity` 旧 L1366 | Infinity 判定を `Holder<Enchantment>` で | 2 |
| `LittleMaidEntity` 旧 L1420 | クロスボウ弾道調整 | 2 |
| `LittleMaidEntity` 旧 L1636/1637 | mobInteract 整理・使用アイテム config 化 | 2/6 |
| `LittleMaidEntity` 旧 L1992/2050/2116 | hurtArmor 計算/getProjectile/IdFactor 仕様 | 2 |
| `LittleMaidEntity` L2225 付近 `isFriend` | TargetingSystem フレンドタグ復活 | 6/7 |
| `LittleMaidEntity` 旧 L2500/2501 | 強制再生メソッド・クールダウン config 化 | 6 |
| `ItemContractable` L15 | client 側活用方針確定 | 6 |
| `LittleMaidScreen` L245 | 取得ずれ防止 | 3/10 |

> ※行番号は Phase 2-3 の編集で前後にずれている。`grep -rn "TODO" src/main/java` で都度確認すること。
