# TODO

## 高

- **旧モデルジオメトリの忠実移植（ModelPart 移行 Phase 3 の残作業)**
  - 4ce5eb5 でのモデル変換は簡易スタブ化されており、複雑モデルの造形が失われている（これが「大型メイドモデルの見た目がおかしい」根本原因）。
  - 移植元は `git show 4ce5eb5~1:apps/modelloader/.../maidmodel/{Model}.java`（GLCompat 版）。
  - 2026-07-25 移植済み: **Beverly7 / Chloe2**（二節腕・二節脚・多段スカート・ポニーテール含む全身）、**Aug**（頭部差し替え＋プレート・センサー）、**SR2/Aug のまばたきオーバーレイ非表示化**。
  - 残り: RX0 405→107, Classic64 323→87, Steve 301→94, Archetype 285→96, Elsa5 353→94（Elsa5 は目視 OK 報告あり・優先度低）。
  - 1 モデルずつ移植 → `runClient` 目視確認 → コミット（計画書 §8 Phase 3 の手順に従う）。
- **未移植の動的アニメーション**（移植済みモデルの静的ポーズ化した箇所）:
  - （まばたきは 2026-07-25 に全対象モデルへ移植済み: Beverly7/Chloe2=`applyBlink(0.20)`, Elsa5=`applyBlink(0.16)`, SR2/Aug=`applyBlinkSlow`。SR2/Aug の弓構え時に利き目側を表示する演出のみ未移植）
  - Aug のセンサー揺れ（sensor1〜4、体力連動）・サイドテールの頭部追従（head.xRot * -0.667）
  - Beverly7/Chloe2 のポニーテール/お団子の頭部追従、乗馬・スニーク時の二節肢固有ポーズ
  - （肘・膝の歩行スイングは 2026-07-25 に `LMModel.applyElbowKneeSwing` として移植済み）

## 中

- **持ち物ハンドアンカーのモデル別調整**: LMHeldItemLayer は旧ベースモデルの Arms オフセット (∓1, 5, -1)/16 を全モデル共通で使用中。モデル移植時に旧 `Arms[n]` の rotation point を各モデルのハンドアンカーパーツ（例: `arm/hand` 子パーツ）として復元し、レイヤーはそれを参照する。
- **共有モデルインスタンスの遅延描画レース**: LMModelManager のモデルは全メイド共有のシングルトン。同一フレームに同一モデルの複数メイドが映ると、submitCustomGeometry の遅延ラムダ実行時点では最後に setupAnim されたポーズが全員に適用される可能性がある。複数体で確認し、必要ならポーズのスナップショット化 or エンティティ毎インスタンス化を検討。

## 低

- 発光ライト値 0xF00000 → 0xF000F0 (LightTexture.FULL_BRIGHT) 修正の検討（移行計画 §7 のスコープ外事項）
- 鎧 glint 描画（ArmorRenderState.glint は保持済みだが未描画）
