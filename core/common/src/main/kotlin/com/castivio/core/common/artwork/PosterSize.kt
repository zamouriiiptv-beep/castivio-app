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

    /** TMDB's width for a title's page's one large poster: sharp at a television's poster height, a third of an original. */
    const val PAGE_SIZE = "w780"

    private const val PAGE_WIDTH = 780

    /**
     * [url] as a title's page should ask for its large poster: `w780` in place of the provider's `original` or anything wider,
     * by the same rule as [forCard]; `w600_and_h900_bestv2` and the like are already under it and are left.
     */
    fun forPage(url: String): String = atMost(url, PAGE_SIZE, PAGE_WIDTH)

    /**
     * TMDB's width for a wide picture behind a title's page: small on purpose. Stretched across the screen it is soft on every
     * device -- a blur effect does nothing under Android 12, which is most television boxes -- and it is a tenth of a w1280.
     */
    const val SOFT_SIZE = "w300"

    private const val SOFT_WIDTH = 300

    /** [url] as a title's page asks for the wide picture behind it: `w300`, by the same rule as [forCard]. */
    fun forSoftBackdrop(url: String): String = atMost(url, SOFT_SIZE, SOFT_WIDTH)

    /** TMDB's width for a picture that fills a screen: a wide picture behind a title's page, dimmed to a third. */
    const val SCREEN_SIZE = "w1280"

    private const val SCREEN_WIDTH = 1280

    /** [url] as the card should ask for it. */
    fun forCard(url: String): String = atMost(url, CARD_SIZE, CARD_WIDTH)

    /**
     * [url] as a title's page should ask for its wide picture: `w1280` in place of an `original` of several megabytes, by the
     * same rule as [forCard] -- anything not a TMDB picture address, or already at that width or under it, is left as it came.
     */
    fun forScreen(url: String): String = atMost(url, SCREEN_SIZE, SCREEN_WIDTH)

    /**
     * The size step of a TMDB picture address -- `original`, `w300`, `w600_and_h900_bestv2` -- or null for any other address.
     * What a development build prints of a page's pictures, to say whether a provider hands over originals.
     */
    fun sizeOf(url: String): String? {
        val hostAt = url.indexOf("://")
        if (hostAt < 0) return null
        val rest = hostAt + 3
        val pathAt = url.indexOf('/', rest)
        if (pathAt < 0 || !url.substring(rest, pathAt).equals(HOST, ignoreCase = true) || !url.startsWith(PATH, pathAt)) return null
        val sizeAt = pathAt + PATH.length
        val sizeEnd = url.indexOf('/', sizeAt)
        if (sizeEnd <= sizeAt) return null
        return url.substring(sizeAt, sizeEnd).takeIf(::isSize)
    }

    private fun atMost(url: String, sizeName: String, width: Int): String {
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
        if (widthOf(size)?.let { it <= width } == true) return url
        if (!isSize(size)) return url
        return url.substring(0, sizeAt) + sizeName + url.substring(sizeEnd)
    }

    /**
     * The width a size names: `w500` is 500, and a composed size of TMDB's own pages is its first width --
     * `w600_and_h900_bestv2` is 600 -- so a page that asks for at most 780 does not trade it for a larger one. Null for
     * `original`, a height alone, or anything else.
     */
    private fun widthOf(size: String): Int? {
        if (size.length < 2 || size[0] != 'w') return null
        val digits = size.drop(1).takeWhile { it.isDigit() }
        val rest = size.drop(1 + digits.length)
        if (digits.isEmpty() || (rest.isNotEmpty() && !rest.startsWith("_"))) return null
        return digits.toIntOrNull()
    }

    /**
     * A step that is a size and not some other path: `original`, `w<n>`, `h<n>`, or one of the composed sizes of TMDB's own
     * pages such as `w600_and_h900_bestv2`.
     */
    private fun isSize(size: String): Boolean =
        size == "original" || SIZE.matches(size)

    private val SIZE = Regex("[wh]\\d+(_and_[wh]\\d+)?(_[a-z0-9]+)?")
}
