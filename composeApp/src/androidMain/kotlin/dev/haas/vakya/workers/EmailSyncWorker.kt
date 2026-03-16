package dev.haas.vakya.workers

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.haas.vakya.ai.AgentDecisionLayer
import dev.haas.vakya.ai.GemmaParser
import dev.haas.vakya.data.database.VakyaDatabase
import dev.haas.vakya.data.google.CalendarApi
import dev.haas.vakya.data.google.GmailApi
import dev.haas.vakya.data.repository.GoogleRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class EmailSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "EmailSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync worker...")

        val db = dev.haas.vakya.AppContextHolder.database
        val notificationManager = dev.haas.vakya.notifications.VakyaNotificationManager(applicationContext)

        val gmailApi = dev.haas.vakya.AppContextHolder.gmailApi
        val calendarApi = dev.haas.vakya.AppContextHolder.calendarApi
        
        val repository = GoogleRepository(gmailApi, db.accountDao(), db.processedEmailDao())
        val parser = dev.haas.vakya.AppContextHolder.gemmaParser
        val decisionLayer = AgentDecisionLayer(calendarApi)

        // Load confidence threshold from settings
        val confidenceThreshold = db.appSettingDao().getSetting(dev.haas.vakya.ui.viewmodel.SettingsViewModel.KEY_CONFIDENCE)
            ?.value?.toFloatOrNull() ?: 0.7f

        val accounts = db.accountDao().getAllAccountsList()
        
        // Sync all accounts that have Gmail enabled
        for (account in accounts) {
            if (!account.isGmailEnabled || account.accessToken == null) continue
            
            // Determine destination account. Default to self if not set or not found.
            val destAccount = accounts.find { it.email == account.targetAccountEmail } ?: account
            
            Log.d(TAG, "Syncing account: ${account.email} -> ${destAccount.email} (${destAccount.targetCalendarId})")
            syncBetweenAccounts(account, destAccount, repository, parser, decisionLayer, db, notificationManager, confidenceThreshold)
        }

        return Result.success()
    }

    private suspend fun syncBetweenAccounts(
        source: dev.haas.vakya.data.database.AccountEntity,
        dest: dev.haas.vakya.data.database.AccountEntity,
        repository: GoogleRepository,
        parser: GemmaParser,
        decisionLayer: AgentDecisionLayer,
        db: VakyaDatabase,
        notificationManager: dev.haas.vakya.notifications.VakyaNotificationManager,
        confidenceThreshold: Float
    ) {
        var emailsProcessed = 0
        var eventsExtracted = 0
        var eventsQueued = 0
        
        try {
            val unreadEmails = repository.getUnreadEmails(source.email, source.accessToken!!)
            Log.d(TAG, "Found ${unreadEmails.size} unread emails for ${source.email}")

            for (email in unreadEmails) {
                emailsProcessed++
                val extracted = parser.parseEmail(email.subject, email.body) // Using body instead of snippet
                if (extracted != null) {
                    eventsExtracted++
                    val calendarId = dest.targetCalendarId ?: "primary"
                    val resultConfig = decisionLayer.decideAndAct(dest.email, dest.accessToken!!, calendarId, extracted, email.sender, confidenceThreshold)
                    val actionResult = resultConfig.actionMessage
                    Log.d(TAG, "Action for ${email.id} (Source: ${source.email}, Dest: ${dest.email}): $actionResult")

                    db.aiActionLogDao().insertLog(
                        dev.haas.vakya.data.database.AiActionLogEntity(
                            emailId = email.id,
                            subject = email.subject,
                            actionSummary = if (actionResult.contains("Ignored", ignoreCase = true)) "Ignored" else actionResult
                        )
                    )

                    if (actionResult.contains("Queued event", ignoreCase = true)) {
                        eventsQueued++
                        db.pendingEventDao().insertEvent(
                            dev.haas.vakya.data.database.pendingEvents.PendingEvent(
                                emailId = email.id,
                                title = extracted.title,
                                description = "Context: ${extracted.context}\nRationale: ${extracted.rationale}\n${extracted.description}",
                                startTime = resultConfig.startTimeIso,
                                endTime = resultConfig.endTimeIso,
                                deadline = extracted.deadline,
                                confidence = extracted.confidence.toFloat(),
                                sender = email.sender,
                                originalBody = email.body,
                                accountId = dest.email,
                                targetCalendarId = calendarId
                            )
                        )
                        notificationManager.notifyNewPendingEvent(extracted.title)
                    }
                }
                repository.markEmailProcessed(email.id, source.email)
            }
            
            // Log final summary
            db.aiActionLogDao().insertLog(
                dev.haas.vakya.data.database.AiActionLogEntity(
                    logType = "SYSTEM",
                    subject = "Sync Summary: ${source.email}",
                    actionSummary = "Processed $emailsProcessed emails, extracted $eventsExtracted events, queued $eventsQueued."
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from ${source.email} to ${dest.email}", e)
            db.aiActionLogDao().insertLog(
                dev.haas.vakya.data.database.AiActionLogEntity(
                    logType = "ERROR",
                    subject = "Sync Error: ${source.email}",
                    actionSummary = e.message ?: "Unknown error"
                )
            )
        }
    }


    private fun parseIsoToLong(iso: String?): Long {
        if (iso == null) return System.currentTimeMillis()
        return try {
            java.time.ZonedDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

