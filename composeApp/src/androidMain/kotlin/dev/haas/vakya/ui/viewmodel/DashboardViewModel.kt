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
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: DashboardRepository
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
}
