package com.anonforge.generator

import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.Phone
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates realistic phone numbers based on nationality.
 * Supports FR (+33), EN/UK (+44), and DE (+49) formats.
 *
 * Security: Uses SecureRandom for cryptographically secure random generation.
 */
@Singleton
class PhoneGenerator @Inject constructor(
    private val secureRandom: SecureRandom
) {
    /**
     * Generates a phone number for the specified nationality.
     * Uses country-specific formatting rules for mobile numbers.
     *
     * @param nationality Target country for phone format (default: FR)
     * @return Generated phone with international format (+XX...)
     */
    fun generatePhone(nationality: Nationality = Nationality.FR): Phone {
        val (countryCode, number) = when (nationality) {
            Nationality.FR -> "33" to generateFrenchNumber()
            Nationality.EN -> "44" to generateUKNumber()
            Nationality.DE -> "49" to generateGermanNumber()
        }

        return Phone("+$countryCode$number")
    }

    /**
     * French mobile number: 6 XX XX XX XX or 7 XX XX XX XX
     * Prefix 6 = traditional mobile, Prefix 7 = newer allocations
     */
    private fun generateFrenchNumber(): String {
        val prefix = if (secureRandom.nextBoolean()) "6" else "7"
        val rest = (0..99999999).random(secureRandom.asKotlinRandom())
            .toString().padStart(8, '0')
        return "$prefix$rest"
    }

    /**
     * UK mobile number: 7XXX XXX XXX
     * All UK mobile numbers start with 07
     */
    private fun generateUKNumber(): String {
        val prefix = "7"
        val part1 = (100..999).random(secureRandom.asKotlinRandom())
        val part2 = (0..999999).random(secureRandom.asKotlinRandom())
            .toString().padStart(6, '0')
        return "$prefix$part1$part2"
    }

    /**
     * German mobile number: 15X XXXXXXXX or 16X XXXXXXXX or 17X XXXXXXXX
     * Common German mobile prefixes for major carriers
     */
    private fun generateGermanNumber(): String {
        val prefixes = listOf("15", "16", "17")
        val prefix = prefixes.random(secureRandom.asKotlinRandom())
        val subPrefix = (0..9).random(secureRandom.asKotlinRandom())
        val rest = (0..99999999).random(secureRandom.asKotlinRandom())
            .toString().padStart(8, '0')
        return "$prefix$subPrefix$rest"
    }

    /**
     * Legacy method for backward compatibility.
     * Generates a random phone from any supported country.
     *
     * @return Generated phone with random nationality
     * @deprecated Use generatePhone(nationality) instead for explicit country selection.
     */
    @Suppress("unused") // Public API - legacy method for backward compatibility
    fun generatePhone(): Phone {
        val nationality = Nationality.entries.toTypedArray()
            .random(secureRandom.asKotlinRandom())
        return generatePhone(nationality)
    }
}
