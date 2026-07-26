# sistr_TODO 高優先調査・対応メモ（2026-07-26）

## 1. ガード実装

### 原因
- 戦闘 AI（`MaidCombatBehavior.MeleeStyle`）には盾を構える処理があったが、`ShieldItem` 固定判定＋大量の `[ShieldDebug]` ログが残存。
- MC 26+ では盾は `DataComponents.BLOCKS_ATTACKS` で判定され、ダメージ軽減は `LivingEntity.getItemBlockingWith()` 経由。
- `LittleMaidEntity.isBlocking()` が `ShieldItem` 専用 override になっており、バニラの block delay / コンポーネント判定と乖離していた（描画・hurt_guard 音と実ダメージ軽減の不一致リスク）。

### 対応
- `isBlocking` override を削除しバニラ実装に委譲。
- 盾検出を `BLOCKS_ATTACKS` コンポーネントに変更（オフハンド優先）。
- デバッグログ削除。接近中／攻撃 CD 中に盾を構える流れを整理。

## 2. 赤石検知中の迷子

### 既知の既往
- `9da38d2` で `createPath(pos, 0)` → `accuracy=1` に変更済み（信号源ブロック直上への到達不能回避）。

### 残っていた問題
- 経路が存在しない信号でもスコア最良を選び `moveTo` 失敗→停止のループになり得た。
- 経路進行中にスタックしても再計画せず `canStillUse` が長く true のままになり得た。

### 対応（`MaidTraceBehavior`）
- スコア順に見て **`createPath` が取れ `canReach()` な候補**だけを採用。
- 約 2 秒位置不変でスタックとみなし navigation 停止→再計画。
- 直前失敗ターゲットの一時除外。到達可能な候補が無い場合はクールダウン延長。

## 3. マルチ時の安定性

### 特定した問題
1. **`FMLEnvironment.getDist() == CLIENT` をエンティティ名表示で使用**  
   統合クライアント（SP ホスト）では Dist が常に CLIENT のため、サーバー側ロジックから `ClientScreenHelper` に触れ得る。`level().isClientSide()` に変更。
2. **`MaidManager` の stale entityId**  
   セーブに entityId を残し、再ログイン後も `isLoaded() == true` になり得た。参照が無い／死亡時は `entityId=-1` に落とし、`isLoaded` は実体参照＋`isAlive` のみ。ログアウト時にも `checkMaidUnload`。
3. **SyncSoundPack 所有者チェック欠如**（前コミットで対応済み）
4. **S2C 追跡送信の不統一**（前コミットで対応済み）

### 未解決・要継続
- 原因不明の「マルチでたまに不安定」は再現手順が無い限り網羅調査が困難。チャンクロード境界でのメイド登録遅れ、Brain 同期、インベントリ開閉中の移動などは個別に再現ログが欲しい。

## 4. HiFM 氏 KMExtend 描画バグ

### 経緯
- 旧 LMRB フォーラム既知: KMExtend の一部モデル（例: ROni）で描画異常。
- ModelPart 移行計画（`docs/plan/2026-07-15_ModelPart移行計画.md`）により **外部 `.class` モデルパック互換（保護コア A）は廃止方向**。現行ツリーに `MultiModelClassLoader` / `MultiModelClassTransformer` は存在せず、ビルトイン 13 モデルのみ。

### 結論
- KMExtend（外部 class パック）由来の描画バグは **現行アーキテクチャでは再現対象外**（パック自体がロードされない）。
- 同等モデルをビルトイン移植する場合は個別のジオメトリ移植課題として扱う。sistr_TODO 上は「アーキテクチャ変更により対象外」と記録。
