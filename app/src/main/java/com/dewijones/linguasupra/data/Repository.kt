package com.dewijones.linguasupra.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
     * Current streak status: consecutive days (with up to one rolling-7-day
     * grace skip) on which every active language hit its daily quota.
     *
     * Grace day rules:
     * - Today never counts as a grace; if today's incomplete we walk back
     *   to yesterday before counting.
     * - Grace can't be the first item in a streak (need at least one
     *   genuinely-completed day before it kicks in).
     * - The day before a graced gap must also be done — otherwise grace is
     *   papering over the user genuinely stopping.
     * - At most one grace per rolling 7-day window of the streak walk.
     *
     * `daysUntilGraceRecharge` is what the UI surfaces. 0 means a grace is
     * available right now; otherwise it's the number of days until the
     * 7-day cool-down on the most recently used grace expires.
     */
    fun observeStreakStatus(): Flow<StreakStatus> = combine(
        completionDao.observeAll(),
        languageDao.observeActive(),
    ) { all, languages ->
        if (languages.isEmpty()) return@combine StreakStatus(0, false, 0)
        val byDay: Map<String, Map<Long, Int>> = all
            .groupBy { it.dayLocalIso }
            .mapValues { (_, rows) -> rows.groupingBy { it.languageId }.eachCount() }
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        fun isDone(day: LocalDate): Boolean {
            val counts = byDay[day.format(isoFormatter)].orEmpty()
            return languages.all { (counts[it.id] ?: 0) >= it.dailyQuota }
        }

        val today = dateProvider.today()
        var day = today
        if (!isDone(day)) day = day.minusDays(1)

        var streak = 0
        var lastGraceDay: LocalDate? = null
        while (streak <= 3650) {
            if (isDone(day)) {
                streak += 1
                day = day.minusDays(1)
                continue
            }
            if (streak < 1) break
            val canGrace = lastGraceDay == null ||
                ChronoUnit.DAYS.between(day, lastGraceDay) >= 7
            if (!canGrace) break
            if (!isDone(day.minusDays(1))) break
            lastGraceDay = day
            streak += 1
            day = day.minusDays(1)
        }
        val recharge = lastGraceDay?.let {
            val sinceUsed = ChronoUnit.DAYS.between(it, today).toInt()
            (7 - sinceUsed).coerceAtLeast(0)
        } ?: 0
        StreakStatus(
            streak = streak,
            graceUsed = lastGraceDay != null,
            daysUntilGraceRecharge = recharge,
        )
    }

    /** Convenience: just the streak count, derived from [observeStreakStatus]. */
    fun observeStreak(): Flow<Int> = observeStreakStatus().map { it.streak }

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

    /** Bulk-delete completions by id. Returns the number of rows actually removed. */
    suspend fun deleteCompletions(ids: Collection<Long>): Int =
        if (ids.isEmpty()) 0 else completionDao.deleteByIds(ids.toList())

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
