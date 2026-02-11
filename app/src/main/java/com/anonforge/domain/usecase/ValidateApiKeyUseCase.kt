package com.anonforge.domain.usecase

import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.domain.repository.AliasRepository
import javax.inject.Inject

/**
 * Use case for validating SimpleLogin API key.
 */
class ValidateApiKeyUseCase @Inject constructor(
    private val repository: AliasRepository,
    private val apiKeyManager: ApiKeyManager
) {
    /**
     * Validates if the stored API key is valid.
     * 
     * @return true if valid, false if invalid, or NetworkResult.Error for other issues
     */
    suspend operator fun invoke(): NetworkResult<Boolean> {
        if (!apiKeyManager.hasApiKey()) {
            return NetworkResult.Error("No API key configured")
        }
        return repository.validateApiKey()
    }
    
    /**
     * Validates and stores a new API key.
     */
    suspend fun validateAndStore(apiKey: CharArray): NetworkResult<Boolean> {
        // Store temporarily to test
        apiKeyManager.storeApiKey(apiKey)
        
        return when (val result = repository.validateApiKey()) {
            is NetworkResult.Success -> {
                if (!result.data) {
                    // Invalid key - remove it
                    apiKeyManager.clearApiKey()
                }
                result
            }
            is NetworkResult.Error -> {
                apiKeyManager.clearApiKey()
                result
            }
            else -> result
        }
    }
}
