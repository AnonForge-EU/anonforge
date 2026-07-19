package com.anonforge.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.data.local.prefs.SecurityPreferences
import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.data.repository.SecurityPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val securityPreferencesRepository: SecurityPreferencesRepository,
    private val securityPreferences: SecurityPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    /**
     * Checks initial state and determines navigation target.
     * Priority:
     * 1. Disclaimer not accepted → Show disclaimer
     * 2. Any unlock method configured (biometric or PIN) → Show unlock screen
     * 3. Otherwise → Go to main
     *
     * Any failure to read the preference stores routes to UNLOCK (fail-closed).
     */
    fun checkInitialState() {
        viewModelScope.launch {
            try {
                // Check if disclaimer was accepted
                val disclaimerAccepted = preferencesRepository.isDisclaimerAccepted()
                if (!disclaimerAccepted) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            navigationTarget = SplashNavigationTarget.DISCLAIMER
                        )
                    }
                    return@launch
                }

                // Route through UNLOCK if any auth method is configured.
                // The PIN lives in SecurityPreferences (the current store used by
                // AuthManager/SettingsViewModel) — NOT in the legacy pin fields of
                // SecurityPreferencesRepository. Checking only the biometric flag
                // here used to let PIN-only users straight into the vault.
                val biometricEnabled = securityPreferencesRepository.biometricEnabledFlow.first()
                val pinConfigured = securityPreferences.hasPin()
                if (biometricEnabled || pinConfigured) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            navigationTarget = SplashNavigationTarget.UNLOCK
                        )
                    }
                    return@launch
                }

                // No special requirements, go to main
                _state.update {
                    it.copy(
                        isLoading = false,
                        navigationTarget = SplashNavigationTarget.MAIN
                    )
                }
            } catch (_: Exception) {
                // Fail-closed: if a preference store is unreadable (DataStore
                // CorruptionException / IOException) we cannot prove the vault
                // is unprotected, so an error must never skip the lock screen.
                // UnlockScreen degrades safely: it keeps PIN entry available
                // with an explicit error instead of crashing, and if no auth
                // is actually configured it lets the user through once the
                // store reads cleanly again.
                _state.update {
                    it.copy(
                        isLoading = false,
                        navigationTarget = SplashNavigationTarget.UNLOCK
                    )
                }
            }
        }
    }
}