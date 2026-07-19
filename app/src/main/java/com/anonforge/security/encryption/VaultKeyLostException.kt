package com.anonforge.security.encryption

/**
 * Thrown when the encrypted SQLCipher database already exists on disk but the
 * passphrase protecting it can no longer be retrieved (EncryptedSharedPreferences
 * lost or wiped, e.g. after a partial device restore).
 *
 * Generating a fresh passphrase in that state would make the existing vault
 * permanently unreadable (SQLCipher "file is not a database") even though the
 * file itself is intact — so [KeyManager.getDatabasePassphrase] refuses to
 * regenerate and throws this instead. Callers must surface an explicit
 * "vault unreadable" UI state rather than crash or silently reset.
 */
class VaultKeyLostException(message: String) : IllegalStateException(message)
