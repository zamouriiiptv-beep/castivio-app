package com.castivio.feature.home

import com.castivio.domain.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The band's one order control, as the holder runs it: every board draws the same control and hands the holder the press, and the
 * holder -- which knows its section -- decides what it orders. The live board's rows keep their numbers, so its control orders the
 * bouquets, between the provider's order and by name; the films', the shows' and the radio's go round four orders of their rows.
 *
 * What the bouquets are ordered by is `BouquetOrderTest`'s, and what the live board does with its order beside reorder mode is
 * `BrowseBouquetOrderTest`'s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseNextOrderTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a board starts in the provider's order, and the control names it`() = runTest(dispatcher) {
        val board = browseBoard()

        assertEquals(SortOrder.PROVIDER, board.state.bandOrder)
    }

    @Test
    fun `on the live board the control orders the bouquets, between two orders`() = runTest(dispatcher) {
        val board = browseBoard()
        val asked = board.pager.asked.size

        board.model.nextOrder(); runCurrent()

        assertEquals(SortOrder.NAME_ASC, board.state.bandOrder)
        assertEquals(SortOrder.NAME_ASC, board.state.bouquetSort)
        // The rows keep their numbers: nothing of them was asked for again, and their order is the provider's.
        assertEquals(SortOrder.PROVIDER, board.state.sort)
        assertEquals(asked, board.pager.asked.size)

        board.model.nextOrder(); runCurrent()

        assertEquals(SortOrder.PROVIDER, board.state.bandOrder)
        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
    }

    @Test
    fun `on the films' board the control orders the rows, round four orders`() = runTest(dispatcher) {
        val board = browseBoard(section = CatalogSection.Movies)

        val seen = buildList {
            repeat(4) {
                board.model.nextOrder(); runCurrent()
                add(board.state.bandOrder)
            }
        }

        assertEquals(
            listOf(SortOrder.NAME_ASC, SortOrder.NAME_DESC, SortOrder.RECENTLY_ADDED, SortOrder.PROVIDER),
            seen,
        )
    }

    @Test
    fun `on the films' board the rows are read in the order the control names, and the bouquets are left as they were`() = runTest(dispatcher) {
        val board = browseBoard(section = CatalogSection.Movies)

        board.model.nextOrder(); runCurrent()

        assertEquals(SortOrder.NAME_ASC, board.last.sort)
        assertEquals(SortOrder.NAME_ASC, board.state.sort)
        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
        assertEquals(listOf("sports", "news", "kids"), board.bouquets)
    }

    @Test
    fun `while bouquets are being moved the control does nothing`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.choose("sports"); runCurrent()
        board.model.arrange(); runCurrent()

        board.model.nextOrder(); runCurrent()

        assertEquals(SortOrder.PROVIDER, board.state.bandOrder)
    }
}
