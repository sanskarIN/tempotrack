plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            // Compose Multiplatform attaches generated resource Kotlin sources to source sets.
            // Generated code is not owned by this repository and must not fail style checks.
            exclude("**/generated/**")
        }
    }
}

tasks.register("quality") {
    group = "verification"
    description = "Runs the primary repository verification tasks."
    dependsOn(
        ":shared:allTests",
        ":desktopApp:test",
        ":androidApp:testDebugUnitTest",
        ":androidApp:lintDebug",
        ":shared:ktlintCheck",
        ":desktopApp:ktlintCheck",
        ":androidApp:ktlintCheck",
    )
}
