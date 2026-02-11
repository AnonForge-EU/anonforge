package com.anonforge.domain.usecase

import android.content.Context
import android.net.Uri
import com.anonforge.security.encryption.ExportCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Imports an encrypted database backup from a user-specified location.
 * Decrypts using user-provided password and replaces current database.
 */
class ImportDatabaseUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val exportCrypto: ExportCryptoManager
) {
    /**
     * Imports and decrypts database from backup file.
     *
     * @param password User-provided export password (wiped after use)
     * @param inputUri Source URI of the backup file
     * @return Success or failure result
     */
    suspend operator fun invoke(password: CharArray, inputUri: Uri): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val encrypted = context.contentResolver.openInputStream(inputUri)?.use { input ->
                    input.readBytes()
                } ?: throw IllegalStateException("Cannot read file")

                val decrypted = exportCrypto.decrypt(encrypted, password.clone())

                val dbFile = context.getDatabasePath("anonforge.db")
                dbFile.writeBytes(decrypted)

                // Security: Wipe sensitive data from memory
                password.fill('0')
                decrypted.fill(0)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            password.fill('0')
            Result.failure(e)
        }
    }
}