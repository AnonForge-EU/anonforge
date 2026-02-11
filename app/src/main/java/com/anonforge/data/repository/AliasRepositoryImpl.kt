package com.anonforge.data.repository

import com.anonforge.core.network.NetworkResult
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.local.dao.getDefaultAlias
import com.anonforge.data.local.dao.recordUsage
import com.anonforge.data.local.dao.setPrimaryAlias
import com.anonforge.data.local.dao.upsertAlias
import com.anonforge.data.local.entity.AliasHistoryEntity
import com.anonforge.data.remote.simplelogin.SimpleLoginApi
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.AliasQuota
import com.anonforge.domain.repository.AliasRepository
import kotlinx.coroutines.flow.Flow
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
    private val aliasHistoryDao: AliasHistoryDao
) : AliasRepository {

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLELOGIN API OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun createRandomAlias(): NetworkResult<AliasEmail> {
        return try {
            val response = api.createRandomAlias()
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
            // API key injected by SimpleLoginInterceptor
            val response = api.getAliases(pageId = 0)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val aliases = dto.aliases.map { aliasDto ->
                        AliasEmail(
                            id = aliasDto.id,
                            email = aliasDto.email,
                            createdAt = aliasDto.creationTimestamp ?: System.currentTimeMillis(),
                            isEnabled = aliasDto.enabled
                        )
                    }
                    NetworkResult.Success(aliases)
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
            isEnabled = enabled
        )
    }
}