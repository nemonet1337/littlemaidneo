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
    // ASM / commons-io は NeoForge が compile classpath に strictly 固定で供給する。
    // 独自にバージョンを書くと Dependabot がより新しい版へ上げ、解決不能になる。
}
