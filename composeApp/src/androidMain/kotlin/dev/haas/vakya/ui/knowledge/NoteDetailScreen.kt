package dev.haas.vakya.ui.knowledge

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.haas.vakya.data.database.KnowledgeNoteEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: KnowledgeViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf<KnowledgeNoteEntity?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    LaunchedEffect(noteId) {
        note = viewModel.getNoteById(noteId)
        note?.let {
            title = it.title
            content = it.content
            summary = it.summary ?: ""
            tags = it.tags
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Note" else "Note Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = {
                            note?.let {
                                val updatedNote = it.copy(title = title, content = content, tags = tags, summary = summary.ifBlank { null })
                                viewModel.updateNote(updatedNote)
                                note = updatedNote
                                isEditing = false
                            }
                        }) {
                            Text("Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            note?.let {
                                viewModel.deleteNote(it)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            androidx.compose.animation.AnimatedContent(
                targetState = isEditing,
                label = "editTransition",
                transitionSpec = {
                    androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                }
            ) { editing ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (editing) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Content") },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("Summary") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    } else {
                        Text(
                            text = currentNote.title, 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        currentNote.summary?.let { 
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = it, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = currentNote.content, 
                            style = MaterialTheme.typography.bodyLarge, 
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (currentNote.tags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                currentNote.tags.split(",").forEach { tag ->
                                    FilterChip(
                                        selected = true,
                                        onClick = {},
                                        label = { Text(tag.trim()) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        if (currentNote.linkedEventId != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                    Text("Linked to Calendar Event #${currentNote.linkedEventId}", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        } else {
                            var isConverting by remember { mutableStateOf(false) }
                            val snackbarHostState = remember { SnackbarHostState() }

                            Box {
                                Button(
                                    onClick = {
                                        isConverting = true
                                        viewModel.convertToTask(currentNote) {
                                            isConverting = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Added to Review Queue")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isConverting,
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    if (isConverting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("AI Analyzing...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Convert to Action with AI", fontWeight = FontWeight.Bold)
                                    }
                                }
                                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
                            }
                        }
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        }
    }
}
