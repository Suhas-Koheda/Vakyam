package dev.haas.vakya.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.haas.vakya.data.database.AccountEntity
import dev.haas.vakya.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val calendars: Map<String, List<dev.haas.vakya.data.google.CalendarEntry>> = emptyMap(),
    val scanningInterval: String = "30 minutes",
    val confidenceThreshold: Float = 0.7f,
    val convertDeadlines: Boolean = true,
    val notifyOnAdd: Boolean = true,
    val notifyOnIgnore: Boolean = false,
    val dailySummary: Boolean = true,
    val localAiOnly: Boolean = true,
    val gmailSourceEmail: String? = null,
    val calendarDestEmail: String? = null,
    val isSigningIn: Boolean = false,
    val showConsentSheet: Boolean = false
)


class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
                // Fetch calendars for each account if they have an access token
                accounts.forEach { account ->
                    if (account.accessToken != null) {
                        fetchCalendars(account.email, account.accessToken)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            repository.getAllSettings().collect { settings ->
                val settingsMap = settings.associate { it.key to it.value }
                _uiState.update { state ->
                    state.copy(
                        scanningInterval = settingsMap[KEY_SCAN_INTERVAL] ?: "30 minutes",
                        confidenceThreshold = settingsMap[KEY_CONFIDENCE]?.toFloatOrNull() ?: 0.7f,
                        convertDeadlines = settingsMap[KEY_CONVERT_DEADLINES]?.toBoolean() ?: true,
                        notifyOnAdd = settingsMap[KEY_NOTIFY_ADD]?.toBoolean() ?: true,
                        notifyOnIgnore = settingsMap[KEY_NOTIFY_IGNORE]?.toBoolean() ?: false,
                        dailySummary = settingsMap[KEY_DAILY_SUMMARY]?.toBoolean() ?: true,
                        localAiOnly = settingsMap[KEY_LOCAL_AI_ONLY]?.toBoolean() ?: true,
                        gmailSourceEmail = settingsMap[KEY_GMAIL_SOURCE],
                        calendarDestEmail = settingsMap[KEY_CALENDAR_DEST]
                    )
                }
            }
        }

    }

    fun setSigningIn(loading: Boolean) {
        _uiState.update { it.copy(isSigningIn = loading) }
    }

    fun setShowConsentSheet(show: Boolean) {
        _uiState.update { it.copy(showConsentSheet = show) }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch { repository.updateAccount(account) }
    }

    fun removeAccount(account: AccountEntity) {
        viewModelScope.launch { repository.removeAccount(account) }
    }

    fun fetchCalendars(email: String, accessToken: String) {
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Starting fetchCalendars for $email")
            val cals = repository.getCalendars(accessToken)
            Log.d("SettingsViewModel", "Received ${cals.size} calendars for $email")
            _uiState.update { state ->
                val newMap = state.calendars.toMutableMap()
                newMap[email] = cals
                state.copy(calendars = newMap)
            }
        }
    }

    fun createCalendar(email: String, summary: String) {
        viewModelScope.launch {
            val account = _uiState.value.accounts.find { it.email == email }
            val accessToken = account?.accessToken ?: return@launch
            try {
                val newCalendar = repository.createCalendar(accessToken, summary)
                fetchCalendars(email, accessToken)
                updateAccount(account.copy(targetCalendarId = newCalendar.id))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to create calendar", e)
            }
        }
    }

    fun setSetting(key: String, value: String) {
        viewModelScope.launch { repository.setSetting(key, value) }
    }

    companion object {
        const val KEY_SCAN_INTERVAL = "scan_interval"
        const val KEY_CONFIDENCE = "ai_confidence"
        const val KEY_CONVERT_DEADLINES = "convert_deadlines"
        const val KEY_NOTIFY_ADD = "notify_add"
        const val KEY_NOTIFY_IGNORE = "notify_ignore"
        const val KEY_DAILY_SUMMARY = "daily_summary"
        const val KEY_LOCAL_AI_ONLY = "local_ai_only"
        const val KEY_GMAIL_SOURCE = "gmail_source"
        const val KEY_CALENDAR_DEST = "calendar_dest"
    }
}

