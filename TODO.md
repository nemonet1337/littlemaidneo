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

### §A — Goal の AI(Brain)化 … ✅ ほぼ完了（HowTo 不要）

- **進捗**: カスタム Goal は全廃。行動は Brain Behavior（`entity/ai/behavior/Maid*Behavior` 13 種）で駆動。`entity/goal/` パッケージ・`ModeWrapperGoal` 削除済み（ソース参照ゼロ）。`Float`→`Swim` / `OpenDoor`→`InteractWithDoor` も Behavior 化。
- **意図的に残置（機能的問題なし・あえて Goal）**: `LittleMaidEntity#registerGoals()` の `AvoidEntityGoal`/`PanicGoal`/`LookAtPlayerGoal`×2/`RandomLookAroundGoal`（視線は `LOOK_TARGET` プロデューサ不在のため Behavior 化しない設計判断）。`MultiModelEntity#registerGoals()` の `FloatGoal`+`LookAtPlayerGoal`×2（ダミー表示エンティティ）。
- [ ] **（任意・低）ADR と実装の差異是正**: ADR-0003 は `CORE/FIGHT/WORK/IDLE` の 4 Activity を記すが、実装は **全 Behavior が CORE に一括**で Activity 分割は未実装。挙動は正常なので、ADR を実態に合わせるか分割を実装してから ADR を正とするか、どちらか（任意）。

### §B — デッドコード削除 … → HOWTO §B（低リスク・即効）

- [ ] `setup/ClientSetup.java`（空 `init()`・参照ゼロ）をクラスごと削除。
- [ ] `entity/util/MaidMode.fromName(String)`（呼び出しゼロ・`byName` の重複）削除。
- [ ] `api/mode/ItemMatchers` の `item(Item)`(@Deprecated)/`name(String)` ＋ private record `ItemInstance`/`NameMatcher`（全て呼び出しゼロ）削除。
- [ ] `api/mode/ModeType.Builder.addItemMatcher(ItemMatcher)` 単一引数版(@Deprecated・呼び出しゼロ)削除。
- [ ] `mixin/MixinPlayerEntity` の空 `@Inject` 2 件（`<init>` / `stopSleepInBed`）削除（§D と同時でも可）。
- [ ] （要確認）`util/BlockFinder` の `findTarget`/`findHorizonPos`/`findLayer`/`findHorizon`（未使用の疑い・`//多分動かん`）— call-site 再確認のうえ削除。
- [ ] （任意・低）単一実装マーカー interface のインライン化: `HasMaidMode`/`Contractable`/`HasMode`/`GuiEntitySupplier`（polymorphic 利用ゼロ）。`HasInventory`/`SalaryBoxPosListener`/`LMCollidable`/`ProcessDivider` は load-bearing で残す。
- [ ] stale ドキュメント是正: `CLAUDE.md:16` の `entity/goal/ — AI Goal`（削除済みパッケージ）／`CLAUDE.md` の `ClientSetup` 言及。

### §C — 各 Mode の Behavior 化 … ✅ 完了（wrapper 方式）→ HOWTO §C（新規 Mode 追加手順）

- **進捗**: 6 モード（`Combat`/`Cooking`/`Healer`/`Pharmcist`/`Ripper`/`Torcher`）は `Mode` サブクラスのまま、単一 `MaidWorkModeBehavior`（CORE）が `ModeManager` 選択中（Priority 降順）の `Mode` へ委譲。`caps_job` 契約・外部パック互換のため個別 Behavior 化は **行わない**（ADR-0002/0003 の設計判断）＝完了扱い。
- 新規モードは `Mode` サブクラス＋`Mode.ENTRIES` 登録＋`ItemMatcher`＋タグ＋lang で追加（Brain 側無改修）。手順は HOWTO §C。

### §D — Mixin の整理・脱 Mixin … → HOWTO §D（中リスク）

- **KEEP（バイトコード必須）**: `MixinExperienceOrbEntity`/`MixinItemEntity`/`MixinRangedWeaponItem`/`MixinAbstractFurnaceBlockEntity`(getRecipeType_LM)/`MixinPlayerEntity`(positionRider/onPassengerTurned override)。
- [ ] `MixinServerPlayerEntity`（6 `@Inject`・状態フィールドなし）→ NeoForge イベント（`PlayerEvent.Clone`/`PlayerTickEvent.Pre`/睡眠/セーブ）へ。ファイルごと撤去候補。
- [ ] `MixinCandleCakeBlock`（復活儀式）→ `UseItemOnBlockEvent` へ。ファイルごと撤去候補。
- [ ] `MixinCrossBowItem` → `MixinRangedWeaponItem` に `instanceof CrossbowItem` 分岐で統合（Mixin 1 件減）。
- [ ] `mixin/CrossbowItemInvoker`（実は Mixin ではない・誤配置）→ `entity/util/`（`CrossbowSpeedUtil` 等）へ移設 or インライン。
- [ ] （任意）`MixinAbstractFurnaceBlockEntity.isBurningFire_LM` → ブロックステート `LIT` 由来にして `@Shadow` 削減。
- 注意: Mixin 撤去時は `littlemaidneo.mixins.json` の登録も同時削除。命名 `_LM`/`_LMRB` 混在は `_LM` へ寄せると一貫（任意）。

### §E — common/ パッケージ切り出し … → HOWTO §E（低〜中リスク）

- [ ] `LittleMaidEntity` と `MultiModelEntity` が重複する `IHasMultiModel`(13)＋`SoundPlayable`(3) の委譲 ~16 メソッドを共通化。
- 方針: 親クラスが異なり（`TamableAnimal` vs `PathfinderMob`）基底共有不可 → `entity/common/MultiModelHolder`・`entity/common/SoundHolder`（default メソッド付きホルダ interface）を新設。両エンティティは `getMultiModel()`/`getSoundCompound()` のみ実装し本体メソッドを削除。`IHasMultiModel` の polymorphic 利用（3 実装・25 ファイル）は不変。
- [ ] 併せて切り出し: スポーン同期（`writeSpawnData`/`readSpawnData`）の multimodel/sound 部、テクスチャ/ボイス初期化。

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

## 🩹 残課題: モード状態 NBT の永続化（§C と並行・挙動変化を伴うため別管理）

- [ ] 一部モードの内部状態（Combat の cooldown、Healer の index 等）が未永続化でリロード時に挙動が変わる。Codec 基盤で永続化を検討（挙動が変わる修正のため記述量削減リファクタとは分離して扱う）。
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
- [ ] config 競合なし生成（`littlemaidneo-lmml-common.toml` / `saves/<world>/serverconfig/littlemaidneo-server.toml`）。

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
- 各 `Mode` の個別 Behavior 化（`caps_job` 契約保護のため wrapper 方式を採用・§C）。
- Forge Energy（概念非該当）。
