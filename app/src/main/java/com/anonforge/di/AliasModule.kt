package com.anonforge.di

import com.anonforge.data.repository.AliasRepositoryImpl
import com.anonforge.domain.repository.AliasRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AliasModule {
    
    @Binds
    @Singleton
    abstract fun bindAliasRepository(
        impl: AliasRepositoryImpl
    ): AliasRepository
}
