package com.dewijones.linguasupra.schedule

import com.dewijones.linguasupra.data.LanguageProgress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the prompt the NanoCopywriter sends to Gemini Nano.
 * The model itself is unavailable on the JVM, so we only assert the
 * deterministic prompt-construction surface.
 */
class NanoCopywriterPromptTest {

    @Test
    fun morning_prompt_mentions_planning_tone_and_outstanding_languages() {
        val prompt = NanoCopywriter.buildPrompt(
            slot = ReminderSlot.MORNING,
            userName = "Dewi",
            outstanding = listOf(welsh(quota = 5, done = 0), mandarin(quota = 1, done = 0)),
        )
        assertTrue(prompt.contains("Dewi"))
        assertTrue(prompt.contains("warm and gentle"))
        assertTrue(prompt.contains("5 Welsh"))
        assertTrue(prompt.contains("1 Mandarin"))
    }

    @Test
    fun evening_prompt_uses_urgent_tone() {
        val prompt = NanoCopywriter.buildPrompt(
            slot = ReminderSlot.EVENING,
            userName = "Dewi",
            outstanding = listOf(welsh(quota = 5, done = 3)),
        )
        assertTrue(prompt.contains("playfully urgent"))
        assertTrue(prompt.contains("2 Welsh"))
    }

    @Test
    fun blank_name_falls_back_to_generic_address() {
        val prompt = NanoCopywriter.buildPrompt(
            slot = ReminderSlot.AFTERNOON,
            userName = "  ",
            outstanding = listOf(welsh(quota = 5, done = 1)),
        )
        assertTrue(prompt.contains("the user"))
        assertFalse(prompt.contains("  "))
    }

    @Test
    fun prompt_constrains_length_and_forbids_quotes() {
        val prompt = NanoCopywriter.buildPrompt(
            slot = ReminderSlot.MORNING,
            userName = "Dewi",
            outstanding = listOf(welsh(quota = 5, done = 0)),
        )
        assertTrue(prompt.contains("max 18 words"))
        assertTrue(prompt.contains("no quote marks"))
    }

    private fun welsh(quota: Int, done: Int) = LanguageProgress(
        languageId = 3, name = "Welsh", flagEmoji = "🏴", vibeEmoji = "🐉",
        motivationPhrase = null, dailyQuota = quota, completedToday = done, displayOrder = 2,
    )

    private fun mandarin(quota: Int, done: Int) = LanguageProgress(
        languageId = 1, name = "Mandarin", flagEmoji = "🇨🇳", vibeEmoji = "🐼",
        motivationPhrase = null, dailyQuota = quota, completedToday = done, displayOrder = 0,
    )
}
