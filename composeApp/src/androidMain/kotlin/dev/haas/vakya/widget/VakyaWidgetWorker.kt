package dev.haas.vakya.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.concurrent.TimeUnit

class VakyaWidgetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        VakyaWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<VakyaWidgetWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "VakyaWidgetUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
