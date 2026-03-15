package dev.haas.vakya.ui.reviewQueue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.haas.vakya.data.database.pendingEvents.PendingEvent
import dev.haas.vakya.data.repository.PendingEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewQueueViewModel(
    private val repository: PendingEventRepository,
    private val onApproveCallback: suspend (PendingEvent) -> Unit
) : ViewModel() {

    private val _pendingEvents = MutableStateFlow<List<PendingEvent>>(emptyList())
    val pendingEvents: StateFlow<List<PendingEvent>> = _pendingEvents.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPendingEvents().collect { events ->
                _pendingEvents.value = events
            }
        }
    }

    fun approveEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.approveEvent(event.id)
            onApproveCallback(event) // This handles deduplication and calendar API logic
        }
    }

    fun rejectEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.rejectEvent(event.id)
        }
    }

    fun updateEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }
}
