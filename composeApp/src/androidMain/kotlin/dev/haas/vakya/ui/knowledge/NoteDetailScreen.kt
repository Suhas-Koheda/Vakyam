package dev.haas.vakya.ui.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var tags by remember { mutableStateOf("") }

    LaunchedEffect(noteId) {
        note = viewModel.getNoteById(noteId)
        note?.let {
            title = it.title
            content = it.content
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
                                val updatedNote = it.copy(title = title, content = content, tags = tags)
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = currentNote.title, style = MaterialTheme.typography.headlineSmall)
                    HorizontalDivider()
                    Text(text = currentNote.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    
                    if (currentNote.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentNote.tags.split(",").forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag.trim()) })
                            }
                        }
                    }

                    if (currentNote.linkedEventId != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Text("Linked to Calendar Event #${currentNote.linkedEventId}")
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
