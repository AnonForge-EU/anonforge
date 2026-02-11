package com.anonforge.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing DataStore instances as singletons.
 *
 * CRITICAL: DataStore MUST be a singleton per file to avoid
 * "multiple DataStores active for the same file" crashes.
 *
 * This module centralizes ALL DataStore creation to prevent duplicate instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val SECURITY_PREFS = "security_prefs"
    private const val SETTINGS_PREFS = "settings_prefs"
    private const val USER_PREFS = "user_preferences"
    private const val PHONE_ALIAS_PREFS = "phone_alias_prefs"

    /**
     * Provides singleton DataStore for security preferences.
     * Used by: SecurityPreferences, SecurityPreferencesRepository
     * Contains: biometric enabled, PIN hash, auto-lock settings, first unlock state
     */
    @Provides
    @Singleton
    @Named("security")
    fun provideSecurityDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SECURITY_PREFS) }
        )
    }

    /**
     * Provides singleton DataStore for app settings.
     * Used by: SettingsDataStore
     * Contains: nationality, age range, alias toggles, notifications
     */
    @Provides
    @Singleton
    @Named("settings")
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SETTINGS_PREFS) }
        )
    }

    /**
     * Provides singleton DataStore for user preferences.
     * Used by: PreferencesRepositoryImpl
     * Contains: disclaimer accepted, generation preferences, alias enabled
     */
    @Provides
    @Singleton
    @Named("user_preferences")
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(USER_PREFS) }
        )
    }

    /**
     * Provides singleton DataStore for phone alias preferences.
     * Used by: PhoneAliasRepositoryImpl
     * Contains: phone alias enabled toggle
     */
    @Provides
    @Singleton
    @Named("phone_alias")
    fun providePhoneAliasDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PHONE_ALIAS_PREFS) }
        )
    }
}