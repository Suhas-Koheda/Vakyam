package dev.haas.vakya.domain.ai

import dev.haas.vakya.data.database.KnowledgeNoteEntity
import dev.haas.vakya.ai.GemmaParser

class SemanticSearchUseCase(
    private val gemmaParser: GemmaParser
) {
    suspend fun search(query: String, candidates: List<KnowledgeNoteEntity>): List<KnowledgeNoteEntity> {
        if (candidates.isEmpty()) return emptyList()
        
        // Simple heuristic filter first to reduce Gemma load
        val basicMatches = candidates.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.content.contains(query, ignoreCase = true) 
        }
        
        if (basicMatches.isEmpty()) return emptyList()

        val context = basicMatches.take(5).mapIndexed { index, note ->
            "[$index] Title: ${note.title}, Content: ${note.content.take(100)}"
        }.joinToString("\n")

        val prompt = """
            Query: "$query"
            
            From the following notes, list only the numeric IDs (e.g. [0], [2]) of the notes most relevant to the query, in order of relevance. 
            If none are relevant, return "NONE".
            
            $context
        """.trimIndent()

        return try {
            val response = gemmaParser.generateResponse(prompt)
            if (response.contains("NONE", ignoreCase = true)) {
                return emptyList()
            }
            
            val ids = "\\[(\\d+)\\]".toRegex().findAll(response)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .distinct()
                .toList()
            
            ids.mapNotNull { index -> basicMatches.getOrNull(index) }
        } catch (e: Exception) {
            basicMatches // Fallback to basic matches on error
        }
    }
}
