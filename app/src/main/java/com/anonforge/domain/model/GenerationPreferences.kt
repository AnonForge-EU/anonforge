package com.anonforge.domain.model

/**
 * User preferences for identity generation.
 */
data class GenerationPreferences(
    val genderPreference: GenderPreference = GenderPreference.RANDOM,
    val ageRangeMin: Int = MIN_AGE,
    val ageRangeMax: Int = MAX_AGE
) {
    companion object {
        const val MIN_AGE = 18
        const val MAX_AGE = 85
    }
}

/**
 * Gender preference for identity generation.
 */
enum class GenderPreference {
    RANDOM,
    MALE,
    FEMALE;

    fun toGender(): Gender? = when (this) {
        RANDOM -> null
        MALE -> Gender.MALE
        FEMALE -> Gender.FEMALE
    }
}