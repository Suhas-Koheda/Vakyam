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

        val sourceEmail = db.appSettingDao().getSetting("gmail_source")?.value
        val destEmail = db.appSettingDao().getSetting("calendar_dest")?.value

        if (sourceEmail != null && destEmail != null) {
            val sourceAccount = db.accountDao().getAccountByEmail(sourceEmail)
            val destAccount = db.accountDao().getAccountByEmail(destEmail)

            if (sourceAccount?.accessToken != null && destAccount?.accessToken != null) {
                syncBetweenAccounts(sourceAccount, destAccount, repository, parser, decisionLayer, db)
            }
        } else {
            // Fallback: process all enabled accounts (original behavior)
            val accounts = db.accountDao().getAllAccountsList()
            for (account in accounts) {
                if (!account.isGmailEnabled || account.accessToken == null) continue
                syncBetweenAccounts(account, account, repository, parser, decisionLayer, db)
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
        db: VakyaDatabase
    ) {
        try {
            val unreadEmails = repository.getUnreadEmails(source.email, source.accessToken!!)
            Log.d(TAG, "Found ${unreadEmails.size} unread emails for ${source.email}")

            for (email in unreadEmails) {
                val extracted = parser.parseEmail(email.subject, email.snippet)
                var actionResult = "No extraction"
                if (extracted != null) {
                    actionResult = decisionLayer.decideAndAct(dest.email, dest.accessToken!!, extracted)
                    Log.d(TAG, "Action for ${email.id} (Source: ${source.email}, Dest: ${dest.email}): $actionResult")

                    db.aiActionLogDao().insertLog(
                        dev.haas.vakya.data.database.AiActionLogEntity(
                            emailId = email.id,
                            subject = email.subject,
                            actionSummary = actionResult
                        )
                    )

                    if (actionResult.contains("Created event", ignoreCase = true)) {
                        db.calendarEventDao().insertEvent(
                            dev.haas.vakya.data.database.CalendarEventEntity(
                                title = extracted.title,
                                startTime = parseIsoToLong(extracted.start_time ?: extracted.deadline),
                                endTime = extracted.end_time?.let { parseIsoToLong(it) },
                                source = "Gmail (${source.email})",
                                accountEmail = dest.email,
                                status = "ADDED"
                            )
                        )
                    }
                }
                repository.markEmailProcessed(email.id, source.email)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from ${source.email} to ${dest.email}", e)
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

