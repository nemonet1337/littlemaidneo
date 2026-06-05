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
- **音量**: `config/LMMLConfig#getVoiceVolume()`（キー名・範囲不変）。
  > 注: 上記クラスでも「外部契約に関わらない内部実装」は整理可。`SoundPlayable.play(String)` のシグネチャと `LMSounds.*` 文字列・`.cfg` キー解決の振る舞いは不変に保つこと。

### 0.4 既存実装の事実（推測しないこと）
- `LittleMaidEntity` の委譲先: `MaidResurrection`/`BookParameterParser`/`LMInteractionHandler`/`LMSafeMovement`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MultiModelCompound`/`SoundPlayableCompound`/`TargetTagManagerImpl`/`TargetingSystem`/`MaidLookControl`。
- NBT 入出力は **`ValueOutput`/`ValueInput`＋Codec**（旧 `CompoundTag` 直書きは原則不使用）。
- パケットは全 15 種が `StreamCodec`（`network/`）。登録は `network/NetworkHandler.register(RegisterPayloadHandlersEvent)`。
- `DeferredRegister` は **`setup/ModRegistration.java`** に集約、`LittleMaidNeo` コンストラクタで `register(modEventBus)`。
- **AI 現状（ADR-0002 / ADR-0003）**: 移動軸 `MaidMode`・作業軸 `Mode`・補助行動とも **Brain Behavior へ全面移行済み**（`entity/ai/behavior/Maid*Behavior` 13 種）。**全 Behavior は CORE Activity に一括登録**（FIGHT/WORK/IDLE への分割は未実装。ADR-0003 の Activity 体系化記述は将来像であり実装と差異あり＝§A 参照）。作業軸は単一 `MaidWorkModeBehavior` が `ModeManager` で選択中の `Mode` に `shouldExecute/start/tick/stop` を委譲する（`ModeWrapperGoal` は廃止・`entity/goal/` パッケージは削除済み・ソース参照ゼロ）。永続化は `MaidMode.CODEC` / `ModeManager.CODEC`。戦闘は `CombatMode` に統合。`registerGoals()` には**意図的に**バニラ補助 Goal（`AvoidEntityGoal`/`PanicGoal`/`LookAtPlayerGoal`×2/`RandomLookAroundGoal`）のみ残置。
- API 調査は `mc-api-research` エージェント必須。NeoForge/Mojang マッピングのメソッド名はバージョンで変わる。ライブラリ Doc は Context7 MCP 優先。

### 0.5 検証（コミット前に必ず）
1. `./gradlew compileJava`（全変更）／`./gradlew build`（区切り、CI 相当）
2. 保護コア A に触れたら `runClient` で **実在の外部モデルパック(.class)** を `LMMLResources/` に置き描画確認
3. 保護コア B に触れたら 外部ボイスパック(.cfg+.ogg) 再生確認
4. セーブ互換に触れたら **既存ワールドをロードして NBT エラーが出ないこと** ＋ `runServer` でマルチ接続・ディメンション移動引き継ぎ
5. DataGen は `runData` 後 `src/generated/resources/` が既存 JSON と **差分ゼロ（意味的一致）**
6. `runGameTestServer`（namespace `littlemaidneo`）で回帰確認

### 0.6 推奨実施順（依存関係）
モダン化 WS1〜5（Brain AI / Codec / DataGen / Brigadier / DataFixer）は**完了済み**。本書は後続の内部整理 §A〜§F を扱う。推奨順は **§B（デッドコード削除）→ §E（common 切り出し）→ §F（LittleMaidEntity 分割）→ §D（Mixin 整理）**（低リスク順。§E と §F は委譲メソッドが重なるため近接実施）。§A（Goal AI 化）・§C（Mode Behavior 化）は完了済みで**状況記載のみ・HowTo 不要**。各区切り＝1 コミット。重要な設計判断は `/doc` で `docs/adr/` に記録し CLAUDE.md の該当節も同コミットで更新する。

---

## §A — Goal の AI(Brain)化（✅ 完了・状況記載のみ／HowTo 不要）

> 本節は **状況報告のみ**。機能的な問題は無いため実装 HowTo は記載しない（指示に基づく）。

**進捗**: カスタム Goal は全廃され、メイドさんの行動はすべて Brain の `BehaviorControl`（`entity/ai/behavior/Maid*Behavior` 13 種）で駆動する。`entity/goal/` パッケージと `entity/mode/ModeWrapperGoal` は削除済み（ソース参照ゼロ・確認済み）。バニラの `FloatGoal`/`OpenDoorGoal` も `Swim`/`InteractWithDoor` Behavior へ置換済み。

**意図的に残置している Goal（機能的問題なし・あえて Goal のまま）**:
- `LittleMaidEntity#registerGoals()`: `AvoidEntityGoal`（危険な敵からの逃避）/ `PanicGoal`（野良メイドさんのパニック）/ `LookAtPlayerGoal`×2 / `RandomLookAroundGoal`。
  - **視線（頭部向き）はあえて Goal**: `LOOK_TARGET` メモリを設定するプロデューサが本 Mod に存在せず、`LookAtTargetSink` 系を入れると常に no-op になる。孤立した不活性 Behavior の混入を避けるため、頭部向きはバニラ Goal に委ねる設計判断（`LittleMaidEntity.java:260-265` のコメント参照）。
- `MultiModelEntity#registerGoals()`: `FloatGoal` + `LookAtPlayerGoal`×2（モデル選択用のダミー表示エンティティ。AI を持たないため Brain 化対象外）。

**唯一の是正候補（任意・低優先）**: ADR-0003 は `CORE/FIGHT/WORK/IDLE` の 4 Activity 体系を記すが、実装は **全 Behavior が `CORE` に一括登録**で Activity 分割は未実装。挙動は正常なので機能上の問題はない。整合させるなら (a) ADR-0003 を実装の実態（CORE 一括）に合わせて更新するか、(b) `setActiveActivityToFirstValid([FIGHT, WORK, IDLE])` を実装してから ADR を正とする。どちらも任意。

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
3. 旧 Goal 系・`ClientSetup` を指す stale ドキュメントを是正（`CLAUDE.md:16` の `entity/goal/ — AI Goal` ／ `CLAUDE.md` の `ClientSetup` 言及 ／ 本 HOWTO 旧記述）。

**検証**: `./gradlew build`（CI）。挙動不変なので実機検証は不要（保護コアに触れない）。

---

## §C — 各 Mode の Behavior 化（✅ 完了・wrapper 方式／新規 Mode 追加手順）

**進捗**: 6 モード（`CombatMode`/`CookingMode`/`HealerMode`/`PharmcistMode`/`RipperMode`/`TorcherMode`）は **`Mode` サブクラスのまま**、単一の `MaidWorkModeBehavior`（CORE）が `ModeManager` で選択中（`ItemMatcher` の Priority 降順）の `Mode` に `shouldExecute/start/tick/stop/resetTask` を委譲する。これにより作業軸も Brain 駆動になっている。

**個別 Behavior 化は行わない（設計判断・ADR-0002/0003）**: 各モードを 1 つずつ独立 Behavior へ割るのではなく、`Mode` ロジックを温存して wrapper 1 枚で駆動する。理由は (1) 外部モデルパックが参照する描画 caps `caps_job`（`Mode#getJobName()`、`CombatMode` は `fencer`/`archer` を返す）契約の保護、(2) `ModeManager` のレジストリ＋Priority 判定の再利用、(3) モード追加コストの最小化。したがって「mode の Behavior 化」は **完了**（これ以上の分割は予定なし）。

**新しい作業モードを追加する手順（Behavior は増やさない）**:
1. `entity/mode/` に `Mode` を継承したクラスを実装（`shouldExecute`/`tick` 等をオーバーライド。状態の永続化が必要なら `writeModeData`/`readModeData` も）。
2. `api/mode/Mode.java` の `ENTRIES` に登録エントリを追加し、`buildXxxMode()` で `ModeType.Builder` を構築（`addItemMatcher(matcher, Priority)` で判定アイテムを定義）。
3. 判定タグを `data/littlemaidneo/tags/items/{mode_name}_mode.json`（DataGen は `data/LMItemTagsProvider`）に、表示名を `assets/littlemaidneo/lang/{en_us,ja_jp}.json` の `mode.littlemaidneo.{Name}` に追加。
4. `MaidWorkModeBehavior`／Brain 側は **無改修**（ModeManager 経由で自動的に駆動対象になる）。

**残課題（別管理・挙動変化を伴う）**: 一部モードの内部状態（`CombatMode` の cooldown、`HealerMode` の index 等）が未永続化でリロード時に挙動が変わる。記述量削減リファクタとは分離し「モード状態 NBT 永続化」（TODO.md）で扱う。

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

**統合・移設**:
- `MixinCrossBowItem`（`getInterval_LMRB` を override するだけ）→ `MixinRangedWeaponItem` に `instanceof CrossbowItem` 分岐として畳み、`MixinCrossBowItem` を削除（Mixin 1 件減）。`mixins.json` から登録も削除。
- `mixin/CrossbowItemInvoker` — **Mixin ではない**（`@Mixin`/`@Invoker` 注釈なし・`mixins.json` 未登録で正しい）。`CrossbowItem.getSpeed` が `private static` のためロジックを定数で再実装した普通のユーティリティ。`mixin/` パッケージにあるのが誤配置。`entity/util/`（例: `CrossbowSpeedUtil`）へ移設、または唯一の呼び出し元（`LittleMaidEntity.java:1118`）へ定数をインライン。

**任意（@Shadow 削減）**: `MixinAbstractFurnaceBlockEntity#isBurningFire_LM` は `litTimeRemaining>0` 相当。呼び出し側（`CookingMode`）でブロックステートの `AbstractFurnaceBlock.LIT` から導けば `@Shadow` を 1 つ減らせる。

**手順（撤去 1 件ごと）**: (1) 代替イベントハンドラを実装し挙動を移す → (2) 旧 Mixin を削除 → (3) `mixins.json` から登録名を削除 → (4) `./gradlew build`（CI）→ (5) 影響系の実機/GameTest 確認（復活儀式・騎乗・就寝ボイス・XP/アイテム拾い・射撃モード分類）。命名は `_LM`/`_LMRB` 混在を `_LM` に寄せると一貫する（任意）。

---

## §E — common/ パッケージの切り出し（重複委譲の共通化）

**目的**: `LittleMaidEntity` と `MultiModelEntity` が **同一の委譲ボイラープレート約 16 メソッド**（`IHasMultiModel` 13 ＋ `SoundPlayable` 3 → `MultiModelCompound`/`SoundPlayableCompound`）を二重に持つのを解消する。

**制約**: 両エンティティは親クラスが異なる（`LittleMaidEntity extends TamableAnimal` / `MultiModelEntity extends PathfinderMob`）ため、**共通の基底クラスは作れない**。`MultiModelCompound`/`SoundPlayableCompound` は委譲先の **実体**（`IHasMultiModel`/`SoundPlayable` を実装した本体ロジック）であり、ここに default メソッドを足すのは不可。→ **「ホルダ interface ＋ default メソッド」**で共通化する。

**方針（新規 `entity/common/` パッケージ）**:
1. `entity/common/MultiModelHolder`（`extends IHasMultiModel`）を新設し、`MultiModelCompound getMultiModel();` を 1 つ宣言。`IHasMultiModel` の 13 メソッドを **default 実装**として `getMultiModel().xxx(...)` へ委譲。
2. 同様に `entity/common/SoundHolder`（`extends SoundPlayable`）を新設し、`SoundPlayableCompound getSoundCompound();` ＋ `play`/`setConfigHolder`/`getConfigHolder` の default 委譲。
3. `LittleMaidEntity` と `MultiModelEntity` は `implements MultiModelHolder, SoundHolder` にし、各々の **13+3 メソッド本体を削除**して `getMultiModel()`/`getSoundCompound()`（既存フィールドを返すだけ）のみ実装。
   - `IHasMultiModel` の polymorphic 利用（3 実装：`MultiModelCompound`/`MultiModelEntity`/`DummyModelEntity`、25 ファイル参照）は **不変**。ホルダは `IHasMultiModel` のサブ型なので既存の型・キャストはそのまま通る。
4. 併せて common 化できる候補（任意・段階的）: スポーン同期の multimodel/sound 部（`writeSpawnData`/`readSpawnData`）、`addAdditionalSaveData`/`readAdditionalSaveData` の `multiModel.writeToNbt/readFromNbt` 呼び出し。共通ヘルパ（static or default）に寄せる。

**手順**: `entity/common/` 作成 → ホルダ interface 2 つ追加 → 両エンティティを付け替え＋重複メソッド削除 → `./gradlew compileJava`（CI）→ `runClient` でモデル/テクスチャ/ボイスが従来どおり（保護コア A/B の観測保証）を確認。

**注意**: `MultiModelEntity`/`DummyModelEntity` は **生きている**（`MULTI_MODEL_ENTITY`/`DUMMY_MODEL_ENTITY` として登録・モデル選択画面で使用）。デッドコードではないので付け替え対象に含める。

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

## 付録: 現代化採用状況（ギャップ分析・main 時点）

| 機能 | 状況 | メモ |
|---|---|---|
| Payload Networking（`StreamCodec`） / Deferred Register / Data Attachments / Mixin / TOML / Mojang mappings / moddev / 並行ロード / BlockState | ✅ 採用済 | `network/`, `setup/ModRegistration`, `build.gradle.kts` 等 |
| `ValueInput/Output`＋Codec 永続化 | ✅ 採用済 | 旧 `getOrCreateTag` 等は不使用 |
| Codec（`MaidMode` / `ModeManager` / `MaidSoulData`） | ✅ 採用済 | ADR-0002 |
| Brain AI（移動・戦闘・作業・補助の全面移行） | ✅ 採用済 | ADR-0003。`entity/ai/behavior/` 13 種・全 CORE。残置はバニラ補助 Goal のみ（§A） |
| DataGen（model/blockstate/lang/tag/recipe/loot/advancement） | ✅ 採用済 | `data/LMModelProvider` ほか |
| Brigadier コマンド | ✅ 採用済 | `command/LMCommands` |
| DataFixer（MaidSoul/エンティティ NBT 限定） | ✅ 採用済 | `entity/soul/MaidDataFixer` |
| 描画 Blaze3D 本体移行 | 🛡️ 見送り決定 | ADR-0001。P-1〜P-6 最適化のみ実施 |
| Forge Energy | ⛔ 非該当 | 電力概念なし |
| **内部整理（デッドコード/Mixin/共通化/分割）** | ⚠️ 進行中 | → **§B / §D / §E / §F** |
