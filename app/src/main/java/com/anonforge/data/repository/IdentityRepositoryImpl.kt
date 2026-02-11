package com.anonforge.data.repository

import com.anonforge.data.local.db.IdentityDao
import com.anonforge.data.mapper.IdentityMapper
import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.repository.IdentityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityRepositoryImpl @Inject constructor(
    private val identityDao: IdentityDao,
    private val mapper: IdentityMapper
) : IdentityRepository {

    override suspend fun insertIdentity(identity: DomainIdentity) {
        val entity = mapper.toEntity(identity)
        identityDao.insertIdentity(entity)
    }

    override suspend fun deleteIdentity(id: String) {
        identityDao.deleteIdentity(id)
    }

    override suspend fun deleteExpiredIdentities() {
        identityDao.deleteExpiredIdentities(System.currentTimeMillis())
    }

    override fun getAllIdentitiesFlow(): Flow<List<DomainIdentity>> {
        return identityDao.getAllIdentitiesFlow().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override suspend fun getIdentityById(id: String): DomainIdentity? {
        return identityDao.getIdentityById(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun getExpiredIdentities(): List<DomainIdentity> {
        return identityDao.getExpiredIdentities(System.currentTimeMillis())
            .map { mapper.toDomain(it) }
    }

    override suspend fun updateCustomName(id: String, customName: String?) {
        identityDao.updateCustomName(id, customName)
    }
}