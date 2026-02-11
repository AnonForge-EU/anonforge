package com.anonforge.domain.repository

import com.anonforge.domain.model.DomainIdentity
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    suspend fun insertIdentity(identity: DomainIdentity)
    suspend fun deleteIdentity(id: String)
    suspend fun deleteExpiredIdentities()
    fun getAllIdentitiesFlow(): Flow<List<DomainIdentity>>
    suspend fun getIdentityById(id: String): DomainIdentity?
    suspend fun getExpiredIdentities(): List<DomainIdentity>

    // NEW: Update custom name for identity renaming (Skill 13)
    suspend fun updateCustomName(id: String, customName: String?)
}