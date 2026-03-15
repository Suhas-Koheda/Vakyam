package dev.haas.vakya.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.haas.vakya.data.database.KnowledgeNoteEntity
import dev.haas.vakya.data.repository.KnowledgeRepository
import dev.haas.vakya.domain.ai.SemanticSearchUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KnowledgeViewModel(
    private val repository: KnowledgeRepository,
    private val gemmaParser: dev.haas.vakya.ai.GemmaParser,
    private val pendingEventRepository: dev.haas.vakya.data.repository.PendingEventRepository,
    private val accountDao: dev.haas.vakya.data.database.AccountDao
) : ViewModel() {

    private val semanticSearchUseCase = SemanticSearchUseCase(gemmaParser)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _semanticResults = MutableStateFlow<List<KnowledgeNoteEntity>?>(null)

    val notes: StateFlow<List<KnowledgeNoteEntity>> = _searchQuery
        .combine(repository.getAllNotes()) { query, notes ->
            Pair(query, notes)
        }
        .combine(_semanticResults) { (query, notes), semantic ->
            if (semantic != null && query.isNotBlank()) {
                semantic
            } else if (query.isBlank()) {
                notes
            } else {
                notes.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.content.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestedTags = MutableStateFlow<List<String>>(emptyList())
    val suggestedTags: StateFlow<List<String>> = _suggestedTags.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _semanticResults.value = null
        }
    }

    fun performSemanticSearch() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            val candidates = repository.getAllNotes().first()
            val results = semanticSearchUseCase.search(query, candidates)
            _semanticResults.value = results
            _isSearching.value = false
        }
    }

    suspend fun getNoteById(id: Long) = repository.getNoteById(id)

    fun saveNote(title: String, content: String, tags: String, summary: String? = null, linkedEventId: Long? = null) {
        viewModelScope.launch {
            val note = KnowledgeNoteEntity(
                title = title,
                content = content,
                summary = summary,
                tags = tags,
                linkedEventId = linkedEventId
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: KnowledgeNoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: KnowledgeNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun suggestTags(content: String) {
        viewModelScope.launch {
            val prompt = "Suggest exactly 3 relevant short tags (single words) for this note content, separated by commas. Do not include any other text: $content"
            val response = gemmaParser.generateResponse(prompt)
            val suggested = response.split(",")
                .map { it.trim().lowercase().removePrefix("#") }
                .filter { it.isNotBlank() }
            _suggestedTags.value = suggested
        }
    }

    fun clearSuggestedTags() {
        _suggestedTags.value = emptyList()
    }

    fun generateTitleAndSummary(content: String, onResult: (String, String) -> Unit) {
        viewModelScope.launch {
            val prompt = """
                Based on the following note content, provide a short concise title and a 1-sentence summary.
                Format exactly as:
                TITLE: [Your Title]
                SUMMARY: [Your Summary]
                
                Content: $content
            """.trimIndent()
            
            val response = gemmaParser.generateResponse(prompt)
            val title = response.substringAfter("TITLE:").substringBefore("SUMMARY:").trim()
                .removePrefix("[").removeSuffix("]")
            val summary = response.substringAfter("SUMMARY:").trim()
                .removePrefix("[").removeSuffix("]")
            
            if (title.isNotEmpty() && summary.isNotEmpty()) {
                onResult(title, summary)
            }
        }
    }

    fun convertToTask(note: KnowledgeNoteEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            val extracted = gemmaParser.parseSentence(note.content) ?: return@launch
            val accounts = accountDao.getAllAccountsList()
            val targetEmail = accounts.firstOrNull()?.email ?: "Manual Entry"

            val pendingEvent = dev.haas.vakya.data.database.pendingEvents.PendingEvent(
                emailId = "Note Conversion",
                title = extracted.title ?: note.title,
                description = extracted.description ?: note.content,
                startTime = extracted.start_time ?: java.time.ZonedDateTime.now().toString(),
                endTime = extracted.end_time,
                deadline = extracted.deadline,
                confidence = extracted.confidence.toFloat(),
                sender = "Note Conversion",
                accountId = targetEmail,
                status = "pending"
            )
            pendingEventRepository.insertEvent(pendingEvent)
            
            // Mark note as linked (optional)
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            onComplete()
        }
    }
}
