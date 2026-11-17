package com.castivio.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The writer's batch runs a little above the default, and its pool thread is handed back as it was. */
class WriterPriorityTest {

    private var nice = 0
    private val changes = ArrayList<Int>()
    private val priority = WriterPriority(currentNice = { nice }, setNice = { nice = it; changes += it })

    @Test
    fun `a default thread is raised for the batch and given back after it`() {
        var during = Int.MIN_VALUE
        val result = priority.during { during = nice; "rows" }

        assertEquals("rows", result)
        assertEquals(WriterPriority.WRITER_NICE, during)
        assertEquals(0, nice)
        assertEquals(listOf(WriterPriority.WRITER_NICE, 0), changes)
    }

    @Test
    fun `a batch that throws still gives the thread back`() {
        try {
            priority.during { error("disk full") }
            fail("the batch's failure is the caller's")
        } catch (e: IllegalStateException) {
            assertEquals("disk full", e.message)
        }
        assertEquals(0, nice)
    }

    @Test
    fun `a thread already above the writer's priority is left alone`() {
        nice = -4
        priority.during { assertEquals(-4, nice) }

        assertEquals(-4, nice)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `a lower thread is raised to the writer's priority and put back to its own`() {
        nice = 10
        priority.during { assertEquals(WriterPriority.WRITER_NICE, nice) }

        assertEquals(10, nice)
    }

    @Test
    fun `a platform that refuses the change runs the batch as it is`() {
        val refusing = WriterPriority(currentNice = { 0 }, setNice = { throw SecurityException("no") })

        assertEquals(7, refusing.during { 7 })
    }

    @Test
    fun `a platform that cannot say the priority is not touched`() {
        val blind = WriterPriority(currentNice = { throw IllegalArgumentException("no thread") }, setNice = { changes += it })

        assertEquals(7, blind.during { 7 })
        assertTrue(changes.isEmpty())
    }
}
