package com.castivio.core.platform

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The smoothness panel's measurements, for a development or a measurement build only ([DevDiagnostics.enabled]): which
 * screen is on display and how long it took to come, the main thread's frames while it is, and actions timed on their own.
 * Off, every call returns at once and no frame callback is posted.
 *
 * The frames are the display's vsyncs as the main thread receives them, asked for every frame while the panels are on: a
 * gap longer than a frame and a half is the main thread having been busy -- a stutter a viewer sees -- whether it was
 * scrolling, moving between bouquets or opening a screen.
 */
object Smoothness {

    private val book = SmoothnessBook()
    private val _rows = MutableStateFlow<List<SmoothnessBook.Row>>(emptyList())

    /** The panel's lines, read again at most twice a second. */
    val rows: StateFlow<List<SmoothnessBook.Row>> = _rows.asStateFlow()

    @Volatile private var watching = false
    private var lastPublished = 0L

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!watching) return
            book.frame(frameTimeNanos)
            val now = SystemClock.uptimeMillis()
            if (now - lastPublished >= PUBLISH_MS) {
                lastPublished = now
                _rows.value = book.rows()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Starts listening to the frames, once, when the panels are on; from the main thread whatever thread asks. */
    fun start() {
        if (!DevDiagnostics.enabled || watching) return
        watching = true
        Handler(Looper.getMainLooper()).post { Choreographer.getInstance().postFrameCallback(callback) }
    }

    /** [name] is the screen on display from now on. */
    fun showing(name: String) {
        if (!DevDiagnostics.enabled) return
        start()
        book.showing(name)
    }

    /** [name] took [ms] to its first frame: a screen, or an action timed on its own. */
    fun opened(name: String, ms: Long) {
        if (!DevDiagnostics.enabled) return
        book.opened(name, ms)
        _rows.value = book.rows()
    }

    fun reset() {
        book.reset()
        _rows.value = book.rows()
    }

    private const val PUBLISH_MS = 500L
}
