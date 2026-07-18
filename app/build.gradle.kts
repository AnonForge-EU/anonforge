// ═══════════════════════════════════════════════════════════════════════════
// AnonForge - App Build Configuration
// Kotlin 2.0+ with integrated Compose Compiler (no composeOptions needed)
// ═══════════════════════════════════════════════════════════════════════════

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // NOTE: org.jetbrains.kotlin.android is intentionally NOT applied — AGP 9
    // ships built-in Kotlin support and rejects the standalone plugin. The
    // Compose/serialization plugins below still drive the KGP version on the
    // classpath (2.4.0 via the version catalog).
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Signing config is loaded from a gitignored `keystore.properties` at the
// repo root. When the file is absent (e.g. on a contributor's machine) we
// fall back to skipping signing so debug builds still work.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use(::load)
}

android {
    namespace = "com.anonforge"
    // compileSdk 36 is required by the latest AndroidX libs (navigation 2.9.8,
    // lifecycle 2.10, activity 1.13). targetSdk stays at 35 (Android 15) so we
    // don't opt into Android 16 runtime behavior changes yet — these two knobs
    // are intentionally decoupled.
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.anonforge"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply the release signing config only when keystore.properties is
            // present; otherwise let assembleRelease produce an unsigned APK
            // that the user can sign manually.
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Kotlin compiler options (modern DSL — `kotlinOptions` was removed in the
// Kotlin Gradle plugin used by Kotlin 2.4 / AGP 9).
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
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
    implementation(libs.androidx.lifecycle.runtime.compose)
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