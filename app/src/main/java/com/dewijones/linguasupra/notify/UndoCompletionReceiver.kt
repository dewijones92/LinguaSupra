package com.dewijones.linguasupra.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dewijones.linguasupra.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reverses the most recent +1 tap for a given language on today's local day.
 * Wired up by [UndoNotifications] which posts a transient notification with
 * an Undo action immediately after every successful tap.
 */
class UndoCompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNDO_LAST) return
        val languageId = intent.getLongExtra(EXTRA_LANGUAGE_ID, INVALID_LANGUAGE_ID)
        if (languageId == INVALID_LANGUAGE_ID) return

        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext
                val container = AppContainer.get(app)
                val undone = container.repository.undoLastCompletion(languageId)
                if (undone) {
                    BannerNotificationManager(app, container.repository).refresh()
                }
                NotificationManagerCompat.from(app).cancel(Notifications.UNDO_NOTIFICATION_ID)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_UNDO_LAST = "com.dewijones.linguasupra.ACTION_UNDO_LAST"
        const val EXTRA_LANGUAGE_ID = "language_id"
        private const val INVALID_LANGUAGE_ID = -1L

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun pendingIntent(context: Context, languageId: Long): PendingIntent {
            val intent = Intent(ACTION_UNDO_LAST).apply {
                component = ComponentName(context, UndoCompletionReceiver::class.java)
                putExtra(EXTRA_LANGUAGE_ID, languageId)
            }
            return PendingIntent.getBroadcast(
                context,
                languageId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
