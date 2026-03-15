package dev.haas.vakya.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.haas.vakya.data.database.AccountEntity
import dev.haas.vakya.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAddAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Google Accounts
            SettingsSection("Google Accounts", Icons.Default.AccountCircle) {
                uiState.accounts.forEach { account ->
                    AccountItem(
                        account = account,
                        availableCalendars = uiState.calendars[account.email] ?: emptyList(),
                        onUpdate = { viewModel.updateAccount(it) },
                        onRemove = { viewModel.removeAccount(it) }
                    )
                }
                Button(
                    onClick = onAddAccount,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Account")
                }
            }

            // Email Parsing Settings
            SettingsSection("Email Parsing", Icons.Default.Email) {
                DropdownSetting(
                    label = "Scanning Interval",
                    options = listOf("15 minutes", "30 minutes", "1 hour"),
                    selected = uiState.scanningInterval,
                    onSelected = { viewModel.setSetting(SettingsViewModel.KEY_SCAN_INTERVAL, it) }
                )
            }

            // AI Behavior Settings
            SettingsSection("AI Behavior", Icons.Default.AutoAwesome) {
                Text("Confidence Threshold: ${uiState.confidenceThreshold}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.confidenceThreshold,
                    onValueChange = { viewModel.setSetting(SettingsViewModel.KEY_CONFIDENCE, it.toString()) },
                    valueRange = 0.5f..0.9f,
                    steps = 3
                )
                
                ToggleSetting(
                    label = "Convert deadlines into reminders",
                    checked = uiState.convertDeadlines,
                    onCheckedChange = { viewModel.setSetting(SettingsViewModel.KEY_CONVERT_DEADLINES, it.toString()) }
                )
            }

            // Notifications
            SettingsSection("Notifications", Icons.Default.Notifications) {
                ToggleSetting(
                    label = "Notify when AI adds an event",
                    checked = uiState.notifyOnAdd,
                    onCheckedChange = { viewModel.setSetting(SettingsViewModel.KEY_NOTIFY_ADD, it.toString()) }
                )
                ToggleSetting(
                    label = "Notify when AI ignores something major",
                    checked = uiState.notifyOnIgnore,
                    onCheckedChange = { viewModel.setSetting(SettingsViewModel.KEY_NOTIFY_IGNORE, it.toString()) }
                )
                ToggleSetting(
                    label = "Daily summary notification",
                    checked = uiState.dailySummary,
                    onCheckedChange = { viewModel.setSetting(SettingsViewModel.KEY_DAILY_SUMMARY, it.toString()) }
                )
            }

            // Privacy Controls
            SettingsSection("Privacy", Icons.Default.Lock) {
                ToggleSetting(
                    label = "Use local AI (Gemma) only",
                    checked = uiState.localAiOnly,
                    onCheckedChange = { viewModel.setSetting(SettingsViewModel.KEY_LOCAL_AI_ONLY, it.toString()) }
                )
                Text(
                    "Local processing ensures your emails never leave your device for AI analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun AccountItem(
    account: AccountEntity,
    availableCalendars: List<dev.haas.vakya.data.google.CalendarEntry>,
    onUpdate: (AccountEntity) -> Unit,
    onRemove: (AccountEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.displayName ?: "Unknown", fontWeight = FontWeight.Bold)
                    Text(account.email, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onRemove(account) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Gmail Sync", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = account.isGmailEnabled,
                    onCheckedChange = { onUpdate(account.copy(isGmailEnabled = it)) }
                )
            }

            if (availableCalendars.isNotEmpty()) {
                val selectedCalendar = availableCalendars.find { it.id == account.targetCalendarId }?.summary ?: account.targetCalendarId
                DropdownSetting(
                    label = "Target Calendar",
                    options = availableCalendars.map { it.summary },
                    selected = selectedCalendar,
                    onSelected = { summary ->
                        val calId = availableCalendars.find { it.summary == summary }?.id ?: "primary"
                        onUpdate(account.copy(targetCalendarId = calId))
                    }
                )
            } else {
                Text("No calendars found", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}


@Composable
fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().width(150.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
