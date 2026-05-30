# LittleMaidNeo

NeoForge 向けに書き直されたメイドさん Mod。レガシー Mod 系譜の **LittleMaidRebirth (LMRB)** と **Little Maid Model Loader (LMML)** を統合し、AI 行動を持つテイム可能なメイドさんエンティティと、外部リソースから動的に読み込むマルチモデルレンダリングシステムを 1 つの Mod として提供する。

## 動作環境

- Minecraft 26.1.2
- NeoForge 26.1.2.64-beta
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

成果物: `build/libs/littlemaidneo-<version>.jar`

## 開発コマンド

| コマンド | 説明 |
|----------|------|
| `./gradlew build` | フルビルド |
| `./gradlew compileJava` | コンパイルのみ（軽量検証） |
| `./gradlew runClient` | クライアント起動 |
| `./gradlew runServer` | サーバー起動（`--nogui`） |
| `./gradlew runGameTestServer` | GameTest 実行（namespace は `littlemaidneo`） |
| `./gradlew runData` | データジェネレータ実行（出力先 `src/generated/resources/`） |

## 外部リソース

外部モデルパック（旧 LMM/MMM 形式の `.class` ファイル含む）は、ゲームディレクトリの `LMMLResources/` フォルダから読み込まれる。`resource/classloader/` の `MultiModelClassLoader` / `MultiModelClassTransformer` が実行時に ASM でリマップ・GL11→GLCompat 置換を行う。

## マッピング

Mojang 公式マッピング（NeoForge MDK デフォルト）。マッピングは独自のライセンス下に置かれており、最新のライセンス本文はマッピングファイル本体、または下記参照コピーで確認できる:

https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## ライセンス

LittleMaid Licence — [LICENCE.md](LICENCE.md) 参照

## 参考リンク

- NeoForged ドキュメント: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/
