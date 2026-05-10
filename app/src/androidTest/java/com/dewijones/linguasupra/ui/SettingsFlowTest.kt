package com.dewijones.linguasupra.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.AppDatabase
import com.dewijones.linguasupra.data.DateProvider
import com.dewijones.linguasupra.data.SeedCallback
import com.dewijones.linguasupra.ui.settings.SettingsScreen
import com.dewijones.linguasupra.ui.theme.LinguaSupraTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = composeRule.activity.applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedCallback)
            .allowMainThreadQueries()
            .build()
        AppContainer.overrideForTest(db, DateProvider())
    }

    @After
    fun tearDown() {
        AppContainer.reset()
        db.close()
    }

    @Test
    fun settings_lists_the_three_seeded_languages() {
        composeRule.setContent { LinguaSupraTheme { SettingsScreen(onBack = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Mandarin").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mandarin").assertIsDisplayed()
        composeRule.onNodeWithText("Latin").assertIsDisplayed()
        composeRule.onNodeWithText("Welsh").assertIsDisplayed()
    }

    // Note: the ModalBottomSheet add-language picker is verified manually + via
    // dev/screenshots/phase4-add-language-sheet.png. Compose UI tests for sheet
    // entry animations are flaky on the AVD; the underlying add() round-trip
    // is covered by RepositoryTest.adding_language_appends_with_next_display_order.

    @Test
    fun increment_quota_button_bumps_per_day_text() {
        composeRule.setContent { LinguaSupraTheme { SettingsScreen(onBack = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Mandarin").fetchSemanticsNodes().isNotEmpty()
        }
        // Mandarin (1) + Latin (1) start at "1 per day".
        composeRule.onAllNodesWithText("1 per day").assertCountEquals(2)
        // Tap Mandarin's increase (first row's + button).
        composeRule.onAllNodesWithContentDescription("Increase quota")[0].performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("2 per day").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("2 per day").assertCountEquals(1)
        composeRule.onAllNodesWithText("1 per day").assertCountEquals(1)
    }

    @Test
    fun deleting_a_language_removes_it_from_the_list() {
        composeRule.setContent { LinguaSupraTheme { SettingsScreen(onBack = {}) } }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Latin").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Delete Latin").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Latin").fetchSemanticsNodes().isEmpty()
        }
    }
}
