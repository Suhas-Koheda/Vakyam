package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.pendingEvents.PendingEvent
import dev.haas.vakya.data.database.pendingEvents.PendingEventDao
import kotlinx.coroutines.flow.Flow

class PendingEventRepository(
    private val pendingEventDao: PendingEventDao
) {
    fun getPendingEvents(): Flow<List<PendingEvent>> {
        return pendingEventDao.getPendingEvents()
    }

    suspend fun insertEvent(event: PendingEvent): Long {
        return pendingEventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: PendingEvent) {
        pendingEventDao.updateEvent(event)
    }

    suspend fun approveEvent(id: Long) {
        pendingEventDao.updateStatus(id, "approved")
    }

    suspend fun rejectEvent(id: Long) {
        pendingEventDao.updateStatus(id, "rejected")
    }

    suspend fun getEventById(id: Long): PendingEvent? {
        return pendingEventDao.getEventById(id)
    }

    suspend fun deleteEvent(event: PendingEvent) {
        pendingEventDao.deleteEvent(event)
    }
}
