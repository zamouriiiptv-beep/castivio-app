package com.castivio.core.common.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the strip says of the posters, from a load made up for the purpose and worked out by hand.
 *
 * The load: the provider's reply of 78 MB arrives from 1.90 s to 52.40 s after the press; one poster is asked before it, five
 * during it and one after, each with times chosen so that every figure below can be checked on paper.
 */
class PosterTimelineReportTest {

    private var now = 0L
    private val recorder = PosterTimelineRecorder(clock = { now })

    private fun at(ms: Long) {
        now = ms * NANOS_PER_MS
    }

    private fun ms(count: Long) = count * NANOS_PER_MS

    /** A call begun at [begun], to be marked by [PictureCall.on]. */
    private fun call(host: ArtworkHost, begun: Long, picture: Boolean = true): PictureCall {
        at(begun)
        return recorder.callStarted(host, picture)!!
    }

    private fun PictureCall.on(ms: Long, mark: PictureCall.() -> Unit) {
        at(ms)
        mark()
    }

    private fun reply(startedMs: Long, endedMs: Long?, megabytes: Long = 78L) = ReplySpan(
        bytes = megabytes * BYTES_PER_MB,
        startedAt = ms(startedMs),
        lastByteAt = ms(endedMs ?: (startedMs + 1)),
        endedAt = endedMs?.let { ms(it) },
    )

    private fun describe(reply: ReplySpan?, nowMs: Long = 65_000L): List<String> =
        PosterTimelineReport.describe(recorder.snapshot(), nowAt = ms(nowMs), reply = reply)

    /** The load described in the class's comment, with twenty-one cards of which twenty have their picture. */
    private fun coldLoad() {
        recorder.begin(originAt = 0L)

        // Before the reply: one poster from another host, on a connection from the pool.
        call(ArtworkHost.OTHER, 1_000).apply {
            on(1_050) { acquired("http/1.1", 10) }
            on(1_051) { requestSent() }
            on(1_151) { firstByte() }
            on(1_152) { headersEnded(200) }
            on(1_152) { bodyStarted() }
            on(1_162) { bodyEnded(10_000L) }
            on(1_163) { ended(ArtworkOutcome.OK) }
        }

        // During it: the provider's, which waits six seconds for the host to begin to answer ...
        call(ArtworkHost.PROVIDER, 3_000).apply {
            on(3_100) { connectStarted() }
            on(3_300) { acquired("http/1.1", 1) }
            on(3_310) { requestSent() }
            on(9_310) { firstByte() }
            on(9_311) { headersEnded(200) }
            on(9_320) { bodyStarted() }
            on(9_420) { bodyEnded(40_000L) }
            on(9_421) { ended(ArtworkOutcome.OK) }
        }
        // ... the public picture host's, which does not ...
        call(ArtworkHost.TMDB, 3_000).apply {
            on(3_050) { acquired("h2", 2) }
            on(3_051) { requestSent() }
            on(3_451) { firstByte() }
            on(3_452) { headersEnded(200) }
            on(3_453) { bodyStarted() }
            on(3_503) { bodyEnded(20_000L) }
            on(3_504) { ended(ArtworkOutcome.OK) }
        }
        // ... one the device's cache answered, one whose card left the screen in the queue, one that timed out ...
        call(ArtworkHost.PROVIDER, 3_100).apply {
            on(3_100) { cached() }
            on(3_101) { ended(ArtworkOutcome.OK) }
        }
        call(ArtworkHost.TMDB, 3_200).apply {
            on(8_000) { ended(ArtworkOutcome.CANCELLED) }
        }
        call(ArtworkHost.PROVIDER, 3_900).apply {
            on(4_100) { connectStarted() }
            on(4_400) { acquired("http/1.1", 3) }
            on(4_401) { requestSent() }
            on(24_000) { ended(ArtworkOutcome.TIMEOUT) }
        }
        // ... and one that is still waiting for a place when the report is read, at 65 s.
        call(ArtworkHost.PROVIDER, 50_000)

        // After it: one, on a connection from the pool, answered at once.
        call(ArtworkHost.PROVIDER, 60_000).apply {
            on(60_000) { acquired("http/1.1", 1) }
            on(60_001) { requestSent() }
            on(60_301) { firstByte() }
            on(60_302) { headersEnded(200) }
            on(60_302) { bodyStarted() }
            on(60_402) { bodyEnded(30_000L) }
            on(60_403) { ended(ArtworkOutcome.OK) }
        }

        // Twenty-one cards, asked ten milliseconds apart from 3 s; twenty have their picture, one a second from 5 s.
        repeat(21) { i ->
            at(3_000L + 10 * i)
            recorder.cardAsked("https://host.example/poster-$i.jpg")
        }
        repeat(20) { i ->
            at(5_000L + 1_000 * i)
            recorder.cardShown("https://host.example/poster-$i.jpg")
        }
    }

    @Test
    fun `a window in which nothing was recorded says nothing`() {
        assertTrue(describe(reply = null).isEmpty())
    }

    @Test
    fun `a window of logos only has no poster to say anything of`() {
        recorder.begin(originAt = 0L)
        call(ArtworkHost.OTHER, 100, picture = false).on(200) { ended(ArtworkOutcome.OK) }

        assertTrue(describe(reply = null).isEmpty())
    }

    @Test
    fun `a cold load is told card by card, call by call and against the reply`() {
        coldLoad()

        val lines = describe(reply(startedMs = 1_900, endedMs = 52_400))

        assertEquals(
            listOf(
                "posters: 21 cards asked from 3.00 s · 20 shown · 1st 5.00 s · 10th 14.00 s · 20th 24.00 s",
                "posters: a card waited on screen, came to shown, 10.91/18.83/20.81 s (median/p90/longest) · 1 still without one",
                "posters: reply 78.0 MB · began 1.90 s · ended 52.40 s · the 20th poster came 28.40 s before it ended",
                "posters: 8 calls · ok 4 · cache 1 · cancelled 1 · failed 1 · going 1 · first began 1.00 s" +
                    " · peak at once: open 3, running 2, waiting 2",
                "posters: 4 connections (h2, http/1.1) · 4 bodies · median 19.5 KB · largest 39.1 KB · 0.1 MB in all",
                "posters, each call filed by when it began · trios are median/p90/slowest in seconds",
                "before the reply (1 call): queue 0.05/0.05/0.05 · connect 0.00/0.00/0.00 · 1st byte 0.10/0.10/0.10",
                "before the reply: wait 0.05/0.05/0.05 · body 0.01/0.01/0.01 · whole 0.16/0.16/0.16",
                "during the reply (5 calls, 1 from the cache): queue 0.10/0.20/0.20 · connect 0.20/0.30/0.30" +
                    " · 1st byte 0.40/6.00/6.00",
                "during the reply: wait 0.30/0.50/0.50 · body 0.05/0.10/0.10 · whole 6.42/20.10/20.10" +
                    " · waiting now 1 (oldest 15.00 s)",
                "after the reply (1 call): queue 0.00/0.00/0.00 · connect 0.00/0.00/0.00 · 1st byte 0.30/0.30/0.30",
                "after the reply: wait 0.00/0.00/0.00 · body 0.10/0.10/0.10 · whole 0.40/0.40/0.40",
                "posters: 1st byte median by host, before the reply: other 0.10 s (1)",
                "posters: 1st byte median by host, during the reply: provider 6.00 s (3) · tmdb 0.40 s (2)",
                "posters: 1st byte median by host, after the reply: provider 0.30 s (1)",
                "posters: the application began reading a body 0.00/0.01/0.01 s after its headers",
            ),
            lines,
        )
    }

    @Test
    fun `a load with no reply to set the posters against tells every call together`() {
        recorder.begin(originAt = 0L)
        // Ten calls begun together and ended one to ten seconds later.
        for (k in 1L..10L) {
            call(ArtworkHost.TMDB, 0).apply {
                on(0) { acquired("h2", 1) }
                on(k * 1_000) { ended(ArtworkOutcome.OK) }
            }
        }

        val lines = describe(reply = null, nowMs = 20_000)

        assertTrue(lines.contains("posters: no reply of this load was measured, so every call is told together"))
        // Nearest rank: the fifth of ten for the median, the ninth for the 90th percentile, and the slowest.
        assertTrue(lines.toString(), lines.contains("all calls: wait 0.00/0.00/0.00 · body - · whole 5.00/9.00/10.00"))
        assertTrue(lines.none { it.contains("before the reply") || it.contains("during the reply") })
        assertTrue(lines.contains("posters: 1st byte median by host, all calls: tmdb - (10)"))
    }

    @Test
    fun `a reply that is still arriving is said so, and the posters begun since are those during it`() {
        recorder.begin(originAt = 0L)
        call(ArtworkHost.PROVIDER, 3_000).on(3_000) { acquired("http/1.1", 1) }

        val lines = describe(reply(startedMs = 1_900, endedMs = null, megabytes = 31L))

        assertTrue(lines.toString(), lines.contains("posters: reply 31.0 MB so far · began 1.90 s · still arriving"))
        assertTrue(lines.any { it.startsWith("during the reply (1 call)") })
    }

    @Test
    fun `fewer than twenty posters on screen when the reply ends is said with how many`() {
        recorder.begin(originAt = 0L)
        repeat(5) { i ->
            at(3_000L + i)
            recorder.cardAsked("https://host.example/p$i.jpg")
        }
        repeat(5) { i ->
            at(5_000L + 1_000 * i)
            recorder.cardShown("https://host.example/p$i.jpg")
        }

        val lines = describe(reply(startedMs = 1_900, endedMs = 52_400))

        assertTrue(lines.toString(), lines.contains("posters: 5 cards asked from 3.00 s · 5 shown · 1st 5.00 s"))
        assertTrue(lines.contains("posters: reply 78.0 MB · began 1.90 s · ended 52.40 s · 5 of 20 posters shown so far"))
    }

    @Test
    fun `a twentieth poster that came after the reply ended is told as after`() {
        recorder.begin(originAt = 0L)
        repeat(20) { i ->
            at(1_000L + i)
            recorder.cardAsked("https://host.example/p$i.jpg")
        }
        repeat(20) { i ->
            at(50_000L + 1_000 * i)
            recorder.cardShown("https://host.example/p$i.jpg")
        }

        val lines = describe(reply(startedMs = 1_900, endedMs = 52_400))

        assertTrue(lines.toString(), lines.any { it.endsWith("the 20th poster came 16.60 s after it ended") })
    }

    @Test
    fun `a card that was never asked about does not count, and cards that were, once`() {
        recorder.begin(originAt = 0L)
        at(1_000)
        recorder.cardAsked("https://host.example/a.jpg")
        recorder.cardAsked("https://host.example/a.jpg")
        recorder.cardShown("https://host.example/never-asked.jpg")

        val lines = describe(reply = null)

        assertTrue(lines.toString(), lines.first().startsWith("posters: 1 card asked from 1.00 s · 0 shown"))
    }

    @Test
    fun `what the recorder could not hold is said`() {
        val small = PosterTimelineRecorder(clock = { now }, callCapacity = 1, cardCapacity = 1)
        small.begin(originAt = 0L)
        repeat(3) { small.callStarted(ArtworkHost.PROVIDER, picture = true) }
        repeat(3) { small.cardAsked("https://host.example/$it.jpg") }

        val lines = PosterTimelineReport.describe(small.snapshot(), nowAt = 0L, reply = null)

        assertTrue(lines.toString(), lines.first().contains("1 card asked (+2 not recorded)"))
        assertTrue(lines.toString(), lines.any { it.startsWith("posters: 1 call (+2 not recorded)") })
    }

    @Test
    fun `no address, host, user or password reaches a line, and no hash of one`() {
        recorder.begin(originAt = 0L)
        val secret = "https://secret-host.example:8080/poster/user123/pass456/film.jpg?username=user123&password=pass456"
        at(1_000)
        recorder.cardAsked(secret)
        call(ArtworkHost.PROVIDER, 1_000).apply {
            on(1_100) { acquired("http/1.1", 1) }
            on(1_200) { ended(ArtworkOutcome.OK) }
        }
        at(2_000)
        recorder.cardShown(secret)

        val all = describe(reply(startedMs = 500, endedMs = 3_000)).joinToString("\n")

        for (word in listOf("secret", "example", "8080", "user123", "pass456", "password", "username", "http://", "https://")) {
            assertFalse("$word in $all", all.contains(word))
        }
        assertFalse("a hash in $all", Regex("[0-9a-f]{10,}").containsMatchIn(all))
    }

    @Test
    fun `digits are Western whatever the locale of the device`() {
        coldLoad()
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar-EG"))

            val all = describe(reply(startedMs = 1_900, endedMs = 52_400)).joinToString("\n")

            assertFalse(all, all.any { it.code in 0x0660..0x0669 || it.code in 0x06F0..0x06F9 })
            assertTrue(all, all.contains("10.91/18.83/20.81"))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }

    @Test
    fun `a card scrolled past and back waits from its return, and the empty departures are told`() {
        recorder.begin(originAt = 0L)
        // a: on screen at 1 s, shown at 1.5 s.
        at(1_000); recorder.cardAsked("https://host.example/a.jpg")
        at(1_500); recorder.cardShown("https://host.example/a.jpg")
        // b: on screen at 1 s, scrolled past empty at 1.2 s, back at 11 s, shown at 11.8 s -- it waited 0.8 s, not 10.8.
        at(1_000); recorder.cardAsked("https://host.example/b.jpg")
        at(1_200); recorder.cardLeft("https://host.example/b.jpg")
        at(11_000); recorder.cardAsked("https://host.example/b.jpg")
        at(11_800); recorder.cardShown("https://host.example/b.jpg")
        // c: on screen at 2 s, scrolled past empty at 2.1 s, never back.
        at(2_000); recorder.cardAsked("https://host.example/c.jpg")
        at(2_100); recorder.cardLeft("https://host.example/c.jpg")

        val lines = describe(reply = null)

        assertTrue(lines.toString(), lines.contains("posters: 3 cards asked from 1.00 s · 2 shown · 1st 1.50 s"))
        assertTrue(
            lines.toString(),
            lines.contains(
                "posters: a card waited on screen, came to shown, 0.50/0.80/0.80 s (median/p90/longest)" +
                    " · 1 still without one · left the screen empty 2 times",
            ),
        )
    }

    @Test
    fun `cards that all left empty are still told`() {
        recorder.begin(originAt = 0L)
        at(1_000); recorder.cardAsked("https://host.example/a.jpg")
        at(1_100); recorder.cardLeft("https://host.example/a.jpg")

        val lines = describe(reply = null)

        assertTrue(lines.toString(), lines.contains("posters: no card has its picture yet · 1 still without one · left the screen empty 1 time"))
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
        const val BYTES_PER_MB = 1_048_576L
    }
}
