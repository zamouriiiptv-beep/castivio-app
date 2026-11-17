package com.castivio.core.common.artwork

/**
 * The address of a poster at the size a card draws it.
 *
 * A provider hands over its films' posters as addresses on TMDB's picture host, and it picks the size: a cold load measured
 * 159 of them at a median of 114.5 KB and up to 1.38 MB -- `w600_and_h900_bestv2` and `original` -- for a card a few hundred
 * pixels wide. Every byte past the card's size came over the connection the film list was arriving on, and was decoded on the
 * processor that was parsing and writing that list, before the picture could be drawn at all; and a picture drawn late on a
 * wall the viewer is scrolling is a card the viewer passes empty.
 *
 * TMDB serves every poster at every size it lists, the size being one step of the path
 * (`https://image.tmdb.org/t/p/<size>/<file>`), so the card asks for [CARD_SIZE] in its place. Anything else is left exactly
 * as it came: another host, a TMDB address of another shape, and one already at the card's size or smaller -- a smaller one
 * is what the provider chose, and enlarging it would be a second download of the same picture for nothing.
 *
 * Pure: an address in, an address out.
 */
object PosterSize {

    /** TMDB's width for a card: 342 pixels, its step between 185, which blurs on a phone's card, and 500. */
    const val CARD_SIZE = "w342"

    private const val CARD_WIDTH = 342

    private const val HOST = "image.tmdb.org"

    private const val PATH = "/t/p/"

    /** [url] as the card should ask for it. */
    fun forCard(url: String): String {
        val hostAt = url.indexOf("://")
        if (hostAt < 0) return url
        val scheme = url.substring(0, hostAt)
        if (!scheme.equals("https", ignoreCase = true) && !scheme.equals("http", ignoreCase = true)) return url
        val rest = hostAt + 3
        val pathAt = url.indexOf('/', rest)
        if (pathAt < 0) return url
        if (!url.substring(rest, pathAt).equals(HOST, ignoreCase = true)) return url
        if (!url.startsWith(PATH, pathAt)) return url
        val sizeAt = pathAt + PATH.length
        val sizeEnd = url.indexOf('/', sizeAt)
        // A size and then a file: "/t/p/w500/abc.jpg". Without a file after it there is nothing to ask for at another size.
        if (sizeEnd <= sizeAt || sizeEnd == url.length - 1) return url
        val size = url.substring(sizeAt, sizeEnd)
        if (widthOf(size)?.let { it <= CARD_WIDTH } == true) return url
        if (!isSize(size)) return url
        return url.substring(0, sizeAt) + CARD_SIZE + url.substring(sizeEnd)
    }

    /** The width of a plain `w<n>` size, or null for anything else. */
    private fun widthOf(size: String): Int? =
        if (size.length > 1 && size[0] == 'w' && size.drop(1).all { it.isDigit() }) size.drop(1).toIntOrNull() else null

    /**
     * A step that is a size and not some other path: `original`, `w<n>`, `h<n>`, or one of the composed sizes of TMDB's own
     * pages such as `w600_and_h900_bestv2`.
     */
    private fun isSize(size: String): Boolean =
        size == "original" || SIZE.matches(size)

    private val SIZE = Regex("[wh]\\d+(_and_[wh]\\d+)?(_[a-z0-9]+)?")
}
