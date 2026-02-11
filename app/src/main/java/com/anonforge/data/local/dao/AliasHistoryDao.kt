package com.anonforge.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anonforge.data.local.entity.AliasHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

/**
 * Data Access Object for alias history operations.
 * Extended for Skill 17: Real Alias Integration.
 * 
 * Manages local storage of used email aliases for quick reuse.
 * All operations are main-safe (Room handles threading).
 */
@Dao
interface AliasHistoryDao {

    // ═══════════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get recent aliases as Flow for reactive UI updates.
     * Limited to 10 most recently used.
     */
    @Query("SELECT * FROM alias_history ORDER BY lastUsedAt DESC LIMIT 10")
    fun getRecentAliases(): Flow<List<AliasHistoryEntity>>

    /**
     * Get all aliases as Flow for reactive UI (Settings).
     */
    @Query("SELECT * FROM alias_history ORDER BY lastUsedAt DESC")
    fun getAllAliasesFlow(): Flow<List<AliasHistoryEntity>>

    /**
     * Get recent aliases as list (for non-reactive use cases).
     */
    @Query("SELECT * FROM alias_history ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecentAliasesList(limit: Int): List<AliasHistoryEntity>

    /**
     * Get all aliases as list (for sync).
     */
    @Query("SELECT * FROM alias_history ORDER BY lastUsedAt DESC")
    suspend fun getAllAliasesList(): List<AliasHistoryEntity>

    /**
     * Get the primary alias for auto-reuse.
     */
    @Query("SELECT * FROM alias_history WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryAlias(): AliasHistoryEntity?

    /**
     * Get the first/oldest alias (fallback if no primary).
     */
    @Query("SELECT * FROM alias_history ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstAlias(): AliasHistoryEntity?

    /**
     * Find alias by email address.
     */
    @Query("SELECT * FROM alias_history WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AliasHistoryEntity?

    /**
     * Get alias by email (alias for findByEmail).
     */
    @Query("SELECT * FROM alias_history WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): AliasHistoryEntity?

    /**
     * Find alias by SimpleLogin ID.
     */
    @Query("SELECT * FROM alias_history WHERE simpleLoginId = :simpleLoginId LIMIT 1")
    suspend fun getBySimpleLoginId(simpleLoginId: Int): AliasHistoryEntity?

    /**
     * Search aliases by email or tag (case-insensitive).
     */
    @Query("""
        SELECT * FROM alias_history 
        WHERE email LIKE '%' || :query || '%' 
           OR tag LIKE '%' || :query || '%'
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """)
    suspend fun searchAliases(query: String, limit: Int = 20): List<AliasHistoryEntity>

    /**
     * Get total count of aliases in history.
     */
    @Query("SELECT COUNT(*) FROM alias_history")
    suspend fun getCount(): Int

    /**
     * Get count of enabled aliases only.
     */
    @Query("SELECT COUNT(*) FROM alias_history WHERE enabled = 1")
    suspend fun getEnabledCount(): Int

    // ═══════════════════════════════════════════════════════════════════════════
    // WRITE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Insert new alias entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alias: AliasHistoryEntity)

    /**
     * Insert multiple aliases (for sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aliases: List<AliasHistoryEntity>)

    /**
     * Update existing alias entry.
     */
    @Update
    suspend fun update(alias: AliasHistoryEntity)

    /**
     * Clear all primary flags.
     */
    @Query("UPDATE alias_history SET isPrimary = 0")
    suspend fun clearAllPrimaryFlags()

    /**
     * Set alias as primary by ID.
     */
    @Query("UPDATE alias_history SET isPrimary = 1 WHERE id = :aliasId")
    suspend fun setPrimaryById(aliasId: Long)

    /**
     * Update alias enabled status.
     */
    @Query("UPDATE alias_history SET enabled = :enabled WHERE email = :email")
    suspend fun setEnabled(email: String, enabled: Boolean)

    /**
     * Update alias tag.
     */
    @Query("UPDATE alias_history SET tag = :tag WHERE id = :aliasId")
    suspend fun setTag(aliasId: Long, tag: String)

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Delete specific alias by email.
     */
    @Query("DELETE FROM alias_history WHERE email = :email")
    suspend fun deleteByEmail(email: String)

    /**
     * Delete alias entity.
     */
    @Delete
    suspend fun delete(alias: AliasHistoryEntity)

    /**
     * Delete oldest entries (for pruning).
     */
    @Query("DELETE FROM alias_history WHERE id IN (SELECT id FROM alias_history ORDER BY lastUsedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    /**
     * Delete all alias history.
     */
    @Query("DELETE FROM alias_history")
    suspend fun deleteAll()
}

// ═══════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Upsert (insert or update) an alias with extended fields.
 * Updates lastUsedAt and increments useCount if exists.
 * 
 * @param email The alias email address
 * @param tag Optional user-defined label
 * @param simpleLoginId Optional remote ID
 * @param enabled Whether alias is active
 * @param setAsPrimary Whether to set as primary
 */
suspend fun AliasHistoryDao.upsertAlias(
    email: String,
    tag: String = "",
    simpleLoginId: Int? = null,
    enabled: Boolean = true,
    setAsPrimary: Boolean = false
) {
    val existing = findByEmail(email)
    val now = Clock.System.now().toEpochMilliseconds()

    if (existing != null) {
        // Update existing entry
        update(existing.copy(
            lastUsedAt = now,
            useCount = existing.useCount + 1,
            tag = tag.ifBlank { existing.tag },
            simpleLoginId = simpleLoginId ?: existing.simpleLoginId,
            enabled = enabled
        ))
        
        if (setAsPrimary && !existing.isPrimary) {
            clearAllPrimaryFlags()
            setPrimaryById(existing.id)
        }
    } else {
        // Prune if at max capacity
        val count = getCount()
        if (count >= AliasHistoryEntity.MAX_HISTORY_SIZE) {
            deleteOldest(count - AliasHistoryEntity.MAX_HISTORY_SIZE + 1)
        }

        // Clear primary if setting new as primary
        if (setAsPrimary) {
            clearAllPrimaryFlags()
        }

        insert(AliasHistoryEntity(
            email = email,
            tag = tag,
            createdAt = now,
            lastUsedAt = now,
            useCount = 1,
            enabled = enabled,
            isPrimary = setAsPrimary,
            simpleLoginId = simpleLoginId
        ))
    }
}

/**
 * Simple upsert (backward compatible).
 */
suspend fun AliasHistoryDao.upsertAlias(email: String) {
    upsertAlias(
        email = email,
        tag = "",
        simpleLoginId = null,
        enabled = true,
        setAsPrimary = false
    )
}

/**
 * Set a specific alias as primary.
 */
suspend fun AliasHistoryDao.setPrimaryAlias(aliasId: Long) {
    clearAllPrimaryFlags()
    setPrimaryById(aliasId)
}

/**
 * Get the default alias for auto-reuse.
 * Returns primary if set, otherwise first/oldest alias.
 */
suspend fun AliasHistoryDao.getDefaultAlias(): AliasHistoryEntity? {
    return getPrimaryAlias() ?: getFirstAlias()
}

/**
 * Record alias usage without changing other fields.
 */
suspend fun AliasHistoryDao.recordUsage(email: String) {
    val existing = findByEmail(email) ?: return
    val now = Clock.System.now().toEpochMilliseconds()
    update(existing.copy(
        lastUsedAt = now,
        useCount = existing.useCount + 1
    ))
}
