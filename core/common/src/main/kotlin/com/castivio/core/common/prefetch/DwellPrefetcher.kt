package com.castivio.core.common.prefetch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Asks for a title's details while the remote rests on its card, before it is pressed.
 *
 * A provider can take ten seconds to build a show's details the first time it is asked (measured: `ask 9.8 s · read 0.0 s`,
 * the line already fast once the reply came), and a viewer on a television rests on a poster for a moment before pressing it.
 * So when the focus has stayed on one card for [dwellMs], that card's details are asked for, and the page that opens finds
 * them already kept -- or the provider already at work on them.
 *
 * Gentle on a provider that is slow: **one request at a time**. A card the focus rests on while one is out waits its turn,
 * and only the latest such card -- the one the viewer is on -- is kept waiting; the ones passed over are forgotten. A card
 * whose details were asked once is not asked again. A focus that moves on before [dwellMs] asks for nothing. A request
 * already sent is let finish: the provider has begun building the answer, and the answer is kept.
 *
 * [fetch] does the asking and keeping; this decides only when. Pure apart from the [scope] it runs in.
 */
class DwellPrefetcher(
    private val scope: CoroutineScope,
    private val dwellMs: Long = DWELL_MS,
    private val fetch: suspend (String) -> Unit,
) {
    private var dwell: Job? = null
    private var dwellOn: String? = null
    private var inFlight: String? = null
    private var waiting: String? = null
    private val asked = HashSet<String>()

    /** The focus has come to the card of [id]. */
    fun focused(id: String) {
        dwell?.cancel()
        dwellOn = null
        if (id in asked) return
        dwellOn = id
        dwell = scope.launch {
            delay(dwellMs)
            if (id in asked) return@launch
            if (inFlight != null) waiting = id else start(id)
        }
    }

    /**
     * The focus has left the card of [id]: a wait for it ends, and a turn it was waiting for is given up. Said of any other
     * card -- one drawn without the focus as the wall scrolls -- it changes nothing.
     */
    fun left(id: String) {
        if (dwellOn == id) {
            dwell?.cancel()
            dwellOn = null
        }
        if (waiting == id) waiting = null
    }

    private fun start(id: String) {
        asked += id
        inFlight = id
        scope.launch {
            try {
                fetch(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A provider that could not answer ahead of the press is asked again by the page, as it always was.
            } finally {
                inFlight = null
                val next = waiting
                waiting = null
                if (next != null && next !in asked) start(next)
            }
        }
    }

    companion object {
        /** How long the focus rests on a card before it is taken as interest: past a quick pass, short of a press. */
        const val DWELL_MS = 600L
    }
}
