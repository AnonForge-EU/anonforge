package com.anonforge.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.data.local.prefs.SecurityPreferences
import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.data.repository.SecurityPreferencesRepository
import com.anonforge.security.encryption.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val securityPreferencesRepository: SecurityPreferencesRepository,
    private val securityPreferences: SecurityPreferences,
    private val keyManager: KeyManager
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    /**
     * Checks initial state and determines navigation target.
     * Priority:
     * 1. Vault exists but its key is lost → Show "vault unreadable" error screen
     * 2. Disclaimer not accepted → Show disclaimer
     * 3. Any unlock method configured (biometric or PIN) → Show unlock screen
     * 4. Otherwise → Go to main
     *
     * Any failure to read the preference stores routes to UNLOCK (fail-closed).
     */
    fun checkInitialState() {
        viewModelScope.launch {
            try {
                // SECURITY: The database is first opened when the vault screen is
                // reached. If its passphrase is gone while the encrypted file
                // still holds data, KeyManager would throw VaultKeyLostException
                // there — so detect it up front (side-effect free) and route to
                // an explicit error screen instead of crashing.
                val vaultKeyLost = withContext(Dispatchers.IO) {
                    keyManager.isVaultKeyLost()
                }
                if (vaultKeyLost) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            navigationTarget = SplashNavigationTarget.VAULT_UNREADABLE
                        )
                    }
                    return@launch
                }

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