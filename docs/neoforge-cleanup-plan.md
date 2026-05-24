# LittleMaidNeo クリーンアップ & クラス統合プラン

> このドキュメントは NeoForge 移行に伴うコードベースのクリーンアップ／クラス統合の
> 調査結果と実行プランです。環境再作成後もブランチ上で参照できるよう永続化しています。
>
> **進捗**: Step 1・2 は実装済み（コミット済み）。Step 3〜8 は `maven.neoforged.net`
> を許可リストに含むネットワークポリシーの環境でコンパイル検証しながら実装する予定。

## Context（背景・目的）

`littlemaidneo` は NeoForge 26.1.2 (MC 26.1.2) 上の Mod で、**2つのレガシー Mod 系譜が融合した構造**を持つ。

- **LML/MMM 系**（LittleMaidMob / Modeling Multi-Model）: `MultiModelEntity`, `LMMLConfig`, `multimodel/`, `maidmodel/`, `client/renderer/MultiModel*`
- **ReBirth 系**: `LittleMaidEntity`, `LMRBConfig`, `LM` 接頭辞付きクラス群（`LM*Goal`, `LMItemContractable` 等）

この融合と「旧Forge/Fabric/独自実装 → NeoForge」移行の過程で、(1) 完全に参照されないデッドコード、(2) 過剰な単一実装抽象や薄いラッパー、(3) 二重登録などの設定上の不整合が蓄積している。

本プランの目的は、**コンパイルエラーとランタイム互換性リスクを最小化しながら段階的にコードベースを縮小・整理する**こと。ユーザー方針は **「積極的」** な統合（デッドコード削除＋薄い抽象の畳み込み＋Interface/Impl統合＋単一サブクラス基底の統合まで）。

> **最重要の前提（削除してはいけないもの）**: `resource/classloader/`（`MultiModelClassLoader`・`MultiModelClassTransformer`）、`maidmodel/` パッケージ全体、`maidmodel/compat/GLCompat` は、**外部ユーザー製の旧 LMM/MMM モデルパック（`.class`ファイル）を実行時に ASM でリマップ・GL11→GLCompat 置換して読み込むための互換インフラ**であり、load-bearing。名前だけ見ると「レガシー＝不要」に見えるが削除厳禁。`entity/EntityLittleMaid`（中身ほぼ空のスタブ）も `MultiModelClassTransformer` のリマップ先（`net/blacklab/lmr/entity/EntityLittleMaid` → これ）なので**残す**。

---

## 1. 不要クラス調査結果 (Obsolete Classes)

### 1-A. 確実に削除可能なデッドコード（0参照を検証済み）→ **Step 1 で削除済み**

| クラス / パッケージ | 種別 | 検証結果 | 削除理由 |
|---|---|---|---|
| `util/Pos2d` | ユーティリティ | 0参照 | 2D座標の薄いラッパー。どこからも未使用。必要なら `Vec2`/`BlockPos` 等で代替可。 |
| `util/SightUtil` (269行) | ユーティリティ | 0参照 | `rotate()` が `throw new AssertionError()` の**未完成実装**。統合されないまま放置されたデッドコード。 |
| `resource/util/ColorConverter` | ユーティリティ | 0参照 | 染料→`TextureColors` 変換。呼び出し元なし。 |
| `mixin/ItemEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ。設定に登録されておらずビルドにも乗らない。 |
| `mixin/PersistentProjectileEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ（旧 Yarn 名 `PersistentProjectileEntity`＝NeoForge の `AbstractArrow`）。Fabric時代の残骸。 |
| `mixin/MeleeAttackGoalAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ。未使用。 |
| `mixin/ProjectileEntityAccessor` | Accessor Mixin | 0参照 + **mixins.json 未登録** | 孤立アクセサ（追加検証で発見）。未使用。 |
| `client/screen/component/ListGUI` (200行) | GUI | 0参照 | `new ListGUI` / `extends ListGUI` ともに0件。`MutableListGUI` が上位互換。完全なデッドコード。 |
| `client/screen/component/ScrollBar` (119行) | GUI | 0参照 | `MutableScrollBar` のみが実際に使われ、こちらは未インスタンス化。`MutableScrollBar` が上位互換（防御的 null チェック・`elemSize` 可変）。 |

**小計: 9クラス・約700行超を安全に削除（実施済み）。**

### 1-B. NeoForge 標準機能で代替済み / 既に正しく移行済み（参考）

- ネットワーク（`network/` の `*Payload` + `NetworkHandler`）は既に NeoForge の `CustomPacketPayload` / `RegisterPayloadHandlersEvent` ベースに移行済み。**追加対応不要**（旧Forge `SimpleChannel` 等の残骸はなし）。
- 登録は `DeferredRegister` / `DeferredHolder`、設定は `ModConfigSpec`、属性は `EntityAttributeCreationEvent` 等、NeoForge 標準APIを使用済み。旧 `net.minecraftforge` import は存在しない。

### 1-C. 名前はレガシーだが「保持」すべきもの（削除候補ではない）

- `resource/classloader/MultiModelClassLoader`, `MultiModelClassTransformer` — 外部モデルパック互換ローダー（保持）
- `maidmodel/**`（`Model*`, `EntityCaps`, `ModelCapsHelper`, `ModelRenderer`, `ModelBoxBase` 等、200+参照）— モデル描画コア（保持）
- `maidmodel/compat/GLCompat`（39参照）— GL11→PoseStack 互換シム（保持）
- `entity/EntityLittleMaid` — transformer のリマップ先スタブ（保持）

---

## 2. 統合推奨クラス調査結果 (Classes to Merge)

### 2-A. デッド上位互換ペアの一本化（低リスク）→ **Step 2 で実施済み**
- `MutableListGUI` → **`ListGUI` にリネーム統合**（旧 `ListGUI` 削除後）。4階層ピラミッド（`ListGUI`→`MutableListGUI`→`ScrollableListGUI`→`FilterableListGUI`）を3階層へ。実利用は `FilterableListGUI`（4スクリーン全て）と `ScrollableListGUI` のみ。
- `MutableScrollBar` → **`ScrollBar` にリネーム統合**（旧 `ScrollBar` 削除後）。

### 2-B. 価値の低い単一実装インターフェースの畳み込み（参照解析後の確定判定）
全候補の「型としての多態利用」を grep で精査した結果、除去できるのは1件のみだった。

- ✅ `entity/util/AimingPoseable`（11行）→ **除去済み**。多態消費者ゼロ（`LittleMaidEntity` のみ実装、`AbstractArcherMode.mob` は `LittleMaidEntity` 直接型）。2メソッドを `LittleMaidEntity` に直接保持。
- ❌ `entity/util/HasMovingMode` → **保持**。`HasMMFollowTameOwnerGoal<T extends TamableAnimal & HasMovingMode>` のジェネリック境界として実利用。
- ❌ `entity/util/SalaryBoxPosListener` → **保持**。`SalaryBoxBlockEntity` が `instanceof SalaryBoxPosListener` + キャストで通知する observer 境界（block→entity の脱結合）。
- ❌ `entity/compound/SoundPlayable` → **保持**。`LittleMaidEntity` と `MultiModelEntity` の2実装があり、`NetworkHandler`/`MoveToDropItemGoal` 等で型として利用される多態契約。

### 2-C. Interface + Impl ペアの統合 → **中止（統合不可）**
当初は単一実装の過剰抽象と見ていたが、参照解析の結果いずれも **Mixin で vanilla クラスに注入されるクロスカッティング契約**であり、interface と impl は別物（impl はエンティティが内部 compose する委譲先、interface は vanilla 側にも staple される）。畳み込むと mixin 注入実装が壊れる。**3件とも保持。**

- ❌ `entity/mode/HasMode` → 保持。`ModeWrapperGoal<T extends LivingEntity & HasMode>` の境界。
- ❌ `entity/targeting/TargetTagManager` → 保持。`MixinPlayerEntity implements TargetTagManager`（vanilla Player に注入）。`NetworkHandler`/`TargetingSystem`/`TargetTagScreen` で型・境界・instanceof として多用。
- ❌ `entity/util/MaidManager` → 保持。`MixinServerPlayerEntity implements MaidManager`（vanilla ServerPlayer に注入）。`((MaidManager) player)` キャストや `MaidManager.LMInfo`/`Status` ネスト型が network/screen/entity で多用。

### 2-D. 単一サブクラス Goal 基底クラスの統合（積極方針）
非LM基底クラスは**いずれも対応する唯一のLMサブクラスからのみ**参照される（検証済み）。汎用の再利用余地はあるが現状ゼロのため、積極方針として統合候補とする。

| 基底（唯一の利用者） | サブクラス | 方針 |
|---|---|---|
| `FollowTameOwnerGoal` | `HasMMFollowTameOwnerGoal` | 統合 or 基底のジェネリクスを残しつつ畳み込み |
| `TeleportTameOwnerGoal` | `LMTeleportTameOwnerGoal` | 同上 |
| `HealMyselfGoal` | `LMHealMyselfGoal` | 同上 |
| `MoveToDropItemGoal`(abstract) | `LMMoveToDropItemGoal` | abstract を具象へ畳み込み |
| `StoreItemToContainerGoal`(abstract) | `LMStoreItemToContainerGoal` | 同上 |
| `CollectItemFromContainerGoal`(abstract) | `LMCollectSalaryFromContainerGoal` | 同上 |
| `StareAtHeldItemGoal` → `TameableStareAtHeldItemGoal` → `LittleMaidEntity.LMStareAtHeldItemGoal`(inner) | 3段 → 1〜2段へ縮約 |

> 注: ここは「クラス数削減」と「将来の汎用再利用性／可読性」のトレードオフ。各ペアは現状きれいに分離されており統合効果は中程度。**着手前に1ペアを試行し差分を確認**してから横展開する。

### 2-E. resource サブシステムの統合
- `resource/manager/`（`LMModelManager`/`LMTextureManager`/`LMConfigManager`）＋ `client/resource/manager/LMSoundManager` は、いずれも `HashMap<String,Holder>` を持つ薄いシングルトン。共通の登録APIへ統合（汎用 `ResourceRegistry` もしくは共通基底）。
- `resource/loader/`（`LMConfigLoader`/`LMTextureLoader`/`LMMultiModelLoader`）＋ `client/resource/loader/LMSoundLoader` は同一の `LMLoader.load()→manager.addX()` パターン。共通ローダー基底を抽出し各ローダーを薄いアダプタ化。
- `resource/holder/ConfigHolder`（DTO）→ **record 化**。`TextureHolder` も検索ロジックを static ヘルパへ寄せて簡素化。

### 2-F. MM* 描画ラッパー層の簡素化（要注意・後半フェーズ）
`multimodel/layer/` の `MMVertexConsumer`（純粋な VertexConsumer 委譲）・`MMMatrixStack`（PoseStack エイリアス）・`MMPose`（Pose の BiMap ラッパー）・`MMRenderContext`（DTO）は薄い抽象層。`IMultiModel` のシグネチャを vanilla 型（`VertexConsumer`/`PoseStack`）に寄せて層を削減可能。
> **リスク**: `IMultiModel` は `maidmodel/` の各 `Model*` 実装と transformer 経由で読み込む**外部モデルパック**が依存する境界。シグネチャ変更は外部互換を壊しうるため、**互換ローダーの想定インターフェースを確認するまで実施しない**（最終フェーズ・別途検証）。

### 2-G. 設定の不整合整理
`LittleMaidNeo` で `LMMLConfig.SPEC`（名前省略＝既定 `littlemaidneo-common.toml`）と `LMRBConfig.SPEC`（明示 `littlemaidneo-common.toml`）が**両方 COMMON で同一ファイル名**を指す。二重登録の不整合。`LMMLConfig`（3項目）を `LMRBConfig` に統合するか、別ファイル名を付与して解消。

---

## 3. 削除・統合実行プラン (Action Plan)

各 Step は独立して `./gradlew compileJava`（クライアント分は `compileJava` + 可能なら `runData`/`runClient`）が通る粒度に分割。Step ごとにコミット。

### Step 0: 安全網
- ビルドが現状通ることを確認（`./gradlew build`）。
- 作業ブランチ `claude/neoforge-cleanup-refactor-FKQVY` で実施。

### Step 1: デッドコード削除（最低リスク・即効）— ✅ 実施済み
- §1-A の9クラスを削除（`Pos2d`, `SightUtil`, `ColorConverter`, 孤立アクセサ4種, `ListGUI`, `ScrollBar`）。
- 削除後 `grep` で参照ゼロを再確認 → `compileJava`。
- **テスト**: コンパイル成功。GUI スクリーン4種（MaidManager/ModelSelect/TargetTag/SoundPack）が起動・スクロール動作すること。

### Step 2: 上位互換ペアの一本化（§2-A）— ✅ 実施済み
- `MutableListGUI`→`ListGUI`、`MutableScrollBar`→`ScrollBar` へリネーム統合。参照箇所を更新。
- **テスト**: 各スクリーンの一覧表示・フィルタ・スクロールバー挙動。

### Step 3: 薄い単一実装インターフェースの畳み込み（§2-B）— ✅ 部分実施済み
- `AimingPoseable` のみ除去済み（多態消費者ゼロ）。`HasMovingMode`/`SalaryBoxPosListener`/`SoundPlayable` は参照解析の結果 load-bearing と判明し**保持**（§2-B 参照）。
- **テスト**: 弓の構えポーズが従来通り動くこと（`AbstractArcherMode`→`LittleMaidEntity.setAimingBow`）。

### Step 4: 設定の二重登録解消（§2-G）— ✅ 実施済み
- `LMMLConfig.SPEC` に独立ファイル名 `littlemaidneo-lmml-common.toml` を付与し、`LMRBConfig`（`littlemaidneo-common.toml`）との衝突を解消。
- 2つの config はアクセスパターンが異なる（`LMMLConfig` は静的 getter、`LMRBConfig` は bean+`bake()`）ため、**1クラスへの完全統合は見送り**（6呼び出し箇所の API 書き換えを伴い、コンパイル検証なしでは高リスク。環境復帰後に任意で実施）。
- **テスト**: `config/` に両 toml が衝突なく生成され、voiceVolume/enableAlpha/debugMode が機能。

### Step 5: Interface+Impl 統合（§2-C）— ❌ 中止
- `HasMode`/`TargetTagManager`/`MaidManager` はいずれも Mixin で vanilla Player/ServerPlayer に注入される多態契約と判明（§2-C 参照）。畳み込むと mixin 注入実装が壊れるため**実施しない**。

### Step 6: resource サブシステム統合（§2-E）— ⏸ 環境復帰後（要コンパイル検証）
- `ConfigHolder` の record 化 → manager 群の共通化 → loader 共通基底抽出、の順。
- `LittleMaidNeo.initFileLoader/initModelLoader/initTextureLoader/initSoundLoader` の配線を更新。
- singleton/loader/wiring に跨る実リファクタのため、`./gradlew compileJava` が通る環境で実施すること。
- **テスト**: `LMMLResources` フォルダからのモデル/テクスチャ/設定/サウンド読み込み（外部パック）が従来通り動くこと。

### Step 7: 単一サブクラス Goal 基底の統合（§2-D）— ⏸ 環境復帰後（要コンパイル検証）
- 各基底は唯一の LM サブクラスからのみ使われるが、abstract/concrete の分離は明快で統合効果は中程度・可読性低下のリスクあり。まず1ペア（例 `HealMyselfGoal`+`LMHealMyselfGoal`）を試行統合し差分評価してから横展開。
- **テスト**: 各AI挙動（追従・テレポート・自己回復・給料回収・アイテム収集/格納・落下物回収・注視）。

### Step 8: MM* 描画層の簡素化（§2-F・最終・要事前検証）— ⏸ 環境復帰後
- 先に `MultiModelClassTransformer` のリマップ表と外部パックが想定する `IModelCaps`/`ModelRenderer`/`IMultiModel` 境界を精査し、**外部互換を壊さない範囲でのみ**着手。壊す場合は本フェーズを見送る。
- `MultiModel` と `LMMultiModel` の重複（後者は前者を継承し entity 参照保持のみ追加）を統合。
- **テスト**: 全モデル（Default/SR2/Aug/Archetype/Steve/Stef/Classic64/Slim64/Beverly7/Chloe2/Elsa5/AC/RX0）の描画、防具レイヤー、発光レイヤー、手持ちアイテム、頭部装飾、MaidSoul 描画。**さらに外部 LMM/MMM モデルパックの読み込み描画**を実機確認。

---

## 注意点・リスク（テストすべき項目）

1. **外部モデルパック互換（最重要）**: `classloader` + `maidmodel/` + `GLCompat` + `EntityLittleMaid` スタブ + `IMultiModel`/`MMRenderContext` 等の境界は外部 `.class` パックが依存。これらのシグネチャ・パッケージ・クラス名変更は外部互換を破壊する。Step 8 は特に慎重に、実パックでの読込テスト必須。
2. **NBT / セーブ互換**: `MaidManager`（ソウル/メイド情報）、`HasMode`（モードデータ）、`TargetTagManager`、`ItemContractable`（給料箱位置）の `write/read` シリアライズキーは変更しない。統合時もNBTタグ名・構造を維持し、既存ワールドのロード確認。
3. **パケット通信**: `network/*Payload` の `StreamCodec`/`Type` ID は変更しない（クライアント/サーバ間互換、既存接続）。今回の統合対象に payload 本体は含めないが、`MaidManager` 等の統合がペイロード内容に波及しないか確認。
4. **Mixin 設定の整合**: `mixins.json` 未登録の `CrossbowItemInvoker` が `LittleMaidEntity` から**参照されている**（潜在バグの可能性）。本クリーンアップとは別件として、クロスボウ発射が実機で機能するか確認し、必要なら `mixins.json` に登録（削除ではなく修正）。孤立アクセサ削除時に誤って稼働中 mixin を消さないよう、削除前に毎回 `mixins.json` と参照を照合。
5. **クライアント/サーバ分離**: resource 統合で `client/resource/*`（描画・サウンド）を共通化する際、サーバ専用環境でクライアント専用クラスをロードしないこと（`Dist.CLIENT` ガード維持、`onCommonSetup` 内の dist 分岐を尊重）。
6. **段階コミット**: 各 Step 後に `./gradlew compileJava` を通し、Step 単位でコミット。問題発生時に切り戻せる粒度を保つ。

### 検証方法（エンドツーエンド）
- 各 Step 後: `./gradlew compileJava`（＋ client）でコンパイル確認。
- 機能確認: `./gradlew runClient` でメイド召喚 → 契約・モード切替・各AI挙動・GUI操作・モデル/サウンド切替・給料箱・MaidSoul を一通り操作。
- 互換確認: 既存ワールドのロード（NBT）、外部モデルパック（`LMMLResources`）の読込描画、マルチプレイ接続（パケット）。

---

## ビルド環境メモ

- `maven.neoforged.net` が許可リストに含まれていないと NeoForge/Minecraft アーティファクト（`neoform-runtime` 等）を取得できず、`./gradlew compileJava` 以降が `403 Forbidden` で失敗する。Step 3〜8 のコンパイル検証には、このホストを許可リストに含むネットワークポリシーで環境を用意すること。
- toolchain は `build.gradle` で Java 25 を指定（MC 26.1.2 のエンドユーザー JRE に合わせる）。ローカルに JDK25 が必要。
