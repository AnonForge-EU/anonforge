package com.anonforge.di

import com.anonforge.data.repository.DonationRepositoryImpl
import com.anonforge.domain.repository.DonationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for donation-related dependencies.
 *
 * Provides:
 * - DonationRepository binding
 *
 * Note: CreateStripeCheckoutUseCase and DeepLinkManager
 * are constructor-injected and don't need explicit bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DonationModule {

    /**
     * Bind DonationRepository implementation.
     * Singleton scope for consistent supporter status across app.
     */
    @Binds
    @Singleton
    abstract fun bindDonationRepository(
        impl: DonationRepositoryImpl
    ): DonationRepository
}
