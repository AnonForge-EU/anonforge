package com.anonforge.data.local.prefs

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anonforge.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manages security-related preferences including biometric settings,
 * PIN configuration, and auto-lock timeout.
 *
 * Security considerations:
 * - PIN is stored encrypted using CryptoManager (AES-256-GCM)
 * - Biometric state is stored as boolean flag only
 * - No sensitive data logged
 * - DataStore injected as singleton to prevent multiple instance crash
 *
 * Public API consumed by:
 * - AuthManager: verifyPin(), hasPin(), isPinConfigured via hasPin()
 * - LockManager: autoLockMinutes for session timeout
 * - SettingsViewModel: setPin(), clearPin(), setBiometricEnabled(), setAutoLockMinutes()
 */
@Singleton
class SecurityPreferences @Inject constructor(
    @param:Named("security") private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager
) {
    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        private val KEY_LAST_ACTIVITY_TIME = stringPreferencesKey("last_activity_time")

        private const val DEFAULT_AUTO_LOCK_MINUTES = 5

        // PIN hashing parameters (PBKDF2-HMAC-SHA256).
        // 200k iterations follow the 2023 OWASP/NIST guidance and stay
        // comfortably below 200ms on modern phones.
        private const val PIN_FORMAT_PREFIX = "pbkdf2"
        private const val PIN_KDF_ITERATIONS = 200_000
        private const val PIN_SALT_BYTES = 16
        private const val PIN_HASH_BITS = 256
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Biometric Preferences
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Flow of biometric enabled state.
     */
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Enable or disable biometric authentication.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PIN Preferences
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if a PIN is configured.
     * Must be called from a coroutine context.
     */
    suspend fun hasPin(): Boolean {
        val prefs = dataStore.data.first()
        return !prefs[KEY_PIN_HASH].isNullOrEmpty()
    }

    /**
     * Set a new PIN. The PIN is stretched with PBKDF2-HMAC-SHA256 using a
     * fresh random salt and stored in the form `pbkdf2$iterations$salt$hash`.
     * Knowing the stored value alone is insufficient to recover the PIN
     * without an offline brute-force attempt against ~200k iterations.
     */
    suspend fun setPin(pin: String) {
        val encoded = encodePin(pin)
        dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = encoded
        }
    }

    /**
     * Verify if the provided PIN matches the stored PIN.
     *
     * Two storage formats are accepted for backward compatibility:
     * - **New** (`pbkdf2$...`): PBKDF2 verification with constant-time
     *   comparison.
     * - **Legacy** (raw Base64 ciphertext from CryptoManager): decrypt and
     *   compare. If the legacy PIN matches, transparently re-store it in the
     *   new format so subsequent verifications use PBKDF2.
     */
    suspend fun verifyPin(pin: String): Boolean {
        val prefs = dataStore.data.first()
        val stored = prefs[KEY_PIN_HASH] ?: return false

        if (stored.startsWith("$PIN_FORMAT_PREFIX$")) {
            return verifyPbkdf2(pin, stored)
        }

        // Legacy AES path. If it matches, opportunistically migrate.
        val legacyOk = try {
            val decrypted = cryptoManager.decryptString(stored)
            constantTimeEquals(pin, decrypted)
        } catch (_: Exception) {
            false
        }
        if (legacyOk) {
            val migrated = encodePin(pin)
            dataStore.edit { it[KEY_PIN_HASH] = migrated }
        }
        return legacyOk
    }

    private fun encodePin(pin: String): String {
        val salt = ByteArray(PIN_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin.toCharArray(), salt, PIN_KDF_ITERATIONS, PIN_HASH_BITS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        // Wipe the derived bytes; the stored Base64 is non-sensitive once
        // the underlying byte array is gone.
        hash.fill(0)
        return "$PIN_FORMAT_PREFIX\$$PIN_KDF_ITERATIONS\$$saltB64\$$hashB64"
    }

    private fun verifyPbkdf2(pin: String, encoded: String): Boolean {
        val parts = encoded.split('$')
        // expected: ["pbkdf2", "<iters>", "<saltB64>", "<hashB64>"]
        if (parts.size != 4) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = try {
            Base64.decode(parts[2], Base64.NO_WRAP)
        } catch (_: Exception) {
            return false
        }
        val expected = try {
            Base64.decode(parts[3], Base64.NO_WRAP)
        } catch (_: Exception) {
            return false
        }
        val candidate = pbkdf2(pin.toCharArray(), salt, iterations, expected.size * 8)
        val matches = MessageDigest.isEqual(expected, candidate)
        candidate.fill(0)
        return matches
    }

    private fun pbkdf2(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int,
        outputBits: Int
    ): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, outputBits)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(
            a.toByteArray(Charsets.UTF_8),
            b.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Clear the stored PIN.
     */
    suspend fun clearPin() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Auto-lock Preferences
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Flow of auto-lock timeout in minutes.
     * 0 = never auto-lock during session
     *
     * Consumed by LockManager.getSessionTimeoutMs() for session expiry.
     */
    val autoLockMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOCK_MINUTES] ?: DEFAULT_AUTO_LOCK_MINUTES
    }

    /**
     * Set the auto-lock timeout.
     *
     * @param minutes Timeout in minutes (0 = never)
     */
    suspend fun setAutoLockMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_MINUTES] = minutes
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Activity Tracking (for auto-lock)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Update the last activity timestamp.
     * Called by LockManager to track user activity for auto-lock.
     */
    @Suppress("unused") // Public API - available for LockManager if needed
    suspend fun updateLastActivityTime() {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_ACTIVITY_TIME] = System.currentTimeMillis().toString()
        }
    }

    /**
     * Get the last activity timestamp.
     * Used by shouldAutoLock() to determine if lock timeout exceeded.
     */
    private suspend fun getLastActivityTime(): Long {
        val prefs = dataStore.data.first()
        return prefs[KEY_LAST_ACTIVITY_TIME]?.toLongOrNull() ?: System.currentTimeMillis()
    }

    /**
     * Check if the app should be locked based on inactivity.
     * Called by LockManager/MainActivity to determine if unlock screen should be shown.
     *
     * @return true if auto-lock timeout has been exceeded
     */
    @Suppress("unused") // Public API - available for alternative lock check
    suspend fun shouldAutoLock(): Boolean {
        val timeout = autoLockMinutes.first()
        if (timeout == 0) return false // Never auto-lock

        val lastActivity = getLastActivityTime()
        val elapsed = System.currentTimeMillis() - lastActivity
        val timeoutMs = timeout * 60 * 1000L

        return elapsed > timeoutMs
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Clear All Security Data
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Clear all security preferences (used during app reset or wipe).
     * Called by SettingsViewModel for data wipe functionality.
     */
    @Suppress("unused") // Public API - called by SettingsViewModel for data wipe
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}