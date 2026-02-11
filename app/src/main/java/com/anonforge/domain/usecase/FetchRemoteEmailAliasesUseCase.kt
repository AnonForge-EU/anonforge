package com.anonforge.domain.usecase

import com.anonforge.domain.model.FetchAliasesResult
import com.anonforge.domain.repository.AliasImportRepository
import javax.inject.Inject

class FetchRemoteEmailAliasesUseCase @Inject constructor(
    private val repository: AliasImportRepository
) {
    suspend operator fun invoke(pageId: Int? = null): FetchAliasesResult {
        if (!repository.isEmailServiceConfigured()) {
            return FetchAliasesResult.NotConfigured
        }
        return try {
            repository.fetchRemoteEmailAliases(pageId)
        } catch (_: Exception) {
            FetchAliasesResult.Error("Failed to fetch email aliases", false)
        }
    }
}