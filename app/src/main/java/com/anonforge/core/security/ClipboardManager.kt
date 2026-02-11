package com.anonforge.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure clipboard manager with auto-clear functionality.
 * Clears sensitive data from clipboard after timeout.
 */
@Singleton
class SecureClipboardManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clearTimer: CountDownTimer? = null

    companion object {
        const val CLIPBOARD_CLEAR_DELAY_MS = 30_000L // 30 seconds
        private const val CLIP_LABEL = "AnonForge"
    }

    /**
     * Copy text to clipboard with auto-clear after 30 seconds.
     * @param text The text to copy
     * @param isSensitive If true, marks as sensitive (Android 13+) and schedules auto-clear
     */
    fun copyToClipboard(text: String, isSensitive: Boolean = true) {
        // Cancel any pending clear
        clearTimer?.cancel()

        val clip = ClipData.newPlainText(CLIP_LABEL, text)

        // Mark as sensitive on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSensitive) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }

        clipboardManager.setPrimaryClip(clip)

        // Schedule auto-clear for sensitive data
        if (isSensitive) {
            scheduleClipboardClear()
        }
    }

    /**
     * Copy multiple fields as formatted block.
     * @param fields Map of label to value (e.g., "Name" to "John Doe")
     */
    fun copyFormattedBlock(fields: Map<String, String>) {
        val formatted = fields.entries
            .filter { it.value.isNotBlank() }
            .joinToString("\n") { "${it.key}: ${it.value}" }

        copyToClipboard(formatted, isSensitive = true)
    }

    /**
     * Clear clipboard immediately.
     * Note: clearPrimaryClip() available since API 28 (P), minSdk is 29.
     */
    fun clearClipboard() {
        clearTimer?.cancel()
        clipboardManager.clearPrimaryClip()
    }

    /**
     * Schedule clipboard clear after delay.
     */
    private fun scheduleClipboardClear() {
        clearTimer?.cancel()

        clearTimer = object : CountDownTimer(CLIPBOARD_CLEAR_DELAY_MS, CLIPBOARD_CLEAR_DELAY_MS) {
            override fun onTick(millisUntilFinished: Long) {
                // No intermediate ticks needed
            }

            override fun onFinish() {
                clearClipboard()
            }
        }.start()
    }

    /**
     * Cancel any pending clipboard clear.
     * Call this when user navigates away or app goes to background.
     */
    fun cancelPendingClear() {
        clearTimer?.cancel()
        clearTimer = null
    }
}