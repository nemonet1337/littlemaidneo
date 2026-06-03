# resources 整理と PR#10 部分マージの是正

- 日付: 2026-06-03
- 種別: research / chore
- 関連: PR #10（`claude/jolly-curie-Hwwgf`）、ベースmodテクスチャ補完（main `e630919`）

## PR #10 が部分マージだった件（重要）

`origin/main` を調査したところ、PR #10 はブランチ HEAD が **コミット `25bf180`（第1弾）時点**でマージされていた（merge commit `e40d91e`）。
そのため、後続コミットの内容が **main に入っていない**：

- `e49f4dc`: **`canStillUse` 追従修正**（`MaidFollowOwner/Freedom/Stare/Wait`）＋ ESCORT ゲート ← 追従の本当の修正
- `e8946a4`: resources クリーンアップ（旧複数形 data の削除・structure 移動）

結果、main では：
- 移動駆動の `PATH` 登録・モード装備・待機即時同期・視線確率・スポーンエッグ・UI 名前空間/塗りハートは入っている（`25bf180`）。
- だが **`canStillUse` 欠落のため追従/徘徊/注視の `tick()` が呼ばれず、依然 follow が機能しない**。
- 冗長な旧形式 data も復活している。

## 本ブランチ（`claude/ui-fix-and-resource-cleanup`）での対応

1. `canStillUse` 追従修正＋ESCORT ゲートを 4 Behavior へ再適用（main から欠落していたため）。
2. resources クリーンアップを再適用（旧複数形 data 削除・`structures/`→`structure/`・`.xcf` 除去）。
3. ユーザー依頼により削除：
   - 未登録アイテムの orphan モデル/テクスチャ（`models/item/maid_*` 等、`textures/items/`）
   - バニラ防具テクスチャの全体上書き（`assets/minecraft/textures/models/armor/*`）
4. UI 崩れの継続調査（別記）。

## UI 崩れの継続調査メモ

main には GUI 名前空間修正（`littlemaidneo`）・実テクスチャ（`littlemaidinventory2.png` = blob `aaff825`、本物）・塗りつぶしハートが揃っているが、ユーザー報告では UI が依然崩れている。
`LittleMaidScreen` の `extractContents`/`extractRenderState`/`extractLabels` は全て `@Override` 付きで、他 `Screen` 系画面と同じ `GuiGraphicsExtractor` パターンを使用しており、コンパイル・実行も通っている（＝シグネチャ・override は妥当）。

→ 構造的な誤りではなく、座標/スケール/描画順など視覚面の問題が疑われる。正確な切り分けには現行ビルドでの画面スクリーンショットが必要。
