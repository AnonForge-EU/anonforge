package com.anonforge.domain.usecase

import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.AliasQuota
import com.anonforge.domain.repository.AliasRepository
import javax.inject.Inject

/**
 * Use case for creating new email aliases via SimpleLogin.
 *
 * Responsibilities:
 * - Check API key configuration
 * - Verify quota before creation
 * - Create alias via API
 * - Handle errors gracefully
 *
 * SECURITY: No sensitive data logged.
 */
class CreateAliasUseCase @Inject constructor(
    private val repository: AliasRepository,
    private val apiKeyManager: ApiKeyManager
) {
    /**
     * Creates a new random alias.
     *
     * @return NetworkResult containing created AliasEmail or error
     */
    suspend operator fun invoke(): NetworkResult<AliasEmail> {
        if (!apiKeyManager.hasApiKey()) {
            return NetworkResult.Error("API key not configured")
        }
        return repository.createRandomAlias()
    }

    /**
     * Creates alias with pre-creation quota check.
     * Returns warning message if quota is low (< 2 remaining).
     *
     * Used by GeneratorViewModel for better UX - warns user before
     * they hit their SimpleLogin alias limit.
     *
     * @return Pair of (AliasEmail result, warning message if any)
     */
    suspend fun createWithQuotaCheck(): Pair<NetworkResult<AliasEmail>, String?> {
        if (!apiKeyManager.hasApiKey()) {
            return Pair(NetworkResult.Error("API key not configured"), null)
        }

        // Check quota before creation
        val quotaWarning = when (val quotaResult = repository.getQuota()) {
            is NetworkResult.Success -> {
                val quota = quotaResult.data
                when {
                    quota.isAtLimit -> {
                        // Cannot create - at limit
                        return Pair(
                            NetworkResult.Error("Alias limit reached. Upgrade to premium or delete old aliases."),
                            null
                        )
                    }
                    quota.isLowQuota -> {
                        // Warn but proceed
                        quota.warningMessage
                    }
                    else -> null
                }
            }
            is NetworkResult.Error -> {
                // Offline - proceed without quota check
                null
            }
            else -> null
        }

        // Create alias
        val result = repository.createRandomAlias()
        return Pair(result, quotaWarning)
    }

    /**
     * Check if alias creation is possible.
     * @return true if API key configured and quota available
     */
    @Suppress("unused") // Public API for pre-creation validation in Generator
    suspend fun canCreateAlias(): Boolean {
        if (!apiKeyManager.hasApiKey()) return false

        return when (val quotaResult = repository.getQuota()) {
            is NetworkResult.Success -> !quotaResult.data.isAtLimit
            else -> true // Assume possible if offline
        }
    }

    /**
     * Gets current quota info.
     */
    @Suppress("unused") // Public API for quota display in AliasSettings
    suspend fun getQuota(): NetworkResult<AliasQuota> {
        if (!apiKeyManager.hasApiKey()) {
            return NetworkResult.Error("API key not configured")
        }
        return repository.getQuota()
    }
}