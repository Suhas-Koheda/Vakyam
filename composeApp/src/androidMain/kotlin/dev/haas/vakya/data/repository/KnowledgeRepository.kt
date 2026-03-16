package dev.haas.vakya.data.repository

import dev.haas.vakya.data.dao.KnowledgeNoteDao
import dev.haas.vakya.data.database.KnowledgeNoteEntity
import kotlinx.coroutines.flow.Flow

class KnowledgeRepository(
    private val knowledgeNoteDao: KnowledgeNoteDao
) {
    fun getAllNotes(): Flow<List<KnowledgeNoteEntity>> = knowledgeNoteDao.getAllNotes()

    fun getArchivedNotes(): Flow<List<KnowledgeNoteEntity>> = knowledgeNoteDao.getArchivedNotes()

    fun getRecentNotes(limit: Int): Flow<List<KnowledgeNoteEntity>> = knowledgeNoteDao.getRecentNotes(limit)

    suspend fun getNoteById(id: Long): KnowledgeNoteEntity? = knowledgeNoteDao.getNoteById(id)

    suspend fun insertNote(note: KnowledgeNoteEntity): Long = knowledgeNoteDao.insertNote(note)

    suspend fun updateNote(note: KnowledgeNoteEntity) = knowledgeNoteDao.updateNote(note)

    suspend fun deleteNote(note: KnowledgeNoteEntity) = knowledgeNoteDao.deleteNote(note)

    suspend fun archiveNote(id: Long) = knowledgeNoteDao.archiveNote(id)

    fun searchNotes(query: String): Flow<List<KnowledgeNoteEntity>> = knowledgeNoteDao.searchNotes(query)
}
