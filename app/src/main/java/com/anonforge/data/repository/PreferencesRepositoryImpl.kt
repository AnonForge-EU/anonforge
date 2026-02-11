package com.anonforge.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using DataStore.
 *
 * IMPORTANT: Uses injected DataStore singleton to prevent
 * "multiple DataStores active for the same file" crash.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @param:Named("user_preferences") private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object Keys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val NATIONALITY = stringPreferencesKey("nationality")
        val GENDER_PREFERENCE = stringPreferencesKey("gender_preference")
        val AGE_RANGE_MIN = intPreferencesKey("age_range_min")
        val AGE_RANGE_MAX = intPreferencesKey("age_range_max")
        val ALIAS_ENABLED = booleanPreferencesKey("alias_enabled")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Disclaimer
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun isDisclaimerAccepted(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
        }.first()
    }

    override suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Nationality
    // ═══════════════════════════════════════════════════════════════════════════

    override val nationalityFlow: Flow<Nationality> = dataStore.data.map { prefs ->
        val code = prefs[Keys.NATIONALITY] ?: Nationality.FR.code
        Nationality.fromCode(code)
    }

    override suspend fun getNationality(): Nationality {
        return nationalityFlow.first()
    }

    override suspend fun setNationality(nationality: Nationality) {
        dataStore.edit { prefs ->
            prefs[Keys.NATIONALITY] = nationality.code
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Generation Preferences
    // ═══════════════════════════════════════════════════════════════════════════

    override val generationPreferencesFlow: Flow<GenerationPreferences> =
        dataStore.data.map { prefs ->
            GenerationPreferences(
                genderPreference = prefs[Keys.GENDER_PREFERENCE]?.let {
                    try {
                        GenderPreference.valueOf(it)
                    } catch (_: IllegalArgumentException) {
                        GenderPreference.RANDOM
                    }
                } ?: GenderPreference.RANDOM,
                ageRangeMin = prefs[Keys.AGE_RANGE_MIN] ?: GenerationPreferences.MIN_AGE,
                ageRangeMax = prefs[Keys.AGE_RANGE_MAX] ?: GenerationPreferences.MAX_AGE
            )
        }

    override suspend fun getGenerationPreferences(): GenerationPreferences {
        return generationPreferencesFlow.first()
    }

    override suspend fun setGenderPreference(preference: GenderPreference) {
        dataStore.edit { prefs ->
            prefs[Keys.GENDER_PREFERENCE] = preference.name
        }
    }

    override suspend fun setAgeRange(minAge: Int, maxAge: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.AGE_RANGE_MIN] = minAge.coerceAtLeast(GenerationPreferences.MIN_AGE)
            prefs[Keys.AGE_RANGE_MAX] = maxAge.coerceAtMost(GenerationPreferences.MAX_AGE)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Alias Email Settings
    // ═══════════════════════════════════════════════════════════════════════════

    override val aliasEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ALIAS_ENABLED] ?: false
    }

    override suspend fun isAliasEnabled(): Boolean {
        return aliasEnabledFlow.first()
    }

    override suspend fun setAliasEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ALIAS_ENABLED] = enabled
        }
    }
}