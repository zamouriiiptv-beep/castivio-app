package com.castivio.feature.home

import com.castivio.domain.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which empty "continue" slots say "not downloaded yet": the same rule the large cards above them use. */
class ContinueSlotsTest {

    @Test
    fun `nothing fetched and nothing counted is every section unfetched`() {
        assertEquals(
            setOf(MediaKind.LIVE, MediaKind.MOVIE, MediaKind.SERIES, MediaKind.RADIO),
            unfetchedKinds(HomeState()),
        )
    }

    @Test
    fun `a section with rows is fetched even without a mark`() {
        val state = HomeState(liveCount = 23_000, movieCount = 5)
        assertEquals(setOf(MediaKind.SERIES, MediaKind.RADIO), unfetchedKinds(state))
    }

    @Test
    fun `a section fetched empty is not unfetched`() {
        val state = HomeState(liveCount = 10, sections = mapOf(MediaKind.RADIO to 1L))
        assertEquals(setOf(MediaKind.MOVIE, MediaKind.SERIES), unfetchedKinds(state))
    }
}
