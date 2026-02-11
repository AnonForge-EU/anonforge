package com.anonforge.domain.repository

import com.anonforge.domain.model.PhoneAlias
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for phone alias management.
 *
 * MANUAL INPUT MODE: No API integration.
 * Users obtain numbers from external services (Hushed, OnOff, TextNow)
 * and save them here for reuse.
 */
interface PhoneAliasRepository {
    /**
     * Get all stored phone aliases as a Flow.
     */
    fun getAllAliases(): Flow<List<PhoneAlias>>

    /**
     * Get the primary phone alias (default for new identities).
     */
    suspend fun getPrimaryAlias(): PhoneAlias?

    /**
     * Insert a new phone alias.
     * @return The ID of the inserted alias
     */
    suspend fun insertAlias(alias: PhoneAlias): Result<Long>

    /**
     * Alias for insertAlias - used by PhoneAliasSettingsViewModel.
     */
    suspend fun saveAlias(alias: PhoneAlias): Result<Long> = insertAlias(alias)

    /**
     * Update an existing phone alias.
     */
    suspend fun updateAlias(alias: PhoneAlias): Result<Unit>

    /**
     * Delete a phone alias by ID.
     */
    suspend fun deleteAlias(id: Long): Result<Unit>

    /**
     * Delete all phone aliases.
     */
    suspend fun clearAllAliases(): Result<Unit>

    /**
     * Set a phone alias as primary.
     * Unsets any previous primary.
     */
    suspend fun setPrimaryAlias(id: Long): Result<Unit>

    /**
     * Record usage of a phone alias.
     * Increments usage count and updates lastUsedAt.
     */
    suspend fun recordUsage(id: Long): Result<Unit>

    /**
     * Check if phone alias feature is enabled (as Flow).
     */
    fun isEnabled(): Flow<Boolean>

    /**
     * Enable or disable phone alias feature.
     */
    suspend fun setEnabled(enabled: Boolean)

    /**
     * Get total count of aliases.
     */
    suspend fun getAliasCount(): Int
}