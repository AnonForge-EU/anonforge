package com.anonforge.generator

import com.anonforge.data.local.datasource.NameDataProvider
import com.anonforge.data.local.datasource.WeightedName
import com.anonforge.domain.model.FullName
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates realistic names using country-specific weighted datasets.
 *
 * Features:
 * - Weighted random selection (common names appear more frequently)
 * - Country-specific name pools (FR, EN, DE)
 * - Gender-aware first name generation
 * - Cryptographically secure randomness
 *
 * Data coverage per country:
 * - 300-500 first names (male/female)
 * - 300-400 last names
 * - Weights based on official statistics (INSEE, ONS, Destatis)
 */
@Singleton
class NameGenerator @Inject constructor(
    private val secureRandom: SecureRandom,
    private val nameDataProvider: NameDataProvider
) {
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate a complete FullName object.
     *
     * @param gender Gender for first name selection
     * @param nationality Target nationality for name style
     * @return FullName object with firstName, lastName, and gender
     */
    fun generateFullName(gender: Gender, nationality: Nationality): FullName {
        val firstName = generateFirstName(gender, nationality)
        val lastName = generateLastName(nationality)
        
        return FullName(
            firstName = firstName,
            middleName = null,  // Middle names not generated for now
            lastName = lastName,
            gender = gender
        )
    }

    /**
     * Generate a first name using weighted selection.
     *
     * @param gender Gender (MALE or FEMALE)
     * @param nationality Target nationality
     * @return First name string
     */
    fun generateFirstName(gender: Gender, nationality: Nationality): String {
        val names = nameDataProvider.getFirstNames(gender, nationality)

        return if (names.isNotEmpty() && names.first().name != "Default") {
            selectWeighted(names)
        } else {
            selectFallbackFirstName(nationality, gender)
        }
    }

    /**
     * Generate a last name using weighted selection.
     *
     * @param nationality Target nationality
     * @return Last name string
     */
    fun generateLastName(nationality: Nationality): String {
        val names = nameDataProvider.getLastNames(nationality)

        return if (names.isNotEmpty() && names.first().name != "Default") {
            selectWeighted(names)
        } else {
            selectFallbackLastName(nationality)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a name using weighted random selection.
     * Higher weight = higher probability of selection.
     */
    private fun selectWeighted(names: List<WeightedName>): String {
        if (names.isEmpty()) return "Unknown"

        val totalWeight = names.sumOf { it.weight }
        if (totalWeight <= 0) return names.random().name

        var target = secureRandom.nextInt(totalWeight)

        for (entry in names) {
            target -= entry.weight
            if (target < 0) {
                return entry.name
            }
        }

        // Fallback (should not reach here)
        return names.last().name
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FALLBACK DATA (when assets not loaded)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun selectFallbackFirstName(nationality: Nationality, gender: Gender): String {
        val names = when (nationality) {
            Nationality.FR -> if (gender == Gender.MALE) {
                listOf("Jean", "Pierre", "Michel", "François", "André", "Philippe", "Louis", "Nicolas", "Thomas", "Alexandre")
            } else {
                listOf("Marie", "Jeanne", "Françoise", "Monique", "Catherine", "Nathalie", "Isabelle", "Sophie", "Julie", "Emma")
            }
            Nationality.EN -> if (gender == Gender.MALE) {
                listOf("James", "John", "William", "Thomas", "George", "Charles", "Henry", "Edward", "Richard", "Oliver")
            } else {
                listOf("Mary", "Elizabeth", "Sarah", "Emma", "Charlotte", "Alice", "Grace", "Emily", "Sophie", "Olivia")
            }
            Nationality.DE -> if (gender == Gender.MALE) {
                listOf("Hans", "Peter", "Michael", "Thomas", "Andreas", "Stefan", "Markus", "Christian", "Martin", "Felix")
            } else {
                listOf("Anna", "Maria", "Elisabeth", "Monika", "Petra", "Sabine", "Claudia", "Susanne", "Stefanie", "Emma")
            }
        }
        return names[secureRandom.nextInt(names.size)]
    }

    private fun selectFallbackLastName(nationality: Nationality): String {
        val names = when (nationality) {
            Nationality.FR -> listOf("Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau")
            Nationality.EN -> listOf("Smith", "Jones", "Williams", "Taylor", "Brown", "Davies", "Evans", "Wilson", "Thomas", "Johnson")
            Nationality.DE -> listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann")
        }
        return names[secureRandom.nextInt(names.size)]
    }
}
