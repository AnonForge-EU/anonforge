package com.anonforge.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
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
 * Secure API key management using Android Keystore with AES-256-GCM encryption.
 *
 * SECURITY FEATURES:
 * - Keys stored in Android Keystore (hardware-backed when available)
 * - AES-256-GCM encryption (authenticated encryption)
 * - Memory wipe after key operations (CharArray.fill)
 * - No plaintext storage anywhere
 * - Keys are never logged or exposed
 * - Double encryption: Keystore + EncryptedSharedPreferences
 *
 * OWASP M2 COMPLIANCE:
 * - Cryptographic keys protected by hardware security module when available
 * - Strong encryption algorithm (AES-256-GCM)
 * - Secure key generation via Android Keystore
 */
@Singleton
class ApiKeyManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "anonforge_simplelogin_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        private const val PREFS_FILE = "anonforge_api_keys"
        private const val STORAGE_KEY = "encrypted_api_key"
    }

    // Encrypted SharedPreferences for persistent storage
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

    // In-memory cache of encrypted key
    @Volatile
    private var encryptedApiKey: String? = null

    init {
        // Load existing encrypted key on initialization
        loadEncryptedKey()
    }

    /**
     * Check if an API key is configured.
     * Does NOT expose the actual key.
     */
    fun hasApiKey(): Boolean {
        return encryptedApiKey != null && encryptedApiKey!!.isNotEmpty()
    }

    /**
     * Store API key securely.
     *
     * @param apiKey The API key as CharArray (will be wiped after storage)
     * @return true if storage succeeded
     */
    fun storeApiKey(apiKey: CharArray): Boolean {
        return try {
            // Convert to String temporarily for encryption
            val keyString = String(apiKey)

            // Encrypt using Android Keystore (additional layer)
            val encrypted = encryptWithKeystore(keyString.toByteArray(Charsets.UTF_8))

            // Store in EncryptedSharedPreferences (double encryption)
            encryptedApiKey = encrypted
            persistEncryptedKey(encrypted)

            // SECURITY: Wipe sensitive data from memory
            apiKey.fill('\u0000')

            true
        } catch (_: Exception) {
            // SECURITY: Don't log the actual error details
            apiKey.fill('\u0000')
            false
        }
    }

    /**
     * Retrieve API key for use.
     *
     * IMPORTANT: Caller MUST wipe the returned CharArray after use!
     * Example:
     * ```
     * val key = apiKeyManager.retrieveApiKey()
     * try {
     *     // use key
     * } finally {
     *     key?.fill('\u0000')
     * }
     * ```
     *
     * @return API key as CharArray or null if not configured
     */
    fun retrieveApiKey(): CharArray? {
        val encrypted = encryptedApiKey ?: return null
        if (encrypted.isEmpty()) return null

        return try {
            val decrypted = decryptWithKeystore(encrypted)
            val keyString = String(decrypted, Charsets.UTF_8)

            // SECURITY: Wipe intermediate byte array
            decrypted.fill(0)

            keyString.toCharArray()
        } catch (_: Exception) {
            // Decryption failed - key may be corrupted or Keystore changed
            null
        }
    }

    /**
     * Clear the stored API key.
     * Removes from both memory and persistent storage.
     */
    fun clearApiKey() {
        encryptedApiKey = null
        clearPersistedKey()

        // Also delete the Keystore key for complete cleanup
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    /**
     * Get masked display string for UI.
     * NEVER returns the actual key, only a fixed mask.
     */
    fun getMaskedDisplay(): String {
        return if (hasApiKey()) "••••••••••••••••" else ""
    }

    /**
     * Get partial hint for UI (first 3 chars + "..." only).
     * Safe to display as it doesn't reveal the full key.
     */
    fun getKeyHint(): String? {
        val key = retrieveApiKey() ?: return null
        return try {
            if (key.size >= 3) {
                "${key[0]}${key[1]}${key[2]}..."
            } else {
                "•••"
            }
        } finally {
            key.fill('\u0000')
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PRIVATE ENCRYPTION METHODS (Android Keystore AES-256-GCM)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Encrypt data using Android Keystore AES-256-GCM.
     * Returns Base64-encoded string containing IV + ciphertext.
     */
    private fun encryptWithKeystore(data: ByteArray): String {
        val secretKey = getOrCreateKeystoreKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)

        // Combine IV + ciphertext for storage
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        // Wipe intermediate data
        data.fill(0)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt data using Android Keystore AES-256-GCM.
     * Expects Base64-encoded string containing IV + ciphertext.
     */
    private fun decryptWithKeystore(encryptedBase64: String): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // Extract IV and ciphertext
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val secretKey = getOrCreateKeystoreKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Get existing or create new AES-256 key in Android Keystore.
     */
    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Return existing key if present
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let {
            return it as SecretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Key available without biometric
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PERSISTENCE (via EncryptedSharedPreferences)
    // ════════════════════════════════════════════════════════════════════════════

    private fun loadEncryptedKey() {
        encryptedApiKey = encryptedPrefs.getString(STORAGE_KEY, null)
    }

    private fun persistEncryptedKey(encrypted: String) {
        encryptedPrefs.edit { putString(STORAGE_KEY, encrypted) }
    }

    private fun clearPersistedKey() {
        encryptedPrefs.edit { remove(STORAGE_KEY) }
    }
}