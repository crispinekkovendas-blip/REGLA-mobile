plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase credentials: env vars (CI) or android/local.properties (dev).
// Empty values => the apps run in demo mode on the in-memory fake repositories.
fun secret(name: String): String {
    System.getenv(name)?.takeIf { it.isNotBlank() }?.let { return it }
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    return f.readLines()
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter("=")
        ?.trim()
        .orEmpty()
}

android {
    namespace = "br.com.imoveisregla.core.data"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        // OAuth *Web* client id from Google Cloud (public value; empty = Google button explains it's not set up yet).
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID")}\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
