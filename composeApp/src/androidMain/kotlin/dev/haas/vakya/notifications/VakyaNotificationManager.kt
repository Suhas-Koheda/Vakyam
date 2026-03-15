package dev.haas.vakya.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.haas.vakya.MainActivity

class VakyaNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "vakya_notifications"
        const val CHANNEL_NAME = "Vakya Notifications"
        const val CHANNEL_DESC = "Notifications for AI detected events and reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun notifyNewPendingEvent(eventTitle: String) {
        showNotification(
            "New Event Detected",
            "Vakya detected a potential event: $eventTitle. Please review it."
        )
    }

    fun notifyEventApproved(eventTitle: String) {
        showNotification(
            "Event Approved",
            "Event '$eventTitle' has been added to your calendar."
        )
    }

    fun notifyDeadlineApproaching(eventTitle: String, timeLeft: String) {
        showNotification(
            "Deadline Approaching",
            "Vakya detected a deadline: $eventTitle due in $timeLeft."
        )
    }
}
