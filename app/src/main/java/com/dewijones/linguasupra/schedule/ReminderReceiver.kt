package com.dewijones.linguasupra.schedule

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dewijones.linguasupra.MainActivity
import com.dewijones.linguasupra.R
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slot = ReminderSlot.fromKey(intent.getStringExtra(EXTRA_SLOT)) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                handle(context.applicationContext, slot)
            } catch (t: Throwable) {
                Log.e(TAG, "handle failed for slot=${slot.key}", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.dewijones.linguasupra.ACTION_REMINDER_FIRE"
        const val ACTION_DEBUG_FIRE = "com.dewijones.linguasupra.DEBUG_FIRE_REMINDER"
        const val EXTRA_SLOT = "slot"
        private const val TAG = "ReminderReceiver"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Testable core: fetch progress, decide whether to nudge, post the
         * notification, and re-schedule the same slot for tomorrow.
         */
        suspend fun handle(context: Context, slot: ReminderSlot) {
            val container = AppContainer.get(context)
            val progress = container.repository.observeTodayProgress().first()
            val outstanding = progress.filter { !it.isComplete }
            val name = container.userPreferences.userName.first()
            val dayOfYear = container.dateProvider.today().dayOfYear

            val curated = ReminderCopy.build(
                slot = slot,
                userName = name,
                outstanding = outstanding,
                dayOfYear = dayOfYear,
            )
            val nanoEnabled = container.userPreferences.nanoEnabled.first()
            // Try Gemini Nano for the body when the user has it on; fall back
            // to the curated tail. Title stays curated either way — the
            // LLM-generated line lives inside the BigText body where it has
            // room to breathe.
            val message = curated?.let { c ->
                val nanoLine = if (nanoEnabled) {
                    nanoCopywriter(context).generate(slot, name, outstanding)
                } else null
                if (nanoLine != null) {
                    val list = c.body.lineSequence().firstOrNull().orEmpty()
                    c.copy(body = if (list.isNotBlank()) "$list\n$nanoLine" else nanoLine)
                } else {
                    c
                }
            }
            if (message != null) postNotification(context, slot, message)

            // Always re-schedule for the next firing so reminders persist
            // even if today's was skipped (everything complete).
            ReminderScheduler(context, container.dateProvider).schedule(slot)
        }

        @Volatile
        private var nanoInstance: NanoCopywriter? = null
        private fun nanoCopywriter(context: Context): NanoCopywriter =
            nanoInstance ?: synchronized(this) {
                nanoInstance ?: NanoCopywriter(context.applicationContext).also {
                    nanoInstance = it
                }
            }

        private fun postNotification(
            context: Context,
            slot: ReminderSlot,
            message: ReminderCopy.Message,
        ) {
            Notifications.ensureChannels(context)
            // Inline permission check — lint doesn't track through helpers.
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) return
            val priority = when (slot) {
                ReminderSlot.MORNING -> NotificationCompat.PRIORITY_LOW
                ReminderSlot.AFTERNOON -> NotificationCompat.PRIORITY_DEFAULT
                ReminderSlot.EVENING -> NotificationCompat.PRIORITY_HIGH
            }
            val notification = NotificationCompat.Builder(context, Notifications.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(message.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                .setContentText(message.body.lineSequence().firstOrNull() ?: message.body)
                .setPriority(priority)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(launchAppIntent(context))
                .build()
            nm.notify(slot.notificationId, notification)
        }

        private fun launchAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
