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
    /** A show's episode pictures, the open season's: see [StillsTimes]. */
    val stills: StillsTimes? = null,
)

/**
 * The episode pictures of a show's open season, for the page's line: how many there are to show, how many have come and when
 * (each in milliseconds from the press), how many episodes have no picture at all, and the sizes their addresses ask TMDB for --
 * `original` is a provider handing over full pictures for a frame a hundred pixels wide.
 */
data class StillsTimes(
    /** Episodes with a picture address. */
    val asked: Int,
    /** Episodes with none: the frame keeps its colours. */
    val withoutPicture: Int,
    /** The pictures that came, each counted from the press. */
    val shown: List<ArtworkShown>,
    /** How many of the addresses are TMDB's, by the size step they ask for (`original` 8, `w300` 2); others not counted. */
    val tmdbSizes: Map<String, Int>,
    /** Addresses that are not TMDB's. */
    val otherHosts: Int,
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
            t.stills?.let(::stills),
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /**
     * `stills 6/10 · 0.80/1.20/2.00 s · net · tmdb original 10 · 2 without`: shown of those with a picture, the first, median and
     * last to come, where the first came from, the sizes the addresses ask for, and the episodes with no picture.
     */
    private fun stills(s: StillsTimes): String? {
        if (s.asked == 0 && s.withoutPicture == 0) return null
        val times = s.shown.map { it.millis }.sorted()
        val parts = mutableListOf("stills ${s.shown.size}/${s.asked}")
        if (times.isNotEmpty()) {
            parts += "${plain(times.first())}/${plain(times[(times.size - 1) / 2])}/${plain(times.last())} s"
            parts += s.shown.minByOrNull { it.millis }!!.let { "${word(it.source)} ${it.width}x${it.height}" }
        }
        if (s.tmdbSizes.isNotEmpty()) {
            parts += "tmdb " + s.tmdbSizes.entries.sortedByDescending { it.value }.joinToString(" ") { "${it.key} ${it.value}" }
        }
        if (s.otherHosts > 0) parts += "other ${s.otherHosts}"
        if (s.withoutPicture > 0) parts += "${s.withoutPicture} without"
        return parts.joinToString(" · ")
    }

    private fun word(source: ArtworkSource): String = when (source) {
        ArtworkSource.MEMORY -> "mem"
        ArtworkSource.DISK -> "disk"
        ArtworkSource.NETWORK -> "net"
    }

    private fun plain(millis: Long): String = String.format(Locale.ROOT, "%.2f", millis.coerceAtLeast(0L) / 1000.0)

    private fun timed(name: String, millis: Long?, pending: Boolean): String? = when {
        millis != null -> "$name ${secs(millis)}"
        pending -> "$name …"
        else -> null
    }

    private fun secs(millis: Long): String = String.format(Locale.ROOT, "%.2f s", millis.coerceAtLeast(0L) / 1000.0)
}
