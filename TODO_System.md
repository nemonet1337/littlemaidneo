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
