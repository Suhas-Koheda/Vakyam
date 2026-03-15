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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ReviewQueueScreen(viewModel: ReviewQueueViewModel) {
    val events by viewModel.pendingEvents.collectAsState()

    var eventToReject by remember { mutableStateOf<PendingEvent?>(null) }

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
                            onReject = { eventToReject = it },
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

    if (eventToReject != null) {
        AlertDialog(
            onDismissRequest = { eventToReject = null },
            title = { Text("Reject Event") },
            text = { Text("Should AI learn to ignore similar \"${eventToReject?.title?.split(" ")?.firstOrNull() ?: ""}\" events in the future?") },
            confirmButton = {
                TextButton(onClick = {
                    eventToReject?.let { viewModel.rejectEvent(it, storeFeedback = true) }
                    eventToReject = null
                }) { Text("Yes, Learn") }
            },
            dismissButton = {
                TextButton(onClick = {
                    eventToReject?.let { viewModel.rejectEvent(it, storeFeedback = false) }
                    eventToReject = null
                }) { Text("No, Just Reject") }
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
                val confidenceColor = if (event.confidence > 0.8f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: PendingEvent,
    onDismiss: () -> Unit,
    onSave: (PendingEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    
    val initialDateTime = remember(event.startTime) {
        try {
            ZonedDateTime.parse(event.startTime)
        } catch (e: Exception) {
            ZonedDateTime.now()
        }
    }
    
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDateTime.toLocalTime()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date", style = MaterialTheme.typography.labelSmall)
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Time", style = MaterialTheme.typography.labelSmall)
                            Text(selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newZonedDateTime = ZonedDateTime.of(selectedDate, selectedTime, ZoneId.systemDefault())
                val newIsoString = newZonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                onSave(event.copy(title = title, description = description, startTime = newIsoString))
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

private fun ZonedDateTime.toLocalDate(): java.time.LocalDate = this.toLocalDate()
private fun ZonedDateTime.toLocalTime(): java.time.LocalTime = this.toLocalTime()
