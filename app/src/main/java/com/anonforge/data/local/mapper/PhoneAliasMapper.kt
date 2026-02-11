package com.anonforge.data.local.mapper

import com.anonforge.data.local.entity.PhoneAliasEntity
import com.anonforge.domain.model.PhoneAlias
import kotlinx.datetime.Instant

/**
 * Mappers between PhoneAlias domain model and PhoneAliasEntity.
 *
 * Entity stores timestamps as Long (epoch millis).
 * Domain model uses kotlinx.datetime.Instant.
 */

fun PhoneAliasEntity.toDomain(): PhoneAlias = PhoneAlias(
    id = id,
    phoneNumber = phoneNumber,
    friendlyName = friendlyName,
    isPrimary = isPrimary,
    usageCount = usageCount,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    lastUsedAt = lastUsedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun PhoneAlias.toEntity(): PhoneAliasEntity = PhoneAliasEntity(
    id = id,
    phoneNumber = phoneNumber,
    friendlyName = friendlyName,
    isPrimary = isPrimary,
    usageCount = usageCount,
    createdAt = createdAt.toEpochMilliseconds(),
    lastUsedAt = lastUsedAt?.toEpochMilliseconds()
)

fun List<PhoneAliasEntity>.toDomainList(): List<PhoneAlias> = map { it.toDomain() }