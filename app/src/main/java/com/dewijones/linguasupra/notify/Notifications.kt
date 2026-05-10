package com.dewijones.linguasupra.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val BANNER_CHANNEL_ID = "lingua_banner"
    const val BANNER_CHANNEL_NAME = "Daily quota banner"
    const val BANNER_CHANNEL_DESC = "Always-on tracker for your daily Duolingo plan."
    const val BANNER_NOTIFICATION_ID = 1001

    const val REMINDER_CHANNEL_ID = "lingua_reminders"
    const val REMINDER_CHANNEL_NAME = "Reminders"
    const val REMINDER_CHANNEL_DESC = "Gentle nudges to keep your streak going."

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(BANNER_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(BANNER_CHANNEL_ID, BANNER_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = BANNER_CHANNEL_DESC
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        if (nm.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(REMINDER_CHANNEL_ID, REMINDER_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = REMINDER_CHANNEL_DESC
                    setShowBadge(true)
                },
            )
        }
    }
}
