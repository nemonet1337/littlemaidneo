# メイドさん描画のバニラ ModelPart 完全移行 実装計画

作成日: 2026-07-15
対象: LittleMaidNeo (MC 26.1.2 / NeoForge 26.1.2.64-beta / Java 25)
モジュール: apps/modelloader（本体）+ apps/mods（renderer/entity 側）
起点資料: `.kilo/plans/1781968811013-modelpart-migration.md`（当初案）、`.kilo/plans/1782566749246-maid-render-fullscreen-fix.md`（前回失敗のポストモーテム）

## 1. 背景・目的

外部モデルパック（`.class` 形式）互換基盤（保護コア A）を完全に廃止し、メイドさんの描画パイプラインをバニラ `ModelPart` + `EntityModel` に一本化する。現行は `GLCompat` による疑似即時モード（`ModelRenderer`/`ModelBox`/`TexturedQuad`）で頂点を直接 `VertexConsumer` に書き込んでおり、`MultiModel`/`LMMultiModel` は cube を持たない空シェルで、実描画は全てレイヤー（`MultiModelSkinLayer` 等）が担っている。

**前回、この移行を一度試みて失敗している**（未コミットのまま revert 済み。VCS に痕跡なし）。症状はテクスチャが画面一面に表示される致命的なもので、原因は `SubmitNodeCollector.submitCustomGeometry` の遅延実行 lambda 内で `snapPose`（スナップショット）ではなくライブ `poseStack` を使ったことによる（詳細は §3）。本計画はこの失敗の根本原因分析を踏まえ、同じ罠を踏まないための変換規則とチェックポイントを組み込む。

## 2. ユーザー確定事項

1. 保護コア A（外部 `.class` モデルパック互換）は**削除で確定**。CLAUDE.md の保護コア A 記述と ADR 0001 も本移行に合わせて更新・supersede する。
2. 保護コア B（外部ボイスパック）は**不変**。`LMMLResources/` 探索パス自体はボイスパック＋**テクスチャパック**用に残る（削除するのは `.class` モデル読み込み経路のみ。`LMFileLoader`/`LMTextureLoader`/`LMConfigLoader` は無関係で維持）。
3. 進め方は**一括移行**。ただしフェーズごとにコンパイル確認と WIP コミットを残し、ビセクト可能にする。最終段階で `runClient` により全 13 モデル×テクスチャ切替の目視検証を行う。

## 3. 前回失敗の根本原因（確定済み・対策込み）

| # | 症状 | 根本原因 | 今回の対策 |
|---|---|---|---|
| 1 | 画面一面に胴体（最重要） | 新レイヤーが `submitCustomGeometry(poseStack, renderType, (snapPose, consumer) -> ...)` のラムダ内で **`snapPose` ではなくライブ `poseStack`** を参照。lambda は遅延実行され、その時点で `poseStack` は pop 済み＝ identity → 巨大化して画面原点に描画 | **鉄則**: 全レイヤーで必ず `PoseStack local = new PoseStack(); local.last().set(snapPose); model.render(local, consumer, ...)` パターンを使う（golden reference: `apps/mods/.../client/renderer/MaidSoulRenderer.java:50`）。コードレビュー観点としてチェックリスト化（§9） |
| 2 | デフォルト肌とカスタム肌の二重描画 | `MobRenderer` の `super.submit` がベイク済みデフォルトモデルを描画してしまい、レイヤーのカスタム肌と重なる | ベースモデルは常に空 `new ModelPart(List.of(), Map.of())` にし、実描画は 100% レイヤーに一任する（現行の `MultiModel` と同じ思想を継続） |
| 3 | 鎧システムのスタブ退化 | 前回は鎧を skinRoot 近似に潰し `getName`/`updateArmorPart` を機能停止のスタブにした → 鎧テクスチャ解決が壊れる | `ArmorSets<ArmorPart>` 構造と `TextureHolder.getArmorTexture`（ダメージ段階フォールバック込み）のテクスチャ解決ロジックは**そのまま**維持し、変わるのは「モデルの型」だけにする |
| 4 | 原因特定不能なまま revert | 全変更が一つの未コミット diff | フェーズごとに `git add -A && git commit -m "wip: ..."` する（§8 参照）。壊れたら `git bisect` 可能にする |

補足でテクスチャ破損の要因になり得るもの（前回の失敗要因ではないが要注意）:
- モデル別テクスチャサイズの取り違え（RX0/Chloe2/Beverly7=128×64、Elsa5=64×64、他=64×32）を `LayerDefinition.create(mesh, texW, texH)` に正しく渡さないと UV 全体がずれる。
- `setMirror(true)` は「以降の全 addBox に効く persistent state」であり、単発の引数ではない。素朴に `CubeListBuilder.mirror()` を都度リセットし忘れると U 反転漏れ/過剰反転が起きる。

## 4. 新アーキテクチャのクラス設計

依存方向 `mods → modelloader → common` は不変。

### 4.1 `apps/modelloader` 側（新規）

- **`maidmodel/LMModel.java`**（新規、`ModelMultiBase`/`IMultiModel`/`IModelCaps` を置換）
  - `abstract class LMModel<S extends MultiModelRenderState> extends EntityModel<S>`
  - コンストラクタ: `protected LMModel(ModelPart root)`（バニラ `EntityModel` の標準パターン）
  - 各モデルは **3 つの `ModelPart` ルート**（skin/inner/outer）を保持する「案 C」方式を採用:
    - `protected static MeshDefinition createMesh(CubeDeformation deform)` を各サブクラスで実装（旧 `initModel(psize, pyoffset)` の直接変換）
    - `LMModel` 側に静的ヘルパー `protected static LayerDefinition bake(MeshDefinition mesh, CubeDeformation deform, int texW, int texH)` を用意し、`createMesh(deform)` → `LayerDefinition.create(mesh, texW, texH)` をラップ
    - モデルごとの inner/outer deform 値は **モデルファミリー依存**（旧 `getArmorModelsSize()` 相当）:
      - `ModelLittleMaidBase` 系（Orign/SR2/Aug/Archetype/Beverly7/Chloe2/Elsa5/AC/RX0）: inner=`CubeDeformation.extend(0.1F)`, outer=`CubeDeformation.extend(0.5F)`
      - `ModelMulti_*`（Steve/Stef/Classic64/Slim64）系（MMM 系譜）: inner=`CubeDeformation.extend(0.5F)`, outer=`CubeDeformation.extend(1.0F)`
    - これを表現するため `LMModel` に `protected abstract CubeDeformation innerDeform(); protected abstract CubeDeformation outerDeform();`（もしくはファミリー基底クラス `LMHumanoidModel`/`LMMultiModel` に既定値を持たせて13モデルは継承のみ）
  - `setupAnim(S state)` — 直接 `state` フィールド参照（`LivingEntity` への直接アクセスは行わず、render state 経由に統一。理由: `MobRenderer.extractRenderState` は非 render スレッドで呼ばれ得る設計を尊重するため）
  - 保持するアーマー可視性API: `int showArmorParts(Part part, int layerPartIndex)` は **廃止**し、代わりに `void setPartVisible(Part part, boolean visible)` を `ModelPart.visible` ベースで実装（旧 `showArmorParts` の「1 legacy ModelRenderer = 1 named ModelPart」の粒度をそのまま踏襲。§5 の対応表参照）
  - `getArmorOffset()` は不要（deform はコンストラクタ時点で確定するため）
  - `renderFirstPersonHand()`/`renderItems()`/`adjustHandItem()` は維持するが、内部実装を `PoseStack` + `ModelPart.translateAndRotate` ベースに書き換える（§6）

- **13 モデルクラス**（`OrignModel`, `SR2Model`, `AugModel`, `ArchetypeModel`, `SteveModel`, `StefModel`, `Classic64Model`, `Slim64Model`, `Beverly7Model`, `Chloe2Model`, `Elsa5Model`, `ACModel`, `RX0Model`）
  - 各 `createMesh(CubeDeformation)` static メソッド + コンストラクタで `ModelPart` フィールドを `root.getChild(name)` で取得
  - 命名は旧 `ModelRenderer` フィールド名を維持（`bipedHead`, `bipedTorso` 等）— レビュー時に旧コードと1:1対応させるため

- **`client/renderer/LMSkinLayer.java`**（旧 `MultiModelSkinLayer` 置換）
  - `snapPose` を必ずローカル `PoseStack` にコピーしてから `render`（§3 鉄則）
- **`client/renderer/LMArmorLayer.java`**（旧 `MultiModelArmorLayer` 置換）
  - INNER-normal / INNER-light / OUTER-normal / OUTER-light の4パス順序を維持
  - 旧 `showArmorParts(partIndex, layerPartIndex)` の代わりに、パート単位で `setPartVisible` を呼んでから該当 `ModelPart` サブツリーのみ render
- **`client/renderer/LMLightLayer.java`**（旧 `MultiModelLightLayer` 置換）
- **`client/renderer/LMHeldItemLayer.java`**（旧 `MultiModelHeldItemLayer` 置換）— `MMMatrixStack` 依存除去、`PoseStack` 直接
- **`client/renderer/MultiModelRenderer.java`**（既存ファイルを書き換え）— `LivingEntityRenderer<MultiModelEntity, MultiModelRenderState, LMModel<MultiModelRenderState>>` に変更
- **`client/renderer/MultiModelRenderState.java`**（既存ファイルを書き換え）— `IModelCaps caps` 除去、`IMultiModel` → `LMModel`、`innerModels/outerModels` 配列 → 単一 `ArmorRenderState[4]`（新規 record、§5）
- **`resource/manager/LMModelManager.java`**（書き換え）
  - `Map<String, ModelFactory> models`（`ModelFactory` は3ルートを保持する新 record。旧 `ModelHolder(IMultiModel skin, inner, outer)` の型だけ `LMModel` に変える）
  - `addModel(String modelName, Supplier<LMModel<?>> skinFactory, Supplier<LMModel<?>> innerFactory, Supplier<LMModel<?>> outerFactory)` — 旧 `buildHolder` のリフレクション経由インスタンス化をやめ、モデル登録側（`LittleMaidNeo.initModelLoader()`）で明示的にラムダを渡す（型安全性向上、リフレクション例外ハンドリング不要に）
  - `getModel(String, Layer)` / `getOrDefaultModel(String, Layer)` のシグネチャは維持（呼び出し側の変更を最小化）

- **`entity/compound/IHasMultiModel.java`**（書き換え）
  - `Optional<LMModel<?>> getModel(Layer layer, Part part)` （`IMultiModel` → `LMModel`）
  - `getCaps()` は**削除**（`IModelCaps` 廃止。寸法取得系は下記の通り直接メソッド化）
  - 寸法取得: `getWidth()`/`getHeight()`/`getEyeHeight()`/`getMountedYOffset()`/`getyOffset()`/`getLeashOffset()` は `IModelCaps caps, MMPose pose` 引数を取っていたが、`Pose pose` （バニラ標準 enum）のみを取る形に簡略化し、`LittleMaidEntity` 側から直接呼べるようにする
  - `getTexture(Layer, Part, boolean)` は維持

- **`entity/compound/MultiModelCompound.java`**（書き換え）
  - `ArmorSets<ArmorPart>` 構造・`TextureHolder` 解決ロジックは**変更しない**（§3 対策3）。`ArmorPart` の中身の型を `IMultiModel` → `LMModel` に置換するだけ

### 4.2 `apps/mods` 側

- **`client/renderer/MaidModelRenderer.java`**（書き換え）
  - `MobRenderer<LittleMaidEntity, MaidRenderState, LMModel<MaidRenderState>>` を継承。コンストラクタは `super(ctx, new NoopMaidModel(), 0.5F)`（`NoopMaidModel` は `LMModel` を継承する空実装、または `new LMModel<>(new ModelPart(List.of(), Map.of())) {}` の匿名クラス）
  - `extractRenderState()` — `IMultiModel`/`IModelCaps` 依存を除去し、`entity` の getter から直接値を state に詰める（`.kilo` 移行計画付録のグループA〜Eの caps→renderState マッピングをそのまま適用。§5 参照）
  - `syncCaps()` は**廃止**（caps バスがなくなるため）。代わりに `setupAnim` 内でモデルが `state` から直接読む
- **`MaidRenderState.java`**（書き換え）— caps 由来フィールドを `.kilo` 付録の対応表通りに追加

## 5. データフロー変更（caps → renderState）

`.kilo/plans/1781968811013-modelpart-migration.md` 末尾の「付録: IModelCaps 定数の完全な使用状況分析」（グループA〜E）をそのまま実装仕様として採用する。要点のみ再掲:

- **グループA**（syncCaps由来）→ `MaidRenderState` のフィールドに直接変換（`swingProgressRight/Left`, `isPassenger`, `isCrouching`, `isBaby`, `isAimingBow`, `ageInTicks`, `isWait`, `isContract`, `isBloodSuck`, `isHoldingClock` 等）
- **グループB**（EntityCaps動的読み取り）→ `setupAnim` 内で `entity` に直接アクセスするか `renderState` に追加（`swimAmount`, `isFallFlying`, `getFallFlyingTicks`, `isAutoSpinAttack`, `xRot/yRot`, `deltaMovement`, `isInWater`, `isSwimming`, `isBlocking`, `isLeashed` 等）
- **グループC**（メイド特化）→ `MaidRenderState` に新規フィールド追加（`interestedAngle`, `isBegging`, `isFreedomMode`, `isTracerMode`, `isPlayingSnow`, `isWorking`, `isPlanter`, `isOverdrive`, `activeJobName`）
- **グループD**（デッドコード: `caps_isCamouflage`, `caps_isOverdriveDelay`, `caps_PartsVisible`, `caps_isWorkingDelay`, `caps_motionSitting`）→ **モデル変換時に対応する if 分岐ごと削除**（永久に実行されないコードなので移植しない）
- **グループE**（syncCapsとEntityCapsの重複）→ 一本化して `renderState` の該当フィールドのみ使用

### アーマー可視性の新表現

旧 `showArmorParts(int parts, int layerPartIndex)` は「レイヤーパス（INNER-normal/INNER-light/OUTER-normal/OUTER-light）ごとに、どの `ModelRenderer` を見せるか」を表現していた。新方式:

```java
public record ArmorRenderState(
    LMModel<?> innerModel, LMModel<?> outerModel,
    Identifier inner, Identifier innerLight,
    Identifier outer, Identifier outerLight,
    boolean visible, boolean glint) {}
```
（`.kilo` の fullscreen-fix プランで既に設計済みの record をそのまま採用）

`LMArmorLayer` は `Part` ごとに対象 `ModelPart`（旧 `showArmorParts` が触っていた粒度と同じ — 旧 `ModelRenderer` 1つにつき新 `ModelPart` 1つ）の `visible` を trueにしてから render し、直後に false に戻す（または render 対象を `ModelPart` の部分木参照に絞る）。

## 6. 機械的変換規則（13モデル共通）

| 旧 API | 新 API | 注意点 |
|---|---|---|
| `new ModelRenderer(this, texU, texV)` | `CubeListBuilder.create().texOffs(texU, texV)` | 1パートに複数 box がある場合、box ごとに `.texOffs(u,v)` を呼び直す（例: `bipedHead` は8box） |
| `.addBox(x, y, z, w, h, d, psize)` | `.addBox(x, y, z, w, h, d, deform)` | `deform` はモデルファミリーの `innerDeform()`/`outerDeform()`（§4.1）。skin は `CubeDeformation.NONE` |
| `.setRotationPoint(x, y, z)` | `PartPose.offset(x, y, z)`（`addOrReplaceChild` 第3引数） | |
| `.addChild(child)` | `parentDef.addOrReplaceChild("name", childCubes, childPose)` | 旧フィールド名をそのまま `"name"` に使う（レビュー時の対応関係維持） |
| `.rotateAngleX/Y/Z` | `part.xRot/yRot/zRot` | ラジアン、同一単位 |
| `.rotationPointX/Y/Z` | `part.x/y/z` | |
| `.showModel` | `part.visible` | |
| `.setMirror(true)`（persistent） | `CubeListBuilder.create().mirror()` を対象 box にのみ適用し、以降の box では明示的に `.mirror(false)` に戻す | 旧仕様は「set 以降ずっと効く」ため、変換時は影響範囲のbox全てに `.mirror()` を付け、影響範囲外には付けないことで等価にする（機械的コピペではなく手動確認が必要） |
| `this.textureWidth/textureHeight` | `LayerDefinition.create(mesh, texW, texH)` | **モデル別に必ず正しい値を使う**（表は下記） |

### モデル別テクスチャサイズ

| モデル | texW×texH |
|---|---|
| RX0, Chloe2, Beverly7 | 128×64 |
| Elsa5 | 64×64 |
| Orign, SR2, Aug, Archetype, Steve, Stef, Classic64, Slim64, AC | 64×32 |

### `ModelPlate`（目・センサー等の片面フラット矩形）

vanilla `ModelPart.Cube` には片面クアッド概念がないため、厚み0の `addBox(x, y, z, w, h, 0, deform)` として表現する。表面（カリングされない側）のUVは旧 `ModelPlate` の平面種別（`planeXY`/`planeZY` 等）に対応する面と一致することを個別に確認する。対象:
- 目: `SR2Model`, `Chloe2Model`, `Beverly7Model`, `Elsa5Model`（`eyeR`/`eyeL`）
- センサー: `AugModel`（4枚、`planeXY`と`planeZY`混在）

背面UVはズレるが `entityCutout`/`entityTranslucent` のバックフェースカリングにより不可視前提（前回調査で確認済み）。**変換後に目視で裏抜け・変な模様が見えないか個別確認すること。**

### 負・小数 deform（RX0 のみ）

RX0 は `psize-0.2F`, `psize-0.04F`, `psize+0.25F` のような box 単位の deform 差分を使う。`createMesh(CubeDeformation base)` 内で各 box に `base.extend(-0.2F)` のように **base からの相対値**を適用する（`CubeDeformation` はイミュータブルなので `base.extend(delta)` で新規生成される点に注意）。

### `setFaceTexture` / GL_TEXTURE 行列 UV シフトの監査

変換着手前に `grep -rn "setFaceTexture\|GL_TEXTURE" apps/modelloader/.../maidmodel/` で使用モデルを特定する。使用箇所があれば、静的 UV では再現不可能なため個別に設計判断する（恐らく未使用の可能性が高いが要確認）。

## 7. その他の維持事項

- **`caps_ScaleFactor` スケール**: `MaidModelRenderer.scale()` は維持（`renderState` の新フィールド `scaleFactor` から読む）
- **`setupTransform`（fall-flying/swimming/riptide 行列）**: `LMModel.setupAnim` 内、または `MaidModelRenderer.setupRotations`（`MobRenderer` の標準フック）に統合。`PoseStack` 直接操作に変換
- **`LMMultiModel.getHead()` の 0.9375(15/16) 係数**: `LMHeadFeatureRenderer` 用のヘッド `ModelPart` 取得ロジックとして、新 `LMModel` に `HeadedModel` 実装（`getHead()` が実 `ModelPart`（`root.getChild("head")` 相当）を返す）を持たせれば **係数自体が不要になる**（バニラ標準の座標系に一本化されるため）。要動作確認
- **発光（emissive）**: シェーダーは存在しない（`lmml_emissive` は未実装）。現行通り packed light `0xF00000` での再描画を維持する（`0xF000F0` への修正は本移行のスコープ外、別課題として TODO.md に残す）
- **held item / first-person hand**: `renderFirstPersonHand`/`adjustHandItem` は `ModelPart` チェーンの `translateAndRotate` ベースに書き換え、`LMHeldItemLayer` から呼ぶ

## 8. 実装フェーズ順序（コンパイルチェックポイント + WIPコミット）

1. **Phase 0**: `LMModel` 基底クラス・`LayerDefinition` 生成ヘルパー・`ArmorRenderState` record を新規追加（既存コードは無変更、コンパイルのみ通す）→ `wip: LMModel 基底クラス追加`
2. **Phase 1**: `OrignModel`（最もシンプル）を変換。`LMModelManager` に新旧両対応の一時的な登録を追加し、`"Default"` キーだけ新モデルに差し替え可能な状態にする → コンパイル確認 → `./gradlew :apps:mods:runClient` で目視（デフォルト肌のみ確認）→ `wip: OrignModel 変換`
3. **Phase 2**: `MaidModelRenderer`/`LMSkinLayer`/`LMArmorLayer`/`LMLightLayer`/`LMHeldItemLayer` を新パイプラインに書き換え（§3 鉄則を厳守）。この時点で `LMModelManager` はまだ旧13モデル中1つ（Orign）のみ新方式 → コンパイル確認 → runClient で Orign 肌＋鎧＋発光＋手持ちアイテムを目視 → `wip: レンダラ・レイヤー新パイプライン化`
4. **Phase 3**: 残り12モデルを変換（`SR2Model`→`RX0Model`の順、簡単なものから）。1モデルごとに `LMModelManager` 登録を差し替えてコンパイル＋目視確認 → モデルごとに `wip: {Model}Model 変換` コミット
   - 優先順位: SR2 → Archetype → Aug（plate混在で複雑） → Steve/Stef/Classic64/Slim64（MMM系、deform値が異なる） → Beverly7/Chloe2（128×64+plate+mirror） → Elsa5（64×64+plate） → AC（ほぼ空、簡単） → RX0（最複雑、負deform+deep tree）
5. **Phase 4**: `IHasMultiModel`/`MultiModelCompound`/`MultiModelRenderState`/`LMModelManager` の恒久的な型置換（`IMultiModel`/`IModelCaps` 完全除去）→ コンパイル確認 → `wip: caps/IMultiModel 完全除去`
6. **Phase 5**: `LittleMaidEntity` 側の寸法取得メソッド呼び出し・`getCaps()` 呼び出し箇所を新APIに追随。`ModelSelectScreen`/`MultiModelGUIUtil`/`ArmorModelGUI`/`MultiModelGUI`/`DummyModelEntity`/`MaidRandomizer` も同様 → コンパイル確認 → `wip: エンティティ・GUI 側の型更新`
7. **Phase 6**: `LittleMaidNeo.java` の `initFileLoader()`/`initModelLoader()` を書き換え（`LMMultiModelLoader`/`MultiModelClassLoader` 登録削除、13モデルを新ファクトリで登録）→ コンパイル確認 → `wip: モデルローダー初期化更新`
8. **Phase 7（点火不可逆点）**: 削除対象ファイル一括削除（§9）→ コンパイル確認 → `wip: 保護コアA 削除`
9. **Phase 8**: ドキュメント更新（CLAUDE.md、新ADR、TODO.md）→ `docs: 保護コアA廃止に伴うドキュメント更新`
10. **Phase 9**: 最終 `runClient` 全モデル目視検証（§10 チェックリスト）

各フェーズ末で最低 `./gradlew :apps:mods:compileJava` を実行し、Phase 1〜3・9 では `runClient` での目視も行う。

## 9. 削除対象ファイル（Phase 7 でのみ実行）

### 保護コア A 本体
- `maidmodel/compat/GLCompat.java`
- `resource/classloader/MultiModelClassLoader.java`
- `resource/classloader/MultiModelClassTransformer.java`
- `resource/loader/LMMultiModelLoader.java`
- `entity/EntityLittleMaid.java`

### ラッパー層
- `multimodel/layer/MMMatrixStack.java`
- `multimodel/layer/MMVertexConsumer.java`
- `multimodel/layer/MMRenderContext.java`
- `multimodel/layer/MMPose.java`

### インターフェース
- `multimodel/IMultiModel.java`
- `maidmodel/IModelCaps.java`
- `maidmodel/ModelCapsHelper.java`
- `maidmodel/EntityCaps.java`
- `apps/mods/.../entity/LittleMaidModelCaps.java`

### 旧モデル基底・ジオメトリ
- `maidmodel/AbstractModelBase.java`
- `maidmodel/ModelBase.java`
- `maidmodel/ModelMultiBase.java`
- `maidmodel/ModelMultiMMMBase.java`
- `maidmodel/ModelLittleMaidBase.java`
- `maidmodel/ModelRenderer.java`
- `maidmodel/ModelBoxBase.java`
- `maidmodel/ModelBox.java`
- `maidmodel/ModelPlate.java`
- `maidmodel/ModelStabilizerBase.java`
- `maidmodel/ModelStabilizer_WitchHat.java`
- `maidmodel/EquippedStabilizer.java`

### 旧13モデル本体（変換完了後）
- `maidmodel/ModelLittleMaid_Orign.java` 他12ファイル

### 旧レイヤー（新ファイルに置換済みなら削除）
- `client/renderer/MultiModelSkinLayer.java`
- `client/renderer/MultiModelArmorLayer.java`
- `client/renderer/MultiModelHeldItemLayer.java`
- `client/renderer/MultiModelLightLayer.java`
- `client/renderer/MultiModelRenderLayer.java`
- `client/renderer/MultiModel.java`
- `client/renderer/LMMultiModel.java`

**維持するもの（削除しないこと）**: `LMFileLoader`, `LMTextureLoader`, `LMConfigLoader`, `LMTextureManager`, `ResourceWrapper`, `LMPackProvider`（テクスチャ/ボイスパック用、保護コア B 及び新テクスチャパイプラインで継続使用）。

## 10. ドキュメント更新（Phase 8）

- **CLAUDE.md**: 「絶対不変の保護コア」セクションから保護コア A の記述を削除し、代わりに「メイドさん本体の描画はバニラ `ModelPart`/`EntityModel` を使用する（外部 `.class` モデルパック互換は廃止済み）」という記述に更新。保護コア B の記述はそのまま維持（`LMMLResources/` はテクスチャ・ボイスパック探索パスとして継続の旨を明記）
- **新 ADR**: `docs/adr/0005-migrate-maid-rendering-to-vanilla-modelpart.md` を作成し、ADR 0001 を supersede する（ADR 0001 の「ステータス」を「Superseded by 0005」に更新）。理由: 外部 `.class` パック互換の維持コストがバニラ描画パイプラインとの継続的な非互換リスク（MC バージョンアップごとの `RenderType`/`VertexFormat` 破壊的変更への追従負荷）を上回ると判断したこと、前回移行の失敗原因と対策を記録すること
- **TODO.md**: 「専用鎧モデル層」「鎧 glint 描画」「発光ライト値 0xF00000→0xF000F0 修正」などスコープ外事項を追記

## 11. 検証チェックリスト（§8 Phase 9、最終確認）

1. `./gradlew build` — 全モジュールフルビルド成功
2. `./gradlew :apps:mods:runClient` 起動、ワールド生成/ロード
3. 13モデル全てを `ModelSelectScreen` から選択し、各モデルで:
   - 肌本体のシルエット・UV が正しい（特に RX0=128×64+負deform, Elsa5=64×64, Beverly7/Chloe2=128×64+plate+mirror, Aug=plate混在）
   - 発光（light）レイヤーが正しく表示される
   - 鎧を4部位（HEAD/BODY/LEGS/FEET）× inner/outer で装備し、テクスチャがダメージ段階に応じて変化する
   - 未装備部位に鎧テクスチャが漏れない
   - 手持ちアイテム（右手・左手・弓構え）の位置が正しい
   - 頭部装飾（`LMHeadFeatureRenderer`）の位置が正しい
   - スケール変更（`caps_ScaleFactor` 相当）が効く
4. 契約・介抱・スニーク・乗馬・徒歩・遊泳・落下滑空など主要アニメーション状態で破綻がないこと
5. `./gradlew :apps:mods:compileJava` の警告に新規の未使用 import/デッドコードが残っていないこと
6. `docs/`・`CLAUDE.md`・`TODO.md` 更新内容が実装と整合していること

## 12. リスク

- モデルファイル変換量: 13ファイル×平均250行。Phase 3 は最も工数がかかる区間。
- `ModelPlate` の片面クアッド近似は目視確認が必須（機械変換だけでは保証できない）。
- `showArmorParts` → `ModelPart.visible` 切替方式は、旧「その部分木自身のcubeのみ隠し子は隠さない」という特殊挙動（`isRendering`）を使うモデルがあれば個別対応が必要（現状13モデルはランタイムで未使用と確認済み）。
- 検証はGameTestが0件・スクリーンショット基盤なしのため実質目視のみ。チェックリスト（§11）を漏れなく実施すること。
