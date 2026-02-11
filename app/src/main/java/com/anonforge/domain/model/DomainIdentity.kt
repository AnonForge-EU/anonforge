package com.anonforge.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Pure domain model for a generated identity.
 * Zero Android dependencies - only kotlinx.datetime.
 *
 * OWASP M1: Encrypted at data layer, never stored in plaintext.
 *
 * @property email NULLABLE - only present when SimpleLogin alias is configured
 * @property customName User-defined name for easy identification (Skill 13 - Renaming)
 * @property nationality Country of origin for the identity (affects name/phone/address format)
 */
data class DomainIdentity(
    val id: String,
    val fullName: FullName,
    val email: Email?,
    val phone: Phone,
    val address: Address?,
    val dateOfBirth: DateOfBirth,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val gender: Gender,
    val nationality: Nationality = Nationality.DEFAULT,
    val customName: String? = null
) {
    /**
     * Check if identity has expired.
     */
    val isExpired: Boolean
        get() = expiresAt?.let { it < Clock.System.now() } ?: false

    /**
     * Check if identity will expire within the given duration.
     * Used for "expires soon" UI indicators.
     *
     * @param withinMillis Milliseconds threshold (default: 24 hours)
     * @return true if expires within threshold, false otherwise
     */
    @Suppress("unused") // Public API for expiry notifications
    fun expiresSoon(withinMillis: Long = 24 * 60 * 60 * 1000): Boolean {
        return expiresAt?.let {
            val now = Clock.System.now()
            it > now && (it.toEpochMilliseconds() - now.toEpochMilliseconds()) < withinMillis
        } ?: false
    }
}
