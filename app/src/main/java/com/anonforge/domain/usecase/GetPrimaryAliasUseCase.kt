package com.anonforge.domain.usecase

import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.repository.AliasRepository
import javax.inject.Inject

/**
 * Use case for getting/setting the primary alias for auto-reuse.
 *
 * Primary alias logic:
 * 1. If user has explicitly set a primary → return it
 * 2. Otherwise → return the first/oldest alias
 * 3. If no aliases exist → return null
 */
class GetPrimaryAliasUseCase @Inject constructor(
    private val repository: AliasRepository
) {
    /**
     * Gets the primary/default alias.
     * Returns primary if set, otherwise first alias.
     *
     * @return Primary alias or null if no aliases exist
     */
    suspend operator fun invoke(): AliasEmail? {
        return repository.getPrimaryAlias()
    }

    /**
     * Sets an alias as the primary.
     *
     * @param email The email to set as primary
     */
    @Suppress("unused") // Public API for AliasHistoryScreen set primary action
    suspend fun setPrimary(email: String) {
        repository.setPrimaryAlias(email)
    }

    /**
     * Checks if a primary alias exists.
     */
    @Suppress("unused") // Public API for Generator auto-select logic
    suspend fun hasPrimary(): Boolean {
        return repository.getPrimaryAlias() != null
    }

    /**
     * Gets primary alias email string, or null.
     */
    @Suppress("unused") // Public API for Generator email pre-fill
    suspend fun getPrimaryEmail(): String? {
        return repository.getPrimaryAlias()?.email
    }
}