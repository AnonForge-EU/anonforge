# ════════════════════════════════════════════════════════════════════════════
# ANONFORGE PROGUARD RULES
# Optimized for R8 with minimal keep rules
# ════════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════════
# HILT / DAGGER - Minimal rules (Hilt includes its own consumer rules)
# ═══════════════════════════════════════
# Only keep what's strictly necessary - Hilt handles most of this automatically
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ═══════════════════════════════════════
# WORKMANAGER + HILT WORKER
# Required to fix NoSuchMethodException in Release builds
# ═══════════════════════════════════════
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep,allowobfuscation @androidx.hilt.work.HiltWorker class *

# ═══════════════════════════════════════
# ROOM DATABASE
# Keep entities, DAOs, and type converters
# ═══════════════════════════════════════
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * { @androidx.room.TypeConverter *; }

# ═══════════════════════════════════════
# SQLCIPHER - Encrypted database
# ═══════════════════════════════════════
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ═══════════════════════════════════════
# DOMAIN MODELS & KOTLINX SERIALIZATION
# Keep serializable models for JSON export/import
# ═══════════════════════════════════════
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Kotlinx Serialization
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.anonforge.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain models used in serialization
-keep class com.anonforge.domain.model.** { *; }
-keep class com.anonforge.data.local.entity.** { *; }
-keep class com.anonforge.data.remote.**.dto.** { *; }

# ═══════════════════════════════════════
# BIOMETRIC AUTHENTICATION
# ═══════════════════════════════════════
-keep class androidx.biometric.BiometricPrompt$* { *; }

# ═══════════════════════════════════════
# SECURITY - Remove logs in release builds
# Strips debug/verbose logs for security
# ═══════════════════════════════════════
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ═══════════════════════════════════════
# OPTIMIZATION SETTINGS
# ═══════════════════════════════════════
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ═══════════════════════════════════════
# RETROFIT / OKHTTP
# Retrofit includes its own consumer rules - only keep API interfaces
# ═══════════════════════════════════════
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Keep only YOUR API interface methods (not all of Retrofit)
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep your specific API interfaces
-keep,allowobfuscation interface com.anonforge.data.remote.**Api

# Tink crypto - missing optional dependencies
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.http.javanet.**
-dontwarn org.joda.time.**