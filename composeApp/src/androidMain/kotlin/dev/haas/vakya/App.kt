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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import dev.haas.vakya.common.SentenceSummarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import vakya.composeapp.generated.resources.*
import vakya.composeapp.generated.resources.Res
import vakya.composeapp.generated.resources.jetbrainsmono_regular
import vakya.composeapp.generated.resources.jetbrainsmono_bold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
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


@Composable
fun VakyaTypography(): Typography {
    val jetbrainsMono = FontFamily(
        Font(Res.font.jetbrainsmono_regular, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_bold, FontWeight.Bold)
    )

    return Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = jetbrainsMono),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = jetbrainsMono),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = jetbrainsMono),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = jetbrainsMono),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = jetbrainsMono),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = jetbrainsMono),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = jetbrainsMono),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = jetbrainsMono),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = jetbrainsMono),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = jetbrainsMono),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = jetbrainsMono),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = jetbrainsMono),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = jetbrainsMono),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = jetbrainsMono),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = jetbrainsMono)
    )
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {

    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
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

    val dashboardRepository = remember { DashboardRepository(db.calendarEventDao(), db.aiActionLogDao()) }
    val settingsRepository = remember { SettingsRepository(db.accountDao(), db.appSettingDao(), db.aiActionLogDao(), calendarApi) }

    val dashboardViewModel = remember { DashboardViewModel(dashboardRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    MaterialTheme(typography = VakyaTypography()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(Screen.Dashboard, Screen.Debug, Screen.Settings)
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
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    Screen.Dashboard -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onOpenDebug = { currentScreen = Screen.Debug }
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