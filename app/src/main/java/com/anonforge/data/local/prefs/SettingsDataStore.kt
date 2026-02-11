package com.anonforge.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anonforge.domain.model.AppLanguage
import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * DataStore for app settings including security, generation, and appearance preferences.
 * Provides reactive Flows and suspend functions for persistence.
 *
 * IMPORTANT: DataStore is injected as singleton to prevent
 * "multiple DataStores active for the same file" crash.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @param:Named("settings") private val dataStore: DataStore<Preferences>
) {
    // ═══════════════════════════════════════════════════════════════════════════
    // SECURITY SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════

    val biometricEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] ?: false
        }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] ?: false
        }

    val autoLockTimeout: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_AUTOLOCK_TIMEOUT] ?: 5
        }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTOLOCK_TIMEOUT] = minutes
        }
    }

    suspend fun getBiometricEnabled(): Boolean {
        return biometricEnabled.first()
    }

    suspend fun getNotificationsEnabled(): Boolean {
        return notificationsEnabled.first()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GENERATION PREFERENCES
    // ═══════════════════════════════════════════════════════════════════════════

    val nationality: Flow<Nationality> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val code = prefs[KEY_NATIONALITY] ?: Nationality.DEFAULT.code
            Nationality.fromCode(code)
        }

    val ageRangeMin: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_AGE_RANGE_MIN] ?: GenerationPreferences.MIN_AGE
        }

    val ageRangeMax: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_AGE_RANGE_MAX] ?: GenerationPreferences.MAX_AGE
        }

    /**
     * Flow for gender preference.
     * Emits RANDOM, MALE, or FEMALE.
     */
    val genderPreference: Flow<GenderPreference> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val name = prefs[KEY_GENDER_PREFERENCE] ?: GenderPreference.RANDOM.name
            try {
                GenderPreference.valueOf(name)
            } catch (_: IllegalArgumentException) {
                GenderPreference.RANDOM
            }
        }

    suspend fun setNationality(nationality: Nationality) {
        dataStore.edit { prefs ->
            prefs[KEY_NATIONALITY] = nationality.code
        }
    }

    suspend fun setAgeRange(min: Int, max: Int) {
        val validMin = min.coerceIn(GenerationPreferences.MIN_AGE, GenerationPreferences.MAX_AGE)
        val validMax = max.coerceIn(validMin, GenerationPreferences.MAX_AGE)
        dataStore.edit { prefs ->
            prefs[KEY_AGE_RANGE_MIN] = validMin
            prefs[KEY_AGE_RANGE_MAX] = validMax
        }
    }

    /**
     * Set gender preference for identity generation.
     */
    suspend fun setGenderPreference(preference: GenderPreference) {
        dataStore.edit { prefs ->
            prefs[KEY_GENDER_PREFERENCE] = preference.name
        }
    }

    /**
     * Get current gender preference synchronously.
     */
    suspend fun getGenderPreference(): GenderPreference {
        return genderPreference.first()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLELOGIN EMAIL ALIAS SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════

    val aliasEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_ALIAS_ENABLED] ?: false
        }

    suspend fun setAliasEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ALIAS_ENABLED] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PHONE ALIAS SETTINGS (Skill 16 - Manual Mode)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Flow for phone alias enabled state.
     * Used by PhoneAliasSettingsViewModel to observe preference changes.
     */
    val phoneAliasEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_PHONE_ALIAS_ENABLED] ?: false
        }

    /**
     * Set phone alias enabled state.
     * Called by PhoneAliasSettingsViewModel when toggling the feature.
     */
    suspend fun setPhoneAliasEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PHONE_ALIAS_ENABLED] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUPPORTER STATUS (Skill 17)
    // ═══════════════════════════════════════════════════════════════════════════

    val isSupporter: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_IS_SUPPORTER] ?: false
        }

    @Suppress("unused") // Public API for supporter status check
    suspend fun getIsSupporter(): Boolean {
        return isSupporter.first()
    }

    /**
     * Set supporter status after successful donation.
     */
    @Suppress("unused") // Public API for donation feature - will be used when payment flow is implemented
    suspend fun setSupporter(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_SUPPORTER] = value
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPEARANCE SETTINGS (Skill 18)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Flow for theme mode preference.
     * Emits SYSTEM, LIGHT, or DARK.
     */
    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val code = prefs[KEY_THEME_MODE] ?: ThemeMode.DEFAULT.code
            ThemeMode.fromCode(code)
        }

    /**
     * Flow for app language preference.
     * Emits SYSTEM, EN, or FR.
     */
    val appLanguage: Flow<AppLanguage> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val code = prefs[KEY_APP_LANGUAGE] ?: AppLanguage.DEFAULT.code
            AppLanguage.fromCode(code)
        }

    /**
     * Set theme mode preference.
     * Change takes effect immediately (no restart needed).
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.code
        }
    }

    /**
     * Set app language preference.
     * Requires activity recreation to take effect.
     */
    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.code
        }
    }

    /**
     * Get current theme mode synchronously.
     */
    suspend fun getThemeMode(): ThemeMode {
        return themeMode.first()
    }

    /**
     * Get current app language synchronously.
     */
    suspend fun getAppLanguage(): AppLanguage {
        return appLanguage.first()
    }

    companion object {
        // Security keys
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_AUTOLOCK_TIMEOUT = intPreferencesKey("autolock_timeout")

        // Generation keys
        private val KEY_NATIONALITY = stringPreferencesKey("nationality")
        private val KEY_AGE_RANGE_MIN = intPreferencesKey("age_range_min")
        private val KEY_AGE_RANGE_MAX = intPreferencesKey("age_range_max")
        private val KEY_GENDER_PREFERENCE = stringPreferencesKey("gender_preference")

        // Alias keys
        private val KEY_ALIAS_ENABLED = booleanPreferencesKey("alias_enabled")
        private val KEY_PHONE_ALIAS_ENABLED = booleanPreferencesKey("phone_alias_enabled")

        // Supporter key (Skill 17)
        private val KEY_IS_SUPPORTER = booleanPreferencesKey("is_supporter")

        // Appearance keys (Skill 18)
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }
}