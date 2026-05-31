# LittleMaidNeo — システム統合における技術的困難と重要維持項目 (TODO_System)

今回の NeoForge 移行および 2MOD 統合プロセスにおいて、**単純なクリーンアップ・削除が困難であり、互換性保護や Mixin の構造上「意図的に現状維持」とした部分**、および**リファクタリング時に注意を要した設計上の罠**をここに記録します。

今後のメンテナンスや追加機能実装の際、以下のシステム境界を変更する場合は細心の注意を払ってください。

---

## 🚨 1. 外部モデルパック（.class 形式）の動的 ASM リマップ
* **関連ファイル**: 
  * [MultiModelClassLoader.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/resource/classloader/MultiModelClassLoader.java)
  * [MultiModelClassTransformer.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/resource/classloader/MultiModelClassTransformer.java)
  * [EntityLittleMaid.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/EntityLittleMaid.java) (リマップ用スタブ)
* **困難だった点**:
  * 旧 `LittleMaidMob` / `MultiModel` 時代の外部モデルパック（コンパイル済みの `.class` を含む zip）を、実行時に Java のクラスローダと ASM (ClassReader / ClassNode / ClassWriter) を用いて動的に読み込み、NeoForge / 自作パッケージ (`work.nemonet`) 向けにバイトコードレベルでリマップしています。
  * このインフラは非常に壊れやすく、少しでもパッケージ構成やメソッド名が変わると、外部モデルパックが一切読み込めなくなります。
* **対処方針**:
  * このサブシステムは **触らずに完全維持 (Load-bearing)** としています。

---

## 🎨 2. 描画ラッパー層 (`MMMatrixStack` / `MMVertexConsumer` など) の外部互換性保護
* **関連ファイル**: 
  * `maidmodel/` パッケージ全般
  * `maidmodel/compat/GLCompat`
* **困難だった点**:
  * バニラや NeoForge の標準型（`PoseStack` や `VertexConsumer`）に統一して描画ラッパー層（`MMMatrixStack`, `MMVertexConsumer`, `MMPose` 等）を削除する計画（Step 8）がありました。
  * しかし、これらのラッパー型は `ModelMultiBase` などのメソッド引数として直接露出しており、**外部のモデルパック（.class）がこれらのメソッドをオーバーライドしています**。
  * もし標準型に統一してしまうと、メソッドシグネチャが変わるため外部モデルパック読み込み時に `LinkageError` や `AbstractMethodError` が発生し、互換性が完全に崩壊します。
* **対処方針**:
  * 描画ラッパー層はリファクタリングを **中止し、外部互換性を守るために現状維持** としました。

---

## 🔀 3. Mixin による Vanilla 注入インターフェースと Impl の結合限界
* **関連ファイル**:
  * `entity/mode/HasMode` (および `MixinLivingEntity`)
  * `entity/targeting/TargetTagManager` (および `MixinPlayerEntity`)
  * `entity/util/MaidManager` (および `MixinServerPlayerEntity`)
* **困難だった点**:
  * これらは一見「1つの実装（Impl）しか持たない薄いインターフェース」に見えるため、統合可能に見えました。
  * しかし、これらは **Mixin を利用して Vanilla の既存クラス（`PlayerEntity`, `ServerPlayerEntity`）へ多態性契約（Interface）を動的に注入するための境界** です。
  * インターフェースを削除したり Impl クラスと強引にマージしてしまうと、Mixin 側で Vanilla クラスに実装を追加できなくなり、他のクラス（`ModeWrapperGoal` 等）での `instanceof` による多態性キャストが破綻します。
* **対処方針**:
  * Interface と Impl の分離構造は **Mixin の動作要件として必須であるため、統合不可として現状維持** としました。

---

## 🧠 4. 多階層 Goal 継承チェーンの縮約に伴うドミノ倒し
* **関連ファイル**:
  * `StareAtHeldItemGoal` (旧基底)
  * `TameableStareAtHeldItemGoal` (旧中間)
  * [FollowAtHeldItemGoal.java](file:///workspaces/littlemaidneo/src/main/java/work/nemonet/littlemaidneo/entity/goal/FollowAtHeldItemGoal.java)
  * `LittleMaidEntity.LMStareAtHeldItemGoal` (インナークラス)
* **困難だった点**:
  * メイドさん固有の挙動のために、Goal（AI目標）が「汎用 → テイム可能モブ用 → メイド専用」と3段階で継承されている構造がありました。
  * 中間の抽象基底クラスを単純に削除しようとすると、その基底を継承していた別の Goal（例: `FollowAtHeldItemGoal`）のフィールド（`mob` や `stareAt`）や `super.tick()` 呼び出しが破壊され、広範囲にコンパイルエラーが波及するドミノ倒しが発生しました。
* **対処方針**:
  * 単に基底クラスを削除するのではなく、依存していたサブクラス側（`FollowAtHeldItemGoal` 等）も**基底クラスに依存しない完全な自己完結型クラスへ書き換える**（ロジックやフィールドをすべて取り込んで一本化する）ことで解決しました。

---

# ✅ 着手可能なリファクタリング候補（統合・記述量削減）

> 上記 1〜4 の「現状維持」境界を**侵さない範囲**で、なお実施可能なクリーンアップ・共通化・分割のタスク一覧。
> 大前提: **読み込むモデル／実際のモデル挙動・レンダリングに影響を与えない**こと。
> 不変厳守: NBT キー名・同期データ・ビット位置・登録モデル名・ローダー実行順序。
> 検証: 各項目ごとに `./gradlew compileJava` が通ることを確認しながら小さく進める。

## 🟦 R-1. Impl の「インスタンス／static 二重実装」の解消（§3 の境界を侵さず可能）

§3 のとおり `MaidManager` / `TargetTagManager` の **interface 自体は Mixin 注入契約のため削除不可**。
一方、Impl 内部には同一シリアライズ処理が **インスタンスメソッドと static メソッドの両方**で重複している。
interface 境界は維持したまま、Impl 内部の重複だけ単一ソース化できる（挙動・NBT フォーマット不変）。
* `MaidManagerImpl#writeMaidManager/readMaidManager`（L35-51）⇔ `MaidManagerImpl.write/read`（static, L90-104）
* `TargetTagManagerImpl#writeTargetTags/readTargetTags`（L127-153）⇔ `TargetTagManagerImpl.write/read`（static, L135-168）
* 方針: インスタンス側を static 側へ委譲。`NetworkHandler`/`ClientNetworkHandler` は static 版を直接利用中なので呼び出しは維持。
* ⚠️ `MaidManager.LMInfo`（sealed `MaidLMInfo`/`SoulLMInfo`/`SoulEntityLMInfo`）のフィールド構成・NBT キーは不変（セーブ互換）。

## 🟦 R-2. `HasMode` / `HasModeImpl` の薄い二重構造（§3 とは別扱い・要再確認）

§3 は `HasMode` を `MixinLivingEntity` 経由の注入境界として列挙しているが、**現状 `MixinLivingEntity` は存在せず**、
`HasModeImpl` は `LittleMaidEntity` の**フィールド合成（コンポジション）**として保持されているのみ
（`instanceof HasMode` による多態キャストも未使用）。
→ Mixin 依存が無いなら interface を畳んで具象へ統合し記述量削減が可能だが、**§3 の記述と実態が食い違う**ため、
  まず「将来 Mixin で `LivingEntity` に注入する予定があるか」を確認してから判断する（誤って消すと将来の注入計画を壊す）。
* 確認が取れるまでは保留。確認後に統合する場合、§3 の記述も実態に合わせて更新する。

## 🟦 R-3. `LittleMaidEntity`（2763 行）の機能分割

`CLAUDE.md` は `LMGoalInitializer`/`LMSafeMovement`/`LMInteractionHandler`/`MaidResurrection`/`MaidSoul` への
分割採用と記載しているが、**実際にこれらのクラスは存在しない**（実在の委譲先は `LMHasInventory` / `LMItemContractable` のみ）。
ドキュメントと実態の乖離を解消しつつ、`super` を含む override 本体はクラスに残し、**ロジックのみ委譲**する。
* 復活演出 `resurrectionMaid()`（static, L266-415, 約150行）→ `MaidResurrection` ユーティリティへ全移動（障壁なし・着手容易）。
* 本パラメータ適用 `applyParametersFromBook()`（L2678-2724）→ `BookParameterParser` 等へ抽出（障壁なし）。
* Goal 登録 `registerGoals()`（L420-669, 約250行）→ 本体は残し Goal 生成を `LMGoalInitializer.init(...)` へ委譲。
  ⚠️ §4 のとおり Goal 継承チェーンは脆い。**Goal クラス自体の継承構造は変えず**、登録呼び出しの外出しに留める。
  ⚠️ `initGoals()` は `Mob` コンストラクタ内で呼ばれフィールド未初期化。委譲はラムダ遅延参照（CLAUDE.md 既出）。
* 右クリック `mobInteract()`（L1639-1870）→ アイテム別分岐を `LMInteractionHandler` へ委譲、override 本体は残す。
* 安全移動 `maybeBackOffFromEdge()` ほか（L1428-1623）→ 危険判定を `LMSafeMovement` へ抽出、override は残す。
* 着手順は「障壁なしの static 系（復活演出・本パラメータ）」→「override 委譲系」を推奨。

## 🟦 R-4. モード具象クラス間の重複ボイラープレート共通化

`entity/mode/` の各モードに同型コードが散在。挙動を変えずに共有ヘルパーへ集約する。
* ブロックエンティティ探索＋キャスト: `CookingMode`(L107-132), `PharmcistMode`(L279-304), `TorcherMode` → `Optional<T> getBlockEntity(level,pos,Class<T>)`。
* インベントリ走査でスロット検索: `CookingMode`, `HealerMode`(L65-89), `PharmcistMode` → `OptionalInt findSlot(Container, Predicate<ItemStack>)`。
* tick ベース経路再計算タイマー: `CookingMode`(L254-261), `PharmcistMode`(L87-89), `RipperMode`(L89-99), `TorcherMode`(L159-178) → `PathRecalcTimer` 小ユーティリティ。
* コンテナ間アイテム移送: `CookingMode`(L296-362 `tryInsert*`/`tryExtract*`), `PharmcistMode`(L159-227) → 「空き/一致スロット探索→検証→移送」共通化。

## 🟦 R-5. `Modes.java` のモード登録をテーブル駆動化

現状は「`buildXxxMode()` × 6」＋「static 初期化で代入」＋「`init()` で `register()` × 6」の三重定義。
ModeType・matcher を 1 テーブルにまとめ `init()` で一括ループ登録へ。
**登録される ModeType・ItemMatcher・Priority・登録順は完全維持**（モード判定挙動不変）。約30行削減。

## 🟦 R-6. `HasMode` ⇔ `Mode` の NBT API 不整合の解消

`Mode#writeModeData/readModeData` は `CompoundTag` 直接、`HasModeImpl` は `ValueOutput/ValueInput`。
このため `HasModeImpl`(L73-75) で毎回 `CompoundTag` 生成→`store("ModeData", CompoundTag.CODEC, …)` のラッパが発生。
どちらかへ統一しラッパ除去。**NBT キー（`ModeID`/`ModeData`）と格納フォーマットは不変**（セーブ互換）。

## 🟩 R-7. モデルローダー系の安全な重複整理（§1・§2 の load-bearing を侵さず）

ASM・`maidmodel/`・描画ラッパー型・登録モデル名は一切触らない。純粋な内部重複のみ削減。
* `resource/manager/`（Model/Texture/Config）: `get()` ごとの `toLowerCase()` を**登録時 1 回**へ集約（探索キー自体は不変）。
* `resource/util/ResourceHelper`: `getFileName()`(L27-32)/`getParentFolderName()`(L56-64)/`getTexturePackName()`(L35-47) の `replace("\\","/")` 正規化を `normalizePath()` へ共通化。
* `resource/loader/LMFileLoader`: `loadArchive()`(L89-90) と `loadFile()`(L112-113) の同一ローダ適用ループを `applyLoaders(...)` へ（実行順不変）。
* `resource/manager/LMModelManager` 内部クラス `ModelHolder`（L65-86, skin/inner/outer 三つ組）のインライン化／record 化。

## 🟩 R-8. 薄い DTO／補助構造の整理（低優先・効果小）

* `resource/util/TexturePair`（2 フィールド record）, `ArmorPart.Builder`（L51-76）: 簡素化検討。
* `LittleMaidEntity` の `setLMMFlag/getLMMFlag`（L1933-1946）ビット操作の enum ラッパー化（**ビット位置・同期値は不変厳守**）。

---

## ⚠️ 本タスクの対象外（挙動が変わるため別途）

* 一部モードの状態 NBT 未永続化（Archer/Fencer の cooldown、Healer の index 等）は**リロード時挙動を変える修正**のため、
  本リスト（挙動不変リファクタ）の範囲外。必要なら `TODO.md` のバックログで扱う。
* §3 の Mixin 注入 interface（`MaidManager` / `TargetTagManager`）の削除・統合は**不可**（multiplayer 同期・`instanceof` キャストが破綻する）。
