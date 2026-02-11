package com.anonforge.domain.model

/**
 * Domain model for alias quota information.
 * Extended for Skill 17 with low quota warning.
 */
data class AliasQuota(
    val isPremium: Boolean,
    val totalAllowed: Int,
    val used: Int,
    val remaining: Int
) {
    /**
     * True if user has reached their alias limit.
     */
    @Suppress("unused") // Public API for UI limit checks
    val isAtLimit: Boolean
        get() = !isPremium && remaining <= 0
    
    /**
     * True if quota is low (< 2 remaining) - triggers warning.
     */
    val isLowQuota: Boolean
        get() = !isPremium && remaining in 1..2
    
    /**
     * Usage as percentage (0.0 to 1.0).
     */
    @Suppress("unused") // Public API for progress indicators
    val usagePercentage: Float
        get() = if (isPremium) 0f else (used.toFloat() / totalAllowed.coerceAtLeast(1))
    
    /**
     * Display text for UI: "X / Y" or "Unlimited".
     */
    val displayText: String
        get() = if (isPremium) "Unlimited" else "$used / $totalAllowed"
    
    /**
     * Warning message if quota is low, null otherwise.
     */
    val warningMessage: String?
        get() = when {
            isAtLimit -> "Alias limit reached"
            isLowQuota -> "Low quota: $remaining alias${if (remaining > 1) "es" else ""} remaining"
            else -> null
        }
    
    /**
     * Localized display for Settings screen.
     */
    @Suppress("unused") // Public API for settings display
    val quotaStatusText: String
        get() = when {
            isPremium -> "Unlimited (Premium)"
            isAtLimit -> "$used / $totalAllowed (Limit reached)"
            isLowQuota -> "$used / $totalAllowed (Low)"
            else -> "$used / $totalAllowed aliases used"
        }
}
