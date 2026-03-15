package dev.haas.vakya.domain.agent

import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.AiActionLogDao
import dev.haas.vakya.data.database.AiActionLogEntity
import dev.haas.vakya.data.database.CalendarEventDao
import dev.haas.vakya.data.database.CalendarEventEntity
import dev.haas.vakya.data.database.pendingEvents.PendingEvent
import dev.haas.vakya.data.google.CalendarApi
import dev.haas.vakya.data.google.CalendarEvent
import dev.haas.vakya.data.google.CalendarTime
import dev.haas.vakya.domain.deduplication.CalendarDeduplicationService

class ApprovePendingEventUseCase(
    private val accountDao: AccountDao,
    private val calendarApi: CalendarApi,
    private val deduplicationService: CalendarDeduplicationService,
    private val aiActionLogDao: AiActionLogDao,
    private val calendarEventDao: CalendarEventDao,
    private val notificationManager: dev.haas.vakya.notifications.VakyaNotificationManager? = null
) {
    suspend fun execute(event: PendingEvent) {
        val account = accountDao.getAccountByEmail(event.accountId)
        if (account?.accessToken == null) {
            logAction(event.emailId, event.title, "Failed approval: Account or token not found.")
            return
        }

        val calendarId = event.targetCalendarId ?: account.targetCalendarId.ifEmpty { "primary" }
        val authHeader = "Bearer ${account.accessToken}"

        // Check for duplicates
        val isDuplicate = deduplicationService.isDuplicateEvent(
            calendarId = calendarId,
            authHeader = authHeader,
            title = event.title,
            startTimeIso = event.startTime,
            endTimeIso = event.endTime
        )

        if (isDuplicate) {
            logAction(event.emailId, event.title, "Duplicate event detected — skipped creation")
            return
        }

        try {
            val newEvent = CalendarEvent(
                summary = event.title,
                description = "Added by Vakya AI Agent (Approved).\n${event.description}",
                start = CalendarTime(dateTime = event.startTime),
                end = CalendarTime(dateTime = event.endTime ?: event.startTime)
            )

            calendarApi.createEvent(calendarId, newEvent, authHeader)
            
            // Log success + add to local generic events table if needed
            logAction(event.emailId, event.title, "Successfully created event: ${event.title}")
            
            calendarEventDao.insertEvent(
                CalendarEventEntity(
                    title = event.title,
                    startTime = parseIsoToLong(event.startTime),
                    endTime = event.endTime?.let { parseIsoToLong(it) },
                    source = "AI Agent (${event.emailId})",
                    accountEmail = account.email,
                    status = "ADDED"
                )
            )
            notificationManager?.notifyEventApproved(event.title)
        } catch (e: Exception) {
            logAction(event.emailId, event.title, "Error creating event: ${e.localizedMessage}")
        }
    }

    private suspend fun logAction(emailId: String?, subject: String, result: String) {
        aiActionLogDao.insertLog(
            AiActionLogEntity(
                emailId = emailId,
                subject = subject,
                actionSummary = result
            )
        )
    }

    private fun parseIsoToLong(iso: String?): Long {
        if (iso == null) return System.currentTimeMillis()
        return try {
            java.time.ZonedDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
