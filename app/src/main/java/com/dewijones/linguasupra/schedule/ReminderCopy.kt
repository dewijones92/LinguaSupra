package com.dewijones.linguasupra.schedule

import com.dewijones.linguasupra.data.LanguageProgress

/**
 * Pure function that turns (slot, name, outstanding) into a reminder title +
 * body. Hand-crafted phrasebook keyed by slot tone:
 *
 *  - Morning   — gentle, planning ("Today's plan")
 *  - Afternoon — neutral, neutral nudge ("Still on the list")
 *  - Evening   — urgent, encouraging final-push ("Last lap!")
 *
 * Variation is deterministic per (slot, dayOfYear) so the user gets a
 * different line each day but the same line if they look at the
 * notification twice. No randomness, no LLM — hand-curated charm.
 */
object ReminderCopy {

    data class Message(val title: String, val body: String)

    fun build(
        slot: ReminderSlot,
        userName: String?,
        outstanding: List<LanguageProgress>,
        dayOfYear: Int,
    ): Message? {
        if (outstanding.isEmpty()) return null
        val variants = VARIANTS.getValue(slot)
        val title = variants.titles[dayOfYear.mod(variants.titles.size)]
            .replaceName(userName)
        val body = formatBody(slot, outstanding, userName, dayOfYear, variants)
        return Message(title = title, body = body)
    }

    private fun formatBody(
        slot: ReminderSlot,
        outstanding: List<LanguageProgress>,
        userName: String?,
        dayOfYear: Int,
        variants: Variants,
    ): String {
        val list = outstanding.joinToString(separator = "  ·  ") { lp ->
            val missing = lp.dailyQuota - lp.completedToday
            "${lp.flagEmoji}${lp.vibeEmoji} $missing ${lp.name}"
        }
        val tail = variants.tails[dayOfYear.mod(variants.tails.size)].replaceName(userName)
        return when (slot) {
            ReminderSlot.MORNING -> "$list\n$tail"
            ReminderSlot.AFTERNOON -> "$list\n$tail"
            ReminderSlot.EVENING -> "$list\n$tail"
        }
    }

    private fun String.replaceName(name: String?): String =
        if (name.isNullOrBlank()) replace("{name}", "you").replace(", {name}", "")
        else replace("{name}", name)

    private data class Variants(val titles: List<String>, val tails: List<String>)

    private val VARIANTS: Map<ReminderSlot, Variants> = mapOf(
        ReminderSlot.MORNING to Variants(
            titles = listOf(
                "🌅 Bore da, {name}! Today's plan",
                "☕ Morning {name} — here's the line-up",
                "🌞 Fresh start, {name}",
                "🐣 Hatching today's plan",
                "🥐 Plan du jour, {name}",
            ),
            tails = listOf(
                "Quick wins now beat panic later 💪",
                "Five minutes a language. You've got this.",
                "Pop the kettle on and chip away ☕",
                "Future-{name} will thank present-{name}.",
                "Tiny daily reps · giant compound interest 📈",
            ),
        ),
        ReminderSlot.AFTERNOON to Variants(
            titles = listOf(
                "⏳ Half-day check-in, {name}",
                "🔔 Still on the list",
                "🥪 Post-lunch nudge, {name}",
                "👀 Quick afternoon look",
                "📋 Outstanding for today",
            ),
            tails = listOf(
                "No drama — just one open and you're rolling.",
                "Even one lesson keeps the streak alive 🔥",
                "Pop into Duolingo for five minutes — done.",
                "Bite-sized progress beats end-of-day cramming.",
                "Take a breath, then take a lesson 🧘",
            ),
        ),
        ReminderSlot.EVENING to Variants(
            titles = listOf(
                "🌙 Last lap, {name}!",
                "🦉 Hooty's watching, {name} 👀",
                "🔥 Streak-saver time",
                "🌃 Final push, {name}",
                "🎯 Don't break the chain",
            ),
            tails = listOf(
                "Five minutes now and you're golden ✨",
                "Future-you, brushing teeth, smiling at this.",
                "One last sprint — settle the score with the owl 🦉",
                "Dim ond pum munud — that's all it takes 🏴󠁧󠁢󠁷󠁬󠁳󠁿",
                "Sleep better with quotas hit 😴",
            ),
        ),
    )
}
