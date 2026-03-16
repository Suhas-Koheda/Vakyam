package dev.haas.vakya.ui.knowledge

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddNoteScreen(
    viewModel: KnowledgeViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    var isEnhancing by remember { mutableStateOf(false) }

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
                            viewModel.saveNote(title, content, tags, summary)
                            viewModel.clearSuggestedTags()
                            onBack()
                        },
                        enabled = (title.isNotBlank() || content.isNotBlank())
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                    .heightIn(min = 200.dp)
            )

            if (suggestedTags.isNotEmpty()) {
                Text("AI Suggested Tags:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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

            if (summary.isNotEmpty()) {
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary (Auto-generated)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            Button(
                onClick = {
                    isEnhancing = true
                    viewModel.suggestTags(content)
                    viewModel.generateTitleAndSummary(content) { genTitle, genSummary ->
                        if (title.isBlank()) title = genTitle
                        summary = genSummary
                        isEnhancing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEnhancing && content.isNotBlank()
            ) {
                if (isEnhancing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Analyzing...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-generate Title & Summary")
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
                        viewModel.suggestTags(content)
                    },
                    enabled = !isEnhancing && content.isNotBlank()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Suggest Tags")
                }
            }
        }
    }
}
