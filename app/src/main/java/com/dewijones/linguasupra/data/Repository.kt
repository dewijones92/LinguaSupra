package com.dewijones.linguasupra.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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
