package com.castivio.core.common.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which posters ahead of a wall are fetched as it scrolls, and which are let go. */
class PrefetchWindowTest {

    private fun plan(pending: Set<Int>, lastShown: Int, ahead: Int = 10, count: Int = 300) =
        PrefetchWindow.plan(pending, lastShown, ahead, count)

    @Test
    fun `a wall that has just drawn asks for the next window in order`() {
        val p = plan(emptySet(), lastShown = 9)

        assertEquals((10..19).toList(), p.ask)
        assertTrue(p.drop.isEmpty())
    }

    @Test
    fun `scrolling one row on asks only for the new row and lets go of the row that came on screen`() {
        val p = plan(pending = (10..19).toSet(), lastShown = 14, ahead = 10)

        assertEquals((20..24).toList(), p.ask)
        assertEquals((10..14).toList(), p.drop)
    }

    @Test
    fun `a fling past the whole window lets all of it go, so no more than the window is ever in flight`() {
        val p = plan(pending = (10..19).toSet(), lastShown = 99, ahead = 10)

        assertEquals((100..109).toList(), p.ask)
        assertEquals((10..19).toList(), p.drop)
    }

    @Test
    fun `scrolling back lets go of what is now beyond the window`() {
        val p = plan(pending = (50..59).toSet(), lastShown = 44, ahead = 10)

        assertEquals((45..49).toList(), p.ask)
        assertEquals((55..59).toList(), p.drop)
    }

    @Test
    fun `a wall that has not moved changes nothing`() {
        val p = plan(pending = (10..19).toSet(), lastShown = 9)

        assertTrue(p.ask.isEmpty())
        assertTrue(p.drop.isEmpty())
    }

    @Test
    fun `the window stops at the end of the list, and nothing is asked before anything is on screen`() {
        assertEquals((12..13).toList(), plan(emptySet(), lastShown = 11, count = 14).ask)
        assertTrue(plan(emptySet(), lastShown = 13, count = 14).ask.isEmpty())
        assertTrue(plan(emptySet(), lastShown = -1).ask.isEmpty())
        assertTrue(plan(emptySet(), lastShown = 4, count = 0).ask.isEmpty())
        assertTrue(plan(emptySet(), lastShown = 4, ahead = 0).ask.isEmpty())
    }

    @Test
    fun `a list that shrank lets go of what is past its end`() {
        val p = plan(pending = (10..19).toSet(), lastShown = 9, count = 15)

        assertTrue(p.ask.isEmpty())
        assertEquals((15..19).toList(), p.drop)
    }
}
