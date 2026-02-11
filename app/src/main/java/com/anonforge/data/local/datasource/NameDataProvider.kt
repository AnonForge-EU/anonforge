package com.anonforge.data.local.datasource

import android.content.Context
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides name data (first names, last names) loaded from assets.
 * Supports weighted random selection based on name frequency.
 * Supports nationality-based name generation (Skill 13).
 *
 * Note: Cache is never cleared as assets are static and app lifecycle
 * handles memory management. Removed clearCache() as dead code.
 */
@Singleton
class NameDataProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // Cache for loaded names by nationality and gender
    private val firstNamesCache = mutableMapOf<Pair<Nationality, Gender>, List<WeightedName>>()
    private val lastNamesCache = mutableMapOf<Nationality, List<WeightedName>>()

    // Legacy: Default names (backward compatibility)
    private val _maleFirstNames: List<WeightedName> by lazy {
        loadWeightedNames("names/male_first_names.txt")
    }

    private val _femaleFirstNames: List<WeightedName> by lazy {
        loadWeightedNames("names/female_first_names.txt")
    }

    private val _lastNames: List<WeightedName> by lazy {
        loadWeightedNames("names/last_names.txt")
    }

    /**
     * Returns first names for the specified gender and nationality.
     * Falls back to default names if nationality-specific file not found.
     */
    fun getFirstNames(gender: Gender, nationality: Nationality = Nationality.FR): List<WeightedName> {
        val cacheKey = nationality to gender

        return firstNamesCache.getOrPut(cacheKey) {
            val genderPrefix = when (gender) {
                Gender.MALE -> "male"
                Gender.FEMALE -> "female"
            }
            val path = "names/${nationality.code}_${genderPrefix}_first_names.txt"

            val loaded = loadWeightedNames(path)
            if (loaded.size > 1 || loaded.firstOrNull()?.name != "Default") {
                loaded
            } else {
                // Fallback to legacy files
                when (gender) {
                    Gender.MALE -> _maleFirstNames
                    Gender.FEMALE -> _femaleFirstNames
                }
            }
        }
    }

    /**
     * Returns list of last names with weights for the specified nationality.
     * Falls back to default names if nationality-specific file not found.
     */
    fun getLastNames(nationality: Nationality = Nationality.FR): List<WeightedName> {
        return lastNamesCache.getOrPut(nationality) {
            val path = "names/${nationality.code}_last_names.txt"

            val loaded = loadWeightedNames(path)
            if (loaded.size > 1 || loaded.firstOrNull()?.name != "Default") {
                loaded
            } else {
                // Fallback to legacy file
                _lastNames
            }
        }
    }

    /**
     * Legacy method for backward compatibility.
     */
    @Suppress("unused") // Legacy API - may be used by existing code
    fun getFirstNames(gender: Gender): List<WeightedName> {
        return getFirstNames(gender, Nationality.FR)
    }

    /**
     * Legacy method for backward compatibility.
     */
    @Suppress("unused") // Legacy API - may be used by existing code
    fun getLastNames(): List<WeightedName> = getLastNames(Nationality.FR)

    /**
     * Loads weighted names from asset file.
     * Format: "Name|Weight" per line (weight defaults to 1 if missing).
     * Also supports simple "Name" format (weight = 1).
     * Lines starting with # are treated as comments.
     */
    private fun loadWeightedNames(assetPath: String): List<WeightedName> {
        return try {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { line ->
                        val parts = line.split("|")
                        WeightedName(
                            name = parts[0].trim(),
                            weight = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                        )
                    }
                    .toList()
            }
        } catch (_: Exception) {
            listOf(WeightedName("Default", 1))
        }
    }

    // NOTE: clearCache() removed - static assets don't need cache invalidation
    // Memory is managed by Android lifecycle
}

/**
 * Represents a name with its frequency weight for realistic random selection.
 */
data class WeightedName(
    val name: String,
    val weight: Int
)
