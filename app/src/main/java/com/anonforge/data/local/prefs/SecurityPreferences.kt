package com.anonforge.data.local.prefs

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
     * Set a new PIN (stored as AES-256-GCM encrypted value).
     *
     * @param pin The plain text PIN to store
     */
    suspend fun setPin(pin: String) {
        val encryptedPin = cryptoManager.encryptString(pin)
        dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = encryptedPin
        }
    }

    /**
     * Verify if the provided PIN matches the stored PIN.
     * Called by AuthManager.verifyPin() for unlock flow.
     *
     * Decrypts the stored PIN and compares with input.
     * This is the ONLY method that should be used for PIN verification
     * to ensure consistency with setPin() encryption.
     *
     * @param pin The PIN to verify
     * @return true if PIN matches, false otherwise
     */
    suspend fun verifyPin(pin: String): Boolean {
        val prefs = dataStore.data.first()
        val storedEncrypted = prefs[KEY_PIN_HASH] ?: return false

        return try {
            val decryptedPin = cryptoManager.decryptString(storedEncrypted)
            pin == decryptedPin
        } catch (_: Exception) {
            // Decryption failed - PIN doesn't match or data corrupted
            false
        }
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