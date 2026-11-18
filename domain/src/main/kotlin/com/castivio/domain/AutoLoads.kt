package com.castivio.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The films, the series and the radio brought onto the device by themselves, so that a viewer who opens them finds them
 * there (Settings → Library, on by default; the owner's decision of 2026-11-18).
 *
 * ## The rules, each a line of [decide]
 *
 *  - **After the channels.** Nothing is asked for before Live TV is on the device: it is what a viewer opens first.
 *  - **One at a time, in [ORDER]**, and only when the queue has nothing else to do: a section the viewer asked for is never
 *    kept waiting behind one nobody asked for.
 *  - **Never while anything plays** -- "لا اريد أي تقطيع". A load running when a channel, a film or a station starts is
 *    stopped at once ([SectionLoads.stopBackground]) and asked for again once the player has closed. [playing] is
 *    [PlaybackActivity.active], which the shell keeps told from the player's pulse and the radio's station.
 *  - **Only on a network that costs nothing to use**: Wi-Fi or a cable, not a phone's data.
 *  - **A section that failed is left** until the viewer asks again, as every failure is ([SectionLoads], rule 3).
 *
 * Pure: the network is a question it is given ([unmetered]), asked again every [RECHECK_MS] as well as whenever anything else
 * changes, because a network that changes says nothing to a flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoLoads(
    private val scope: CoroutineScope,
    private val loads: SectionLoads,
    private val activeSource: Flow<String?>,
    private val marks: SectionCatalogue,
    private val enabled: Flow<Boolean>,
    private val playing: Flow<Boolean>,
    private val unmetered: () -> Boolean,
) {
    private val started = AtomicBoolean(false)

    /** Starts watching, once for the process; asking again does nothing. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        val loaded = activeSource.flatMapLatest { id ->
            if (id == null) flowOf(null) else marks.loaded(id).map { it.keys }
        }
        val ticks = flow {
            while (true) {
                emit(Unit)
                delay(RECHECK_MS)
            }
        }
        scope.launch {
            combine(enabled, playing, loaded, loads.runs, ticks) { on, busy, onDevice, runs, _ ->
                decide(on && unmetered(), busy, onDevice, runs)
            }.collect { step ->
                when (step) {
                    Step.Stop -> loads.stopBackground()
                    is Step.Load -> loads.request(step.kind, first = true, background = true)
                    Step.Wait -> Unit
                }
            }
        }
    }

    /** What to do now. */
    sealed interface Step {
        data object Wait : Step
        data object Stop : Step
        data class Load(val kind: MediaKind) : Step
    }

    companion object {
        /** The sections brought in by themselves, in this order: the films first, as the most opened after the channels. */
        val ORDER = listOf(MediaKind.MOVIE, MediaKind.SERIES, MediaKind.RADIO)

        /** How often the network is asked again when nothing else has changed. */
        const val RECHECK_MS = 60_000L

        /**
         * The rule, given the facts: whether loading on its own is allowed at all ([allowed]: the switch is on and the
         * network costs nothing), whether anything plays, the sections on the device -- null while the subscription is not
         * known -- and the queue's answers.
         */
        fun decide(allowed: Boolean, playing: Boolean, onDevice: Set<MediaKind>?, runs: Map<MediaKind, SectionRun>): Step {
            if (!allowed || playing) return Step.Stop
            if (onDevice == null || MediaKind.LIVE !in onDevice) return Step.Wait
            if (runs.values.any { it.load is SectionLoad.Queued || it.load is SectionLoad.Loading }) return Step.Wait
            val next = ORDER.firstOrNull { it !in onDevice && it !in runs } ?: return Step.Wait
            return Step.Load(next)
        }
    }
}

/** Whether the films, the series and the radio are brought in by themselves (Settings → Library). On until turned off. */
interface AutoLoadSetting {
    val enabled: Flow<Boolean>
    fun set(on: Boolean)
}
