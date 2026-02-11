package com.anonforge.domain.usecase

import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.repository.AliasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving alias history from local storage.
 * 
 * Provides access to:
 * - Recent aliases (Flow & suspend)
 * - All aliases (Flow & suspend)
 * - Search functionality
 * - CRUD operations
 */
class GetAliasHistoryUseCase @Inject constructor(
    private val repository: AliasRepository
) {
    /**
     * Gets recent aliases as Flow for reactive UI.
     */
    fun getRecentFlow(): Flow<List<AliasEmail>> {
        return repository.getRecentAliasesFlow()
    }

    /**
     * Gets all aliases as Flow for reactive UI.
     */
    fun getAllFlow(): Flow<List<AliasEmail>> {
        return repository.getAllAliasesFlow()
    }

    /**
     * Gets recent aliases as list.
     * @param limit Maximum number to return (default 10)
     */
    suspend fun getRecent(limit: Int = 10): List<AliasEmail> {
        return repository.getRecentAliases(limit)
    }

    /**
     * Gets all aliases from history.
     */
    suspend fun getAll(): List<AliasEmail> {
        return repository.getAllAliases()
    }

    /**
     * Searches aliases by email or tag.
     * @param query Search term
     */
    suspend fun search(query: String): List<AliasEmail> {
        if (query.isBlank()) return getRecent()
        return repository.searchAliases(query)
    }

    /**
     * Gets count of aliases in history.
     */
    suspend fun getCount(): Int {
        return repository.getHistoryCount()
    }

    /**
     * Checks if history is empty.
     */
    suspend fun isEmpty(): Boolean {
        return repository.getHistoryCount() == 0
    }

    /**
     * Records usage of an alias (updates lastUsedAt, useCount).
     */
    suspend fun recordUsage(email: String) {
        repository.recordAliasUsage(email)
    }

    /**
     * Deletes an alias from history.
     */
    suspend fun delete(email: String) {
        repository.deleteFromHistory(email)
    }

    /**
     * Clears all history.
     */
    suspend fun clearAll() {
        repository.clearHistory()
    }
}
