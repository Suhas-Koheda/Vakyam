package dev.haas.vakya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import dev.haas.vakya.data.database.AiActionLogEntity
import dev.haas.vakya.data.database.CalendarEventEntity
import dev.haas.vakya.ui.viewmodel.DashboardViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDebug: () -> Unit,
    onNavigateToAddTask: () -> Unit,
    onNavigateToAddNote: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    if (uiState.weeklySummary != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSummary() },
            title = { Text("Weekly Summary", fontWeight = FontWeight.Bold) },
            text = { Text(uiState.weeklySummary ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSummary() }) {
                    Text("Close")
                }
            }
        )
    }

    if (uiState.dailyBriefing != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBriefing() },
            title = { Text("Daily Briefing", fontWeight = FontWeight.Bold) },
            text = { Text(uiState.dailyBriefing ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissBriefing() }) {
                    Text("Great!")
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateManualEventDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { sentence ->
                viewModel.processManualSentence(sentence)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Vakya", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Personal Assistant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncEmails() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Emails")
                    }
                    IconButton(onClick = onOpenDebug) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Action")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionChip(
                        icon = Icons.Default.AddTask,
                        label = "Task",
                        onClick = onNavigateToAddTask
                    )
                    ActionChip(
                        icon = Icons.Default.NoteAdd,
                        label = "Note",
                        onClick = onNavigateToAddNote
                    )
                    ActionChip(
                        icon = Icons.Default.Search,
                        label = "Explore",
                        onClick = onNavigateToKnowledge
                    )
                    ActionChip(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { /* Add settings navigation if needed */ }
                    )
                }
            }

            // AI Insight Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightCard(
                        modifier = Modifier.width(180.dp),
                        onClick = { viewModel.generateDailyBriefing() },
                        icon = Icons.Default.WbSunny,
                        title = "Daily Briefing",
                        subtitle = "Morning update",
                        color = Color(0xFFFFB300)
                    )
                    InsightCard(
                        modifier = Modifier.width(180.dp),
                        onClick = { viewModel.generateWeeklySummary() },
                        icon = Icons.Default.AutoAwesome,
                        title = "Weekly Insight",
                        subtitle = "Performance wrap",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // TODAY Section
            item {
                SectionHeader("TODAY", MaterialTheme.colorScheme.error)
            }
            
            if (uiState.todayEvents.isEmpty()) {
                item { EmptyState("Your schedule is clear for today!") }
            } else {
                items(uiState.todayEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onIgnore = { viewModel.markAsIgnored(event.id) },
                        onDelete = { viewModel.deleteEvent(event) }
                    )
                }
            }

            // KNOWLEDGE Section
            item {
                SectionHeader("KNOWLEDGE BASE", MaterialTheme.colorScheme.tertiary)
            }
            if (uiState.recentNotes.isEmpty()) {
                item { EmptyState("Start capturing knowledge with AI") }
            } else {
                items(uiState.recentNotes, key = { it.id }) { note ->
                    dev.haas.vakya.ui.knowledge.NoteCard(note = note, onClick = { onNoteClick(note.id) })
                }
                item {
                    TextButton(onClick = onNavigateToKnowledge, modifier = Modifier.fillMaxWidth()) {
                        Text("Explore All Notes", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // UPCOMING Section
            item {
                SectionHeader("FUTURE PERSPECTIVE", MaterialTheme.colorScheme.primary)
            }
            
            if (uiState.upcomingEvents.isEmpty()) {
                item { EmptyState("No upcoming commitments") }
            } else {
                items(uiState.upcomingEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onIgnore = { viewModel.markAsIgnored(event.id) },
                        onDelete = { viewModel.deleteEvent(event) }
                    )
                }
            }

            // RECENT AI ACTIONS
            item {
                SectionHeader("AI ANALYTICS", MaterialTheme.colorScheme.secondary)
            }
            
            if (uiState.recentActions.isEmpty()) {
                item { EmptyState("Cognitive core initialized. Waiting for input.") }
            } else {
                items(uiState.recentActions, key = { it.id }) { action ->
                    AiActionItem(action)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .background(color.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
fun EventCard(
    event: CalendarEventEntity,
    onIgnore: () -> Unit,
    onDelete: () -> Unit
) {
    val urgencyColor = when {
        event.status == "IGNORED" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> {
            val now = System.currentTimeMillis()
            val diff = event.startTime - now
            when {
                diff < 24 * 60 * 60 * 1000 -> Color(0xFFEF5350) // Red: today
                diff < 48 * 60 * 60 * 1000 -> Color(0xFFFFA726) // Orange: tomorrow
                else -> Color(0xFF42A5F5) // Blue: upcoming
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(urgencyColor, shape = MaterialTheme.shapes.extraSmall))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = event.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = urgencyColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(event.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.Email, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (event.status != "IGNORED") {
                    TextButton(onClick = onIgnore) {
                        Text("Ignore", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun AiActionItem(action: AiActionLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Email: \"${action.subject}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                if (action.actionSummary != "Ignored") {
                    Text(
                        text = "Action: ${action.actionSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}


@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = color, 
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox, 
                contentDescription = null, 
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                message, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CreateManualEventDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var sentence by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create AI Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type a sentence like \"Meeting with Sam tomorrow at 10am\"", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    label = { Text("Event details") },
                    placeholder = { Text("Lunch at 1pm...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (sentence.isNotBlank()) onCreate(sentence) }) {
                Text("Analyze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatTime(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
}
