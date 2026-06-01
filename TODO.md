# LittleMaidNeo — 未完 TODO / 開発ロードマップ

> 更新日: 2026-06-01
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合
> 統合リファクタリングプラン（フェーズ0〜10）: `docs/plan/2026-06-01_統合リファクタリングプラン.md`
> 技術的負債と保護コアの詳細: `TODO_System.md`

このファイルは「実態と一致する単一バックログ」。各項目は統合プランのフェーズ番号にひも付ける。
完了した項目は削除する（履歴は不要）。

---

## 🔍 検証・テスト用チェックリスト（Phase 10 で消化）

### 🎮 実機検証 (runClient / runServer)
- [ ] `./gradlew runClient` が起動し、クラッシュしないこと。
- [ ] メイドさんを右クリックして `LittleMaidScreen` (インベントリ、防具、手持ちスロット等) が正常に表示され、GUI高さ(208)がズレていないこと。
- [ ] 各種ボタンの動作（ターゲットタグ設定／サウンドパック選択／モデル選択／移動モード切替・吸血トグル／メイド管理／お仕事スロット数設定）。
- [ ] `ModelSelectScreen` / `SoundPackSelectScreen` でのリストスクロール、テキストフィルタ検索が正常動作すること。
- [ ] GUI内のメイドさんプレビューがマウス追従して描画されること。
- [ ] マウスクリック判定のズレ（`mouseClicked` 移行による座標系への影響）がないこと。
- [ ] モデル描画 (SR2, AC, RX0, Steve等含む全モデル) および防具、手持ち、頭部装飾が正常に表示されること。
- [ ] `config/` 以下に `littlemaidneo-common.toml` および `littlemaidneo-lmml-common.toml` が競合せず生成され反映されること。

### 📦 互換性・ネットワーク検証
- [ ] 既存セーブデータのロード時に NBT 読み込みエラーが起きないこと（NBTキー名の互換性維持）。
- [ ] `LMMLResources` 等の外部 LMM/MMM モデルパック (.class) が ASM リマップで正常に読み込めること（保護コア A）。
- [ ] 外部ボイスパック (.cfg + .ogg) が読み込め再生されること（保護コア B）。
- [ ] マルチプレイ接続時に、メイドさんのスポーン同期パケット等が正常に同期されること。
- [ ] クロスボウ発射動作の確認（`MixinCrossBowItem` 経由）。

---

## 📋 機能バックログ（各フェーズに織り込み）

| 優先度 | タグ | 項目 | 担当フェーズ |
|---|---|---|---|
| 高 | feature | 醸造モード（醸造台を使うモード） | Phase 3 |
| 高 | bug | 赤石検知中に迷子になる | Phase 3 |
| 高 | other | ドキュメント整備（CLAUDE.md 実態化＝R-17） | Phase 0/10 横断 |
| 中 | feature | インベントリを開いている間は動きを止める（QOL） | Phase 2 |
| 中 | feature | 装飾品スロットの拡張（現状は頭のみ） | Phase 2 |
| 中 | feature | 鯖蔵コンフィグの同期（手動コピー不要に） | Phase 6 (R-16) |
| 中 | feature | ModelCaps 未実装箇所の実装 | Phase 10 |
| 中 | feature | LivingVoiceRate 実装 | Phase 10 |
| 中 | feature | 潜水能力 / 好感度 / メイドのグループ分け | Phase 6(基盤)/7(AI) |
| 中 | problem | 連続発声問題（射手・明かりモード等での重複発声） | Phase 3 |
| 中 | problem | 大量Modマルチ環境での安定性改善 | Phase 7 (Sensor 最適化) |
| 中 | problem | 経験値瓶にガラスが大量に必要 | Phase 10 |
| 低 | feature | 利き手設定 / 本で一括設定 / 体力増加 / 成長要素 / 農業モード | Phase 7/10 |
| 低 | feature/original | Ripper隠し機能 / 糸 / ポーション等付与 / TNT / 弓と火打ち石 | Phase 10 |

---

## 🛠️ ソースコード中の TODO コメント（Phase でインライン解消）

> 注: 以前の版ではこの一覧を `[x]` 済みと記していたが、実コードには未解消で残存していたため
> 実態に合わせて再掲する。各項目を担当フェーズで解消し、コメントも除去する。

### Phase 1（記述量削減と同時に解消）
- `LittleMaidEntity.java` L2376: 空の `// TODO` を解消 or 削除
- `LittleMaidEntity.java` L1426: 「コメントを差す」→ 説明コメント追記
- `LittleMaidEntity.java` L1624: 「複数モデルで問題ないかチェック」→ 検証コメント化

### Phase 2（神クラス分割と同時に解消）
- `LittleMaidEntity.java` L239: 単一引数コンストラクタの削除
- `LittleMaidEntity.java` L319: パーティクル演出の強化（→ `MaidResurrection`）
- `LittleMaidEntity.java` L763 / L2116: IdFactor のタイミング・仕様改善
- `LittleMaidEntity.java` L1218 / L1257 / L1359: 処理改善・try/catch 追加
- `LittleMaidEntity.java` L1366: Infinity 判定を `Holder<Enchantment>` で
- `LittleMaidEntity.java` L1420: クロスボウ弾道調整（performCrossbowAttack override）
- `LittleMaidEntity.java` L1636 / L1637: `mobInteract` 整理・使用アイテムのコンフィグ化（→ `LMInteractionHandler`、コンフィグは Phase 6）
- `LittleMaidEntity.java` L1992: hurtArmor 計算式の改善
- `LittleMaidEntity.java` L2050: getProjectile の改善
- `LittleMaidEntity.java` L2500 / L2501: 強制再生メソッド・再生クールダウンのコンフィグ化
- `ItemContractable.java` L15: クライアント側活用方針の確定

### Phase 3（モード共通化・赤石バグと同時に解消）
- `RedstoneTraceGoal.java` L57: `getBlockPos()` で判定して動作させる（赤石迷子バグ）
- `LittleMaidScreen.java` L245: 取得ずれを防ぐ方法を検討

### Phase 6（Config / 属性現代化と同時に解消）
- `LittleMaidEntity.java` L215: クライアント側 accelerationTicks の信頼性（同期方針）
- `LittleMaidEntity.java` L244: 付与属性の再考
- `LittleMaidEntity.java` L256 / L1043: スポーン条件のコンフィグ化
- `LittleMaidEntity.java` L1109: ボイス周りの調整・コンフィグ化

### Phase 8（首/視線・サイズ最適化と同時に解消）
- `LittleMaidEntity.java` L1086: `getDefaultDimensions` のキャッシュ最適化
- `LittleMaidEntity.java` L1064: マウント系の位置調整

### Phase 7/10（AI 化・仕上げ）
- `LittleMaidEntity.java` L2376 付近 `isFriend()`: TargetingSystem フレンドタグの復活

---

## ⚠️ 本リファクタの対象外（挙動が変わるため別途）

- 一部モードの状態 NBT 未永続化（Archer/Fencer の cooldown、Healer の index 等）は
  リロード時の挙動を変える修正のため、記述量削減リファクタとは分けて扱う。
