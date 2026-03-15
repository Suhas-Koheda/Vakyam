package dev.haas.vakya.domain.deduplication

import dev.haas.vakya.data.google.CalendarApi
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class CalendarDeduplicationService(
    private val calendarApi: CalendarApi
) {

    suspend fun isDuplicateEvent(
        calendarId: String,
        authHeader: String,
        title: String,
        startTimeIso: String,
        endTimeIso: String?
    ): Boolean {
        try {
            // Parse start and end times for comparison
            val startZdt = try {
                ZonedDateTime.parse(startTimeIso)
            } catch (e: Exception) {
                return false // Can't parse time, err on side of not deduplicating
            }
            
            val endZdt = endTimeIso?.let {
                try {
                    ZonedDateTime.parse(it)
                } catch (e: Exception) {
                    startZdt.plusHours(1) // Fallback
                }
            } ?: startZdt.plusHours(1)


            // Fetch events from calendar for the same day
            val startOfDay = startZdt.toLocalDate().atStartOfDay(startZdt.zone)
            val endOfDay = startOfDay.plusDays(1)
            
            val timeMin = startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val timeMax = endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val existingEvents = calendarApi.listEvents(
                calendarId = calendarId,
                timeMin = timeMin,
                timeMax = timeMax,
                authHeader = authHeader
            ).items ?: emptyList()

            for (event in existingEvents) {
                // 1. Time overlap check (within ±30 minutes)
                val eventStartStr = event.start.dateTime
                if (eventStartStr != null) {
                    try {
                        val eventStartZdt = ZonedDateTime.parse(eventStartStr)
                        
                        // Check if within +/- 30 mins
                        val diffMinutes = abs(java.time.Duration.between(startZdt, eventStartZdt).toMinutes())
                        if (diffMinutes <= 30) {
                            
                            // 2. Title similarity > 80% check
                            val existingTitle = event.summary ?: ""
                            val similarity = calculateSimilarity(title, existingTitle)
                            
                            if (similarity > 0.8) {
                                return true // Duplicate found!
                            }
                        }
                    } catch (e: Exception) {
                        // ignore unparseable times
                    }
                }
            }
            
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Calculates Jaccard similarity (or a simple word-overlap) for titles.
     * Could also use Levenshtein, but basic token overlap is usually fine for >80%.
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val tokens1 = s1.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val tokens2 = s2.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()

        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2).size.toDouble()
        val union = tokens1.union(tokens2).size.toDouble()

        return intersection / union
    }
}
