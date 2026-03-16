package dev.haas.vakya.common

expect object SentenceSummarizer {
    suspend fun summarize(text: String): String
}
