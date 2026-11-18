package com.castivio.core.common.prefetch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

/** When a card's details are asked for ahead of the press: after a rest, one at a time, once each. */
@OptIn(ExperimentalCoroutinesApi::class)
class DwellPrefetcherTest {

    private val scope = TestScope()
    private val asked = ArrayList<String>()
    private val gates = HashMap<String, CompletableDeferred<Unit>>()
    private val prefetcher = DwellPrefetcher(scope, dwellMs = 600) { id ->
        asked += id
        gates.getOrPut(id) { CompletableDeferred() }.await()
    }

    private fun after(ms: Long) {
        scope.advanceTimeBy(ms)
        scope.runCurrent()
    }

    private fun answer(id: String) {
        gates.getOrPut(id) { CompletableDeferred() }.complete(Unit)
        scope.runCurrent()
    }

    @Test
    fun `a card rested on is asked for, and one passed over is not`() {
        prefetcher.focused("a")
        after(300)
        prefetcher.focused("b")
        after(599)
        assertEquals(emptyList<String>(), asked)
        after(1)
        assertEquals(listOf("b"), asked)
    }

    @Test
    fun `a focus that leaves before the rest is up asks nothing`() {
        prefetcher.focused("a")
        after(400)
        prefetcher.left("a")
        after(1000)
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `one at a time, and only the latest card waits its turn`() {
        prefetcher.focused("a")
        after(600)
        prefetcher.focused("b")
        after(600)
        prefetcher.focused("c")
        after(600)
        assertEquals(listOf("a"), asked)

        answer("a")
        assertEquals("b was passed over; c, where the viewer is, is next", listOf("a", "c"), asked)
    }

    @Test
    fun `a card asked once is not asked again`() {
        prefetcher.focused("a")
        after(600)
        answer("a")
        prefetcher.focused("b")
        after(100)
        prefetcher.focused("a")
        after(2000)
        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `a card that waited its turn and was left is not asked`() {
        prefetcher.focused("a")
        after(600)
        prefetcher.focused("b")
        after(600)
        prefetcher.left("b")
        answer("a")
        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `a request that fails still gives the next its turn`() {
        val failing = DwellPrefetcher(scope, dwellMs = 600) { id -> asked += id; if (id == "a") error("no answer") }
        failing.focused("a")
        after(600)
        failing.focused("b")
        after(600)
        assertEquals(listOf("a", "b"), asked)
    }

    @Test
    fun `a card drawn without the focus does not end the wait on the card that has it`() {
        prefetcher.focused("a")
        after(300)
        prefetcher.left("z")
        after(300)
        assertEquals(listOf("a"), asked)
    }
}
