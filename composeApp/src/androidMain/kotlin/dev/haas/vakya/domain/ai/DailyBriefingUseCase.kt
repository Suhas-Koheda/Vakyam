package dev.haas.vakya.domain.ai

import dev.haas.vakya.data.database.CalendarEventDao
import dev.haas.vakya.ai.GemmaParser
import java.text.SimpleDateFormat
import java.util.*

class DailyBriefingUseCase(
    private val calendarEventDao: CalendarEventDao,
    private val gemmaParser: GemmaParser
) {
    suspend fun execute(): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Today range
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        
        val todayEvents = calendarEventDao.getTodayEventsList(startOfDay, endOfDay)
        val upcomingEvents = calendarEventDao.getUpcomingEventsList(endOfDay)
            .take(5) // Limit to next few events

        if (todayEvents.isEmpty() && upcomingEvents.isEmpty()) {
            return "You have no upcoming tasks or events."
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val context = StringBuilder()
        context.append("Today's Events:\n")
        todayEvents.forEach { 
            context.append("- ${it.title} at ${sdf.format(Date(it.startTime))}\n")
        }
        
        context.append("\nUpcoming Events:\n")
        upcomingEvents.forEach {
            context.append("- ${it.title} on ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it.startTime))}\n")
        }

        val prompt = """
            Based on the following calendar events, provide a brief, friendly summary of the day and what's coming up. 
            Keep it concise (2-3 sentences).
            
            $context
        """.trimIndent()

        return try {
            gemmaParser.generateResponse(prompt)
        } catch (e: Exception) {
            "Briefing error: ${e.message}"
        }
    }
}
