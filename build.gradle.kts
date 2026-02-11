// ═══════════════════════════════════════════════════════════════════════════
// AnonForge - Root Build Configuration
// Kotlin 1.9.x stable - NO Compose Compiler Plugin needed
// ═══════════════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}