package dev.haas.vakya.ui.reviewQueue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.haas.vakya.data.database.pendingEvents.PendingEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ReviewQueueScreen(viewModel: ReviewQueueViewModel) {
    val events by viewModel.pendingEvents.collectAsState()

    var eventToEdit by remember { mutableStateOf<PendingEvent?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "AI Review Queue",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pending events", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Group by Date for fast scanning
            val grouped = events.groupBy { 
                try {
                    ZonedDateTime.parse(it.startTime).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                } catch (e: Exception) {
                    "Unknown Date"
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                grouped.forEach { (dateStr, dateEvents) ->
                    item {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(dateEvents) { event ->
                        PendingEventCard(
                            event = event,
                            onApprove = { viewModel.approveEvent(it) },
                            onReject = { viewModel.rejectEvent(it) },
                            onEdit = { eventToEdit = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (eventToEdit != null) {
        EditEventDialog(
            event = eventToEdit!!,
            onDismiss = { eventToEdit = null },
            onSave = { updatedEvent ->
                viewModel.updateEvent(updatedEvent)
                eventToEdit = null
            }
        )
    }
}

@Composable
fun PendingEventCard(
    event: PendingEvent,
    onApprove: (PendingEvent) -> Unit,
    onReject: (PendingEvent) -> Unit,
    onEdit: (PendingEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Urgency / Confidence indicator
                val confidenceColor = if (event.confidence > 0.8f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                Box(
                    modifier = Modifier
                        .background(confidenceColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Conf: ${(event.confidence * 100).toInt()}%",
                        color = confidenceColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val timeStr = try {
                ZonedDateTime.parse(event.startTime).format(DateTimeFormatter.ofPattern("hh:mm a"))
            } catch (e: Exception) {
                event.startTime
            }

            Text(
                text = "Time: $timeStr",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Source: ${event.emailId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onEdit(event) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onReject(event) }) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { onApprove(event) }) {
                    Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
fun EditEventDialog(
    event: PendingEvent,
    onDismiss: () -> Unit,
    onSave: (PendingEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var time by remember { mutableStateOf(event.startTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (ISO string)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(event.copy(title = title, startTime = time))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
