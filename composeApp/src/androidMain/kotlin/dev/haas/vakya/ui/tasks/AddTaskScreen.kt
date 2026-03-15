package dev.haas.vakya.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.haas.vakya.data.database.CalendarEventEntity
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onSaveRequested: (CalendarEventEntity) -> Unit,
    onBack: () -> Unit,
    availableEmails: List<String>
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var courseTag by remember { mutableStateOf("") }
    var selectedEmail by remember { mutableStateOf(availableEmails.firstOrNull() ?: "") }
    
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    val timeState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Manual Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalCalendar = Calendar.getInstance()
                            finalCalendar.timeInMillis = selectedDate
                            finalCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                            finalCalendar.set(Calendar.MINUTE, selectedMinute)
                            
                            val event = CalendarEventEntity(
                                title = if (courseTag.isNotEmpty()) "[$courseTag] $title" else title,
                                startTime = finalCalendar.timeInMillis,
                                source = "manual",
                                accountEmail = selectedEmail,
                                status = "PENDING" // Goes to review queue
                            )
                            onSaveRequested(event)
                            onBack()
                        },
                        enabled = title.isNotBlank() && selectedEmail.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = courseTag,
                onValueChange = { courseTag = it },
                label = { Text("Course Tag (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. CS101") }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Date", style = MaterialTheme.typography.labelSmall)
                        Text(dateFormatter.format(Date(selectedDate)), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Time", style = MaterialTheme.typography.labelSmall)
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                        }
                        Text(timeFormatter.format(cal.time), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (availableEmails.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEmail,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableEmails.forEach { email ->
                            DropdownMenuItem(
                                text = { Text(email) },
                                onClick = {
                                    selectedEmail = email
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timeState.hour
                    selectedMinute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}
