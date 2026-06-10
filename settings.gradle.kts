plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "littlemaidneo"

include(":apps:common")
include(":apps:modelloader")
include(":apps:mods")
