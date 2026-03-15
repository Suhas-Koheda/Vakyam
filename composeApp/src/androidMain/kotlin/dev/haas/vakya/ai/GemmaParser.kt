package dev.haas.vakya.ai

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.haas.vakya.AppContextHolder
import dev.haas.vakya.domain.models.ExtractedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GemmaParser {
    private val TAG = "GemmaParser"
    private val MODEL_PATH = "/data/user/0/dev.haas.vakya/files/gemma.task"
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(ExtractedEvent::class.java)

    private var llmInference: LlmInference? = null

    private fun getOrCreateLlm(): LlmInference? {
        if (llmInference != null) return llmInference
        
        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found at $MODEL_PATH")
            return null
        }

        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(AppContextHolder.context, options)
            llmInference
        } catch (e: Exception) {
            Log.e(TAG, "Inference engine creation error", e)
            null
        }
    }

    suspend fun parseEmail(subject: String, body: String): ExtractedEvent? = withContext(Dispatchers.IO) {
        val safeSubject = subject.replace("```", "")
        val safeBody = body.replace("```", "")

        val prompt = """
            Extract structured information from the following email.
            Return ONLY a valid JSON object.
            Format:
            {
              "type": "event | assignment | announcement | ignore",
              "title": "",
              "description": "",
              "start_time": "",
              "end_time": "",
              "deadline": "",
              "course": "",
              "confidence": 0.0
            }
            Rules:
            - type: "event" for meetings/classes, "assignment" for homework/deadlines, "announcement" for updates, "ignore" for marketing/spam.
            - confidence: 0.0 to 1.0.
            - If no event/deadline, set type to "ignore".
            - Ignore newsletters and marketing.
            
            Email Subject:
            ```
            ${safeSubject.take(100)}
            ```
            Email Body:
            ```
            ${safeBody.take(1000)}
            ```
        """.trimIndent()

        val response = runInference(prompt) ?: return@withContext null
        return@withContext parseJsonResponse(response)
    }

    suspend fun parseSentence(sentence: String): ExtractedEvent? = withContext(Dispatchers.IO) {
        val safeSentence = sentence.replace("```", "")
        val prompt = """
            Extract structured information from the following sentence.
            Return ONLY a valid JSON object.
            Format:
            {
              "type": "event | assignment | announcement | ignore",
              "title": "",
              "description": "",
              "start_time": "",
              "end_time": "",
              "deadline": "",
              "course": "",
              "confidence": 0.0
            }
            Sentence:
            ```
            $safeSentence
            ```
        """.trimIndent()

        val response = runInference(prompt) ?: return@withContext null
        return@withContext parseJsonResponse(response)
    }

    private fun parseJsonResponse(response: String): ExtractedEvent? {
        try {
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                return adapter.fromJson(jsonStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON: $response", e)
        }
        return null
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        runInference(prompt) ?: "Error: Could not generate response."
    }

    internal fun runInference(prompt: String): String? {
        val llm = getOrCreateLlm() ?: return null
        return try {
            llm.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            null
        }
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
