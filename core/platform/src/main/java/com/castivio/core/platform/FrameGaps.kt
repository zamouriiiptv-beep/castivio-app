package com.castivio.core.platform

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import java.util.Locale
import kotlin.math.roundToLong

/**
 * The main thread's frames while a section loads, counted for the development strip: how many it drew, how many came late,
 * and the longest wait between two -- the answer to "does a load that runs by itself make the channel list stutter".
 *
 * A frame is late when it comes more than [lateAfterNanos] after the one before it: longer than one frame and a half at
 * 60 Hz, so a screen that is merely idle between vsyncs is not counted and a stall the eye sees is. Fed the vsync times the
 * platform gives ([FrameWatch]); pure, so the rule is tested without a device.
 */
class FrameGaps(private val lateAfterNanos: Long = LATE_AFTER_NANOS) {
    private var last = -1L
    private var frames = 0
    private var late = 0
    private var worst = 0L

    @Synchronized
    fun record(frameNanos: Long) {
        if (last >= 0 && frameNanos > last) {
            val gap = frameNanos - last
            frames++
            if (gap > lateAfterNanos) late++
            if (gap > worst) worst = gap
        }
        last = frameNanos
    }

    /** "frames 1,204 · late 3 (over 25 ms) · longest 41 ms", or null before two frames. */
    @Synchronized
    fun describe(): String? {
        if (frames == 0) return null
        val ms = (worst / NANOS_PER_MS.toDouble()).roundToLong()
        return String.format(Locale.ROOT, "frames %,d · late %d (over %d ms) · longest %d ms", frames, late, lateAfterNanos / NANOS_PER_MS, ms)
    }

    companion object {
        const val LATE_AFTER_NANOS = 25_000_000L
        private const val NANOS_PER_MS = 1_000_000L
    }
}

/**
 * Asks the platform for every vsync on the main thread from [start] to [stop], and hands each to a [FrameGaps]. A
 * development build's measurement only: it keeps the display's clock ticking while it runs.
 */
class FrameWatch private constructor() {
    private val gaps = FrameGaps()
    @Volatile private var running = true
    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            gaps.record(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Stops listening, and says what it counted. */
    fun stop(): String? {
        running = false
        return gaps.describe()
    }

    companion object {
        /** Starts listening, from the main thread whatever thread asks. */
        fun start(): FrameWatch = FrameWatch().also { watch ->
            Handler(Looper.getMainLooper()).post { if (watch.running) Choreographer.getInstance().postFrameCallback(watch.callback) }
        }
    }
}
