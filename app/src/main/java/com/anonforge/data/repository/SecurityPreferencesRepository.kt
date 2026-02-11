package com.anonforge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository for security-related preferences using DataStore.
 *
 * Manages:
 * - Biometric authentication settings
 * - PIN hash storage (hashed, never plaintext)
 * - First unlock tracking
 *
 * Security notes:
 * - PIN is stored as SHA-256 hash only
 * - No sensitive data logged
 * - Backed by EncryptedDataStore via DI module
 */
@Singleton
class SecurityPreferencesRepository @Inject constructor(
    @param:Named("security") private val dataStore: DataStore<Preferences>,
    @Suppress("unused") // Reserved for future context-dependent operations
    @param:ApplicationContext private val context: Context
) {
    companion object {
        // Preference keys
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_FIRST_UNLOCK_DONE = booleanPreferencesKey("first_unlock_done")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    }

    // ==================== Biometric Settings ====================

    /**
     * Flow of biometric enabled state.
     */
    val biometricEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Check if biometric is enabled (blocking).
     */
    @Suppress("unused") // Public API for settings UI
    suspend fun isBiometricEnabled(): Boolean {
        return dataStore.data.first()[KEY_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Set biometric enabled state.
     */
    @Suppress("unused") // Public API for settings UI
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    // ==================== First Unlock Tracking ====================

    /**
     * Flow of first unlock done state.
     */
    @Suppress("unused") // Public API for reactive settings observation
    val firstUnlockDoneFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_FIRST_UNLOCK_DONE] ?: false
    }

    /**
     * Check if first unlock is done (blocking).
     */
    @Suppress("unused") // Public API for onboarding flow
    suspend fun isFirstUnlockDone(): Boolean {
        return dataStore.data.first()[KEY_FIRST_UNLOCK_DONE] ?: false
    }

    /**
     * Set first unlock done.
     */
    @Suppress("unused") // Public API for onboarding flow
    suspend fun setFirstUnlockDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_UNLOCK_DONE] = done
        }
    }

    // ==================== PIN Settings ====================

    /**
     * Flow of PIN enabled state.
     */
    @Suppress("unused") // Public API for reactive PIN status observation
    val pinEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PIN_ENABLED] ?: false
    }

    /**
     * Check if PIN is enabled.
     */
    @Suppress("unused") // Public API for auth checks
    suspend fun isPinEnabled(): Boolean {
        return dataStore.data.first()[KEY_PIN_ENABLED] ?: false
    }

    /**
     * Get stored PIN hash.
     * Returns null if no PIN is set.
     */
    suspend fun getPinHash(): String? {
        return dataStore.data.first()[KEY_PIN_HASH]
    }

    /**
     * Store PIN hash.
     * @param hash SHA-256 hash of the PIN
     */
    suspend fun setPinHash(hash: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_PIN_ENABLED] = true
        }
    }

    /**
     * Clear PIN hash and disable PIN authentication.
     */
    suspend fun clearPinHash() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_PIN_ENABLED] = false
        }
    }

    // ==================== Utility ====================

    /**
     * Clear all security preferences.
     * Use with caution - this resets all security settings.
     */
    @Suppress("unused") // Public API for app reset functionality
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_BIOMETRIC_ENABLED)
            prefs.remove(KEY_FIRST_UNLOCK_DONE)
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_ENABLED)
        }
    }
}
