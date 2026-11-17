package com.castivio.feature.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The band every board calls, as a board sees it: the board's own words in the field, the order control the holder names and a
 * press on it handed back -- and none where the board has no order to offer. What the band lays out, and where, is
 * `BoardBandTest`'s.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land")
class BoardHeaderTest {

    @get:Rule
    val compose = createComposeRule()

    private var presses = 0

    private fun show(order: SortOrder?, hint: String = "Search movies") {
        compose.setContent {
            CastivioTheme {
                BoardHeader(
                    m = channelsMetricsFor(tv = true, width = 960.dp, height = 540.dp),
                    searchText = "",
                    onSearch = {},
                    hint = hint,
                    order = order,
                    onOrder = { presses++ },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the field says what the board searches`() {
        show(order = SortOrder.PROVIDER, hint = "Search stations")

        compose.onNodeWithText("Search stations").assertIsDisplayed()
    }

    @Test
    fun `the control names the order the board is in`() {
        show(order = SortOrder.NAME_ASC)

        compose.onNodeWithText("Sort: A to Z").assertIsDisplayed()
    }

    @Test
    fun `a press on the control is handed back to the board`() {
        show(order = SortOrder.PROVIDER)

        compose.onNodeWithText("Sort: Provider").performClick()
        compose.waitForIdle()

        assertEquals(1, presses)
    }

    @Test
    fun `a board with no order to offer has no control for one`() {
        show(order = null)

        assertTrue(compose.onAllNodesWithText("Sort:", substring = true).fetchSemanticsNodes().isEmpty())
    }
}
