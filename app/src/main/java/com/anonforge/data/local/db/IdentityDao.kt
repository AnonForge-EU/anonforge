package com.anonforge.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: IdentityEntity)

    @Query("SELECT * FROM identities ORDER BY createdAt DESC")
    fun getAllIdentitiesFlow(): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun getIdentityById(id: String): IdentityEntity?

    @Query("DELETE FROM identities WHERE id = :id")
    suspend fun deleteIdentity(id: String)

    @Query("SELECT * FROM identities WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun getExpiredIdentities(currentTime: Long): List<IdentityEntity>

    @Query("DELETE FROM identities WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpiredIdentities(currentTime: Long)

    // NEW: Update custom name for identity renaming (Skill 13)
    @Query("UPDATE identities SET customName = :customName WHERE id = :id")
    suspend fun updateCustomName(id: String, customName: String?)
}