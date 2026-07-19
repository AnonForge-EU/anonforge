package com.anonforge

import com.anonforge.security.encryption.KeyManager
import com.anonforge.security.encryption.VaultKeyLostException
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [KeyManager.ensureRegenerationIsSafe] — the guard that prevents
 * silently generating a fresh database passphrase when the SQLCipher database
 * already holds data (lost EncryptedSharedPreferences, e.g. partial restore).
 *
 * Without the guard, a new passphrase would leave the existing vault
 * permanently unreadable ("file is not a database") even though the file is
 * intact. The guard must throw [VaultKeyLostException] in that case, while
 * still allowing the legitimate first-launch generation (no database yet).
 */
class KeyManagerRegenerationGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `refuses to regenerate when database file already holds data`() {
        val dbFile = tempFolder.newFile("anonforge.db").apply {
            writeBytes(ByteArray(1024) { 0x42 })
        }

        val exception = assertFailsWith<VaultKeyLostException> {
            KeyManager.ensureRegenerationIsSafe(dbFile)
        }
        assertTrue(
            exception.message.orEmpty().contains("anonforge.db"),
            "Exception message should name the database file: ${exception.message}"
        )
    }

    @Test
    fun `allows regeneration on first launch when no database file exists`() {
        val missingDbFile = File(tempFolder.root, "anonforge.db")

        // Must not throw: generating the very first passphrase is legitimate
        KeyManager.ensureRegenerationIsSafe(missingDbFile)
    }

    @Test
    fun `allows regeneration when database file exists but is empty`() {
        val emptyDbFile = tempFolder.newFile("anonforge.db")

        // 0 bytes = no user data at stake
        KeyManager.ensureRegenerationIsSafe(emptyDbFile)
    }

    @Test
    fun `databaseFileHasData only true for a non-empty file`() {
        val missing = File(tempFolder.root, "missing.db")
        val empty = tempFolder.newFile("empty.db")
        val withData = tempFolder.newFile("data.db").apply { writeBytes(byteArrayOf(1)) }

        assertFalse(KeyManager.databaseFileHasData(missing))
        assertFalse(KeyManager.databaseFileHasData(empty))
        assertTrue(KeyManager.databaseFileHasData(withData))
    }
}
