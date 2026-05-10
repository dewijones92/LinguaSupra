package com.dewijones.linguasupra

import android.app.Application
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.notify.Notifications
import com.dewijones.linguasupra.schedule.ReminderScheduler

class LinguaSupraApp : Application() {

    val container: AppContainer by lazy { AppContainer.get(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // Idempotent: re-arms the three reminder alarms every cold start
        // (cheap, sub-ms) so a fresh install picks them up without waiting
        // for boot.
        ReminderScheduler(this, container.dateProvider).scheduleAll()
        // The banner is now owned by BannerService (a foreground service),
        // started from MainActivity once the user has granted
        // POST_NOTIFICATIONS, and from BootReceiver after a reboot.
        // We deliberately do NOT start it here — Application.onCreate runs
        // even when the process is woken by background broadcasts, where
        // startForegroundService would throw on API 31+.
    }
}
