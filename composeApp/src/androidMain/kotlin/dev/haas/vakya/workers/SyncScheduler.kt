package dev.haas.vakya.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val WORK_NAME = "EmailSyncWork"

    fun scheduleSync(workManager: WorkManager, intervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val clampedInterval = if (intervalMinutes < 15) 15L else intervalMinutes

        val syncRequest = PeriodicWorkRequestBuilder<EmailSyncWorker>(clampedInterval, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun scheduleImmediateSync(workManager: WorkManager) {
        val syncRequest = OneTimeWorkRequestBuilder<EmailSyncWorker>()
            .build()
        workManager.enqueueUniqueWork(
            "ImmediateSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
