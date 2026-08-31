# LittleMaidNeo

NeoForge 向けに書き直されたメイドさん Mod。レガシー Mod 系譜の **LittleMaidRebirth (LMRB)** と **Little Maid Model Loader (LMML)** を統合し、AI 行動を持つテイム可能なメイドさんエンティティと、外部リソースから動的に読み込むマルチモデルレンダリングシステムを 1 つの Mod として提供する。

## 動作環境

- Minecraft 26.2
- NeoForge 26.2.0.32-beta
- Java 25

旧 LMRB / LMML（MC 1.20.1 + Architectury + Java 17、Fabric/Forge 両対応）からは大幅にアップデートされており、**NeoForge 単一プラットフォーム** に移行している。

## セットアップ

1. リポジトリをクローン
2. Java 25（temurin 等）を用意し、`JAVA_HOME` を設定するか Gradle の toolchain による自動取得に任せる
3. IDE で開く（IntelliJ IDEA または Eclipse 推奨）
4. 依存解決に問題が出た場合は以下を実行:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

## ビルド

```bash
./gradlew build
```

成果物: `apps/mods/build/libs/LittleMaidNeo_<Minecraft バージョン>_<Mod バージョン>.jar`

## 開発コマンド

| コマンド | 説明 |
|----------|------|
| `./gradlew build` | フルビルド |
| `./gradlew compileJava` | コンパイルのみ（軽量検証） |
| `./gradlew :apps:mods:runClient` | クライアント起動 |
| `./gradlew :apps:mods:runServer` | サーバー起動（`--nogui`） |
| `./gradlew :apps:mods:runGameTestServer` | GameTest 実行（namespace は `littlemaidneo`） |
| `./gradlew :apps:mods:mergeData` | データジェネレータ実行（出力先 `apps/mods/src/generated/resources/`） |

## 外部リソース

外部ボイスパック（`.cfg` + `.ogg`）と PNG テクスチャパックは、ゲームディレクトリの `LMMLResources/` フォルダから読み込まれる。内蔵モデルは `maidmodel/` の `ModelPart` 実装。旧 LMM/MMM 形式の `.class` モデルパック互換は廃止済み。

## マッピング

Mojang 公式マッピング（NeoForge MDK デフォルト）。マッピングは独自のライセンス下に置かれており、最新のライセンス本文はマッピングファイル本体、または下記参照コピーで確認できる:

https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## ライセンス

LittleMaid Licence — [LICENCE.md](LICENCE.md) 参照

## 参考リンク

- NeoForged ドキュメント: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/
