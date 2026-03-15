package dev.haas.vakya.ui.reviewQueue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.haas.vakya.data.database.pendingEvents.PendingEvent
import dev.haas.vakya.data.repository.PendingEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewQueueViewModel(
    private val repository: PendingEventRepository,
    private val aiLearningRuleDao: dev.haas.vakya.data.dao.AiLearningRuleDao? = null,
    private val onApproveCallback: suspend (PendingEvent) -> Unit
) : ViewModel() {

    private val _pendingEvents = MutableStateFlow<List<PendingEvent>>(emptyList())
    val pendingEvents: StateFlow<List<PendingEvent>> = _pendingEvents.asStateFlow()

    private val _accounts = MutableStateFlow<List<dev.haas.vakya.data.database.AccountEntity>>(emptyList())
    val accounts: StateFlow<List<dev.haas.vakya.data.database.AccountEntity>> = _accounts.asStateFlow()

    private val _calendars = MutableStateFlow<Map<String, List<dev.haas.vakya.data.google.CalendarEntry>>>(emptyMap())
    val calendars: StateFlow<Map<String, List<dev.haas.vakya.data.google.CalendarEntry>>> = _calendars.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPendingEvents().collect { events ->
                _pendingEvents.value = events
            }
        }
        
        viewModelScope.launch {
            val settingsRepo = dev.haas.vakya.AppContextHolder.database.accountDao()
            settingsRepo.getAllAccounts().collect { accs ->
                _accounts.value = accs
                // Fetch calendars for accounts that don't have them in the map yet
                accs.forEach { account ->
                    if (account.accessToken != null && !_calendars.value.containsKey(account.email)) {
                        fetchCalendars(account.email, account.accessToken)
                    }
                }
            }
        }
    }

    private fun fetchCalendars(email: String, accessToken: String) {
        viewModelScope.launch {
            try {
                // Using a simple fetch - ideally this should be in a repository
                val calendarApi = dev.haas.vakya.AppContextHolder.calendarApi
                val response = calendarApi.listCalendarList("Bearer $accessToken")
                val items = response.items ?: emptyList()
                _calendars.update { current ->
                    current + (email to items)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewQueueViewModel", "Failed to fetch calendars for $email", e)
            }
        }
    }

    fun approveEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.approveEvent(event.id)
            onApproveCallback(event) // This handles deduplication and calendar API logic
        }
    }

    fun rejectEvent(event: PendingEvent, storeFeedback: Boolean = false) {
        viewModelScope.launch {
            repository.rejectEvent(event.id)
            if (storeFeedback) {
                aiLearningRuleDao?.let { dao ->
                    // Extract a keyword from the title
                    val keyword = event.title.split(" ").firstOrNull() ?: event.title
                    val domain = event.sender?.substringAfterLast("@")
                    
                    dao.insertRule(dev.haas.vakya.data.database.AiLearningRuleEntity(
                        keyword = keyword,
                        senderDomain = domain,
                        action = "ignore",
                        confidenceAdjustment = -0.4f // Slightly stronger penalty for specific domain
                    ))
                }
            }
        }
    }

    fun updateEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(event: PendingEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }
}
