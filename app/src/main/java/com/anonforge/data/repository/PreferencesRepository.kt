package com.anonforge.data.repository

import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user preferences.
 */
interface PreferencesRepository {

    // ════════════════════════════════════════════════════════════════════════
    // Disclaimer
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Check if disclaimer has been accepted.
     */
    suspend fun isDisclaimerAccepted(): Boolean

    /**
     * Set disclaimer accepted state.
     */
    suspend fun setDisclaimerAccepted(accepted: Boolean)

    // ════════════════════════════════════════════════════════════════════════
    // Nationality
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Flow of current nationality preference.
     */
    val nationalityFlow: Flow<Nationality>

    /**
     * Get current nationality.
     */
    suspend fun getNationality(): Nationality

    /**
     * Set nationality preference.
     */
    suspend fun setNationality(nationality: Nationality)

    // ════════════════════════════════════════════════════════════════════════
    // Generation Preferences
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Flow of generation preferences.
     */
    val generationPreferencesFlow: Flow<GenerationPreferences>

    /**
     * Get current generation preferences.
     */
    suspend fun getGenerationPreferences(): GenerationPreferences

    /**
     * Set gender preference.
     */
    suspend fun setGenderPreference(preference: GenderPreference)

    /**
     * Set age range.
     */
    suspend fun setAgeRange(minAge: Int, maxAge: Int)

    // ════════════════════════════════════════════════════════════════════════
    // Alias Email Settings
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Flow indicating if alias email feature is enabled.
     */
    val aliasEnabledFlow: Flow<Boolean>

    /**
     * Check if alias email is enabled.
     */
    suspend fun isAliasEnabled(): Boolean

    /**
     * Enable or disable alias email feature.
     */
    suspend fun setAliasEnabled(enabled: Boolean)
}