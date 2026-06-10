# LittleMaidNeo — TODO / バックログ

> 更新日: 2026-06-05
> 方針: NeoForge 単一プラットフォーム / LMRB＋LMML 統合 / モダン化リファクタリング
> 実装ガイド・保護コア詳細: `HOWTO.md`（§A〜§F）
> 設計判断: `docs/adr/`（0001 描画ブリッジ維持 / 0002 MaidMode Codec・AI 状態統一 / 0003 Brain AI 完全移行）
> 全体プラン: `docs/plan/2026-06-01_統合リファクタリングプラン.md`

このファイルは「実態と一致する単一バックログ」。完了項目は削除する（履歴は不要）。

> **完了済みベースライン（履歴省略・現状把握用に要点のみ）**: Brain AI 全面移行（ADR-0003）／ `MaidMode`・`Mode`・`MaidSoul` の Codec 化（ADR-0002）／ DataGen（model/blockstate/lang/tag/recipe/loot/advancement）／ Brigadier コマンド（`command/LMCommands`）／ DataFixer（`entity/soul/MaidDataFixer`）。これらは消化済みのため本バックログからは除外。

---

## 🧹 内部整理リファクタリング（6 ワークストリーム・進行中）

> 背景: Brain AI 移行後のクリーンアップ。低リスクから消化する。推奨順 **§B → §E → §F → §D**（§A/§C は完了済み・状況のみ）。詳細手順は `HOWTO.md` の対応節。
> ⚠️ ローカル JDK25 不在環境では検証は CI（Java25）が前提。保護コアに触れる §D/§E は `runClient` 実機確認を併用。

### §A — Goal の AI(Brain)化（残存バニラ Goal を全廃）… → HOWTO §A

- **進捗**: カスタム Goal は全廃済み（`entity/goal/`・`ModeWrapperGoal` 削除・参照ゼロ、`Float`→`Swim`/`OpenDoor`→`InteractWithDoor` も Behavior 化）。**ただし `registerGoals()` にバニラ補助 Goal が残存**。これも Brain へ移し `registerGoals()` を空にするのがゴール。
- [ ] 視線（`LookAtPlayerGoal`×2/`RandomLookAroundGoal`）→ `SetEntityLookTarget`＋`LookAtTargetSink`（`LOOK_TARGET`/`NEAREST_VISIBLE_PLAYER` メモリとプロデューサを追加し、旧「no-op だから未登録」判断を解禁）。HOWTO §A-1。
- [ ] パニック（`PanicGoal`・**未テイム時のみ**）→ カスタム `MaidPanicBehavior`（`escapeSpeed` で逃走）。HOWTO §A-2。
- [ ] **退避（`AvoidEntityGoal`）→ `MaidAvoidBehavior`（慎重に）**: バニラ汎用ではなく `fleeEntities`（`MaidTargetBehavior` が動的登録）からのみ逃げる独自退避。述語プルーニング・距離/速度・competing との同値を厳守。HOWTO §A-3。
- [ ] `MultiModelEntity#registerGoals()` の `FloatGoal`+`LookAtPlayerGoal`×2（ダミー表示・優先度低）。
- [ ] 仕上げ: `registerGoals()` 空化後に `CORE/FIGHT/WORK/IDLE` の Activity 体系化を実装し ADR-0003 と整合（§C と同時実施が綺麗）。HOWTO §A-5。

### §B — デッドコード削除 … → HOWTO §B（低リスク・即効）

- [ ] `setup/ClientSetup.java`（空 `init()`・参照ゼロ）をクラスごと削除。
- [ ] `entity/util/MaidMode.fromName(String)`（呼び出しゼロ・`byName` の重複）削除。
- [ ] `api/mode/ItemMatchers` の `item(Item)`(@Deprecated)/`name(String)` ＋ private record `ItemInstance`/`NameMatcher`（全て呼び出しゼロ）削除。
- [ ] `api/mode/ModeType.Builder.addItemMatcher(ItemMatcher)` 単一引数版(@Deprecated・呼び出しゼロ)削除。
- [ ] `mixin/MixinPlayerEntity` の空 `@Inject` 2 件（`<init>` / `stopSleepInBed`）削除（§D と同時でも可）。
- [ ] （要確認）`util/BlockFinder` の `findTarget`/`findHorizonPos`/`findLayer`/`findHorizon`（未使用の疑い・`//多分動かん`）— call-site 再確認のうえ削除。
- [ ] （任意・低）単一実装マーカー interface のインライン化: `HasMaidMode`/`Contractable`/`HasMode`/`GuiEntitySupplier`（polymorphic 利用ゼロ）。`HasInventory`/`SalaryBoxPosListener`/`LMCollidable`/`ProcessDivider` は load-bearing で残す。
- [ ] stale ドキュメント是正: `CLAUDE.md:16` の `entity/goal/ — AI Goal`（削除済みパッケージ）／`CLAUDE.md` の `ClientSetup` 言及／**`CLAUDE.md` Architecture Notes が実在しないクラス（`BlockWorkMode`/`WorkStrategy<T>`/`BlockSearch`/`SearchCondition`/`BlockReservationManager`）を前提に記述**（実体は `util/BlockFinder`・`BlockFinderPD`・`ProcessDivider`・`ModeHelpers`）→ E-4 と同時に是正（**CLAUDE.md は既にコミット済み**）。
- **criteria は廃止不可（調査済み）**: `ContractMaidCriterion`/`ResurrectMaidCriterion` の 2 件は契約・復活の進行条件として正常実装中（`CriteriaTriggers.register` + NeoForge 26.x `RegisterEvent` 方式・正常）。残存理由は明確であり対応不要。

### §C — Mode/ItemMatcher 廃止：Behavior が直接 AI＋アイテム識別を保持する … → HOWTO §C

- **現状**: `Mode` 抽象クラス（6 サブクラス）＋`ItemMatcher`/`ModeType`/`ModeManager`/`HasModeImpl` が絡み合い、単一 `MaidWorkModeBehavior` が `Mode.tick()` を委譲する 2 層構造。`getMode()` に caps_job/caps_isWorking/TargetingSystem/Codec 永続化が依存。
- **方針: `Mode`・`ItemMatcher`/`ItemMatchers`・`ModeType`・`ModeManager`・`HasModeImpl`/`HasMode`・`MaidWorkModeBehavior` を完全廃止。各 Behavior クラス本体がアイテム識別＋AI 実行＋永続状態を直接保持する。**
- [ ] **C-1 設計確定・ADR 記録**: `ACTIVE_JOB_NAME`（`MemoryModuleType<String>`）、`PersistentMaidBehavior` interface、`AbstractMaidWorkBehavior` の abstract メソッド一覧を確定し ADR に記録してから実装開始（HOWTO §C-1 参照）。
- [ ] **C-2 個別 Behavior 新設**: `MaidCookingBehavior`/`MaidHealerBehavior`/`MaidPharmcistBehavior`/`MaidTorcherBehavior`/`MaidRipperBehavior`/`MaidCombatBehavior`（各 Mode の AI ロジックを直接移植・アイテム識別を各 Behavior に持たせる）。
- [ ] **C-3 旧システム全撤去**: `Mode`・`ItemMatcher`/`ItemMatchers`・`ModeType`・`ModeManager`・`HasModeImpl`/`HasMode`・`MaidWorkModeBehavior`・`entity/mode/` パッケージ（`ModeHelpers` は §E-4 で判断）を削除。`getMode()` 廃止・全消費側を新 API へ移行。
- **注意（caps_job 保護コア A 隣接）**: `caps_job`（外部モデルパック依存）は `ACTIVE_JOB_NAME` メモリ経由で `LittleMaidModelCaps` が読む形で維持。`MaidCombatBehavior` は武器種に応じ `"fencer"`/`"archer"` を tick ごとに書き込む（旧 `CombatMode#getJobName()` の動的評価を継承）。
- §B の `ItemMatchers` deprecated 削除・`ModeType.Builder` 単一引数版削除は §C-3 の全廃に吸収される（個別に対応不要）。

### §D — Mixin の整理・脱 Mixin … → HOWTO §D（中リスク）

- **KEEP（バイトコード必須）**: `MixinExperienceOrbEntity`/`MixinItemEntity`/`MixinRangedWeaponItem`/`MixinAbstractFurnaceBlockEntity`(getRecipeType_LM)/`MixinPlayerEntity`(positionRider/onPassengerTurned override)。
- [ ] `MixinServerPlayerEntity`（6 `@Inject`・状態フィールドなし）→ NeoForge イベント（`PlayerEvent.Clone`/`PlayerTickEvent.Pre`/睡眠/セーブ）へ。ファイルごと撤去候補。
- [ ] `MixinCandleCakeBlock`（復活儀式）→ `UseItemOnBlockEvent` へ。ファイルごと撤去候補。
- [ ] `MixinCrossBowItem` → `MixinRangedWeaponItem` に `instanceof CrossbowItem` 分岐で統合（Mixin 1 件減）。
- [ ] `mixin/CrossbowItemInvoker`（実は Mixin ではない・誤配置）→ `entity/util/`（`CrossbowSpeedUtil` 等）へ移設 or インライン。
- [ ] （任意）`MixinAbstractFurnaceBlockEntity.isBurningFire_LM` → ブロックステート `LIT` 由来にして `@Shadow` 削減。
- 注意: Mixin 撤去時は `littlemaidneo.mixins.json` の登録も同時削除。命名は `_LM` サフィックスへ統一済み。

### §G — ディレクトリ・クラス数の削減（構造刷新）… → HOWTO §G

- **現状**: 36 ディレクトリ・220 Java ファイル。単一クラスしか入っていないディレクトリが 9 個あり、機能の散らばりが大きい。
- [ ] **G-1 単一クラスディレクトリを平坦化**（低リスク・import 変更のみ）:
  - `entity/ai/control/` (MaidLookControl のみ) → `entity/ai/`
  - `entity/ai/sensor/` (LittleMaidSensor のみ) → `entity/ai/`
  - `client/key/` (LMKeys のみ) → `client/`
  - `client/network/` (ClientNetworkHandler のみ) → `client/`
  - `client/resource/loader/` + `client/resource/manager/` → `client/resource/`
- [ ] **G-2 `api/` パッケージ廃止（§C 完了後）**: `api/mode/` が全廃されると `api/` が空になる。`IRangedWeapon` interface（`MixinRangedWeaponItem` 経由で使用）は `entity/ai/behavior/` or `util/` へ移設してから `api/` ディレクトリごと削除。
- [ ] **G-3 誤配置・判断待ち**:
  - `mixin/CrossbowItemInvoker.java`（`@Mixin` 無し・`mixins.json` 未登録の普通のユーティリティ）→ `util/` へ移設（§D-4 参照）。
  - `multimodel/IMultiModel.java`（唯一のファイル）→ `multimodel/layer/` との距離を縮める移設検討（保護コア A 隣接のため慎重に）。
- [ ] **G-4 `setup/` 整理**: `ClientSetup.java` 削除（§B）後、`ModSetup` + `ModRegistration` の 2 ファイルに。`ModSetup` が薄ければ `LittleMaidNeo.java` にインライン化して `setup/` ごと廃止を検討。
- **不変（移動厳禁）**: `maidmodel/`・`resource/classloader/`（保護コア A の ASM リマップ基盤）は内部構造含め移動しない。

### §E — 共通化（Mod 全体・`common/` 切り出し）… → HOWTO §E

> 方針: エンティティに限らず Mod 全体の重複スキャフォールディングを共通化。横断的部品は `common/`、ドメイン固有基底は各ドメインパッケージへ（神パッケージ化しない）。足場のみ抽出しガードは呼び出し側に残す。payoff 順: E-2 → E-3 → E-1 → E-4 → E-5。

- [ ] **E-2 ネットワーク（最高 payoff）**: C2S ハンドラ 8 種の「entity 解決→所有者 UUID 照合→実行」定型（~54 行重複）を `network/PayloadHandlers.onOwnedMaid(...)` ＋ codec ファクトリへ。具体的な是正箇所: (a) `SyncMultiModelPayload`/`SyncSoundPackPayload` の `buf.writeInt()` → `VAR_INT` に統一、(b) 3 payload の手書き encode/decode → `StreamCodec.composite()` へ統一、(c) `RegistryFriendlyByteBuf` vs `FriendlyByteBuf` の混在を整理。固有ガード（`isStrike` 等）はラムダに残す。~120–150 行削減。
- [ ] **E-3 画面（低リスク）**: `MaidManagerScreen`/`TargetTagScreen`/`SoundPackSelectScreen` の 6 イベント転送を `client/screen/AbstractFilterableListScreen<T>` 基底へ。二重実装の `drawScrollingText` を `client/util/ClientScreenHelper` に 1 本化。GUI コンポーネント層は触らない。
- [ ] **E-1 エンティティ委譲**: `LittleMaidEntity`/`MultiModelEntity` 重複の `IHasMultiModel`(13)＋`SoundPlayable`(3) を `common/MultiModelHolder`・`common/SoundHolder`（default メソッド付き）で解消。`NetworkHandler.sendSyncMultiModel{C2S,S2C}` の同一ブロックも `collectTextureNames()` 抽出。パケットワイヤ順不変。
- [ ] **E-4 作業モード**: 5 モード＋behavior の「接近＋recalc カウントダウン」定型を `ModeNavigation.approachOrStop()`／`RecalcWalker` へ。`BlockFinder.searchTargetBlock`↔`BlockFinderPD.tick` の BFS 二重実装を `BlockFinderPD` 単一ソース化。ドメインロジック（治癒/醸造/strafe）は共通化しない。
- [ ] **E-5 Behavior 基底**: 13 behavior 共通の `IS_WAITING VALUE_ABSENT` コンストラクタ・`canStillUse` フットガン・`WALK_TARGET`+`EntityTracker` 構築を `AbstractMaidBehavior` 基底＋`walkTo()` へ（§C の `AbstractMaidModeBehavior` もここに乗せる）。
- 対象外（監査確認済）: `LMNConfig` 二相パターン／`data/LM*Provider`／`resource/manager` 小文字キー Map（差異実質的）／`resource/loader`（Strategy 済・保護コア B 隣接）／`entity/util` の interface+Impl（意図的分割）。

### §F — LittleMaidEntity の分割 … → HOWTO §F（中リスク）

- **現状**: `entity/LittleMaidEntity.java` は **1934 行・10 interface 実装**で最大ファイル（次点の約 3 倍）。既存委譲: `LMSafeMovement`/`LMInteractionHandler`/`LMHasInventory`/`LMItemContractable`/`HasModeImpl`/`MaidResurrection`/`BookParameterParser`/`TargetTagManagerImpl`/`TargetingSystem`/`MultiModelCompound`/`SoundPlayableCompound`/`MaidLookControl`。
- [ ] 未委譲クラスタをコンポーネント抽出（1 クラスタ＝1 コミット）:
  - 戦闘 → `MaidCombat`（`doHurtTarget`/`hurtServer`/`performRangedAttack`/クロスボウ/`hurtArmor`/`hurtHelmet`/`killedEntity`/`canAttack`/`getProjectile`）
  - 加速 → `MaidAcceleration`（`getTickMultiple`/`setAccelerationTicks`/`dec…`/`get…`/`isAcceleration`/`inTickMultiplePre`/`Post`）
  - 環境音・演出 → `MaidVoice`/`MaidParticle`（`playAmbientSound`/`die` ボイス/`handleEntityEvent` 粒子/`showFreedomParticle`/`showTracerParticle`）
  - 個体差初期化 → ヘルパ（`setRandomTexture`/`setRandomVoice`）
  - multimodel/sound 委譲 → §E の common ホルダで解消
- 注意: `@Override`（特に `super` 呼び出し・バニラ protected override）は本体に残し中身のみ委譲（CLAUDE.md 方針）。

---

## 🩹 残課題（§C 設計決定後に再判断）

- [ ] `MaidCombatBehavior` の cooldown・`MaidHealerBehavior` の index 等の内部状態を `PersistentMaidBehavior.saveBehaviorData` で永続化するか判断（挙動が変わる修正のため §C-2 本体とは分離。§C-3 全廃後に実体が明確になってから着手）。
- [ ] 移動モード enum 値名 `FREEDOM`/`ESCORT`/`TRACER` を `IDLE`/`FOLLOW`/`GUARD` 等へ改称する場合は、lang / DataGen / 描画 caps（`caps_isFreedom` 等）/ 本パラメータの同時更新が必要（ADR-0002 で見送り済み・任意）。

---

## 📋 機能バックログ

| 優先度 | タグ | 項目 |
|---|---|---|
| 高 | feature | 醸造モード（醸造台を使うモード） |
| 中 | feature | インベントリを開いている間は動きを止める（QOL） |
| 中 | feature | 装飾品スロットの拡張（現状は頭のみ） |
| 中 | feature | ModelCaps 未実装箇所の実装 |
| 中 | feature | LivingVoiceRate 実装 |
| 中 | feature | 潜水能力 / 好感度 / メイドさんのグループ分け |
| 中 | problem | 連続発声問題（射手・明かりモード等での重複発声） |
| 中 | problem | 大量 Mod マルチ環境での安定性改善（Sensor 最適化） |
| 中 | problem | 経験値瓶にガラスが大量に必要 |
| 低 | feature | 利き手設定 / 本で一括設定 / 体力増加 / 成長要素 / 農業モード |
| 低 | feature/original | Ripper 隠し機能 / 糸 / ポーション等付与 / TNT / 弓と火打ち石 |

---

## 🔍 実機検証チェックリスト（リリース前・保護コア回帰）

### 🎮 runClient / runServer
- [ ] `runClient` が起動しクラッシュしない。
- [ ] メイドさん右クリックで `LittleMaidScreen`（インベントリ/防具/手持ち）が正常表示（GUI 高さズレ無し）。
- [ ] 各ボタン動作（ターゲットタグ／サウンドパック選択／モデル選択／移動モード切替・吸血トグル／メイドさん管理／お仕事スロット数）。
- [ ] `ModelSelectScreen`/`SoundPackSelectScreen` のスクロール・フィルタ検索。GUI 内プレビューのマウス追従。マウスクリック判定のズレ無し。
- [ ] 全モデル（SR2/AC/RX0/Steve 等）・防具・手持ち・頭部装飾が正常描画。
- [ ] config 競合なし生成（`littlemaidneo-common.toml` / `saves/<world>/serverconfig/littlemaidneo-server.toml`）。

### 📦 互換性・ネットワーク
- [ ] 既存セーブのロードで NBT エラーが起きない（キー名互換）。
- [ ] 外部 LMM/MMM モデルパック（.class）が ASM リマップで読み込める（保護コア A）。
- [ ] 外部ボイスパック（.cfg + .ogg）が読み込め再生される（保護コア B）。
- [ ] マルチ接続でメイドさんのスポーン同期パケット等が同期される。
- [ ] クロスボウ発射動作（`MixinCrossBowItem` 経由）。

---

## ⛔ 本リファクタの対象外（明示）

- 描画 Blaze3D/Core Shader 本体移行（ADR-0001・保護コア A）。P-1〜P-6 以降の構造課題は効果限定・高リスクのため対象外。
- `resource/classloader/`・`maidmodel/`・`GLCompat`・`EntityLittleMaid` スタブ。
- 保護コア B（ボイスパック形式・`LMSounds`・探索パス・同期パケット形式）。
- 移動モード旧 byte 形式の後方互換（ADR-0002 で非対応宣言済み）。
- Forge Energy（概念非該当）。
