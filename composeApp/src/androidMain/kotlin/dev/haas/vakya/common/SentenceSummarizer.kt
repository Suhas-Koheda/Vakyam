package dev.haas.vakya.common

import android.util.Log
import dev.haas.vakya.AppContextHolder

actual object SentenceSummarizer {
    private const val TAG = "SentenceSummarizer"

    actual suspend fun summarize(text: String): String {
        Log.d(TAG, "Starting summarization using shared GemmaParser")
        return try {
            val prompt = "Summarize this text in one short sentence: $text"
            AppContextHolder.gemmaParser.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error during summarization", e)
            "Error: ${e.localizedMessage ?: e.message}"
        }
    }
}
