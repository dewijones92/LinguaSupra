package com.dewijones.linguasupra.notify

import android.app.Notification
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
    private var lastNotification: Notification? = null
    private lateinit var bannerManager: BannerNotificationManager
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
        bannerManager = BannerNotificationManager(applicationContext, container.repository)
        val placeholder = bannerManager.placeholderNotification()
        lastNotification = placeholder
        try {
            startForeground(
                Notifications.BANNER_NOTIFICATION_ID,
                placeholder,
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
        observationJob = scope.launch {
            container.repository.observeTodayProgress().collect { progress ->
                Log.i(TAG, "observation emit, progress=${progress.size} langs")
                val notification = bannerManager.notificationFor(progress)
                lastNotification = notification
                // Update the FGS notification via startForeground so it stays
                // owned by the service rather than living as a separate post.
                runCatching {
                    startForeground(
                        Notifications.BANNER_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                }.onFailure { Log.e(TAG, "startForeground update failed", it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REPOST) {
            // User swiped the banner. Re-post the most recent notification we
            // built — observation is still running, so a fresh emit will
            // overwrite this shortly if data changed in the meantime.
            val n = lastNotification ?: bannerManager.placeholderNotification()
            runCatching {
                startForeground(
                    Notifications.BANNER_NOTIFICATION_ID,
                    n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            }.onFailure { Log.e(TAG, "repost failed", it) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(dateChangedReceiver) }
        scope.cancel()
    }

    companion object {
        private const val TAG = "BannerService"
        private const val ACTION_REPOST = "com.dewijones.linguasupra.notify.BannerService.REPOST"

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

        /**
         * Asks the running BannerService to re-post its current notification.
         * Used by [BannerDismissReceiver] when the user swipes the banner away
         * — Android 14+ allows individual swipe of FGS notifications, and we
         * want the banner to be sticky from the user's POV.
         *
         * Safe to call from a BroadcastReceiver: an already-running FGS can
         * accept new intents without the API 31+ background-start restriction.
         */
        fun requestRepost(context: Context) {
            val intent = Intent(context, BannerService::class.java).apply {
                action = ACTION_REPOST
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "repost request refused", e)
            }
        }
    }
}
