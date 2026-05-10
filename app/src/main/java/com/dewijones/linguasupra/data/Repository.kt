package com.dewijones.linguasupra.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Repository(
    private val languageDao: LanguageDao,
    private val completionDao: CompletionDao,
    private val dateProvider: DateProvider,
) {

    fun observeProgressForDay(dayIso: String): Flow<List<LanguageProgress>> =
        combine(
            languageDao.observeActive(),
            completionDao.observeCountsForDay(dayIso),
        ) { languages, counts ->
            val countByLang = counts.associate { it.languageId to it.count }
            languages.map { it.toProgress(countByLang[it.id] ?: 0) }
        }

    fun observeTodayProgress(): Flow<List<LanguageProgress>> =
        observeProgressForDay(dateProvider.todayIso())

    suspend fun recordCompletion(languageId: Long) {
        val now = dateProvider.now()
        completionDao.insert(
            Completion(
                languageId = languageId,
                completedAtEpochMs = now.toEpochMilli(),
                dayLocalIso = dateProvider.todayIso(),
            ),
        )
    }

    /**
     * Remove the most recent completion for a language on today's local day.
     * Returns true if a row was deleted, false if there was nothing to undo
     * (e.g. day rolled over, count is already zero).
     */
    suspend fun undoLastCompletion(languageId: Long): Boolean =
        completionDao.deleteMostRecentForDay(languageId, dateProvider.todayIso()) > 0

    /**
     * Current streak: consecutive days, ending no later than today, on which
     * every active language hit its daily quota. Today only counts if it's
     * already complete; if it isn't, we look at yesterday — that way the
     * streak doesn't appear to "reset" mid-morning before you've done your
     * day's lessons.
     */
    fun observeStreak(): Flow<Int> = combine(
        completionDao.observeAll(),
        languageDao.observeActive(),
    ) { all, languages ->
        if (languages.isEmpty()) return@combine 0
        val byDay: Map<String, Map<Long, Int>> = all
            .groupBy { it.dayLocalIso }
            .mapValues { (_, rows) -> rows.groupingBy { it.languageId }.eachCount() }
        val today = LocalDate.now(ZoneId.systemDefault())
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        fun isDone(day: LocalDate): Boolean {
            val counts = byDay[day.format(isoFormatter)].orEmpty()
            return languages.all { (counts[it.id] ?: 0) >= it.dailyQuota }
        }
        var streak = 0
        var day = today
        if (!isDone(day)) day = day.minusDays(1)
        while (isDone(day)) {
            streak += 1
            day = day.minusDays(1)
            if (streak > 3650) break // safety
        }
        streak
    }

    /** All completions newest-first, joined with language for display. */
    fun observeAllCompletionsWithLanguage(): Flow<List<CompletionWithLanguage>> =
        combine(completionDao.observeAll(), languageDao.observeAllIncludingInactive()) { rows, languages ->
            val byId = languages.associateBy { it.id }
            rows.mapNotNull { c ->
                byId[c.languageId]?.let { lang -> CompletionWithLanguage(c, lang) }
            }
        }

    /**
     * Per-day per-language completion counts for the trailing [days] days
     * (inclusive of today). Days with zero rows for a language are filled
     * in as zero so the chart x-axis is dense.
     */
    fun observeDailySeries(days: Int): Flow<List<DailySeriesPoint>> {
        val startDay = LocalDate.now(ZoneId.systemDefault()).minusDays(days - 1L)
        return combine(
            completionDao.observeCountsSince(startDay.format(DateTimeFormatter.ISO_LOCAL_DATE)),
            languageDao.observeAllIncludingInactive(),
        ) { counts, languages ->
            val countByDayLang = counts.associateBy { it.dayIso to it.languageId }
            val dayList = (0 until days).map { startDay.plusDays(it.toLong()) }
            dayList.flatMap { day ->
                val dayIso = day.format(DateTimeFormatter.ISO_LOCAL_DATE)
                languages.map { lang ->
                    DailySeriesPoint(
                        day = day,
                        languageId = lang.id,
                        languageName = lang.name,
                        count = countByDayLang[dayIso to lang.id]?.count ?: 0,
                    )
                }
            }
        }
    }

    suspend fun deleteCompletion(id: Long) = completionDao.delete(id)

    /** Update a completion's timestamp (also recomputes day_local_iso). */
    suspend fun updateCompletionTime(id: Long, newInstant: Instant) {
        val row = completionDao.byId(id) ?: return
        completionDao.update(
            row.copy(
                completedAtEpochMs = newInstant.toEpochMilli(),
                dayLocalIso = newInstant.atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
            ),
        )
    }

    /** Add a manual completion at an arbitrary instant. */
    suspend fun addManualCompletion(languageId: Long, instant: Instant): Long {
        return completionDao.insert(
            Completion(
                languageId = languageId,
                completedAtEpochMs = instant.toEpochMilli(),
                dayLocalIso = instant.atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
            ),
        )
    }

    suspend fun addLanguage(
        name: String,
        dailyQuota: Int,
        flagEmoji: String,
        vibeEmoji: String,
        motivationPhrase: String? = null,
    ): Long {
        val nextOrder = languageDao.maxDisplayOrder() + 1
        return languageDao.insert(
            Language(
                name = name,
                dailyQuota = dailyQuota,
                displayOrder = nextOrder,
                flagEmoji = flagEmoji,
                vibeEmoji = vibeEmoji,
                motivationPhrase = motivationPhrase,
            ),
        )
    }

    suspend fun updateLanguage(language: Language) = languageDao.update(language)

    suspend fun deleteLanguage(id: Long) = languageDao.delete(id)

    suspend fun reorderLanguages(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            val existing = languageDao.byId(id) ?: return@forEachIndexed
            if (existing.displayOrder != index) {
                languageDao.update(existing.copy(displayOrder = index))
            }
        }
    }
}

private fun Language.toProgress(completedToday: Int): LanguageProgress = LanguageProgress(
    languageId = id,
    name = name,
    flagEmoji = flagEmoji,
    vibeEmoji = vibeEmoji,
    motivationPhrase = motivationPhrase,
    dailyQuota = dailyQuota,
    completedToday = completedToday,
    displayOrder = displayOrder,
)
