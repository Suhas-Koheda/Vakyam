package dev.haas.vakya.data.google

import retrofit2.http.*

interface GmailApi {
    @GET("gmail/v1/users/{userId}/messages")
    suspend fun listMessages(
        @Path("userId") userId: String = "me",
        @Query("q") query: String? = null,
        @Query("pageToken") pageToken: String? = null,
        @Header("Authorization") authHeader: String
    ): GmailMessageListResponse

    @GET("gmail/v1/users/{userId}/messages/{id}")
    suspend fun getMessage(
        @Path("userId") userId: String = "me",
        @Path("id") id: String,
        @Header("Authorization") authHeader: String
    ): GmailMessageResponse
}

interface CalendarApi {
    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun listEvents(
        @Path("calendarId") calendarId: String = "primary",
        @Query("timeMin") timeMin: String?,
        @Query("timeMax") timeMax: String?,
        @Header("Authorization") authHeader: String
    ): CalendarEventListResponse

    @POST("calendar/v3/calendars/{calendarId}/events")
    suspend fun createEvent(
        @Path("calendarId") calendarId: String = "primary",
        @Body event: CalendarEvent,
        @Header("Authorization") authHeader: String
    ): CalendarEvent
}

// Response Models
data class GmailMessageListResponse(
    val messages: List<GmailMessageRef>?,
    val nextPageToken: String?
)

data class GmailMessageRef(
    val id: String,
    val threadId: String
)

data class GmailMessageResponse(
    val id: String,
    val threadId: String,
    val snippet: String,
    val payload: GmailPayload
)

data class GmailPayload(
    val headers: List<GmailHeader>,
    val body: GmailBody?,
    val parts: List<GmailPart>?
)

data class GmailHeader(val name: String, val value: String)
data class GmailBody(val size: Int, val data: String?)
data class GmailPart(val mimeType: String, val body: GmailBody?)

data class CalendarEventListResponse(
    val items: List<CalendarEvent>?
)

data class CalendarEvent(
    val id: String? = null,
    val summary: String,
    val description: String?,
    val start: CalendarTime,
    val end: CalendarTime,
    val reminders: CalendarReminders? = null
)

data class CalendarTime(
    val dateTime: String?, // ISO 8601
    val timeZone: String? = null
)

data class CalendarReminders(
    val useDefault: Boolean,
    val overrides: List<CalendarReminderOverride>?
)

data class CalendarReminderOverride(
    val method: String, // "email" or "popup"
    val minutes: Int
)
