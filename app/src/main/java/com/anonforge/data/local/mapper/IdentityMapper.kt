package com.anonforge.data.mapper

import com.anonforge.data.local.db.IdentityEntity
import com.anonforge.domain.model.Address
import com.anonforge.domain.model.DateOfBirth
import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.model.Email
import com.anonforge.domain.model.FullName
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Phone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper between DomainIdentity and IdentityEntity.
 *
 * Domain models used:
 * - FullName(firstName, middleName, lastName, gender)
 * - Address(street, city, zipCode, country)
 * - Phone(value) - formatted is computed property
 * - DateOfBirth(date) - age is computed property
 * - Email(value) - nullable in DomainIdentity
 * - customName - user-defined name for identification
 */
@Singleton
class IdentityMapper @Inject constructor() {

    /**
     * Convert domain model to database entity.
     */
    fun toEntity(domain: DomainIdentity): IdentityEntity {
        return IdentityEntity(
            id = domain.id,
            firstName = domain.fullName.firstName,
            middleName = domain.fullName.middleName,
            lastName = domain.fullName.lastName,
            gender = domain.gender.name,
            email = domain.email?.value,
            phone = domain.phone.value,
            streetAddress = domain.address?.street,
            city = domain.address?.city,
            zipCode = domain.address?.zipCode,
            country = domain.address?.country,
            dateOfBirth = domain.dateOfBirth.date.toString(),
            createdAt = domain.createdAt.toEpochMilliseconds(),
            expiresAt = domain.expiresAt?.toEpochMilliseconds(),
            isFavorite = false,
            customName = domain.customName
        )
    }

    /**
     * Convert database entity to domain model.
     */
    fun toDomain(entity: IdentityEntity): DomainIdentity {
        val gender = Gender.valueOf(entity.gender)

        val address = if (entity.streetAddress != null) {
            Address(
                street = entity.streetAddress,
                city = entity.city ?: "",
                zipCode = entity.zipCode ?: "",
                country = entity.country ?: ""
            )
        } else {
            null
        }

        return DomainIdentity(
            id = entity.id,
            fullName = FullName(
                firstName = entity.firstName,
                middleName = entity.middleName,
                lastName = entity.lastName,
                gender = gender
            ),
            email = entity.email?.let { Email(it) },
            phone = Phone(entity.phone),
            address = address,
            dateOfBirth = DateOfBirth(LocalDate.parse(entity.dateOfBirth)),
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            expiresAt = entity.expiresAt?.let { Instant.fromEpochMilliseconds(it) },
            gender = gender,
            customName = entity.customName
        )
    }
}