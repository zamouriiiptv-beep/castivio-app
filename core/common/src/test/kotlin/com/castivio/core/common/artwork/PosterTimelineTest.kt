package com.castivio.core.common.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the recorder keeps of a picture's call and of a card, from a clock that is moved by hand. */
class PosterTimelineTest {

    private var now = 0L
    private val recorder = PosterTimelineRecorder(clock = { now })

    private fun at(ms: Long) {
        now = ms * NANOS_PER_MS
    }

    private fun ms(count: Long) = count * NANOS_PER_MS

    @Test
    fun `nothing is held, and no call is followed, until a load begins`() {
        assertFalse(recorder.recording)

        assertNull(recorder.callStarted(ArtworkHost.PROVIDER, picture = true))
        recorder.cardAsked("https://host/a.jpg")
        recorder.cardShown("https://host/a.jpg")

        assertTrue(recorder.snapshot().isEmpty)
    }

    @Test
    fun `a call's moments are the times the report is made of`() {
        recorder.begin(originAt = 0L)
        at(100)
        val call = recorder.callStarted(ArtworkHost.PROVIDER, picture = true)!!
        at(300)
        call.dnsStarted()
        at(350)
        call.connectStarted()
        at(500)
        call.acquired("http/1.1", connection = 7)
        at(520)
        call.requestSent()
        at(900)
        call.firstByte()
        at(905)
        call.headersEnded(200)
        at(1_000)
        call.bodyStarted()
        at(1_400)
        call.bodyEnded(40_000L)
        at(1_410)
        call.ended(ArtworkOutcome.OK)

        val figures = recorder.snapshot().calls.single()

        assertEquals(ms(100), figures.begunAt)
        assertEquals(ms(300), figures.runningFrom())
        assertEquals(ms(200), figures.queuedNanos())
        // From the first of the name and the socket, which began at 300, to having the connection at 500.
        assertEquals(ms(200), figures.connectingNanos())
        assertEquals(ms(400), figures.beforeConnectionNanos())
        assertEquals(ms(380), figures.firstByteNanos())
        assertEquals(ms(95), figures.pickUpNanos())
        assertEquals(ms(400), figures.bodyNanos())
        assertEquals(ms(1_310), figures.wholeNanos())
        assertEquals(40_000L, figures.bytes)
        assertEquals(200, figures.status)
        assertEquals("http/1.1", figures.protocol)
        assertEquals(7, figures.connection)
        assertEquals(ArtworkHost.PROVIDER, figures.host)
        assertTrue(figures.picture)
        assertTrue(figures.ended)
    }

    @Test
    fun `a call that took a connection from the pool has no time to connect`() {
        recorder.begin(originAt = 0L)
        at(100)
        val call = recorder.callStarted(ArtworkHost.TMDB, picture = true)!!
        at(150)
        call.acquired("h2", connection = 2)

        val figures = recorder.snapshot().calls.single()

        assertEquals(ms(50), figures.queuedNanos())
        assertEquals(0L, figures.connectingNanos())
        assertEquals(ms(50), figures.beforeConnectionNanos())
    }

    @Test
    fun `a call that never had a connection has no time to say for it`() {
        recorder.begin(originAt = 0L)
        at(100)
        val call = recorder.callStarted(ArtworkHost.PROVIDER, picture = true)!!
        at(900)
        call.ended(ArtworkOutcome.CANCELLED)

        val figures = recorder.snapshot().calls.single()

        assertNull(figures.runningFrom())
        assertNull(figures.queuedNanos())
        assertNull(figures.connectingNanos())
        assertNull(figures.beforeConnectionNanos())
        assertNull(figures.firstByteNanos())
        assertNull(figures.bodyNanos())
        // Nobody wanted it any more: its end is not a time anybody waited for.
        assertNull(figures.wholeNanos())
    }

    @Test
    fun `a call that is not over has no whole, and says what it has`() {
        recorder.begin(originAt = 0L)
        at(100)
        val call = recorder.callStarted(ArtworkHost.PROVIDER, picture = true)!!
        at(200)
        call.acquired("http/1.1", connection = 1)

        val figures = recorder.snapshot().calls.single()

        assertFalse(figures.ended)
        assertNull(figures.wholeNanos())
        assertEquals(ms(100), figures.beforeConnectionNanos())
        assertEquals(ms(900), figures.endOr(ms(900)))
    }

    @Test
    fun `a redirect's first answer is forgotten and the last one is timed`() {
        recorder.begin(originAt = 0L)
        val call = recorder.callStarted(ArtworkHost.OTHER, picture = true)!!
        at(10)
        call.requestSent()
        at(110)
        call.firstByte()
        at(111)
        call.headersEnded(302)
        at(120)
        call.requestSent()

        // The hop that was followed has no answer yet: nothing is left of the one before it.
        assertNull(recorder.snapshot().calls.single().firstByteNanos())

        at(420)
        call.firstByte()
        assertEquals(ms(300), recorder.snapshot().calls.single().firstByteNanos())
    }

    @Test
    fun `the first word of an end is the last`() {
        recorder.begin(originAt = 0L)
        val call = recorder.callStarted(ArtworkHost.PROVIDER, picture = true)!!
        at(100)
        call.ended(ArtworkOutcome.OK)
        at(900)
        call.ended(ArtworkOutcome.TIMEOUT)

        val figures = recorder.snapshot().calls.single()

        assertEquals(ArtworkOutcome.OK, figures.outcome)
        assertEquals(ms(100), figures.wholeNanos())
    }

    @Test
    fun `a call is a picture or a logo, as the listener said`() {
        recorder.begin(originAt = 0L)
        recorder.callStarted(ArtworkHost.PROVIDER, picture = true)
        recorder.callStarted(ArtworkHost.OTHER, picture = false)

        assertEquals(listOf(true, false), recorder.snapshot().calls.map { it.picture })
    }

    @Test
    fun `a card is asked once and shown once however often it says so`() {
        recorder.begin(originAt = 0L)
        at(100)
        recorder.cardAsked("https://host/a.jpg")
        at(200)
        recorder.cardAsked("https://host/a.jpg")
        at(900)
        recorder.cardShown("https://host/a.jpg")
        at(950)
        recorder.cardShown("https://host/a.jpg")

        val card = recorder.snapshot().cards.single()

        assertEquals(ms(100), card.askedAt)
        assertEquals(ms(900), card.shownAt)
        assertTrue(card.shown)
    }

    @Test
    fun `a card's wait is from its last coming on screen, and an empty departure is counted once`() {
        recorder.begin(originAt = 0L)
        at(100)
        recorder.cardAsked("https://host/a.jpg")
        at(300)
        recorder.cardLeft("https://host/a.jpg")
        at(350)
        recorder.cardLeft("https://host/a.jpg") // said twice, one departure
        at(5_000)
        recorder.cardAsked("https://host/a.jpg")
        at(5_100)
        recorder.cardAsked("https://host/a.jpg") // a recomposition, not a return
        at(5_400)
        recorder.cardShown("https://host/a.jpg")
        at(6_000)
        recorder.cardLeft("https://host/a.jpg") // left with its picture: not an abandoned wait

        val card = recorder.snapshot().cards.single()

        assertEquals(ms(100), card.askedAt)
        assertEquals(ms(5_400), card.shownAt)
        assertEquals(ms(400), card.waitNanos)
        assertEquals(1, card.leftEmpty)
    }

    @Test
    fun `a card shown on its first visit waits from its first ask, and leaving before a load is nothing`() {
        recorder.cardLeft("https://host/a.jpg")
        recorder.begin(originAt = 0L)
        recorder.cardLeft("https://host/never-asked.jpg")
        at(100)
        recorder.cardAsked("https://host/a.jpg")
        at(700)
        recorder.cardShown("https://host/a.jpg")

        val card = recorder.snapshot().cards.single()

        assertEquals(ms(600), card.waitNanos)
        assertEquals(0, card.leftEmpty)
    }

    @Test
    fun `a picture that was handed to a card nobody asked about is nobody's`() {
        recorder.begin(originAt = 0L)
        recorder.cardShown("https://host/never-asked.jpg")

        assertTrue(recorder.snapshot().cards.isEmpty())
    }

    @Test
    fun `a card that has not been shown has no time`() {
        recorder.begin(originAt = 0L)
        at(100)
        recorder.cardAsked("https://host/a.jpg")

        assertFalse(recorder.snapshot().cards.single().shown)
    }

    @Test
    fun `what does not fit is counted and not kept`() {
        val small = PosterTimelineRecorder(clock = { now }, callCapacity = 2, cardCapacity = 2)
        small.begin(originAt = 0L)

        repeat(5) { small.callStarted(ArtworkHost.PROVIDER, picture = true) }
        repeat(4) { small.cardAsked("https://host/$it.jpg") }

        val snapshot = small.snapshot()
        assertEquals(2, snapshot.calls.size)
        assertEquals(3, snapshot.droppedCalls)
        assertEquals(2, snapshot.cards.size)
        assertEquals(2, snapshot.droppedCards)
    }

    @Test
    fun `a new load forgets the last one's and counts from its own press`() {
        recorder.begin(originAt = 0L)
        recorder.callStarted(ArtworkHost.PROVIDER, picture = true)
        recorder.cardAsked("https://host/a.jpg")

        at(5_000)
        recorder.begin(originAt = ms(4_800))

        val snapshot = recorder.snapshot()
        assertTrue(snapshot.calls.isEmpty())
        assertTrue(snapshot.cards.isEmpty())
        assertEquals(ms(4_800), snapshot.origin)
    }

    @Test
    fun `a load counts from now when no press is given`() {
        at(1_234)
        recorder.begin()

        assertEquals(ms(1_234), recorder.snapshot().origin)
    }

    @Test
    fun `a snapshot is a copy that later marks do not change`() {
        recorder.begin(originAt = 0L)
        val call = recorder.callStarted(ArtworkHost.PROVIDER, picture = true)!!
        val before = recorder.snapshot()

        at(500)
        call.ended(ArtworkOutcome.OK)

        assertFalse(before.calls.single().ended)
        assertTrue(recorder.snapshot().calls.single().ended)
    }

    @Test
    fun `stopping forgets everything and records no more`() {
        recorder.begin(originAt = 0L)
        recorder.callStarted(ArtworkHost.PROVIDER, picture = true)
        recorder.cardAsked("https://host/a.jpg")

        recorder.stop()

        assertFalse(recorder.recording)
        assertTrue(recorder.snapshot().isEmpty)
        assertNull(recorder.callStarted(ArtworkHost.PROVIDER, picture = true))
    }

    @Test
    fun `an address is told apart by a hash that is the well known one`() {
        // The published test vectors of FNV-1a, 64 bits.
        assertEquals(java.lang.Long.parseUnsignedLong("cbf29ce484222325", 16), recorder.keyOf(""))
        assertEquals(java.lang.Long.parseUnsignedLong("af63dc4c8601ec8c", 16), recorder.keyOf("a"))
        assertEquals(java.lang.Long.parseUnsignedLong("85944171f73967e8", 16), recorder.keyOf("foobar"))
        assertNotEquals(recorder.keyOf("https://host/a.jpg"), recorder.keyOf("https://host/b.jpg"))
    }

    @Test
    fun `the application's recorder is off until a load begins`() {
        assertFalse(PosterTimeline.recording)
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
    }
}
