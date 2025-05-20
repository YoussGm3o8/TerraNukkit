rootProject.name = "Terra"


fun includeImmediateChildren(dir: File, type: String, excludeDirs: Set<String> = emptySet()) {
    dir.walkTopDown().maxDepth(1).forEach {
        if (!it.isDirectory || !File(it, "build.gradle.kts").exists() || it.name in excludeDirs) return@forEach
        val addonDir = it.relativeTo(file(".")).path.replace("/", ":").replace("\\", ":")
        logger.info("Including $type directory \"$addonDir\" as subproject.")
        include(addonDir)
    }
}

includeImmediateChildren(file("common/api"), "API")

includeImmediateChildren(file("common/implementation"), "implementation")

includeImmediateChildren(file("common/addons"), "addon")

includeImmediateChildren(file("platforms"), "platform", setOf("bukkit"))

// includeImmediateChildren(file("platforms/bukkit/nms"), "Bukkit NMS") // Commented out to avoid build failure

// include(":platforms:bukkit:common") // Commented out to avoid build failure
include(":platforms:minestom:example")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") {
            name = "Fabric Maven"
        }
        maven("https://maven.architectury.dev/") {
            name = "Architectury Maven"
        }
        maven("https://files.minecraftforge.net/maven/") {
            name = "Forge Maven"
        }
        maven("https://maven.quiltmc.org/repository/release/") {
            name = "Quilt"
        }
    }
}

// settings.gradle.kts
val isCiServer = System.getenv().containsKey("CI")
// Cache build artifacts, so expensive operations do not need to be re-computed
buildCache {
    local {
        isEnabled = !isCiServer
    }
}