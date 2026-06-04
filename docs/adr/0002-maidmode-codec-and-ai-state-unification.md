# ADR 0002: MaidMode enum + Codec によるモード状態シリアライズの統一

- ステータス: 採用（第1〜4段階を実装済み・各段階 CI(Java25) で検証）
- 日付: 2026-06-04
- 関連: ADR 0001（描画ブリッジ維持）, `docs/plan/2026-06-01_統合リファクタリングプラン.md`（Phase 7 Brain 化）

## 背景

「Mode と Goal に AI を導入したが統合が甘い」という指摘を受け、現状を調査した。
理想形として「単一 enum + `Codec` でモードを表しシリアライズする」機構（`MaidMode` 相当）が
導入されているかの確認が求められた。

### 調査で判明した現状（統合が甘い理由）

メイドさんの「モード」概念は **2 つの無関係な系統に分裂** していた。

1. **移動モード `MovingMode`**（`entity/util/MovingMode.java`）
   - `FREEDOM` / `ESCORT` / `TRACER` の enum。`EntityData`(byte) 同期 + NBT は手書き byte 変換。
   - **Codec なし**（`fromId`/`fromName` の手書きシリアライズ）。
   - AI 実装は **Brain Behavior** に分散：`MaidFollowOwnerBehavior`(ESCORT) /
     `MaidFreedomBehavior`(FREEDOM/野良徘徊) / `MaidWaitBehavior` / `MaidStareBehavior`。
   - ただし **TRACER だけは Brain ではなく GoalSelector の `RedstoneTraceGoal`** にあり非対称。

2. **作業モード `Mode`**（抽象クラス・`api/mode/`）
   - Cooking / Fencer / Archer / Healer / Torcher / Ripper / Pharmcist の 7 種。
   - `ModeManager`（`Identifier` レジストリ）+ NBT 文字列（`ModeID`）。**Codec なし**。
   - AI 実装は GoalSelector の単一 `ModeWrapperGoal` が `Mode#tick()` を回すブリッジ方式。

つまり、理想形の「単一 enum + Codec」機構は **未導入** で、移動＝Brain・作業＝Goal と
実行基盤がバラバラ、シリアライズも byte / Identifier 文字列とバラバラだった。

### 重要な観察：移動モードと作業モードは「直交軸」

メイドさんは「ご主人様を追従(ESCORT)しながら料理(Cooking)する」のように、
**移動モードと作業モードを同時に持つ**。両者は本質的に直交する 2 軸であり、提示例のように
1 つの enum へ畳み込むと表現力が落ちる（追従しながら戦闘、等が表せなくなる）。
したがって「より高度な AI」は、両軸を **それぞれ現代化** しつつ一貫した基盤に載せる方向が正しい。

## 決定

### 第1段階（本コミットで実装）

移動軸を担う enum を `MovingMode` → **`MaidMode`** に改名し、
要望の中核である **`Codec`（ワールド保存）と `StreamCodec`（ネットワーク同期）** を導入した。

- `MaidMode.CODEC` = `Codec.STRING.xmap(byName, getName)`。
  - 永続化を `ValueOutput#store("MaidMode", MaidMode.CODEC, …)` /
    `ValueInput#read("MaidMode", MaidMode.CODEC)` に置換（手書き byte 変換を廃止）。
- `MaidMode.STREAM_CODEC` = `ByteBufCodecs.VAR_INT.map(...)`。
  - `C2SSetMovingStatePayload` の手組み StreamCodec を `MaidMode.STREAM_CODEC` に置換。
- `EntityData`(byte) 同期は VarInt と等価の `getId()`/`fromId()` をそのまま用いる。

#### 不変として守ったもの（破壊しないことの根拠）

- enum の**値名**（`FREEDOM`/`ESCORT`/`TRACER`）と `getName()` の戻り値は、
  - 表示用 lang キー `state.littlemaidneo.<Name>`、
  - 外部モデルパックが参照する描画 caps `caps_isFreedom` / `caps_isTracer`（保護コア隣接）、
  - 本パラメータ `moving=...`（`BookParameterParser`）
  と結合しているため **不変** とした。型名と機構のみ刷新している。
- 旧ワールドの保存形式（byte の `MovingMode`）との後方互換は、合意のうえ **非対応**（新形式のみ）。

### 第2〜4段階（実装済み・利用者合意のうえ実施）

本環境は **JDK 25 不在 + foojay 解決がネットワーク制限** のため `./gradlew` でのローカル
コンパイル検証が不可能で、検証は push 後の CI（Java 25）が唯一の手段。各段階を個別コミットし
CI green を確認しながら適用した。

- **AI-2**: 作業モード `Mode` 側に `ModeManager.CODEC`（`Identifier` 文字列・BiMap 裏付け）を導入し、
  `HasModeImpl` の永続化を `ModeID` 文字列 → `ValueOutput#store`/`ValueInput#read`(Codec) へ統一。
- **AI-3**: `RedstoneTraceGoal`(TRACER) を `MaidTraceBehavior`(Brain) へ移植し、全移動モード
  （ESCORT/FREEDOM/TRACER）を Brain に一元化。移動は直接 navigation を操作し旧 Goal の挙動を厳密維持。
- **AI-4**: 戦闘系 Fencer/Archer を単一 `CombatMode` へ統合。武器種に応じて内部で近接/射撃スタイルを
  動的に切り替える（既存 `FencerMode`/`ArcherMode` を内部ストラテジとして再利用）。
  - 毛刈り（Ripper）は敵対ターゲットと戦う「戦闘」ではなく受動的な刈り取り作業のため統合対象外とし、
    独立モードのまま残した（機能の意味的整合を優先）。
  - 外部モデルパック互換: `caps_job` の job 名契約を守るため `Mode#getJobName()` を追加し、
    `CombatMode` はアクティブスタイルに応じ従来の `"fencer"`/`"archer"` を返す。
    `getBattleModeType()` も SWORD/BOW を返し `TargetingSystem` の挙動を維持。

### 既知の残課題

- 戦闘サブモードの内部状態（cooldown 等）は従来同様 Codec 永続化していない（リロードで挙動が変わるため別扱い）。
- enum 値名 FREEDOM/ESCORT/TRACER の改称（IDLE/FOLLOW/GUARD 等）は lang/DataGen/描画 caps/本パラメータの
  同時更新が必要なため見送り（要望の中核は型名＋Codec 機構であり充足済み）。

## 影響

- `entity/util/MovingMode.java` → `MaidMode.java`、`HasMovingMode.java` → `HasMaidMode.java` に改名。
- メソッド名 `getMovingMode`/`setMovingMode` → `getMaidMode`/`setMaidMode`（全呼び出し側を一括更新）。
- 既存の表示・UI・本パラメータ・描画 caps の挙動は不変。
