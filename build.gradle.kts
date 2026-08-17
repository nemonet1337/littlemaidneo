// ルートプロジェクトは共通設定のみを持つ。実体は apps/ 配下の 3 モジュール:
//   apps/common      — 全モジュール共通の基盤（LMNLib・汎用ユーティリティ）
//   apps/modelloader — 外部モデル/テクスチャ/ボイスパック読み込み基盤（旧 LMML 系・保護コア A/B）
//   apps/mods        — メイドさん本体の Mod 実装（エンティティ・AI・GUI・ネットワーク等）
// 依存方向は mods -> modelloader -> common の一方向のみ。
plugins {
    id("net.neoforged.moddev") version "2.0.143" apply false
    idea
}

tasks.named<Wrapper>("wrapper").configure {
    // Define wrapper values here so as to not have to always do so when updating gradlew.properties.
    distributionType = Wrapper.DistributionType.BIN
}

val mod_version: String by project
val mod_group_id: String by project

subprojects {
    // 中間ディレクトリプロジェクト (:apps) には何も適用しない
    if (childProjects.isNotEmpty()) return@subprojects

    apply(plugin = "java-library")

    version = mod_version
    group = mod_group_id

    repositories {
        // The NeoForge moddev plugin adds the NeoForge/Mojang repositories automatically,
        // but Maven Central must be declared for project-level dependencies (ASM, commons-io).
        mavenCentral()
    }

    // Mojang ships Java 25 to end users in 26.1.2, so mods should target Java 25.
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8" // Use the UTF-8 charset for Java compilation
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "500", "-Xmaxwarns", "500"))
    }

    tasks.withType<ProcessResources>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
