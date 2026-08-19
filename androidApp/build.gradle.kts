plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val tempoTrackVersion = providers.gradleProperty("appVersion").get()
val tempoTrackVersionCode = providers.gradleProperty("appVersionCode").get().toInt()
val releaseKeystorePath = providers.environmentVariable("TEMPOTRACK_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("TEMPOTRACK_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("TEMPOTRACK_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("TEMPOTRACK_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
val releaseSigningPartiallyConfigured = releaseSigningValues.any { !it.isNullOrBlank() } && !releaseSigningConfigured

require(!releaseSigningPartiallyConfigured) {
    "Android release signing is partially configured. Set all TEMPOTRACK_KEYSTORE_PATH, " +
        "TEMPOTRACK_KEYSTORE_PASSWORD, TEMPOTRACK_KEY_ALIAS, and TEMPOTRACK_KEY_PASSWORD values or none of them."
}

android {
    namespace = "in.sanskar.tempotrack"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "in.sanskar.tempotrack"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = tempoTrackVersionCode
        versionName = tempoTrackVersion
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                val keystore = file(requireNotNull(releaseKeystorePath))
                require(keystore.isFile) { "Configured Android release keystore does not exist or is not a file." }
                storeFile = keystore
                storePassword = requireNotNull(releaseKeystorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
}
