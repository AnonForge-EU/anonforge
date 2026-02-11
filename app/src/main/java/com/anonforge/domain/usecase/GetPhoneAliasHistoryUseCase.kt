package com.anonforge.domain.usecase

import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.PhoneAliasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhoneAliasHistoryUseCase @Inject constructor(
    private val repository: PhoneAliasRepository
) {
    operator fun invoke(): Flow<List<PhoneAlias>> {
        return repository.getAllAliases()
    }
}
