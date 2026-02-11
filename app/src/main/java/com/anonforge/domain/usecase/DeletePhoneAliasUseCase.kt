package com.anonforge.domain.usecase

import com.anonforge.domain.repository.PhoneAliasRepository
import javax.inject.Inject

class DeletePhoneAliasUseCase @Inject constructor(
    private val repository: PhoneAliasRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return repository.deleteAlias(id)
    }
}
