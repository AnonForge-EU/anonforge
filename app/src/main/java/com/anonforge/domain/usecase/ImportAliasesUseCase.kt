package com.anonforge.domain.usecase

import com.anonforge.domain.model.ImportResult
import com.anonforge.domain.model.RemoteAlias
import com.anonforge.domain.repository.AliasImportRepository
import javax.inject.Inject

/**
 * Use case for importing email aliases from SimpleLogin.
 *
 * Note: Phone alias import removed - Twilio integration abandoned.
 * Phone aliases are now manual-entry only (Skill 16).
 */
class ImportAliasesUseCase @Inject constructor(
    private val repository: AliasImportRepository
) {
    /**
     * Import email aliases from SimpleLogin.
     */
    suspend fun importEmails(aliases: List<RemoteAlias.Email>): ImportResult {
        if (aliases.isEmpty()) return ImportResult(0, 0, 0)
        return try {
            repository.importEmailAliases(aliases)
        } catch (_: Exception) {
            ImportResult(0, aliases.size, 0, listOf("Import failed"))
        }
    }

    /**
     * Import all aliases (emails only, phones removed).
     * Convenience wrapper for importEmails.
     */
    @Suppress("unused") // Public API for batch import from AliasImportDialog
    suspend fun importAll(emails: List<RemoteAlias.Email>): ImportResult {
        return importEmails(emails)
    }
}