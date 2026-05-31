# TODO_systems — システム統合・記述量削減タスク

> 更新日: 2026-05-31
> 方針: **読み込むモデル／実際のモデル挙動・レンダリングに影響を与えない範囲** での
> Impl 削除・処理共通化・巨大クラス分割・薄い抽象の整理・記述量削減。
> LMRB 系（エンティティ／AI／モード）と LMML 系（モデルローダー／レンダリング）の融合過程で
> 蓄積した「薄い抽象・単一実装 interface・重複ボイラープレート」を棚卸しする。

## ⛔ 触ってはいけない load-bearing 基盤（再掲・厳守）

以下は外部ユーザー製モデルパック（`.class`）の ASM リマップ読み込みに不可欠。**本タスクの対象外**。
- `resource/classloader/MultiModelClassLoader`・`MultiModelClassTransformer`
- `maidmodel/` パッケージ全体（`ModelMultiBase`, `EntityCaps`, `ModelRenderer`, `IModelCaps` 等）と `maidmodel/compat/GLCompat`
- `entity/EntityLittleMaid`（Transformer のリマップ先スタブ）
- `multimodel/IMultiModel` のメソッドシグネチャ、`LMModelManager` の登録モデル名（"Default", "SR2" 等＝パック探索キー）

---

## 🔴 優先度: 高

### S-1. `Impl` クラスと単一実装 interface の統合（最優先・記述量削減効果大）

`interface + 単一 Impl` の薄い二重構造が 3 箇所ある。いずれも実装は 1 つだけで、ポリモーフィズムの実益がない。
interface を廃止して具象クラスへ統合する（または interface 名へ実装を畳む）。**挙動不変のリネーム／統合のみ**。

| 対象 interface | 単一 Impl | 利用箇所 | メモ |
|---|---|---|---|
| `entity/mode/HasMode` | `HasModeImpl` | `LittleMaidEntity`（`hasModeImpl` フィールド）のみ | `LittleMaidEntity` は既に `HasModeImpl` 具象型で保持。interface 不要 |
| `entity/util/MaidManager` | `MaidManagerImpl` | `MixinServerPlayerEntity`, `NetworkHandler`, `ClientNetworkHandler`, `MaidManagerScreen` | `LMInfo`（sealed）は interface 側に定義。畳む際は `LMInfo` の置き場所に注意 |
| `entity/targeting/TargetTagManager` | `TargetTagManagerImpl` | `MixinPlayerEntity`, `LittleMaidEntity`, `NetworkHandler`, `ClientNetworkHandler` | `Sync` 内部 interface も同様に整理可 |

- 統合方針: interface を消すと外部参照箇所の型を具象へ置換するだけで済む（上表の利用箇所はわずか）。
- ⚠️ 注意: `MaidManager.LMInfo`（sealed `MaidLMInfo`/`SoulLMInfo`/`SoulEntityLMInfo`）は NBT 互換のデータ構造。
  クラス移動はしても **フィールド構成・NBT キー名は不変**に保つこと（既存セーブ互換）。

### S-2. `Impl` 内の「インスタンス write/read」と「static write/read」の二重実装を解消

`MaidManagerImpl` と `TargetTagManagerImpl` は、同じシリアライズ処理を
**インスタンスメソッド**と**static メソッド**の両方で持っており、ロジックが重複している。

- `MaidManagerImpl#writeMaidManager` / `readMaidManager`（インスタンス、L35-51）
  ⇔ `MaidManagerImpl.write(output, list)` / `read(input, list)`（static、L90-104）
- `TargetTagManagerImpl#writeTargetTags` / `readTargetTags`（インスタンス、L127-153）
  ⇔ `TargetTagManagerImpl.write(map, output)` / `read(map, input)`（static、L135-168）

→ インスタンス側を static 側へ委譲させて単一ソース化（`NetworkHandler`/`ClientNetworkHandler` は static 版を直接利用中なので、その呼び出しは維持）。挙動・NBT フォーマット不変。

### S-3. `LittleMaidEntity`（2763 行）の機能分割

CLAUDE.md は `LMGoalInitializer` / `LMSafeMovement` / `LMInteractionHandler` / `MaidResurrection` /
`MaidSoul` への分割パターン採用と記載しているが、**実際にはこれらのクラスは存在しない**
（実在の委譲先は `LMHasInventory` と `LMItemContractable` のみ）。
ドキュメントと実態の乖離を解消しつつ、以下を実際に抽出する。
`super` 呼び出しを含む override 本体はクラスに残し、**ロジックのみ static ユーティリティ／ヘルパーへ委譲**する（CLAUDE.md のレビュー指針準拠）。

| 抽出候補 | 現在の位置 | 方法 | 障壁 |
|---|---|---|---|
| **復活演出** `resurrectionMaid()`（static） | L266-415（約150行） | `MaidResurrection` ユーティリティクラスへ全移動 | なし（既に static） |
| **Goal 登録** `registerGoals()` | L420-669（約250行） | 本体は残し `LMGoalInitializer.init(this, goalSelector, ...)` へ Goal 生成を委譲 | override（ラムダ遅延参照に注意・CLAUDE.md 既出） |
| **右クリック操作** `mobInteract()` ほか | L1639-1870 | 本体は残しアイテム別分岐を `LMInteractionHandler` へ委譲 | override |
| **安全移動** `maybeBackOffFromEdge()` ほか | L1428-1623 | 危険判定ロジックを `LMSafeMovement` ヘルパーへ抽出、override は残す | override |
| **本パラメータ適用** `applyParametersFromBook()` | L2678-2724 | `BookParameterParser` 等へ抽出 | なし |

- まず障壁のない static 系（復活演出・本パラメータ）から着手すると安全。
- `MaidSoul` 内部クラス（L2587 付近）は既に分離済みだが、別ファイル化の余地あり（NBT キー不変厳守）。

---

## 🟡 優先度: 中

### S-4. モード具象クラス間の重複ボイラープレート共通化

`entity/mode/` の各モードに同型コードが散在。挙動を変えずに共有ヘルパーへ集約する。

- **ブロックエンティティ探索＋キャスト**: `CookingMode`(L107-132), `PharmcistMode`(L279-304), `TorcherMode`
  → 汎用 `Optional<T> getBlockEntity(level, pos, Class<T>)` ヘルパー化。
- **インベントリ走査でスロット検索**: `CookingMode`, `HealerMode`(L65-89), `PharmcistMode`
  → `OptionalInt findSlot(Container, Predicate<ItemStack>)` ヘルパー化。
- **tick ベースの経路再計算タイマー**: `CookingMode`(L254-261), `PharmcistMode`(L87-89), `RipperMode`(L89-99), `TorcherMode`(L159-178)
  → `PathRecalcTimer`（decrement→閾値→`createPath`→`moveTo`）小ユーティリティ化。
- **コンテナ間アイテム移送**: `CookingMode`(L296-362 の `tryInsert*`/`tryExtract*`), `PharmcistMode`(L159-227)
  → 「空き/一致スロット探索→検証→移送」を共通メソッド化。

### S-5. `Modes.java` のモード登録をテーブル駆動化

現状は「`buildXxxMode()` 静的メソッド × 6」＋「static 初期化ブロックでフィールド代入」＋「`init()` で `register()` × 6」の三重定義。
ModeType・matcher を 1 つのテーブル（リスト）にまとめ、`init()` で一括ループ登録する形へ。
**登録される ModeType・ItemMatcher・Priority・登録順は完全維持**（モード判定挙動不変）。約30行削減。

### S-6. `HasMode` ⇔ `Mode` の NBT API 不整合の解消

`Mode#writeModeData/readModeData` は `CompoundTag` 直接、`HasMode`（`HasModeImpl`）は `ValueOutput/ValueInput`。
このため `HasModeImpl`(L73-75) で毎回 `CompoundTag` を生成して `store("ModeData", CompoundTag.CODEC, …)` するラッパが発生。
どちらかの API へ統一しラッパを除去。**NBT キー名（`ModeID`/`ModeData`）と格納フォーマットは不変**に保つこと（セーブ互換）。

### S-7. リソース系マネージャの重複正規化を集約（モデル挙動に無影響なもののみ）

`resource/manager/` の 3 マネージャ（Model/Texture/Config）で、`get()` ごとに `toLowerCase()` を都度実行。
→ **登録（`put`）時に 1 度だけ正規化**するよう統一し `get()` 側の重複を削減。
- ⚠️ 登録モデル名そのもの（探索キー）は変えない。小文字化の**タイミング**だけ整理する。

### S-8. `resource/util/ResourceHelper` のパス正規化重複を共通化

`getFileName()`(L27-32) / `getParentFolderName()`(L56-64) / `getTexturePackName()`(L35-47) が
それぞれ同じ `replace("\\","/")` 正規化を持つ → `normalizePath(path, isArchive)` へ抽出。挙動不変。

---

## 🟢 優先度: 低

### S-9. `LMFileLoader` のローダ適用ループ重複

`loadArchive()`(L89-90) と `loadFile()`(L112-113) が同一の
`loaders.stream().filter(...).forEach(loader -> loader.load(...))` を持つ → `applyLoaders(...)` ヘルパーへ。
**ローダ実行順・対象判定は不変**（読み込み結果に影響させない）。

### S-10. `LMModelManager` 内部クラス `ModelHolder` のインライン化

`ModelHolder`（L65-86、skin/inner/outer の三つ組ホルダー、`getModel(Layer)` のみ）は
`LMModelManager` 内部専用。レコード化 or マネージャ本体へ畳んで記述量削減。挙動不変。

### S-11. 薄い DTO/補助構造の整理

- `resource/util/TexturePair`（2 フィールド record）: 呼出側でのインライン可否を検討（低優先）。
- `resource/util/ArmorPart.Builder`（L51-76）: コンストラクタ／record で簡素化可（低優先）。
- `multimodel/layer/MMRenderContext.Renderer`（単一用途関数 interface）: ラムダ化検討（レンダリング呼び出し経路は不変厳守、慎重に）。

### S-12. `LittleMaidEntity` のフラグ／同期データのボイラープレート

`setLMMFlag()/getLMMFlag()`（L1933-1946）のビット操作が wait/aiming/begging/bloodSuck/strike/playingSnow 等で散発。
enum ベースのフラグラッパー化を検討（同期値・ビット位置は不変厳守＝ネット互換）。低優先・効果小。

---

## ⚠️ 本タスクの対象外（挙動が変わるため別途検討）

調査中に「状態が永続化されていないモード」（Archer/Fencer の cooldown、Healer の index 等の NBT 未保存）が
見つかったが、これは**挙動を変える修正**（リロード時の状態保持）のため、本 TODO（挙動不変リファクタ）の範囲外。
必要なら別タスク（バックログ）として `TODO.md` 側で扱う。

---

## 進め方メモ

1. 各項目は「コンパイル（`./gradlew compileJava`）が通る」「NBT キー名・同期データ・登録名・ローダ順序が不変」を満たすことを確認しながら小さく進める。
2. S-1/S-2（Impl 統合）→ S-5（テーブル駆動）→ S-4/S-7/S-8（共通化）→ S-3（大物の分割）の順が低リスク。
3. 分割・抽出を行った際は CLAUDE.md の記述（存在しないヘルパークラス名）も実態に合わせて更新する。
