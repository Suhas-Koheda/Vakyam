package dev.haas.vakya.ai

import android.util.Log
import dev.haas.vakya.data.google.CalendarApi
import dev.haas.vakya.data.google.CalendarEvent
import dev.haas.vakya.data.google.CalendarTime
import dev.haas.vakya.domain.models.ExtractedEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AgentDecisionLayer(
    private val calendarApi: CalendarApi
) {
    private val TAG = "AgentDecisionLayer"

    suspend fun decideAndAct(
        accountEmail: String,
        accessToken: String,
        calendarId: String,
        extracted: ExtractedEvent
    ): ResultConfig {
        val authHeader = "Bearer $accessToken"
        
        if (extracted.type == "ignore" || extracted.confidence < 0.7) {
            return ResultConfig("Ignored: Low confidence or irrelevant type.", null, "", "")
        }

        // Tool: list_calendar_events
        val existingEvents = try {
            calendarApi.listEvents(
                calendarId = calendarId,
                timeMin = getCurrentIsoTimestamp(),
                timeMax = getFutureIsoTimestamp(7), // Check next 7 days
                authHeader = authHeader
            ).items ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list events", e)
            emptyList()
        }

        // Logic: avoid duplicates
        val duplicate = existingEvents.find { it.summary.contains(extracted.title, ignoreCase = true) }
        if (duplicate != null) {
            return ResultConfig("Skipped: Duplicate event '${extracted.title}' found.", null, "", "")
        }

        // The event is put in the review queue rather than immediately created.
        val startTime = extracted.start_time ?: extracted.deadline ?: getCurrentIsoTimestamp()
        val endTime = extracted.end_time ?: startTime
        
        return ResultConfig(
            actionMessage = "Queued event: ${extracted.title}",
            extractedEvent = extracted,
            startTimeIso = startTime,
            endTimeIso = endTime
        )
    }

    data class ResultConfig(
        val actionMessage: String,
        val extractedEvent: ExtractedEvent?,
        val startTimeIso: String,
        val endTimeIso: String
    )

    private fun getCurrentIsoTimestamp(): String {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun getFutureIsoTimestamp(days: Long): String {
        return ZonedDateTime.now().plusDays(days).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
