# TODO

## 高

- かまど使用マップ `USED_FURNACE_MAP` が static・次元非対応（despawn リーク）

## 中

- 描画: `IHasMultiModel.Layer.isArmor()` の歴史的な反転（新規は `isArmorLayer()`）
- `pharmcist` 誤字のリネーム（datafixer 必須）

## 低

- 好感度（`sistr_TODO.md`、仕様未確定。凍結）
- `IHasMultiModel.Layer.isArmor()` の命名（真偽がフィールドと逆。`ArmorPart` 依存）
- GUI の `ChatFormatting` を `Style` へ
- `MASTER_STANCE` SynchedEntityData 未使用
- `LMAdvancementProvider` の `parent(Identifier)` が `[removal]`（`AdvancementHolder` へ置換）

## 機能要望

詳細は `sistr_TODO.md` を参照。
