package com.dewijones.linguasupra.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: Repository
    private lateinit var clock: MutableTestClock
    private lateinit var dateProvider: DateProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clock = MutableTestClock(Instant.parse("2026-05-10T10:00:00Z"))
        dateProvider = DateProvider { clock }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedCallback)
            .allowMainThreadQueries()
            .build()
        repository = Repository(db.languageDao(), db.completionDao(), dateProvider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seed_creates_three_default_languages_in_display_order() = runTest {
        val progress = repository.observeTodayProgress().first()
        assertEquals(3, progress.size)
        val byOrder = progress.sortedBy { it.displayOrder }
        assertEquals(listOf("Mandarin", "Latin", "Welsh"), byOrder.map { it.name })
        assertEquals(5, byOrder.first { it.name == "Welsh" }.dailyQuota)
        assertEquals("🐉", byOrder.first { it.name == "Welsh" }.vibeEmoji)
    }

    @Test
    fun recordCompletion_increments_today_count() = runTest {
        val welsh = languageByName("Welsh")
        repository.recordCompletion(welsh.id)
        val progress = repository.observeTodayProgress().first()
        assertEquals(1, progress.first { it.name == "Welsh" }.completedToday)
    }

    @Test
    fun yesterdays_completions_dont_count_today() = runTest {
        val mandarin = languageByName("Mandarin")
        // Insert directly with yesterday's iso date
        db.completionDao().insert(
            Completion(
                languageId = mandarin.id,
                completedAtEpochMs = 0L,
                dayLocalIso = "2026-05-09",
            ),
        )
        val today = repository.observeTodayProgress().first()
        assertEquals(0, today.first { it.name == "Mandarin" }.completedToday)
    }

    @Test
    fun day_rollover_resets_visible_progress() = runTest {
        val welsh = languageByName("Welsh")
        repeat(5) { repository.recordCompletion(welsh.id) }
        val before = repository.observeTodayProgress().first().first { it.name == "Welsh" }
        assertTrue(before.isComplete)
        assertEquals(5, before.completedToday)

        clock.advance(Duration.ofDays(1))

        val after = repository.observeTodayProgress().first().first { it.name == "Welsh" }
        assertFalse(after.isComplete)
        assertEquals(0, after.completedToday)
        // Yesterday's history must still be in the DB
        assertEquals(5, db.completionDao().countsForDay("2026-05-10").first { it.languageId == welsh.id }.count)
    }

    @Test
    fun over_quota_is_allowed_and_reflected() = runTest {
        val mandarin = languageByName("Mandarin") // quota 1
        repeat(3) { repository.recordCompletion(mandarin.id) }
        val view = repository.observeTodayProgress().first().first { it.name == "Mandarin" }
        assertEquals(3, view.completedToday)
        assertTrue(view.isComplete)
        assertEquals(0, view.outstanding)
    }

    @Test
    fun adding_language_appends_with_next_display_order() = runTest {
        val newId = repository.addLanguage(
            name = "French",
            dailyQuota = 2,
            flagEmoji = "🇫🇷",
            vibeEmoji = "🥖",
            motivationPhrase = "Continue!",
        )
        val all = db.languageDao().all()
        assertEquals(4, all.size)
        val french = all.first { it.id == newId }
        assertEquals(3, french.displayOrder)
    }

    @Test
    fun update_language_persists_changes() = runTest {
        val welsh = languageByName("Welsh")
        repository.updateLanguage(welsh.copy(dailyQuota = 7))
        val updated = db.languageDao().byId(welsh.id)
        assertNotNull(updated)
        assertEquals(7, updated!!.dailyQuota)
    }

    @Test
    fun delete_language_cascades_completions() = runTest {
        val welsh = languageByName("Welsh")
        repository.recordCompletion(welsh.id)
        repository.recordCompletion(welsh.id)
        repository.deleteLanguage(welsh.id)
        val remaining = repository.observeTodayProgress().first()
        assertEquals(2, remaining.size)
        // Welsh's completions should be gone via FK cascade
        assertTrue(db.completionDao().countsForDay(dateProvider.todayIso()).none { it.languageId == welsh.id })
    }

    @Test
    fun reorder_swaps_display_order() = runTest {
        val before = db.languageDao().all()
        val ids = before.map { it.id }.reversed()
        repository.reorderLanguages(ids)
        val after = db.languageDao().all()
        assertEquals(ids, after.sortedBy { it.displayOrder }.map { it.id })
    }

    private suspend fun languageByName(name: String): Language =
        db.languageDao().all().first { it.name == name }
}
