package com.anonforge.domain.usecase

import com.anonforge.domain.repository.IdentityRepository
import javax.inject.Inject

class DeleteIdentityUseCase @Inject constructor(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteIdentity(id)
    }
}
