package com.anonforge.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val securityPreferencesRepository: SecurityPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    /**
     * Checks initial state and determines navigation target.
     * Priority:
     * 1. Disclaimer not accepted → Show disclaimer
     * 2. Biometric enabled → Show unlock screen
     * 3. Otherwise → Go to main
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

                // Check if biometric is enabled
                val biometricEnabled = securityPreferencesRepository.biometricEnabledFlow.first()
                if (biometricEnabled) {
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
                // On error, go to main
                _state.update {
                    it.copy(
                        isLoading = false,
                        navigationTarget = SplashNavigationTarget.MAIN
                    )
                }
            }
        }
    }
}