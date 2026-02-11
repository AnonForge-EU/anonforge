package com.anonforge.generator

import com.anonforge.data.local.datasource.AddressDataProvider
import com.anonforge.data.local.datasource.CityEntry
import com.anonforge.data.local.datasource.PostalCodeFormat
import com.anonforge.domain.model.Address
import com.anonforge.domain.model.Nationality
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates realistic addresses using country-specific datasets.
 *
 * Features:
 * - Country-specific street patterns and formats
 * - Real cities with matching postal codes from INSEE/ONS/Destatis
 * - Realistic street numbers based on country conventions
 * - Cryptographically secure randomness
 *
 * Data coverage per country:
 * - 200-300 cities with postal codes
 * - 200+ street name patterns
 * - Country-specific address formatting
 */
@Singleton
class AddressGenerator @Inject constructor(
    private val secureRandom: SecureRandom,
    private val addressDataProvider: AddressDataProvider
) {
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate a complete address for the given nationality.
     *
     * @param nationality Target nationality for address style
     * @return Address domain object with all fields populated
     */
    fun generateAddress(nationality: Nationality): Address {
        val city = selectCity(nationality)
        val street = selectStreet(nationality)
        val streetNumber = generateStreetNumber(nationality)

        // Use postal code from city data, or generate one if not available
        val zipCode = city?.postalCode?.takeIf { it.isNotBlank() }
            ?: generateZipCode(nationality)

        return Address(
            street = formatStreetLine(nationality, street, streetNumber),
            city = city?.name ?: selectFallbackCity(nationality),
            zipCode = zipCode,
            country = addressDataProvider.getCountryName(nationality)
        )
    }

    /**
     * Generate a zip/postal code based on nationality format.
     */
    fun generateZipCode(nationality: Nationality): String {
        return when (addressDataProvider.getPostalCodeFormat(nationality)) {
            PostalCodeFormat.NUMERIC_5 -> generateNumericZipCode5(nationality)
            PostalCodeFormat.UK_FORMAT -> generateUKPostcode()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Selection
    // ═══════════════════════════════════════════════════════════════════════════

    private fun selectCity(nationality: Nationality): CityEntry? {
        val cities = addressDataProvider.getCities(nationality)
        return if (cities.isNotEmpty()) {
            cities[secureRandom.nextInt(cities.size)]
        } else {
            null
        }
    }

    private fun selectStreet(nationality: Nationality): String {
        val streets = addressDataProvider.getStreets(nationality)
        return if (streets.isNotEmpty()) {
            streets[secureRandom.nextInt(streets.size)]
        } else {
            selectFallbackStreet(nationality)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Formatting
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Format street line according to country conventions.
     * - FR: "12 Rue de la Paix" (number first)
     * - EN: "12 High Street" (number first)
     * - DE: "Hauptstraße 12" (street first, then number)
     */
    private fun formatStreetLine(nationality: Nationality, street: String, number: String): String {
        return when (nationality) {
            Nationality.FR -> "$number $street"
            Nationality.EN -> "$number $street"
            Nationality.DE -> "$street $number"
        }
    }

    /**
     * Generate realistic street number based on country patterns.
     */
    private fun generateStreetNumber(nationality: Nationality): String {
        return when (nationality) {
            Nationality.FR -> {
                // French: 1-150, occasional bis/ter
                val num = secureRandom.nextInt(150) + 1
                val suffix = when {
                    secureRandom.nextInt(100) < 5 -> " bis"
                    secureRandom.nextInt(100) < 2 -> " ter"
                    else -> ""
                }
                "$num$suffix"
            }
            Nationality.EN -> {
                // UK: 1-200, occasional A/B suffix
                val num = secureRandom.nextInt(200) + 1
                val suffix = when {
                    secureRandom.nextInt(100) < 3 -> "A"
                    secureRandom.nextInt(100) < 2 -> "B"
                    else -> ""
                }
                "$num$suffix"
            }
            Nationality.DE -> {
                // German: 1-100, occasional a/b suffix
                val num = secureRandom.nextInt(100) + 1
                val suffix = when {
                    secureRandom.nextInt(100) < 5 -> "a"
                    secureRandom.nextInt(100) < 3 -> "b"
                    else -> ""
                }
                "$num$suffix"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Postal Code Generation (fallback when city has no postal code)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun generateNumericZipCode5(nationality: Nationality): String {
        return when (nationality) {
            Nationality.FR -> {
                // French: Department (01-95) + commune (000-999)
                val dept = (secureRandom.nextInt(95) + 1).toString().padStart(2, '0')
                val commune = secureRandom.nextInt(1000).toString().padStart(3, '0')
                "$dept$commune"
            }
            Nationality.DE -> {
                // German: 5 digits (01000-99999)
                val code = secureRandom.nextInt(99000) + 1000
                code.toString().padStart(5, '0')
            }
            else -> {
                // Generic 5-digit
                secureRandom.nextInt(100000).toString().padStart(5, '0')
            }
        }
    }

    private fun generateUKPostcode(): String {
        // UK format: A9 9AA, A99 9AA, AA9 9AA, AA99 9AA, A9A 9AA, AA9A 9AA
        val outwardPatterns = listOf(
            { "${randomLetter()}${randomDigit()}" },                                    // A9
            { "${randomLetter()}${randomDigit()}${randomDigit()}" },                    // A99
            { "${randomLetter()}${randomLetter()}${randomDigit()}" },                   // AA9
            { "${randomLetter()}${randomLetter()}${randomDigit()}${randomDigit()}" },   // AA99
            { "${randomLetter()}${randomDigit()}${randomLetter()}" },                   // A9A
            { "${randomLetter()}${randomLetter()}${randomDigit()}${randomLetter()}" }   // AA9A
        )

        val outward = outwardPatterns[secureRandom.nextInt(outwardPatterns.size)]()
        val inward = "${randomDigit()}${randomLetter()}${randomLetter()}"

        return "$outward $inward"
    }

    private fun randomLetter(): Char {
        // Exclude letters not used in UK postcodes: C, I, K, M, O, V
        val letters = "ABDEFGHJLNPQRSTUWXYZ"
        return letters[secureRandom.nextInt(letters.length)]
    }

    private fun randomDigit(): Char {
        return ('0'.code + secureRandom.nextInt(10)).toChar()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FALLBACK DATA (when assets not loaded)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun selectFallbackCity(nationality: Nationality): String {
        val cities = when (nationality) {
            Nationality.FR -> listOf("Paris", "Lyon", "Marseille", "Toulouse", "Nice", "Nantes", "Bordeaux", "Lille", "Strasbourg", "Rennes")
            Nationality.EN -> listOf("London", "Manchester", "Birmingham", "Leeds", "Glasgow", "Liverpool", "Bristol", "Sheffield", "Edinburgh", "Cardiff")
            Nationality.DE -> listOf("Berlin", "Hamburg", "München", "Köln", "Frankfurt", "Stuttgart", "Düsseldorf", "Leipzig", "Dortmund", "Essen")
        }
        return cities[secureRandom.nextInt(cities.size)]
    }

    private fun selectFallbackStreet(nationality: Nationality): String {
        val streets = when (nationality) {
            Nationality.FR -> listOf("Rue de la République", "Avenue Victor Hugo", "Boulevard Saint-Michel", "Rue du Commerce", "Place de la Mairie")
            Nationality.EN -> listOf("High Street", "Station Road", "Church Lane", "Park Road", "London Road")
            Nationality.DE -> listOf("Hauptstraße", "Bahnhofstraße", "Schulstraße", "Gartenstraße", "Kirchstraße")
        }
        return streets[secureRandom.nextInt(streets.size)]
    }
}