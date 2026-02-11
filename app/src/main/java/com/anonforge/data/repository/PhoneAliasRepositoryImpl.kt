package com.anonforge.data.repository

import com.anonforge.data.local.dao.PhoneAliasDao
import com.anonforge.data.local.mapper.toDomain
import com.anonforge.data.local.mapper.toDomainList
import com.anonforge.data.local.mapper.toEntity
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.PhoneAliasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PhoneAliasRepository.
 *
 * Uses Room for phone alias storage and SettingsDataStore for preferences.
 */
@Singleton
class PhoneAliasRepositoryImpl @Inject constructor(
    private val phoneAliasDao: PhoneAliasDao,
    private val settingsDataStore: SettingsDataStore
) : PhoneAliasRepository {

    override fun getAllAliases(): Flow<List<PhoneAlias>> {
        return phoneAliasDao.getAllAliases()
            .map { entities -> entities.toDomainList() }
            .catch { emit(emptyList()) }
    }

    override suspend fun getPrimaryAlias(): PhoneAlias? {
        return phoneAliasDao.getPrimaryAlias()?.toDomain()
    }

    override suspend fun insertAlias(alias: PhoneAlias): Result<Long> {
        return runCatching {
            val entity = alias.toEntity()
            phoneAliasDao.insert(entity)
        }
    }

    override suspend fun saveAlias(alias: PhoneAlias): Result<Long> {
        return insertAlias(alias)
    }

    override suspend fun updateAlias(alias: PhoneAlias): Result<Unit> {
        return runCatching {
            val entity = alias.toEntity()
            phoneAliasDao.update(entity)
        }
    }

    override suspend fun deleteAlias(id: Long): Result<Unit> {
        return runCatching {
            // Check if deleting primary
            val alias = phoneAliasDao.getById(id)
            val wasPrimary = alias?.isPrimary == true

            phoneAliasDao.deleteById(id)

            // If the deleted alias was primary, set the first remaining as primary
            if (wasPrimary) {
                val remaining = phoneAliasDao.getAllAliases().first()
                if (remaining.isNotEmpty()) {
                    phoneAliasDao.setPrimary(remaining.first().id)
                }
            }
        }
    }

    override suspend fun clearAllAliases(): Result<Unit> {
        return runCatching {
            phoneAliasDao.deleteAll()
        }
    }

    override suspend fun setPrimaryAlias(id: Long): Result<Unit> {
        return runCatching {
            phoneAliasDao.clearAllPrimary()
            phoneAliasDao.setPrimary(id)
        }
    }

    override suspend fun recordUsage(id: Long): Result<Unit> {
        return runCatching {
            phoneAliasDao.recordUsage(id)
        }
    }

    override fun isEnabled(): Flow<Boolean> {
        return settingsDataStore.phoneAliasEnabled
    }

    override suspend fun setEnabled(enabled: Boolean) {
        settingsDataStore.setPhoneAliasEnabled(enabled)
    }

    override suspend fun getAliasCount(): Int {
        return phoneAliasDao.getCount()
    }
}