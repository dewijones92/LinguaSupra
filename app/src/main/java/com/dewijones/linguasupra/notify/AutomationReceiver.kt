package com.dewijones.linguasupra.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dewijones.linguasupra.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Exported automation surface for Tasker, Pixel Routines, and the Shortcuts
 * app. External apps can broadcast:
 *
 *     adb shell am broadcast -a com.dewijones.linguasupra.AUTOMATION_COMPLETE \
 *         -n com.dewijones.linguasupra/.notify.AutomationReceiver \
 *         --es language_name "Welsh"
 *
 * `language_name` matches case-insensitively against active languages.
 * No-op if the name is missing, blank, or doesn't match anything active.
 *
 * This wraps [CompletionReceiver.handle] so the side effects are exactly
 * the same as tapping the in-app banner: DB write, banner refresh, undo
 * confirmation. We keep this as a separate exported receiver rather than
 * exporting [CompletionReceiver] itself so the stable internal contract
 * (language_id long) doesn't have to change shape for automation users.
 */
class AutomationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val name = intent.getStringExtra(EXTRA_LANGUAGE_NAME)?.trim()
        if (name.isNullOrBlank()) {
            Log.w(TAG, "AUTOMATION_COMPLETE without language_name extra")
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext
                val language = AppContainer.get(app).database.languageDao()
                    .byNameActive(name)
                if (language == null) {
                    Log.w(TAG, "No active language matches '$name'")
                    return@launch
                }
                CompletionReceiver.handle(app, language.id)
                Log.i(TAG, "Recorded completion for '${language.name}' (id=${language.id}) via automation")
            } catch (t: Throwable) {
                Log.e(TAG, "Automation completion failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.dewijones.linguasupra.AUTOMATION_COMPLETE"
        const val EXTRA_LANGUAGE_NAME = "language_name"
        private const val TAG = "AutomationReceiver"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
