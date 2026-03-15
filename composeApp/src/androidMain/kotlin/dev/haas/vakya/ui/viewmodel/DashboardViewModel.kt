package dev.haas.vakya.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.haas.vakya.data.database.AiActionLogEntity
import dev.haas.vakya.data.database.CalendarEventEntity
import dev.haas.vakya.data.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayEvents: List<CalendarEventEntity> = emptyList(),
    val upcomingEvents: List<CalendarEventEntity> = emptyList(),
    val recentNotes: List<dev.haas.vakya.data.database.KnowledgeNoteEntity> = emptyList(),
    val recentActions: List<AiActionLogEntity> = emptyList(),
    val isLoading: Boolean = false,
    val weeklySummary: String? = null,
    val isSummaryLoading: Boolean = false,
    val dailyBriefing: String? = null,
    val isBriefingLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val pendingEventRepository: dev.haas.vakya.data.repository.PendingEventRepository? = null,
    private val gemmaParser: dev.haas.vakya.ai.GemmaParser? = null,
    private val weeklySummaryUseCase: dev.haas.vakya.domain.ai.WeeklySummaryUseCase? = null,
    private val dailyBriefingUseCase: dev.haas.vakya.domain.ai.DailyBriefingUseCase? = null,
    private val knowledgeRepository: dev.haas.vakya.data.repository.KnowledgeRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val notesFlow = knowledgeRepository?.getRecentNotes(3) ?: flowOf(emptyList())

            combine(
                repository.getTodayEvents(),
                repository.getUpcomingEvents(),
                notesFlow,
                repository.getRecentAiActions()
            ) { today, upcoming, notes, actions ->
                _uiState.value.copy(
                    todayEvents = today,
                    upcomingEvents = upcoming,
                    recentNotes = notes,
                    recentActions = actions,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun generateDailyBriefing() {
        val useCase = dailyBriefingUseCase ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBriefingLoading = true) }
            val briefing = useCase.execute()
            _uiState.update { it.copy(dailyBriefing = briefing, isBriefingLoading = false) }
        }
    }

    fun dismissBriefing() {
        _uiState.update { it.copy(dailyBriefing = null) }
    }

    fun markAsIgnored(eventId: Long) {
        viewModelScope.launch {
            repository.markAsIgnored(eventId)
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun generateWeeklySummary() {
        val useCase = weeklySummaryUseCase ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSummaryLoading = true) }
            val summary = useCase.generateWeeklySummary()
            _uiState.update { it.copy(weeklySummary = summary, isSummaryLoading = false) }
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(weeklySummary = null) }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun processManualSentence(sentence: String) {
        val parser = gemmaParser ?: return
        val pendingRepo = pendingEventRepository ?: return
        
        viewModelScope.launch {
            val extracted = parser.parseSentence(sentence) ?: return@launch
            if (extracted.type != "ignore") {
                val pendingEvent = dev.haas.vakya.data.database.pendingEvents.PendingEvent(
                    title = extracted.title ?: "Untitled Event",
                    description = extracted.description,
                    startTime = extracted.start_time ?: java.time.ZonedDateTime.now().toString(),
                    endTime = extracted.end_time,
                    course = extracted.course,
                    confidence = extracted.confidence,
                    emailId = "Manual Entry",
                    type = extracted.type
                )
                pendingRepo.insertPendingEvent(pendingEvent)
            }
        }
    }
}
