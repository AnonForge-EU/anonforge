package com.anonforge.data.repository

import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val biometricEnabledFlow: Flow<Boolean> = dataStore.biometricEnabled
    override val notificationsEnabledFlow: Flow<Boolean> = dataStore.notificationsEnabled
    override val autoLockTimeoutFlow: Flow<Int> = dataStore.autoLockTimeout

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.setBiometricEnabled(enabled)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.setAutoLockTimeout(minutes)
    }

    override suspend fun getBiometricEnabled(): Boolean {
        return dataStore.getBiometricEnabled()
    }

    override suspend fun getNotificationsEnabled(): Boolean {
        return dataStore.getNotificationsEnabled()
    }
}