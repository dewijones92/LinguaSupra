package com.dewijones.linguasupra.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.AppDatabase
import com.dewijones.linguasupra.data.DateProvider
import com.dewijones.linguasupra.data.MutableTestClock
import com.dewijones.linguasupra.data.SeedCallback
import com.dewijones.linguasupra.ui.home.HomeScreen
import com.dewijones.linguasupra.ui.theme.LinguaSupraTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HomeFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = composeRule.activity.applicationContext
        val clock = MutableTestClock(Instant.parse("2026-05-10T10:00:00Z"))
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedCallback)
            .allowMainThreadQueries()
            .build()
        AppContainer.overrideForTest(db, DateProvider { clock })
    }

    @After
    fun tearDown() {
        AppContainer.reset()
        db.close()
    }

    @Test
    fun three_default_languages_render_on_home_screen() {
        composeRule.setContent { LinguaSupraTheme { HomeScreen(onOpenSettings = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Mandarin", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mandarin", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Latin", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Welsh", substring = true).assertIsDisplayed()
    }

    @Test
    fun tapping_plus_on_welsh_updates_count_to_one_of_five() {
        composeRule.setContent { LinguaSupraTheme { HomeScreen(onOpenSettings = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription("Mark one Welsh lesson done")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Mark one Welsh lesson done")[0].performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("1 / 5", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("1 / 5").assertIsDisplayed()
    }

    @Test
    fun three_taps_on_welsh_show_three_of_five() {
        composeRule.setContent { LinguaSupraTheme { HomeScreen(onOpenSettings = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription("Mark one Welsh lesson done")
                .fetchSemanticsNodes().isNotEmpty()
        }
        repeat(3) {
            composeRule.onAllNodesWithContentDescription("Mark one Welsh lesson done")[0].performClick()
        }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("3 / 5", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("3 / 5").assertIsDisplayed()
    }

    @Test
    fun completing_quota_shows_motivation_phrase() {
        composeRule.setContent { LinguaSupraTheme { HomeScreen(onOpenSettings = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription("Mark one Mandarin lesson done")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Mark one Mandarin lesson done")[0].performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("加油!", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("加油!").assertIsDisplayed()
    }
}
