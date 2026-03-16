package dev.haas.vakya.data.database.pendingEvents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEventDao {
    @Query("SELECT * FROM pending_events WHERE status = 'pending' ORDER BY createdAt DESC")
    fun getPendingEvents(): Flow<List<PendingEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PendingEvent): Long

    @Update
    suspend fun updateEvent(event: PendingEvent)

    @Query("UPDATE pending_events SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE pending_events SET originalBody = NULL WHERE id = :id")
    suspend fun clearBody(id: Long)

    @Query("SELECT * FROM pending_events WHERE id = :id")
    suspend fun getEventById(id: Long): PendingEvent?

    @androidx.room.Delete
    suspend fun deleteEvent(event: PendingEvent)
}
