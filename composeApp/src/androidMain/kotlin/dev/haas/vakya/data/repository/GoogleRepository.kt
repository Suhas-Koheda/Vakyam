package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.ProcessedEmailDao
import dev.haas.vakya.data.database.ProcessedEmailEntity
import dev.haas.vakya.data.google.GmailApi
import dev.haas.vakya.domain.models.EmailMessage
import kotlinx.coroutines.flow.first

class GoogleRepository(
    private val gmailApi: GmailApi,
    private val accountDao: AccountDao,
    private val processedEmailDao: ProcessedEmailDao
) {
    suspend fun getUnreadEmails(accountEmail: String, accessToken: String): List<EmailMessage> {
        val authHeader = "Bearer $accessToken"
        // Query: unread, newer than 1d
        val query = "is:unread newer_than:1d"
        val response = gmailApi.listMessages(query = query, authHeader = authHeader)
        
        val messages = mutableListOf<EmailMessage>()
        response.messages?.forEach { msgRef ->
            if (!processedEmailDao.isProcessed(msgRef.id)) {
                val fullMsg = gmailApi.getMessage(id = msgRef.id, authHeader = authHeader)
                val subject = fullMsg.payload.headers.find { it.name == "Subject" }?.value ?: "(No Subject)"
                val from = fullMsg.payload.headers.find { it.name == "From" }?.value ?: "Unknown"
                
                messages.add(
                    EmailMessage(
                        id = fullMsg.id,
                        threadId = fullMsg.threadId,
                        subject = subject,
                        sender = from,
                        snippet = fullMsg.snippet,
                        body = fullMsg.snippet, // Simplified: body extraction from parts can be complex
                        timestamp = System.currentTimeMillis(),
                        accountEmail = accountEmail
                    )
                )
            }
        }
        return messages
    }

    suspend fun markEmailProcessed(emailId: String, accountEmail: String) {
        processedEmailDao.markProcessed(ProcessedEmailEntity(emailId, accountEmail))
    }
}
