package com.castivio.feature.licence

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.castivioMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The licence screen's arithmetic, on the JVM, against the frames Castivio ships to.
 *
 * Robolectric does not lay text out — every `Text` measures 35dp whatever its style —
 * so whether the drawing *fits* cannot be asked of a composition. It is asked here, of
 * the numbers the composition is built from: the drawing's pixels through one unit
 * (see [LicenceMetrics]).
 */
class LicenceBudgetTest {

    private data class Surface(val name: String, val width: Dp, val height: Dp, val tv: Boolean) {
        override fun toString() = name
    }

    private val shortest = Surface("shortest phone 800x360", 800.dp, 360.dp, tv = false)
    private val handset = Surface("reference phone 873x393", 873.dp, 393.dp, tv = false)
    private val tablet = Surface("tablet 1280x800", 1280.dp, 800.dp, tv = false)
    private val television = Surface("television 960x540", 960.dp, 540.dp, tv = true)
    private val set1080 = Surface("1080p set 1920x1080", 1920.dp, 1080.dp, tv = true)

    private val frames = listOf(shortest, handset, tablet, television, set1080)

    private fun metrics(s: Surface) = licenceMetricsFor(s.tv, s.width, s.height)

    /** Hero, cards and identifiers fit under the header, on every frame. */
    @Test
    fun `the drawing fits under the header on every surface`() {
        for (s in frames) {
            val m = metrics(s)
            val room = s.height - m.headerBand - m.frame.stageBottom
            val spare = room - m.contentHeight()
            println("licence budget — $s unit ${m.unit} spare $spare")
            assertTrue("$s: the drawing overruns the room under the header by ${-spare}", spare >= (-0.5).dp)
        }
    }

    /** The row of three cards fits between the stage's edges. */
    @Test
    fun `the row of cards fits between the edges on every surface`() {
        for (s in frames) {
            val m = metrics(s)
            val room = s.width - m.frame.edge * 2
            assertTrue("$s: the row overruns by ${m.rowWidth() - room}", m.rowWidth() <= room + 0.5.dp)
        }
    }

    /**
     * The sum the unit is chosen against is the sum the screen draws.
     *
     * [CONTENT] is the drawing's height under the header, in its pixels, and
     * [contentHeight] is the same stack built from the metrics. If a gap is added to
     * one and not the other, the unit stops guaranteeing the fit — this is where that
     * shows.
     */
    @Test
    fun `the content constant is the content the screen stacks`() {
        for (s in frames) {
            val m = metrics(s)
            assertEquals("$s", (m.unit * CONTENT).value, m.contentHeight().value, m.unit.value * 2)
        }
    }

    /**
     * The reference phone is the drawing itself: 393dp tall is 1080 pixels at 2.75 to
     * the dp, so one drawing pixel is 1/2.75dp and the QR is the activation screen's.
     */
    @Test
    fun `the reference phone draws the drawing at its own scale`() {
        val m = metrics(handset)
        assertEquals("unit", 393f / 1080f, m.unit.value, 0.002f)
        assertEquals("card height", 440f * 393f / 1080f, m.cardHeight.value, 0.5f)
        assertEquals("QR plate", 372f * 393f / 1080f, m.plate.value, 0.5f)
    }

    /** The QR is never smaller than a phone camera reads from across a room. */
    @Test
    fun `the code stays readable on every surface`() {
        for (s in frames) {
            assertTrue("$s: the QR plate is ${metrics(s).plate}", metrics(s).plate >= QR_FLOOR)
        }
    }

    /** And the frame comes from the system every other screen reads, header included. */
    @Test
    fun `the stage comes from the shared system`() {
        for (s in frames) {
            val m = metrics(s)
            assertEquals("$s: the frame did not come from castivioMetrics", castivioMetrics(s.width, s.height, s.tv), m.frame)
            assertEquals("$s: the header band is not Home's", m.frame.header + m.frame.stageTop, m.headerBand)
        }
    }

    /** The plan cards are buttons: a card is far taller than any device's floor. */
    @Test
    fun `a plan card clears the floor for its device`() {
        for (s in frames) {
            val m = metrics(s)
            val floor = com.castivio.core.design.theme.Sizing.minTarget(s.tv)
            assertTrue("$s: a plan card is ${m.plan.minHeight}, below $floor", m.plan.minHeight >= floor)
        }
    }
}

/** The activation screen's own floor for its plate on the shortest phone. */
private val QR_FLOOR = 112.dp
