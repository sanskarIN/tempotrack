plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

tasks.register("quality") {
    group = "verification"
    description = "Runs the primary repository verification tasks."
    dependsOn(
        ":shared:allTests",
        ":desktopApp:test",
        ":androidApp:testDebugUnitTest",
        ":androidApp:lintDebug",
    )
}
