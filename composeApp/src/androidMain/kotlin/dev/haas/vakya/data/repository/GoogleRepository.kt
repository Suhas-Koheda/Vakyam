package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.ProcessedEmailDao
import dev.haas.vakya.data.database.ProcessedEmailEntity
import dev.haas.vakya.data.google.GmailApi
import dev.haas.vakya.domain.models.EmailMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GoogleRepository(
    private val gmailApi: GmailApi,
    private val accountDao: AccountDao,
    private val processedEmailDao: ProcessedEmailDao
) {
    suspend fun getUnreadEmails(accountEmail: String, accessToken: String): List<EmailMessage> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val authHeader = "Bearer $accessToken"
        val query = "is:unread newer_than:1d"
        val response = try {
            gmailApi.listMessages(query = query, authHeader = authHeader)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
        
        val messageRefs = response.messages ?: return@withContext emptyList()
        
        val unprocessed = messageRefs.filter { !processedEmailDao.isProcessed(it.id) }
        
        val results: List<EmailMessage> = coroutineScope {
            unprocessed.map { msgRef ->
                async {
                    try {
                        val fullMsg = gmailApi.getMessage(id = msgRef.id, authHeader = authHeader)
                        val subject = fullMsg.payload.headers.find { it.name == "Subject" }?.value ?: "(No Subject)"
                        val from = fullMsg.payload.headers.find { it.name == "From" }?.value ?: "Unknown"
                        
                        val bodyText = extractBody(fullMsg) ?: fullMsg.snippet
                        
                        EmailMessage(
                            id = fullMsg.id,
                            threadId = fullMsg.threadId,
                            subject = subject,
                            sender = from,
                            snippet = fullMsg.snippet,
                            body = bodyText,
                            timestamp = System.currentTimeMillis(),
                            accountEmail = accountEmail
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        
        return@withContext results
    }

    private fun extractBody(message: dev.haas.vakya.data.google.GmailMessageResponse): String? {
        // Try to find text/plain part
        val parts = message.payload.parts
        if (parts != null) {
            val plainTextPart = parts.find { it.mimeType == "text/plain" }
            if (plainTextPart?.body?.data != null) {
                return decodeBase64(plainTextPart.body.data)
            }
        }
        
        // Fallback to top-level body if parts is null
        if (message.payload.body?.data != null) {
            return decodeBase64(message.payload.body.data)
        }
        
        return null
    }

    private fun decodeBase64(base64Data: String): String {
        return try {
            val decodedBytes = android.util.Base64.decode(
                base64Data, 
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun markEmailProcessed(emailId: String, accountEmail: String) {
        processedEmailDao.markProcessed(ProcessedEmailEntity(emailId, accountEmail))
    }
}
