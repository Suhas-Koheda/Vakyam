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

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emailId: String,
    val subject: String,
    val aiOutput: String, // JSON
    val actionTaken: String,
    val timestamp: Long = System.currentTimeMillis()
)


@Dao
interface AccountDao {
    @Query("SELECT * FROM google_accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM google_accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface ProcessedEmailDao {
    @Query("SELECT EXISTS(SELECT 1 FROM processed_emails WHERE id = :emailId)")
    suspend fun isProcessed(emailId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markProcessed(processedEmail: ProcessedEmailEntity)

    @Insert
    suspend fun insertActionLog(log: ActionLogEntity)

    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<ActionLogEntity>>
}

@Database(entities = [AccountEntity::class, ProcessedEmailEntity::class, ActionLogEntity::class], version = 2)
abstract class VakyaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun processedEmailDao(): ProcessedEmailDao
}

