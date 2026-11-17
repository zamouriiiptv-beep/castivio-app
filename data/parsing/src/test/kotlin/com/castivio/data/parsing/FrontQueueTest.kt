package com.castivio.data.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The list a walk of the categories takes from: the provider's order, with a viewer's asks first. */
class FrontQueueTest {

    private fun queue(vararg keys: String) = FrontQueue(keys.toList()) { it }

    private fun FrontQueue<String>.drain(): List<String> = generateSequence { next() }.toList()

    @Test
    fun `with no ask it hands out the provider's order`() {
        assertEquals(listOf("a", "b", "c"), queue("a", "b", "c").drain())
    }

    @Test
    fun `an asked item comes first and is not handed out again when its turn comes`() {
        val q = queue("a", "b", "c", "d")

        q.bump("c")

        assertEquals(listOf("c", "a", "b", "d"), q.drain())
    }

    @Test
    fun `the asks are served in the order they were made, ahead of the rest`() {
        val q = queue("a", "b", "c", "d", "e")

        q.bump("d")
        q.bump("b")

        assertEquals(listOf("d", "b", "a", "c", "e"), q.drain())
    }

    @Test
    fun `an ask made after some were handed out puts the item at the front of what is left`() {
        val q = queue("a", "b", "c", "d")

        assertEquals("a", q.next())
        q.bump("d")

        assertEquals(listOf("d", "b", "c"), q.drain())
    }

    @Test
    fun `an item already handed out, one asked twice and one that is not in the list are nothing`() {
        val q = queue("a", "b", "c")
        assertEquals("a", q.next())

        q.bump("a")
        q.bump("c")
        q.bump("c")
        q.bump("not-a-category")

        assertEquals(listOf("c", "b"), q.drain())
    }

    @Test
    fun `an empty list hands out nothing, and asks for nothing change that`() {
        val q = queue()

        q.bump("a")

        assertNull(q.next())
    }

    @Test
    fun `every item is handed out exactly once when workers take and a viewer asks at the same time`() {
        val keys = (0 until 400).map { "k$it" }
        val q = FrontQueue(keys) { it }
        val handed = ConcurrentLinkedQueue<String>()
        val pool = Executors.newFixedThreadPool(5)
        val start = CountDownLatch(1)
        val done = CountDownLatch(5)

        repeat(4) {
            pool.execute {
                start.await()
                while (true) handed += q.next() ?: break
                done.countDown()
            }
        }
        pool.execute {
            start.await()
            for (i in 399 downTo 0 step 3) q.bump("k$i")
            done.countDown()
        }
        start.countDown()

        done.await(10, TimeUnit.SECONDS)
        pool.shutdownNow()
        assertEquals("each once, none lost", keys.sorted(), handed.toList().sorted())
    }
}
