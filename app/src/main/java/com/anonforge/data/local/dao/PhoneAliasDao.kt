package com.anonforge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anonforge.data.local.entity.PhoneAliasEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for phone alias operations.
 *
 * Table: phone_alias_history (legacy name for backward compatibility)
 */
@Dao
interface PhoneAliasDao {

    /**
     * Get all phone aliases ordered by primary first, then by usage count.
     */
    @Query("SELECT * FROM phone_alias_history ORDER BY is_primary DESC, usage_count DESC, created_at DESC")
    fun getAllAliases(): Flow<List<PhoneAliasEntity>>

    /**
     * Get the primary phone alias.
     */
    @Query("SELECT * FROM phone_alias_history WHERE is_primary = 1 LIMIT 1")
    suspend fun getPrimaryAlias(): PhoneAliasEntity?

    /**
     * Get a phone alias by ID.
     */
    @Query("SELECT * FROM phone_alias_history WHERE id = :id")
    suspend fun getById(id: Long): PhoneAliasEntity?

    /**
     * Check if a phone number already exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM phone_alias_history WHERE phone_number = :phoneNumber)")
    suspend fun existsByPhoneNumber(phoneNumber: String): Boolean

    /**
     * Insert a new phone alias.
     * @return The inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhoneAliasEntity): Long

    /**
     * Update an existing phone alias.
     */
    @Update
    suspend fun update(entity: PhoneAliasEntity)

    /**
     * Delete a phone alias by ID.
     */
    @Query("DELETE FROM phone_alias_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all phone aliases.
     */
    @Query("DELETE FROM phone_alias_history")
    suspend fun deleteAll()

    /**
     * Clear primary flag from all aliases.
     */
    @Query("UPDATE phone_alias_history SET is_primary = 0")
    suspend fun clearAllPrimary()

    /**
     * Set a specific alias as primary.
     */
    @Query("UPDATE phone_alias_history SET is_primary = 1 WHERE id = :id")
    suspend fun setPrimary(id: Long)

    /**
     * Increment usage count and update last used timestamp.
     */
    @Query("UPDATE phone_alias_history SET usage_count = usage_count + 1, last_used_at = :timestamp WHERE id = :id")
    suspend fun recordUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Get total count of aliases.
     */
    @Query("SELECT COUNT(*) FROM phone_alias_history")
    suspend fun getCount(): Int
}