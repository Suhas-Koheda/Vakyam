package dev.haas.vakya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
                title = { Text("Vakya Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenDebug) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug")
                    }
                }
            )
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
            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedButton(
                        onClick = onNavigateToAddTask,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Task")
                    }
                    ElevatedButton(
                        onClick = onNavigateToAddNote,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Note")
                    }
                }
            }

            // Summaries Section
            // Summaries Section
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.generateDailyBriefing() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Daily Briefing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.generateWeeklySummary() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Weekly Insight", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
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
                item { EmptyState("AI is monitoring your accounts...") }
            } else {
                items(uiState.recentActions, key = { it.id }) { action ->
                    AiActionItem(action)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, shape = MaterialTheme.shapes.small))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                Text(
                    text = "Action: ${action.actionSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
