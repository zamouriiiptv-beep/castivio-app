package com.castivio.core.common.artwork

/** An instant that did not come. No moment is ever this one, and a figure that would be made of it is not made. */
internal const val NEVER = Long.MIN_VALUE

/**
 * One picture's call, moment by moment: what a poster did between being handed to the client and being whole.
 *
 * Every moment is a reading of the recorder's clock, and **none is an address**: the call knows which kind of host it asked
 * ([ArtworkHost], three words), whether it was a poster or a backdrop and not a logo, and which connection carried it by a number
 * that stands for nothing outside this process.
 *
 * ## The moments, in the order a call goes through them
 *
 *  - **begun**: the call was handed to the client -- before the dispatcher has given it a place.
 *  - **name / socket**: the first sign that the call is running: a name being looked up, a socket being opened. A call that
 *    reuses a connection has neither.
 *  - **acquired**: the call has a connection to use.
 *  - **sent**: the request's last header has gone.
 *  - **first byte**: the host answered. OkHttp says so when the answer's headers are in, and for a picture's few hundred bytes of
 *    headers that is the first byte to the millisecond.
 *  - **body start**: the application began reading the body. Between the headers and this is the application's, not the road's.
 *  - **body end**: the picture's last byte is in.
 *  - **ended**: the call is over, however it ended.
 *
 * From these [CallFigures] makes the times the strip says: how long a call **queued** for a place, how long it spent
 * **connecting**, how long the host took to **begin** answering, how long the **body** took, and the **whole**.
 *
 * Written by the image client's listener from several threads, one call at a time, and read by the strip from another: every
 * moment is volatile, which is all that is needed for a figure that is read to be a figure that was written.
 */
class PictureCall internal constructor(
    private val clock: () -> Long,
    val host: ArtworkHost,
    /** A poster or a backdrop, asked for as a picture, and not a logo. */
    val picture: Boolean,
) {
    private val begunAt: Long = clock()

    @Volatile private var dnsAt = NEVER

    @Volatile private var connectAt = NEVER

    @Volatile private var acquiredAt = NEVER

    @Volatile private var sentAt = NEVER

    @Volatile private var firstByteAt = NEVER

    @Volatile private var headersAt = NEVER

    @Volatile private var bodyStartAt = NEVER

    @Volatile private var bodyEndAt = NEVER

    @Volatile private var endAt = NEVER

    @Volatile private var bytes = 0L

    @Volatile private var status = 0

    @Volatile private var fromCache = false

    @Volatile private var outcome: ArtworkOutcome? = null

    @Volatile private var protocol: String? = null

    @Volatile private var connection = 0

    fun dnsStarted() {
        if (dnsAt == NEVER) dnsAt = clock()
    }

    fun connectStarted() {
        if (connectAt == NEVER) connectAt = clock()
    }

    /** The call has a connection: [protocol] is `http/1.1` or `h2`, [connection] a number that tells two connections apart. */
    fun acquired(protocol: String, connection: Int) {
        if (acquiredAt != NEVER) return
        acquiredAt = clock()
        this.protocol = protocol
        this.connection = connection
    }

    /** OkHttp's own cache answered, and no connection was made. */
    fun cached() {
        fromCache = true
    }

    /**
     * The request's headers have gone. A redirect sends another request, and the answer that is timed is the last one's: what the
     * hop before it saw is forgotten.
     */
    fun requestSent() {
        sentAt = clock()
        firstByteAt = NEVER
        headersAt = NEVER
        bodyStartAt = NEVER
    }

    fun firstByte() {
        firstByteAt = clock()
    }

    fun headersEnded(status: Int) {
        headersAt = clock()
        this.status = status
    }

    /** The application read the first of the body. */
    fun bodyStarted() {
        if (bodyStartAt == NEVER) bodyStartAt = clock()
    }

    fun bodyEnded(bytes: Long) {
        bodyEndAt = clock()
        this.bytes = bytes
    }

    /** The call is over; the first word is the last. */
    fun ended(outcome: ArtworkOutcome) {
        if (endAt != NEVER) return
        endAt = clock()
        this.outcome = outcome
    }

    internal fun figures(): CallFigures = CallFigures(
        begunAt = begunAt,
        dnsAt = dnsAt,
        connectAt = connectAt,
        acquiredAt = acquiredAt,
        sentAt = sentAt,
        firstByteAt = firstByteAt,
        headersAt = headersAt,
        bodyStartAt = bodyStartAt,
        bodyEndAt = bodyEndAt,
        endAt = endAt,
        bytes = bytes,
        outcome = outcome,
        status = status,
        host = host,
        picture = picture,
        protocol = protocol,
        connection = connection,
        fromCache = fromCache,
    )
}

/**
 * A [PictureCall] as it stood, copied, so that a report is made of one reading and not of calls that move under it.
 *
 * Every time is in nanoseconds on the recorder's clock; a method answers null when the moments it is made of did not come -- a
 * call that was cancelled in the queue has no connection time, one that failed has no body.
 */
class CallFigures internal constructor(
    val begunAt: Long,
    private val dnsAt: Long,
    private val connectAt: Long,
    private val acquiredAt: Long,
    private val sentAt: Long,
    private val firstByteAt: Long,
    private val headersAt: Long,
    private val bodyStartAt: Long,
    private val bodyEndAt: Long,
    val endAt: Long,
    val bytes: Long,
    val outcome: ArtworkOutcome?,
    val status: Int,
    val host: ArtworkHost,
    val picture: Boolean,
    val protocol: String?,
    val connection: Int,
    val fromCache: Boolean,
) {
    val ended: Boolean get() = endAt != NEVER

    /** The call has a connection to use. */
    val hasConnection: Boolean get() = acquiredAt != NEVER

    /** The call carried a body: a picture came, whole or in part. */
    val hasBody: Boolean get() = bytes > 0L

    /**
     * The instant the call left the dispatcher's queue and began to do something -- look a name up, open a socket, take a
     * connection -- or null for a call that never did: one answered from the cache, one that was given up on in the queue.
     */
    fun runningFrom(): Long? = earliest(dnsAt, connectAt, acquiredAt)

    /** The instant the call stopped being waited for: its end, or [nowAt] while it goes on. */
    fun endOr(nowAt: Long): Long = if (endAt != NEVER) endAt else nowAt

    /** From being handed to the client to the first sign that it is running: the dispatcher's queue. */
    fun queuedNanos(): Long? = runningFrom()?.let { it - begunAt }

    /** From the lookup or the socket to having the connection: nought for a reused one, null for a call that never had one. */
    fun connectingNanos(): Long? {
        if (acquiredAt == NEVER) return null
        val start = earliest(dnsAt, connectAt) ?: return 0L
        return acquiredAt - start
    }

    /** Everything before the call had a connection -- [queuedNanos] and [connectingNanos] together -- or null if it never had one. */
    fun beforeConnectionNanos(): Long? = if (acquiredAt == NEVER) null else acquiredAt - begunAt

    /** From the request being sent to the host beginning to answer: what the host spent before it said a word. */
    fun firstByteNanos(): Long? = if (sentAt == NEVER || firstByteAt == NEVER) null else firstByteAt - sentAt

    /** From the answer's headers to the application reading the first of the body: the application's, and not the road's. */
    fun pickUpNanos(): Long? = if (headersAt == NEVER || bodyStartAt == NEVER) null else bodyStartAt - headersAt

    /** From the application reading the first of the body to its last byte: the picture coming in. */
    fun bodyNanos(): Long? = if (bodyStartAt == NEVER || bodyEndAt == NEVER) null else bodyEndAt - bodyStartAt

    /** From being handed to the client to the end. Null for a call that is not over, and for one nobody wanted any more. */
    fun wholeNanos(): Long? = if (endAt == NEVER || outcome == ArtworkOutcome.CANCELLED) null else endAt - begunAt

    private fun earliest(vararg instants: Long): Long? = instants.filter { it != NEVER }.minOrNull()
}

/**
 * A card of the wall: when its poster was first asked for, when the picture was handed to it, and what the viewer waited.
 *
 * [waitNanos] is counted from the card's **last coming on screen** before its picture came, not from its first: a card the
 * viewer scrolled past, whose call was given up, and that came back ten seconds later waited for as long as it was back, and
 * not for the ten seconds it was off the screen. [leftEmpty] is how many times it left the screen with no picture -- a wait the
 * viewer abandoned, told apart rather than folded into a time.
 */
class CardFigures internal constructor(val askedAt: Long, val shownAt: Long, val waitNanos: Long, val leftEmpty: Int) {
    val shown: Boolean get() = shownAt != NEVER
}

/**
 * What the recorder holds, copied: the calls in the order they began, the cards in the order they asked, where the count of time
 * began, and what was over the limit.
 */
class PosterTimelineSnapshot(
    val origin: Long,
    val calls: List<CallFigures>,
    val cards: List<CardFigures>,
    val droppedCalls: Int,
    val droppedCards: Int,
) {
    /** Nothing was recorded: the instruments were off, or no picture has been asked for since they were turned on. */
    val isEmpty: Boolean get() = calls.isEmpty() && cards.isEmpty() && droppedCalls == 0 && droppedCards == 0
}

/**
 * The provider's reply that is the largest of the load, as far as the clock knows it: when its headers came, when its last byte
 * came, and whether it has ended. What the pictures' times are set against.
 *
 * On the recorder's clock -- both are the monotonic one -- so that a poster that began at one instant and a reply that ended at
 * another can be compared without converting either.
 */
class ReplySpan(
    /** What has come of it, as the meter counts it. */
    val bytes: Long,
    /** The reply's headers are in: it has begun. */
    val startedAt: Long,
    /** The latest byte that came. */
    val lastByteAt: Long,
    /** The reply's body ended, or null while it goes on. */
    val endedAt: Long?,
)

/**
 * What the posters of a wall did, instant by instant, so that *when they appeared* can be set against *what else the connection
 * was doing* -- which the strip's counters, which are sums and means, cannot say.
 *
 * ## What it answers
 *
 * Whether a poster waits for the reply of tens of megabytes that the same phone is receiving: for how long it queued for a
 * place, how long it connected, how long the host spent before it began to answer, how long the picture took, how many were at
 * it at once -- each for the calls that began before the reply, during it and after it -- and when the first twenty cards had
 * their pictures against when the reply's last byte came.
 *
 * ## Passive, and for development
 *
 * It is told what happened and has no say in it: the listener on the image client hands each call to [callStarted] and marks the
 * moments, and a poster card says [cardAsked] and [cardShown]. It begins to record at [begin], which the load strip calls at a
 * press when the instruments are on, and **until then every entry point returns at its first line** -- so in a build that is not
 * debuggable nothing is held, nothing is read and nothing is allocated.
 *
 * What it keeps is bounded -- [callCapacity] calls and [cardCapacity] cards, then a count of what did not fit -- and holds no
 * address: a card is known by a 64-bit hash of its poster's address, and the hash is never printed.
 *
 * A call and a card are **not linked** to one another. A card cannot say which call served it -- a picture asked for by two cards
 * is one call, a picture from the device's cache is none -- and a link that is guessed makes a figure that is wrong without
 * showing it. The two are told as they are, and the report sets them side by side.
 *
 * A class, with [PosterTimeline] as the application's one, so a test records with a clock of its own.
 */
open class PosterTimelineRecorder(
    private val clock: () -> Long = System::nanoTime,
    private val callCapacity: Int = DEFAULT_CALL_CAPACITY,
    private val cardCapacity: Int = DEFAULT_CARD_CAPACITY,
) {

    private class Card(val askedAt: Long) {
        @Volatile var shownAt = NEVER

        /** When it last came on screen: the start of the wait that counts. */
        @Volatile var visitAt = askedAt

        @Volatile var onScreen = true

        @Volatile var waitNanos = NEVER

        @Volatile var leftEmpty = 0
    }

    /** True from [begin] on: what the hooks check first, and the only thing they do when it is false. */
    @Volatile
    var recording: Boolean = false
        private set

    private val lock = Any()
    private val calls = ArrayList<PictureCall>()
    private var droppedCalls = 0
    private val cards = ArrayList<Card>()
    private val cardsByKey = HashMap<Long, Card>()
    private var droppedCards = 0
    private var origin = 0L

    /** The recorder's clock as it reads now, for whoever has to put a moment of their own on it. */
    fun now(): Long = clock()

    /**
     * A load is beginning: whatever was recorded is the last load's, and goes. From here on it records.
     *
     * [originAt] is the instant the times are counted from -- the viewer's press, on this recorder's clock -- or now when it is
     * not given.
     */
    fun begin(originAt: Long? = null) {
        synchronized(lock) {
            calls.clear()
            cards.clear()
            cardsByKey.clear()
            droppedCalls = 0
            droppedCards = 0
            origin = originAt ?: clock()
            recording = true
        }
    }

    /**
     * A call of the image client has begun. The call it returns is for the listener to mark, or null where nothing is being
     * recorded or the recorder is full -- and a null is a call that is not followed, which costs the caller one comparison.
     */
    fun callStarted(host: ArtworkHost, picture: Boolean): PictureCall? {
        if (!recording) return null
        synchronized(lock) {
            if (calls.size >= callCapacity) {
                droppedCalls++
                return null
            }
            return PictureCall(clock, host, picture).also { calls += it }
        }
    }

    /**
     * The card for the poster at [url] is on screen and has asked for it. Said on every composition; only the first after the
     * card came on screen counts. A card scrolled away and back is the same card -- counted once, its first ask kept -- and its
     * wait begins again from its return.
     */
    fun cardAsked(url: String) {
        if (!recording) return
        val key = keyOf(url)
        synchronized(lock) {
            val known = cardsByKey[key]
            if (known != null) {
                if (!known.onScreen) {
                    known.onScreen = true
                    known.visitAt = clock()
                }
                return
            }
            if (cards.size >= cardCapacity) {
                droppedCards++
                return
            }
            val card = Card(clock())
            cards += card
            cardsByKey[key] = card
        }
    }

    /** The picture of the poster at [url] was handed to its card. The first time counts; a card that was never asked about is nobody's. */
    fun cardShown(url: String) {
        if (!recording) return
        val key = keyOf(url)
        synchronized(lock) {
            val card = cardsByKey[key] ?: return
            if (card.shownAt == NEVER) {
                card.shownAt = clock()
                card.waitNanos = card.shownAt - card.visitAt
            }
        }
    }

    /** The card for the poster at [url] left the screen. Without its picture, that is a wait the viewer gave up on. */
    fun cardLeft(url: String) {
        if (!recording) return
        val key = keyOf(url)
        synchronized(lock) {
            val card = cardsByKey[key] ?: return
            if (!card.onScreen) return
            card.onScreen = false
            if (card.shownAt == NEVER) card.leftEmpty++
        }
    }

    /** Everything held, copied, for a report. */
    fun snapshot(): PosterTimelineSnapshot = synchronized(lock) {
        PosterTimelineSnapshot(
            origin = origin,
            calls = calls.map { it.figures() },
            cards = cards.map { CardFigures(it.askedAt, it.shownAt, it.waitNanos, it.leftEmpty) },
            droppedCalls = droppedCalls,
            droppedCards = droppedCards,
        )
    }

    /**
     * An address as a number: FNV-1a over its characters, 64 bits. It tells two cards apart and cannot be turned back into the
     * address, and nothing prints it.
     */
    internal fun keyOf(url: String): Long {
        var hash = FNV_OFFSET
        for (c in url) hash = (hash xor c.code.toLong()) * FNV_PRIME
        return hash
    }

    /** Stops recording and forgets what was held: for a test, and for a build that turns the instruments off. */
    fun stop() {
        synchronized(lock) {
            recording = false
            calls.clear()
            cards.clear()
            cardsByKey.clear()
            droppedCalls = 0
            droppedCards = 0
        }
    }

    companion object {
        /** Enough for a cold load's first minutes: a wall asks for dozens of posters, and a scroll through it for some hundreds. */
        const val DEFAULT_CALL_CAPACITY = 500
        const val DEFAULT_CARD_CAPACITY = 300

        private const val FNV_OFFSET = -3750763034362895579L
        private const val FNV_PRIME = 1099511628211L
    }
}

/** The recorder the application writes to and the load strip reads. */
object PosterTimeline : PosterTimelineRecorder()
