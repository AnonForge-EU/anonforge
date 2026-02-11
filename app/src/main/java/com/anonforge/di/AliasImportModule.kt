package com.anonforge.di

import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.remote.simplelogin.SimpleLoginApi
import com.anonforge.data.repository.AliasImportRepositoryImpl
import com.anonforge.domain.repository.AliasImportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Alias Import functionality.
 *
 * NOTE: Twilio integration has been removed.
 * Only SimpleLogin email alias import is supported.
 */
@Module
@InstallIn(SingletonComponent::class)
object AliasImportModule {

    @Provides
    @Singleton
    fun provideAliasImportRepository(
        simpleLoginApi: SimpleLoginApi,
        aliasHistoryDao: AliasHistoryDao,
        apiKeyManager: ApiKeyManager
    ): AliasImportRepository = AliasImportRepositoryImpl(
        simpleLoginApi = simpleLoginApi,
        aliasHistoryDao = aliasHistoryDao,
        apiKeyManager = apiKeyManager
    )
}