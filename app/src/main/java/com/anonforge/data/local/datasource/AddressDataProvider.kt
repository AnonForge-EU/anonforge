package com.anonforge.data.local.datasource

import android.content.Context
import com.anonforge.domain.model.Nationality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides address data (streets, cities with postal codes) loaded from assets.
 * Supports nationality-based address generation.
 * Data is lazily loaded and cached for performance.
 *
 * Asset file formats:
 * - Streets: One street name per line
 * - Cities: "CityName|PostalCode" per line (e.g., "Paris|75001")
 *
 * Note: Cache is never cleared as assets are static and app lifecycle
 * handles memory management.
 */
@Singleton
class AddressDataProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Cache for loaded data by nationality
    private val streetsCache = mutableMapOf<Nationality, List<String>>()
    private val citiesCache = mutableMapOf<Nationality, List<CityEntry>>()

    // Legacy: Default data (backward compatibility)
    private val _streets: List<String> by lazy {
        loadLines("addresses/streets.txt")
    }

    private val _cities: List<CityEntry> by lazy {
        loadCityEntries("addresses/cities.txt")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns list of street names for the specified nationality.
     * Falls back to default streets if nationality-specific file not found.
     */
    fun getStreets(nationality: Nationality = Nationality.FR): List<String> {
        return streetsCache.getOrPut(nationality) {
            val path = "addresses/${nationality.code}_streets.txt"
            val loaded = loadLines(path)
            loaded.ifEmpty { _streets }
        }
    }

    /**
     * Returns list of cities with postal codes for the specified nationality.
     * Falls back to default cities if nationality-specific file not found.
     */
    fun getCities(nationality: Nationality = Nationality.FR): List<CityEntry> {
        return citiesCache.getOrPut(nationality) {
            val path = "addresses/${nationality.code}_cities.txt"
            val loaded = loadCityEntries(path)
            loaded.ifEmpty { _cities }
        }
    }

    /**
     * Returns the country name for display in addresses.
     */
    fun getCountryName(nationality: Nationality): String {
        return when (nationality) {
            Nationality.FR -> "France"
            Nationality.EN -> "United Kingdom"
            Nationality.DE -> "Germany"
        }
    }

    /**
     * Get postal code format for nationality.
     */
    fun getPostalCodeFormat(nationality: Nationality): PostalCodeFormat {
        return when (nationality) {
            Nationality.FR -> PostalCodeFormat.NUMERIC_5      // 75001
            Nationality.EN -> PostalCodeFormat.UK_FORMAT      // SW1A 1AA
            Nationality.DE -> PostalCodeFormat.NUMERIC_5      // 10115
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY API (backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Legacy method - returns city names only (no postal codes).
     */
    @Suppress("unused")
    fun getCityNames(nationality: Nationality = Nationality.FR): List<String> {
        return getCities(nationality).map { it.name }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - Asset Loading
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loads text asset file as list of trimmed, non-blank lines.
     * Lines starting with # are treated as comments.
     */
    private fun loadLines(path: String): List<String> {
        return try {
            context.assets.open(path).bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.trim() }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Loads city entries from asset file.
     * Format: "CityName|PostalCode" per line (e.g., "Paris|75001")
     * Also supports simple "CityName" format (postal code = "").
     */
    private fun loadCityEntries(path: String): List<CityEntry> {
        return try {
            context.assets.open(path).bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { line ->
                        val parts = line.split("|")
                        CityEntry(
                            name = parts[0].trim(),
                            postalCode = parts.getOrNull(1)?.trim() ?: ""
                        )
                    }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * City entry with name and postal code.
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
