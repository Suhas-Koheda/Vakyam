package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.AiActionLogDao
import dev.haas.vakya.data.database.CalendarEventDao
import dev.haas.vakya.data.database.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class DashboardRepository(
    private val calendarEventDao: CalendarEventDao,
    private val aiActionLogDao: AiActionLogDao
) {
    fun getTodayEvents(): Flow<List<CalendarEventEntity>> {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return calendarEventDao.getTodayEvents(startOfDay, endOfDay)
    }

    fun getUpcomingEvents(): Flow<List<CalendarEventEntity>> {
        val fromTime = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toTime = LocalDate.now().plusDays(8).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return calendarEventDao.getUpcomingEvents(fromTime, toTime)
    }

    fun getRecentAiActions() = aiActionLogDao.getRecentLogs()

    suspend fun markAsIgnored(eventId: Long) = calendarEventDao.markAsIgnored(eventId)

    suspend fun deleteEvent(event: CalendarEventEntity) = calendarEventDao.deleteEvent(event)

    suspend fun clearLogs() = aiActionLogDao.deleteAllLogs()
}
