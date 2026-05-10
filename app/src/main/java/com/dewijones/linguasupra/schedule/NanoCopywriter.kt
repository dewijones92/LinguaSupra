package com.dewijones.linguasupra.schedule

import android.content.Context
import android.util.Log
import com.dewijones.linguasupra.data.LanguageProgress
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * On-device reminder copy via Gemini Nano (AICore).
 *
 * Tries to generate a single varied line of motivation; returns null if
 * the device can't run the model, the model isn't ready (downloading,
 * out of capacity), or the call exceeds [GENERATE_TIMEOUT_MS]. The
 * caller falls back to [ReminderCopy].
 *
 * The first call lazily prepares the inference engine. If preparation
 * fails (most devices — only Pixel 8 Pro+ / S24+ class flagships pass)
 * we cache the failure and skip subsequent attempts within the process,
 * so we don't pay the failure cost three times a day.
 */
class NanoCopywriter(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var unavailable: Boolean = false
    private val mutex = Mutex()
    private var model: GenerativeModel? = null

    suspend fun generate(
        slot: ReminderSlot,
        userName: String?,
        outstanding: List<LanguageProgress>,
    ): String? {
        if (unavailable || outstanding.isEmpty()) return null
        val prompt = buildPrompt(slot, userName, outstanding)
        return try {
            withTimeout(GENERATE_TIMEOUT_MS) {
                val active = mutex.withLock { ensureReady() } ?: return@withTimeout null
                val response = active.generateContent(prompt)
                response.text?.cleanLine()?.takeIf { it.length in MIN_LEN..MAX_LEN }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Generation timed out", e)
            null
        } catch (e: GenerativeAIException) {
            Log.w(TAG, "Generation failed", e)
            null
        } catch (t: Throwable) {
            Log.w(TAG, "Generation threw", t)
            null
        }
    }

    private suspend fun ensureReady(): GenerativeModel? {
        if (unavailable) return null
        model?.let { return it }
        val candidate = GenerativeModel(
            generationConfig = generationConfig {
                this.context = appContext
                temperature = 0.95f
                topK = 32
                maxOutputTokens = 80
            },
        )
        return try {
            candidate.prepareInferenceEngine()
            model = candidate
            candidate
        } catch (e: GenerativeAIException) {
            Log.i(TAG, "Gemini Nano unavailable on this device, using curated copy", e)
            unavailable = true
            null
        } catch (t: Throwable) {
            Log.i(TAG, "Gemini Nano init threw, using curated copy", t)
            unavailable = true
            null
        }
    }

    companion object {
        private const val TAG = "NanoCopywriter"
        private const val GENERATE_TIMEOUT_MS = 5_000L
        private const val MIN_LEN = 10
        private const val MAX_LEN = 220

        internal fun buildPrompt(
            slot: ReminderSlot,
            userName: String?,
            outstanding: List<LanguageProgress>,
        ): String {
            val name = userName?.trim()?.takeIf { it.isNotEmpty() } ?: "the user"
            val tone = when (slot) {
                ReminderSlot.MORNING -> "warm and gentle, planning the day"
                ReminderSlot.AFTERNOON -> "neutral and matter-of-fact, a half-day check-in"
                ReminderSlot.EVENING -> "playfully urgent, last chance to keep the streak"
            }
            val plan = outstanding.joinToString(", ") { lp ->
                val missing = lp.dailyQuota - lp.completedToday
                "$missing ${lp.name}"
            }
            return buildString {
                append("Write ONE short reminder line (max 18 words) for $name to do their daily Duolingo lessons. ")
                append("Tone: $tone. British English, light emoji OK, no greeting at the start, no quote marks. ")
                append("They still need to do: $plan. ")
                append("Be encouraging, never guilt-tripping. Just the line, nothing else.")
            }
        }

        private fun String.cleanLine(): String =
            trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
    }
}
