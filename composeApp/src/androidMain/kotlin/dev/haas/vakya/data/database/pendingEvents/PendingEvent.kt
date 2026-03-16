package dev.haas.vakya.data.database.pendingEvents

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_events")
data class PendingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emailId: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String?,
    val deadline: String?,
    val confidence: Float,
    val sender: String?, // The original sender of the email
    val originalBody: String? = null, // The full email body
    val accountId: String, // Destination account email
    val targetCalendarId: String? = null, // Destination calendar ID
    val status: String = "pending", // pending, approved, rejected
    val createdAt: Long = System.currentTimeMillis()
)
