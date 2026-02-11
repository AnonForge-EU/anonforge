package com.anonforge.domain.usecase

import com.anonforge.core.network.NetworkResult
import com.anonforge.domain.model.AliasQuota
import com.anonforge.domain.repository.AliasRepository
import javax.inject.Inject

/**
 * Use case for checking alias quota.
 */
class CheckAliasQuotaUseCase @Inject constructor(
    private val repository: AliasRepository
) {
    suspend operator fun invoke(): NetworkResult<AliasQuota> {
        return repository.getQuota()
    }
}
