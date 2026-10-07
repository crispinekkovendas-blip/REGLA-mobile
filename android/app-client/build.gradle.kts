plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.com.imoveisregla.client"
    compileSdk = 37
    defaultConfig {
        applicationId = "br.com.imoveisregla.client"
        minSdk = 26
        targetSdk = 36
        // CI passes the GitHub run number so every published build installs over the last one.
        // Builds without Supabase config run on sample data: give them their own id + name
        // ("… Demo") so they can never be mistaken for, or installed over, the real app.
        val live = !System.getenv("SUPABASE_URL").isNullOrBlank() ||
            rootProject.file("local.properties").takeIf { it.exists() }?.readLines()
                ?.any { it.startsWith("SUPABASE_URL=") && it.substringAfter("=").isNotBlank() } == true
        if (!live) applicationIdSuffix = ".demo"
        manifestPlaceholders["appLabel"] = if (live) "REGLA" else "REGLA Demo"
        val build = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "0.1.$build"
    }
    // Stable REGLA signing key from CI secrets (keystore kept outside the repo). Without it every
    // runner signs with a throwaway debug key and Android refuses to update an installed app.
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("regla") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = "regla"
                keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        val signing = signingConfigs.findByName("regla") ?: signingConfigs.getByName("debug")
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = false
            signingConfig = signing
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
}
