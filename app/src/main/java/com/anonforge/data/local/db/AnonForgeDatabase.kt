package com.anonforge.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anonforge.data.local.dao.AliasHistoryDao
import com.anonforge.data.local.dao.PhoneAliasDao
import com.anonforge.data.local.entity.AliasHistoryEntity
import com.anonforge.data.local.entity.PhoneAliasEntity
import com.anonforge.security.encryption.KeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        IdentityEntity::class,
        AliasHistoryEntity::class,
        PhoneAliasEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AnonForgeDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao
    abstract fun aliasHistoryDao(): AliasHistoryDao
    abstract fun phoneAliasDao(): PhoneAliasDao

    companion object {
        private const val DATABASE_NAME = "anonforge.db"

        fun create(context: Context, keyManager: KeyManager): AnonForgeDatabase {
            val passphrase = keyManager.getDatabasePassphrase()
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))

            return Room.databaseBuilder(
                context.applicationContext,
                AnonForgeDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        // Migration 1→2: Add gender column to identities
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE identities ADD COLUMN gender TEXT NOT NULL DEFAULT 'MALE'")
            }
        }

        // Migration 2→3: Add alias_history table for alias reuse feature
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alias_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        email TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastUsedAt INTEGER NOT NULL,
                        useCount INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_alias_history_email 
                    ON alias_history (email)
                """.trimIndent())
            }
        }

        // Migration 3→4: Add customName column for identity renaming (Skill 13)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE identities ADD COLUMN customName TEXT DEFAULT NULL")
            }
        }

        // Migration 4→5: Add phone_alias_history table for Twilio phone aliases (Skill 16)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS phone_alias_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phone_number TEXT NOT NULL,
                        friendly_name TEXT NOT NULL,
                        twilio_sid TEXT NOT NULL,
                        is_primary INTEGER NOT NULL DEFAULT 0,
                        usage_count INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        last_used_at INTEGER
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_phone_alias_history_phone_number 
                    ON phone_alias_history (phone_number)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_phone_alias_history_is_primary 
                    ON phone_alias_history (is_primary)
                """.trimIndent())
            }
        }
    }
}
