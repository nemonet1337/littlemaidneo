# ModelPart 移行後の描画不具合調査（武器の持ち方・大型モデル崩壊）

日付: 2026-07-25
対象: バニラ ModelPart 移行（`docs/plan/2026-07-15_ModelPart移行計画.md`）後の未コミット作業ツリー

## 症状

1. 手持ち武器・盾が頭上に浮いて描画される
2. 大型メイドモデル（Beverly7/Chloe2 系）の造形が崩れる・パーツが散乱する

## 特定した原因と修正

| # | 原因 | 修正 |
|---|---|---|
| 1 | `LMHeldItemLayer` が `arm.translateAndRotate` のみ適用。旧 `ModelRenderer.postRender` は親チェーン（main_frame→biped_torso→biped_neck→arm→Arms）を再帰適用していた | 親チェーン全体を順に適用し、旧 Arms 相当のハンドアンカーオフセット (∓1, 5, -1)/16 を追加 |
| 2 | `LMMultiModel.setupAnim` が delegate（選択中モデル）にも armor モデルにも転送されず、モデルが永遠にベイク時ポーズのまま | delegate と可視 armor モデル全てに `setupAnim(state)` を転送 |
| 3 | `LMModel.applyMaidPose` が `state.xRot/yRot`（度数）をラジアンとして頭に適用 | `Math.toRadians` で変換 |
| 4 | `setDefaultPose` が旧ベースモデルの回転点（pelvic.y=7, arm.y=1.6 等）をハードコードし、回転点の異なるモデル（Beverly7: arm y=-7.5, pelvic y=4 等）の階層を毎フレーム破壊 | `loadPose(getInitialPose())` でベイク時初期ポーズへリセットしてから歩行スイングのみ適用 |
| 5 | `LMArmorLayer` が (a) armorStates ループ内で全 4 部位を再描画し全身鎧を最大 16 回描画、(b) 旧 `showArmorParts` 相当の部位可視制御が皆無、(c) ポーズ未適用 | 部位ごとに 1 回、遅延ラムダ内で対象部位のみ可視化して描画→全可視に復元。ポーズは #2 で解決 |
| 6 | 13 モデルの `setupAnim` オーバーライドが skinRoot のみポーズし、armor 用 inner/outer ルートが未ポーズ | 共通実装を `LMModel.setupAnim` 基底へ移動（3 ルート全てをポーズ）し、13 個の同一オーバーライドを削除。RX0 の `biped_trunk` 階層は `BipedParts` のフォールバックで吸収 |

## 未解決（残作業 — TODO.md 参照）

- **モデルジオメトリのスタブ化**: 4ce5eb5 の変換で複雑モデルの造形が簡易スケルトンに置き換わっている（Beverly7 555→86 行、Chloe2 678→86 行等）。「大型メイドモデルの見た目がおかしい」の根本原因はこれで、旧 GLCompat 版（`4ce5eb5~1`）からの忠実移植が必要。
- 共有モデルインスタンス + submitCustomGeometry 遅延実行による同一フレーム複数体のポーズレース（要実機確認）。
