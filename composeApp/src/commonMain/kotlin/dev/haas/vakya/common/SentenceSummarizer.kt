package dev.haas.vakya.common

expect object SentenceSummarizer {
    fun summarize(text: String): String
}
