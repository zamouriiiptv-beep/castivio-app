package com.castivio.feature.settings

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.MotionLevel
import com.castivio.domain.AutoLoadSetting
import com.castivio.domain.PictureCache
import kotlinx.coroutines.flow.MutableStateFlow
import com.castivio.domain.TrackLanguage
import com.castivio.domain.TrackLanguagePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings on a handset, composed over its real state holder with the languages and the cache as fakes: the rows the
 * mockup draws, a count of nothing hidden as a line with nothing to press, a language chosen from its list and kept, and the
 * help page's two buttons. The television's QR codes are `SettingsQrTest`'s.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land")
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private class Kept : TrackLanguagePreferences {
        var sound: TrackLanguage? = null
        var text: TrackLanguage? = null
        override fun audio() = sound
        override fun subtitle() = text
        override fun setAudio(language: TrackLanguage?) { sound = language }
        override fun setSubtitle(language: TrackLanguage?) { text = language }
    }

    private object NoPictures : PictureCache {
        override suspend fun sizeBytes(): Long = 0
        override suspend fun clear() = Unit
    }

    private val kept = Kept()

    private class Auto : AutoLoadSetting {
        val state = MutableStateFlow(true)
        override val enabled = state
        override fun set(on: Boolean) {
            state.value = on
        }
    }

    private val auto = Auto()
    private var motion = MotionLevel.FULL
    private var restoredBouquets = 0
    private var restoredChannels = 0
    private var about = 0
    private var backed = 0

    private fun settings(hiddenBouquets: Int = 3, hiddenChannels: Int = 0, measuring: Boolean = false) {
        val model = SettingsViewModel(kept, NoPictures, auto)
        compose.setContent {
            CastivioTheme {
                SettingsScreen(
                    motion = motion,
                    onMotion = { motion = it },
                    hiddenBouquets = hiddenBouquets,
                    hiddenChannels = hiddenChannels,
                    onRestoreBouquets = { restoredBouquets++ },
                    onRestoreChannels = { restoredChannels++ },
                    onChangePin = {},
                    onAbout = { about++ },
                    version = "1.0.0",
                    onBack = { backed++ },
                    measuring = measuring,
                    model = model,
                )
            }
        }
        compose.waitForIdle()
    }

    /** Opens a section from the side list. */
    private fun open(pane: SettingsPane) {
        compose.onNodeWithTag(settingsNavTag(pane)).performClick()
        compose.waitForIdle()
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(SETTINGS_PAGE_TAG).performScrollToNode(hasText(text))
    }

    @Test
    fun `the sections and their rows are the mockup's`() {
        settings()

        val rows = mapOf(
            SettingsPane.PLAYER to listOf("Preferred audio language", "Preferred subtitle language"),
            SettingsPane.LIBRARY to listOf("Restore hidden bouquets", "Restore hidden channels"),
            SettingsPane.LOCK to listOf("Change the lock password"),
            SettingsPane.APPLICATION to listOf("Motion", "Clear picture cache", "About"),
            SettingsPane.HELP to listOf("Help and support", "Privacy policy", "Terms of use"),
        )
        for ((pane, texts) in rows) {
            open(pane)
            for (row in texts) {
                scrollTo(row)
                compose.onNodeWithText(row).assertExists()
            }
        }
        // No preference yet: the original sound, and no subtitles.
        open(SettingsPane.PLAYER)
        compose.onNodeWithText("Original sound").assertExists()
        compose.onNodeWithText("No subtitles").assertExists()
    }

    @Test
    fun `the side list opens one section at a time`() {
        settings()

        compose.onNodeWithText("Preferred audio language").assertExists()
        assertTrue(compose.onAllNodesWithText("Change the lock password").fetchSemanticsNodes().isEmpty())
        open(SettingsPane.LOCK)
        compose.onNodeWithText("Change the lock password").assertExists()
        assertTrue(compose.onAllNodesWithText("Preferred audio language").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `a count of nothing hidden is a line with nothing to press, and one of some brings them back`() {
        settings(hiddenBouquets = 3, hiddenChannels = 0)
        open(SettingsPane.LIBRARY)

        compose.onNodeWithText("Restore hidden channels").assertHasNoClickAction()
        compose.onNodeWithText("Restore hidden bouquets").performClick()

        assertEquals(1, restoredBouquets)
        assertEquals(0, restoredChannels)
    }

    @Test
    fun `a language is chosen from its list, kept, and shown on its row`() {
        settings()

        compose.onNodeWithText("Preferred audio language").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("العربية").performClick()
        compose.waitForIdle()

        assertEquals(TrackLanguage.ARABIC, kept.sound)
        compose.onNodeWithText("العربية").assertExists()
        assertTrue(compose.onAllNodesWithText("Original sound").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `loading the films and series by themselves is on, and one press turns it off`() {
        settings()
        open(SettingsPane.LIBRARY)

        compose.onNodeWithText("Load films and series automatically").assertExists()
        compose.onNodeWithText("On Wi-Fi, and not while you watch").assertExists()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.waitForIdle()

        assertEquals(false, auto.state.value)
        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `a viewer's build offers no smoothness panel`() {
        settings(measuring = false)

        assertTrue(compose.onAllNodesWithText(SMOOTHNESS_ROW).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `a measuring build opens the smoothness panel from Settings, and Back closes it`() {
        settings(measuring = true)
        open(SettingsPane.MEASURE)

        scrollTo(SMOOTHNESS_ROW)
        compose.onNodeWithText(SMOOTHNESS_ROW).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Nothing measured yet: use the application, then come back.").assertExists()
        compose.onNodeWithText("Clear").assertHasClickAction()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(SMOOTHNESS_ROW).assertExists()
    }

    @Test
    fun `the motion level is picked in its own row`() {
        settings()
        open(SettingsPane.APPLICATION)

        scrollTo("Motion")
        compose.onNodeWithText("Reduced").performClick()

        assertEquals(MotionLevel.REDUCED, motion)
    }

    @Test
    fun `about opens the licence screen, and the arrow goes back`() {
        settings()
        open(SettingsPane.APPLICATION)

        scrollTo("About")
        compose.onNodeWithText("About").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, about)
        assertEquals(1, backed)
    }

    @Test
    fun `on a handset the help page opens the browser and the mail`() {
        settings()
        open(SettingsPane.HELP)

        scrollTo("Help and support")
        compose.onNodeWithText("Help and support").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Open in the browser").assertHasClickAction()
        compose.onNodeWithText(SettingsLinks.EMAIL).assertHasClickAction()
    }
}
