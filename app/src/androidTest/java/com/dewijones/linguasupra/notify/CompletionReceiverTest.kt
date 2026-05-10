package com.dewijones.linguasupra.notify

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.AppDatabase
import com.dewijones.linguasupra.data.DateProvider
import com.dewijones.linguasupra.data.MutableTestClock
import com.dewijones.linguasupra.data.SeedCallback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CompletionReceiverTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    private lateinit var db: AppDatabase
    private lateinit var clock: MutableTestClock

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clock = MutableTestClock(Instant.parse("2026-05-10T10:00:00Z"))
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedCallback)
            .allowMainThreadQueries()
            .build()
        AppContainer.overrideForTest(db, DateProvider { clock })
        // Clear any leftover banner from a prior run
        context.getSystemService(NotificationManager::class.java)?.cancel(Notifications.BANNER_NOTIFICATION_ID)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSystemService(NotificationManager::class.java)?.cancel(Notifications.BANNER_NOTIFICATION_ID)
        AppContainer.reset()
        db.close()
    }

    @Test
    fun single_tap_writes_completion_for_today() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val welshId = languageIdByName("Welsh")
        CompletionReceiver.handle(context, welshId)

        val progress = AppContainer.get(context).repository.observeTodayProgress().first()
        val welsh = progress.first { it.name == "Welsh" }
        assertEquals(1, welsh.completedToday)
        assertEquals("2026-05-10", db.completionDao().observeForDay("2026-05-10").first().first().dayLocalIso)
    }

    /**
     * Verifies the banner the manager *builds* (channel + ongoing flag), not
     * what the system later renders. Going through `nm.activeNotifications`
     * here would race with `BannerService` (started by `BootReceiver` on
     * `MY_PACKAGE_REPLACED` after each test install) — the assertion is about
     * the Notification we construct, not delivery.
     */
    @Test
    fun handle_builds_ongoing_banner_on_correct_channel() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer.get(context)
        container.repository.recordCompletion(languageIdByName("Mandarin"))

        val progress = container.repository.observeTodayProgress().first()
        val notification = BannerNotificationManager(context, container.repository)
            .notificationFor(progress)

        assertEquals(Notifications.BANNER_CHANNEL_ID, notification.channelId)
        assertTrue(
            "Banner should be ongoing",
            notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0,
        )
    }

    @Test
    fun three_taps_increment_count_to_three() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val welshId = languageIdByName("Welsh")
        repeat(3) { CompletionReceiver.handle(context, welshId) }

        val progress = AppContainer.get(context).repository.observeTodayProgress().first()
        assertEquals(3, progress.first { it.name == "Welsh" }.completedToday)
    }

    @Test
    fun over_quota_taps_are_not_clamped() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mandarinId = languageIdByName("Mandarin") // quota 1
        repeat(5) { CompletionReceiver.handle(context, mandarinId) }

        val mandarin = AppContainer.get(context).repository.observeTodayProgress().first()
            .first { it.name == "Mandarin" }
        assertEquals(5, mandarin.completedToday)
        assertTrue(mandarin.isComplete)
    }

    @Test
    fun unknown_language_id_is_a_safe_no_op() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Inserting a completion against a non-existent FK would fail; we test the receiver
        // surface by checking that an invalid extra never reaches handle(). The on-receive
        // gate in CompletionReceiver discards `-1L`. We assert behaviour through the public
        // surface: nothing in DB after a no-op intent.
        val before = db.completionDao().observeForDay("2026-05-10").first().size
        // Directly exercising the gate: simulate an Intent without the extra by calling
        // through the BroadcastReceiver path. Sending an explicit -1 round-trips the check.
        val intent = android.content.Intent(CompletionReceiver.ACTION_COMPLETE_LANGUAGE).apply {
            putExtra(CompletionReceiver.EXTRA_LANGUAGE_ID, -1L)
        }
        CompletionReceiver().onReceive(context, intent)
        val after = db.completionDao().observeForDay("2026-05-10").first().size
        assertEquals(before, after)
    }

    private suspend fun languageIdByName(name: String): Long =
        db.languageDao().all().first { it.name == name }.id
}
