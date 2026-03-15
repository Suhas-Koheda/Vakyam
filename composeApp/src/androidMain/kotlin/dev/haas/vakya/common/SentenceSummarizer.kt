package dev.haas.vakya.common

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dev.haas.vakya.AppContextHolder
import java.io.File

actual object SentenceSummarizer {

    private const val TAG = "SentenceSummarizer"
    private const val MODEL_PATH = "/data/user/0/dev.haas.vakya/files/gemma.task"

    actual fun summarize(text: String): String {
        Log.d(TAG, "Starting summarization for: ${text.take(20)}...")
        
        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            val error = "Model file not found at $MODEL_PATH"
            Log.e(TAG, error)
            return error
        }
        
        if (!modelFile.canRead()) {
            val error = "Model file found but not readable: $MODEL_PATH"
            Log.e(TAG, error)
            return error
        }

        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(128)
                .build()

            Log.d(TAG, "Creating LlmInference instance...")
            val llm = LlmInference.createFromOptions(
                AppContextHolder.context,
                options
            )

            llm.use { 
                Log.d(TAG, "Generating response...")
                val result = it.generateResponse("Summarize this text in one short sentence: $text")
                Log.d(TAG, "Summarization successful")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during summarization", e)
            "Error: ${e.localizedMessage ?: e.message}"
        }
    }
}
