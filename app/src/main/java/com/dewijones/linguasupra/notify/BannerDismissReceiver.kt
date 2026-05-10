package com.dewijones.linguasupra.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Fires when the user swipes the banner away. On Android 14+ the platform
 * allows individual swipe-dismissal of FGS notifications even with
 * `setOngoing(true)` — only "Clear all" is blocked. This receiver re-posts
 * the banner so the user always sees their daily progress in the shade.
 *
 * Re-post is via [BannerService.requestRepost] which delivers an intent to
 * the already-running FGS; no service restart, no permission gauntlet.
 */
class BannerDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BANNER_DISMISSED) return
        Log.i(TAG, "banner dismissed by user — requesting repost")
        BannerService.requestRepost(context.applicationContext)
    }

    companion object {
        private const val TAG = "BannerDismissReceiver"
        const val ACTION_BANNER_DISMISSED = "com.dewijones.linguasupra.ACTION_BANNER_DISMISSED"

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(ACTION_BANNER_DISMISSED).apply {
                component = ComponentName(context, BannerDismissReceiver::class.java)
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
