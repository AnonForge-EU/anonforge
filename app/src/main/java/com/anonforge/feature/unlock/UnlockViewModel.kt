package com.anonforge.feature.unlock

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.security.auth.AuthManager
import com.anonforge.security.auth.AuthResult
import com.anonforge.security.auth.AuthState
import com.anonforge.security.auth.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the unlock screen.
 *
 * Responsibilities:
 * - Check authentication requirements on init
 * - Handle biometric authentication callbacks
 * - Manage PIN verification flow
 * - Track lockout state
 *
 * Security notes:
 * - PIN is passed as CharArray and wiped in AuthManager
 * - No sensitive data logging
 */
@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val lockManager: LockManager
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    init {
        checkAuthRequirements()
    }

    /**
     * Check what authentication is needed on screen load.
     */
    private fun checkAuthRequirements() {
        viewModelScope.launch {
            // Check if any auth is configured
            val isAuthConfigured = authManager.isAuthConfigured()
            if (!isAuthConfigured) {
                // No auth configured - allow immediate access
                _state.update { it.copy(authState = AuthState.Authenticated) }
                return@launch
            }

            // Check if we have an active session
            val shouldRequireAuth = lockManager.shouldRequireAuth()
            if (!shouldRequireAuth && lockManager.hasActiveSession()) {
                // Active session exists - allow access
                _state.update { it.copy(authState = AuthState.Authenticated) }
                return@launch
            }

            // Check lockout status
            if (authManager.isLockedOut()) {
                _state.update {
                    it.copy(
                        authState = AuthState.LockedOut(authManager.getRemainingLockoutSeconds()),
                        showLockoutDialog = true
                    )
                }
                return@launch
            }

            // Determine available auth methods
            val biometricEnabled = authManager.isBiometricEnabled()
            val biometricEnrolled = authManager.isBiometricEnrolled()
            val pinConfigured = authManager.isPinConfigured()
            val biometricAvailable = biometricEnabled && biometricEnrolled

            _state.update {
                it.copy(
                    authState = AuthState.RequiresAuth,
                    biometricAvailable = biometricAvailable,
                    pinAvailable = pinConfigured,
                    shouldTryBiometric = biometricAvailable, // Auto-trigger if available
                    attemptsRemaining = authManager.getRemainingAttempts()
                )
            }
        }
    }

    /**
     * Manually trigger biometric prompt.
     */
    fun triggerBiometric() {
        if (_state.value.biometricAvailable) {
            _state.update {
                it.copy(
                    authState = AuthState.BiometricInProgress,
                    shouldTryBiometric = true
                )
            }
        }
    }

    /**
     * Called when biometric authentication succeeds.
     */
    fun onBiometricSuccess() {
        lockManager.startSession()
        _state.update {
            it.copy(
                authState = AuthState.Authenticated,
                shouldTryBiometric = false
            )
        }
    }

    /**
     * Called when biometric authentication has an error.
     */
    fun onBiometricError(errorCode: Int, errorMessage: String) {
        val shouldFallbackToPin = when (errorCode) {
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> true
            else -> _state.value.pinAvailable
        }

        if (shouldFallbackToPin && _state.value.pinAvailable) {
            // Fallback to PIN
            _state.update {
                it.copy(
                    authState = AuthState.PinInProgress,
                    shouldTryBiometric = false,
                    showPinDialog = true,
                    errorMessage = when (errorCode) {
                        BiometricPrompt.ERROR_LOCKOUT -> "Biometric locked. Use PIN."
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometric disabled. Use PIN."
                        else -> null
                    }
                )
            }
        } else {
            // No fallback available
            _state.update {
                it.copy(
                    authState = AuthState.Error(errorMessage),
                    shouldTryBiometric = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    /**
     * Called when biometric doesn't match (but user can retry).
     */
    fun onBiometricFailed() {
        _state.update {
            it.copy(errorMessage = "Fingerprint not recognized. Try again.")
        }
    }

    /**
     * Show PIN input dialog.
     */
    fun showPinDialog() {
        _state.update {
            it.copy(
                authState = AuthState.PinInProgress,
                showPinDialog = true,
                pinError = null
            )
        }
    }

    /**
     * Verify entered PIN.
     * @param pin PIN as CharArray (will be wiped after verification)
     */
    fun verifyPin(pin: CharArray) {
        _state.update { it.copy(isVerifyingPin = true, pinError = null) }

        // PIN verification is synchronous (blocking hash comparison)
        // Note: pin is wiped inside authManager.verifyPin()
        when (val result = authManager.verifyPin(pin)) {
            is AuthResult.Success -> {
                _state.update {
                    it.copy(
                        authState = AuthState.Authenticated,
                        showPinDialog = false,
                        isVerifyingPin = false,
                        pinError = null
                    )
                }
            }
            is AuthResult.Failed -> {
                _state.update {
                    it.copy(
                        isVerifyingPin = false,
                        pinError = result.message,
                        attemptsRemaining = result.attemptsRemaining
                    )
                }
            }
            is AuthResult.LockedOut -> {
                _state.update {
                    it.copy(
                        authState = AuthState.LockedOut(authManager.getRemainingLockoutSeconds()),
                        showPinDialog = false,
                        showLockoutDialog = true,
                        isVerifyingPin = false
                    )
                }
            }
            is AuthResult.Error -> {
                _state.update {
                    it.copy(
                        isVerifyingPin = false,
                        pinError = result.message
                    )
                }
            }
        }
    }

    /**
     * Dismiss PIN dialog without verifying.
     */
    fun dismissPinDialog() {
        _state.update {
            it.copy(
                showPinDialog = false,
                pinError = null,
                authState = AuthState.RequiresAuth
            )
        }
    }

    /**
     * Dismiss lockout dialog.
     */
    fun dismissLockoutDialog() {
        _state.update { it.copy(showLockoutDialog = false) }
    }

    /**
     * Clear error messages.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null, pinError = null) }
    }

    /**
     * Refresh lockout timer (called every second during lockout).
     */
    fun refreshLockoutTimer() {
        if (authManager.isLockedOut()) {
            _state.update {
                it.copy(
                    authState = AuthState.LockedOut(authManager.getRemainingLockoutSeconds())
                )
            }
        } else {
            // Lockout expired - allow retry
            lockManager.clearLockout()
            _state.update {
                it.copy(
                    authState = AuthState.RequiresAuth,
                    showLockoutDialog = false,
                    attemptsRemaining = authManager.getRemainingAttempts()
                )
            }
        }
    }

    /**
     * Expose AuthManager for BiometricPrompt creation in UI.
     * This is needed because BiometricPrompt requires a FragmentActivity.
     */
    fun getAuthManager(): AuthManager = authManager
}

/**
 * UI state for UnlockScreen.
 */
data class UnlockUiState(
    /** Current authentication state */
    val authState: AuthState = AuthState.RequiresAuth,

    /** Whether biometric authentication is available and enabled */
    val biometricAvailable: Boolean = false,

    /** Whether PIN authentication is configured */
    val pinAvailable: Boolean = false,

    /** Whether to auto-trigger biometric prompt */
    val shouldTryBiometric: Boolean = false,

    /** Whether PIN dialog is showing */
    val showPinDialog: Boolean = false,

    /** Whether lockout dialog is showing */
    val showLockoutDialog: Boolean = false,

    /** Whether PIN verification is in progress */
    val isVerifyingPin: Boolean = false,

    /** Error message for PIN input */
    val pinError: String? = null,

    /** Remaining PIN attempts before lockout */
    val attemptsRemaining: Int = 5,

    /** General error message for snackbar */
    val errorMessage: String? = null
)