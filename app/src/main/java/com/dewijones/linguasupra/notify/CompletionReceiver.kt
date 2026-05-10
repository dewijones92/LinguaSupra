package com.dewijones.linguasupra.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.LanguageProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val languageId = intent.getLongExtra(EXTRA_LANGUAGE_ID, INVALID_LANGUAGE_ID)
        if (languageId == INVALID_LANGUAGE_ID) return
        val languageName = intent.getStringExtra(EXTRA_LANGUAGE_NAME)
        val emojis = intent.getStringExtra(EXTRA_EMOJIS)

        val pending = goAsync()
        scope.launch {
            try {
                handle(context.applicationContext, languageId)
                if (languageName != null && emojis != null) {
                    UndoNotifications.postConfirmation(
                        context = context.applicationContext,
                        languageId = languageId,
                        languageName = languageName,
                        vibeEmoji = emojis,
                    )
                } else {
                    Log.w(TAG, "Missing extras for undo confirmation: name=$languageName emojis=$emojis")
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CompletionReceiver"
        const val ACTION_COMPLETE_LANGUAGE = "com.dewijones.linguasupra.ACTION_COMPLETE_LANGUAGE"
        const val EXTRA_LANGUAGE_ID = "language_id"
        const val EXTRA_LANGUAGE_NAME = "language_name"
        const val EXTRA_EMOJIS = "emojis"
        private const val INVALID_LANGUAGE_ID = -1L

        // Single shared scope; Receivers are short-lived but this avoids leak warnings.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Testable core: write the completion + re-post the banner. */
        suspend fun handle(context: Context, languageId: Long) {
            val container = AppContainer.get(context)
            container.repository.recordCompletion(languageId)
            BannerNotificationManager(context, container.repository).refresh()
        }

        fun pendingIntent(context: Context, lp: LanguageProgress): PendingIntent {
            val intent = Intent(ACTION_COMPLETE_LANGUAGE).apply {
                component = ComponentName(context, CompletionReceiver::class.java)
                putExtra(EXTRA_LANGUAGE_ID, lp.languageId)
                putExtra(EXTRA_LANGUAGE_NAME, lp.name)
                putExtra(EXTRA_EMOJIS, "${lp.flagEmoji}${lp.vibeEmoji}")
            }
            return PendingIntent.getBroadcast(
                context,
                lp.languageId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        // Kept for tests that pass just an id (e.g. from `am broadcast`).
        fun pendingIntent(context: Context, languageId: Long): PendingIntent {
            val intent = Intent(ACTION_COMPLETE_LANGUAGE).apply {
                component = ComponentName(context, CompletionReceiver::class.java)
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
