package com.anonforge.security.auth

/**
 * Represents the current authentication state of the app.
 * Used by UnlockScreen to determine UI and navigation.
 */
sealed class AuthState {
    /** User needs to authenticate */
    data object RequiresAuth : AuthState()

    /** Biometric authentication in progress */
    data object BiometricInProgress : AuthState()

    /** PIN authentication in progress */
    data object PinInProgress : AuthState()

    /** Successfully authenticated */
    data object Authenticated : AuthState()

    /** Locked out due to too many failed attempts */
    data class LockedOut(val remainingSeconds: Int) : AuthState()

    /**
     * Authentication error occurred.
     *
     * @param message Error description for display
     */
    data class Error(
        val message: String
    ) : AuthState()
}