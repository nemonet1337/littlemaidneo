# LittleMaidNeo — 未完 TODO / 開発ロードマップ

> 更新日: 2026-05-30
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合

---

## 🔍 検証・テスト用チェックリスト (未完了)

### 🎮 実機検証 (runClient / runServer)
- [ ] `./gradlew runClient` が起動し、クラッシュしないこと。
- [ ] メイドさんを右クリックして `LittleMaidScreen` (インベントリ、防具、手持ちスロット等) が正常に表示され、GUI高さ(208)がズレていないこと。
- [ ] 各種ボタンの動作:
    - [ ] ターゲットタグ設定ボタン
    - [ ] サウンドパック選択ボタン
    - [ ] モデル選択ボタン
    - [ ] 移動モード切替 / 吸血トグルの切替
    - [ ] メイド管理ボタン
    - [ ] お仕事スロット数設定
- [ ] `ModelSelectScreen` / `SoundPackSelectScreen` でのリストスクロール、テキストフィルタ検索が正常動作すること。
- [ ] GUI内のメイドさんプレビューがマウス追従して描画されること。
- [ ] マウスクリック判定のズレ（`mouseClicked` 移行による座標系への影響）がないこと。
- [ ] モデル描画 (SR2, AC, RX0, Steve等含む全モデル) および防具、手持ち、頭部装飾が正常に表示されること.
- [ ] `config/` フォルダ以下に `littlemaidneo-common.toml` および `littlemaidneo-lmml-common.toml` が競合せず生成され、各設定項目が反映されること。

### 📦 互換性・ネットワーク検証
- [ ] 既存セーブデータのロード時に NBT 読み込みエラーが起きないこと（NBTキー名の互換性維持）。
- [ ] `LMMLResources` 等に配置した外部 LMM/MMM モデルパック (.class 形式) が ASM リマップにより正常に読み込めること。
- [ ] マルチプレイ接続時に、メイドさんのスポーン同期パケット等が正常に動作し同期されること。
- [ ] `mixins.json` 未登録 of `CrossbowItemInvoker` が `LittleMaidEntity` から参照されている件について、クロスボウ発射動作に不具合がないか。必要があれば Mixin への追加を行う。

---

## 📋 機能バックログ (原作未実装・新規開発要素)

| 優先度 | タグ | 項目 |
|---|---|---|
| 高 | feature | 醸造モード（醸造台を使うモード） |
| 高 | bug | 赤石検知中に迷子になる |
| 高 | other | ドキュメント整備 |
| 中 | feature | インベントリを開いている間は動きを止める（QOL） |
| 中 | feature | 装飾品スロットの拡張（現状は頭のみ） |
| 中 | feature | 鯖蔵コンフィグの同期（手動コピー不要に） |
| 中 | feature | ModelCaps 未実装箇所の実装 |
| 中 | feature | LivingVoiceRate 実装 |
| 中 | feature | 潜水能力 / 好感度 / メイドのグループ分け |
| 中 | problem | 連続発声問題（射手・明かりモード等での重複発声） |
| 中 | problem | 大量Modマルチ環境での安定性改善 |
| 中 | problem | 経験値瓶にガラスが大量に必要 |
| 低 | feature | 利き手設定 / 本で一括設定 / 体力増加 / 成長要素 / 農業モード |
| 低 | feature/original | Ripper隠し機能 / 糸 / ポーション等付与 / TNT / 弓と火打ち石 |

---

## 🛠️ ソースコード中のTODO

ソースコード内に残されている `TODO` コメントのリストです。

### 💻 クライアント / 画面関連
- [LittleMaidScreen.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java)
  - [x] [L29](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java#L29): モード名表示/移動状態をアイコンで表記する
  - [x] [L30](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/client/screen/LittleMaidScreen.java#L30): ストライキ時の表示改善

### 🧠 目標 / AI (Goal)
- [LMStoreItemToContainerGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java)
  - [x] [L52](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java#L52): チェストに仕舞うときの演出を強化する
  - [x] [L53](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMStoreItemToContainerGoal.java#L53): チェストに仕舞わない条件を追加する
- [LMTargetGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMTargetGoal.java)
  - [x] [L37](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/LMTargetGoal.java#L37): コンフィグ化
- [RedstoneTraceGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java)
  - [x] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L21): 180度ターン時に首がグリッとなるのがこわいので挙動を修正
  - [x] [L22](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L22): この状態では自由行動の起点が最後に検知した赤石動力付近に再設定されます。
  - [x] [L23](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/RedstoneTraceGoal.java#L23): 処理のリファクタリング
- [WaitWhenOpenGUIGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/WaitWhenOpenGUIGoal.java)
  - [x] [L11](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/WaitWhenOpenGUIGoal.java#L11): 実装する

### 👤 メイド本体 (Entity)
- [LittleMaidEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java)
  - [x] [L135](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L135): 声タイミング調整
  - [x] [L136](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L136): ドロップアイテム
  - [x] [L137](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L137): 契約期間の残りと砂糖をあげた時の音符の色を対応させる。
  - [x] [L138](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L138): 雪バイオームで雪合戦させる、日が暮れると終わるように
  - [x] [L139](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L139): モードトリガーアイテム指定
  - [x] [L140](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L140): 署名済みではない書き込み可能な本にパラメータを記述して、メイドさんに右クリックで使用すると値が反映されるように
  - [x] [L141](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L141): メイドさんも金リンゴや牛乳を飲めるように
  - [x] [L142](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L142): つまみ食い
  - [x] [L143](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L143): ダメージ/水没待機解除 実装済みだっけ？
  - [x] [L144](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L144): トランザム機能追加
  - [x] [L145](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L145): 経験値追加
  - [x] [L146](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L146): 座ったメイドでも追従時に立つようにする
  - [x] [L147](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L147): スト時砂糖ドカ食い機能
  - [x] [L148](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L148): GUIを開いている時に動きを止める
  - [x] [L149](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L149): リスポーン機能
  - [x] [L150](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L150): 死亡メッセ追加
  - [x] [L151](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L151): はしごを使えるように
  - [x] [L152](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L152): おさわり厳禁：他人のメイドに触ると殴られる
  - [x] [L153](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L153): 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
  - [x] [L180](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L180): enumにまとめる
  - [x] [L244](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L244): クラス化検討
  - [x] [L259](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L259): クライアント側のこの値は信用ならない
  - [x] [L298](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L298): メイドさんに付与する属性の再考
  - [x] [L310](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L310): コンフィグでスポーン条件を設定可能にする
  - [x] [L382](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L382): パーティクル演出の強化
  - [x] [L639](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L639): 頭の装飾品をチェストに仕舞わないようにする
  - [x] [L889](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L889): IdFactorが確実にセットされたタイミングで実行されるようにする
  - [x] [L1169](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1169): スポーン条件をコンフィグで設定可能にする
  - [x] [L1190](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1190): マウント系の位置を調整
  - [x] [L1239](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1239): ボイス周りの調整、コンフィグ化
  - [x] [L1312](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1312): 強制再生メソッドを生やす
  - [x] [L1357](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1357): 処理の改善
  - [x] [L1398](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1398): 処理の改善
  - [x] [L1507](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1507): try/catchを挟む。処理の改善
  - [x] [L1514](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1514): `Holder<Enchantment>` を取得してチェックする
  - [x] [L1573](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1573): 弾道調整(archerShootVelocityFactor)が必要な場合 performCrossbowAttack をオーバーライドする
  - [x] [L1578](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1578): コメントを差す
  - [x] [L1811](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1811): 複数モデルで問題ないかチェックする
  - [x] [L1823](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1823): 処理の見直し、処理を追加可能に
  - [x] [L1824](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L1824): 使用アイテムをコンフィグから追加可能に
  - [x] [L2101](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2101): 計算式の改善
  - [x] [L2142](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2142): どこで使われるメソッドかわからん、使われてない or 代替可能なら消す
  - [x] [L2156](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2156): 処理の改善
  - [x] [L2228](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2228): IdFactorの仕様の改善
  - [x] [L2490](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2490): 暫定でテイム済みのモブは攻撃対象から外す
  - [x] [L2491](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2491): TODO
  - [x] [L2623](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2623): 強制再生メソッドを生やす
  - [x] [L2624](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2624): 再生クールダウンをコンフィグ化
  - [x] [L2686](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidEntity.java#L2686): このクラス置く場所がここでいいのかチェック、間違っているなら代替可能なら削除、そうでない場合は正しい位置に移動
- [LittleMaidModelCaps.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidModelCaps.java)
  - [x] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/LittleMaidModelCaps.java#L21): インベントリの挙動を修正
- [MaidSoulEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java)
  - [x] [L170](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java#L170): エフェクト調整
  - [x] [L183](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/MaidSoulEntity.java#L183): 憑依ステータス効果

### ⚔️ モード (Mode)
- [ArcherMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/ArcherMode.java)
  - [x] [L33](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/ArcherMode.java#L33): 処理の見直し
- [FencerMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/FencerMode.java)
  - [x] [L21](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/FencerMode.java#L21): 相手が無敵時間中は殴らない
- [HealerMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/HealerMode.java)
  - [x] [L25](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/HealerMode.java#L25): 処理のリファクタリング
- [PharmcistMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/PharmcistMode.java)
  - [x] [L15](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/PharmcistMode.java#L15): 実装する
- [TorcherMode.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java)
  - [x] [L29](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java#L29): 処理の改善
  - [x] [L60](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/mode/TorcherMode.java#L60): blockFinder of TorcherMode 共通化

### 🔀 その他 / Mixin / タグ
- [MixinPlayerEntity.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/mixin/MixinPlayerEntity.java)
  - [x] [L32](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/mixin/MixinPlayerEntity.java#L32): TargetTagManagerはServerPlayer側で実装するべきかチェック
- [LMTags.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/tags/LMTags.java)
  - [x] [L14](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/tags/LMTags.java#L14): 判定をタグとコンフィグで行えるように仕様を調整
