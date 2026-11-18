package com.castivio.core.platform

import java.util.Locale

/**
 * What the smoothness panel says, kept by name: each screen, and each action timed on its own -- a bouquet switched, a search
 * answered. Pure, so the rules are tested without a device; [Smoothness] feeds it from the display's clock.
 *
 *  - **Opens**: how long a screen, or an action, took to its first frame, as the median and the slowest of what was timed.
 *  - **Frames**: the main thread's frames while it was on screen, how many came late -- more than [lateAfterNanos] after the
 *    one before, a stall the eye sees -- and the longest wait between two.
 */
class SmoothnessBook(private val lateAfterNanos: Long = FrameGaps.LATE_AFTER_NANOS) {

    /** One line of the panel. */
    data class Row(
        val name: String,
        val opens: Int,
        val openMedianMs: Long?,
        val openSlowestMs: Long?,
        val frames: Int,
        val late: Int,
        val longestMs: Long,
    ) {
        /** Of the frames, the share that came late, in percent with one decimal; null with no frame. */
        val latePercent: Double? get() = if (frames == 0) null else late * 100.0 / frames

        fun describe(): String = buildString {
            append(name)
            if (opens > 0) append(String.format(Locale.ROOT, " · opens %d · %d/%d ms", opens, openMedianMs, openSlowestMs))
            if (frames > 0) {
                append(String.format(Locale.ROOT, " · frames %,d · late %d (%.1f%%) · longest %d ms", frames, late, latePercent, longestMs))
            }
        }
    }

    private class Tally {
        val opens = ArrayList<Long>()
        var frames = 0
        var late = 0
        var longest = 0L
    }

    private val tallies = LinkedHashMap<String, Tally>()
    private var current: String? = null
    private var lastFrame = -1L

    /** The screen on display from now on; its frames are its own. */
    @Synchronized
    fun showing(name: String) {
        current = name
        tallies.getOrPut(name) { Tally() }
        // The gap across the change belongs to neither screen: the next frame starts a new count.
        lastFrame = -1L
    }

    /** A vsync of the main thread, at [frameNanos]. */
    @Synchronized
    fun frame(frameNanos: Long) {
        val name = current ?: return
        val last = lastFrame
        lastFrame = frameNanos
        if (last < 0 || frameNanos <= last) return
        val gap = frameNanos - last
        val tally = tallies.getOrPut(name) { Tally() }
        tally.frames++
        if (gap > lateAfterNanos) tally.late++
        if (gap > tally.longest) tally.longest = gap
    }

    /** [name] -- a screen or an action -- took [ms] to its first frame. */
    @Synchronized
    fun opened(name: String, ms: Long) {
        if (ms < 0) return
        val tally = tallies.getOrPut(name) { Tally() }
        tally.opens += ms
        if (tally.opens.size > KEEP) tally.opens.removeAt(0)
    }

    @Synchronized
    fun rows(): List<Row> = tallies.map { (name, t) ->
        val sorted = t.opens.sorted()
        Row(
            name = name,
            opens = t.opens.size,
            openMedianMs = sorted.getOrNull(sorted.size / 2),
            openSlowestMs = sorted.lastOrNull(),
            frames = t.frames,
            late = t.late,
            longestMs = t.longest / NANOS_PER_MS,
        )
    }

    /** Starts over, the screen on display included. */
    @Synchronized
    fun reset() {
        val name = current
        tallies.clear()
        lastFrame = -1L
        if (name != null) tallies[name] = Tally()
    }

    companion object {
        /** The openings kept for each name: the latest, so the panel says how the application is now. */
        const val KEEP = 50
        private const val NANOS_PER_MS = 1_000_000L
    }
}
