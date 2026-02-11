package com.anonforge.feature.generator

import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.domain.model.DateOfBirth
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates random, valid dates of birth within user-specified age range.
 * Handles leap years correctly and respects user preferences.
 */
@Singleton
class DateOfBirthGenerator @Inject constructor(
    private val secureRandom: SecureRandom,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        const val DEFAULT_MIN_AGE = 18
        const val DEFAULT_MAX_AGE = 85
    }

    /**
     * Generates a random date of birth within the configured age range.
     */
    suspend fun generateDateOfBirth(): DateOfBirth {
        val prefs = preferencesRepository.getGenerationPreferences()
        val minAge = prefs.ageRangeMin.coerceAtLeast(DEFAULT_MIN_AGE)
        val maxAge = prefs.ageRangeMax.coerceAtMost(DEFAULT_MAX_AGE)

        return generateDateOfBirth(minAge, maxAge)
    }

    /**
     * Generates a random date of birth for the specified age range.
     */
    fun generateDateOfBirth(minAge: Int, maxAge: Int): DateOfBirth {
        val validMinAge = minAge.coerceAtLeast(DEFAULT_MIN_AGE)
        val validMaxAge = maxAge.coerceAtMost(DEFAULT_MAX_AGE).coerceAtLeast(validMinAge)

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val latestBirthYear = today.year - validMinAge
        val earliestBirthYear = today.year - validMaxAge - 1

        val birthYear = earliestBirthYear + secureRandom.nextInt(latestBirthYear - earliestBirthYear + 1)
        val birthMonth = 1 + secureRandom.nextInt(12)

        val maxDays = when (birthMonth) {
            2 -> if ((birthYear % 4 == 0 && birthYear % 100 != 0) || (birthYear % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        val birthDay = 1 + secureRandom.nextInt(maxDays)

        return DateOfBirth(LocalDate(birthYear, birthMonth, birthDay))
    }
}