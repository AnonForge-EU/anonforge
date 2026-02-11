package com.anonforge.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val biometricEnabledFlow: Flow<Boolean>
    val notificationsEnabledFlow: Flow<Boolean>
    val autoLockTimeoutFlow: Flow<Int>
    
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAutoLockTimeout(minutes: Int)
    suspend fun getBiometricEnabled(): Boolean
    suspend fun getNotificationsEnabled(): Boolean
}
