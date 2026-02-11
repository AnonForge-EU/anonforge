package com.anonforge.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for stored identities.
 *
 * Maps to domain models:
 * - FullName(firstName, middleName, lastName, gender)
 * - Address(street, city, zipCode, country)
 * - Phone(value) - formatted is computed
 * - DateOfBirth(date) - age is computed
 * - Email(value) - nullable
 *
 * @property customName User-defined name for easy identification (Skill 13 - Renaming)
 */
@Entity(tableName = "identities")
data class IdentityEntity(
    @PrimaryKey
    val id: String,

    // FullName fields
    val firstName: String,
    val middleName: String?,
    val lastName: String,

    // Gender (used in FullName and DomainIdentity)
    val gender: String,

    // Email (nullable - only when alias configured)
    val email: String?,

    // Phone
    val phone: String,

    // Address (all nullable together)
    val streetAddress: String?,
    val city: String?,
    val zipCode: String?,
    val country: String?,

    // Date of birth (ISO format: yyyy-MM-dd)
    val dateOfBirth: String,

    // Timestamps
    val createdAt: Long,
    val expiresAt: Long?,

    // User preferences
    val isFavorite: Boolean = false,

    // Custom name for identity (user can rename for easy identification)
    val customName: String? = null
)