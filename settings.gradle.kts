pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TransportApp2"

include(":app")
include(":core:common")
include(":domain:transport")
include(":core:designsystem")
include(":core:ui")
include(":feature:auth")
include(":feature:dashboard")
include(":feature:booking")
include(":feature:consignment")
include(":feature:challan")
include(":feature:billing")
include(":feature:masters")
include(":feature:reports")
include(":feature:settings")
include(":feature:templates")