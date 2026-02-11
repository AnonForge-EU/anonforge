package com.anonforge.domain.usecase

import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import com.anonforge.feature.generator.DateOfBirthGenerator
import com.anonforge.generator.AddressGenerator
import com.anonforge.generator.NameGenerator
import com.anonforge.generator.PhoneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Use case for generating a complete fictional identity.
 *
 * EMAIL POLICY:
 * - Email is ALWAYS null in this use case
 * - Email is ONLY added when user configures SimpleLogin alias via UI
 * - NO fallback, NO placeholder, NO local generation
 *
 * Generators used:
 * - NameGenerator.generateFullName(gender, nationality) → FullName
 * - PhoneGenerator.generatePhone(nationality) → Phone
 * - AddressGenerator.generateAddress(nationality) → Address
 * - DateOfBirthGenerator.generateDateOfBirth(ageMin, ageMax) → DateOfBirth
 */
class GenerateIdentityUseCase @Inject constructor(
    private val nameGenerator: NameGenerator,
    private val phoneGenerator: PhoneGenerator,
    private val addressGenerator: AddressGenerator,
    private val dobGenerator: DateOfBirthGenerator
) {
    /**
     * Generate a new fictional identity.
     *
     * @param gender Gender for name generation
     * @param nationality Nationality for name/address/phone format
     * @param includeAddress Whether to generate an address
     * @param expiryDuration Duration until identity expires (null = permanent)
     * @param ageMin Minimum age for date of birth generation
     * @param ageMax Maximum age for date of birth generation
     * @return Result containing the generated identity or an error
     */
    suspend operator fun invoke(
        gender: Gender = Gender.random(),
        nationality: Nationality = Nationality.DEFAULT,
        includeAddress: Boolean = true,
        expiryDuration: Duration? = null,
        ageMin: Int = GenerationPreferences.MIN_AGE,
        ageMax: Int = GenerationPreferences.MAX_AGE
    ): Result<DomainIdentity> = withContext(Dispatchers.Default) {
        // Move CPU-bound generation work off main thread
        try {
            // Generate identity components with nationality support
            val fullName = nameGenerator.generateFullName(gender, nationality)
            val phone = phoneGenerator.generatePhone(nationality)
            val address = if (includeAddress) addressGenerator.generateAddress(nationality) else null
            val dob = dobGenerator.generateDateOfBirth(ageMin, ageMax)

            // Calculate timestamps
            val now = Clock.System.now()
            val expiresAt = expiryDuration?.takeIf { it.isFinite() }?.let { now + it }

            // Build identity (email = null, SimpleLogin handles email via UI)
            val identity = DomainIdentity(
                id = java.util.UUID.randomUUID().toString(),
                fullName = fullName,
                email = null,  // ALWAYS null - SimpleLogin handles email via AliasSettingsScreen
                phone = phone,
                address = address,
                dateOfBirth = dob,
                createdAt = now,
                expiresAt = expiresAt,
                gender = gender,
                nationality = nationality
            )

            Result.success(identity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}