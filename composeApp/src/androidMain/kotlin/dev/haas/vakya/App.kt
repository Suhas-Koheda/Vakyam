package dev.haas.vakya

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.Font
import dev.haas.vakya.common.SentenceSummarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import vakya.composeapp.generated.resources.*
import vakya.composeapp.generated.resources.Res
import vakya.composeapp.generated.resources.compose_multiplatform
import vakya.composeapp.generated.resources.jetbrainsmono_regular
import vakya.composeapp.generated.resources.jetbrainsmono_bold

@Composable
fun VakyaTypography(): Typography {
    val jetbrainsMono = FontFamily(
        Font(Res.font.jetbrainsmono_regular, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_bold, FontWeight.Bold)
    )

    return Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = jetbrainsMono),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = jetbrainsMono),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = jetbrainsMono),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = jetbrainsMono),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = jetbrainsMono),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = jetbrainsMono),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = jetbrainsMono),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = jetbrainsMono),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = jetbrainsMono),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = jetbrainsMono),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = jetbrainsMono),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = jetbrainsMono),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = jetbrainsMono),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = jetbrainsMono),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = jetbrainsMono)
    )
}

@Composable
@Preview
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val userState by authManager.userState.collectAsState()
    val scope = rememberCoroutineScope()

    MaterialTheme(typography = VakyaTypography()) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            if (userState == null) {
                Text(
                    text = "Welcome to Vakya",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = {
                        scope.launch {
                            authManager.signIn()
                        }
                    }
                ) {
                    Text("Sign in with Google")
                }
            } else {
                Text(
                    text = "Hello, ${userState?.displayName ?: "User"}!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = userState?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { authManager.signOut() }) {
                    Text("Sign Out")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

                // Gemma Test Section
                Text("Gemma Model Test", style = MaterialTheme.typography.titleMedium)
                var inputText by remember { mutableStateOf("Gemma is a family of lightweight, state-of-the-art open models from Google, built from the same research and technology used to create the Gemini models.") }
                var summaryResult by remember { mutableStateOf("") }
                var isProcessing by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Input text for Gemma") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            summaryResult = "Processing..."
                            try {
                                summaryResult = withContext(Dispatchers.Default) {
                                    SentenceSummarizer.summarize(inputText)
                                }
                            } catch (e: Exception) {
                                summaryResult = "Error: ${e.message}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "Summarizing..." else "Test Summarization")
                }

                if (summaryResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Result:", style = MaterialTheme.typography.labelLarge)
                            Text(summaryResult, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}