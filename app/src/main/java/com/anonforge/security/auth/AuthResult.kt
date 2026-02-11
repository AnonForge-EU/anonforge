package com.anonforge.security.auth

/**
 * Result of an authentication attempt (PIN verification).
 * Provides detailed feedback for UI updates.
 */
sealed class AuthResult {
    /** Authentication successful */
    data object Success : AuthResult()

    /** Authentication failed with remaining attempts */
    data class Failed(
        val message: String,
        val attemptsRemaining: Int
    ) : AuthResult()

    /**
     * Account locked due to too many failures.
     *
     * @param durationSeconds Lockout duration in seconds
     */
    data class LockedOut(
        val durationSeconds: Int
    ) : AuthResult()

    /**
     * Generic error during authentication.
     *
     * @param message Error description
     */
    data class Error(
        val message: String
    ) : AuthResult()
}