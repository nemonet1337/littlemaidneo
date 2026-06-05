# ADR 0003: メイドAIの完全Brain化および残存GoalのBehavior移行

- ステータス: 採用
- 日付: 2026-06-05
- 関連: ADR 0002（MaidMode Codec化とAI状態統一）, `HOWTO.md` (WS1)

## 背景

ADR 0002 において、移動モード（`MaidMode`）の Codec 化および Brain 化（`MaidFollowOwnerBehavior`, `MaidFreedomBehavior`, `MaidTraceBehavior`）が実現されました。しかし、メイドさんの主要な行動要素である「作業モード（`Mode`）の実行」や「自己回復、給料回収、アイテム収納、雪遊び、ターゲット追従」などは、依然として `GoalSelector` による Goal 駆動（`ModeWrapperGoal` 等）として残存しており、AI の実行基盤が分裂していました。

AI の設計方針を統一し、1.21.x のモダンな Minecraft/NeoForge API に適合させ、挙動の安定性とデバッグ性を向上させるため、残存するすべてのカスタム Goal を Brain の `Activity` および `Behavior`（`BehaviorControl`）へと完全移行しました。

## 決定

### 1. Activity の体系化
メイドさんの行動優先度を明確にするため、以下の4つの Activity を定義し、`customServerAiStep` 内で優先度順にアクティベートする構造としました。
- **CORE**: 生存に関わる最優先行動（待機 `Wait`、追従 `FollowOwner`、緊急テレポート `EmergencyTeleport` などの生存移動）
- **FIGHT**: 戦闘行動（ターゲット認識、攻撃移動。`CombatMode` 等の武器による攻撃スタイル自動切替を含む）
- **WORK**: お仕事および自己維持（各種作業モードの `tick`、砂糖による自己回復、コンテナからの給料回収、アイテムのコンテナ収納、ドロップアイテム回収など）
- **IDLE**: 非お仕事時の自由行動（雪遊び、見回り、プレイヤーを見つめる等）

### 2. 作業モードの Brain 駆動化（`MaidWorkModeBehavior`）
従来の `ModeWrapperGoal` を廃止し、Brain の `WORK` アクティビティ内で実行される `MaidWorkModeBehavior` を新設しました。
- 既存の `ModeManager` と各 `Mode` の優先度判定（Priority 降順）、`ItemMatcher` 等のロジックはそのまま再利用・委譲する形としました。
- これにより、描画 caps（`caps_job` で返される fencer/archer 等の役割名）や、既存モデルパックが参照する API シグネチャ、動作ロジックに一切の破壊的影響を与えることなく、駆動基盤のみを Brain に統合しました。

### 3. 各種カスタム Goal の Behavior 化
以下の Goal を対応する Behavior として新規実装し、Brain に登録しました。
- `LMTeleportTameOwnerGoal` → `MaidEmergencyTeleportBehavior` (CORE)
- `LMHealMyselfGoal` → `MaidHealSelfBehavior` (WORK)
- `LMCollectSalaryFromContainerGoal` → `MaidCollectSalaryBehavior` (WORK)
- `LMStoreItemToContainerGoal` → `MaidStoreItemBehavior` (WORK)
- `LMMoveToDropItemGoal` → `MaidMoveToDropItemBehavior` (WORK)
- `PlaySnowGoal` → `MaidPlaySnowBehavior` (IDLE)
- `LMTargetGoal` (targetSelector) → `MaidTargetBehavior` (FIGHT/CORE)
  - ターゲットの選定や `CombatMode` への橋渡しは、既存の `TargetingSystem` および `TargetTagManagerImpl` をそのまま再利用し、戦闘対象の条件判定に一貫性を持たせました。

### 4. 最小限の Goal の残置と妥協点
バニラの等価な Behavior が存在しない、あるいは Goal として駆動する方が安全かつ簡潔である以下のバニラ Goal については、例外的に `GoalSelector` に残置することとしました。
- `FloatGoal`: 水中で窒息死しないための最優先の生存行動。
- `OpenDoorGoal`: メイドさんがドアを通り抜けるためのドア開閉制御。
これらの残置は、挙動の安定性を第一とする「保護コア」重視の設計判断です。

## 影響

- メイドさんの AI 構造がモダンな `Brain` 駆動に統合され、Goal 駆動との混在による実行順序のバグや競合が解消されました。
- 既存のモデル・テクスチャ・ボイス等の「保護コア」への破壊的影響は一切ありません。
- すべての既存 GameTest およびビルドが正常にパスすることを確認済みです。
