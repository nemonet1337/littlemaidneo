// apps/modelloader — 外部モデル/テクスチャ/ボイスパック読み込み基盤（旧 LMML 系）
// resource/classloader（ASM リマップ）と maidmodel/ は保護コア A。削除・破壊的変更厳禁。
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

    // 外部 .class モデルパックの ASM リマップ（MultiModelClassTransformer）用
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation("commons-io:commons-io:2.22.0")
}
