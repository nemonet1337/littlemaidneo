// apps/common — 全モジュール共通の基盤（LMNLib・汎用ユーティリティ）
plugins {
    `java-library`
    id("net.neoforged.moddev")
}

val neo_version: String by project

neoForge {
    this@neoForge.version = neo_version
}
