package com.castivio.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The main thread's frames during a load: how many, how many late, and the longest wait. */
class FrameGapsTest {

    private val ms = 1_000_000L

    @Test
    fun `nothing is said before two frames`() {
        val gaps = FrameGaps()
        assertNull(gaps.describe())
        gaps.record(0)
        assertNull(gaps.describe())
    }

    @Test
    fun `frames a vsync apart are not late, and a stall is`() {
        val gaps = FrameGaps()
        var t = 0L
        gaps.record(t)
        repeat(10) { t += 16 * ms; gaps.record(t) }
        t += 41 * ms
        gaps.record(t)
        t += 26 * ms
        gaps.record(t)

        assertEquals("frames 12 · late 2 (over 25 ms) · longest 41 ms", gaps.describe())
    }

    @Test
    fun `a clock that does not move forward is not a frame`() {
        val gaps = FrameGaps()
        gaps.record(100 * ms)
        gaps.record(100 * ms)
        gaps.record(90 * ms)
        gaps.record(106 * ms)

        assertEquals("frames 1 · late 0 (over 25 ms) · longest 16 ms", gaps.describe())
    }
}
