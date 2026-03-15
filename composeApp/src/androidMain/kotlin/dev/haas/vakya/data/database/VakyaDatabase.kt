package dev.haas.vakya.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "google_accounts")
data class AccountEntity(
    @PrimaryKey val email: String,
    val displayName: String?,
    val isGmailEnabled: Boolean,
    val targetCalendarId: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

@Entity(tableName = "processed_emails")
data class ProcessedEmailEntity(
    @PrimaryKey val id: String,
    val accountEmail: String,
    val processedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val source: String, // email, LMS, manual
    val accountEmail: String,
    val status: String, // ADDED, IGNORED, PENDING
    val externalId: String? = null, // Google Calendar Event ID
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_action_logs")
data class AiActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emailId: String? = null,
    val logType: String = "ACTION", // ACTION, AUTH, SYSTEM, ERROR
    val subject: String,
    val actionSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM google_accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM google_accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM google_accounts WHERE email = :email")
    suspend fun getAccountByEmail(email: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Delete
    suspend fun deleteAccount(account: AccountEntity): Int

    @Update
    suspend fun updateAccount(account: AccountEntity)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE startTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    fun getTodayEvents(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :fromTime AND startTime < :toTime ORDER BY startTime ASC")
    fun getUpcomingEvents(fromTime: Long, toTime: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :fromTime ORDER BY startTime ASC")
    suspend fun getUpcomingEventsList(fromTime: Long): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    suspend fun getTodayEventsList(startOfDay: Long, endOfDay: Long): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
    
    @Query("UPDATE calendar_events SET status = 'IGNORED' WHERE id = :eventId")
    suspend fun markAsIgnored(eventId: Long)
}

@Dao
interface AiActionLogDao {
    @Query("SELECT * FROM ai_action_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AiActionLogEntity>>

    @Insert
    suspend fun insertLog(log: AiActionLogEntity): Long

    @Query("DELETE FROM ai_action_logs")
    suspend fun deleteAllLogs()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>
}

@Dao
interface ProcessedEmailDao {
    @Query("SELECT EXISTS(SELECT 1 FROM processed_emails WHERE id = :emailId)")
    suspend fun isProcessed(emailId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markProcessed(processedEmail: ProcessedEmailEntity): Long
}

@Database(entities = [
    AccountEntity::class, 
    ProcessedEmailEntity::class, 
    CalendarEventEntity::class, 
    AiActionLogEntity::class, 
    AppSettingEntity::class,
    dev.haas.vakya.data.database.pendingEvents.PendingEvent::class,
    KnowledgeNoteEntity::class,
    AiLearningRuleEntity::class
], version = 6, exportSchema = false)
abstract class VakyaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun aiActionLogDao(): AiActionLogDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun processedEmailDao(): ProcessedEmailDao
    abstract fun pendingEventDao(): dev.haas.vakya.data.database.pendingEvents.PendingEventDao
    abstract fun knowledgeNoteDao(): dev.haas.vakya.data.dao.KnowledgeNoteDao
    abstract fun aiLearningRuleDao(): dev.haas.vakya.data.dao.AiLearningRuleDao
}


