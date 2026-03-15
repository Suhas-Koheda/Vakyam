package dev.haas.vakya.domain.ai

import dev.haas.vakya.ai.GemmaParser
import dev.haas.vakya.data.database.CalendarEventDao
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class WeeklySummaryUseCase(
    private val calendarEventDao: CalendarEventDao,
    private val gemmaParser: GemmaParser
) {
    suspend fun generateWeeklySummary(): String {
        val now = ZonedDateTime.now()
        val endOfWeek = now.plusDays(7)
        
        val events = calendarEventDao.getUpcomingEventsList(now.toInstant().toEpochMilli())
            .filter { it.startTime < endOfWeek.toInstant().toEpochMilli() }
        
        if (events.isEmpty()) {
            return "You have no upcoming events for the next 7 days."
        }
        
        val eventListText = events.joinToString("\n") { event ->
            val date = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(event.startTime),
                java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            "- ${event.title} on $date"
        }
        
        val prompt = """
            Summarize the following upcoming tasks and events for the week in a friendly way.
            Mention the number of deadlines or important meetings.
            
            Events:
            $eventListText
            
            Summary:
        """.trimIndent()
        
        return try {
            gemmaParser.generateResponse(prompt)
        } catch (e: Exception) {
            "Could not generate summary: ${e.message}"
        }
    }
}
