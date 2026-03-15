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

        val db = Room.databaseBuilder(
            applicationContext,
            VakyaDatabase::class.java, "vakya-db"
        ).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val gmailApi = retrofit.create(GmailApi::class.java)
        val calendarApi = retrofit.create(CalendarApi::class.java)
        
        val repository = GoogleRepository(gmailApi, db.accountDao(), db.processedEmailDao())
        val parser = GemmaParser()
        val decisionLayer = AgentDecisionLayer(calendarApi)

        val accounts = db.accountDao().getAllAccountsList()
        
        for (account in accounts) {
            if (!account.isGmailEnabled || account.accessToken == null) continue

            try {
                val unreadEmails = repository.getUnreadEmails(account.email, account.accessToken)
                Log.d(TAG, "Found ${unreadEmails.size} unread emails for ${account.email}")

                for (email in unreadEmails) {
                    val extracted = parser.parseEmail(email.subject, email.snippet)
                    var actionResult = "No extraction"
                    if (extracted != null) {
                        actionResult = decisionLayer.decideAndAct(account.email, account.accessToken, extracted)
                        Log.d(TAG, "Action for ${email.id}: $actionResult")
                        
                        // Log the action
                        db.aiActionLogDao().insertLog(
                            dev.haas.vakya.data.database.AiActionLogEntity(
                                emailId = email.id,
                                subject = email.subject,
                                actionSummary = actionResult
                            )
                        )

                        // If successful addition to calendar, record it
                        if (actionResult.contains("Created event", ignoreCase = true)) {
                            db.calendarEventDao().insertEvent(
                                dev.haas.vakya.data.database.CalendarEventEntity(
                                    title = extracted.title,
                                    startTime = parseIsoToLong(extracted.start_time ?: extracted.deadline),
                                    endTime = extracted.end_time?.let { parseIsoToLong(it) },
                                    source = "Gmail",
                                    accountEmail = account.email,
                                    status = "ADDED"
                                )
                            )
                        } else if (actionResult.contains("Ignored", ignoreCase = true)) {
                             db.calendarEventDao().insertEvent(
                                dev.haas.vakya.data.database.CalendarEventEntity(
                                    title = extracted.title,
                                    startTime = parseIsoToLong(extracted.start_time ?: extracted.deadline),
                                    source = "Gmail",
                                    accountEmail = account.email,
                                    status = "IGNORED"
                                )
                            )
                        }

                    }
                    repository.markEmailProcessed(email.id, account.email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing account ${account.email}", e)
            }
        }

        return Result.success()
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

