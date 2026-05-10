package com.dewijones.linguasupra.data

import java.time.LocalDate

/** A completion row joined with its language for display. */
data class CompletionWithLanguage(
    val completion: Completion,
    val language: Language,
)

/**
 * One bar in the daily-counts chart: how many completions on a given local
 * day for a given language. Languages with zero count for a day still get a
 * row (with count = 0) so the chart x-axis is dense.
 */
data class DailySeriesPoint(
    val day: LocalDate,
    val languageId: Long,
    val languageName: String,
    val count: Int,
)
