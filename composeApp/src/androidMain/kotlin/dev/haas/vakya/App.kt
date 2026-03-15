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

@Composable
@Preview
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()

    // Permission Handling
    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    var hasNotificationPermission by remember { 
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasNotificationPermission = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (permissionsToRequest.isNotEmpty() && !hasNotificationPermission) {
            launcher.launch(permissionsToRequest)
        }
    }



    val db = remember {
        androidx.room.Room.databaseBuilder(
            context,
            dev.haas.vakya.data.database.VakyaDatabase::class.java, "vakya-db"
        ).fallbackToDestructiveMigration().build()
    }
    val accounts by db.accountDao().getAllAccounts().collectAsState(initial = emptyList())

    var showDebug by remember { mutableStateOf(false) }

    MaterialTheme(typography = VakyaTypography()) {
        if (showDebug) {
            dev.haas.vakya.ui.debug.DebugScreen(
                logsFlow = db.processedEmailDao().getRecentLogs(),
                onBack = { showDebug = false }
            )
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                if (!hasNotificationPermission) {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Notifications are disabled. The agent cannot alert you to new events.",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { launcher.launch(permissionsToRequest) }) {
                                Text("Fix")
                            }
                        }
                    }
                }

                Row(

                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vakya AI Agent",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Button(onClick = { showDebug = true }) {
                        Text("Debug")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (accounts.isEmpty()) {
                    Box(modifier = Modifier.padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Connect Google account to track events.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        authManager.signIn()
                                    }
                                }
                            ) {
                                Text("Sign In with Google")
                            }
                        }
                    }
                } else {
                    Text("Connected Accounts", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        accounts.forEach { account ->
                            Card(
                                modifier = Modifier.padding(8.dp).fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(account.displayName ?: "Unknown", fontWeight = FontWeight.Bold)
                                        Text(account.email, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = account.isGmailEnabled,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                db.accountDao().insertAccount(account.copy(isGmailEnabled = checked))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { scope.launch { authManager.signIn() } },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Add Another Account")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                GemmaTestSection()
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