package dev.haas.vakya

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.haas.vakya.common.SentenceSummarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.ui.graphics.vector.ImageVector
import dev.haas.vakya.data.repository.DashboardRepository
import dev.haas.vakya.data.repository.SettingsRepository
import dev.haas.vakya.ui.screens.DashboardScreen
import dev.haas.vakya.ui.screens.SettingsScreen
import dev.haas.vakya.ui.viewmodel.DashboardViewModel
import dev.haas.vakya.ui.viewmodel.SettingsViewModel
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import dev.haas.vakya.data.google.CalendarApi
import dev.haas.vakya.ui.theme.VakyaTheme




sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Loading : Screen("loading", "Loading", Icons.Default.Refresh)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Review : Screen("review", "Review", Icons.Default.PlaylistAddCheck)
    object Knowledge : Screen("knowledge", "Knowledge", Icons.Default.AutoStories)
    object AddTask : Screen("add_task", "Add Task", Icons.Default.AddTask)
    object NoteDetail : Screen("note_detail", "Note Detail", Icons.Default.Description)
    object AddNote : Screen("add_note", "Add Note", Icons.Default.NoteAdd)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()

    val db = AppContextHolder.database

    val moshi = remember {
        com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    val calendarApi = remember { retrofit.create(CalendarApi::class.java) }

    val gemmaParser = remember { dev.haas.vakya.ai.GemmaParser() }
    val weeklySummaryUseCase = remember { dev.haas.vakya.domain.ai.WeeklySummaryUseCase(db.calendarEventDao(), gemmaParser) }
    val dailyBriefingUseCase = remember { dev.haas.vakya.domain.ai.DailyBriefingUseCase(db.calendarEventDao(), gemmaParser) }

    val knowledgeRepository = remember { dev.haas.vakya.data.repository.KnowledgeRepository(db.knowledgeNoteDao()) }
    val dashboardRepository = remember { DashboardRepository(db.calendarEventDao(), db.aiActionLogDao()) }
    val settingsRepository = remember { SettingsRepository(db.accountDao(), db.appSettingDao(), db.aiActionLogDao(), calendarApi) }
    val pendingEventRepository = remember { dev.haas.vakya.data.repository.PendingEventRepository(db.pendingEventDao()) }

    val startupViewModel = remember { dev.haas.vakya.ui.viewmodel.StartupViewModel(db, context) }
    val dashboardViewModel = remember { DashboardViewModel(dashboardRepository, pendingEventRepository, gemmaParser, weeklySummaryUseCase, dailyBriefingUseCase, knowledgeRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val knowledgeViewModel = remember { dev.haas.vakya.ui.knowledge.KnowledgeViewModel(knowledgeRepository, gemmaParser, pendingEventRepository, db.accountDao()) }
    
    val reviewQueueViewModel = remember { 
        val notificationManager = dev.haas.vakya.notifications.VakyaNotificationManager(context)
        val deduplicationService = dev.haas.vakya.domain.deduplication.CalendarDeduplicationService(calendarApi)
        val approveUseCase = dev.haas.vakya.domain.agent.ApprovePendingEventUseCase(
            db.accountDao(), calendarApi, deduplicationService, db.aiActionLogDao(), db.calendarEventDao(), notificationManager
        )
        dev.haas.vakya.ui.reviewQueue.ReviewQueueViewModel(
            pendingEventRepository,
            db.aiLearningRuleDao(),
            onApproveCallback = { event -> approveUseCase.execute(event) }
        ) 
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }

    VakyaTheme {
        Surface {
            Scaffold(
            bottomBar = {
                if (currentScreen != Screen.Loading) {
                    NavigationBar {
                        val items = listOf(Screen.Dashboard, Screen.Review, Screen.Knowledge, Screen.Settings)
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    Screen.Loading -> {
                        dev.haas.vakya.ui.loading.LoadingScreen(
                            viewModel = startupViewModel,
                            onNavigateToDashboard = { currentScreen = Screen.Dashboard },
                            onNavigateToSettings = { currentScreen = Screen.Settings }
                        )
                    }
                    Screen.Dashboard -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onOpenDebug = { currentScreen = Screen.Debug },
                            onNavigateToAddTask = { currentScreen = Screen.AddTask },
                            onNavigateToAddNote = { currentScreen = Screen.AddNote },
                            onNavigateToKnowledge = { currentScreen = Screen.Knowledge },
                            onNoteClick = { id -> 
                                selectedNoteId = id
                                currentScreen = Screen.NoteDetail
                            }
                        )
                    }
                    Screen.Review -> {
                        dev.haas.vakya.ui.reviewQueue.ReviewQueueScreen(
                            viewModel = reviewQueueViewModel
                        )
                    }
                    Screen.Knowledge -> {
                        dev.haas.vakya.ui.knowledge.KnowledgeBaseScreen(
                            viewModel = knowledgeViewModel,
                            onAddNote = { currentScreen = Screen.AddNote },
                            onNoteClick = { id -> 
                                selectedNoteId = id
                                currentScreen = Screen.NoteDetail 
                            }
                        )
                    }
                    Screen.AddNote -> {
                        dev.haas.vakya.ui.knowledge.AddNoteScreen(
                            viewModel = knowledgeViewModel,
                            onBack = { currentScreen = Screen.Knowledge }
                        )
                    }
                    Screen.NoteDetail -> {
                        selectedNoteId?.let { id ->
                            dev.haas.vakya.ui.knowledge.NoteDetailScreen(
                                noteId = id,
                                viewModel = knowledgeViewModel,
                                onBack = { currentScreen = Screen.Knowledge }
                            )
                        }
                    }
                    Screen.AddTask -> {
                        val uiState by settingsViewModel.uiState.collectAsState()
                        dev.haas.vakya.ui.tasks.AddTaskScreen(
                            availableEmails = uiState.accounts.map { it.email },
                            onSaveRequested = { event -> 
                                scope.launch {
                                    pendingEventRepository.insertEvent(dev.haas.vakya.data.database.pendingEvents.PendingEvent(
                                        title = event.title,
                                        description = event.description ?: "",
                                        startTime = java.time.Instant.ofEpochMilli(event.startTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                        endTime = null,
                                        deadline = null,
                                        emailId = event.accountEmail,
                                        accountId = event.accountEmail,
                                        confidence = 1.0f,
                                        status = "pending"
                                    ))
                                }
                            },
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                    Screen.Debug -> {
                        dev.haas.vakya.ui.debug.DebugScreen(
                            logsFlow = db.aiActionLogDao().getRecentLogs(),
                            onClear = { dashboardViewModel.clearLogs() },
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onAddAccount = {
                                scope.launch {
                                    settingsViewModel.setSigningIn(true)
                                    try {
                                        authManager.signIn()
                                    } finally {
                                        settingsViewModel.setSigningIn(false)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
}


@Composable
fun GemmaTestSection() {
    val scope = rememberCoroutineScope()
    Text("Gemma Model Test", style = MaterialTheme.typography.titleMedium)
    var inputText by remember { mutableStateOf("NLP Assignment due on Friday 5 PM.") }
    var summaryResult by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        label = { Text("Input text for Gemma") },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        textStyle = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = {
            scope.launch {
                isProcessing = true
                summaryResult = "Processing..."
                try {
                    summaryResult = withContext(Dispatchers.Default) {
                        SentenceSummarizer.summarize(inputText)
                    }
                } catch (e: Exception) {
                    summaryResult = "Error: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        },
        enabled = !isProcessing
    ) {
        Text(if (isProcessing) "Analyzing..." else "Test Extraction")
    }

    if (summaryResult.isNotEmpty()) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Result:", style = MaterialTheme.typography.labelLarge)
                Text(summaryResult, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}