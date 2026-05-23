# LittleMaidNeo — 実装状況 & TODO

> 調査日: 2026-05-23  
> ベース: `LMMBase/LittleMaidModelLoader-Architectury` + `LMMBase/LittleMaidReBirth-Architectury`  
> 方針: Architectury除去 / NeoForge専用 / `net.sistr` → `work.nemonet` / 2MOD統合

---

## ✅ 実装済み

### コアシステム (LittleMaidModelLoader 由来)
| パッケージ | ファイル数 | 内容 |
|---|---|---|
| `work.nemonet.littlemaidneo` (root) | 2 | `LittleMaidNeo.java`, `LittleMaidNeoClient.java` |
| `config/` | 1 | `LMMLConfig.java` |
| `setup/` | 2 | `Registration.java`, `ClientSetup.java` |
| `util/` | 1 | `Tuple.java` |
| `network/` | 4 | `NetworkHandler`, `SyncMultiModelPayload`, `SyncSoundPackPayload`, `LMSoundPayload` |
| `entity/` | 6 | `MultiModelEntity`, `EntityLittleMaid`, `IHasMultiModel`, `MultiModelCompound`, `SoundPlayable`, `SoundPlayableCompound` |
| `resource/classloader/` | 2 | `MultiModelClassLoader`, `MultiModelClassTransformer` |
| `resource/holder/` | 2 | `TextureHolder`, `ConfigHolder` |
| `resource/loader/` | 5 | `LMLoader`, `LMFileLoader`, `LMConfigLoader`, `LMMultiModelLoader`, `LMTextureLoader` |
| `resource/manager/` | 3 | `LMModelManager`, `LMTextureManager`, `LMConfigManager` |
| `resource/util/` | 8 | `TextureColors`, `TextureIndexes`, `LMSounds`, `ColorConverter`, `TexturePair`, `ArmorSets`, `ArmorPart`, `ResourceHelper` |
| `multimodel/` | 5 | `IMultiModel`, `MMMatrixStack`, `MMPose`, `MMVertexConsumer`, `MMRenderContext` |
| `maidmodel/` | 25 | 全モデルクラス (`AbstractModelBase` 〜 `ModelLittleMaid_RX0`) |
| `client/resource/` | 5 | `ResourceWrapper`, `LMPackProvider`, `LMSoundInstance`, `LMSoundLoader`, `LMSoundManager` |
| `client/renderer/` | 6 | `MultiModel`, `MultiModelRenderer`, `MultiModelRenderLayer`, `MultiModelArmorLayer`, `MultiModelHeldItemLayer`, `MultiModelLightLayer` |
| `client/screen/` | 2 | `ModelSelectScreen`, `SoundPackSelectScreen` |
| `client/screen/component/` | 17 | `GUIElement` 〜 `ArmorModelGUI` (全コンポーネント) |

**合計: 96 Java ファイル**

---

## ❌ 未実装 — 優先度: 高

### 1. build.gradle — 依存ライブラリの追加が必要

`MultiModelClassLoader` / `MultiModelClassTransformer` が使用する ASM と commons-io が未定義。
ビルドが通らない。

```groovy
// build.gradle の dependencies ブロックに追加する
implementation 'org.ow2.asm:asm:9.7'
implementation 'org.ow2.asm:asm-tree:9.7'
implementation 'commons-io:commons-io:2.15.1'
```

### 2. LittleMaidNeo.java — モデル登録漏れ

`initModelLoader()` に以下の2モデルが未登録:

```java
// 追加が必要
modelManager.addModel("AC", ModelLittleMaid_AC.class);
modelManager.addModel("RX0", ModelLittleMaid_RX0.class);
```

### 3. リソースファイル — 必須テクスチャ・GUI が欠落

以下を `LMMBase/LittleMaidModelLoader-Architectury/common/src/main/resources/assets/littlemaidmodelloader/` からコピー（namespace を `littlemaidmodelloader` → `littlemaidneo` に変更）:

| コピー元 (LMMBase内) | コピー先 (src/main/resources) |
|---|---|
| `assets/littlemaidmodelloader/textures/empty.png` | `assets/littlemaidneo/textures/empty.png` |
| `assets/littlemaidmodelloader/textures/gui/model_select.png` | `assets/littlemaidneo/textures/gui/model_select.png` |
| `assets/littlemaidmodelloader/lang/en_us.json` | `assets/littlemaidneo/lang/en_us.json` (内容更新) |
| `assets/littlemaidmodelloader/lang/ja_jp.json` | `assets/littlemaidneo/lang/ja_jp.json` |

バンドルスキンテクスチャ（約 100 枚以上の PNG）:
```
LMMBase/…/assets/minecraft/textures/entity/littlemaid/**/*.png
  → src/main/resources/assets/minecraft/textures/entity/littlemaid/
```

`en_us.json` の内容は現在テンプレートのままなので正しい翻訳キーに書き換えること。

---

## ❌ 未実装 — LittleMaidReBirth 統合 (大規模)

LittleMaidReBirth-Architectury の Java ソースは **一切移植されていない**。
これが「2MOD統合」の残り部分。ファイル数は約 80 ファイル。

### 3-1. メインエンティティ
| ソース (LMMBase内 net.sistr.littlemaidrebirth.*) | 移植先 (work.nemonet.littlemaidneo.*) |
|---|---|
| `entity/LittleMaidEntity.java` | `entity/LittleMaidEntity.java` |
| `entity/MaidSoulEntity.java` | `entity/MaidSoulEntity.java` |
| `entity/LittleMaidModelCaps.java` | `entity/LittleMaidModelCaps.java` |
| `entity/LittleMaidScreenHandler.java` | `entity/LittleMaidScreenHandler.java` |
| `entity/LMHasInventory.java` | `entity/LMHasInventory.java` |
| `entity/LMItemContractable.java` | `entity/LMItemContractable.java` |
| `entity/LMScreenHandlerFactory.java` | `entity/LMScreenHandlerFactory.java` |
| `entity/ItemContractable.java` | `entity/ItemContractable.java` |
| `entity/FixedMoveControl.java` | `entity/FixedMoveControl.java` |

### 3-2. AI ゴール (15 ファイル)
`entity/goal/` 以下を全て移植:
`CollectItemFromContainerGoal`, `FollowAtHeldItemGoal`, `FollowTameOwnerGoal`,
`FreedomGoal`, `HasMMFollowTameOwnerGoal`, `HealMyselfGoal`,
`LMCollectSalaryFromContainerGoal`, `LMHealMyselfGoal`, `LMMoveToDropItemGoal`,
`LMStoreItemToContainerGoal`, `LMTargetGoal`, `LMTeleportTameOwnerGoal`,
`MoveToDropItemGoal`, `PlaySnowGoal`, `PredicateRevengeGoal`,
`RedstoneTraceGoal`, `StareAtHeldItemGoal`, `StoreItemToContainerGoal`,
`TameableStareAtHeldItemGoal`, `TeleportTameOwnerGoal`, `WaitGoal`, `WaitWhenOpenGUIGoal`

### 3-3. モード system (10 ファイル)
`entity/mode/` 以下:
`AbstractArcherMode`, `AbstractBattleMode`, `AbstractFencerMode`,
`ArcherMode`, `CookingMode`, `FencerMode`, `HasMode`, `HasModeImpl`,
`HealerMode`, `ModeWrapperGoal`, `PharmcistMode`, `RipperMode`, `TorcherMode`

### 3-4. ターゲットシステム (5 ファイル)
`entity/targeting/`:
`TargetIdentifier`, `TargetTagManager`, `TargetTagManagerImpl`,
`TargetingConfig`, `TargetingSystem`

### 3-5. エンティティユーティリティ (10 ファイル)
`entity/util/`:
`AimingPoseable`, `Contractable`, `EPEntityUtil`, `GuiEntitySupplier`,
`HasInventory`, `HasMovingMode`, `MaidManager`, `MaidManagerImpl`,
`MovingMode`, `SalaryBoxPosListener`, `TameableUtil`

### 3-6. Mode API (9 ファイル)
`api/mode/`:
`IRangedWeapon`, `ItemMatcher`, `ItemMatchers`, `Mode`,
`ModeManager`, `Modes`, `ModeType`

### 3-7. ブロック / アイテム
| ソース | 移植先 |
|---|---|
| `block/SalaryBoxBlock.java` | `block/SalaryBoxBlock.java` |
| `block/SalaryBoxBlockEntity.java` | `block/SalaryBoxBlockEntity.java` |
| `item/LittleMaidSpawnEggItem.java` | `item/LittleMaidSpawnEggItem.java` |

### 3-8. クライアント (ReBirth 固有)
| ソース | 移植先 |
|---|---|
| `client/key/LMKeys.java` | `client/key/LMKeys.java` |
| `client/renderer/LMHeadFeatureRenderer.java` | `client/renderer/LMHeadFeatureRenderer.java` |
| `client/renderer/LMMultiModel.java` | `client/renderer/LMMultiModel.java` |
| `client/renderer/MaidModelRenderer.java` | `client/renderer/MaidModelRenderer.java` |
| `client/renderer/MaidSoulRenderer.java` | `client/renderer/MaidSoulRenderer.java` |
| `client/screen/LittleMaidScreen.java` | `client/screen/LittleMaidScreen.java` |
| `client/screen/MaidManagerScreen.java` | `client/screen/MaidManagerScreen.java` |
| `client/screen/TargetTagScreen.java` | `client/screen/TargetTagScreen.java` |

### 3-9. 設定・ネットワーク
- `config/LMRBConfig.java`
- `network/C2SCallWaitPacket`, `C2SOpenInventoryPacket`, `C2SSetBloodSuckPacket`,
  `C2SSetMovingStatePacket`, `C2SSetTargetTagsPacket` (+ 対応 S2C パケット)
- これらは Architectury Networking → NeoForge `CustomPacketPayload` に変換必要

### 3-10. Mixin (8 ファイル)
Fabric Mixin から NeoForge Mixin に変換。構成ファイルの書き方が異なる点に注意:

| ソース | 変換後 |
|---|---|
| `mixin/MixinAbstractFurnaceBlockEntity.java` | そのまま移植 (アノテーションは共通) |
| `mixin/MixinCandleCakeBlock.java` | 〃 |
| `mixin/MixinCrossBowItem.java` | 〃 |
| `mixin/MixinExperienceOrbEntity.java` | 〃 |
| `mixin/MixinItemEntity.java` | 〃 |
| `mixin/MixinPlayerEntity.java` | 〃 |
| `mixin/MixinRangedWeaponItem.java` | 〃 |
| `mixin/MixinServerPlayerEntity.java` | 〃 |
| `mixin/MixinSaddleItem.java` | 〃 |
| Accessor/Invoker 各種 | 〃 |

`src/main/resources/littlemaidneo.mixins.json` の作成と、  
`src/main/templates/META-INF/neoforge.mods.toml` への `[[mixins]]` セクション追加が必要。

### 3-11. Advancement Criterion
- `advancement/criterion/ContractMaidCriterion.java`
- `advancement/criterion/ResurrectMaidCriterion.java`

### 3-12. Registration への追加登録
`setup/Registration.java` に以下を追加:
- `LittleMaidEntity`
- `MaidSoulEntity`
- `SalaryBoxBlock`
- `SalaryBoxBlockEntity`
- `LittleMaidSpawnEggItem` (スポーンエッグ)
- モードの登録

### 3-13. LittleMaidNeo.java / LittleMaidNeoClient.java への統合処理追加
- `LittleMaidEntity` のアトリビュート登録
- キーバインド登録 (`LMKeys`)
- レンダラー登録 (`MaidModelRenderer`, `MaidSoulRenderer`)

---

## ❌ 未実装 — リソースファイル (ReBirth 由来)

以下を `LMMBase/LittleMaidReBirth-Architectury/common/src/main/resources/` からコピー、  
namespace `littlemaidrebirth` → `littlemaidneo` に書き換え:

```
assets/littlemaidneo/blockstates/salary_box.json
assets/littlemaidneo/models/block/salary_box.json
assets/littlemaidneo/models/block/salary_box_open.json
assets/littlemaidneo/models/item/little_maid_spawn_egg.json
assets/littlemaidneo/models/item/salary_box.json
assets/littlemaidneo/textures/gui/salary_window.png
assets/littlemaidneo/lang/en_us.json  (LML と ReBirth の内容をマージ)
assets/littlemaidneo/lang/ja_jp.json  (同上)
data/littlemaidneo/advancements/husbandry/contract_maid.json
data/littlemaidneo/advancements/husbandry/resurrect_maid.json
data/littlemaidneo/advancements/recipes/**
data/littlemaidneo/loot_tables/blocks/salary_box.json
data/littlemaidneo/loot_tables/entities/little_maid_mob.json
data/littlemaidneo/recipes/little_maid_spawn_egg.json
data/littlemaidneo/recipes/salary_box.json
data/littlemaidneo/tags/blocks/maid_alter_component_blocks.json
data/littlemaidneo/tags/items/*.json  (8 ファイル)
data/littlemaidneo/tags/worldgen/biome/*.json
```

---

## ⚠️ 要確認・修正

### neoforge.mods.toml のプレースホルダー
`src/main/templates/META-INF/neoforge.mods.toml` の以下を実際の内容に更新:
```toml
description='''
Example mod description.    ← 要変更
'''
```

### en_us.json の内容
`src/main/resources/assets/littlemaidneo/lang/en_us.json` は現在テンプレートのまま  
(`"itemGroup.littlemaidneo": "Example Mod Tab"` など)。  
LML / ReBirth の正しい翻訳キーと値に書き換える。

### ModelLittleMaid_AC / RX0 の未登録
`LittleMaidNeo.java` の `initModelLoader()` に以下を追加:
```java
modelManager.addModel("AC", ModelLittleMaid_AC.class);
modelManager.addModel("RX0", ModelLittleMaid_RX0.class);
```

---

## 作業優先順位

| 優先度 | 作業 |
|---|---|
| 🔴 今すぐ | `build.gradle` に ASM / commons-io 追加 → ビルド確認 |
| 🔴 今すぐ | 必須リソース (`empty.png`, `model_select.png`) をコピー |
| 🟠 次 | `ModelLittleMaid_AC` / `RX0` を `initModelLoader()` に登録 |
| 🟠 次 | `en_us.json` / `neoforge.mods.toml` のプレースホルダーを修正 |
| 🟡 その後 | LittleMaidReBirth 本体エンティティ (`LittleMaidEntity`) の移植 |
| 🟡 その後 | ゴール・モード・ターゲットシステムの移植 |
| 🟡 その後 | ブロック・アイテム・GUI (ReBirth) の移植 |
| 🟡 その後 | Mixin の移植 + `littlemaidneo.mixins.json` 作成 |
| 🟡 その後 | ネットワーク (ReBirth C2S/S2C) の移植 |
| 🟢 最後 | ReBirth 由来リソースファイルのコピー・namespace 書き換え |
| 🟢 最後 | バンドルスキンテクスチャ群のコピー |
