package com.castivio.core.common.artwork

import java.util.Locale

/**
 * When a title's page was asked for: the press on a poster, a rail's card, a similar show. What a development build counts the
 * opening of the page from, so a page slow to appear is told from a page slow to fill.
 *
 * The press is marked by whoever opens the page ([press]); the page takes it once, as it is first drawn ([take]). A press that
 * is older than [STALE_MS], or none at all, is no press: the page was reached some other way, and counts from its own first
 * drawing instead. Times are the caller's monotonic clock in milliseconds.
 */
open class PageOpenClock {
    private val lock = Any()
    private var pressedAt = NONE

    /** A page has just been asked for, at [atMillis]. */
    fun press(atMillis: Long) {
        synchronized(lock) { pressedAt = atMillis }
    }

    /** The press that opened the page now being drawn at [nowMillis], once; null when there was none, or it is stale. */
    fun take(nowMillis: Long): Long? = synchronized(lock) {
        val at = pressedAt
        pressedAt = NONE
        at.takeIf { it != NONE && nowMillis - it in 0..STALE_MS }
    }

    companion object {
        /** A press this long before the page is drawn is not what opened it. */
        const val STALE_MS = 10_000L
        private const val NONE = Long.MIN_VALUE
    }
}

/** The application's one. */
object PageOpen : PageOpenClock()

/**
 * Everything a title's page measures of its own opening, in milliseconds **from the press** (or from the page's first drawing
 * when there was no press): the page on screen, its details from the provider, a show's episodes, and its two pictures.
 */
data class PageOpenTimes(
    /** From the press to the page's first frame; null when there was no press to count from. */
    val open: Long? = null,
    /** From the press to the details on screen; null while they have not come, or when the page asks for none. */
    val details: Long? = null,
    /** Whether the details are still being asked for. */
    val detailsPending: Boolean = false,
    /** A show's episodes: from the press to the first season's list on screen. */
    val episodes: Long? = null,
    val episodesPending: Boolean = false,
    val poster: ArtworkShown? = null,
    val posterAsked: Boolean = false,
    val backdrop: ArtworkShown? = null,
    val backdropAsked: Boolean = false,
)

/**
 * The line a development build prints of a page's opening:
 * `open 0.18 s · details 0.65 s · poster 0.42 s net 1000x1500 · backdrop 0.95 s net 1280x720 · episodes 1.20 s`.
 * A part still on its way is `details …`; one the page does not have is left out. Pure; Western digits.
 */
object PageOpenLine {

    fun describe(t: PageOpenTimes): String? {
        val parts = listOfNotNull(
            t.open?.let { "open ${secs(it)}" },
            timed("details", t.details, t.detailsPending),
            ArtworkShownLine.describe(t.poster, t.posterAsked, t.backdrop, t.backdropAsked),
            timed("episodes", t.episodes, t.episodesPending),
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun timed(name: String, millis: Long?, pending: Boolean): String? = when {
        millis != null -> "$name ${secs(millis)}"
        pending -> "$name …"
        else -> null
    }

    private fun secs(millis: Long): String = String.format(Locale.ROOT, "%.2f s", millis.coerceAtLeast(0L) / 1000.0)
}
