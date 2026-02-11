package com.anonforge.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for Support feature dependencies.
 *
 * Note: DonationRepository binding is in DonationModule.
 * DeepLinkManager is automatically provided as @Singleton via its @Inject constructor.
 *
 * This module is kept for future Support-specific dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SupportModule {
    // DonationRepository binding removed - already provided by DonationModule
    // DeepLinkManager is auto-provided via @Inject constructor
}