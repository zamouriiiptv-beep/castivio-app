package com.castivio.core.common.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which poster addresses a card asks for at its own size, and which it leaves as they came. */
class PosterSizeTest {

    private fun card(url: String) = PosterSize.forCard(url)

    @Test
    fun `a large TMDB poster is asked for at the card's size`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w342/abc123.jpg",
            card("https://image.tmdb.org/t/p/w600_and_h900_bestv2/abc123.jpg"),
        )
        assertEquals("https://image.tmdb.org/t/p/w342/abc123.jpg", card("https://image.tmdb.org/t/p/original/abc123.jpg"))
        assertEquals("https://image.tmdb.org/t/p/w342/abc123.jpg", card("https://image.tmdb.org/t/p/w500/abc123.jpg"))
        assertEquals("https://image.tmdb.org/t/p/w342/abc123.jpg", card("https://image.tmdb.org/t/p/w780/abc123.jpg"))
        assertEquals("https://image.tmdb.org/t/p/w342/abc123.jpg", card("https://image.tmdb.org/t/p/h632/abc123.jpg"))
    }

    @Test
    fun `the scheme and the host's case are kept, and so is what follows the file`() {
        assertEquals("http://image.tmdb.org/t/p/w342/a.jpg", card("http://image.tmdb.org/t/p/original/a.jpg"))
        assertEquals("https://Image.TMDB.org/t/p/w342/a.jpg?x=1", card("https://Image.TMDB.org/t/p/w500/a.jpg?x=1"))
    }

    @Test
    fun `a poster already at the card's size or smaller is left as it came`() {
        for (size in listOf("w342", "w185", "w154", "w92")) {
            val url = "https://image.tmdb.org/t/p/$size/abc123.jpg"
            assertEquals(url, card(url))
        }
    }

    @Test
    fun `another host or another shape of address is left as it came`() {
        val untouched = listOf(
            "https://provider.example/images/w600_and_h900_bestv2/abc.jpg",
            "https://image.tmdb.org.evil.example/t/p/original/abc.jpg",
            "https://cdn.example/image.tmdb.org/t/p/original/abc.jpg",
            "https://image.tmdb.org/3/movie/550",
            "https://image.tmdb.org/t/p/original/",
            "https://image.tmdb.org/t/p/original",
            "https://image.tmdb.org/t/p/not-a-size/abc.jpg",
            "https://image.tmdb.org",
            "ftp://image.tmdb.org/t/p/original/abc.jpg",
            "image.tmdb.org/t/p/original/abc.jpg",
            "",
        )
        for (url in untouched) assertEquals(url, url, card(url))
    }
}
