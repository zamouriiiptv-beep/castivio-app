package com.castivio.core.platform

/**
 * Whether this build draws the measurement panels: a debuggable build always, and otherwise what its manifest says, read once.
 *
 * The manifest is asked through the package manager, which is a call across processes; the answer cannot change while the
 * process lives, so it is asked the first time and kept. A debuggable build never asks. Plain Kotlin, so the rule is tested
 * without a device; [PerformanceLog.isVisibleIn] gives it the platform's two answers.
 */
internal class MeasureFlag(private val readManifest: () -> Boolean) {

    @Volatile private var said: Boolean? = null

    fun isOn(debuggable: Boolean): Boolean {
        if (debuggable) return true
        said?.let { return it }
        // A manifest that cannot be read is a build that does not measure.
        val read = runCatching(readManifest).getOrDefault(false)
        said = read
        return read
    }
}
