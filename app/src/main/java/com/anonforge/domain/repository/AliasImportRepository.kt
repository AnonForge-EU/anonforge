package com.anonforge.domain.repository

import com.anonforge.domain.model.FetchAliasesResult
import com.anonforge.domain.model.ImportResult
import com.anonforge.domain.model.RemoteAlias

/**
 * Repository for importing email aliases from SimpleLogin.
 *
 * NOTE: Phone/Twilio functionality has been removed from AnonForge.
 */
interface AliasImportRepository {
    suspend fun fetchRemoteEmailAliases(pageId: Int? = null): FetchAliasesResult
    suspend fun importEmailAliases(aliases: List<RemoteAlias.Email>): ImportResult
    suspend fun isEmailServiceConfigured(): Boolean
    suspend fun emailAliasExists(email: String): Boolean
}