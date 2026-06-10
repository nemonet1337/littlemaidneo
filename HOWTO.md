# LittleMaidNeo 実装 HOWTO（モダン化リファクタリング）

> 対象読者: 本リポジトリを実装する開発者／AI。
> 前提環境: **Java 25**。`./gradlew build` / `runClient` / `runServer` / `runData` / `runGameTestServer`。
>   ※ ローカルに JDK25/foojay 解決が無い環境では、検証は **push 後の CI（Java 25）** が唯一の手段（ADR-0002 と同方針）。
> 関連ドキュメント: 全体プラン `docs/plan/2026-06-01_統合リファクタリングプラン.md`、設計判断 `docs/adr/`、バックログ `TODO.md`。
> 本書はモダン化リファクタ（Brain AI 化・Codec 化・DataGen・Brigadier・DataFixer）**完了後**の **内部整理リファクタ**（デッドコード削除 / Mixin 整理 / 共通化 / 巨大クラス分割）の実装ガイド。旧モダン化ワークストリーム WS1〜5 は完了済みのため本書からは除外し、後続の §A〜§F に差し替えた。

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
- **音量**: `config/LMNModelConfig#getVoiceVolume()`（キー名・範囲不変）。
  > 注: 上記クラスでも「外部契約に関わらない内部実装」は整理可。`SoundPlayable.play(String)` のシグネチャと `LMSounds.*` 文字列・`.cfg` キー解決の振る舞いは不変に保つこと。

### 0.4 既存実装の事実（推測しないこと）
- `LittleMaidEntity` の委譲先: `MaidResurrection`/`BookParameterParser`/`LMInteractionHandler`/`LMSafeMovement`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MultiModelCompound`/`SoundPlayableCompound`/`TargetTagManagerImpl`/`TargetingSystem`/`MaidLookControl`。
- NBT 入出力は **`ValueOutput`/`ValueInput`＋Codec**（旧 `CompoundTag` 直書きは原則不使用）。
- パケットは全 15 種が `StreamCodec`（`network/`）。登録は `network/NetworkHandler.register(RegisterPayloadHandlersEvent)`。
- `DeferredRegister` は **`setup/ModRegistration.java`** に集約、`LittleMaidNeo` コンストラクタで `register(modEventBus)`。
- **AI 現状（ADR-0002 / ADR-0003）**: 移動軸 `MaidMode`・補助行動は **Brain Behavior へ全面移行済み**（`entity/ai/behavior/Maid*Behavior` 13 種）。**全 Behavior は CORE Activity に一括登録**（FIGHT/WORK/IDLE への分割は未実装。ADR-0003 の Activity 体系化記述は将来像であり実装と差異あり＝§A 参照）。作業軸は**現状は `MaidWorkModeBehavior`→`Mode` 委譲の 2 層構造だが、§C で `Mode`・`ItemMatcher`・`ModeType`・`ModeManager`・`HasModeImpl` を完全廃止し各 Behavior が直接 AI＋アイテム識別を保持する形へ移行する**（`ModeWrapperGoal` は廃止済み・`entity/goal/` 削除済み）。現時点で `getMode()`/`hasModeImpl`/`ModeManager.CODEC` が caps_job/targeting/Codec に依存しているが、§C 完了後はすべて `ACTIVE_JOB_NAME`/`ACTIVE_BATTLE_MODE` メモリ＋`PersistentMaidBehavior` へ置き換わる。`registerGoals()` にはバニラ補助 Goal（`AvoidEntityGoal`/`PanicGoal`/`LookAtPlayerGoal`×2/`RandomLookAroundGoal`）が**残存**（→ §A で Brain へ全廃予定）。
- **カスタム criteria（`advancement/criterion/`）は廃止不可（調査済み）**: `ContractMaidCriterion`/`ResurrectMaidCriterion` の 2 件は契約・復活の進行条件として正常実装・動作中（`RegisterEvent` + `CriteriaTriggers.register` 方式・NeoForge 26.x 準拠・DataGen 連動）。残存理由は明確であり対応不要。
- **`network/` は NeoForge 現行 API 準拠済み**（廃止 API なし）。ただし entityId エンコーディング不統一・C2S 定型重複・手書き encode/decode 3 件の課題あり → §E-2 参照。
- API 調査は `mc-api-research` エージェント必須。NeoForge/Mojang マッピングのメソッド名はバージョンで変わる。ライブラリ Doc は Context7 MCP 優先。

### 0.5 検証（コミット前に必ず）
1. `./gradlew compileJava`（全変更）／`./gradlew build`（区切り、CI 相当）
2. 保護コア A に触れたら `runClient` で **実在の外部モデルパック(.class)** を `LMMLResources/` に置き描画確認
3. 保護コア B に触れたら 外部ボイスパック(.cfg+.ogg) 再生確認
4. セーブ互換に触れたら **既存ワールドをロードして NBT エラーが出ないこと** ＋ `runServer` でマルチ接続・ディメンション移動引き継ぎ
5. DataGen は `runData` 後 `src/generated/resources/` が既存 JSON と **差分ゼロ（意味的一致）**
6. `runGameTestServer`（namespace `littlemaidneo`）で回帰確認

### 0.6 推奨実施順（依存関係）
モダン化 WS1〜5（Brain AI / Codec / DataGen / Brigadier / DataFixer）は**完了済み**。本書は後続の内部整理 §A〜§G を扱う。推奨順は **§B（デッドコード削除・最低リスク）→ §G-1（単一クラスディレクトリ平坦化・同低リスク）→ §E（common 切り出し）→ §F（LittleMaidEntity 分割）→ §D（Mixin 整理）→ §C（Mode/ItemMatcher 廃止・最高リスク）→ §G-2（`api/` 廃止・§C と同時）→ §A（残存 Goal 全廃＋Activity 体系化）**。AI に深く関わる §C・§A は最高リスクのため最後に集中（§C は §E の `AbstractMaidBehavior` 基底に乗ると楽、§A-5 の Activity 体系化は §C と同時実施が綺麗）。各区切り＝1 コミット。重要な設計判断は `/doc` で `docs/adr/` に記録し CLAUDE.md の該当節も同コミットで更新する。

---

## §A — Goal の AI(Brain)化（残存バニラ Goal を全廃する）

**進捗と残件**: カスタム Goal は全廃済み（`entity/goal/`・`ModeWrapperGoal` 削除・参照ゼロ、`Float`→`Swim`/`OpenDoor`→`InteractWithDoor` も Behavior 化済み）。**ただし `registerGoals()` にバニラ補助 Goal が残存している**。本節のゴールは**これらも Brain Behavior へ移し、`registerGoals()` を空（または撤去）にする**こと。

**置換対象（すべて Brain へ）**:
- `LittleMaidEntity#registerGoals()`（`LittleMaidEntity.java:314-365`）: `LookAtPlayerGoal`×2（Player 優先確率0.8 ＋ 全 LivingEntity）/ `RandomLookAroundGoal`（視線）、`PanicGoal`（**未テイム時のみ**）、`AvoidEntityGoal`（`fleeEntities` 登録 Mob からの退避）。
- `MultiModelEntity#registerGoals()`（`MultiModelEntity.java:45-49`）: `FloatGoal` + `LookAtPlayerGoal`×2。

### A-1. 視線（LookAtPlayer / RandomLookAround）→ Brain（容易）

バニラの `SetEntityLookTarget`（プロデューサ：`LOOK_TARGET` メモリへ対象をセット）＋ `LookAtTargetSink`（コンシューマ：`LOOK_TARGET` を消費し頭/体を向ける）で置換。`RandomLookAround` 相当は `SetEntityLookTarget` の確率版（`SetEntityLookTargetSometimes` 等）か軽量カスタムで代替。
- **過去の判断を覆す**: 以前は「`LOOK_TARGET` を設定するプロデューサが居らず `LookAtTargetSink` が常に no-op」のため未登録にしていた（`LittleMaidEntity.java:260-265` のコメント）。本作業はプロデューサ（`SetEntityLookTarget`）を追加して**これを解禁する**。`registerGoals()` の視線 3 件は削除し、当該コメントも更新。
- 手順: (1) `BRAIN_PROVIDER` に `MemoryModuleType.LOOK_TARGET` と `NEAREST_VISIBLE_PLAYER`（または `NEAREST_PLAYERS`/`NEAREST_LIVING_ENTITIES`）を追加 → (2) `LittleMaidSensor` か `NearestLivingEntitySensor`/`PlayerSensor` でメモリ充填 → (3) CORE/IDLE に `SetEntityLookTarget`（プレイヤー優先・旧確率0.8 を引数で再現）＋ランダム注視版＋`LookAtTargetSink` を追加 → (4) `registerGoals()` から視線 Goal 削除。
- **注意（競合）**: 既存 `MaidStareBehavior` は `getLookControl().setLookAt(...)` で直接頭を向ける（`LOOK_TARGET` 非依存）。注視の二重制御にならないか runClient で確認（最終的に `MaidLookControl` の角度クランプが効く）。

### A-2. パニック（PanicGoal・未テイム時のみ）→ Brain（中）

野良メイドさん（未テイム）が被弾時に逃げる挙動。Brain 版パニックが無ければ軽量カスタム `MaidPanicBehavior`（CORE）を新設。
- 開始条件: `!TameableUtil.hasTameOwner(entity)` ＋ 旧 `PanicGoal#canUse` 相当（直近被弾など）。**「未テイム時のみ」を厳守**（テイム済みの退避は A-3 の `fleeEntities` 系に委ねる。現状コードも `PanicGoal#canUse` に未テイム条件を AND している）。
- 動作: `DefaultRandomPos.getPosAway` 等で逃走先を求め `WALK_TARGET` を `config.movement.escapeSpeed` でセット（`MoveToTargetSink` が消費）→ `registerGoals()` から `PanicGoal` 削除。

### A-3. 退避（AvoidEntityGoal）→ Brain（**慎重に・最重要**）

> **これはバニラ汎用の退避ではない**。`fleeEntities`（`Map<Mob,Predicate<Mob>>`）に動的登録された Mob からのみ逃げる本 Mod 独自の退避。結合を厳密に保つこと。

現状の事実（`LittleMaidEntity.java:319-341`／`:193,1288`／`MaidTargetBehavior.java:55-62`）:
- `fleeEntities` は `MaidTargetBehavior` が「退避が必要」と判断した危険な敵（`TargetingSystem.getDangerousEnemies`）を**削除述語付き**で登録（述語＝「死亡」または「距離 > `dangerousAvoidDistance`+4」で除去）。
- `AvoidEntityGoal` は `entity -> fleeEntities.containsKey(entity)` で対象を選び、毎 tick 期限切れを除去、`stop()` で `navigation.stop()`。距離 `config.target.dangerousAvoidDistance`、歩行 `followSpeed`、疾走 `sprintSpeed`。

置換方針（`MaidAvoidBehavior`・CORE）:
1. **`fleeEntities` の毎 tick プルーニング（述語 true の除去）を維持**。保持場所は現状の `LittleMaidEntity` のままで可だが、§F の退避/戦闘コンポーネントへ移すと整理できる。
2. 開始条件: `fleeEntities` 内かつ `dangerousAvoidDistance` 以内に生存 Mob が居る。
3. 動作: 最近接の逃避対象から離れる安全位置（`DefaultRandomPos.getPosAway`/`LandRandomPos`）を求め `WALK_TARGET` を **sprintSpeed** でセット（`MoveToTargetSink` が消費）。終了時 `navigation.stop()` 相当。
4. `registerGoals()` から `AvoidEntityGoal` 削除。
- **検証必須**: 「強敵に囲まれ退避→距離が開けば退避解除→再ターゲット」の往復、および `MaidTargetBehavior`（攻撃）との優先・競合が旧 Goal と**同値**であることを runClient/GameTest で確認。

### A-4. MultiModelEntity の Goal

AI 不要なダミー表示エンティティ。`FloatGoal` のみ残すか `Swim` Behavior に揃えるかは任意（優先度低）。

### A-5. 仕上げ: Activity 体系化（ADR-0003 整合）

A-1〜A-3 で `registerGoals()` が空になったら、ADR-0003 が記す `CORE/FIGHT/WORK/IDLE` を実装し `customServerAiStep` で `setActiveActivityToFirstValid([FIGHT, WORK, IDLE])`。FIGHT=`MaidTargetBehavior`(+退避)、WORK=作業モード/治癒/給料/収納/搬送、IDLE=見回り/PlaySnow/Stare。これで「実装は CORE 一括」という ADR との差異も解消する（§C の個別モード Behavior と同時実施が綺麗）。

**検証**: 各ステップで `./gradlew build`（CI）＋ runClient（視線追従・野良パニック・強敵退避・追従/作業との両立）。保護コアには触れない。

---

## §B — デッドコード削除（最低リスク・即効）

**目的**: 参照ゼロ／二重定義の未使用コードを除去し、読みやすさと保守性を上げる。挙動は一切変えない。

**確実に削除可能（参照ゼロを監査で確認済み）**:
1. `setup/ClientSetup.java` — `init()` が空で呼び出しゼロ（`ModSetup.init()` とは別物。`LittleMaidNeoClient` の `onClientSetup`/`FMLClientSetupEvent` は無関係）。クラスごと削除。
2. `entity/util/MaidMode.fromName(String)` — 呼び出しゼロ。`byName`（`CODEC` で使用）の throwing 重複。メソッド削除。
3. `api/mode/ItemMatchers` — `item(Item)`（既 `@Deprecated`）/ `name(String)` と、それらだけが生成する private record `ItemInstance`/`NameMatcher` を削除（全て呼び出しゼロ）。`tag()`/`clazz()`/`item(Supplier)` と `TagMatcher`/`ClassMatcher` は使用中なので残す。
4. `api/mode/ModeType.Builder.addItemMatcher(ItemMatcher)` 単一引数版（既 `@Deprecated`）— 全 14 呼び出しが 2 引数 `(matcher, Priority)` 版。単一引数版を削除。
5. `mixin/MixinPlayerEntity` の空 `@Inject` 2 件（`<init>` / `stopSleepInBed`）— ボディ空のデッドコード（`defaultRequire:1` で無意味に注入点解決のコストだけ発生）。削除（詳細は §D）。

**要確認（疑い・削除前に call-site を再確認）**:
- `util/BlockFinder` の `findTarget`/`findHorizonPos`/`findLayer`/`findHorizon` — `searchTargetBlock`/`seedFill` のみ外部利用、上記は未使用の疑い（`findTarget` に `//多分動かん` コメント）。各 static メソッドの参照を `Grep` で確認してから削除する。

**任意・低優先（単一実装マーカー interface のインライン化）**: `entity/util/HasMaidMode` / `entity/util/Contractable` / `entity/mode/HasMode` / `entity/util/GuiEntitySupplier` は **polymorphic 利用がゼロ**（`instanceof`/キャスト/型としての引数・フィールド・ジェネリック境界いずれも無し）で、実装は実質 `LittleMaidEntity` 系のみ。薄い抽象なので、メソッドを具象へ取り込んで interface を畳むことが可能。ただし `HasInventory`（`T extends LivingEntity & HasInventory` のジェネリック境界）/ `SalaryBoxPosListener`（`instanceof`）/ `LMCollidable`（`instanceof`＋2 Mixin 実装）/ `ProcessDivider`（default メソッド利用）は **load-bearing なので残す**。

**手順**:
1. 各対象を `Grep`（クラス名／メソッド名）で全リポジトリ横断検索し、参照ゼロを再確認（コメント・docs・`mixins.json` も含む）。
2. 削除 → `./gradlew compileJava`（CI）。マーカー interface を畳む場合は、その interface を `implements` 句から外し、メソッドを具象側に移し、呼び出し側のキャスト/import を除去。
3. 旧 Goal 系・`ClientSetup` を指す stale ドキュメントを是正（`CLAUDE.md:16` の `entity/goal/ — AI Goal` ／ `CLAUDE.md` の `ClientSetup` 言及 ／ 本 HOWTO 旧記述）。加えて **CLAUDE.md Architecture Notes の実在しないクラス記述**（`BlockWorkMode`/`WorkStrategy<T>`/`BlockSearch`/`SearchCondition`/`BlockReservationManager`）を実体（`BlockFinder`/`BlockFinderPD`/`ProcessDivider`/`ModeHelpers`）へ是正（E-4 と同時）。

**検証**: `./gradlew build`（CI）。挙動不変なので実機検証は不要（保護コアに触れない）。

---

## §C — Mode/ItemMatcher 廃止：Behavior が直接 AI＋アイテム識別を保持する

**廃止対象（すべて削除）**: `api/mode/Mode.java`（抽象クラス＋6 サブクラス）/ `api/mode/ItemMatcher.java`・`ItemMatchers.java` / `api/mode/ModeType.java`・`ModeManager.java` / `entity/mode/HasModeImpl.java`・`HasMode.java` / `entity/ai/behavior/MaidWorkModeBehavior.java`。`entity/mode/ModeHelpers.java` は §E-4 の共通ヘルパ化後に判断。

**廃止後の代替設計（対応表）**:

| 廃止するもの | 代替 |
|---|---|
| `ItemMatcher` / `ItemMatcher.Priority` | 各 Behavior の `protected abstract boolean isMyItem(ItemStack)` ＋ Brain 登録優先度番号 |
| `HasModeImpl.tick()`（アイテム判定・インベントリ走査・装備） | 各 Behavior の `checkExtraStartConditions` に inline |
| `Mode.shouldExecute()/tick()/resetTask()` 等 | 各 Behavior の `checkExtraStartConditions()/tick()/stop()` へ直接移植 |
| `maid.getMode().map(Mode::getJobName)`（caps_job） | `brain.getMemory(ACTIVE_JOB_NAME)` |
| `maid.getMode().isPresent()`（caps_isWorking） | `brain.hasMemoryValue(ACTIVE_JOB_NAME)` |
| `mode.getBattleModeType()`（TargetingSystem） | `brain.getMemory(ACTIVE_BATTLE_MODE)` |
| `mode.isBattleMode()`（LittleMaidEntity:1009） | `brain.hasMemoryValue(ACTIVE_BATTLE_MODE)` |
| `mode.getModeType().isModeItem(stack)`（MaidStoreItemBehavior） | `workBehaviors.stream().anyMatch(b -> b.isMyItem(stack))` |
| `HasModeImpl.writeModeData/readModeData` | `PersistentMaidBehavior.saveBehaviorData/loadBehaviorData` |

---

### C-1. 設計確定・ADR 記録（実装前に完了させる）

以下を決定し `docs/adr/` に記録してから C-2 へ進む。

**新メモリ型**（`ModRegistration.MEMORY_MODULES` に追加）:
- `ACTIVE_JOB_NAME : MemoryModuleType<String>` — 作業 Behavior の `start()` で `setMemory`、`stop()` で `eraseMemory`。`caps_job`/`caps_isWorking` が読む。
- `ACTIVE_BATTLE_MODE : MemoryModuleType<BattleModeType>` — `MaidCombatBehavior` の `start()/tick()` で設定、`stop()` で削除。`TargetingSystem`・`LittleMaidEntity#hurtServer` が読む（旧 `getMode().getBattleModeType()` 相当）。`BattleModeType` enum は `MaidCombatBehavior` 内（または `entity/ai/` 直下）に移設。

**`AbstractMaidWorkBehavior`**（§E-5 の `AbstractMaidBehavior` を継承）:
```java
abstract class AbstractMaidWorkBehavior extends AbstractMaidBehavior {
    protected abstract boolean isMyItem(ItemStack stack);
    protected abstract String jobName(); // ACTIVE_JOB_NAME に書き込む値
    // checkExtraStartConditions: isStrike / isEmergency ガード + isMyItem(mainHand) or equipFromInventory
    // start: brain.setMemory(ACTIVE_JOB_NAME, jobName())
    // stop: brain.eraseMemory(ACTIVE_JOB_NAME)
}
```

**`PersistentMaidBehavior`**（interface）:
```java
interface PersistentMaidBehavior {
    void saveBehaviorData(ValueOutput output);
    void loadBehaviorData(ValueInput input);
}
```
`LittleMaidEntity` は `List<AbstractMaidWorkBehavior> workBehaviors`（BRAIN_PROVIDER 構築時に初期化）を保持し、`addAdditionalSaveData`/`readAdditionalSaveData` でイテレートして `PersistentMaidBehavior` を実装するものだけ call する。

**アイテム識別の優先度**: Brain 登録の整数優先度（高い方が先に `checkExtraStartConditions` を通る）で制御する。旧 `ItemMatcher.Priority.HIGHER/LOWER` に相当する優先度番号を各 Behavior に割り当てる。同一優先度のときは登録順。

---

### C-2. 個別 Behavior 新設（1 Behavior＝1 コミット）

`entity/ai/behavior/` に以下を新設。各 `Mode` サブクラスの実行ロジックをそのまま移植し、`isMyItem` と `jobName()` を実装する。

| Behavior | 旧 Mode | jobName | BattleMode | Activity |
|---|---|---|---|---|
| `MaidCookingBehavior` | `CookingMode` | `"cooking"` | NONE | WORK |
| `MaidHealerBehavior` | `HealerMode` | `"healer"` | NONE | WORK |
| `MaidPharmcistBehavior` | `PharmcistMode` | `"pharmcist"` | NONE | WORK |
| `MaidTorcherBehavior` | `TorcherMode` | `"torcher"` | NONE | WORK |
| `MaidRipperBehavior` | `RipperMode` | `"ripper"` | NONE | WORK |
| `MaidCombatBehavior` | `CombatMode` | `"fencer"`/`"archer"`（tick 毎更新） | SWORD/BOW | FIGHT |

**`MaidCombatBehavior` の caps_job 動的更新**: `tick()` 内で現在の武器種を判定し `brain.setMemory(ACTIVE_JOB_NAME, ...)` を毎 tick 更新。`ACTIVE_BATTLE_MODE` も同様に更新（旧 `CombatMode#getJobName()` の動的評価を引き継ぐ・外部パック互換を維持）。

**`isMyItem` の実装**: 旧 `Mode.java` 各 `buildXxxMode()` の `addItemMatcher(...)` 呼び出しを参照し、同じ述語を `isMyItem` に移植。タグ参照（`LMTags.Items.COOKING_MODE` 等）は変更なし。

**`checkExtraStartConditions` の装備ロジック**: 旧 `HasModeImpl.equipModeItemFromInventory()` の「インベントリを優先度順にスキャンしてメインハンドへ装備」処理を各 Behavior の `checkExtraStartConditions` に統合（優先度は登録順で担保されているため再スキャンは 1 Behavior 分だけ）。

---

### C-3. 旧システム全撤去

C-2 で全 Behavior が揃い CI が通ってから 1 コミットで実施。

1. **`entity/mode/`** のファイルを削除（`Mode`・6 サブクラス・`HasModeImpl`・`HasMode`）。`ModeHelpers` は §E-4 判断まで保留。
2. **`api/mode/`** の `ItemMatcher`・`ItemMatchers`・`ModeType`・`ModeManager` を削除（`Mode.java` ごと）。
3. **`MaidWorkModeBehavior`** を削除し `BRAIN_PROVIDER` の登録を個別 Behavior 群へ。
4. **`getMode()`・`writeModeData()`・`readModeData()`・`addMode()`・`addAllMode()`** を `LittleMaidEntity` から削除。`hasModeImpl` フィールドごと削除。
5. 各消費側の移行:
   - `LittleMaidModelCaps:50`（caps_job） → `brain.getMemory(ACTIVE_JOB_NAME).orElse(null)`
   - `LittleMaidModelCaps:34`（caps_isWorking） → `brain.hasMemoryValue(ACTIVE_JOB_NAME)`
   - `TargetingSystem:104`（getBattleModeType） → `brain.getMemory(ACTIVE_BATTLE_MODE).orElse(NONE)`
   - `LittleMaidEntity:1009`（isBattleMode） → `brain.hasMemoryValue(ACTIVE_BATTLE_MODE)`
   - `MaidStoreItemBehavior:103`（isExceptItem） → `workBehaviors.stream().anyMatch(b -> b.isMyItem(stack))`
6. `MixinRangedWeaponItem` が参照する `IRangedWeapon`（`Mode.java:157` 経由）は `MaidCombatBehavior.isMyItem` 内で直接 `instanceof ProjectileWeaponItem` 判定に移行（Mixin 自体は §D で KEEP）。
7. `§B` の削除候補「`ItemMatchers` deprecated / `ModeType.Builder` 単一引数版」は本 C-3 で全廃されるため §B からは除外してよい。

---

### 新しい作業モードを追加する手順（廃止後）

1. `entity/ai/behavior/` に `AbstractMaidWorkBehavior` を継承した `Maid<Name>Behavior` を作成。`isMyItem`・`jobName()`・`checkExtraStartConditions`（作業条件）・`tick()` を実装。必要なら `PersistentMaidBehavior` も実装。
2. `BRAIN_PROVIDER` に登録（適切な優先度番号・WORK Activity）。
3. アイテムタグ（`tags/items/{name}_mode.json`）・lang（`mode.littlemaidneo.{Name}`）を追加（DataGen）。

**検証**: C-2・C-3 それぞれで `./gradlew build`（CI）。C-3 完了後に `runClient`/GameTest で全モード動作・武器持替時の job 名切替・`caps_job` 描画・`caps_isWorking`・TargetingSystem 戦闘判定・MaidStoreItemBehavior のモードアイテム保護を確認。

---

## §D — Mixin の整理・脱 Mixin

**目的**: 9 ファイル（8 登録）の Mixin を「バイトコード必須＝KEEP」「NeoForge イベントで代替可＝撤去」「重複＝統合」に仕分けし、Mixin 面積を縮小する。Mixin を削除/追加したら必ず `src/main/resources/littlemaidneo.mixins.json` の登録も同時更新する（孤立 Mixin・未登録 Mixin はいずれも事故の元）。

**KEEP（バイトコード注入が必須・代替なし）**: 触らない。
- `MixinExperienceOrbEntity` / `MixinItemEntity` — バニラクラスに `LMCollidable` を実装＋`private` フィールド（`count`/`target`）を `@Shadow`。メイドさんの XP/アイテム拾い（mending 修理含む）。メイドさんはプレイヤーでないため pickup 系イベントが使えない。
- `MixinRangedWeaponItem` — `ProjectileWeaponItem` に `IRangedWeapon` を実装（`protected getDefaultProjectileRange()` を `@Shadow`）。`Mode.java:157` の `ItemMatchers.clazz(IRangedWeapon.class)` が弓/クロスボウを射撃モードへ自動分類する **load-bearing**。
- `MixinAbstractFurnaceBlockEntity#getRecipeType_LM` — `private final` ctor 引数 `recipeType` を `<init>` インジェクトで捕捉（getter 無し・代替なし）。`CookingMode` が使用。
- `MixinPlayerEntity` の `positionRider`/`onPassengerTurned`/`copyEntityData` override — メイドさんがプレイヤーに騎乗する際の位置計算。`super` 呼び出しを含む override は外部委譲不可（CLAUDE.md 方針）かつ `Player` サブクラス化が必須。

**撤去候補（NeoForge イベントで等価実装し、ファイルごと削除）**:
- `MixinServerPlayerEntity`（6 `@Inject`・状態フィールドなし）→ NeoForge イベント購読クラス（`@EventBusSubscriber`）へ移設:
  - `restoreFrom`（リスポーン/次元移動時の Attachment コピー）→ `PlayerEvent.Clone`。
  - `tick`（1/20 で `checkMaidUnload`）→ `PlayerTickEvent.Pre`。
  - `addAdditionalSaveData`（保存前 `checkMaidUnload`）→ セーブ系イベント（or Attachment シリアライズ）。
  - `startSleepInBed`/`stopSleepInBed`（就寝/起床ボイス）→ `PlayerSleepInBedEvent`/起床フック。
  - `readAdditionalSaveData`（旧 `maidList` NBT 移行）→ 移行が不要になれば削除、必要なら最小限の読込フックへ。
  - → これでファイルごと撤去できる見込み（状態は既に `MAID_MANAGER_ATTACHMENT`/`TARGET_TAG_ATTACHMENT` にあり、本 Mixin は orchestration のみ）。
- `MixinCandleCakeBlock`（`useItemOn` HEAD インジェクトで復活儀式）→ `UseItemOnBlockEvent`（ブロック右クリックの正準的フック・`cancelWithResult` で同等にキャンセル）。ファイルごと撤去。
  - **⚠️ 要確認（実装前にテストせよ）**: 監査ツールは HEAD インジェクトと `UseItemOnBlockEvent` のタイミングが一致しない可能性を指摘。代替前に GameTest で「着火→Maid 復活」の動作を確認し、イベントが同じタイミング・キャンセル効果を再現できることを実証してから撤去する。再現できない場合は KEEP 扱いとし §D から除外。

**統合・移設**:
- `MixinCrossBowItem`（`getInterval_LM` を override するだけ）→ `MixinRangedWeaponItem` に `instanceof CrossbowItem` 分岐として畳み、`MixinCrossBowItem` を削除（Mixin 1 件減）。`mixins.json` から登録も削除。
- `mixin/CrossbowItemInvoker` — **Mixin ではない**（`@Mixin`/`@Invoker` 注釈なし・`mixins.json` 未登録で正しい）。`CrossbowItem.getSpeed` が `private static` のためロジックを定数で再実装した普通のユーティリティ。`mixin/` パッケージにあるのが誤配置。`entity/util/`（例: `CrossbowSpeedUtil`）へ移設、または唯一の呼び出し元（`LittleMaidEntity.java:1118`）へ定数をインライン。

**任意（@Shadow 削減）**: `MixinAbstractFurnaceBlockEntity#isBurningFire_LM` は `litTimeRemaining>0` 相当。呼び出し側（`CookingMode`）でブロックステートの `AbstractFurnaceBlock.LIT` から導けば `@Shadow` を 1 つ減らせる。

**手順（撤去 1 件ごと）**: (1) 代替イベントハンドラを実装し挙動を移す → (2) 旧 Mixin を削除 → (3) `mixins.json` から登録名を削除 → (4) `./gradlew build`（CI）→ (5) 影響系の実機/GameTest 確認（復活儀式・騎乗・就寝ボイス・XP/アイテム拾い・射撃モード分類）。命名は `_LM` サフィックスへ統一済み。

---

## §E — 共通化（Mod 全体・`common/` 切り出し）

**方針**: エンティティに限らず Mod 全体で重複しているスキャフォールディングを共通化する。**「ドメイン横断の真に共通な部品は新規 `common/` パッケージへ、ドメイン固有の基底は各ドメインパッケージへ」** を原則とする（全部を `common/` に押し込む神パッケージは作らない）。共通化は**スキャフォールディング（足場）だけ**を抜き、ガード条件（所有者判定・`isStrike()`・各 Behavior 固有メモリ等）はラムダ/オーバーライドで**呼び出し側に残す**。優先度は監査の payoff 順（E-2 → E-3 → E-1 → E-4 → E-5）。

> 注: CLAUDE.md の Architecture Notes は **stale**。`BlockWorkMode`/`WorkStrategy<T>`/`BlockSearch`/`SearchCondition`/`BlockReservationManager` は**実在しない**（実体は `util/BlockFinder`〔同期 BFS〕・`util/BlockFinderPD`〔逐次 BFS・`ProcessDivider`〕・`entity/mode/ModeHelpers`・`CookingMode` 内の予約 Map のみ）。E-4 着手時に CLAUDE.md も是正する（§B のドキュメント是正に含む）。

### E-1. エンティティの multimodel/sound 委譲 → ホルダ interface（既知・中）

`LittleMaidEntity` と `MultiModelEntity` が **同一の委譲 ~16 メソッド**（`IHasMultiModel` 13 ＋ `SoundPlayable` 3 → `MultiModelCompound`/`SoundPlayableCompound`）を二重に持つ。親クラスが異なり（`TamableAnimal` vs `PathfinderMob`）基底クラス共有は不可・委譲先 `MultiModelCompound` 自身が実体なので、**default メソッド付きホルダ interface**で解消する。
- `common/MultiModelHolder`（`extends IHasMultiModel`、`MultiModelCompound getMultiModel();` ＋ 13 メソッドの default 委譲）/ `common/SoundHolder`（`extends SoundPlayable`、`SoundPlayableCompound getSoundCompound();` ＋ 3 メソッドの default 委譲）を新設。
- 両エンティティは `implements MultiModelHolder, SoundHolder` にし、本体メソッドを削除して `getMultiModel()`/`getSoundCompound()`（既存フィールドを返すだけ）のみ実装。
- `IHasMultiModel` の polymorphic 利用（3 実装：`MultiModelCompound`/`MultiModelEntity`/`DummyModelEntity`・25 ファイル）は不変（ホルダは `IHasMultiModel` のサブ型なので既存型/キャストはそのまま通る）。`MultiModelEntity`/`DummyModelEntity` は生存（登録済み・モデル選択画面で使用）。
- 併せて `NetworkHandler.sendSyncMultiModel{C2S,S2C}` の同一 5 行ブロック（全 Part の `getTextureHolder` 列挙）を `collectTextureNames(IHasMultiModel)` に抽出。保護コア A のパケット**ワイヤ順は不変**に保つ。

### E-2. ネットワーク: 所有者検証ハンドラ＋ codec 定型（最高 payoff）

**調査で確認された問題点（network/ は NeoForge 現行 API 準拠済み・廃止 API なし）:**
1. **定型コード重複**: C2S ハンドラ 8 種が `enqueueWork → level.getEntity(id) → instanceof LittleMaidEntity → 所有者 UUID 照合 → 実行` を約 54 行繰り返す。
2. **entityId エンコーディング不統一**: `SyncMultiModelPayload`/`SyncSoundPackPayload` が `buf.writeInt()`（固定 4 バイト）、他 8 payload が `ByteBufCodecs.VAR_INT`（可変）。これら 2 つは高頻度送信 payload なので無駄がある。
3. **手書き encode/decode**: `SyncMultiModelPayload`/`SyncSoundPackPayload`/`LMSoundPayload` が手書きで `encode`/`decode` を実装。他は `StreamCodec.composite()` で自動生成。保守性が低い。
4. **`RegistryFriendlyByteBuf` vs `FriendlyByteBuf` 混在**: レジストリ依存のある 3 payload と依存のない残りで型が混在。

**対処方針:**
- `network/PayloadHandlers`（static）に `onOwnedMaid(IPayloadContext, int entityId, BiConsumer<ServerPlayer, LittleMaidEntity>)` を新設し、定型を 1 箇所へ。各ハンドラはラムダ 1 行に縮む。非メイド系（`SoundPlayable`/`TargetTagManager`）向けに `resolveEntity(ctx, id, Class<E>, BiConsumer)` も。
- `SyncMultiModelPayload`/`SyncSoundPackPayload` を `StreamCodec.composite()` + `VAR_INT` に統一。手書き encode/decode を廃止。
- `RegistryFriendlyByteBuf` vs `FriendlyByteBuf` の使い分けを整理（レジストリ依存が本当に必要なものだけ前者を使う）。
- **セキュリティ注意**: 所有者チェック一本化の作業のため、各ハンドラ固有の差（`isStrike()` ゲート・`TamableAnimal`/`OwnableEntity` キャスト・target-tag の Attachment フォールバック）はラムダ側に**必ず保持**。~120–150 行削減＋判定の一貫性向上。

### E-3. 画面: `AbstractFilterableListScreen` 基底＋`drawScrollingText` 共通化（低リスク・client 限定）

`MaidManagerScreen`/`TargetTagScreen`/`SoundPackSelectScreen` が単一 `FilterableListGUI` への 6 イベント転送（`mouseClicked`/`Released`/`Dragged`/`Scrolled`/`keyPressed`/`charTyped`）をほぼ同一本体で持つ。マーキー描画 `drawScrollingText`（scissor＋時間スクロール）は `MaidManagerScreen` と `TargetTagScreen` に**ほぼ重複実装**（既に構造がドリフト）。
- `client/screen/AbstractFilterableListScreen<T extends GUIElement> extends Screen`（`protected FilterableListGUI<T> list`＋6 転送＋`abstract buildList()`＋`renderBackground` フック）を新設し 3 画面を薄く（各 ~40 行減）。
- `drawScrollingText` を `client/util/ClientScreenHelper`（既存・client 静的ヘルパ）へ移し 1 実装に。`ModelSelectScreen`/`SoundPackSelectScreen` 共通の GUI テクスチャ/サイズ定数も定数ホルダへ。
- **GUI コンポーネント層（`GUIElement→ListGUI→ScrollableListGUI→FilterableListGUI`）は既に良く factored されているので触らない**。`ModelSelectScreen`（二重リスト）は対象外。

### E-4. 作業モード: 接近＋recalc 共通ヘルパ＋BFS 重複解消（中）

「範囲外なら recalc カウントダウンで経路生成し `moveTo`、範囲内なら `navigation.stop()` して作業」の定型が 5 モード＋behavior に各自のタイマー（`timeToRecalcPath`/`recalcPathTimer`/`pathReCalcCool`/`recalcPathCool`）でコピーされている（`CookingMode:241`/`PharmcistMode:86`/`TorcherMode:159`/`RipperMode:89`/`CombatMode:204`/`MaidCollectSalary:123`）。経路有効性ガード（`endNode.closerThan` 等）も複数で同一。
- `entity/mode/ModeNavigation`（static）or `ModeHelpers` 拡張に `approachOrStop(...)` / 小さな `RecalcWalker` を新設。`MaidCollectSalary`/`MaidMoveToDropItem` でも再利用。
- BFS 重複: `BlockFinder.searchTargetBlock`（同期）と `BlockFinderPD.tick`（逐次）が同じ seed/searched/linkable BFS を二重実装 → `BlockFinderPD` を単一ソースにし `searchTargetBlock` を `while(!isEnd()) tick()` の薄いラッパに。
- **共通化しない**: `HealerMode`/`PharmcistMode` のドメインロジック（食料/ポーション効果評価・醸造スロット状態機械）、`CombatMode.RangedStyle` の strafe/charge は意味的に固有。reach 閾値（1.75/3×3/BB 距離）・melee の `moveTo(target,speed)` 差はパラメタ化して吸収（強制統一しない）。

### E-5. Brain behavior: `AbstractMaidBehavior` 基底（§C と合流）

13 behavior 中 7 つが `super(ImmutableMap.of(IS_WAITING, VALUE_ABSENT))` を同一コードで持ち、全 behavior が `canStillUse` を「default の `false` で `tick()` が呼ばれない」フットガン回避のため override（同趣旨コメントが ~5× コピペ）。`WALK_TARGET`＋`EntityTracker` 構築も 3×。
- `entity/ai/behavior/AbstractMaidBehavior extends Behavior<LittleMaidEntity>`（`IS_WAITING VALUE_ABSENT` を既定マージするコンストラクタ＋permissive な `canStillUse` 既定＋`walkTo(target, speed, closeEnough)` ヘルパ）を新設。§C の作業モード個別 Behavior 化の共通基底（`AbstractMaidModeBehavior`）もこれに乗せる。
- **注意**: 必要メモリは behavior ごとに違い、`Wait`(IS_WAITING PRESENT)/`Trace`/`Target` は固有 `canStillUse` が要る。基底は permissive＋opt-in にし、意味差を潰さない（line 削減より footgun 除去とエルゴノミクスが主目的）。

### 対象外（監査で確認・蒸し返さない）

- `config/LMNConfig` の define ブロック↔`bake()`: NeoForge `ModConfigSpec` の二相パターン（仕様定義 vs ベイク）であり重複ではない。
- `data/LM*Provider`: NeoForge の定型サブクラスで重複ではない。
- `resource/manager/*`（`LMModelManager`/`LMTextureManager`/`LMConfigManager`/`LMSoundManager`）の `INSTANCE`＋小文字キー Map: 共通面が薄く差異が実質的（~15-20 行のみ）。触るついでに `LowercaseRegistry` 基底にする程度（低優先）。`resource/loader/` は `LMLoader`＋`LMFileLoader` で既に Strategy 化済み・保護コア B 隣接のため触らない。
- `entity/util` の interface+Impl ペア: 委譲シーム（意図的分割）であり共通化対象ではない（むしろ §B のインライン候補）。

**手順（各項目共通）**: 足場を抽出（`common/` or ドメインパッケージ）→ ガードは呼び出し側に残す → `./gradlew build`（CI）→ 影響系の検証（E-2 はマルチで所有者判定・各 setter、E-3 は runClient で GUI 操作、E-4 は各モード動作、E-1/E-5 は描画/AI 同値）。保護コア A/B の観測保証（描画・ボイス・パケットワイヤ順）を厳守。

---

## §F — LittleMaidEntity の分割（巨大クラスのコンポーネント抽出）

**現状**: `entity/LittleMaidEntity.java` は **1934 行・10 interface 実装**で最大ファイル（次点 `LittleMaidScreen` 694 行の約 3 倍）。既に多くがコンポーネント委譲済み（`LMSafeMovement`/`LMInteractionHandler`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MaidResurrection`/`BookParameterParser`/`TargetTagManagerImpl`/`TargetingSystem`/`MultiModelCompound`/`SoundPlayableCompound`/`MaidLookControl`）。残る **インライン機能クラスタ**を同じ委譲パターンで順次切り出す。

**抽出パターン（既存に倣う）**: 状態＋ロジックを `entity/util/`（または `entity/component/`）の `LMXxx` クラスへ移し、`LittleMaidEntity` はフィールド 1 つで保持して呼ぶ。`@Override`（特に `super` を呼ぶもの・バニラ protected の override）は **本体に残し、中身だけ委譲**する（CLAUDE.md 方針）。protected フィールド/メソッドが必要なら同パッケージのパッケージプライベートゲッター or `_LM` ブリッジを足す（`LMSafeMovement` の `calculateFallDamage`/`fallDistance` 方式）。

**切り出し候補（独立性の高い順・1 クラスタ＝1 コミット）**:
1. **戦闘** → `MaidCombat`: `doHurtTarget`/`hurtServer`/`performRangedAttack`/クロスボウ（`isCharging`/`setChargingCrossbow`/`onCrossbowAttackPerformed`）/`hurtArmor`/`hurtHelmet`/`killedEntity`/`canAttack`/`getProjectile`。`@Override` 本体は残し中身を委譲。`CrossbowAttackMob` 契約と他 Mod 互換の try/catch は維持。
2. **加速機能** → `MaidAcceleration`: `getTickMultiple`/`setAccelerationTicks`/`decAccelerationTicks`/`getAccelerationTicks`/`isAcceleration`/`inTickMultiplePre`/`inTickMultiplePost`＋`accelerationTicks` フィールド。`ACCELERATE` 同期とスポーンパケットの varint は据え置き。
3. **環境音・演出** → `MaidVoice`/`MaidParticle`: `playAmbientSound`（時間帯/天候/体力/時計の分岐）/`die` の死亡ボイス/`handleEntityEvent` の粒子/`showFreedomParticle`/`showTracerParticle`。**保護コア B**（`LMSounds` 定数・`play(String)` シグネチャ）は不変。
4. **個体差初期化** → 初期化ヘルパ: `setRandomTexture`/`setRandomVoice`（`idFactor` ベース）。コンストラクタからの呼び出し順（`initIdFactor()` 後）に注意。
5. **multimodel/sound 委譲（~16 メソッド）** → **§E の common ホルダで解消**（重複削除と同時に本体も縮む）。`writeSpawnData`/`readSpawnData` の同期もここで整理。

**注意**: `initGoals()`/`registerGoals()` は `Mob` コンストラクタ内で呼ばれサブクラスのフィールドが未初期化。外部委譲する場合はラムダで遅延参照する（CLAUDE.md）。NBT 入出力は `ValueOutput`/`ValueInput`＋Codec を踏襲。

**検証**: 1 クラスタ移すごとに `./gradlew build`（CI）＋ 該当機能の GameTest／`runClient` 目視（戦闘・加速・ボイス・描画）。挙動同値を確認してから次へ。

---

## §G — ディレクトリ・クラス数の削減（構造刷新）

**現状**: 36 ディレクトリ・220 Java ファイル。単一クラスしか入っていないディレクトリが 9 個あり、機能の散らばりが大きい。§C 完了で `api/mode/` と `entity/mode/` が消え自然に縮むが、それ以外にも平坦化できる箇所がある。

**不変原則**: `maidmodel/`・`resource/classloader/` は保護コア A の ASM リマップ基盤であり、ディレクトリ構造含め移動厳禁。`multimodel/` と `resource/` の内部構造も極力保持する。

---

### G-1. 単一クラスディレクトリの平坦化（低リスク・import 変更のみ）

| 現在のパス | ファイル | 移動先 |
|---|---|---|
| `entity/ai/control/MaidLookControl.java` | 1 | `entity/ai/` |
| `entity/ai/sensor/LittleMaidSensor.java` | 1 | `entity/ai/` |
| `client/key/LMKeys.java` | 1 | `client/` |
| `client/network/ClientNetworkHandler.java` | 1 | `client/` |
| `client/resource/loader/` | 1 | `client/resource/` |
| `client/resource/manager/` | 1 | `client/resource/` |

**手順**: ファイルを移動 → import を一括置換 → `./gradlew compileJava` でエラーゼロを確認。1 ディレクトリ＝1 コミット。

---

### G-2. `api/` パッケージ廃止（§C 完了後）

§C で `api/mode/` の全クラスが削除されると `api/` が空になる。

- `IRangedWeapon` interface（`MixinRangedWeaponItem` が `implements` に使用）を `entity/ai/behavior/` または `util/` へ移設してから `api/mode/` → `api/` の順で削除。
- §C-3 の「全撤去」コミットに含めてよい。

---

### G-3. 誤配置の是正

- **`mixin/CrossbowItemInvoker.java`**: `@Mixin` 注釈なし・`mixins.json` 未登録。Mixin ではなくただのユーティリティクラス。`util/` 配下（例: `util/CrossbowSpeedUtil`）へ移設、または唯一の呼び出し元（`LittleMaidEntity.java`）へ定数をインライン化して削除。§D と同時実施が自然。
- **`criteria/` または `advancement/criterion/`**: `ContractMaidCriterion`/`ResurrectMaidCriterion` は正しく実装・使用中（契約/復活の進行条件）。廃止不可。残存理由は明確なのでこれ以上調査しない。

---

### G-4. `setup/` の整理（ClientSetup 削除後）

`setup/ClientSetup.java` は §B で削除済み（または予定）。残るのは `ModSetup.java` + `ModRegistration.java` の 2 ファイル。

- `ModSetup` が薄い場合（`LittleMaidNeo` コンストラクタから数行呼ぶだけ）: `LittleMaidNeo.java` へインライン化し `ModSetup` を削除 → `setup/` が `ModRegistration` のみになれば、`ModRegistration` をルート直下（または `setup/` 維持）に移して `setup/` 廃止を検討。
- `ModRegistration` は巨大なため安易に移動しない。`setup/` が `ModRegistration` 専用パッケージとして機能しているなら残してよい。

---

### G-5. 目標パッケージ構造（§C・§G 完了後の想定）

```
work.nemonet.littlemaidneo/
├── LittleMaidNeo.java
├── LittleMaidNeoClient.java
├── entity/
│   ├── LittleMaidEntity.java
│   ├── MultiModelEntity.java
│   ├── MaidSoulEntity.java
│   ├── EntityLittleMaid.java       ← 保護コア A スタブ・移動不可
│   ├── ai/
│   │   ├── behavior/               ← Maid*Behavior 群（§C 後に増加）
│   │   ├── MaidLookControl.java    ← G-1 で移動
│   │   └── LittleMaidSensor.java   ← G-1 で移動
│   ├── compound/                   ← IHasMultiModel 等（§E-1 で整理）
│   ├── soul/
│   ├── targeting/
│   └── util/
├── block/
├── item/
├── network/                        ← NetworkHandler + Payload 群（§E-2 で整理）
├── client/
│   ├── LMKeys.java                 ← G-1 で移動
│   ├── ClientNetworkHandler.java   ← G-1 で移動
│   ├── renderer/
│   ├── screen/
│   │   ├── component/
│   │   └── AbstractFilterableListScreen.java  ← §E-3 で追加
│   ├── resource/                   ← G-1 で loader/manager を統合
│   └── util/
├── maidmodel/                      ← 保護コア A・移動不可
├── multimodel/                     ← 保護コア A・極力維持
├── resource/                       ← 保護コア A/B・維持
├── mixin/
├── config/
├── data/
├── setup/                          ← ClientSetup 削除後 2 ファイル以下
├── tags/
├── advancement/
├── command/
└── util/
```

削減目標: **36 → 約 24 ディレクトリ**（-12）、**220 → 約 185 ファイル**（§C/§B/§G 合計で -35 前後）。

**検証（G-1 平坦化）**: 移動は `./gradlew compileJava` + `./gradlew build`（CI）のみで十分（動作変更なし）。G-2/G-4 は §C/§B との同コミットで可。

---

## 付録: 現代化採用状況（ギャップ分析・main 時点）

| 機能 | 状況 | メモ |
|---|---|---|
| Payload Networking（`StreamCodec`） / Deferred Register / Data Attachments / Mixin / TOML / Mojang mappings / moddev / 並行ロード / BlockState | ✅ 採用済 | `network/`, `setup/ModRegistration`, `build.gradle.kts` 等 |
| `ValueInput/Output`＋Codec 永続化 | ✅ 採用済 | 旧 `getOrCreateTag` 等は不使用 |
| Codec（`MaidMode` / `ModeManager` / `MaidSoulData`） | ✅ 採用済 | ADR-0002 |
| Brain AI（移動・戦闘・作業・補助） | ⚠️ 大半採用・残件あり | ADR-0003。`entity/ai/behavior/` 13 種・全 CORE。`registerGoals()` にバニラ補助 Goal（視線/パニック/退避）残存→**§A で全廃予定**。Activity 分割（CORE/FIGHT/WORK/IDLE）も未実装→§A-5/§C |
| 作業モードの Behavior 化 | ⚠️ wrapper 方式・廃止予定 | `MaidWorkModeBehavior`→`Mode` 委譲構造を**§C で全廃**。`Mode`/`ItemMatcher`/`ModeManager` ごと撤去し各 Behavior が直接保持する形へ |
| DataGen（model/blockstate/lang/tag/recipe/loot/advancement） | ✅ 採用済 | `data/LMModelProvider` ほか |
| Brigadier コマンド | ✅ 採用済 | `command/LMCommands` |
| DataFixer（MaidSoul/エンティティ NBT 限定） | ✅ 採用済 | `entity/soul/MaidDataFixer` |
| 描画 Blaze3D 本体移行 | 🛡️ 見送り決定 | ADR-0001。P-1〜P-6 最適化のみ実施 |
| Forge Energy | ⛔ 非該当 | 電力概念なし |
| **内部整理（デッドコード/Mixin/共通化/分割）** | ⚠️ 進行中 | → **§B / §D / §E / §F** |
