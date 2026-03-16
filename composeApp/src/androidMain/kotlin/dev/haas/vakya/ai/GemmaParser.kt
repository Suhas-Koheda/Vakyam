package dev.haas.vakya.ai

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.haas.vakya.AppContextHolder
import dev.haas.vakya.domain.models.ExtractedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class GemmaParser {
    private val TAG = "GemmaParser"
    private val MODEL_PATH by lazy { "/data/local/tmp/llm/gemma3-1b-it-int4.task" }
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(ExtractedEvent::class.java)

    private val mutex = Mutex()
    private var llmInference: LlmInference? = null

    private fun getOrCreateLlm(): LlmInference? {
        if (llmInference != null) return llmInference
        
        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found at $MODEL_PATH")
            return null
        }

        // Validation: Check if file exists and has size
        if (modelFile.length() < 1024 * 1024) { // Expecting at least 1MB for an LLM
            Log.e(TAG, "Model file is too small or empty. Size: ${modelFile.length()}")
            return null
        }

        // Log the first few bytes for debugging purposes
        try {
            val fis = java.io.FileInputStream(modelFile)
            val header = ByteArray(4)
            fis.read(header)
            fis.close()
            val hex = header.joinToString("") { "%02x".format(it) }
            Log.d(TAG, "Model file header (first 4 bytes): $hex")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading model header", e)
        }

        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(2048)
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
            Extract structured information from the email.

Return **ONLY a valid JSON object**.
No explanations, no markdown, no extra text.

Schema:
{
"type": "event | task | reminder | info | ignore",
"title": "short title",
"description": "clear summary",
"start_time": "ISO 8601 or empty",
"end_time": "ISO 8601 or empty",
"deadline": "ISO 8601 or empty",
"context": "Work | School | Personal | System | Other",
"rationale": "short reason",
"confidence": 0.0-1.0
}

Rules:

1. Set `"type": "ignore"` and `"confidence": 0.0` if the email is:

* security alert
* login notification
* password reset
* verification code
* marketing or newsletter
* automated system notification with no required action (e.g. login success, password changed)

1.1. NEVER ignore emails about:
* bill payments, due dates or credit card statements
* subscription renewals
* appointments or bookings
Mark these as **task** or **reminder**.
2. Type meanings:

* **event** → meeting, class, scheduled call, webinar
* **task** → something the user must do
* **reminder** → alert about upcoming event or deadline
* **info** → informational update
* **ignore** → irrelevant email

3. Date rules:

* Use **ISO 8601 format**.
* Only fill `start_time`, `end_time`, or `deadline` if explicitly mentioned.
* Otherwise return empty string "".

4. If multiple items exist, extract **the most important one**.

Email Subject:
${safeSubject.take(150)}

Email Body:
${safeBody.take(4000)}

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
              "type": "event | task | reminder | info | ignore",
              "title": "Short descriptive title",
              "description": "Brief summary",
              "start_time": "ISO 8601 or empty",
              "end_time": "ISO 8601 or empty",
              "deadline": "ISO 8601 or empty",
              "context": "Related context",
              "rationale": "Why this was extracted",
              "confidence": 0.0 to 1.0
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

    internal suspend fun runInference(prompt: String): String? = mutex.withLock {
        val llm = getOrCreateLlm() ?: return@withLock null
        return@withLock try {
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
