package com.anonforge.di

import android.content.Context
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.security.encryption.EncryptionBridge
import com.anonforge.security.encryption.ExportCryptoManager
import com.anonforge.security.encryption.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies.
 *
 * All security components are singletons to ensure:
 * - Consistent encryption keys across the app
 * - Proper Keystore key lifecycle management
 * - No duplicate instances causing key conflicts
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeyManager(
        @ApplicationContext context: Context
    ): KeyManager = KeyManager(context)

    @Provides
    @Singleton
    fun provideEncryptionBridge(keyManager: KeyManager): EncryptionBridge =
        EncryptionBridge(keyManager)

    @Provides
    @Singleton
    fun provideExportCryptoManager(): ExportCryptoManager =
        ExportCryptoManager()

    /**
     * Provides ApiKeyManager for SimpleLogin API key storage.
     *
     * SECURITY:
     * - Uses Android Keystore for AES-256-GCM encryption
     * - Double encryption with EncryptedSharedPreferences
     * - Memory wipe after key operations
     * - Key is NEVER exposed in plaintext
     */
    @Provides
    @Singleton
    fun provideApiKeyManager(
        @ApplicationContext context: Context
    ): ApiKeyManager = ApiKeyManager(context)
}