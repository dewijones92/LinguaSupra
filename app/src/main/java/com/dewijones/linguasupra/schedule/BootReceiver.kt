package com.dewijones.linguasupra.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.notify.BannerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-posts the ongoing banner and re-arms reminder alarms after device boot.
 * Without this, `setAndAllowWhileIdle` alarms are dropped on reboot and the
 * notification record is cleared.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext
                val container = AppContainer.get(app)
                BannerNotificationManager(app, container.repository).refresh()
                ReminderScheduler(app, container.dateProvider).scheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
