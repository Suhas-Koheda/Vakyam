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
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAddAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (uiState.showConsentSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowConsentSheet(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Google Permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "To help you stay on top of your schedule, Vakya needs permission to:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PermissionReasonItem(Icons.Default.Email, "Read emails", "To find meeting invitations and deadlines automatically.")
                PermissionReasonItem(Icons.Default.CalendarMonth, "Manage Calendar", "To add extracted meetings to your schedule.")
                PermissionReasonItem(Icons.Default.Dns, "Local Processing", "Your data stays on this device. AI analysis is local.")

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        viewModel.setShowConsentSheet(false)
                        onAddAccount() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("I Understand, Let's Sign In")
                }
                
                TextButton(onClick = { viewModel.setShowConsentSheet(false) }) {
                    Text("Maybe Later")
                }
            }
        }
    }

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
            val trackedCount = uiState.accounts.count { it.isGmailEnabled }
            SettingsSection("Google Accounts", Icons.Default.AccountCircle) {

                // Tracked accounts summary badge
                if (uiState.accounts.isNotEmpty()) {
                    Surface(
                        color = if (trackedCount > 0) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (trackedCount > 0) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (trackedCount == 0) "No accounts being tracked"
                                       else if (trackedCount == 1) "1 account being tracked for Gmail"
                                       else "$trackedCount accounts being tracked for Gmail",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (trackedCount > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.accounts.forEach { account ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                        ) {
                            AccountItem(
                                account = account,
                                availableCalendars = uiState.calendars[account.email] ?: emptyList(),
                                onUpdate = { viewModel.updateAccount(it) },
                                onRemove = { viewModel.removeAccount(it) },
                                onAddAccount = onAddAccount,
                                onCreateCalendar = { viewModel.createCalendar(account.email, it) }
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { viewModel.setShowConsentSheet(true) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.isSigningIn,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (uiState.isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Google Account")
                    }
                }
                
                Text(
                    "Toggle \"Track Gmail\" on each account you want Vakya to monitor for events. Each account syncs to its own calendar by default.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }


            // Email → Calendar Routing
            SettingsSection("Email → Calendar Routing", Icons.Default.SwapHoriz) {
                Text(
                    "Route Gmail from one account into another account's calendar. Useful when you read work emails on a personal account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownSetting(
                            label = "Source Gmail",
                            options = listOf("None") + uiState.accounts.map { it.email },
                            selected = uiState.gmailSourceEmail ?: "None",
                            onSelected = { 
                                if (it == "None") viewModel.setSetting(SettingsViewModel.KEY_GMAIL_SOURCE, "")
                                else viewModel.setSetting(SettingsViewModel.KEY_GMAIL_SOURCE, it) 
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownSetting(
                            label = "Dest. Calendar",
                            options = listOf("None") + uiState.accounts.map { it.email },
                            selected = uiState.calendarDestEmail ?: "None",
                            onSelected = { 
                                if (it == "None") viewModel.setSetting(SettingsViewModel.KEY_CALENDAR_DEST, "")
                                else viewModel.setSetting(SettingsViewModel.KEY_CALENDAR_DEST, it) 
                            }
                        )
                    }
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

            SettingsSection("AI Behavior", Icons.Default.AutoAwesome) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Confidence Threshold",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "${(uiState.confidenceThreshold * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = uiState.confidenceThreshold,
                        onValueChange = { viewModel.setSetting(SettingsViewModel.KEY_CONFIDENCE, it.toString()) },
                        valueRange = 0.5f..0.9f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Balanced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Strict", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
    onRemove: (AccountEntity) -> Unit,
    onAddAccount: () -> Unit,
    onCreateCalendar: (String) -> Unit
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
            
            // Gmail Tracking toggle — prominent and descriptive
            Surface(
                color = if (account.isGmailEnabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (account.isGmailEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Track Gmail",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (account.isGmailEnabled) "Scanning for events"
                            else "Not being monitored",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = account.isGmailEnabled,
                        onCheckedChange = { onUpdate(account.copy(isGmailEnabled = it)) }
                    )
                }
            }

            if (availableCalendars.isNotEmpty()) {
                val selectedCalendar = availableCalendars.find { it.id == account.targetCalendarId }?.summary ?: account.targetCalendarId ?: "Not Selected"
                var showCreateDialog by remember { mutableStateOf(false) }

                if (showCreateDialog) {
                    var newCalName by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showCreateDialog = false },
                        title = { Text("Create Calendar", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = newCalName,
                                onValueChange = { newCalName = it },
                                label = { Text("Calendar Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newCalName.isNotBlank()) {
                                    onCreateCalendar(newCalName)
                                }
                                showCreateDialog = false
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                DropdownSetting(
                    label = "Target Calendar",
                    options = availableCalendars.map { it.summary } + listOf("+ Create New Calendar"),
                    selected = selectedCalendar,
                    onSelected = { summary ->
                        if (summary == "+ Create New Calendar") {
                            showCreateDialog = true
                        } else {
                            val calId = availableCalendars.find { it.summary == summary }?.id ?: "primary"
                            onUpdate(account.copy(targetCalendarId = calId))
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (account.accessToken == null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (account.accessToken == null) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (account.accessToken == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (account.accessToken == null) "Permission required for Calendar" else "No calendars found",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (account.accessToken == null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (account.accessToken == null) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = onAddAccount, contentPadding = PaddingValues(0.dp)) {
                                Text("AUTHORIZE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionReasonItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) 
                        },
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

