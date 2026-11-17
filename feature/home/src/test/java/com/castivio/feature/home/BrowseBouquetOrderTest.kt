package com.castivio.feature.home

import com.castivio.domain.MediaGroup
import com.castivio.domain.MediaKind
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
 * The channels' band has an order for the rail's bouquets -- the provider's, or by name -- as the board's holder runs it:
 * the rail lists them in it, the rows are not touched (a channel's place is its number, and the large player's keys step
 * through that list), a bouquet that arrives while the order is by name takes its place in it, and reorder mode, which
 * moves the bouquets as they are saved, is of the provider's order and cannot be left for another.
 *
 * What the names are ordered by is `BouquetOrderTest`'s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseBouquetOrderTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Sports, News and Kids, in that order: the provider's, and the reverse of what the alphabet makes of them. */
    private val provider = listOf("sports", "news", "kids")

    @Test
    fun `the rail lists the bouquets in the provider's order until another is asked for`() = runTest(dispatcher) {
        val board = browseBoard()

        assertEquals(provider, board.bouquets)
        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
    }

    @Test
    fun `by name the rail lists them in order, and what is stored is as it was`() = runTest(dispatcher) {
        val board = browseBoard()

        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        assertEquals(listOf("kids", "news", "sports"), board.bouquets)
        assertEquals(SortOrder.NAME_ASC, board.state.bouquetSort)
        assertEquals(provider, board.state.groups.map { it.id })
    }

    @Test
    fun `the rows are read as they were, and the list does not start again`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.choose("sports"); runCurrent()
        val asked = board.pager.asked.size

        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        // A channel's place in the list is its number: the query is the one it was, and nothing asked for the rows again.
        assertEquals(asked, board.pager.asked.size)
        assertEquals(SortOrder.PROVIDER, board.last.sort)
        assertEquals(SortOrder.PROVIDER, board.state.sort)
        assertEquals("sports", board.state.selectedGroup)
    }

    @Test
    fun `the provider's order brings them back`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        board.model.sortBouquetsBy(SortOrder.PROVIDER); runCurrent()

        assertEquals(provider, board.bouquets)
        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
    }

    @Test
    fun `the rail's field narrows the list that is in order`() = runTest(dispatcher) {
        val arts = MediaGroup(id = "arts", name = "Arts", kind = MediaKind.LIVE, itemCount = 6)
        val board = browseBoard(groups = listOf(SPORTS, NEWS, KIDS, arts))
        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        board.model.filterGroups("ts"); runCurrent()

        // "Sports" and "Arts" hold "ts", "News" and "Kids" do not -- and the two stand in the alphabet's order, not the provider's.
        assertEquals(listOf("arts", "sports"), board.bouquets)
    }

    @Test
    fun `a bouquet that arrives while the order is by name takes its place in it`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        board.catalogue.held.value = listOf(SPORTS, NEWS, KIDS, MediaGroup(id = "arts", name = "Arts", kind = MediaKind.LIVE, itemCount = 6))
        runCurrent()

        assertEquals(listOf("arts", "kids", "news", "sports"), board.bouquets)
    }

    @Test
    fun `reorder mode is of the order the bouquets are saved in`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()
        board.model.choose("sports"); runCurrent()

        board.model.arrange(); runCurrent()

        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
        assertEquals(provider, board.bouquets)
    }

    @Test
    fun `the order cannot be changed while bouquets are being moved`() = runTest(dispatcher) {
        val board = browseBoard()
        board.model.choose("sports"); runCurrent()
        board.model.arrange(); runCurrent()

        board.model.sortBouquetsBy(SortOrder.NAME_ASC); runCurrent()

        assertEquals(SortOrder.PROVIDER, board.state.bouquetSort)
        assertEquals(provider, board.bouquets)
    }
}
