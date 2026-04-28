@file:Suppress("DEPRECATION") // EncryptedSharedPreferences/MasterKey - no replacement available

package com.anonforge.security.biometric

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cryptographic gate for biometric authentication.
 *
 * Replaces the previous "biometric prompt as a yes/no flag" model with a real
 * cryptographic check: a Keystore key with [setUserAuthenticationRequired] is
 * used to encrypt/decrypt a sentinel token via [BiometricPrompt.CryptoObject].
 * If [BiometricPrompt] is bypassed (e.g. by hooking the success callback), the
 * cipher cannot complete — the operation actually fails.
 *
 * The same [authenticate] entry point performs both **setup** (when no
 * sentinel exists yet) and **verification** afterwards. Existing users with
 * `biometricEnabled = true` but no sentinel are migrated transparently the
 * next time biometric is used: the single prompt sets up the gate.
 *
 * Hardening:
 * - Key requires user authentication for every operation
 *   (`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)` on API 30+).
 * - Key is invalidated when the user enrolls/removes a biometric
 *   ([setInvalidatedByBiometricEnrollment] = true). On
 *   [KeyPermanentlyInvalidatedException] the gate is reset and the caller
 *   gets a typed [Result.NeedsReenrollment] back.
 * - Sentinel is stored in [EncryptedSharedPreferences] (double encryption).
 */
@Singleton
class BiometricGate @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    sealed class Result {
        /** Authentication succeeded; gate is configured. */
        data object Success : Result()
        /** User cancelled or dismissed the prompt. */
        data object Cancelled : Result()
        /** Biometric attempt did not match (callback can retry). */
        data object Failed : Result()
        /** Permanent error — caller should fall back to PIN. */
        data class Error(val code: Int, val message: String) : Result()
        /**
         * Biometric set has changed (enrolled/removed). The Keystore key is
         * invalidated; the user must re-enable biometric in settings.
         */
        data object NeedsReenrollment : Result()
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "anonforge_biometric_gate"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val SENTINEL_PLAINTEXT = "anonforge_ok"
        private const val PREFS_FILE = "anonforge_biometric_gate"
        private const val PREF_SENTINEL = "sentinel_v1"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * True when both the Keystore key AND the sentinel exist — i.e. the gate
     * is in a coherent configured state. If only one of them survives (e.g.
     * Keystore was wiped by the OS), [authenticate] will treat the call as a
     * fresh setup.
     */
    fun isConfigured(): Boolean {
        if (!encryptedPrefs.contains(PREF_SENTINEL)) return false
        return try {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.containsAlias(KEY_ALIAS)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Run a biometric prompt that performs a real cryptographic operation.
     *
     * - First call: encrypts the sentinel and stores it (setup).
     * - Subsequent calls: decrypts the sentinel and verifies it equals the
     *   expected marker (verification).
     *
     * The provided callbacks fire on the main thread.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onResult: (Result) -> Unit
    ) {
        val isSetup = !isConfigured()

        val cipher = try {
            initCipher(isSetup = isSetup)
        } catch (_: KeyPermanentlyInvalidatedException) {
            reset()
            onResult(Result.NeedsReenrollment)
            return
        } catch (e: Exception) {
            onResult(Result.Error(-1, e.message ?: "Cipher init failed"))
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authenticatedCipher = result.cryptoObject?.cipher
                if (authenticatedCipher == null) {
                    onResult(Result.Error(-1, "No cipher in result"))
                    return
                }
                try {
                    if (isSetup) {
                        val ciphertext = authenticatedCipher.doFinal(
                            SENTINEL_PLAINTEXT.toByteArray(Charsets.UTF_8)
                        )
                        val combined = ByteArray(authenticatedCipher.iv.size + ciphertext.size)
                        System.arraycopy(authenticatedCipher.iv, 0, combined, 0, authenticatedCipher.iv.size)
                        System.arraycopy(ciphertext, 0, combined, authenticatedCipher.iv.size, ciphertext.size)
                        encryptedPrefs.edit {
                            putString(PREF_SENTINEL, Base64.encodeToString(combined, Base64.NO_WRAP))
                        }
                        onResult(Result.Success)
                    } else {
                        val combined = Base64.decode(
                            encryptedPrefs.getString(PREF_SENTINEL, null) ?: "",
                            Base64.NO_WRAP
                        )
                        val cipherText = combined.copyOfRange(
                            authenticatedCipher.iv.size, combined.size
                        )
                        val plain = authenticatedCipher.doFinal(cipherText)
                        if (String(plain, Charsets.UTF_8) == SENTINEL_PLAINTEXT) {
                            onResult(Result.Success)
                        } else {
                            onResult(Result.Error(-1, "Sentinel mismatch"))
                        }
                    }
                } catch (_: KeyPermanentlyInvalidatedException) {
                    reset()
                    onResult(Result.NeedsReenrollment)
                } catch (e: Exception) {
                    onResult(Result.Error(-1, e.message ?: "Crypto operation failed"))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> onResult(Result.Cancelled)
                    else -> onResult(Result.Error(errorCode, errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                onResult(Result.Failed)
            }
        }

        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        BiometricPrompt(activity, executor, callback).authenticate(promptInfo, cryptoObject)
    }

    /** Drop the gate state (key + sentinel). Used on disable or invalidation. */
    fun reset() {
        try {
            val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // ignore — Keystore cleanup is best-effort
        }
        encryptedPrefs.edit { remove(PREF_SENTINEL) }
    }

    /**
     * True when the device has BIOMETRIC_STRONG hardware enrolled. Used to
     * decide whether to even offer biometric to the user.
     */
    fun canUseBiometric(): Boolean {
        return BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun initCipher(isSetup: Boolean): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (isSetup) {
            // Always (re)generate the key on setup so we don't reuse a stale
            // alias if the sentinel was wiped by accident.
            generateKey()
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
        } else {
            val combined = Base64.decode(
                encryptedPrefs.getString(PREF_SENTINEL, null) ?: error("No sentinel"),
                Base64.NO_WRAP
            )
            // GCM IV is 12 bytes — see encryption side above.
            val iv = combined.copyOfRange(0, 12)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }
        return cipher
    }

    private fun getKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (ks.getKey(KEY_ALIAS, null) as SecretKey)
    }

    private fun generateKey() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                /* timeout = */ 0,
                /* type = */ KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, KEYSTORE
        )
        generator.init(builder.build())
        generator.generateKey()
    }
}
