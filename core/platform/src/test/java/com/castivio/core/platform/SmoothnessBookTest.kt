package com.castivio.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The smoothness panel's book: each screen's frames and openings, and each action's. */
class SmoothnessBookTest {

    private val ms = 1_000_000L

    @Test
    fun `frames belong to the screen on display, and the gap across a change to neither`() {
        val book = SmoothnessBook()
        book.showing("Home")
        var t = 0L
        book.frame(t)
        repeat(5) { t += 16 * ms; book.frame(t) }
        book.showing("Channels")
        t += 300 * ms
        book.frame(t)
        t += 40 * ms
        book.frame(t)

        val rows = book.rows().associateBy { it.name }
        assertEquals(5, rows.getValue("Home").frames)
        assertEquals(0, rows.getValue("Home").late)
        assertEquals(1, rows.getValue("Channels").frames)
        assertEquals(1, rows.getValue("Channels").late)
        assertEquals(40L, rows.getValue("Channels").longestMs)
    }

    @Test
    fun `openings are the median and the slowest of the latest`() {
        val book = SmoothnessBook()
        listOf(120L, 80L, 300L, 90L, 100L).forEach { book.opened("Movies", it) }

        val row = book.rows().single()
        assertEquals(5, row.opens)
        assertEquals(100L, row.openMedianMs)
        assertEquals(300L, row.openSlowestMs)
        assertNull(row.latePercent)
        assertEquals("Movies · opens 5 · 100/300 ms", row.describe())
    }

    @Test
    fun `only the latest openings are kept`() {
        val book = SmoothnessBook()
        repeat(SmoothnessBook.KEEP) { book.opened("Search", 1000) }
        repeat(SmoothnessBook.KEEP) { book.opened("Search", 10) }

        assertEquals(10L, book.rows().single().openSlowestMs)
    }

    @Test
    fun `a line with frames says the share that came late`() {
        val book = SmoothnessBook()
        book.showing("Series")
        var t = 0L
        book.frame(t)
        repeat(9) { t += 16 * ms; book.frame(t) }
        t += 50 * ms
        book.frame(t)

        assertEquals("Series · frames 10 · late 1 (10.0%) · longest 50 ms", book.rows().single().describe())
    }

    @Test
    fun `starting over keeps the screen on display and forgets the rest`() {
        val book = SmoothnessBook()
        book.showing("Home")
        book.opened("Movies", 100)
        book.reset()

        assertEquals(listOf("Home"), book.rows().map { it.name })
        assertEquals(0, book.rows().single().frames)
    }
}
