package com.anonforge.data.repository

import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.local.entity.AliasHistoryEntity
import com.anonforge.data.remote.simplelogin.SimpleLoginApi
import com.anonforge.data.remote.simplelogin.dto.AliasDetailDto
import com.anonforge.domain.model.FetchAliasesResult
import com.anonforge.domain.model.ImportResult
import com.anonforge.domain.model.RemoteAlias
import com.anonforge.domain.repository.AliasImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for importing aliases from SimpleLogin.
 *
 * NOTE: Phone/Twilio functionality has been removed from AnonForge.
 *
 * SECURITY:
 * - API key injected via SimpleLoginInterceptor
 * - No sensitive data in logs
 */
@Singleton
class AliasImportRepositoryImpl @Inject constructor(
    private val simpleLoginApi: SimpleLoginApi,
    private val aliasHistoryDao: AliasHistoryDao,
    private val apiKeyManager: ApiKeyManager
) : AliasImportRepository {

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL ALIASES (SimpleLogin)
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun fetchRemoteEmailAliases(pageId: Int?): FetchAliasesResult = withContext(Dispatchers.IO) {
        // Check if API key is configured
        if (!apiKeyManager.hasApiKey()) {
            return@withContext FetchAliasesResult.NotConfigured
        }

        try {
            // API key is injected by SimpleLoginInterceptor
            val response = simpleLoginApi.getAliases(pageId = pageId ?: 0)

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null || body.aliases.isEmpty()) {
                    return@withContext FetchAliasesResult.Empty
                }

                FetchAliasesResult.Success(
                    aliases = body.aliases.map { it.toDomain() },
                    totalCount = body.total ?: body.aliases.size,
                    hasMore = body.pageId != null
                )
            } else {
                handleError(response.code(), response.message())
            }
        } catch (_: Exception) {
            FetchAliasesResult.Error("Network error", false)
        }
    }

    override suspend fun importEmailAliases(aliases: List<RemoteAlias.Email>): ImportResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var duplicateCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        val now = Clock.System.now().toEpochMilliseconds()

        for (alias in aliases) {
            try {
                // Check for existing alias
                if (aliasHistoryDao.getByEmail(alias.email) != null) {
                    duplicateCount++
                    continue
                }

                // Insert new alias
                aliasHistoryDao.insert(
                    AliasHistoryEntity(
                        email = alias.email,
                        createdAt = alias.createdAt?.toEpochMilliseconds() ?: now,
                        lastUsedAt = now,
                        useCount = 0
                    )
                )
                successCount++
            } catch (_: Exception) {
                failureCount++
                errors.add("Failed: ${alias.maskedDisplay}")
            }
        }

        ImportResult(successCount, failureCount, duplicateCount, errors)
    }

    override suspend fun isEmailServiceConfigured(): Boolean = apiKeyManager.hasApiKey()

    override suspend fun emailAliasExists(email: String): Boolean =
        aliasHistoryDao.getByEmail(email) != null

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun AliasDetailDto.toDomain() = RemoteAlias.Email(
        id = id.toString(),
        email = email,
        note = note,
        createdAt = creationTimestamp?.let { Instant.fromEpochSeconds(it) },
        isEnabled = enabled,
        forwardCount = nbForward,
        blockCount = nbBlock
    )

    private fun handleError(code: Int, message: String?): FetchAliasesResult = when (code) {
        429 -> FetchAliasesResult.Error("Rate limited", true, 60)
        401, 403 -> FetchAliasesResult.Error("Authentication failed", false)
        else -> FetchAliasesResult.Error(message ?: "Unknown error", false)
    }
}