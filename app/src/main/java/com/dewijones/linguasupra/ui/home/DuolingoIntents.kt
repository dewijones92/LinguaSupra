package com.dewijones.linguasupra.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Opens Duolingo from a row tap. Per-course deep-linking on Android is
 * unreliable, so we go to the user's current course (which is what
 * "Continue" lands on anyway) via the canonical web URL. If Duolingo is
 * installed it intercepts via App Links and opens in-app; otherwise the
 * browser opens the web app. Last resort is the Play Store.
 *
 * No \<queries\> manifest entry is needed because we never inspect the
 * package — the system resolver handles routing.
 */
internal object DuolingoIntents {

    private const val TAG = "DuolingoIntents"
    private const val LEARN_URL = "https://www.duolingo.com/learn"
    private const val PLAY_MARKET_URL = "market://details?id=com.duolingo"
    private const val PLAY_WEB_URL = "https://play.google.com/store/apps/details?id=com.duolingo"

    fun openCourse(context: Context) {
        val attempts = listOf(LEARN_URL, PLAY_MARKET_URL, PLAY_WEB_URL)
        for (url in attempts) {
            if (tryStart(context, url)) return
        }
        Log.w(TAG, "No activity could handle any Duolingo URL")
    }

    private fun tryStart(context: Context, url: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: ActivityNotFoundException) {
        Log.d(TAG, "No handler for $url", e)
        false
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to start activity for $url", t)
        false
    }
}
