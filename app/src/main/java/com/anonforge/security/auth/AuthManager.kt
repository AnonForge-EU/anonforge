package com.anonforge.security.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anonforge.data.local.prefs.SecurityPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all authentication operations including biometric and PIN.
 *
 * Security features:
 * - BiometricPrompt for fingerprint/face authentication
 * - PIN verification using SecurityPreferences (AES decryption)
 * - Lockout after failed attempts
 * - No sensitive data logging
 *
 * FIXED: Now uses SecurityPreferences for PIN operations (same class used by SettingsViewModel)
 * instead of SecurityPreferencesRepository with SHA-256 hash comparison.
 *
 * Root cause of PIN bug: SettingsViewModel.setPin() used SecurityPreferences.setPin()
 * which ENCRYPTS the PIN with AES. But AuthManager.verifyPin() was comparing against
 * a SHA-256 HASH - completely incompatible algorithms! Now both use AES encryption.
 */
@Singleton
class AuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val securityPreferences: SecurityPreferences,
    private val lockManager: LockManager
) {
    private val biometricManager = BiometricManager.from(context)

    companion object {
        private const val MAX_PIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_SECONDS = 300 // 5 minutes
    }

    // ==================== Configuration Checks ====================

    /**
     * Check if any authentication method is configured.
     */
    suspend fun isAuthConfigured(): Boolean {
        return isBiometricEnabled() || isPinConfigured()
    }

    /**
     * Check if biometric authentication is enabled in app settings.
     */
    suspend fun isBiometricEnabled(): Boolean {
        return securityPreferences.biometricEnabled.first()
    }

    /**
     * Check if biometrics are enrolled on the device.
     */
    fun isBiometricEnrolled(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if biometric hardware is available.
     */
    @Suppress("unused") // Public API for settings UI capability checks
    fun isBiometricHardwareAvailable(): Boolean {
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        return result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    /**
     * Check if PIN is configured.
     * FIXED: Now uses SecurityPreferences.hasPin() - same storage as SettingsViewModel
     */
    suspend fun isPinConfigured(): Boolean {
        return securityPreferences.hasPin()
    }

    // ==================== Biometric Authentication ====================

    /**
     * Create BiometricPrompt instance for the given activity.
     *
     * @param activity FragmentActivity for prompt display
     * @param onSuccess Called when authentication succeeds
     * @param onError Called when error occurs (code, message)
     * @param onFailed Called when biometric doesn't match
     */
    fun createBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        onFailed: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                lockManager.startSession()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Create PromptInfo for biometric dialog.
     */
    fun createPromptInfo(
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
    }

    // ==================== PIN Authentication ====================

    /**
     * Set up a new PIN.
     * Delegates to SecurityPreferences which encrypts with AES.
     *
     * @param pin The PIN as CharArray (will be wiped after use)
     * @return true if PIN was set successfully
     */
    @Suppress("unused") // Public API - PIN setup available via SettingsViewModel.setPin()
    suspend fun setPin(pin: CharArray): Boolean {
        return try {
            val pinString = String(pin)
            securityPreferences.setPin(pinString)
            // Security: wipe PIN from memory
            pin.fill('\u0000')
            true
        } catch (_: Exception) {
            pin.fill('\u0000')
            false
        }
    }

    /**
     * Verify PIN against stored value.
     *
     * FIXED: Now uses SecurityPreferences.verifyPin() which decrypts the stored
     * PIN and compares directly. Previously used SHA-256 hashing which was
     * incompatible with the AES encryption used by setPin().
     *
     * @param pin The PIN to verify (will be wiped after use)
     * @return AuthResult indicating success, failure, or lockout
     */
    fun verifyPin(pin: CharArray): AuthResult {
        // Check lockout first
        if (isLockedOut()) {
            pin.fill('\u0000')
            return AuthResult.LockedOut(getRemainingLockoutSeconds())
        }

        return try {
            val pinString = String(pin)
            pin.fill('\u0000') // Security: wipe immediately

            // Use SecurityPreferences.verifyPin() which decrypts and compares
            val isValid = runBlocking { securityPreferences.verifyPin(pinString) }

            if (isValid) {
                lockManager.resetFailedAttempts()
                lockManager.startSession()
                AuthResult.Success
            } else {
                val remaining = lockManager.recordFailedAttempt()
                if (remaining <= 0) {
                    lockManager.startLockout(LOCKOUT_DURATION_SECONDS)
                    AuthResult.LockedOut(LOCKOUT_DURATION_SECONDS)
                } else {
                    AuthResult.Failed("Incorrect PIN", remaining)
                }
            }
        } catch (_: Exception) {
            pin.fill('\u0000')
            AuthResult.Error("Verification failed")
        }
    }

    /**
     * Remove configured PIN.
     */
    @Suppress("unused") // Public API for PIN removal in settings
    suspend fun clearPin() {
        securityPreferences.clearPin()
    }

    // ==================== Lockout Management ====================

    /**
     * Check if currently locked out.
     */
    fun isLockedOut(): Boolean = lockManager.isLockedOut()

    /**
     * Get remaining lockout time in seconds.
     */
    fun getRemainingLockoutSeconds(): Int = lockManager.getRemainingLockoutSeconds()

    /**
     * Get remaining PIN attempts before lockout.
     */
    fun getRemainingAttempts(): Int = MAX_PIN_ATTEMPTS - lockManager.getFailedAttempts()
}