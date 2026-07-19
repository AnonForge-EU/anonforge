package com.anonforge.data.repository

import android.util.Log
import com.anonforge.data.local.dao.PhoneAliasDao
import com.anonforge.data.local.mapper.toDomain
import com.anonforge.data.local.mapper.toDomainList
import com.anonforge.data.local.mapper.toEntity
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.PhoneAliasRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
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

    /**
     * Single shared upstream subscription on phone_alias_history.
     *
     * With Room 2.8 compat mode over the SQLCipher SupportFactory, a SECOND
     * concurrent Flow subscription on the same table stalls before its first
     * emission (observed on device, no exception thrown) — which left the
     * saved-numbers UI empty whenever two screens observed this table at
     * once. Sharing one upstream subscription (like the vault, which only
     * ever has one collector and has always been stable) removes the
     * concurrent-subscription case entirely; replay=1 gives late collectors
     * the current list immediately.
     */
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allAliasesShared: Flow<List<PhoneAlias>> = flow {
        // Seed with a direct snapshot so the first paint never waits on the
        // invalidation tracker registration.
        emit(phoneAliasDao.getAllAliasesList())
        emitAll(phoneAliasDao.getAllAliases())
    }
        .map { entities -> entities.toDomainList() }
        .distinctUntilChanged()
        .retryWhen { cause, attempt ->
            // A transient DB error must not kill the observable stream for
            // the rest of the app's life. Log.w/Log.e survive R8.
            val willRetry = attempt < MAX_FLOW_RETRIES - 1
            Log.w(TAG, "getAllAliases flow failed (attempt ${attempt + 1}/$MAX_FLOW_RETRIES), willRetry=$willRetry", cause)
            if (willRetry) {
                delay(FLOW_RETRY_DELAY_MS * (attempt + 1))
            }
            willRetry
        }
        .catch { e ->
            Log.e(TAG, "getAllAliases flow failed after $MAX_FLOW_RETRIES attempts, emitting empty list", e)
            emit(emptyList())
        }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS),
            replay = 1
        )

    override fun getAllAliases(): Flow<List<PhoneAlias>> = allAliasesShared

    override suspend fun getAllAliasesList(): List<PhoneAlias> {
        return phoneAliasDao.getAllAliasesList().toDomainList()
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
                val remaining = phoneAliasDao.getAllAliasesList()
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

    private companion object {
        const val TAG = "PhoneAliasRepository"
        const val MAX_FLOW_RETRIES = 3L
        const val FLOW_RETRY_DELAY_MS = 250L

        /** Keep the shared upstream alive across quick screen transitions so
         *  navigating Settings → Virtual Numbers reuses one subscription. */
        const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}