package dev.haas.vakya.data.database.pendingEvents

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_events")
data class PendingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emailId: String,
    val title: String,
    val description: String,
    val startTime: String, // Stored as ISO string or Long depending on how others are usually stored; the others are Long. I will use Long. Let's look at the prompt.
    val endTime: String?,
    val deadline: String?,
    val confidence: Float,
    val accountId: String,
    val status: String = "pending", // pending, approved, rejected
    val createdAt: Long = System.currentTimeMillis()
)
