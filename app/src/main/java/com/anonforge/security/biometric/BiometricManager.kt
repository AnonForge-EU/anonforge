package com.anonforge.security.biometric

import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages biometric authentication for AnonForge.
 *
 * Supports:
 * - Fingerprint
 * - Face recognition
 * - PIN/Pattern/Password fallback (DEVICE_CREDENTIAL)
 *
 * Note: This is an alternative biometric implementation. The primary
 * biometric management is handled by AuthManager + BiometricAuthenticator.
 * This class provides DEVICE_CREDENTIAL fallback for enhanced UX scenarios.
 */
@Singleton
class BiometricManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val biometricManager = AndroidBiometricManager.from(context)

    /**
     * Check if biometric authentication is available.
     * Returns the current capability status.
     */
    @Suppress("unused") // Public API for capability checks
    fun canAuthenticate(): BiometricCapability {
        // Check for biometric OR device credential
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return when (result) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.UNAVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            AndroidBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.UNAVAILABLE
            else -> BiometricCapability.UNAVAILABLE
        }
    }

    /**
     * Check if ONLY biometric (no PIN fallback) is available.
     * Reserved for future use when strict biometric-only mode is needed.
     */
    @Suppress("unused") // Reserved for future strict biometric mode
    fun canAuthenticateBiometricOnly(): BiometricCapability {
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        return when (result) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.UNAVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            else -> BiometricCapability.UNAVAILABLE
        }
    }

    /**
     * Authenticate user with biometric + PIN/Pattern/Password fallback.
     *
     * IMPORTANT: When using DEVICE_CREDENTIAL, the system handles the fallback
     * automatically - no negative button is shown.
     */
    @Suppress("unused") // Public API for biometric + device credential auth
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock AnonForge",
        subtitle: String = "Authenticate to access your identities",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't treat user cancellation as error
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        // Use BIOMETRIC_STRONG | DEVICE_CREDENTIAL for PIN/Pattern fallback
        // Note: Cannot use setNegativeButtonText() with DEVICE_CREDENTIAL
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false) // Skip confirmation for faster UX
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Authenticate with biometric ONLY (no PIN fallback).
     * Use this when you specifically need biometric confirmation.
     * Reserved for future use in high-security scenarios.
     */
    @Suppress("unused") // Reserved for future strict biometric mode
    fun authenticateBiometricOnly(
        activity: FragmentActivity,
        title: String = "Biometric Required",
        subtitle: String = "Use fingerprint or face to continue",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        // BIOMETRIC_STRONG only - allows negative button
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
