package com.anonforge.domain.usecase

import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.repository.IdentityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIdentitiesUseCase @Inject constructor(
    private val repository: IdentityRepository
) {
    operator fun invoke(): Flow<List<DomainIdentity>> {
        return repository.getAllIdentitiesFlow()
    }
}
