plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// Golden-file workflow: `gradlew :doc-engine:test -Pgolden.update=true` regenerates the
// committed golden HTML; the normal run compares against it byte-for-byte.
tasks.withType<Test>().configureEach {
    systemProperty("golden.update", providers.gradleProperty("golden.update").getOrElse(""))
}
