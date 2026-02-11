// ═══════════════════════════════════════════════════════════════════════════
// AnonForge - App Build Configuration
// Kotlin 2.0+ with integrated Compose Compiler (no composeOptions needed)
// ═══════════════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)  // Required for Kotlin 2.0+ Compose
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.anonforge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anonforge"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
    }

    // NOTE: composeOptions block removed - Kotlin 2.0+ includes Compose Compiler
    // via the kotlin.compose plugin (org.jetbrains.kotlin.plugin.compose)

    packaging {
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

dependencies {
    // ═══════════════════════════════════════════════════════════════════════
    // Core Android
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ═══════════════════════════════════════════════════════════════════════
    // Material Components (View-based) - Required for XML themes
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.material)

    // ═══════════════════════════════════════════════════════════════════════
    // Jetpack Compose
    // ═══════════════════════════════════════════════════════════════════════
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    // ═══════════════════════════════════════════════════════════════════════
    // Hilt Dependency Injection
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // ═══════════════════════════════════════════════════════════════════════
    // Room Database + SQLCipher Encryption
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)

    // ═══════════════════════════════════════════════════════════════════════
    // Network - Retrofit + OkHttp (SimpleLogin API)
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)

    // ═══════════════════════════════════════════════════════════════════════
    // Security
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // ═══════════════════════════════════════════════════════════════════════
    // DataStore & WorkManager
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)

    // ═══════════════════════════════════════════════════════════════════════
    // Kotlin Extensions
    // ═══════════════════════════════════════════════════════════════════════
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // ═══════════════════════════════════════════════════════════════════════
    // Testing
    // ═══════════════════════════════════════════════════════════════════════
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)

    // ═══════════════════════════════════════════════════════════════════════
    // Kotlin Test (now using version catalog)
    // ═══════════════════════════════════════════════════════════════════════
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
}