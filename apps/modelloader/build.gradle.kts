// apps/modelloader — 外部テクスチャ/ボイスパック読み込みと内蔵 ModelPart モデル。
// ボイス（.cfg/.ogg）と PNG テクスチャの命名は保護コア B。外部 .class モデルパック互換は廃止済み。
plugins {
    `java-library`
    id("net.neoforged.moddev")
}

val neo_version: String by project

neoForge {
    this@neoForge.version = neo_version
}

dependencies {
    api(project(":apps:common"))
    // ASM / commons-io は NeoForge が compile classpath に strictly 固定で供給する。
    // 独自にバージョンを書くと Dependabot がより新しい版へ上げ、解決不能になる。
}
