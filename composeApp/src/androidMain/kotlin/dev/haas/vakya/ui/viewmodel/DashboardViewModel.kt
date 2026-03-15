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
    val recentActions: List<AiActionLogEntity> = emptyList(),
    val isLoading: Boolean = false,
    val weeklySummary: String? = null,
    val isSummaryLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val weeklySummaryUseCase: dev.haas.vakya.domain.ai.WeeklySummaryUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                repository.getTodayEvents(),
                repository.getUpcomingEvents(),
                repository.getRecentAiActions()
            ) { today, upcoming, actions ->
                DashboardUiState(
                    todayEvents = today,
                    upcomingEvents = upcoming,
                    recentActions = actions,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
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
}
