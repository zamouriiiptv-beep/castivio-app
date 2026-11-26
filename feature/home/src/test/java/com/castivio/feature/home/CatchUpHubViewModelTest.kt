package com.castivio.feature.home

import com.castivio.core.common.Outcome
import com.castivio.domain.ArchiveGroup
import com.castivio.domain.CatchUpChannel
import com.castivio.domain.CatchUpGateway
import com.castivio.domain.CatchUpListing
import com.castivio.domain.CatchUpProgramme
import com.castivio.domain.Channel
import com.castivio.domain.MediaGroup
import com.castivio.domain.time.TimeAnchorSource
import com.castivio.domain.time.TimeReading
import com.castivio.domain.time.TimeTrust
import com.castivio.domain.time.TrustedTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Where the viewer is inside Catch-up: the three columns and a day's programmes, the order Back leaves them in, and what
 * the two searches do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatchUpHubViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Clock(var epochMs: Long) : TrustedTime {
        override fun now(): TimeReading = TimeReading(epochMs, TimeTrust.DEVICE)
        override fun anchor(epochMs: Long, source: TimeAnchorSource): TimeReading {
            this.epochMs = epochMs
            return now()
        }
    }

    /** A provider that is never asked: the places and the search are the view-model's own. */
    private class Gateway : CatchUpGateway {
        override suspend fun listing(channel: CatchUpChannel, nowMs: Long): CatchUpListing = error("not asked")
        override suspend fun playbackUrl(channel: CatchUpChannel, programme: CatchUpProgramme): Outcome<String> =
            error("not asked")
        override fun knownEmpty(mediaId: String, nowMs: Long): Boolean = false
    }

    private val clock = Clock(1_000L)
    private val gateway = Gateway()
    private val pager = FakePager()

    private fun hub() = CatchUpHubViewModel(
        catalog = FakeCatalogue(emptyList<MediaGroup>()),
        pager = pager,
        gateway = gateway,
        clock = clock,
    )

    private val france = ArchiveGroup(id = "g1", name = "France", channels = 12)
    private val channel = Channel(
        id = "c1",
        title = "One",
        artworkUrl = null,
        number = 1,
        groupId = "g1",
        streamUrl = "http://host/live/u/p/1.ts",
        epgChannelId = null,
        catchUpHours = 72,
    )

    // ------------------------------------------------------------------------------- the stages

    @Test
    fun `it opens on the three columns, at all, with no channel chosen`() {
        assertEquals(HubStage.Browse(), hub().stage.value)
    }

    @Test
    fun `a bouquet opens its channels, and a channel shows its days beside them`() {
        val hub = hub()
        hub.openGroup(france)
        assertEquals(HubStage.Browse(france), hub.stage.value)
        hub.openChannel(channel)
        assertEquals(HubStage.Browse(france, channel), hub.stage.value)
    }

    @Test
    fun `another bouquet lets the chosen channel go`() {
        val hub = hub()
        hub.openChannel(channel)
        hub.openGroup(france)
        assertEquals(HubStage.Browse(france, null), hub.stage.value)
    }

    @Test
    fun `a day opens the chosen channel's programmes, and nothing with no channel chosen`() {
        val hub = hub()
        hub.openDay()
        assertEquals(HubStage.Browse(), hub.stage.value)

        hub.openGroup(france)
        hub.openChannel(channel)
        hub.openDay()
        assertEquals(HubStage.Programmes(france, channel), hub.stage.value)
    }

    @Test
    fun `another channel beside the programmes opens its programmes, and back returns to the columns`() {
        val hub = hub()
        val other = channel.copy(id = "c2", title = "Two")
        hub.openGroup(france)
        hub.openChannel(channel)
        hub.openDay()
        hub.openChannel(other)
        assertEquals(HubStage.Programmes(france, other), hub.stage.value)

        assertTrue(hub.back())
        assertEquals(HubStage.Browse(france, other), hub.stage.value)
    }

    @Test
    fun `back on the columns with nothing typed is not handled, so the caller leaves Catch-up`() {
        val hub = hub()
        hub.openGroup(france)
        assertFalse(hub.back())
    }

    @Test
    fun `leaving Catch-up from any place makes the next visit begin at all`() {
        val hub = hub()
        hub.openGroup(france)
        hub.openChannel(channel)
        hub.openDay()
        hub.reset()
        assertEquals(HubStage.Browse(), hub.stage.value)
    }

    // ------------------------------------------------------------------------------------ the search

    @Test
    fun `what is typed is what the box holds, as typed`() {
        val hub = hub()
        hub.search("  SPort ")
        assertEquals("  SPort ", hub.searchText.value)
        hub.searchGroups(" fr ")
        assertEquals(" fr ", hub.groupSearchText.value)
    }

    @Test
    fun `back closes the channels' search, then the bouquets', before it leaves`() {
        val hub = hub()
        hub.searchGroups("fr")
        hub.search("tf")
        assertTrue(hub.back())
        assertEquals("", hub.searchText.value)
        assertEquals("fr", hub.groupSearchText.value)
        assertTrue(hub.back())
        assertEquals("", hub.groupSearchText.value)
        assertFalse(hub.back())
    }

    @Test
    fun `another bouquet starts with nothing typed among the channels`() {
        val hub = hub()
        hub.search("tf")
        hub.openGroup(france)
        assertEquals("", hub.searchText.value)
    }

    @Test
    fun `leaving Catch-up clears both searches as it clears the place`() {
        val hub = hub()
        hub.search("sport")
        hub.searchGroups("fr")
        hub.reset()
        assertEquals("", hub.searchText.value)
        assertEquals("", hub.groupSearchText.value)
    }

    // ---------------------------------------------------------------------------------- the counts

    @Test
    fun `the first page counts every channel that keeps an archive, whichever category holds it`() {
        val groups = HubGroups(listOf(france, ArchiveGroup("g2", "Sport", 5), ArchiveGroup("g3", "News", 0)))
        assertEquals(17, groups.total)
        assertEquals(0, HubGroups(emptyList()).total)
    }
}
