package com.castivio.feature.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The control the three boards' bands carry for their order, drawn by one function: it says the order it is in, and a press is
 * the board's -- the films and the shows go round their four, the channels go between the two of their bouquets.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land")
class SortChipTest {

    @get:Rule
    val compose = createComposeRule()

    private var presses = 0

    private fun show(order: SortOrder) {
        compose.setContent {
            CastivioTheme {
                SortChip(order, channelsMetricsFor(tv = true, width = 960.dp, height = 540.dp)) { presses++ }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the chip says the order the provider gave`() {
        show(SortOrder.PROVIDER)

        compose.onNodeWithText("Sort: Provider").assertIsDisplayed()
    }

    @Test
    fun `by name it says A to Z`() {
        show(SortOrder.NAME_ASC)

        compose.onNodeWithText("Sort: A to Z").assertIsDisplayed()
    }

    @Test
    fun `a press is the board's`() {
        show(SortOrder.PROVIDER)

        compose.onNodeWithText("Sort: Provider").performClick()
        compose.waitForIdle()

        assertEquals(1, presses)
    }
}
