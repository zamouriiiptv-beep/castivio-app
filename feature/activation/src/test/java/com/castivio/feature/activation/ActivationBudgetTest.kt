package com.castivio.feature.activation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.Sizing
import com.castivio.core.design.theme.castivioMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The first screen's arithmetic, on the JVM, against the frames Castivio ships to.
 *
 * Robolectric does not lay text out, so whether the drawing *fits* is asked here, of
 * the numbers the composition is built from: the drawing's pixels through one unit
 * (see [Metrics]).
 */
class ActivationBudgetTest {

    private data class Surface(val name: String, val width: Dp, val height: Dp, val tv: Boolean) {
        override fun toString() = name
    }

    private val frames = listOf(
        Surface("shortest phone 800x360", 800.dp, 360.dp, tv = false),
        Surface("reference phone 873x393", 873.dp, 393.dp, tv = false),
        Surface("tablet 1280x800", 1280.dp, 800.dp, tv = false),
        Surface("television 960x540", 960.dp, 540.dp, tv = true),
        Surface("1080p set 1920x1080", 1920.dp, 1080.dp, tv = true),
    )

    private fun metrics(s: Surface) = metricsFor(s.tv, s.width, s.height)

    @Test
    fun `the drawing fits under the header on every surface`() {
        for (s in frames) {
            val m = metrics(s)
            val spare = s.height - m.headerBand - m.frame.stageBottom - m.contentHeight()
            println("activation budget — $s unit ${m.unit} spare $spare")
            assertTrue("$s: the drawing overruns the room under the header by ${-spare}", spare >= (-0.5).dp)
        }
    }

    @Test
    fun `the two cards fit between the edges on every surface`() {
        for (s in frames) {
            val m = metrics(s)
            val room = s.width - m.frame.edge * 2
            assertTrue("$s: the row overruns by ${m.rowWidth() - room}", m.rowWidth() <= room + 0.5.dp)
        }
    }

    @Test
    fun `what is added here fits the card's height`() {
        for (s in frames) {
            val m = metrics(s)
            assertTrue(
                "$s: the identifiers, the heading, the kinds and the button need " +
                    "${m.rightColumnHeight()} of a ${m.cardHeight} card",
                m.rightColumnHeight() <= m.cardHeight,
            )
        }
    }

    @Test
    fun `the content constant is the content the screen stacks`() {
        for (s in frames) {
            val m = metrics(s)
            assertEquals("$s", (m.unit * CONTENT).value, m.contentHeight().value, m.unit.value * 2)
        }
    }

    @Test
    fun `the reference phone draws the drawing at its own scale`() {
        val m = metrics(frames[1])
        assertTrue("the unit ${m.unit} is not the drawing's 1/2.75", m.unit.value in 0.355f..0.3645f)
    }

    @Test
    fun `every control clears the floor for its device`() {
        for (s in frames) {
            val m = metrics(s)
            val floor = Sizing.minTarget(s.tv)
            assertTrue("$s: a kind is ${m.kindHeight}", m.kindHeight >= floor)
            assertTrue("$s: the button is ${m.ctaHeight}", m.ctaHeight >= floor)
            assertTrue("$s: a copy button is ${m.copy}", m.copy >= floor)
            assertTrue("$s: the header is ${m.header}", m.header >= floor)
        }
    }

    @Test
    fun `the stage comes from the shared system`() {
        for (s in frames) {
            assertEquals("$s", castivioMetrics(s.width, s.height, s.tv), metrics(s).frame)
        }
    }
}
