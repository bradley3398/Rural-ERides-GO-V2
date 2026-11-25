package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_rides")
data class OfflineRideEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val vehicleName: String,
  val title: String,
  val durationSec: Long,
  val distanceKm: Double,
  val maxSpeedKmh: Double,
  val avgSpeedKmh: Double,
  val elevationGainM: Double,
  val whUsed: Double,
  val polylineCsv: String,
  val syncedToCloud: Boolean,
  val createdAtMs: Long,
)

@Entity(tableName = "copilot_sessions")
data class CopilotSessionEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val title: String,
  val persona: String,
  val updatedAtMs: Long,
)

@Entity(tableName = "copilot_messages")
data class CopilotMessageEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val isUser: Boolean,
  val text: String,
  val persona: String,
  val widgetTag: String?,
  val timestampMs: Long,
)

@Dao
interface OfflineRideDao {
  @Query("SELECT * FROM offline_rides WHERE userId = :userId ORDER BY createdAtMs DESC")
  fun observeRides(userId: String): Flow<List<OfflineRideEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(ride: OfflineRideEntity)

  @Query("DELETE FROM offline_rides WHERE userId = :userId")
  suspend fun clearUserRides(userId: String)
}

@Dao
interface CopilotChatDao {
  @Query("SELECT * FROM copilot_sessions WHERE userId = :userId ORDER BY updatedAtMs DESC")
  fun observeSessions(userId: String): Flow<List<CopilotSessionEntity>>

  @Query("SELECT * FROM copilot_messages WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
  fun observeMessages(sessionId: String): Flow<List<CopilotMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(session: CopilotSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(msg: CopilotMessageEntity)

  @Query("DELETE FROM copilot_messages WHERE sessionId = :sessionId")
  suspend fun deleteSessionMessages(sessionId: String)

  @Query("DELETE FROM copilot_sessions WHERE id = :sessionId")
  suspend fun deleteSession(sessionId: String)
}

@Database(
  entities = [OfflineRideEntity::class, CopilotSessionEntity::class, CopilotMessageEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun offlineRideDao(): OfflineRideDao
  abstract fun copilotChatDao(): CopilotChatDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "rural_erides_vault.db",
              )
              .fallbackToDestructiveMigration()
              .build()
              .also { instance = it }
        }
  }
}
