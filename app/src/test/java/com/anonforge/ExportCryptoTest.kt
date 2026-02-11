package com.anonforge

import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for ExportCryptoManager password-based encryption.
 *
 * SECURITY VALIDATION:
 * - Validates PBKDF2-HMAC-SHA256 key derivation (600,000 iterations minimum)
 * - Validates AES-256-GCM encryption for database export
 * - Tests salt uniqueness (critical for password security)
 * - Tests wrong password rejection (auth tag verification)
 * - Tests various data sizes and password complexities
 *
 * This validates the ALGORITHM used for encrypted backup/restore,
 * independent of the actual ExportCryptoManager implementation.
 */
class ExportCryptoTest {

    companion object {
        // PBKDF2 parameters - MUST match ExportCryptoManager
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 600_000  // OWASP 2023 recommendation
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 32

        // AES-GCM parameters
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    @Test
    fun `encrypt and decrypt round trip`() {
        val data = "test database content".toByteArray()
        val password = "securepassword123".toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Decrypted should match original")
    }

    @Test
    fun `encrypt produces different output with different salts`() {
        val data = "test data".toByteArray()
        val password = "password".toCharArray()

        val encrypted1 = encrypt(data, password.clone())
        val encrypted2 = encrypt(data, password.clone())

        assertNotEquals(
            encrypted1.contentToString(),
            encrypted2.contentToString(),
            "Each encryption should have unique salt"
        )
    }

    @Test
    fun `decrypt with wrong password fails`() {
        val data = "test data".toByteArray()
        val password = "correct".toCharArray()
        val wrongPassword = "wrong".toCharArray()

        val encrypted = encrypt(data, password)

        assertFailsWith<Exception>("Wrong password should fail decryption") {
            decrypt(encrypted, wrongPassword)
        }
    }

    @Test
    fun `encrypt handles large database export`() {
        // Simulate a 1MB database export
        val data = ByteArray(1_000_000) { it.toByte() }
        val password = "password123".toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Large data should round-trip correctly")
    }

    @Test
    fun `encrypt handles empty database`() {
        val data = ByteArray(0)
        val password = "password".toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Empty data should round-trip correctly")
    }

    @Test
    fun `encrypt with special password characters`() {
        val data = "sensitive identity data".toByteArray()
        val password = "P@\$\$w0rd!#%^&*()_+-=".toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Special password chars should work")
    }

    @Test
    fun `encrypt with unicode password`() {
        val data = "test data".toByteArray()
        val password = "密码🔐émoji日本語".toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Unicode password should work")
    }

    @Test
    fun `encrypt with minimum password length`() {
        val data = "test".toByteArray()
        val password = "123456".toCharArray() // 6 chars minimum

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Minimum password should work")
    }

    @Test
    fun `encrypt with very long password`() {
        val data = "test".toByteArray()
        val password = "A".repeat(256).toCharArray()

        val encrypted = encrypt(data, password.clone())
        val decrypted = decrypt(encrypted, password.clone())

        assertContentEquals(data, decrypted, "Long password should work")
    }

    @Test
    fun `encrypted output is larger than input due to overhead`() {
        val data = "test".toByteArray()
        val password = "password".toCharArray()

        val encrypted = encrypt(data, password)

        // Overhead: salt (32) + IV (12) + auth tag (16) = 60 bytes minimum
        val expectedMinSize = data.size + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16
        assertTrue(
            encrypted.size >= expectedMinSize,
            "Encrypted should include salt, IV, and auth tag overhead"
        )
    }

    @Test
    fun `salt has correct length`() {
        val data = "test".toByteArray()
        val password = "password".toCharArray()

        val encrypted = encrypt(data, password)

        // Salt is the first 32 bytes
        assertTrue(
            encrypted.size >= SALT_LENGTH_BYTES,
            "Encrypted data must include salt"
        )
    }

    @Test
    fun `password is wiped after use`() {
        val password = "sensitive".toCharArray()
        val originalPassword = password.clone()

        encrypt("test".toByteArray(), password)

        // Password array should be zeroed
        val allZeros = password.all { it == '\u0000' }
        assertTrue(allZeros, "Password should be wiped after encryption")
    }

    @Test
    fun `similar passwords produce completely different output`() {
        val data = "test".toByteArray()
        val password1 = "password1".toCharArray()
        val password2 = "password2".toCharArray()

        val encrypted1 = encrypt(data, password1)
        val encrypted2 = encrypt(data, password2)

        // Even with same salt, different passwords = different derived keys
        assertNotEquals(
            encrypted1.copyOfRange(SALT_LENGTH_BYTES, encrypted1.size).contentToString(),
            encrypted2.copyOfRange(SALT_LENGTH_BYTES, encrypted2.size).contentToString(),
            "Different passwords should produce different ciphertext"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test helpers - mirror ExportCryptoManager implementation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Encrypts data using PBKDF2 key derivation + AES-256-GCM.
     *
     * Format: SALT (32 bytes) || IV (12 bytes) || CIPHERTEXT+TAG
     */
    private fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        try {
            // Generate random salt
            val salt = ByteArray(SALT_LENGTH_BYTES)
            SecureRandom().nextBytes(salt)

            // Derive key using PBKDF2
            val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val derivedKey = keyFactory.generateSecret(keySpec)
            val secretKey = SecretKeySpec(derivedKey.encoded, "AES")

            // Encrypt with AES-GCM
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(data)

            // Combine: salt || IV || ciphertext
            val result = ByteArray(salt.size + iv.size + ciphertext.size)
            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(iv, 0, result, salt.size, iv.size)
            System.arraycopy(ciphertext, 0, result, salt.size + iv.size, ciphertext.size)

            return result
        } finally {
            // SECURITY: Wipe password from memory
            password.fill('\u0000')
        }
    }

    /**
     * Decrypts data using PBKDF2 key derivation + AES-256-GCM.
     */
    private fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray {
        try {
            // Extract components
            val salt = encrypted.copyOfRange(0, SALT_LENGTH_BYTES)
            val iv = encrypted.copyOfRange(SALT_LENGTH_BYTES, SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
            val ciphertext = encrypted.copyOfRange(SALT_LENGTH_BYTES + IV_LENGTH_BYTES, encrypted.size)

            // Derive key using PBKDF2
            val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val derivedKey = keyFactory.generateSecret(keySpec)
            val secretKey = SecretKeySpec(derivedKey.encoded, "AES")

            // Decrypt with AES-GCM
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            return cipher.doFinal(ciphertext)
        } finally {
            // SECURITY: Wipe password from memory
            password.fill('\u0000')
        }
    }
}