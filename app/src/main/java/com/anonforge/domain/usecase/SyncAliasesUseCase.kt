package com.anonforge.domain.usecase

import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.domain.repository.AliasRepository
import javax.inject.Inject

/**
 * Use case for syncing aliases from SimpleLogin to local history.
 *
 * Operations:
 * - Fetch remote aliases from SimpleLogin
 * - Add new aliases to local history
 * - Update enabled status of existing aliases
 * - Set primary alias if none exists
 */
class SyncAliasesUseCase @Inject constructor(
    private val repository: AliasRepository,
    private val apiKeyManager: ApiKeyManager
) {
    /**
     * Syncs aliases from SimpleLogin.
     *
     * @return NetworkResult with count of synced aliases (new + updated)
     */
    suspend operator fun invoke(): NetworkResult<Int> {
        if (!apiKeyManager.hasApiKey()) {
            return NetworkResult.Error("API key not configured")
        }
        return repository.syncAliases()
    }

    /**
     * Checks if sync is possible (API key configured).
     */
    @Suppress("unused") // Public API for AliasSettingsScreen sync button enabled state
    fun canSync(): Boolean {
        return apiKeyManager.hasApiKey()
    }
}