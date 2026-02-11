package com.anonforge.domain.repository

import com.anonforge.core.network.NetworkResult
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.AliasQuota
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for alias operations.
 * Defines contract for data layer implementation.
 *
 * Combines SimpleLogin API operations with local history management.
 */
interface AliasRepository {

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLELOGIN API OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new random alias via SimpleLogin API.
     */
    suspend fun createRandomAlias(): NetworkResult<AliasEmail>

    /**
     * Gets current user's alias quota from SimpleLogin.
     */
    suspend fun getQuota(): NetworkResult<AliasQuota>

    /**
     * Validates API key by attempting to fetch user info.
     */
    suspend fun validateApiKey(): NetworkResult<Boolean>

    /**
     * Gets available domain suffixes for custom alias creation.
     */
    suspend fun getAvailableSuffixes(): NetworkResult<List<String>>

    /**
     * Fetches all aliases from SimpleLogin account.
     */
    suspend fun fetchRemoteAliases(): NetworkResult<List<AliasEmail>>

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCAL HISTORY OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets recent aliases as a Flow for reactive UI updates.
     */
    fun getRecentAliasesFlow(): Flow<List<AliasEmail>>

    /**
     * Gets all aliases as a Flow for reactive UI updates.
     */
    fun getAllAliasesFlow(): Flow<List<AliasEmail>>

    /**
     * Gets recent aliases (one-shot).
     */
    suspend fun getRecentAliases(limit: Int = 10): List<AliasEmail>

    /**
     * Gets all aliases from local history.
     */
    suspend fun getAllAliases(): List<AliasEmail>

    /**
     * Gets the primary/default alias.
     */
    suspend fun getPrimaryAlias(): AliasEmail?

    /**
     * Saves an alias to local history.
     */
    suspend fun saveToHistory(
        email: String,
        tag: String = "",
        simpleLoginId: Int? = null,
        setAsPrimary: Boolean = false
    )

    /**
     * Records usage of an alias (updates lastUsedAt and useCount).
     */
    suspend fun recordAliasUsage(email: String)

    /**
     * Sets an alias as the primary/default.
     */
    suspend fun setPrimaryAlias(email: String)

    /**
     * Searches aliases by email pattern.
     */
    suspend fun searchAliases(query: String): List<AliasEmail>

    /**
     * Deletes an alias from local history.
     */
    suspend fun deleteFromHistory(email: String)

    /**
     * Clears all aliases from local history.
     */
    suspend fun clearHistory()

    /**
     * Gets the count of aliases in local history.
     */
    suspend fun getHistoryCount(): Int

    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Syncs aliases from SimpleLogin to local history.
     * @return Number of new/updated aliases
     */
    suspend fun syncAliases(): NetworkResult<Int>
}