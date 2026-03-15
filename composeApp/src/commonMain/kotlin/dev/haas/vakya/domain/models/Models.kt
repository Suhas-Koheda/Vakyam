package dev.haas.vakya.domain.models

import com.squareup.moshi.JsonClass

data class EmailMessage(
    val id: String,
    val threadId: String,
    val subject: String,
    val sender: String,
    val snippet: String,
    val body: String,
    val timestamp: Long,
    val accountEmail: String
)

@JsonClass(generateAdapter = true)
data class ExtractedEvent(
    val type: String, // "event" | "assignment" | "announcement" | "ignore"
    val title: String,
    val description: String,
    val start_time: String?, // ISO 8601 or similar
    val end_time: String?,
    val deadline: String?,
    val course: String?,
    val confidence: Double
)

data class GoogleAccountConfig(
    val email: String,
    val displayName: String?,
    val isGmailEnabled: Boolean = true,
    val targetCalendarId: String = "primary"
)
