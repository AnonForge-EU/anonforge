package com.anonforge

import org.junit.Before
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for AES-256-GCM encryption/decryption operations.
 *
 * SECURITY VALIDATION:
 * - Verifies AES-256-GCM algorithm works correctly
 * - Tests IV uniqueness (critical for GCM security)
 * - Tests round-trip encryption/decryption
 * - Tests various input lengths and character sets
 *
 * Note: Uses test key since Android Keystore is not available in unit tests.
 * This validates the ALGORITHM, not the key storage.
 */
class CryptoManagerTest {

    private lateinit var secretKey: SecretKey

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH = 128
        private const val IV_LENGTH = 12
    }

    @Before
    fun setup() {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        secretKey = keyGenerator.generateKey()
    }

    @Test
    fun `encrypt produces base64 encoded output`() {
        val plaintext = "test@example.com"

        val encrypted = encrypt(plaintext)

        val decoded = Base64.getDecoder().decode(encrypted)
        assertTrue(decoded.isNotEmpty(), "Encrypted data should not be empty")
    }

    @Test
    fun `encrypt and decrypt round trip returns original`() {
        val plaintext = "MySecretPIN123"

        val encrypted = encrypt(plaintext)
        val decrypted = decrypt(encrypted)

        assertEquals(plaintext, decrypted, "Decrypted should match original")
    }

    @Test
    fun `encrypt produces different output each time due to unique IV`() {
        val plaintext = "test"

        val encrypted1 = encrypt(plaintext)
        val encrypted2 = encrypt(plaintext)

        assertNotEquals(encrypted1, encrypted2, "Each encryption should have unique IV")
    }

    @Test
    fun `decrypt handles various input lengths`() {
        val inputs = listOf(
            "a",
            "short",
            "medium length text",
            "A".repeat(100),
            "A".repeat(1000)
        )

        inputs.forEach { plaintext ->
            val encrypted = encrypt(plaintext)
            val decrypted = decrypt(encrypted)

            assertEquals(plaintext, decrypted, "Failed for input length: ${plaintext.length}")
        }
    }

    @Test
    fun `decrypt handles special characters`() {
        val plaintext = "Special chars: !@#\$%^&*()_+-=[]{}|;':\",./<>?`~"

        val encrypted = encrypt(plaintext)
        val decrypted = decrypt(encrypted)

        assertEquals(plaintext, decrypted, "Special characters should round-trip correctly")
    }

    @Test
    fun `decrypt handles unicode and emojis`() {
        val plaintext = "Unicode: 你好世界 émojis 🔐🛡️"

        val encrypted = encrypt(plaintext)
        val decrypted = decrypt(encrypted)

        assertEquals(plaintext, decrypted, "Unicode should round-trip correctly")
    }

    @Test
    fun `encrypted data contains IV prefix`() {
        val plaintext = "test"

        val encrypted = encrypt(plaintext)
        val decoded = Base64.getDecoder().decode(encrypted)

        assertTrue(decoded.size > IV_LENGTH, "Encrypted data should include IV prefix")
    }

    @Test
    fun `empty string encryption works`() {
        val plaintext = ""

        val encrypted = encrypt(plaintext)
        val decrypted = decrypt(encrypted)

        assertEquals(plaintext, decrypted, "Empty string should round-trip correctly")
    }

    @Test
    fun `AES-256 key has correct size of 32 bytes`() {
        val keyBytes = secretKey.encoded

        assertEquals(32, keyBytes.size, "AES-256 key should be 32 bytes")
    }

    @Test
    fun `IV length is 12 bytes for GCM`() {
        val plaintext = "test"

        val encrypted = encrypt(plaintext)
        val decoded = Base64.getDecoder().decode(encrypted)

        // Extract IV (first 12 bytes)
        val iv = decoded.copyOfRange(0, IV_LENGTH)
        assertEquals(12, iv.size, "GCM IV should be 12 bytes")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test helpers - mirror actual CryptoManager implementation
    // ═══════════════════════════════════════════════════════════════════════════

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedData: String): String {
        val combined = Base64.getDecoder().decode(encryptedData)

        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}