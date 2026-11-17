package com.castivio.core.common.artwork

import java.util.Locale

/** Where a picture a page drew came from: the loader's memory, the device's disk, or the network. */
enum class ArtworkSource { MEMORY, DISK, NETWORK }

/**
 * One picture of a title's page, as it came to the screen: how long after the page asked for it, from where, and at what size
 * it was decoded. What a development build prints of the page's large poster and its wide picture, so that a slow one can be
 * told from a quick one before anything is changed about either.
 */
data class ArtworkShown(
    val millis: Long,
    val source: ArtworkSource,
    val width: Int,
    val height: Int,
    /** The monotonic clock when it came, in milliseconds: what a page counts it from its press with ([since]). */
    val shownAt: Long = 0L,
) {
    /** The same picture, its time counted from [startMillis] -- a press -- instead of from the page asking for it. */
    fun since(startMillis: Long): ArtworkShown = copy(millis = shownAt - startMillis)
}

/**
 * The line a title's page prints of its two pictures, in a development build: `poster 0.42 s net 500x750 · backdrop 0.88 s disk
 * 1280x720`. A picture still on its way is `poster …`; one the page does not draw is left out. Null when there is nothing to say.
 *
 * No address is in it: the time, the source and the size. Western digits always. Pure, so a test holds the words.
 */
object ArtworkShownLine {

    fun describe(poster: ArtworkShown?, posterAsked: Boolean, backdrop: ArtworkShown?, backdropAsked: Boolean): String? =
        listOfNotNull(part("poster", poster, posterAsked), part("backdrop", backdrop, backdropAsked))
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")

    private fun part(name: String, shown: ArtworkShown?, asked: Boolean): String? = when {
        shown != null -> String.format(
            Locale.ROOT,
            "%s %.2f s %s %dx%d",
            name,
            shown.millis.coerceAtLeast(0L) / MILLIS_PER_S,
            word(shown.source),
            shown.width,
            shown.height,
        )
        asked -> "$name …"
        else -> null
    }

    private fun word(source: ArtworkSource): String = when (source) {
        ArtworkSource.MEMORY -> "mem"
        ArtworkSource.DISK -> "disk"
        ArtworkSource.NETWORK -> "net"
    }

    private const val MILLIS_PER_S = 1000.0
}
