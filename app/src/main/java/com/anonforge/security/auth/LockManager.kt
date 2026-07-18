@file:Suppress("DEPRECATION") // EncryptedSharedPreferences/MasterKey - no replacement available

package com.anonforge.security.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.anonforge.data.local.prefs.SecurityPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication sessions and lockout state.
 *
 * Features:
 * - Session timeout tracking (reads from SecurityPreferences)
 * - Failed attempt counting
 * - Lockout enforcement
 * - Encrypted preference storage
 *
 * FIXED: Now reads autoLockMinutes from SecurityPreferences instead of hardcoded value.
 */
@Singleton
class LockManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val securityPreferences: SecurityPreferences
) {
    companion object {
        private const val PREFS_NAME = "anonforge_lock_prefs"
        private const val KEY_SESSION_START = "session_start_time"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_END = "lockout_end_time"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"

        // Fallback timeout if preferences unavailable
        private const val DEFAULT_TIMEOUT_MINUTES = 5

        // Sentinel: auto-lock preference not read from DataStore yet
        private const val TIMEOUT_NOT_LOADED = -1

        // Maximum failed attempts before lockout
        private const val MAX_FAILED_ATTEMPTS = 5
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback to regular prefs if encryption fails
            // This should not happen on modern devices
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * In-memory cache of the user's auto-lock preference, kept up to date by
     * collecting the DataStore flow eagerly for the lifetime of this singleton.
     * Avoids a main-thread runBlocking on every session check.
     *
     * Fail-closed: until the first DataStore emission arrives, the sentinel
     * makes every session look expired (stored sessions survive process death,
     * so an optimistic default could otherwise skip re-auth at cold start).
     */
    private val autoLockMinutes: StateFlow<Int> = securityPreferences.autoLockMinutes
        .catch { emit(DEFAULT_TIMEOUT_MINUTES) }
        .stateIn(
            CoroutineScope(SupervisorJob() + Dispatchers.IO),
            SharingStarted.Eagerly,
            TIMEOUT_NOT_LOADED
        )

    /**
     * Get session timeout in milliseconds from user preferences.
     * Returns Long.MAX_VALUE if auto-lock is disabled (never timeout),
     * 0 while the preference has not been loaded yet (forces re-auth).
     */
    private fun getSessionTimeoutMs(): Long {
        return when (val minutes = autoLockMinutes.value) {
            TIMEOUT_NOT_LOADED -> 0L
            0 -> Long.MAX_VALUE // 0 minutes = never auto-lock
            else -> minutes * 60 * 1000L
        }
    }

    // ==================== Session Management ====================

    /**
     * Start a new authenticated session.
     */
    fun startSession() {
        val now = System.currentTimeMillis()
        prefs.edit {
            putLong(KEY_SESSION_START, now)
            putLong(KEY_LAST_ACTIVITY, now)
        }
    }

    /**
     * Update last activity timestamp (call on user interactions).
     */
    @Suppress("unused") // Public API - available for activity tracking in MainActivity
    fun updateActivity() {
        prefs.edit {
            putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
        }
    }

    /**
     * Check if there's an active (non-expired) session.
     * Uses timeout from SecurityPreferences (user configurable).
     */
    fun hasActiveSession(): Boolean {
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        if (lastActivity == 0L) return false

        val elapsed = System.currentTimeMillis() - lastActivity
        val timeoutMs = getSessionTimeoutMs()

        return elapsed < timeoutMs
    }

    /**
     * End current session (on manual lock or app close).
     */
    @Suppress("unused") // Public API - available for manual lock feature
    fun endSession() {
        prefs.edit {
            remove(KEY_SESSION_START)
            remove(KEY_LAST_ACTIVITY)
        }
    }

    /**
     * Check if authentication should be required.
     * Returns true if no active session or session expired.
     */
    fun shouldRequireAuth(): Boolean {
        return !hasActiveSession()
    }

    // ==================== Failed Attempts ====================

    /**
     * Record a failed authentication attempt.
     * @return Remaining attempts before lockout
     */
    fun recordFailedAttempt(): Int {
        val current = getFailedAttempts()
        val newCount = current + 1
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, newCount)
        }
        return MAX_FAILED_ATTEMPTS - newCount
    }

    /**
     * Get current failed attempt count.
     */
    fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    /**
     * Reset failed attempts counter (after successful auth).
     */
    fun resetFailedAttempts() {
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
        }
    }

    // ==================== Lockout Management ====================

    /**
     * Start lockout period.
     * @param durationSeconds How long to lock out
     */
    fun startLockout(durationSeconds: Int) {
        val lockoutEnd = System.currentTimeMillis() + (durationSeconds * 1000L)
        prefs.edit {
            putLong(KEY_LOCKOUT_END, lockoutEnd)
        }
    }

    /**
     * Check if currently locked out.
     */
    fun isLockedOut(): Boolean {
        val lockoutEnd = prefs.getLong(KEY_LOCKOUT_END, 0L)
        if (lockoutEnd == 0L) return false

        val isLocked = System.currentTimeMillis() < lockoutEnd

        // Clear lockout if expired
        if (!isLocked) {
            clearLockout()
        }

        return isLocked
    }

    /**
     * Get remaining lockout time in seconds.
     */
    fun getRemainingLockoutSeconds(): Int {
        val lockoutEnd = prefs.getLong(KEY_LOCKOUT_END, 0L)
        if (lockoutEnd == 0L) return 0

        val remaining = lockoutEnd - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    /**
     * Clear lockout state.
     */
    fun clearLockout() {
        prefs.edit {
            remove(KEY_LOCKOUT_END)
            putInt(KEY_FAILED_ATTEMPTS, 0)
        }
    }

    /**
     * Clear all lock manager data (for testing or reset).
     */
    @Suppress("unused") // Public API - available for app reset functionality
    fun clearAll() {
        prefs.edit { clear() }
    }
}