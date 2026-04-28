package com.anonforge.data.repository

import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.local.dao.getDefaultAlias
import com.anonforge.data.local.dao.recordUsage
import com.anonforge.data.local.dao.setPrimaryAlias
import com.anonforge.data.local.dao.upsertAlias
import com.anonforge.data.local.entity.AliasHistoryEntity
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.data.remote.simplelogin.SimpleLoginApi
import com.anonforge.data.remote.simplelogin.dto.UpdateAliasRequest
import com.anonforge.domain.model.AliasDetails
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.AliasQuota
import com.anonforge.domain.repository.AliasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AliasRepository.
 * Extended for Skill 17: Real Alias Integration.
 *
 * Handles both SimpleLogin API calls and local history management.
 *
 * SECURITY:
 * - No sensitive data logging
 * - API key managed by SimpleLoginInterceptor
 * - Local data in SQLCipher encrypted DB
 */
@Singleton
class AliasRepositoryImpl @Inject constructor(
    private val api: SimpleLoginApi,
    private val aliasHistoryDao: AliasHistoryDao,
    private val apiKeyManager: ApiKeyManager,
    private val settingsDataStore: SettingsDataStore
) : AliasRepository {

    private companion object {
        // Hard cap to avoid unbounded paging if the server misbehaves.
        const val MAX_ALIAS_PAGES = 50
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLELOGIN API OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun createRandomAlias(): NetworkResult<AliasEmail> {
        return try {
            val mode = settingsDataStore.aliasMode.first()
            val response = api.createRandomAlias(mode = mode)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val alias = AliasEmail(
                        id = dto.id,
                        email = dto.email,
                        createdAt = dto.creationTimestamp,
                        isEnabled = dto.enabled
                    )

                    // Auto-save to history
                    val isFirstAlias = aliasHistoryDao.getCount() == 0
                    saveToHistory(
                        email = alias.email,
                        simpleLoginId = alias.id,
                        setAsPrimary = isFirstAlias
                    )

                    NetworkResult.Success(alias)
                } else {
                    NetworkResult.Error("Empty response body")
                }
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Invalid API key"
                    429 -> "Rate limit exceeded"
                    else -> response.message() ?: "HTTP ${response.code()}"
                }
                NetworkResult.Error(errorMsg, response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getQuota(): NetworkResult<AliasQuota> {
        return try {
            val response = api.getUserInfo()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    NetworkResult.Success(
                        AliasQuota(
                            isPremium = dto.isPremium || dto.inTrial,
                            totalAllowed = dto.maxAliasFreeAccount,
                            used = dto.aliasCount,
                            remaining = dto.remainingAliases
                        )
                    )
                } else {
                    NetworkResult.Error("Empty response body")
                }
            } else {
                NetworkResult.Error(response.message() ?: "HTTP ${response.code()}", response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun validateApiKey(): NetworkResult<Boolean> {
        return try {
            val response = api.getUserInfo()
            when {
                response.isSuccessful -> NetworkResult.Success(true)
                response.code() == 401 -> NetworkResult.Success(false)
                else -> NetworkResult.Error(response.message() ?: "HTTP ${response.code()}", response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getAvailableSuffixes(): NetworkResult<List<String>> {
        return try {
            val response = api.getAliasOptions()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    NetworkResult.Success(
                        dto.suffixes
                            .filter { !it.isPremium }
                            .map { it.suffix }
                    )
                } else {
                    NetworkResult.Error("Empty response body")
                }
            } else {
                NetworkResult.Error(response.message() ?: "HTTP ${response.code()}", response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun fetchRemoteAliases(): NetworkResult<List<AliasEmail>> {
        return try {
            val collected = mutableListOf<AliasEmail>()
            var page = 0
            while (page < MAX_ALIAS_PAGES) {
                val response = api.getAliases(pageId = page)
                if (!response.isSuccessful) {
                    return NetworkResult.Error(
                        response.message() ?: "HTTP ${response.code()}",
                        response.code()
                    )
                }
                val body = response.body() ?: return NetworkResult.Error("Empty response body")
                if (body.aliases.isEmpty()) break

                body.aliases.forEach { aliasDto ->
                    collected += AliasEmail(
                        id = aliasDto.id,
                        email = aliasDto.email,
                        createdAt = aliasDto.creationTimestamp ?: System.currentTimeMillis(),
                        isEnabled = aliasDto.enabled
                    )
                }
                // SimpleLogin returns 20 per page; if we got fewer, this is the last one.
                if (body.aliases.size < 20) break
                page += 1
            }
            NetworkResult.Success(collected)
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun deleteRemoteAlias(simpleLoginId: Int): NetworkResult<Unit> {
        return try {
            val response = api.deleteAlias(simpleLoginId)
            if (response.isSuccessful || response.code() == 404) {
                // 404 = already gone on server, treat as success and clean up locally.
                aliasHistoryDao.deleteBySimpleLoginId(simpleLoginId)
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(httpErrorMessage(response.code(), response.message()), response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun toggleRemoteAlias(simpleLoginId: Int): NetworkResult<Boolean> {
        return try {
            val response = api.toggleAlias(simpleLoginId)
            if (response.isSuccessful) {
                val newEnabled = response.body()?.enabled ?: return NetworkResult.Error("Empty response body")
                aliasHistoryDao.setEnabledBySimpleLoginId(simpleLoginId, newEnabled)
                NetworkResult.Success(newEnabled)
            } else {
                NetworkResult.Error(httpErrorMessage(response.code(), response.message()), response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getAliasDetails(simpleLoginId: Int): NetworkResult<AliasDetails> {
        return try {
            val response = api.getAlias(simpleLoginId)
            if (response.isSuccessful) {
                val dto = response.body() ?: return NetworkResult.Error("Empty response body")
                NetworkResult.Success(
                    AliasDetails(
                        id = dto.id,
                        email = dto.email,
                        isEnabled = dto.enabled,
                        name = dto.name,
                        note = dto.note,
                        createdAt = dto.creationTimestamp,
                        forwardCount = dto.nbForward,
                        blockCount = dto.nbBlock
                    )
                )
            } else {
                NetworkResult.Error(httpErrorMessage(response.code(), response.message()), response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun updateRemoteAlias(
        simpleLoginId: Int,
        note: String?,
        name: String?
    ): NetworkResult<Unit> {
        if (note == null && name == null) return NetworkResult.Success(Unit)
        return try {
            val response = api.updateAlias(
                simpleLoginId,
                UpdateAliasRequest(note = note, name = name)
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(httpErrorMessage(response.code(), response.message()), response.code())
            }
        } catch (_: java.net.UnknownHostException) {
            NetworkResult.Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getInstanceUrl(): String = apiKeyManager.getInstanceUrl()

    override suspend fun setInstanceUrl(url: String?): Boolean = apiKeyManager.setInstanceUrl(url)

    private fun httpErrorMessage(code: Int, fallback: String?): String = when (code) {
        401 -> "Invalid API key"
        403 -> "Access forbidden"
        404 -> "Alias not found"
        429 -> "Rate limit exceeded"
        in 500..599 -> "Server error ($code)"
        else -> fallback ?: "HTTP $code"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCAL HISTORY OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getRecentAliasesFlow(): Flow<List<AliasEmail>> {
        return aliasHistoryDao.getRecentAliases().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAliasesFlow(): Flow<List<AliasEmail>> {
        return aliasHistoryDao.getAllAliasesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRecentAliases(limit: Int): List<AliasEmail> {
        return aliasHistoryDao.getRecentAliasesList(limit).map { it.toDomain() }
    }

    override suspend fun getAllAliases(): List<AliasEmail> {
        return aliasHistoryDao.getAllAliasesList().map { it.toDomain() }
    }

    override suspend fun getPrimaryAlias(): AliasEmail? {
        return aliasHistoryDao.getDefaultAlias()?.toDomain()
    }

    override suspend fun saveToHistory(
        email: String,
        tag: String,
        simpleLoginId: Int?,
        setAsPrimary: Boolean
    ) {
        aliasHistoryDao.upsertAlias(
            email = email,
            tag = tag,
            simpleLoginId = simpleLoginId,
            enabled = true,
            setAsPrimary = setAsPrimary
        )
    }

    override suspend fun recordAliasUsage(email: String) {
        aliasHistoryDao.recordUsage(email)
    }

    override suspend fun setPrimaryAlias(email: String) {
        val alias = aliasHistoryDao.findByEmail(email) ?: return
        aliasHistoryDao.setPrimaryAlias(alias.id)
    }

    override suspend fun searchAliases(query: String): List<AliasEmail> {
        return aliasHistoryDao.searchAliases(query).map { it.toDomain() }
    }

    override suspend fun deleteFromHistory(email: String) {
        aliasHistoryDao.deleteByEmail(email)
    }

    override suspend fun clearHistory() {
        aliasHistoryDao.deleteAll()
    }

    override suspend fun getHistoryCount(): Int {
        return aliasHistoryDao.getCount()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun syncAliases(): NetworkResult<Int> {
        return when (val remoteResult = fetchRemoteAliases()) {
            is NetworkResult.Success -> {
                val remoteAliases = remoteResult.data
                var syncCount = 0

                for (alias in remoteAliases) {
                    val existing = aliasHistoryDao.getBySimpleLoginId(alias.id)

                    if (existing == null) {
                        // New alias from SimpleLogin - add to history
                        aliasHistoryDao.upsertAlias(
                            email = alias.email,
                            simpleLoginId = alias.id,
                            enabled = alias.isEnabled,
                            setAsPrimary = false
                        )
                        syncCount++
                    } else {
                        // Update enabled status if changed
                        if (existing.enabled != alias.isEnabled) {
                            aliasHistoryDao.setEnabled(alias.email, alias.isEnabled)
                            syncCount++
                        }
                    }
                }

                // Ensure we have a primary alias
                if (aliasHistoryDao.getPrimaryAlias() == null && aliasHistoryDao.getCount() > 0) {
                    aliasHistoryDao.getFirstAlias()?.let {
                        aliasHistoryDao.setPrimaryAlias(it.id)
                    }
                }

                NetworkResult.Success(syncCount)
            }
            is NetworkResult.Error -> remoteResult
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Maps entity to domain model.
     */
    private fun AliasHistoryEntity.toDomain(): AliasEmail {
        return AliasEmail(
            id = simpleLoginId ?: id.toInt(),
            email = email,
            createdAt = createdAt,
            isEnabled = enabled,
            simpleLoginId = simpleLoginId
        )
    }
}