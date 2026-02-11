package com.anonforge.di

import android.content.Context
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.local.dao.PhoneAliasDao
import com.anonforge.data.local.db.AnonForgeDatabase
import com.anonforge.data.local.db.IdentityDao
import com.anonforge.security.encryption.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: KeyManager
    ): AnonForgeDatabase {
        return AnonForgeDatabase.create(context, keyManager)
    }

    @Provides
    @Singleton
    fun provideIdentityDao(database: AnonForgeDatabase): IdentityDao {
        return database.identityDao()
    }

    @Provides
    @Singleton
    fun provideAliasHistoryDao(database: AnonForgeDatabase): AliasHistoryDao {
        return database.aliasHistoryDao()
    }

    // Phone Alias DAO for manual phone number storage
    @Provides
    @Singleton
    fun providePhoneAliasDao(database: AnonForgeDatabase): PhoneAliasDao {
        return database.phoneAliasDao()
    }
}