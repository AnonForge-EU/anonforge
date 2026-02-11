package com.anonforge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing previously used email aliases.
 * Extended for Skill 17: Real Alias Integration.
 *
 * Max 50 entries, oldest auto-deleted when limit reached.
 * Stored in SQLCipher encrypted database.
 *
 * Skill 17 additions:
 * - tag: User-defined label for easy identification
 * - enabled: Whether alias is active on SimpleLogin
 * - isPrimary: Marks default alias for auto-reuse
 * - simpleLoginId: Remote ID for sync operations
 *
 * NOTE: Maintains backward compatibility with v5 schema.
 * New columns have defaults, no migration needed.
 */
@Entity(
    tableName = "alias_history",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["isPrimary"]),
        Index(value = ["lastUsedAt"])
    ]
)
data class AliasHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The alias email address (e.g., abc123@simplelogin.com) */
    val email: String,

    /** Timestamp when alias was first created/imported */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp of last use in identity generation */
    val lastUsedAt: Long = System.currentTimeMillis(),

    /** Number of times this alias was used */
    val useCount: Int = 1,

    // ═══════════════════════════════════════════════════════════════════════════
    // SKILL 17: Extended fields (with defaults for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Optional user-defined tag/label for identification */
    @ColumnInfo(defaultValue = "")
    val tag: String = "",

    /** Whether the alias is enabled on SimpleLogin (can receive emails) */
    @ColumnInfo(defaultValue = "1")
    val enabled: Boolean = true,

    /** Marks this as the primary/default alias for auto-reuse */
    @ColumnInfo(defaultValue = "0")
    val isPrimary: Boolean = false,

    /** SimpleLogin alias ID for sync operations (null if manually added) */
    @ColumnInfo(defaultValue = "NULL")
    val simpleLoginId: Int? = null
) {
    companion object {
        /** Maximum number of aliases to keep in history */
        const val MAX_HISTORY_SIZE = 50
    }

    /**
     * Display name for UI - shows tag if available, otherwise truncated email prefix.
     */
    @Suppress("unused") // Public API for alias history list UI display
    val displayName: String
        get() = tag.ifBlank {
            val prefix = email.substringBefore("@")
            if (prefix.length > 12) "${prefix.take(10)}…" else prefix
        }

    /**
     * Formatted display showing usage count.
     */
    @Suppress("unused") // Public API for alias history stats display
    val usageDisplay: String
        get() = when (useCount) {
            1 -> "Used once"
            else -> "Used $useCount times"
        }

    /**
     * Domain part of email (e.g., "simplelogin.com").
     */
    val domain: String
        get() = email.substringAfter("@", "")
}