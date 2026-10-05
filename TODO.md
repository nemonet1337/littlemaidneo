# TODO

## 高

（なし）

## 中

- datagen: client/server 2 パスが同一出力先と `HashCache` を共有し、キャッシュを冷えた状態で `mergeData` すると互いの生成物を削除し合う（lang / models / blockstates が消える）。`mergeData` の 2 連続実行、または個別に再生成が必要

## 低

- 好感度（`sistr_TODO.md`、仕様未確定。凍結）
- `src/generated/resources` の生成物が、現在の datagen 出力と乖離している（`maid_job.json` のキー順、`priority` / `category` / `bonus_rolls` の既定値省略）。次回 `mergeData` 時に一大括の整形差分が出る

## 機能要望

詳細は `sistr_TODO.md` を参照。