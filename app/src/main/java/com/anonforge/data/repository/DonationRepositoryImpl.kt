package com.anonforge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anonforge.domain.repository.DonationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance for donation preferences
private val Context.donationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "donation_prefs"
)

/**
 * Implementation of DonationRepository using DataStore for supporter status.
 *
 * SECURITY:
 * - Uses Android DataStore (encrypted at rest on devices with encryption)
 * - Only stores boolean flag and timestamp
 * - No personal or payment data ever stored
 *
 * PRIVACY:
 * - Supporter status is local only
 * - Never synced to any server
 * - Can be cleared by user via app data deletion
 *
 * NOTE: Stripe Payment Link URLs are managed in StripePaymentLinks.kt (core/stripe).
 * This repository only handles local supporter status persistence.
 */
@Singleton
class DonationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DonationRepository {

    private object PrefsKeys {
        val IS_SUPPORTER = booleanPreferencesKey("is_supporter")
        val LAST_DONATION_TIMESTAMP = longPreferencesKey("last_donation_timestamp")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Supporter Status
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun isSupporter(): Boolean {
        return context.donationDataStore.data
            .map { prefs -> prefs[PrefsKeys.IS_SUPPORTER] ?: false }
            .first()
    }

    override suspend fun markAsSupporter() {
        context.donationDataStore.edit { prefs ->
            prefs[PrefsKeys.IS_SUPPORTER] = true
            prefs[PrefsKeys.LAST_DONATION_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    override suspend fun clearSupporterStatus() {
        context.donationDataStore.edit { prefs ->
            prefs.remove(PrefsKeys.IS_SUPPORTER)
            prefs.remove(PrefsKeys.LAST_DONATION_TIMESTAMP)
        }
    }

    override suspend fun getLastDonationTimestamp(): Long? {
        return context.donationDataStore.data
            .map { prefs -> prefs[PrefsKeys.LAST_DONATION_TIMESTAMP] }
            .first()
    }
}