package com.anonforge.data.generator

import android.content.Context
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides country-specific data for identity generation.
 * Loads weighted data from assets based on selected nationality.
 *
 * Data sources:
 * - FR: INSEE 2020-2024 (names, cities)
 * - EN: ONS 2020-2024 (names, cities)
 * - DE: Destatis 2020-2024 (names, cities)
 *
 * All data includes frequency weights for realistic distribution.
 * Cache is managed by Android lifecycle (static assets don't need invalidation).
 */
@Singleton
class CountryDataProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // Cached weighted names per nationality and gender
    private val maleFirstNamesCache = mutableMapOf<Nationality, List<WeightedEntry>>()
    private val femaleFirstNamesCache = mutableMapOf<Nationality, List<WeightedEntry>>()
    private val lastNamesCache = mutableMapOf<Nationality, List<WeightedEntry>>()

    // Cached location data per nationality
    private val citiesCache = mutableMapOf<Nationality, List<CityEntry>>()
    private val streetsCache = mutableMapOf<Nationality, List<String>>()

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Names
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get weighted first names for nationality and gender.
     * Returns list with frequency weights for realistic selection.
     */
    fun getFirstNames(nationality: Nationality, gender: Gender): List<WeightedEntry> {
        val cache = if (gender == Gender.MALE) maleFirstNamesCache else femaleFirstNamesCache
        val genderPrefix = if (gender == Gender.MALE) "male" else "female"

        return cache.getOrPut(nationality) {
            loadWeightedEntries("names/${nationality.code}_${genderPrefix}_first_names.txt")
        }
    }

    /**
     * Get weighted last names for nationality.
     * Returns list with frequency weights for realistic selection.
     */
    fun getLastNames(nationality: Nationality): List<WeightedEntry> {
        return lastNamesCache.getOrPut(nationality) {
            loadWeightedEntries("names/${nationality.code}_last_names.txt")
        }
    }

    /**
     * Get flat list of first names (for simple random selection).
     */
    @Suppress("unused") // Public API for alternative selection strategy
    fun getFirstNamesFlat(nationality: Nationality, gender: Gender): List<String> {
        return getFirstNames(nationality, gender).map { it.value }
    }

    /**
     * Get flat list of last names (for simple random selection).
     */
    @Suppress("unused") // Public API for alternative selection strategy
    fun getLastNamesFlat(nationality: Nationality): List<String> {
        return getLastNames(nationality).map { it.value }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Locations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get cities with postal codes for nationality.
     */
    fun getCities(nationality: Nationality): List<CityEntry> {
        return citiesCache.getOrPut(nationality) {
            loadCityEntries("addresses/${nationality.code}_cities.txt")
        }
    }

    /**
     * Get flat list of city names (for simple random selection).
     */
    @Suppress("unused") // Public API for alternative selection strategy
    fun getCitiesFlat(nationality: Nationality): List<String> {
        return getCities(nationality).map { it.name }
    }

    /**
     * Get street names for nationality.
     */
    @Suppress("unused") // Public API for address generation enhancement
    fun getStreets(nationality: Nationality): List<String> {
        return streetsCache.getOrPut(nationality) {
            loadLines("addresses/${nationality.code}_streets.txt")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Phone & Postal
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get phone prefix for nationality.
     */
    @Suppress("unused") // Public API - phone prefix accessed via Nationality.phonePrefix
    fun getPhonePrefix(nationality: Nationality): String {
        return nationality.phonePrefix
    }

    /**
     * Get postal code format for nationality.
     */
    @Suppress("unused") // Public API for postal code validation
    fun getPostalCodeFormat(nationality: Nationality): PostalCodeFormat {
        return when (nationality) {
            Nationality.FR -> PostalCodeFormat.NUMERIC_5      // 75001
            Nationality.EN -> PostalCodeFormat.UK_FORMAT      // SW1A 1AA
            Nationality.DE -> PostalCodeFormat.NUMERIC_5      // 10115
        }
    }

    /**
     * Get postal code regex pattern for validation.
     */
    @Suppress("unused") // Public API for postal code validation
    fun getPostalCodePattern(nationality: Nationality): Regex {
        return when (nationality) {
            Nationality.FR -> Regex("^\\d{5}$")                          // 75001
            Nationality.EN -> Regex("^[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}$")  // SW1A 1AA
            Nationality.DE -> Regex("^\\d{5}$")                          // 10115
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WEIGHTED SELECTION UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a random entry using weighted distribution.
     * Higher weight = higher probability of selection.
     *
     * @param entries List of weighted entries
     * @param random Random source (should be SecureRandom)
     * @return Selected entry value, or null if list is empty
     */
    @Suppress("unused") // Public API for weighted random selection
    fun selectWeighted(entries: List<WeightedEntry>, random: java.util.Random): String? {
        if (entries.isEmpty()) return null

        val totalWeight = entries.sumOf { it.weight }
        if (totalWeight <= 0) return entries.randomOrNull()?.value

        var target = random.nextInt(totalWeight)

        for (entry in entries) {
            target -= entry.weight
            if (target < 0) {
                return entry.value
            }
        }

        // Fallback (should not reach here)
        return entries.last().value
    }

    /**
     * Select a random city with its postal code using weighted distribution.
     */
    @Suppress("unused") // Public API for city selection with postal code
    fun selectWeightedCity(nationality: Nationality, random: java.util.Random): CityEntry? {
        val cities = getCities(nationality)
        if (cities.isEmpty()) return null
        return cities[random.nextInt(cities.size)]
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Asset Loading
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Load weighted entries from asset file.
     * Format: Value|Weight (e.g., "Martin|100")
     * Lines starting with # are comments.
     */
    private fun loadWeightedEntries(path: String): List<WeightedEntry> {
        return loadLines(path).mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                val value = parts[0].trim()
                val weight = parts[1].trim().toIntOrNull() ?: DEFAULT_WEIGHT
                if (value.isNotBlank()) WeightedEntry(value, weight) else null
            } else if (parts.size == 1 && line.isNotBlank()) {
                // Support simple format (no weight)
                WeightedEntry(line.trim(), DEFAULT_WEIGHT)
            } else {
                null
            }
        }
    }

    /**
     * Load city entries from asset file.
     * Format: CityName|PostalCode (e.g., "Paris|75001")
     */
    private fun loadCityEntries(path: String): List<CityEntry> {
        return loadLines(path).mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                val name = parts[0].trim()
                val postalCode = parts[1].trim()
                if (name.isNotBlank()) CityEntry(name, postalCode) else null
            } else if (parts.size == 1 && line.isNotBlank()) {
                // Support simple format (no postal code)
                CityEntry(line.trim(), "")
            } else {
                null
            }
        }
    }

    /**
     * Load asset file as list of strings.
     * Filters out empty lines and comments (lines starting with #).
     */
    private fun loadLines(path: String): List<String> {
        return try {
            context.assets.open(path).bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.trim() }
                    .toList()
            }
        } catch (_: Exception) {
            // Asset not found - return empty list (fallback handled by generators)
            emptyList()
        }
    }

    companion object {
        private const val DEFAULT_WEIGHT = 50
    }
}

/**
 * Entry with frequency weight for realistic distribution.
 *
 * @property value The actual value (name, city, etc.)
 * @property weight Frequency weight (higher = more common)
 */
data class WeightedEntry(
    val value: String,
    val weight: Int
)

/**
 * City entry with postal code.
 *
 * @property name City name
 * @property postalCode Postal/ZIP code
 */
data class CityEntry(
    val name: String,
    val postalCode: String
)

/**
 * Postal code format types by country.
 */
enum class PostalCodeFormat {
    /** 5-digit numeric: 75001 (FR), 10115 (DE) */
    NUMERIC_5,

    /** UK format: SW1A 1AA */
    UK_FORMAT
}