package com.castivio.core.common.artwork

/**
 * Which posters below a wall are being fetched ahead of the cards, as the wall moves -- **while it scrolls**, and not only once it
 * is still.
 *
 * A viewer scrolling a wall of films is the case that matters, and it was the case left out: the posters ahead were asked for
 * only after the wall had been still for a moment, and everything on its way was cancelled the instant a scroll began. Every card
 * that scrolled in then asked for its own poster as it arrived, and while a section's first load was running each of those asks
 * waited out a connection busy with the film list: a poster's first byte took 0.72 s (median), 2.10 s (p90), against 0.16 s once
 * the list had come. A poster asked for a row or two before its card arrives has that time to come.
 *
 * The window is the [ahead] items after the last one on screen. As it moves, the posters that came into it are asked for and the
 * ones that left it are let go: one that scrolled **onto** the screen is its card's to fetch now, and one the viewer scrolled back
 * past is not wanted. So what is in flight is never more than the window, however fast the thumb goes, and a card on screen never
 * waits behind posters for cards that are not.
 *
 * Pure: positions in, positions out. The caller holds the requests.
 */
object PrefetchWindow {

    /** What to change, given the positions being fetched now. */
    class Plan(
        /** Positions newly in the window, in order, to ask for. */
        val ask: List<Int>,
        /** Positions being fetched that are out of the window, to let go. */
        val drop: List<Int>,
    )

    /**
     * The [ahead] positions after [lastShown], none past the end of a list of [count], and none at all until something is on
     * screen to measure from ([lastShown] is -1).
     */
    fun window(lastShown: Int, ahead: Int, count: Int): IntRange =
        if (lastShown < 0 || ahead <= 0 || count <= 0) IntRange.EMPTY
        else (lastShown + 1)..minOf(lastShown + ahead, count - 1)

    /** What to ask for and what to let go, for a wall whose last card on screen is [lastShown], with [pending] being fetched. */
    fun plan(pending: Set<Int>, lastShown: Int, ahead: Int, count: Int): Plan {
        val target = window(lastShown, ahead, count)
        return Plan(
            ask = target.filter { it !in pending },
            drop = pending.filter { it !in target }.sorted(),
        )
    }
}
