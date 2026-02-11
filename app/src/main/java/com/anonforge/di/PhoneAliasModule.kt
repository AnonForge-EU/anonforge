package com.anonforge.di

import com.anonforge.data.repository.PhoneAliasRepositoryImpl
import com.anonforge.domain.repository.PhoneAliasRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhoneAliasModule {

    @Binds
    @Singleton
    abstract fun bindPhoneAliasRepository(
        impl: PhoneAliasRepositoryImpl
    ): PhoneAliasRepository
}
