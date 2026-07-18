package com.anonforge

import com.anonforge.security.encryption.ExportCryptoManager
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [ExportCryptoManager] — the REAL production class, not a mirror.
 *
 * Covers the encrypted backup/restore path:
 * - PBKDF2-HMAC-SHA256 key derivation + AES-256-GCM
 * - Output format: SALT (32 bytes) || IV (12 bytes) || CIPHERTEXT+TAG
 * - Wrong-password rejection (GCM auth tag verification)
 * - Password wipe after use
 *
 * IMPORTANT: these tests exercise the exact production constants. Any change
 * that breaks them (iterations, salt/IV length, format) would also break
 * decryption of backups created by released versions — do not "fix" the tests
 * without a versioned migration path for existing export files.
 */
class ExportCryptoManagerTest {

    private val manager = ExportCryptoManager()

    private companion object {
        const val SALT_LENGTH = 32
        const val IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BYTES = 16
    }

    @Test
    fun `encrypt and decrypt round trip`() {
        val data = "test database content".toByteArray()

        val encrypted = manager.encrypt(data, "securepassword123".toCharArray())
        val decrypted = manager.decrypt(encrypted, "securepassword123".toCharArray())

        assertContentEquals(data, decrypted, "Decrypted should match original")
    }

    @Test
    fun `encrypt produces different output for same input (unique salt and IV)`() {
        val data = "test data".toByteArray()

        val encrypted1 = manager.encrypt(data, "password".toCharArray())
        val encrypted2 = manager.encrypt(data, "password".toCharArray())

        assertNotEquals(
            encrypted1.contentToString(),
            encrypted2.contentToString(),
            "Each encryption should use a fresh salt/IV"
        )
    }

    @Test
    fun `decrypt with wrong password fails`() {
        val encrypted = manager.encrypt("test data".toByteArray(), "correct".toCharArray())

        assertFailsWith<Exception>("Wrong password should fail GCM tag verification") {
            manager.decrypt(encrypted, "wrong".toCharArray())
        }
    }

    @Test
    fun `decrypt with truncated data fails`() {
        assertFailsWith<IllegalArgumentException>("Data shorter than salt+IV must be rejected") {
            manager.decrypt(ByteArray(SALT_LENGTH + IV_LENGTH), "password".toCharArray())
        }
    }

    @Test
    fun `round trip with large payload`() {
        // Simulate a ~1MB database export
        val data = ByteArray(1_000_000) { it.toByte() }

        val encrypted = manager.encrypt(data, "password123".toCharArray())
        val decrypted = manager.decrypt(encrypted, "password123".toCharArray())

        assertContentEquals(data, decrypted, "Large data should round-trip correctly")
    }

    @Test
    fun `round trip with empty payload`() {
        val data = ByteArray(0)

        val encrypted = manager.encrypt(data, "password".toCharArray())
        val decrypted = manager.decrypt(encrypted, "password".toCharArray())

        assertContentEquals(data, decrypted, "Empty data should round-trip correctly")
    }

    @Test
    fun `round trip with special and unicode password characters`() {
        val data = "sensitive identity data".toByteArray()
        val password = "P@\$\$w0rd!#%^&*()_+-=密码🔐émoji"

        val encrypted = manager.encrypt(data, password.toCharArray())
        val decrypted = manager.decrypt(encrypted, password.toCharArray())

        assertContentEquals(data, decrypted, "Special/unicode passwords should work")
    }

    @Test
    fun `encrypted output has salt, IV and auth tag overhead`() {
        val data = "test".toByteArray()

        val encrypted = manager.encrypt(data, "password".toCharArray())

        val expectedMinSize = data.size + SALT_LENGTH + IV_LENGTH + GCM_TAG_LENGTH_BYTES
        assertTrue(
            encrypted.size >= expectedMinSize,
            "Encrypted output must include salt, IV, and auth tag overhead"
        )
    }

    @Test
    fun `password is wiped with null chars after encrypt`() {
        val password = "sensitive".toCharArray()

        manager.encrypt("test".toByteArray(), password)

        assertTrue(
            password.all { it == '\u0000' },
            "Password array must be zeroed (\\u0000) after encryption"
        )
    }

    @Test
    fun `password is wiped with null chars after decrypt`() {
        val encrypted = manager.encrypt("test".toByteArray(), "sensitive".toCharArray())
        val password = "sensitive".toCharArray()

        manager.decrypt(encrypted, password)

        assertTrue(
            password.all { it == '\u0000' },
            "Password array must be zeroed (\\u0000) after decryption"
        )
    }
}
