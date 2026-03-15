package dev.haas.vakya.ui.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    viewModel: KnowledgeViewModel,
    onBack: () -> Unit
) {
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    var isSuggestingTags by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Note") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSuggestedTags()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveNote(title, content, tags)
                            viewModel.clearSuggestedTags()
                            onBack()
                        },
                        enabled = title.isNotBlank() && content.isNotBlank()
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
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            if (suggestedTags.isNotEmpty()) {
                Text("AI Suggested Tags:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedTags.forEach { tag ->
                        FilterChip(
                            selected = tags.contains(tag),
                            onClick = {
                                if (tags.contains(tag)) {
                                    tags = tags.split(",").map { it.trim() }.filter { it != tag }.joinToString(", ")
                                } else {
                                    tags = (tags.split(",").map { it.trim() }.filter { it.isNotEmpty() } + tag).joinToString(", ")
                                }
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        isSuggestingTags = true
                        viewModel.suggestTags(content)
                        isSuggestingTags = false
                    },
                    enabled = !isSuggestingTags && content.isNotBlank()
                ) {
                    if (isSuggestingTags) {
                        CircularProgressIndicator(size = 24.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Suggest Tags")
                    }
                }
            }
        }
    }
}
