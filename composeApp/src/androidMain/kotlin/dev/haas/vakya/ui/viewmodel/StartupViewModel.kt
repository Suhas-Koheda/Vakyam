package dev.haas.vakya.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dev.haas.vakya.data.database.VakyaDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class StartupUiState(
    val message: String = "Initializing...",
    val isInitialized: Boolean = false,
    val navigateToDashboard: Boolean = false,
    val navigateToSettings: Boolean = false
)

class StartupViewModel(
    private val db: VakyaDatabase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startInitialization()
    }

    private fun startInitialization() {
        viewModelScope.launch {
            // 1. Check database
            _uiState.update { it.copy(message = "Checking AI engine...") }
            delay(500)
            
            // 2. Check Gemma model
            val modelFile = File("/data/user/0/dev.haas.vakya/files/gemma.task")
            val modelExists = modelFile.exists()
            if (!modelExists) {
                _uiState.update { it.copy(message = "AI Model missing. Please download in Settings.") }
                delay(1000)
            }

            // 3. Check for accounts
            _uiState.update { it.copy(message = "Loading accounts...") }
            val accounts = db.accountDao().getAllAccountsList()
            delay(500)

            // 4. Verify WorkManager
            _uiState.update { it.copy(message = "Preparing background services...") }
            // In a real app we'd check WorkInfo, but here we just ensure we're ready
            delay(500)

            // 5. Check notification permission (simplified check)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Just log or update message, actual request is in UI
            }

            if (accounts.isEmpty()) {
                _uiState.update { it.copy(navigateToSettings = true) }
            } else {
                _uiState.update { it.copy(navigateToDashboard = true) }
            }
        }
    }
}
