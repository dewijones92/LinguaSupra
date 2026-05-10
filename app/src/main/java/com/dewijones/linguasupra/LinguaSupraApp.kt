package com.dewijones.linguasupra

import android.app.Application
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.notify.BannerNotificationManager
import com.dewijones.linguasupra.notify.Notifications
import com.dewijones.linguasupra.schedule.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LinguaSupraApp : Application() {

    val container: AppContainer by lazy { AppContainer.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // Idempotent: re-arms the three reminder alarms every cold start
        // (cheap, sub-ms) so a fresh install picks them up without waiting
        // for boot.
        ReminderScheduler(this, container.dateProvider).scheduleAll()
        // Post the banner once on app start so it is visible from first launch.
        appScope.launch {
            BannerNotificationManager(applicationContext, container.repository).refresh()
        }
    }
}
