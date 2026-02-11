package com.anonforge.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.prefs.SecurityPreferences
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.AppLanguage
import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for Settings screen.
 */
data class SettingsState(
    // Security
    val biometricAvailable: Boolean = false,
    val biometricEnrolled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinConfigured: Boolean = false,
    val autoLockMinutes: Int = 5,

    // Generation preferences
    val selectedNationality: Nationality = Nationality.DEFAULT,
    val selectedGenderPreference: GenderPreference = GenderPreference.RANDOM,
    val ageRangeMin: Int = 18,
    val ageRangeMax: Int = 80,

    // Alias configuration status
    val aliasConfigured: Boolean = false,
    val phoneAliasConfigured: Boolean = false,

    // Appearance (Skill 18)
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.DEFAULT,
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val languageChangeRequested: Boolean = false,

    // Dialog states
    val showBiometricEnrollmentDialog: Boolean = false,
    val showBiometricVerificationPending: Boolean = false,
    val showPinSetupDialog: Boolean = false,
    val showAutoLockDialog: Boolean = false,
    val showNationalityDialog: Boolean = false,
    val showGenderDialog: Boolean = false,
    val showAgeRangeDialog: Boolean = false,

    // Settings launch
    val launchFingerprintSettings: Boolean = false,

    // Messages
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val securityPreferences: SecurityPreferences,
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val biometricManager = BiometricManager.from(context)

    // Flag to track pending biometric enable request
    private var pendingBiometricEnable = false

    init {
        loadSettings()
    }

    /**
     * Load all settings from preferences and data stores.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Check biometric availability
            val biometricStatus = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )

            val biometricAvailable = biometricStatus != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                    biometricStatus != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE

            val biometricEnrolled = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

            // Load preferences
            val biometricEnabled = securityPreferences.biometricEnabled.first()
            val pinConfigured = securityPreferences.hasPin()
            val autoLockMinutes = securityPreferences.autoLockMinutes.first()
            val nationality = settingsDataStore.nationality.first()
            val genderPreference = settingsDataStore.getGenderPreference()
            val ageMin = settingsDataStore.ageRangeMin.first()
            val ageMax = settingsDataStore.ageRangeMax.first()

            // Check alias configurations
            val aliasConfigured = apiKeyManager.hasApiKey()

            // Appearance (Skill 18)
            val themeMode = settingsDataStore.getThemeMode()
            val appLanguage = settingsDataStore.getAppLanguage()

            _state.update {
                it.copy(
                    biometricAvailable = biometricAvailable,
                    biometricEnrolled = biometricEnrolled,
                    biometricEnabled = biometricEnabled && biometricEnrolled,
                    pinConfigured = pinConfigured,
                    autoLockMinutes = autoLockMinutes,
                    selectedNationality = nationality,
                    selectedGenderPreference = genderPreference,
                    ageRangeMin = ageMin,
                    ageRangeMax = ageMax,
                    aliasConfigured = aliasConfigured,
                    themeMode = themeMode,
                    appLanguage = appLanguage
                )
            }
        }
    }

    /**
     * Refresh biometric status (called after returning from system settings).
     */
    fun refreshBiometricStatus() {
        val biometricStatus = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        val biometricEnrolled = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

        _state.update {
            it.copy(
                biometricEnrolled = biometricEnrolled,
                // Disable biometric if no longer enrolled
                biometricEnabled = it.biometricEnabled && biometricEnrolled
            )
        }

        // If user just enrolled and we were waiting, show verification
        if (biometricEnrolled && pendingBiometricEnable) {
            _state.update { it.copy(showBiometricVerificationPending = true) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Biometric Management
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Toggle biometric authentication.
     * If enabling: requires biometric verification first.
     * If disabling: immediately disables.
     */
    fun toggleBiometric(enable: Boolean) {
        if (enable) {
            // Check if biometric is enrolled
            if (!_state.value.biometricEnrolled) {
                // Show enrollment dialog - user needs to enroll fingerprint first
                pendingBiometricEnable = true
                _state.update { it.copy(showBiometricEnrollmentDialog = true) }
            } else {
                // Biometric enrolled - request verification before enabling
                pendingBiometricEnable = true
                _state.update { it.copy(showBiometricVerificationPending = true) }
            }
        } else {
            // Disable immediately
            viewModelScope.launch {
                securityPreferences.setBiometricEnabled(false)
                _state.update {
                    it.copy(
                        biometricEnabled = false,
                        snackbarMessage = "Biometric authentication disabled"
                    )
                }
            }
        }
    }

    /**
     * Called by the UI when BiometricPrompt succeeds.
     * Completes the biometric enable flow.
     */
    fun onBiometricVerificationSuccess() {
        if (pendingBiometricEnable) {
            viewModelScope.launch {
                securityPreferences.setBiometricEnabled(true)
                _state.update {
                    it.copy(
                        biometricEnabled = true,
                        showBiometricVerificationPending = false,
                        snackbarMessage = "Biometric authentication enabled"
                    )
                }
                pendingBiometricEnable = false
            }
        }
    }

    /**
     * Called by the UI when BiometricPrompt fails.
     */
    fun onBiometricVerificationFailed() {
        pendingBiometricEnable = false
        _state.update {
            it.copy(
                showBiometricVerificationPending = false,
                snackbarMessage = "Biometric verification failed"
            )
        }
    }

    /**
     * Called by the UI when BiometricPrompt is cancelled by user.
     */
    fun onBiometricVerificationCancelled() {
        pendingBiometricEnable = false
        _state.update {
            it.copy(showBiometricVerificationPending = false)
        }
    }

    /**
     * Navigate to fingerprint settings in system settings.
     */
    fun onGoToFingerprintSettings() {
        _state.update {
            it.copy(
                showBiometricEnrollmentDialog = false,
                launchFingerprintSettings = true
            )
        }
    }

    /**
     * Clear the fingerprint settings launch flag.
     */
    fun clearFingerprintSettingsLaunch() {
        _state.update { it.copy(launchFingerprintSettings = false) }
    }

    /**
     * Dismiss biometric enrollment dialog.
     */
    fun dismissBiometricEnrollmentDialog() {
        pendingBiometricEnable = false
        _state.update { it.copy(showBiometricEnrollmentDialog = false) }
    }

    /**
     * Get intent to open fingerprint settings.
     */
    fun getFingerprintSettingsIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PIN Management
    // ═══════════════════════════════════════════════════════════════════════════

    fun showPinSetup() {
        _state.update { it.copy(showPinSetupDialog = true) }
    }

    fun dismissPinSetup() {
        _state.update { it.copy(showPinSetupDialog = false) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            securityPreferences.setPin(pin)
            _state.update {
                it.copy(
                    pinConfigured = true,
                    showPinSetupDialog = false,
                    snackbarMessage = "PIN configured successfully"
                )
            }
        }
    }

    fun removePin() {
        viewModelScope.launch {
            securityPreferences.clearPin()
            _state.update {
                it.copy(
                    pinConfigured = false,
                    snackbarMessage = "PIN removed"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Auto-lock Management
    // ═══════════════════════════════════════════════════════════════════════════

    fun showAutoLockDialog() {
        _state.update { it.copy(showAutoLockDialog = true) }
    }

    fun dismissAutoLockDialog() {
        _state.update { it.copy(showAutoLockDialog = false) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            securityPreferences.setAutoLockMinutes(minutes)
            _state.update { it.copy(autoLockMinutes = minutes) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Generation Preferences
    // ═══════════════════════════════════════════════════════════════════════════

    fun showNationalityDialog() {
        _state.update { it.copy(showNationalityDialog = true) }
    }

    fun dismissNationalityDialog() {
        _state.update { it.copy(showNationalityDialog = false) }
    }

    fun setNationality(nationality: Nationality) {
        viewModelScope.launch {
            settingsDataStore.setNationality(nationality)
            _state.update { it.copy(selectedNationality = nationality) }
        }
    }

    /**
     * Show gender preference selection dialog.
     */
    fun showGenderDialog() {
        _state.update { it.copy(showGenderDialog = true) }
    }

    /**
     * Dismiss gender preference selection dialog.
     */
    fun dismissGenderDialog() {
        _state.update { it.copy(showGenderDialog = false) }
    }

    /**
     * Set gender preference for identity generation.
     */
    fun setGenderPreference(preference: GenderPreference) {
        viewModelScope.launch {
            settingsDataStore.setGenderPreference(preference)
            _state.update {
                it.copy(
                    selectedGenderPreference = preference,
                    showGenderDialog = false
                )
            }
        }
    }

    fun showAgeRangeDialog() {
        _state.update { it.copy(showAgeRangeDialog = true) }
    }

    fun dismissAgeRangeDialog() {
        _state.update { it.copy(showAgeRangeDialog = false) }
    }

    fun setAgeRange(min: Int, max: Int) {
        viewModelScope.launch {
            settingsDataStore.setAgeRange(min, max)
            _state.update {
                it.copy(
                    ageRangeMin = min,
                    ageRangeMax = max
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Appearance Management (Skill 18)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Show theme selection dialog.
     */
    fun showThemeDialog() {
        _state.update { it.copy(showThemeDialog = true) }
    }

    /**
     * Dismiss theme selection dialog.
     */
    fun dismissThemeDialog() {
        _state.update { it.copy(showThemeDialog = false) }
    }

    /**
     * Set theme mode preference.
     * Theme change is applied immediately without restart.
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _state.update {
                it.copy(
                    themeMode = mode,
                    showThemeDialog = false
                )
            }
        }
    }

    /**
     * Show language selection dialog.
     */
    fun showLanguageDialog() {
        _state.update { it.copy(showLanguageDialog = true) }
    }

    /**
     * Dismiss language selection dialog.
     */
    fun dismissLanguageDialog() {
        _state.update { it.copy(showLanguageDialog = false) }
    }

    /**
     * Set app language preference.
     * Sets flag for UI to handle activity recreation.
     */
    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.setAppLanguage(language)
            _state.update {
                it.copy(
                    appLanguage = language,
                    showLanguageDialog = false,
                    languageChangeRequested = true
                )
            }
        }
    }

    /**
     * Clear language change request flag after handling.
     */
    fun clearLanguageChangeRequest() {
        _state.update { it.copy(languageChangeRequested = false) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Snackbar Management
    // ═══════════════════════════════════════════════════════════════════════════

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }
}