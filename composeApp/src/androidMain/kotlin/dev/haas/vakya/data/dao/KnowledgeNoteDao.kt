package dev.haas.vakya.data.dao

import androidx.room.*
import dev.haas.vakya.data.database.KnowledgeNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: KnowledgeNoteEntity): Long

    @Update
    suspend fun updateNote(note: KnowledgeNoteEntity)

    @Delete
    suspend fun deleteNote(note: KnowledgeNoteEntity)

    @Query("SELECT * FROM knowledge_notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<KnowledgeNoteEntity>>

    @Query("SELECT * FROM knowledge_notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<KnowledgeNoteEntity>>

    @Query("SELECT * FROM knowledge_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): KnowledgeNoteEntity?

    @Query("SELECT * FROM knowledge_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<KnowledgeNoteEntity>>
    
    @Query("SELECT * FROM knowledge_notes WHERE isArchived = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentNotes(limit: Int): Flow<List<KnowledgeNoteEntity>>

    @Query("SELECT * FROM knowledge_notes WHERE isArchived = 0 ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentNotesList(limit: Int): List<KnowledgeNoteEntity>

    @Query("UPDATE knowledge_notes SET isArchived = 1, updatedAt = :ts WHERE id = :id")
    suspend fun archiveNote(id: Long, ts: Long = System.currentTimeMillis())
}
