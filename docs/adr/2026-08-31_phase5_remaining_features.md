# Phase 5: 残機能（頭飾り・グループ・ジョブ Data Map・GameTest）

日付: 2026-08-31

## 判断

sistr 残件のうち仕様が足りるものだけ入れる。好感度は仕様未確定のまま凍結。

## 頭飾りスロット

ヘルメットは `EquipmentSlot.HEAD` のまま（防御・アーマーレイヤ）。カボチャ／頭蓋骨など `Equippable` で HEAD だが `ItemTags.HEAD_ARMOR` ではないものを専用スロットへ。

保存はエンティティ NBT `HeadCosmetic`（`ItemStack.MAP_CODEC`）と `SynchedEntityData`。ソウル経由の `saveWithoutId` に乗る。独自 `DataComponentType` はアイテム側の Equippable で足りるので増やさない。

描画は `MaidModelRenderer.applyHeadCosmetic` が `state.headItem` / `wornHeadType` を専用スロット優先で上書きする。専用スロットが空で HEAD が非防具なら従来どおり HEAD を飾る（既存ワールド互換）。

GUI はヘルメットと胸の間 `(8, 26)`。テクスチャに穴が無いのでヘルメット枠 UV を流用 blit。

## グループ

`MaidManager.LMInfo` に `group` 文字列（最大 32、プレイヤー Attachment に永続）。再登録・アンロード時は既存グループをコピーする。

割り当ては管理画面の入力＋カードの Set、または `/lmn maid group <name>`。検索は名前／状態／グループ。MenuTabBar は既存画面に検索があるので追加しない。

## ジョブトリガー

インゲームのタグ編集 GUI は作らない。datapack の Item Data Map `littlemaidneo:maid_job` で追加し、`/lmn job` で手持ちと付近のメイドさんを確認する。

## GameTest

NeoForge 26.1 以降の `BuiltInRegistries.TEST_FUNCTION` ＋ `data/.../test_instance/*.json`。ストラクチャは既存 `small_floor`（8x4x8）。

- `contract` — ケーキで雇用
- `job_switch` — 鉄剣で combat
- `store_items` — FREEDOM ＋隣接チェストへ丸石

## 凍結

好感度。仕様が決まるまでコードを足さない。
