package com.dewijones.linguasupra.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dewijones.linguasupra.R

/**
 * Posts a brief "Undo last tap?" notification after a successful +1, then
 * auto-dismisses after [TIMEOUT_MS]. Re-posts to the same notification ID
 * on each tap so rapid +1s don't pile up — only the most recent is undoable,
 * which matches the user's expectation ("undo the thing I just did").
 */
internal object UndoNotifications {

    private const val TIMEOUT_MS = 6_000L

    fun postConfirmation(context: Context, languageId: Long, languageName: String, vibeEmoji: String) {
        Notifications.ensureChannels(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val undoIntent = UndoCompletionReceiver.pendingIntent(context, languageId)
        val notification = NotificationCompat.Builder(context, Notifications.UNDO_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$vibeEmoji $languageName +1 ✓")
            .setContentText(context.getString(R.string.undo_prompt))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(TIMEOUT_MS)
            .addAction(0, context.getString(R.string.undo_action), undoIntent)
            .build()
        nm.notify(Notifications.UNDO_NOTIFICATION_ID, notification)
    }
}
