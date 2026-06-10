# ADR 0004: Gradle 3 モジュール構成への再編（apps/common・apps/modelloader・apps/mods）

- 日付: 2026-06-10
- ステータス: 採用

## 背景

ユーザー要望により、単一モジュール構成からディレクトリ構造を再編し、旧 LMML（モデルローダー）系と
メイドさん本体（旧 LMRB）系の境界をビルドレベルで強制する。あわせて LMRB/LMML/LMReengaged の
名称を LittleMaidNeo / LMN に統一した。

## 決定

```
root/
├── settings.gradle.kts
├── build.gradle.kts          # ルートの共通設定（toolchain 25 / UTF-8 / mavenCentral / moddev apply false）
└── apps/
    ├── common/        # LMNLib（MODID/LOGGER）・汎用 util（BlockFinder, ProcessDivider 等）
    ├── modelloader/   # 旧 LMML 系: multimodel/ maidmodel/ resource/ client/resource/
    │                  #   client/renderer/MultiModel* entity/compound/ MultiModelEntity
    │                  #   EntityLittleMaid(スタブ) MultiModelHolder/SoundHolder LMNModelConfig
    └── mods/          # メイドさん本体: entity/ block/ item/ network/ mixin/ setup/ ...
                       #   全リソース・templates・src/generated・runs・最終 jar 生成
```

- **依存方向は `mods -> modelloader -> common` の一方向のみ**。コンパイル時に強制される。
- Java パッケージ名は変更しない（`work.nemonet.littlemaidneo.*` を全モジュールで共有。
  classpath ベースなので split package は問題ない）。保護コア A のリマップ先
  `entity/EntityLittleMaid` の FQCN も不変。
- 最終成果物は従来どおり**単一の Mod jar**。`apps/mods` の `jar` タスクが common /
  modelloader の `sourceSets.main.output` を取り込む。`neoForge.mods` ブロックにも
  3 ソースセットを登録して開発時 run でも 1 Mod として認識させる。

## 逆方向依存の解消

| 旧依存 | 解消方法 |
|---|---|
| modelloader 各所 → `LittleMaidNeo.MODID/LOGGER` | `apps/common` の `common/LMNLib` を新設し参照を差し替え。`LittleMaidNeo.MODID` は `LMNLib.MODID` のエイリアスとして維持 |
| `SoundPlayableCompound` → `NetworkHandler.sendLMSoundS2C` | `setSoundSyncSender(BiConsumer)` フックを新設し、`NetworkHandler.register` が起動時に注入 |
| `client/renderer` のメイドさん固有レンダラー（`MaidModelRenderer` 等） | mods 側に残置（`MultiModel*` の汎用レイヤーのみ modelloader） |

## 影響・注意

- ASM / commons-io 依存は使用箇所のある `apps:modelloader` に移動。
- `runClient` 等の run タスクは `:apps:mods` 配下になった。
- 保護コア A/B のコード本体は modelloader にまとまり、誤改変の検知がしやすくなった。
