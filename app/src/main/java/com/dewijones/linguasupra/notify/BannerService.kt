package com.dewijones.linguasupra.notify

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.dewijones.linguasupra.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the ongoing banner notification. Marked as a
 * `specialUse` FGS because no other type matches an "always-on accountability
 * tracker for the user's own daily plan" — see the
 * `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declared in the manifest.
 *
 * Why a service rather than the previous post-from-Application approach?
 * Notifications posted by `notify(...)` alone can be wiped by the system's
 * "Clear all" button. A foreground notification cannot, so the banner stays
 * visible the way the user expects.
 *
 * Cost: keeps the process resident in RAM (~tens of MB). Negligible CPU
 * since the only thing happening here is observing a Room flow that emits
 * when the user taps `+1`. No wakelocks, no networking, no GPS.
 */
class BannerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observationJob: Job? = null
    private val dateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_DATE_CHANGED ||
                intent.action == Intent.ACTION_TIMEZONE_CHANGED
            ) {
                restartObservation()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        val container = AppContainer.get(applicationContext)
        val mgr = BannerNotificationManager(applicationContext, container.repository)
        try {
            startForeground(
                Notifications.BANNER_NOTIFICATION_ID,
                mgr.placeholderNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return
        }
        ContextCompat.registerReceiver(
            this,
            dateChangedReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        restartObservation()
    }

    private fun restartObservation() {
        observationJob?.cancel()
        val container = AppContainer.get(applicationContext)
        val mgr = BannerNotificationManager(applicationContext, container.repository)
        observationJob = scope.launch {
            container.repository.observeTodayProgress().collect { progress ->
                Log.i(TAG, "observation emit, progress=${progress.size} langs")
                // Update the FGS notification via startForeground so it stays
                // owned by the service rather than living as a separate post.
                runCatching {
                    startForeground(
                        Notifications.BANNER_NOTIFICATION_ID,
                        mgr.notificationFor(progress),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                }.onFailure { Log.e(TAG, "startForeground update failed", it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(dateChangedReceiver) }
        scope.cancel()
    }

    companion object {
        private const val TAG = "BannerService"

        /**
         * Idempotent — calling repeatedly is safe; Android folds duplicate
         * starts into the running instance.
         *
         * Call sites must be foreground-exempt (Activity, BOOT_COMPLETED
         * receiver, or post-permission-grant) on API 31+ or this will throw
         * `ForegroundServiceStartNotAllowedException`. The exception is
         * caught and logged in [onCreate] so a bad call site degrades to
         * "no banner" rather than a crash.
         */
        fun start(context: Context) {
            val intent = Intent(context, BannerService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService refused", e)
            }
        }
    }
}
