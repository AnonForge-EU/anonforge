package com.anonforge.domain.usecase

import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.PhoneAliasRepository
import javax.inject.Inject

class GetPrimaryPhoneAliasUseCase @Inject constructor(
    private val repository: PhoneAliasRepository
) {
    suspend operator fun invoke(): PhoneAlias? {
        return repository.getPrimaryAlias()
    }
}
