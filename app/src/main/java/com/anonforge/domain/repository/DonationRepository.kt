package com.anonforge.domain.repository

/**
 * Repository interface for donation-related data.
 *
 * This repository handles:
 * - Supporter status persistence
 * - Donation history (optional, for future use)
 *
 * PRIVACY: No personal data is stored. Only a boolean flag
 * indicating if user has ever donated (for supporter badge).
 */
interface DonationRepository {

    /**
     * Check if user has ever donated.
     * Used to show supporter badge.
     *
     * @return true if user is a supporter
     */
    suspend fun isSupporter(): Boolean

    /**
     * Mark user as a supporter after successful donation.
     * This persists locally (encrypted).
     */
    suspend fun markAsSupporter()

    /**
     * Clear supporter status.
     * Useful for testing or if user requests data deletion.
     */
    suspend fun clearSupporterStatus()

    /**
     * Get the last donation timestamp (if tracked).
     * Returns null if no donation recorded.
     */
    suspend fun getLastDonationTimestamp(): Long?
}
