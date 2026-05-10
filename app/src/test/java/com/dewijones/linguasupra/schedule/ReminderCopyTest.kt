package com.dewijones.linguasupra.schedule

import com.dewijones.linguasupra.data.LanguageProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCopyTest {

    private fun progress(
        name: String,
        flag: String,
        vibe: String,
        quota: Int,
        completed: Int,
    ) = LanguageProgress(
        languageId = name.hashCode().toLong(),
        name = name,
        flagEmoji = flag,
        vibeEmoji = vibe,
        motivationPhrase = null,
        dailyQuota = quota,
        completedToday = completed,
        displayOrder = 0,
    )

    @Test
    fun `returns null when nothing is outstanding`() {
        val message = ReminderCopy.build(
            slot = ReminderSlot.EVENING,
            userName = "Dewi",
            outstanding = emptyList(),
            dayOfYear = 100,
        )
        assertNull(message)
    }

    @Test
    fun `morning copy mentions outstanding language counts`() {
        val outstanding = listOf(
            progress("Welsh", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "🐉", quota = 5, completed = 2),
            progress("Mandarin", "🇨🇳", "🐼", quota = 1, completed = 0),
        )
        val message = ReminderCopy.build(
            slot = ReminderSlot.MORNING,
            userName = "Dewi",
            outstanding = outstanding,
            dayOfYear = 1,
        )
        assertNotNull(message)
        assertTrue("body should mention 3 Welsh outstanding", message!!.body.contains("3 Welsh"))
        assertTrue("body should mention 1 Mandarin", message.body.contains("1 Mandarin"))
    }

    @Test
    fun `name substitution falls back to 'you' when name is null`() {
        val outstanding = listOf(progress("Welsh", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "🐉", 5, 0))
        val withName = ReminderCopy.build(
            ReminderSlot.MORNING, "Dewi", outstanding, dayOfYear = 0,
        )!!
        val noName = ReminderCopy.build(
            ReminderSlot.MORNING, null, outstanding, dayOfYear = 0,
        )!!
        assertTrue(withName.title.contains("Dewi"))
        assertTrue("no-name title shouldn't leave a placeholder", !noName.title.contains("{name}"))
    }

    @Test
    fun `same day-of-year produces the same copy (deterministic)`() {
        val outstanding = listOf(progress("Welsh", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "🐉", 5, 0))
        val a = ReminderCopy.build(ReminderSlot.EVENING, "Dewi", outstanding, dayOfYear = 42)
        val b = ReminderCopy.build(ReminderSlot.EVENING, "Dewi", outstanding, dayOfYear = 42)
        assertEquals(a, b)
    }

    @Test
    fun `evening tone differs from morning tone for same inputs`() {
        val outstanding = listOf(progress("Welsh", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "🐉", 5, 0))
        val morning = ReminderCopy.build(ReminderSlot.MORNING, "Dewi", outstanding, dayOfYear = 0)!!
        val evening = ReminderCopy.build(ReminderSlot.EVENING, "Dewi", outstanding, dayOfYear = 0)!!
        // Different titles by construction (different phrasebook entries).
        assertTrue(morning.title != evening.title)
    }
}
