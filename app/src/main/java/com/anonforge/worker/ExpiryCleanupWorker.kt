package com.anonforge.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anonforge.domain.repository.IdentityRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that periodically cleans up expired identities.
 * Uses Hilt for dependency injection via @HiltWorker.
 *
 * Schedule: Daily via WorkManager in AnonForgeApplication.
 */
@HiltWorker
class ExpiryCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val identityRepository: IdentityRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Delete all expired identities (repository handles timestamp internally)
            identityRepository.deleteExpiredIdentities()
            Result.success()
        } catch (_: Exception) {
            // Retry on failure (WorkManager will handle backoff)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "expiry_cleanup_worker"
    }
}