package com.anonforge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for phone aliases.
 *
 * Stores user-entered virtual phone numbers for reuse in identity generation.
 *
 * NOTE: Table name is `phone_alias_history` for backward compatibility.
 * The `twilio_sid` column is kept but unused (always empty string).
 */
@Entity(
    tableName = "phone_alias_history",
    indices = [
        Index(value = ["phone_number"], unique = true),
        Index(value = ["is_primary"])
    ]
)
data class PhoneAliasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "friendly_name")
    val friendlyName: String = "",

    /**
     * LEGACY: Kept for backward compatibility with migration 4→5.
     * Always empty string in manual input mode.
     */
    @ColumnInfo(name = "twilio_sid")
    val twilioSid: String = "",

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null
)