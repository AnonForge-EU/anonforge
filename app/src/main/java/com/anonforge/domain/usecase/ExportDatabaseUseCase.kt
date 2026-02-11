package com.anonforge.domain.usecase

import android.content.Context
import android.net.Uri
import com.anonforge.security.encryption.ExportCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Exports the encrypted database to a user-specified location.
 * Applies additional PBKDF2-based encryption layer for export security.
 */
class ExportDatabaseUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val exportCrypto: ExportCryptoManager
) {
    /**
     * Exports database with password-based encryption.
     *
     * @param password User-provided export password (wiped after use)
     * @param outputUri Destination URI for the export file
     * @return Success or failure result
     */
    suspend operator fun invoke(password: CharArray, outputUri: Uri): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val dbFile = context.getDatabasePath("anonforge.db")
                require(dbFile.exists()) { "Database file not found" }

                val dbBytes = dbFile.readBytes()
                val encrypted = exportCrypto.encrypt(dbBytes, password.clone())

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    output.write(encrypted)
                }

                // Security: Wipe password from memory
                password.fill('0')

                Result.success(Unit)
            }
        } catch (e: Exception) {
            password.fill('0')
            Result.failure(e)
        }
    }
}