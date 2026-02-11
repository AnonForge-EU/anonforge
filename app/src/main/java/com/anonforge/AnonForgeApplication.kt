package com.anonforge

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anonforge.worker.ExpiryCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Main Application class for AnonForge.
 *
 * Implements Configuration.Provider for WorkManager + Hilt integration.
 * This is REQUIRED for @HiltWorker annotated workers to be instantiated correctly.
 *
 * IMPORTANT: WorkManager auto-initialization is disabled in AndroidManifest.xml.
 * WorkManager is initialized on-demand via workManagerConfiguration.
 */
@HiltAndroidApp
class AnonForgeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Flag to track if cleanup has been scheduled
    private var cleanupScheduled = false

    /**
     * Provides WorkManager configuration with HiltWorkerFactory.
     * This allows Hilt to inject dependencies into Workers.
     *
     * Called lazily when WorkManager.getInstance() is first accessed.
     */
    override val workManagerConfiguration: Configuration
        get() {
            // Schedule cleanup on first configuration access (WorkManager is ready)
            if (!cleanupScheduled) {
                cleanupScheduled = true
                // Post to ensure we're not in configuration phase
                Handler(Looper.getMainLooper()).postDelayed({
                    scheduleExpiryCleanup()
                }, 1000) // 1 second delay for safety
            }

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(Log.INFO)
                .build()
        }

    /**
     * Schedules periodic cleanup of expired identities.
     * Runs every 12 hours when battery is not low.
     */
    private fun scheduleExpiryCleanup() {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<ExpiryCleanupWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                ExpiryCleanupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
        } catch (e: Exception) {
            Log.w("AnonForgeApp", "Failed to schedule cleanup worker", e)
        }
    }
}