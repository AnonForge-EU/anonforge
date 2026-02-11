package com.anonforge.security.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cryptographic keys for AnonForge.
 *
 * Security architecture:
 * - Field encryption: Uses Android Keystore directly for AES-GCM operations
 * - Database passphrase: Random passphrase encrypted with Keystore key, stored in EncryptedSharedPreferences
 *
 * Note: Android Keystore keys don't expose raw bytes (getEncoded() returns null),
 * so we use them for encryption/decryption operations only.
 */
@Singleton
class KeyManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val FIELD_ENCRYPTION_KEY_ALIAS = "anonforge_field_encryption_key"
        private const val DATABASE_PASSPHRASE_KEY_ALIAS = "anonforge_db_passphrase_key"
        private const val PREFS_NAME = "anonforge_secure_prefs"
        private const val PREF_DB_PASSPHRASE = "encrypted_db_passphrase"
        private const val PASSPHRASE_LENGTH = 32 // 256 bits
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    /**
     * Returns the field encryption key for encrypting sensitive identity fields.
     * Creates a new key if one doesn't exist.
     */
    fun getFieldEncryptionKey(): SecretKey {
        return keyStore.getKey(FIELD_ENCRYPTION_KEY_ALIAS, null) as? SecretKey
            ?: generateFieldEncryptionKey()
    }

    /**
     * Returns the database passphrase for SQLCipher.
     * The passphrase is randomly generated once, then encrypted and stored securely.
     */
    fun getDatabasePassphrase(): CharArray {
        // Try to retrieve existing passphrase
        val storedPassphrase = encryptedPrefs.getString(PREF_DB_PASSPHRASE, null)

        return if (storedPassphrase != null) {
            // Decrypt and return existing passphrase
            decryptPassphrase(storedPassphrase)
        } else {
            // Generate, encrypt, store, and return new passphrase
            generateAndStorePassphrase()
        }
    }

    /**
     * Generates a cryptographically secure random passphrase,
     * encrypts it with the Keystore key, and stores it.
     */
    private fun generateAndStorePassphrase(): CharArray {
        // Generate random passphrase bytes
        val passphraseBytes = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphraseBytes)

        // Convert to hex string for SQLCipher compatibility
        val passphrase = passphraseBytes.joinToString("") { "%02x".format(it) }

        // Securely wipe the raw bytes
        passphraseBytes.fill(0)

        // Encrypt and store the passphrase using KTX extension
        val encryptedPassphrase = encryptPassphrase(passphrase)
        encryptedPrefs.edit {
            putString(PREF_DB_PASSPHRASE, encryptedPassphrase)
        }

        return passphrase.toCharArray()
    }

    /**
     * Encrypts the passphrase using the Keystore key.
     * Format: Base64(IV + ciphertext)
     */
    private fun encryptPassphrase(passphrase: String): String {
        val key = getOrCreatePassphraseKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))

        // Combine IV + ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts the stored passphrase using the Keystore key.
     */
    private fun decryptPassphrase(encryptedData: String): CharArray {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

        // Extract IV and ciphertext
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val key = getOrCreatePassphraseKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decrypted = cipher.doFinal(ciphertext)
        val passphrase = String(decrypted, Charsets.UTF_8).toCharArray()

        // Securely wipe intermediate data
        decrypted.fill(0)

        return passphrase
    }

    /**
     * Gets or creates the key used to encrypt the database passphrase.
     */
    private fun getOrCreatePassphraseKey(): SecretKey {
        return keyStore.getKey(DATABASE_PASSPHRASE_KEY_ALIAS, null) as? SecretKey
            ?: generatePassphraseKey()
    }

    /**
     * Generates AES key for field-level encryption.
     * Uses StrongBox if available, falls back to TEE otherwise.
     */
    private fun generateFieldEncryptionKey(): SecretKey {
        return try {
            generateAesKey(FIELD_ENCRYPTION_KEY_ALIAS, useStrongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            // Fallback to TEE-backed key if StrongBox not available
            generateAesKey(FIELD_ENCRYPTION_KEY_ALIAS, useStrongBox = false)
        }
    }

    /**
     * Generates AES key for encrypting the database passphrase.
     * Uses StrongBox if available, falls back to TEE otherwise.
     */
    private fun generatePassphraseKey(): SecretKey {
        return try {
            generateAesKey(DATABASE_PASSPHRASE_KEY_ALIAS, useStrongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            // Fallback to TEE-backed key if StrongBox not available
            generateAesKey(DATABASE_PASSPHRASE_KEY_ALIAS, useStrongBox = false)
        }
    }

    /**
     * Generates an AES-256-GCM key in Android Keystore.
     */
    private fun generateAesKey(alias: String, useStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (useStrongBox) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Clears all keys and stored data.
     * Use with caution - this will make existing encrypted data unrecoverable!
     *
     * Called during:
     * - Factory reset
     * - User-initiated data wipe
     * - Security incident response
     */
    @Suppress("unused")
    fun clearAllKeys() {
        // Remove Keystore keys
        if (keyStore.containsAlias(FIELD_ENCRYPTION_KEY_ALIAS)) {
            keyStore.deleteEntry(FIELD_ENCRYPTION_KEY_ALIAS)
        }
        if (keyStore.containsAlias(DATABASE_PASSPHRASE_KEY_ALIAS)) {
            keyStore.deleteEntry(DATABASE_PASSPHRASE_KEY_ALIAS)
        }

        // Clear encrypted preferences using KTX extension
        encryptedPrefs.edit { clear() }
    }
}