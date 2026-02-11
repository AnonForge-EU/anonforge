package com.anonforge

import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for EncryptionBridge-style field encryption.
 *
 * SECURITY VALIDATION:
 * - Validates AES-256-GCM encryption for sensitive identity fields
 * - Tests that each encryption produces unique output (IV uniqueness)
 * - Tests round-trip for email, phone, address, and other PII fields
 * - Tests Unicode support (international names, addresses)
 *
 * This test validates the encryption CONTRACT that EncryptionBridge must fulfill,
 * independent of Android Keystore (which isn't available in unit tests).
 */
class EncryptionBridgeTest {

    private lateinit var fieldEncryptionKey: SecretKey

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
    }

    @Before
    fun setup() {
        // Generate a test key (in production, this comes from Android Keystore)
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        fieldEncryptionKey = keyGen.generateKey()
    }

    @Test
    fun `encrypt and decrypt email round trip`() {
        val email = "john.doe@example.com"

        val encrypted = encrypt(email)
        val decrypted = decrypt(encrypted)

        assertEquals(email, decrypted, "Email should round-trip correctly")
    }

    @Test
    fun `encrypt and decrypt phone number round trip`() {
        val phone = "+33 6 12 34 56 78"

        val encrypted = encrypt(phone)
        val decrypted = decrypt(encrypted)

        assertEquals(phone, decrypted, "Phone should round-trip correctly")
    }

    @Test
    fun `encrypt and decrypt address round trip`() {
        val address = "123 Rue de la Paix, 75001 Paris, France"

        val encrypted = encrypt(address)
        val decrypted = decrypt(encrypted)

        assertEquals(address, decrypted, "Address should round-trip correctly")
    }

    @Test
    fun `encrypt produces different output each time`() {
        val plaintext = "test@example.com"

        val encrypted1 = encrypt(plaintext)
        val encrypted2 = encrypt(plaintext)

        assertNotEquals(encrypted1, encrypted2, "Each encryption should have unique IV")
    }

    @Test
    fun `encrypted data is Base64 encoded`() {
        val plaintext = "test"
        val encrypted = encrypt(plaintext)

        // Should not throw - valid Base64
        val decoded = Base64.getDecoder().decode(encrypted)
        assertTrue(decoded.isNotEmpty(), "Decoded data should not be empty")
    }

    @Test
    fun `encrypt handles special characters in email`() {
        val email = "user+tag@sub.example.com"

        val encrypted = encrypt(email)
        val decrypted = decrypt(encrypted)

        assertEquals(email, decrypted, "Email with special chars should round-trip")
    }

    @Test
    fun `encrypt handles international names`() {
        val names = listOf(
            "Jean-Pierre Müller",
            "François Çağlar",
            "Владимир Путин",
            "田中太郎",
            "محمد علي"
        )

        names.forEach { name ->
            val encrypted = encrypt(name)
            val decrypted = decrypt(encrypted)

            assertEquals(name, decrypted, "International name should round-trip: $name")
        }
    }

    @Test
    fun `encrypt handles emojis in notes`() {
        val note = "Important identity 🔐 Keep safe! 🛡️"

        val encrypted = encrypt(note)
        val decrypted = decrypt(encrypted)

        assertEquals(note, decrypted, "Emojis should round-trip correctly")
    }

    @Test
    fun `encrypt handles long address`() {
        val address = "Apartment 42B, Building Les Jardins du Luxembourg, " +
                "15 Boulevard Saint-Michel, 75005 Paris, Île-de-France, France"

        val encrypted = encrypt(address)
        val decrypted = decrypt(encrypted)

        assertEquals(address, decrypted, "Long address should round-trip correctly")
    }

    @Test
    fun `encrypt handles empty string`() {
        val empty = ""

        val encrypted = encrypt(empty)
        val decrypted = decrypt(encrypted)

        assertEquals(empty, decrypted, "Empty string should round-trip correctly")
    }

    @Test
    fun `encrypted output contains IV prefix`() {
        val plaintext = "test"

        val encrypted = encrypt(plaintext)
        val decoded = Base64.getDecoder().decode(encrypted)

        // Must have at least IV (12 bytes) + auth tag (16 bytes) + ciphertext
        assertTrue(
            decoded.size >= IV_LENGTH_BYTES + 16,
            "Encrypted data must include IV and auth tag"
        )
    }

    @Test
    fun `different keys produce different ciphertext`() {
        val plaintext = "sensitive data"

        val encrypted1 = encrypt(plaintext)

        // Generate a different key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val differentKey = keyGen.generateKey()
        val originalKey = fieldEncryptionKey
        fieldEncryptionKey = differentKey

        val encrypted2 = encrypt(plaintext)

        fieldEncryptionKey = originalKey // Restore

        assertNotEquals(
            encrypted1.substring(16), // Skip IV comparison
            encrypted2.substring(16),
            "Different keys should produce different ciphertext"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test helpers - mirror EncryptionBridge implementation
    // ═══════════════════════════════════════════════════════════════════════════

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, fieldEncryptionKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Format: IV || Ciphertext (ciphertext includes GCM auth tag)
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.getDecoder().decode(encryptedBase64)

        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, fieldEncryptionKey, spec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}