package com.anonforge.di

import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.data.repository.PreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding PreferencesRepository.
 * Used by Skill 11: Alias Email Manager
 *
 * Note: This module is used by Hilt's annotation processor at compile-time,
 * not directly in code, hence the @Suppress annotations.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository
}