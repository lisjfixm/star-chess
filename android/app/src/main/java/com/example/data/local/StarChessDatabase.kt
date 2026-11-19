package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "storage_entries")
data class StorageEntryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_snapshots")
data class LocalSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val schemaVersion: String,
    val payloadJson: String,
    val starCoins: Int,
    val eloPeak: Int,
    val gamesPlayed: Int,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_login_reward")
data class DailyLoginRewardEntity(
    @PrimaryKey val profileKey: String = "default",
    val lastLoginDate: String,
    val consecutiveDays: Int = 1,
    val totalClaims: Int = 1,
    val lastRewardCoins: Int = 50,
    val updatedAtMs: Long = System.currentTimeMillis(),
)

@Dao
interface StarChessDao {
    @Query("SELECT * FROM storage_entries ORDER BY `key` ASC")
    fun observeEntries(): Flow<List<StorageEntryEntity>>

    @Query("SELECT * FROM storage_entries ORDER BY `key` ASC")
    suspend fun getAllEntries(): List<StorageEntryEntity>

    @Query("SELECT * FROM storage_entries WHERE `key` = :key LIMIT 1")
    suspend fun getEntryByKey(key: String): StorageEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: StorageEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<StorageEntryEntity>)

    @Query("DELETE FROM storage_entries WHERE `key` = :key")
    suspend fun deleteEntry(key: String)

    @Query("DELETE FROM storage_entries")
    suspend fun clearAllEntries()

    @Transaction
    suspend fun replaceAllEntries(entries: List<StorageEntryEntity>) {
        clearAllEntries()
        if (entries.isNotEmpty()) {
            upsertEntries(entries)
        }
    }

    @Query("SELECT * FROM local_snapshots ORDER BY createdAtMs DESC LIMIT 20")
    fun observeSnapshots(): Flow<List<LocalSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: LocalSnapshotEntity)

    @Query("SELECT * FROM local_snapshots WHERE id = :snapshotId LIMIT 1")
    suspend fun getSnapshotById(snapshotId: Long): LocalSnapshotEntity?

    @Query("DELETE FROM local_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshot(snapshotId: Long)

    @Query("SELECT * FROM daily_login_reward WHERE profileKey = :profileKey LIMIT 1")
    fun observeDailyReward(profileKey: String = "default"): Flow<DailyLoginRewardEntity?>

    @Query("SELECT * FROM daily_login_reward WHERE profileKey = :profileKey LIMIT 1")
    suspend fun getDailyReward(profileKey: String = "default"): DailyLoginRewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyReward(entity: DailyLoginRewardEntity)
}

@Database(
    entities = [
        StorageEntryEntity::class,
        LocalSnapshotEntity::class,
        DailyLoginRewardEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class StarChessDatabase : RoomDatabase() {
    abstract fun starChessDao(): StarChessDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_login_reward` (
                        `profileKey` TEXT NOT NULL,
                        `lastLoginDate` TEXT NOT NULL,
                        `consecutiveDays` INTEGER NOT NULL,
                        `totalClaims` INTEGER NOT NULL,
                        `lastRewardCoins` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`profileKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: StarChessDatabase? = null

        fun getInstance(context: Context): StarChessDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StarChessDatabase::class.java,
                    "star_chess_local.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
