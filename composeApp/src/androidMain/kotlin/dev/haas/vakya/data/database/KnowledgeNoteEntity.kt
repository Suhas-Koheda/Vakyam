package dev.haas.vakya.data.database

import androidx.room.*

@Entity(tableName = "knowledge_notes")
data class KnowledgeNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val summary: String? = null,
    val tags: String, // comma separated
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val linkedEventId: Long? = null
)
