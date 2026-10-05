---
date: 2026-10-06
category: adr
---

# 2026-10-06 TODO.md 残項目の実装

`TODO.md` の残項目（`sistr_TODO.md` の好感度のみ凍結）を実装した。判断が必要だった 3 点を記録する。

## 1. `USED_FURNACE_MAP` の static マップを廃止しワールド問い合わせへ置換

**問題:** `MaidCookingBehavior` の `USED_FURNACE_MAP`（static な `Object2ObjectOpenHashMap<BlockPos, LittleMaidEntity>`）が、次元を無視し、メイドが despawn してもエントリが残り続けていた。

**検討した案:**

- (a) `Map<ServerLevel, ...>` にして `EntityLeaveLevelEvent` で掃除する
- (b) `GlobalPos` をキーにし、寿命管理をイベントで行う
- (c) static な状態をやめて、ワールドを直接問い合わせる

**(c) を採用。** static な可変状態そのものを消せるため、次元非対応も despawn リークも「起き得ない」状態になる。イベントハンドラを新設する必要もなくなった。

実体は `claimedFurnaces(mob)` で、半径 10 以内の `LittleMaidEntity` の `cookingBehavior.furnacePos` を集めるだけ。予約の正しさは「そのメイドが実際にそのかまどのそばにいる」ことをワールド自身が保証するので、旧実装の保守機構（死亡判定・`level().getEntity(id)` による同一性照合）が丸ごと不要になった。

**探索半径 10 の根拠:** 仕事場 POI の探索範囲 8 に当人のかまどまでの寄り 1.75 を足した 9.75 を超えると他メイドの声称を見落とすため、余裕を持って 10 とする。8 のままでは近接した 2 人のメイドが同じかまどを奪い合う。

**副作用と対処:** 旧実装は `stop()` でマップのエントリを削除していたが、`furnacePos` フィールド自体は残るため、仕事を変えたメイドが「まだかまどを声称している」ように見えてしまう。`stop()` の末尾で `furnacePos = null` にして解消した。旧実装と異なりチャンクリロード後も `furnacePos` が保存されるため、予約はリロードをまたいで維持される。

## 2. `pharmcist` → `pharmacist` は読み取り時正規化で移行

**検討した案:**

- (a) `MaidDataFixer` / Entity DataFixer で NBT を書き換える
- (b) 読み取り時に旧名を現行名へ正規化する

**(b) を採用。** datafixer だと保存経路が複数ある（Entity NBT、`MaidSoulData` の魂 NBT、datapack の `maid_job.json`）すべてを網羅する必要があるが、正規化ならどの経路でも自動的に効く。書き込み側は現行名だけ出すので、1 tick もすれば旧名はセーブから消える。

正規化点は 2 箇所に絞った。

- `MaidJobEntry` のコンパクトコンストラクタ — datapack や `mappedJob` 由来のジョブ名をまとめて現行名へ揃える
- `MaidJobManager.tick` のメモリ書戻し — 旧セーブから読み込まれた Brain memory を上書きする

NBT の子キー `"pharmcist"` は `LittleMaidEntity.readModeData` で旧キーからも読めるようにした（読み取り専用フォールバック）。

**失効しうるもの:** タグ `littlemaidneo:pharmcist_mode` / `pharmcist_ingredients` のリネームは実行時の補正では救えない。タグは ResourceLocation なので、旧タグを参照する外部 datapack や追加 Mod は壊れる。プロジェクト内のタグは自前の `LMItemTagsProvider` が生成しているため影響はない。

**副として `ChatFormatting` は非推奨ではなかった:** TODO の「GUI の `ChatFormatting` を `Style` へ」という記述は、`MutableComponent#withStyle(ChatFormatting...)` が NeoForge 26.2 でも非推奨ではないため、このままだと意図しない変更になる。コードベースの記法統一（`MaidManagerScreen` と `MaidManager` は既に `withStyle(style -> style.withColor(0x...))` を使っている）を意図と解釈し、`LittleMaidScreen` をその記法に揃えた。色は `ChatFormatting` と同じ値を 16 進でそのまま書いてある（RED=`0xFF5555` / GOLD=`0xFFAA00` / DARK_GREEN=`0x00AA00`）。

## 3. `IHasMultiModel.Layer.isArmor()` は削除

反転していた `isArmor()`（SKIN で true を返す）を削除した。呼び出し箇所が 0 だったため置換先がなく、コードベースは全域で `layer == Layer.SKIN` という明示比較を使っている（`ArmorPart` も同じ）。よって移行コードは 1 行も書かなくてすんだ。正しい名前の `isArmorLayer()` だけ残す（private フィールド `isArmor` の読み出し先が必要）。

## 参考: 遭遇した落とし穴

`./gradlew :apps:mods:mergeData` は client / server の 2 パスが同じ出力先と `HashCache` を共有しているため、キャッシュを冷えた状態で実行すると互いの生成物を削除し合う。client パスが `lang` / `models` / `blockstates` を書き、server パスの stale 判定がそれを消す。今回の lang 変更は `mergeData` の出力を信用せず、生成済み JSON への機械的リネームと `git diff` による検算で行った。