package com.castivio.data.database

import android.os.Process

/**
 * The importer's row writer is given a little more of the processor while it writes a batch, and its thread is given back as it
 * was.
 *
 * Measured at 20:09, a films load with the viewer scrolling about 780 cards: the process spent 257 s of CPU over 76 s, the main
 * thread 52.5 s and the pool 119 s decoding and drawing posters, and the writer, at the same nice as all of them, **waited 12.15 s
 * for a processor** in 54.46 s of writing -- against 5.55 s in 35.59 s when the viewer scrolled a sixth as much. The reader, with
 * the reply in hand, was held up by the writer for 14.41 s, and the load took 79 s where it had taken 50. The rows are what the
 * viewer is waiting for; a poster's decode can wait a little longer for its turn.
 *
 * [WRITER_NICE] is two steps above the default: roughly half again the share of a default thread when they contend, which is
 * every pool thread, the main thread included. It stays under the display's priority, so the render thread still comes first and
 * a frame is not given up for a row. The batch runs on whichever pool thread the dispatcher hands it, so the priority is taken for
 * the batch and given back in a `finally`: a pool thread is not left raised for the next task it runs, and a thread already above
 * [WRITER_NICE] is left alone. A platform that refuses the change costs nothing but the change.
 *
 * Pure apart from the two calls it is given, so a test holds the rule.
 */
internal class WriterPriority(
    private val currentNice: () -> Int,
    private val setNice: (Int) -> Unit,
) {

    /** Runs [block] at [WRITER_NICE] at least, and puts the thread back as it found it. */
    fun <T> during(block: () -> T): T {
        val before = runCatching(currentNice).getOrNull()
        val raised = before != null && before > WRITER_NICE && runCatching { setNice(WRITER_NICE) }.isSuccess
        try {
            return block()
        } finally {
            if (raised && before != null) runCatching { setNice(before) }
        }
    }

    companion object {
        /** Two steps more favourable than [Process.THREAD_PRIORITY_DEFAULT], and short of [Process.THREAD_PRIORITY_DISPLAY]. */
        const val WRITER_NICE = -2

        /** For the thread that calls it. */
        fun ofThisThread(): WriterPriority = WriterPriority(
            currentNice = { Process.getThreadPriority(Process.myTid()) },
            setNice = { Process.setThreadPriority(it) },
        )
    }
}
