package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

data class OfflineRideEntity(
  val id: String,
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
  val vehicleId: String = "",
  val startBatteryPct: Int = -1,
  val endBatteryPct: Int = -1,
  val weatherSummary: String = "",
  val aiDebrief: String = "",
)

data class CopilotSessionEntity(
  val id: String,
  val userId: String,
  val title: String,
  val persona: String,
  val updatedAtMs: Long,
)

data class CopilotMessageEntity(
  val id: String,
  val sessionId: String,
  val isUser: Boolean,
  val text: String,
  val persona: String,
  val widgetTag: String?,
  val timestampMs: Long,
)

interface OfflineRideDao {
  fun observeRides(userId: String): Flow<List<OfflineRideEntity>>

  suspend fun upsert(ride: OfflineRideEntity)

  suspend fun deleteRide(rideId: String)

  suspend fun updateAiDebrief(rideId: String, aiDebrief: String)

  suspend fun clearUserRides(userId: String)
}

interface CopilotChatDao {
  fun observeSessions(userId: String): Flow<List<CopilotSessionEntity>>

  fun observeMessages(sessionId: String): Flow<List<CopilotMessageEntity>>

  suspend fun upsertSession(session: CopilotSessionEntity)

  suspend fun insertMessage(msg: CopilotMessageEntity)

  suspend fun deleteSessionMessages(sessionId: String)

  suspend fun deleteSession(sessionId: String)
}

/**
 * Lightweight SQLite-backed vault keeping exact table/column compatibility with
 * rural_erides_vault.db without requiring KSP/Room annotation processing at build time.
 */
class AppDatabase private constructor(context: Context) {

  private val dbHelper = VaultSqliteHelper(context.applicationContext)
  private val ridesChanged =
    MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  private val sessionsChanged =
    MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  private val messagesChanged =
    MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  private val offlineRideDaoImpl =
    object : OfflineRideDao {
      override fun observeRides(userId: String): Flow<List<OfflineRideEntity>> =
        ridesChanged
          .onStart { emit(Unit) }
          .map { queryRides(userId) }
          .flowOn(Dispatchers.IO)

      override suspend fun upsert(ride: OfflineRideEntity) {
        withContext(Dispatchers.IO) {
          val values =
            ContentValues().apply {
              put("id", ride.id)
              put("userId", ride.userId)
              put("vehicleName", ride.vehicleName)
              put("title", ride.title)
              put("durationSec", ride.durationSec)
              put("distanceKm", ride.distanceKm)
              put("maxSpeedKmh", ride.maxSpeedKmh)
              put("avgSpeedKmh", ride.avgSpeedKmh)
              put("elevationGainM", ride.elevationGainM)
              put("whUsed", ride.whUsed)
              put("polylineCsv", ride.polylineCsv)
              put("syncedToCloud", if (ride.syncedToCloud) 1 else 0)
              put("createdAtMs", ride.createdAtMs)
              put("vehicleId", ride.vehicleId)
              put("startBatteryPct", ride.startBatteryPct)
              put("endBatteryPct", ride.endBatteryPct)
              put("weatherSummary", ride.weatherSummary)
              put("aiDebrief", ride.aiDebrief)
            }
          dbHelper.writableDatabase.insertWithOnConflict(
            "offline_rides",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
          )
        }
        ridesChanged.tryEmit(Unit)
      }

      override suspend fun deleteRide(rideId: String) {
        withContext(Dispatchers.IO) {
          dbHelper.writableDatabase.delete("offline_rides", "id = ?", arrayOf(rideId))
        }
        ridesChanged.tryEmit(Unit)
      }

      override suspend fun updateAiDebrief(rideId: String, aiDebrief: String) {
        withContext(Dispatchers.IO) {
          val values = ContentValues().apply { put("aiDebrief", aiDebrief) }
          dbHelper.writableDatabase.update("offline_rides", values, "id = ?", arrayOf(rideId))
        }
        ridesChanged.tryEmit(Unit)
      }

      override suspend fun clearUserRides(userId: String) {
        withContext(Dispatchers.IO) {
          dbHelper.writableDatabase.delete("offline_rides", "userId = ?", arrayOf(userId))
        }
        ridesChanged.tryEmit(Unit)
      }
    }

  private val copilotChatDaoImpl =
    object : CopilotChatDao {
      override fun observeSessions(userId: String): Flow<List<CopilotSessionEntity>> =
        sessionsChanged
          .onStart { emit(Unit) }
          .map { querySessions(userId) }
          .flowOn(Dispatchers.IO)

      override fun observeMessages(sessionId: String): Flow<List<CopilotMessageEntity>> =
        messagesChanged
          .onStart { emit(Unit) }
          .map { queryMessages(sessionId) }
          .flowOn(Dispatchers.IO)

      override suspend fun upsertSession(session: CopilotSessionEntity) {
        withContext(Dispatchers.IO) {
          val values =
            ContentValues().apply {
              put("id", session.id)
              put("userId", session.userId)
              put("title", session.title)
              put("persona", session.persona)
              put("updatedAtMs", session.updatedAtMs)
            }
          dbHelper.writableDatabase.insertWithOnConflict(
            "copilot_sessions",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
          )
        }
        sessionsChanged.tryEmit(Unit)
      }

      override suspend fun insertMessage(msg: CopilotMessageEntity) {
        withContext(Dispatchers.IO) {
          val values =
            ContentValues().apply {
              put("id", msg.id)
              put("sessionId", msg.sessionId)
              put("isUser", if (msg.isUser) 1 else 0)
              put("text", msg.text)
              put("persona", msg.persona)
              if (msg.widgetTag == null) putNull("widgetTag") else put("widgetTag", msg.widgetTag)
              put("timestampMs", msg.timestampMs)
            }
          dbHelper.writableDatabase.insertWithOnConflict(
            "copilot_messages",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
          )
        }
        messagesChanged.tryEmit(Unit)
      }

      override suspend fun deleteSessionMessages(sessionId: String) {
        withContext(Dispatchers.IO) {
          dbHelper.writableDatabase.delete("copilot_messages", "sessionId = ?", arrayOf(sessionId))
        }
        messagesChanged.tryEmit(Unit)
      }

      override suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) {
          dbHelper.writableDatabase.delete("copilot_sessions", "id = ?", arrayOf(sessionId))
        }
        sessionsChanged.tryEmit(Unit)
      }
    }

  fun offlineRideDao(): OfflineRideDao = offlineRideDaoImpl

  fun copilotChatDao(): CopilotChatDao = copilotChatDaoImpl

  private fun queryRides(userId: String): List<OfflineRideEntity> {
    val result = mutableListOf<OfflineRideEntity>()
    dbHelper.readableDatabase
      .rawQuery(
        "SELECT id, userId, vehicleName, title, durationSec, distanceKm, maxSpeedKmh, avgSpeedKmh, elevationGainM, whUsed, polylineCsv, syncedToCloud, createdAtMs, vehicleId, startBatteryPct, endBatteryPct, weatherSummary, aiDebrief FROM offline_rides WHERE userId = ? ORDER BY createdAtMs DESC",
        arrayOf(userId),
      )
      .use { c ->
        while (c.moveToNext()) {
          result.add(
            OfflineRideEntity(
              id = c.getString(0),
              userId = c.getString(1),
              vehicleName = c.getString(2),
              title = c.getString(3),
              durationSec = c.getLong(4),
              distanceKm = c.getDouble(5),
              maxSpeedKmh = c.getDouble(6),
              avgSpeedKmh = c.getDouble(7),
              elevationGainM = c.getDouble(8),
              whUsed = c.getDouble(9),
              polylineCsv = c.getString(10),
              syncedToCloud = c.getInt(11) != 0,
              createdAtMs = c.getLong(12),
              vehicleId = if (c.isNull(13)) "" else c.getString(13),
              startBatteryPct = if (c.isNull(14)) -1 else c.getInt(14),
              endBatteryPct = if (c.isNull(15)) -1 else c.getInt(15),
              weatherSummary = if (c.isNull(16)) "" else c.getString(16),
              aiDebrief = if (c.isNull(17)) "" else c.getString(17),
            )
          )
        }
      }
    return result
  }

  private fun querySessions(userId: String): List<CopilotSessionEntity> {
    val result = mutableListOf<CopilotSessionEntity>()
    dbHelper.readableDatabase
      .rawQuery(
        "SELECT id, userId, title, persona, updatedAtMs FROM copilot_sessions WHERE userId = ? ORDER BY updatedAtMs DESC",
        arrayOf(userId),
      )
      .use { c ->
        while (c.moveToNext()) {
          result.add(
            CopilotSessionEntity(
              id = c.getString(0),
              userId = c.getString(1),
              title = c.getString(2),
              persona = c.getString(3),
              updatedAtMs = c.getLong(4),
            )
          )
        }
      }
    return result
  }

  private fun queryMessages(sessionId: String): List<CopilotMessageEntity> {
    val result = mutableListOf<CopilotMessageEntity>()
    dbHelper.readableDatabase
      .rawQuery(
        "SELECT id, sessionId, isUser, text, persona, widgetTag, timestampMs FROM copilot_messages WHERE sessionId = ? ORDER BY timestampMs ASC",
        arrayOf(sessionId),
      )
      .use { c ->
        while (c.moveToNext()) {
          result.add(
            CopilotMessageEntity(
              id = c.getString(0),
              sessionId = c.getString(1),
              isUser = c.getInt(2) != 0,
              text = c.getString(3),
              persona = c.getString(4),
              widgetTag = if (c.isNull(5)) null else c.getString(5),
              timestampMs = c.getLong(6),
            )
          )
        }
      }
    return result
  }

  private class VaultSqliteHelper(context: Context) :
    SQLiteOpenHelper(context, "rural_erides_vault.db", null, 2) {

    init {
      setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
      ensureTables(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
      super.onOpen(db)
      if (!db.isReadOnly) {
        ensureTables(db)
      }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      ensureTables(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      ensureTables(db)
    }

    private fun ensureTables(db: SQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS offline_rides (
          id TEXT NOT NULL PRIMARY KEY,
          userId TEXT NOT NULL,
          vehicleName TEXT NOT NULL,
          title TEXT NOT NULL,
          durationSec INTEGER NOT NULL,
          distanceKm REAL NOT NULL,
          maxSpeedKmh REAL NOT NULL,
          avgSpeedKmh REAL NOT NULL,
          elevationGainM REAL NOT NULL,
          whUsed REAL NOT NULL,
          polylineCsv TEXT NOT NULL,
          syncedToCloud INTEGER NOT NULL,
          createdAtMs INTEGER NOT NULL,
          vehicleId TEXT NOT NULL DEFAULT '',
          startBatteryPct INTEGER NOT NULL DEFAULT -1,
          endBatteryPct INTEGER NOT NULL DEFAULT -1,
          weatherSummary TEXT NOT NULL DEFAULT '',
          aiDebrief TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent()
      )
      val existingCols = mutableSetOf<String>()
      db.rawQuery("PRAGMA table_info(offline_rides)", null).use { c ->
        val nameIdx = c.getColumnIndex("name")
        while (c.moveToNext()) {
          if (nameIdx >= 0) existingCols += c.getString(nameIdx)
        }
      }
      val extraCols =
        listOf(
          "vehicleId" to "TEXT NOT NULL DEFAULT ''",
          "startBatteryPct" to "INTEGER NOT NULL DEFAULT -1",
          "endBatteryPct" to "INTEGER NOT NULL DEFAULT -1",
          "weatherSummary" to "TEXT NOT NULL DEFAULT ''",
          "aiDebrief" to "TEXT NOT NULL DEFAULT ''",
        )
      for ((col, def) in extraCols) {
        if (col !in existingCols) {
          db.execSQL("ALTER TABLE offline_rides ADD COLUMN $col $def")
        }
      }
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS copilot_sessions (
          id TEXT NOT NULL PRIMARY KEY,
          userId TEXT NOT NULL,
          title TEXT NOT NULL,
          persona TEXT NOT NULL,
          updatedAtMs INTEGER NOT NULL
        )
        """.trimIndent()
      )
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS copilot_messages (
          id TEXT NOT NULL PRIMARY KEY,
          sessionId TEXT NOT NULL,
          isUser INTEGER NOT NULL,
          text TEXT NOT NULL,
          persona TEXT NOT NULL,
          widgetTag TEXT,
          timestampMs INTEGER NOT NULL
        )
        """.trimIndent()
      )
    }
  }

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance ?: AppDatabase(context.applicationContext).also { instance = it }
        }
  }
}
