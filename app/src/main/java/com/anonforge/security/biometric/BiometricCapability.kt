package com.anonforge.security.biometric

/**
 * Biometric authentication capability status.
 * 
 * Used by BiometricManager to report device biometric support status.
 * Extracted to standalone file after BiometricAuthenticator.kt removal.
 */
enum class BiometricCapability {
    /** Hardware available and biometrics enrolled - ready to use */
    AVAILABLE,
    
    /** No biometric hardware on device */
    NO_HARDWARE,
    
    /** Hardware exists but temporarily unavailable */
    UNAVAILABLE,
    
    /** Hardware available but no biometrics enrolled */
    NOT_ENROLLED
}
