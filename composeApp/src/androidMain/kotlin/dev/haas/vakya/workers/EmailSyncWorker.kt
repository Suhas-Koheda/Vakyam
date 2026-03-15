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

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val gmailApi = retrofit.create(GmailApi::class.java)
        val calendarApi = retrofit.create(CalendarApi::class.java)
        
        val repository = GoogleRepository(gmailApi, db.accountDao(), db.processedEmailDao())
        val parser = GemmaParser()
        val decisionLayer = AgentDecisionLayer(calendarApi)

        val sourceEmail = db.appSettingDao().getSetting("gmail_source")?.value
        val destEmail = db.appSettingDao().getSetting("calendar_dest")?.value
        
        // Load confidence threshold from settings
        val confidenceThreshold = db.appSettingDao().getSetting(dev.haas.vakya.ui.viewmodel.SettingsViewModel.KEY_CONFIDENCE)
            ?.value?.toFloatOrNull() ?: 0.7f

        if (sourceEmail != null && destEmail != null) {
            val sourceAccount = db.accountDao().getAccountByEmail(sourceEmail)
            val destAccount = db.accountDao().getAccountByEmail(destEmail)

            if (sourceAccount?.accessToken != null && destAccount?.accessToken != null) {
                syncBetweenAccounts(sourceAccount, destAccount, repository, parser, decisionLayer, db, notificationManager, confidenceThreshold)
            }
        } else {
            // Fallback: process all enabled accounts (original behavior)
            val accounts = db.accountDao().getAllAccountsList()
            for (account in accounts) {
                if (!account.isGmailEnabled || account.accessToken == null) continue
                syncBetweenAccounts(account, account, repository, parser, decisionLayer, db, notificationManager, confidenceThreshold)
            }
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
                    val resultConfig = decisionLayer.decideAndAct(dest.email, dest.accessToken!!, calendarId, extracted, confidenceThreshold)
                    val actionResult = resultConfig.actionMessage
                    Log.d(TAG, "Action for ${email.id} (Source: ${source.email}, Dest: ${dest.email}): $actionResult")

                    db.aiActionLogDao().insertLog(
                        dev.haas.vakya.data.database.AiActionLogEntity(
                            emailId = email.id,
                            subject = email.subject,
                            actionSummary = actionResult
                        )
                    )

                    if (actionResult.contains("Queued event", ignoreCase = true)) {
                        eventsQueued++
                        db.pendingEventDao().insertEvent(
                            dev.haas.vakya.data.database.pendingEvents.PendingEvent(
                                emailId = email.id,
                                title = extracted.title,
                                description = "Course: ${extracted.course}\n${extracted.description}",
                                startTime = resultConfig.startTimeIso,
                                endTime = resultConfig.endTimeIso,
                                deadline = extracted.deadline,
                                confidence = extracted.confidence.toFloat(),
                                accountId = dest.email
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

