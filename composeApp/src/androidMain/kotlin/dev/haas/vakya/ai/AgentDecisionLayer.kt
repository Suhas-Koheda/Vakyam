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
        extracted: ExtractedEvent
    ): String {
        val authHeader = "Bearer $accessToken"
        
        if (extracted.type == "ignore" || extracted.confidence < 0.7) {
            return "Ignored: Low confidence or irrelevant type."
        }

        // Tool: list_calendar_events
        val existingEvents = try {
            calendarApi.listEvents(
                calendarId = "primary",
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
            return "Skipped: Duplicate event '${extracted.title}' found."
        }

        // Tool: create_calendar_event
        return try {
            val startTime = extracted.start_time ?: extracted.deadline ?: getCurrentIsoTimestamp()
            val endTime = extracted.end_time ?: startTime // Simple fallback
            
            val newEvent = CalendarEvent(
                summary = extracted.title,
                description = "Added by Vakya AI Agent. \nCourse: ${extracted.course}\n${extracted.description}",
                start = CalendarTime(dateTime = startTime),
                end = CalendarTime(dateTime = endTime)
            )
            
            calendarApi.createEvent(calendarId = "primary", event = newEvent, authHeader = authHeader)
            "Created event: ${extracted.title}"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create event", e)
            "Error creating event: ${e.localizedMessage}"
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun getFutureIsoTimestamp(days: Long): String {
        return ZonedDateTime.now().plusDays(days).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
