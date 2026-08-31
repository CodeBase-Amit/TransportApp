// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

// Phase 2 S1 — Spec.md §2 module rules, enforced (TransportApp.md §5):
// pure modules must compile without android.*, features must not import Room.
// Feature -> core.ui.sample imports are allowed only until that screen migrates (Phase2.md D10).
tasks.register("checkPureModules") {
    group = "verification"
    description = "Fails the build if pure-Kotlin modules import android.* or features import androidx.room."
    val rootDirPath = projectDir.absolutePath // captured at configuration time for config-cache safety
    doLast {
        val offenders = mutableListOf<String>()

        val pureModules = listOf("core/common", "domain/transport", "export-engine")
        pureModules.forEach { mod ->
            java.io.File(rootDirPath, "$mod/src").takeIf { it.exists() }?.walkTopDown()
                ?.filter { it.isFile && it.extension == "kt" }?.forEach { f ->
                    f.readLines().forEachIndexed { i, line ->
                        val t = line.trimStart()
                        if (t.startsWith("import android.")) offenders += "$mod -> ${f.name}:${i + 1}: $t"
                    }
                }
        }

        java.io.File(rootDirPath, "feature/src").takeIf { it.exists() }?.walkTopDown()
            ?.filter { it.isFile && it.extension == "kt" }?.forEach { f ->
                f.readLines().forEachIndexed { i, line ->
                    val t = line.trimStart()
                    if (t.startsWith("import androidx.room")) offenders += "feature -> ${f.name}:${i + 1}: $t"
                }
            }

        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Module dependency rules violated (Spec.md §2):\n" + offenders.joinToString("\n")
            )
        }
    }
}
