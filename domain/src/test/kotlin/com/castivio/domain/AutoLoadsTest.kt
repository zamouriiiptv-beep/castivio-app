package com.castivio.domain

import com.castivio.core.common.AppError
import com.castivio.domain.AutoLoads.Companion.decide
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When the films, the series and the radio are brought in by themselves, and when they give way. */
class AutoLoadsTest {

    private val live = setOf(MediaKind.LIVE)
    private fun run(load: SectionLoad) = SectionRun(load, forced = false, elapsedMs = 0)

    @Test
    fun `after the channels, the films first, then the series, then the radio`() {
        assertEquals(AutoLoads.Step.Load(MediaKind.MOVIE), decide(true, false, live, emptyMap()))
        assertEquals(AutoLoads.Step.Load(MediaKind.SERIES), decide(true, false, live + MediaKind.MOVIE, emptyMap()))
        assertEquals(
            AutoLoads.Step.Load(MediaKind.RADIO),
            decide(true, false, live + MediaKind.MOVIE + MediaKind.SERIES, emptyMap()),
        )
        assertEquals(AutoLoads.Step.Wait, decide(true, false, live + AutoLoads.ORDER, emptyMap()))
    }

    @Test
    fun `nothing before the channels, or before the subscription is known`() {
        assertEquals(AutoLoads.Step.Wait, decide(true, false, emptySet(), emptyMap()))
        assertEquals(AutoLoads.Step.Wait, decide(true, false, null, emptyMap()))
    }

    @Test
    fun `anything playing stops it, and so does a network that costs or the switch turned off`() {
        assertEquals(AutoLoads.Step.Stop, decide(true, true, live, emptyMap()))
        assertEquals(AutoLoads.Step.Stop, decide(false, false, live, emptyMap()))
    }

    @Test
    fun `a section the viewer is loading is not queued behind, and none is asked for while one arrives`() {
        val arriving = mapOf(MediaKind.SERIES to run(SectionLoad.Loading(10, 1)))
        assertEquals(AutoLoads.Step.Wait, decide(true, false, live, arriving))
        val waiting = mapOf(MediaKind.RADIO to run(SectionLoad.Queued))
        assertEquals(AutoLoads.Step.Wait, decide(true, false, live, waiting))
    }

    @Test
    fun `a section that failed is left until the viewer asks again`() {
        val failed = mapOf(MediaKind.MOVIE to run(SectionLoad.Failed(AppError.NETWORK_UNAVAILABLE, retryable = true)))
        assertEquals(AutoLoads.Step.Load(MediaKind.SERIES), decide(true, false, live, failed))
    }

    @Test
    fun `playing stops the films arriving on their own, and they are asked for again once it ends`() = runTest {
        val asked = mutableListOf<MediaKind>()
        val loads = SectionLoads(
            backgroundScope,
            MutableStateFlow("xtream"),
            { kind, _ ->
                asked += kind
                flow { emit(SectionLoad.Loading(1, 1)); awaitCancellation() }
            },
            monotonicMs = { 0L },
        )
        val playing = MutableStateFlow(false)
        val marks = object : SectionCatalogue {
            override suspend fun loadedAt(sourceId: String, kind: MediaKind): Long? = null
            override fun loaded(sourceId: String): Flow<Map<MediaKind, Long>> = flowOf(mapOf(MediaKind.LIVE to 1L))
            override suspend fun markLoaded(sourceId: String, kind: MediaKind, atMs: Long) = Unit
            override suspend fun forget(sourceId: String) = Unit
        }
        AutoLoads(
            backgroundScope, loads, MutableStateFlow("xtream"), marks,
            enabled = flowOf(true), playing = playing, unmetered = { true },
        ).start()
        runCurrent()
        assertEquals(listOf(MediaKind.MOVIE), asked)

        playing.value = true
        runCurrent()
        assertNull("stopped the moment a channel played", loads.runs.value[MediaKind.MOVIE])

        playing.value = false
        runCurrent()
        assertEquals("asked for again once the player closed", listOf(MediaKind.MOVIE, MediaKind.MOVIE), asked)
    }

    @Test
    fun `on a network that costs, nothing is asked for`() = runTest {
        val asked = mutableListOf<MediaKind>()
        val loads = SectionLoads(backgroundScope, MutableStateFlow("x"), { kind, _ -> asked += kind; flowOf(SectionLoad.Done(1)) }, { 0L })
        val marks = object : SectionCatalogue {
            override suspend fun loadedAt(sourceId: String, kind: MediaKind): Long? = null
            override fun loaded(sourceId: String) = MutableStateFlow(mapOf(MediaKind.LIVE to 1L)).map { it }
            override suspend fun markLoaded(sourceId: String, kind: MediaKind, atMs: Long) = Unit
            override suspend fun forget(sourceId: String) = Unit
        }
        AutoLoads(backgroundScope, loads, MutableStateFlow("x"), marks, flowOf(true), flowOf(false), unmetered = { false }).start()
        runCurrent()

        assertEquals(emptyList<MediaKind>(), asked)
    }
}
