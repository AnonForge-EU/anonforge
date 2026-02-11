package com.anonforge.di

import android.content.Context
import com.anonforge.data.local.datasource.AddressDataProvider
import com.anonforge.data.local.datasource.NameDataProvider
import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.feature.generator.DateOfBirthGenerator
import com.anonforge.generator.AddressGenerator
import com.anonforge.generator.NameGenerator
import com.anonforge.generator.PhoneGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton

/**
 * Hilt module providing generator dependencies.
 * Note: SecureRandom is provided by AppModule, not duplicated here.
 *
 * Providers are used by Hilt DI at runtime - IDE warnings are false positives.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorModule {

    @Provides
    @Singleton
    fun provideNameDataProvider(
        @ApplicationContext context: Context
    ): NameDataProvider = NameDataProvider(context)

    @Provides
    @Singleton
    fun provideAddressDataProvider(
        @ApplicationContext context: Context
    ): AddressDataProvider = AddressDataProvider(context)

    @Provides
    @Singleton
    fun provideNameGenerator(
        secureRandom: SecureRandom,
        nameDataProvider: NameDataProvider
    ): NameGenerator = NameGenerator(secureRandom, nameDataProvider)

    @Provides
    @Singleton
    fun provideAddressGenerator(
        secureRandom: SecureRandom,
        addressDataProvider: AddressDataProvider
    ): AddressGenerator = AddressGenerator(secureRandom, addressDataProvider)

    @Provides
    @Singleton
    fun providePhoneGenerator(
        secureRandom: SecureRandom
    ): PhoneGenerator = PhoneGenerator(secureRandom)

    @Provides
    @Singleton
    fun provideDateOfBirthGenerator(
        secureRandom: SecureRandom,
        preferencesRepository: PreferencesRepository
    ): DateOfBirthGenerator = DateOfBirthGenerator(secureRandom, preferencesRepository)
}