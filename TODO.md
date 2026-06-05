# LittleMaidNeo — TODO / バックログ

> 更新日: 2026-06-05
> 方針: NeoForge 単一プラットフォーム / LMRB＋LMML 統合 / モダン化リファクタリング
> 実装ガイド・保護コア詳細: `HOWTO.md`
> 設計判断: `docs/adr/`（0001 描画ブリッジ維持 / 0002 MaidMode Codec・AI 状態統一 / 0002 並列パックロード）
> 全体プラン: `docs/plan/2026-06-01_統合リファクタリングプラン.md`

このファイルは「実態と一致する単一バックログ」。完了項目は削除する（履歴は不要）。

---

## 🚀 モダン化リファクタリング（5 ワークストリーム・進行中）

> 背景: 1.6.4→1.21.1 系で刷新された内部システム／Modding API のうち、本 Mod の穴を埋める差分リファクタ。
> 既採用分（Deferred Register / PayloadRegistrar / Data Attachments / DataGen / Mojang mappings / TOML 等）は省略。
> 推奨実施順: **WS3 → WS2 → WS5 → WS4 → WS1**（低リスク基盤を先に、最高リスクの AI を最後に）。詳細手順は `HOWTO.md`。
> ⚠️ ローカル JDK25 不在環境では検証は CI（Java25）が前提。

- [x] **WS3 — DataGen で model/blockstate 生成**（低リスク・即効）
  手書き JSON（`salary_box`/`little_maid_spawn_egg` の blockstates/models/items）を `LMBlockStateProvider`/`LMItemModelProvider` で生成。`runData` 出力が既存 JSON と diff ゼロを確認後に手書き JSON 撤去。
- [x] **WS2 — MaidSoul の Codec 化 + カスタム Data Components**
  生 `CompoundTag` の MaidSoul を `record MaidSoulData`＋Codec/StreamCodec へ。`DeferredRegister<DataComponentType<?>>` で `MAID_SOUL` component 登録。`MaidSoulEntity`/`WorldMaidSoulState`/`LittleMaidSpawnEggItem` を移行。
- [x] **WS5 — DataFixerUpper 導入（MaidSoul/エンティティ NBT 限定）**
  永続データに `dataVersion` を埋め、WS2 の Codec に旧キー（旧 `Owner`/`UUID`・旧モード ID）→新スキーマのフォールバック分岐 `MaidDataFixer` を実装。完全 DFU スキーマ登録の可否は ADR で確定後に着手。移動モード byte 旧互換は対象外（ADR-0002）。
- [x] **WS4 — Brigadier 管理コマンド**
  `RegisterCommandsEvent` で `/littlemaidneo`（`reload`/`models list`/`maid count|tp|dismiss`/`debug dump`）。OP 権限分岐。保護コア B は読取専用。
- [x] **WS1 — AI 完全 Brain 化（作業モード含む・最高リスク）**
  ADR-0002 の「作業=Goal」2軸を改訂。`ModeWrapperGoal`＋残存補助 Goal（Heal/給料/収納/搬送/Teleport/PlaySnow/Target/Look 系）を Brain の Activity（CORE/IDLE/WORK/FIGHT）へ移植。`ModeManager`/`CombatMode`/`ItemMatcher`/`TargetingSystem`/`BlockSearch` は再利用。新 ADR を起こす。GameTest を厚く。

---

## 🩹 残課題: モード状態 NBT の永続化（WS1/WS2 と並行検討）

- [ ] 一部モードの内部状態（Archer/Fencer→`CombatMode` の cooldown、Healer の index 等）が未永続化でリロード時に挙動が変わる。WS2 の Codec 基盤で永続化を検討（挙動が変わる修正のため記述量削減リファクタとは分離して扱う）。
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
- Forge Energy（概念非該当）。
