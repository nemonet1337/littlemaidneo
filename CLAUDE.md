## Project Overview

LittleMaidNeo は、レガシー Mod 系譜 **LittleMaidRebirth (LMRB)** と **Little Maid Model Loader (LMML)** を統合し、NeoForge 向けに書き直された Minecraft Mod。テイム可能なメイドさんエンティティ（AI 行動付き）と、外部リソースから動的に読み込むマルチモデルレンダリングシステムを 1 つの Mod として提供する。

- **LMRB 系**: `LittleMaidEntity` を中心としたメイドさんエンティティ・AI・モード（料理・戦闘・治癒など）・契約システム
- **LMML 系**: `MultiModelEntity`、`multimodel/`、`maidmodel/`、`MultiModel*` レンダラー — 外部モデルパックの読み込み/レンダリング基盤
- 旧 Architectury / Fabric / Forge マルチプラットフォーム構成は破棄され、**NeoForge 単一プラットフォーム** に移行済み

## Architecture

- 単一モジュール構成（旧 `common/fabric/forge` の三分割は廃止）
- ルートパッケージ: `work.nemonet.littlemaidneo`
- メインエントリ: `LittleMaidNeo.java`（`@Mod("littlemaidneo")`）／`LittleMaidNeoClient.java`
- 主要パッケージ:
  - `entity/` — `LittleMaidEntity`（中心エンティティ・約 89KB）、`MultiModelEntity`、`MaidSoulEntity`
    - `entity/goal/` — AI Goal
    - `entity/mode/` — モード実装（`CookingMode`, `FencerMode`, `ArcherMode`, `HealerMode` 等）
    - `entity/targeting/` — ターゲティング
    - `entity/compound/` — `IHasMultiModel` 等の合成 interface
  - `api/mode/` — モード API（`Mode`, `ModeType`, `ModeManager`, `Modes`, `ItemMatcher`, `ItemMatchers`）
  - `block/` — `SalaryBoxBlockEntity` 等
  - `item/` — アイテム
  - `multimodel/` — モデル定義・レイヤー（LMML 由来）
  - `maidmodel/` — 外部モデルパック互換基盤（**load-bearing、削除厳禁**）
  - `resource/` — 外部リソース読み込み
    - `classloader/` — `MultiModelClassLoader` / `MultiModelClassTransformer`（ASM リマップ、**削除厳禁**）
    - `loader/`, `holder/`, `manager/`, `util/`
  - `client/` — クライアント（`renderer/`, `screen/`, `resource/`, `key/`）
  - `network/` — `NetworkHandler`（NeoForge `RegisterPayloadHandlersEvent` ベース）
  - `mixin/` — Mixin（`littlemaidneo.mixins.json` で登録）
  - `config/` — `LMRBConfig`（メイン）、`LMMLConfig`（モデルローダー設定）
  - `setup/` — `Registration`, `ModSetup`, `ClientSetup`
  - `tags/`, `advancement/`, `world/`, `util/`
- 外部リソース読み込み: ゲームディレクトリの `LMMLResources/` フォルダから（`FMLPaths.GAMEDIR`）
- モード登録: `api/mode/Modes.java` で `ModeType` と `ItemMatcher` を定義し `init()` で一括登録
- モード判定: `ItemMatcher` の Priority 降順で最初にマッチしたモードが採用（同 Priority 時は登録順）
- lang: `assets/littlemaidneo/lang/{en_us,ja_jp}.json` — モード名キーは `mode.littlemaidneo.{Name}`
- タグ: `data/littlemaidneo/tags/items/` — モード用タグは `{mode_name}_mode.json`

### 絶対不変の保護コア（**削除・破壊的変更厳禁**）

- **保護コア A: 外部モデル/テクスチャ読み込み・描画 (互換インフラの死守)**
  - `resource/classloader/`（`MultiModelClassLoader`・`MultiModelClassTransformer`）と `maidmodel/` パッケージ全体、`maidmodel/compat/GLCompat` は、**外部ユーザー製の旧 LMM/MMM モデルパック（`.class` ファイル）を実行時に ASM でリマップ・GL11→GLCompat 置換して読み込むための互換インフラ**です。
  - `entity/EntityLittleMaid`（中身ほぼ空のスタブ）も `MultiModelClassTransformer` のリマップ先（`net/blacklab/lmr/entity/EntityLittleMaid` → これ）なので残す必要があります。
  - メイドさん本体の描画システムにおいて、外部パックとの互換性を崩すバニラ `ModelPart` や GeckoLib への本体移行は行わず、独自ラッパー（`MMMatrixStack` 等）と `GLCompat` を用いたブリッジ構造を維持・保護する必要があります（詳細は ADR 0001 を参照）。
- **保護コア B: 外部ボイスパック読み込み・再生**
  - `.cfg` 形式、`LMSounds` 定数文字列、命名規則、探索パス（`LMMLResources/`）、およびネットワーク同期パケット形式は、外部ボイスパック（`.cfg` + `.ogg`）を正常に読み込み・再生するために不変を維持します。

## Environment

- Minecraft 26.1.2 / NeoForge 26.1.2.64-beta
- **Java 25** が必要（`java.toolchain.languageVersion = 25`）
- Gradle: NeoForge moddev plugin `net.neoforged.moddev` 2.0.141
- 旧 LMML/LMRB は MC 1.20.1 + Architectury + Java 17 だったが、NeoForge 移行で大幅にアップデートされている

## Build & Test

旧 LMML/LMRB の `spotless` / `checkstyle` / `spotbugs` は **未導入**（NeoForge MDK ベースのため）。

- `./gradlew build` — フルビルド
- `./gradlew compileJava` — コンパイルのみ（軽量検証）
- `./gradlew runClient` — クライアント起動
- `./gradlew runServer` — サーバー起動（`--nogui`）
- `./gradlew runGameTestServer` — GameTest 実行（namespace は `littlemaidneo`）
- `./gradlew runData` — データジェネレータ実行（出力先 `src/generated/resources/`）
- CI: `.github/workflows/build.yml`（push/PR で `./gradlew build`、Java 25 / temurin）

## Localization and Communication Guidelines

- メイドさんのことはメイドさんと呼んでください。 (Always refer to maids as "メイドさん")

## TODO 管理

- `TODO.md` をタスクリストとして自律管理する
- 開発中に発見した課題・技術的負債・リファクタ候補などを随時追記する
- 完了したタスクは削除する（履歴は不要）
- 優先度（高/中/低）でカテゴリ分けする
- Notion タスクボードは公開されているため、ユーザーの明示的な指示がない限り変更しない（読み取りは自由）

## 作業記録

- 設計判断や重要な技術的決定を行った際は `docs/` に作業記録を残す
- ユーザーの指示がなくても、記録に値する判断をした場合は自律的に `/doc` スキルで記録する
- 形式: `docs/{category}/yyyy-mm-dd_{タイトル}.md`
- カテゴリ: `adr/`（設計判断）, `plan/`（作業プラン）, `research/`（調査メモ）等
- NeoForge クリーンアップ／統合の進行プラン: `docs/plan/2026-06-01_統合リファクタリングプラン.md`（旧 `docs/neoforge-cleanup-plan.md` は廃止）

## Cross-Environment Workflow

- WSL2 から Windows リポジトリへローカル remote 経由で転送可能
- `git remote add local /mnt/c/github/littlemaidneo`
- Windows 側でチェックアウト中のブランチには push 不可。別ブランチ名に push: `git push local main:wsl/{branch-name}`

### Code Editing Guidelines
- 既存ファイルを Write で全体書き換えする際は、既存の内容が失われないよう注意する（Edit で差分追加を優先）
- 返り値に Optional を使用し、フィールドや引数には `@Nullable` を使用する
- `org.jetbrains.annotations.Nullable` を使用する
- `@Nullable` フィールドはローカル変数にキャッシュしてから使用する（NPE / SpotBugs NP_NULL_PARAM_DEREF 対策）
- Mixin Accessor は `util/` または `mixin/` 配下に配置し、メソッド名に `_LM` サフィックスを付ける（例: `getBrewTime_LM()`）
- protected フィールド/メソッドへの外部アクセスが必要な場合、同パッケージ内ならパッケージプライベートゲッターを追加する（Mixin Accessor より簡潔）
- Mixin を追加した際は `src/main/resources/littlemaidneo.mixins.json` への登録を忘れない（未登録の孤立 Mixin は過去にデッドコード化した実績あり）

### Architecture Notes
- コンフィグ: `config/LMRBConfig.java`（メイン）と `config/LMMLConfig.java`（モデルローダー）— NeoForge `ModConfigSpec` ベース、TOML
  - `LMRBConfig` は `ModConfig.Type.SERVER`（`littlemaidneo-server.toml`、ワールド同期設定）
  - `LMMLConfig` は `ModConfig.Type.COMMON`（`littlemaidneo-lmml-common.toml`、クライアント共有設定）
  - `LMRBConfig.bake()` は `ModConfigEvent` で呼ばれる
  - 旧 LMRB の AutoConfig + Cloth Config からは置き換え済み
- コンフィグ追加手順: (1) `LMRBConfig` に `ModConfigSpec.XxxValue` フィールド追加 → static ブロックで定義 (2) 消費側でゲッター経由参照に置換 (3) lang/{en_us,ja_jp}.json にキー追加
- ターゲティング設定は `TargetingConfig` ラッパー経由でアクセスする（`TargetingConfig.getAlertRange()` 等）
- ブロック操作モード（料理・醸造）: `BlockWorkMode` + `WorkStrategy<T>`（Strategy パターン、委譲）
- ブロック探索: `BlockSearch`（非同期 BFS）+ `SearchCondition`（linkable 条件ビルダー）
- ブロック排他制御: `BlockReservationManager`（`GlobalPos` でディメンション対応）
- `LMSounds` 定数は `String` 型。`mob.play(LMSounds.COOKING_START)` のように使用
- `LittleMaidEntity` は以下の委譲クラス・コンポーネントに機能が分割されています：
  - `MaidResurrection` (契約期間延長・復活演出の処理)
  - `BookParameterParser` (本アイテムによるパラメタ設定のパース)
  - `LMInteractionHandler` (`mobInteract` の右クリック・アイテム別分岐の移譲。`@Override` 本体は残し委譲)
  - `LMSafeMovement` (`maybeBackOffFromEdge` の落下/危険ブロック安全移動の移譲。`calculateFallDamage`/`fallDistance` へは `_LM` ブリッジ経由)
  - `LMHasInventory` (インベントリ処理の移譲)
  - `LMItemContractable` (給料・契約・時間管理の移譲)
  - `HasModeImpl` (モード管理・切り替えの移譲)
  - `TargetTagManagerImpl` (ターゲットタグ情報の管理)
  - `TargetingSystem` (他エンティティの友好/敵対ターゲット評価)
  - `work.nemonet.littlemaidneo.entity.ai.control.MaidLookControl` (首振り最大角度制限のクランプおよび視線・頭部向き制御の一元化)
- `getNavigation()` は `Mob` に定義（`LivingEntity` ではない）— NeoForge / Mojang マッピングでは Yarn 時代の `MobEntity` → `Mob`
- `initGoals()` は `Mob` コンストラクタ内で呼ばれる — サブクラスのフィールドは未初期化。外部委譲時はラムダで遅延参照すること

### Networking
- NeoForge `RegisterPayloadHandlersEvent` ベース（`network/NetworkHandler.register(event)`）。旧 Architectury Networking API からは置き換え済み

### Registration
- すべての `DeferredRegister` は `setup/ModRegistration.java` に集約し、`LittleMaidNeo` コンストラクタで `register(modEventBus)`
- エンティティ属性は `EntityAttributeCreationEvent` で登録（`onEntityAttributeCreation`）
- Brain AI: `ModRegistration.MEMORY_MODULES`（`IS_WAITING`/`OWNER`）と `ModRegistration.SENSORS`（`LITTLE_MAID_SENSOR`）を DeferredRegister で登録
- Data Attachment: `ModRegistration.ATTACHMENT_TYPES` に `MAID_MANAGER_ATTACHMENT`（`MaidManagerImpl`）と `TARGET_TAG_ATTACHMENT`（`TargetTagManagerImpl`）を登録。プレイヤーステートは Mixin+interface ではなく Attachment 経由で取得する（`player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get())`）
- DataGen: `LMDataGenerator` が `GatherDataEvent.Client`/`GatherDataEvent.Server` を受け取り、Lang/Tag/Recipe/LootTable/Advancement/BiomeModifier を生成。出力は `src/generated/resources/`（git 追跡対象）

### Rendering Notes
- カスタムシェーダーは `assets/minecraft/shaders/core/` に配置する（NeoForge / バニラのシェーダー解決が `minecraft` 名前空間前提）
- 最大輝度の light 値は `LightTexture.FULL_BRIGHT = 15728880`（`0xF000F0`）
- `MultiModelRenderLayer` は `RenderType` を継承し、`RenderStateShard` の protected 定数にアクセスする
- 発光テクスチャ用カスタムシェーダー `lmml_emissive` は NeoForge の `RegisterShadersEvent` で登録

### GameTest
- GameTest 用 namespace は `littlemaidneo`（`build.gradle` の `neoforge.enabledGameTestNamespaces`）
- 旧 LMRB のような `common/fabric/forge` 三分割の委譲は不要。テストは単一ソースセットに直接配置可能
- ストラクチャーは `src/main/resources/data/littlemaidneo/structures/`
- FakePlayer ワールド登録が必要な場合は `createWorldPlayer()` + `cleanupWorldPlayers()` ペアで使用
- コンフィグ変更テスト: try/finally でフィールドを直接書き換え+復元

### API Research
- Minecraft バニラ・NeoForge などの前提 Mod の API 調査には必ず `mc-api-research` エージェントを使用する
- `.gradle` キャッシュの jar を直接検索しない
- ライブラリ／フレームワーク（NeoForge SDK 含む）のドキュメント参照は Context7 MCP を優先する

### Design Review Guidelines
- レビュー観点: SOLID 原則, Effective Java, Law of Demeter / Tell Don't Ask, OOP アンチパターン
- `super` 呼び出しを含む override メソッドは外部クラスに委譲できない — 本体に残す
- 状態を持たないオーケストレーション/ファクトリは static ユーティリティクラスで可（過度なオブジェクト化を避ける）
- LMML/LMRB 両系譜の融合により薄い抽象・単一実装 interface が蓄積している。整理プランは `docs/plan/2026-06-01_統合リファクタリングプラン.md` を参照
