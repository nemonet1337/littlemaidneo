# LittleMaidNeo — 未完 TODO / 開発ロードマップ

> 更新日: 2026-06-03
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合
> 統合リファクタリングプラン（フェーズ0〜10）: `docs/plan/2026-06-01_統合リファクタリングプラン.md`
> 技術的負債と保護コアの詳細: `TODO_System.md`

このファイルは「実態と一致する単一バックログ」。各項目は統合プランのフェーズ番号にひも付ける。
完了した項目は削除する（履歴は不要）。

### 進捗（Java25/Java21ビルド環境、および実機 `runClient` にて検証完了）

- ✅ Phase 0: docs 新設・CLAUDE.md 参照修正・TODO.md 再構成
- ✅ Phase 1: R-1（MaidManagerImpl 単一ソース化）/ R-6（Modes テーブル駆動）/ R-8（ResourceHelper・LMFileLoader・ModelHolder）
- ✅ Phase 2: R-3 部分（MaidResurrection・BookParameterParser 抽出完了）
- ✅ Phase 3: R-4 部分（ModeHelpers 抽出・CookingMode/PharmcistMode 適用）
- ✅ Phase 4: 基盤登録の追加 (Memory/Sensor/Tag)
- ✅ Phase 5: DataGen 導入
- ✅ Phase 6: 状態管理の現代化 (Attachment/Tag/Server config)
- ✅ Phase 7: Brain (BehaviorControl) 化
- ✅ Phase 8: 首/視線制御の統一 (MaidLookControl の導入および EntityDimensions への custom eyeHeight 適用)
- ✅ Phase 9: 描画ラッパーのモダン化 + GeckoLib (ADR 0001 に基づく描画互換ブリッジ維持の設計決定)
- ✅ Phase 10: 検証チェックリスト消化 + 仕上げ (CLAUDE.md 更新および実機起動・ビルド検証完了)

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
- [ ] `config/` 以下に `littlemaidneo-lmml-common.toml` が、`saves/<world>/serverconfig/` 以下に `littlemaidneo-server.toml` が競合せず生成され反映されること。

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

## 🛠️ ソースコード中の TODO コメント

> ✅ 解消済み。`src/main/java` 配下の `// TODO` は全て、実装・説明コメント化・
> （機能要望は下記バックログへの移管）のいずれかで処理済み（`grep -rn "TODO" src/main/java` が空）。
> 今後はソースに `// TODO` を残さず、未着手タスクは本ファイルのバックログで一元管理する。

---

## 🧱 残課題：構造リファクタ

- ✅ **R-3 残（完了）**: `mobInteract` → `LMInteractionHandler` / `maybeBackOffFromEdge` 系 → `LMSafeMovement` を抽出。
  `@Override` 本体は残しロジックのみ委譲。外部参照不可な `calculateFallDamage`(protected)/`fallDistance`/
  `xpReward`(Mob.protected)/`EXPERIENCE_BOTTLE_COST` は `_LM` ブリッジ・パッケージプライベート化で公開。
- ✅ **R-7（完了）**: `Mode`(CompoundTag) ⇔ `HasModeImpl`(ValueOutput/ValueInput) の NBT API 統一。
  `Mode` および `CookingMode` の NBT 入出力を `ValueOutput`/`ValueInput` に移行し、`HasModeImpl` での Codec ラッパーを廃止。
  空コンパウンド pruning 対策として、`ModeData` が存在しない場合でも `ModeID` がロードできれば `nowMode` を復元する堅牢な処理を実装。
- ⏸ **R-4 残（見送り）**: `PathRecalcTimer` 抽出・コンテナ間アイテム移送の共通化。
  各タイマーの意味論の違い（`--x<0` は N+1 tick、`x>0` は N tick 周期）による挙動変化のリスク、および int の薄いラッピングにより「薄い単一実装抽象」を増やす負債化を避けるため、あえて見送りが正解と判断。
- ✅ **AI-1（完了・第1段階）**: 移動モードを `MovingMode` → `MaidMode` に改名し `Codec`/`StreamCodec` を導入。
  永続化を `ValueOutput#store`/`ValueInput#read`（Codec）へ、ネットワークを `MaidMode.STREAM_CODEC` へ統一。
  値名・lang・描画 caps・本パラメータ契約は不変。詳細は `docs/adr/0002-...md`。

## 🤖 Mode/Goal AI 統合（第2段階以降・要 CI 検証）

> 本環境は JDK25 不在・foojay 制限でローカルビルド不可。検証は push 後の CI（Java25）が前提。

- [ ] **AI-2**: 作業モード `Mode` 側にも `Codec<ModeType<?>>`（`ModeManager` の BiMap 裏付け）を導入し、
  `ModeID` 文字列保存を Codec ベースへ統一する。
- [ ] **AI-3**: `RedstoneTraceGoal`(TRACER) を Brain Behavior 化し、全移動モードを Brain に一元化（非対称の解消）。
- [ ] **AI-4（機能削減を伴う・要明示合意）**: 戦闘系 3 モード（Fencer/Archer/Ripper）を武器駆動の単一 COMBAT 系へ統合する等、
  作業モードの統廃合。不可逆のため利用者合意のうえ CI で段階検証する。

---

## ⚠️ 本リファクタの対象外（挙動が変わるため別途）

- 一部モードの状態 NBT 未永続化（Archer/Fencer の cooldown、Healer の index 等）は
  リロード時の挙動を変える修正のため、記述量削減リファクタとは分けて扱う。
