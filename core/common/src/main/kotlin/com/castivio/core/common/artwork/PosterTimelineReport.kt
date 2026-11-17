package com.castivio.core.common.artwork

import java.util.Locale

/**
 * [PosterTimelineSnapshot] as the lines the load strip draws: the six things a wall of posters that is slow to appear has to be
 * told apart by, each said by itself and none left to an average.
 *
 *  1. **How much data a poster is** -- the bodies' median, the largest, and the total.
 *  2. **How long a call waited before it had a connection** -- for a place among the other posters (*queue*) and for a name, a
 *     socket and a handshake (*connect*), and the two together (*wait*).
 *  3. **How long the host took to begin answering** -- *1st byte*, from the request's last header to the answer's first.
 *  4. **How long the picture took to come in** -- *body*, from the application reading its first byte to its last, and *whole*, the
 *     call from being handed to the client to its end.
 *  5. **How many were at it at once** -- the peak of calls open, of calls that had left the queue, and of calls still in it.
 *  6. **Whether the reply delays them** -- every call is filed by when it **began** against the provider's reply: before it, while it
 *     was arriving, or after it ended, and the first-byte time of each host is told for each, so that a host the reply does not
 *     share (the public picture host) is the control for the one it does.
 *
 * And the viewer's own question, from the card: when the first, the tenth and the twentieth poster had its picture, against when
 * the reply ended, and how long a card waited between asking and having it.
 *
 * Every time is in seconds from the press. A figure that has no call to be made of is a dash and never a zero. **Trios** are the
 * median, the 90th percentile and the slowest -- nearest rank, so that each is a time a call really had -- because a mean said
 * that the wall was slow and said nothing about which posters were.
 *
 * No address, host name, user or password is in any line: the hosts are [ArtworkHost]'s three words, the numbers are times and
 * counts, and the cards' hashes are not printed. Western digits always.
 *
 * Pure: the clock and the reply are given.
 */
object PosterTimelineReport {

    /**
     * The lines, or none for a window in which nothing was recorded.
     *
     * @param nowAt the recorder's clock as it reads now: the end of every call that has not ended, and the age of those still
     *   waiting.
     * @param reply the provider's largest reply of the load, or null when none was measured -- a warm open, a load that was
     *   served from a cache -- in which case every call is told together.
     */
    fun describe(snapshot: PosterTimelineSnapshot, nowAt: Long, reply: ReplySpan?): List<String> {
        val pictures = snapshot.calls.filter { it.picture }
        // A window of logos only -- a list of channels -- has no poster to say anything of.
        if (pictures.isEmpty() && snapshot.cards.isEmpty() && snapshot.droppedCards == 0 && snapshot.droppedCalls == 0) return emptyList()
        return buildList {
            addAll(cardLines(snapshot, reply))
            addAll(callLines(snapshot, pictures, nowAt))
            addAll(bucketLines(snapshot, pictures, reply, nowAt))
        }
    }

    /* -------------------------------------------------------------------- the cards */

    private fun cardLines(snapshot: PosterTimelineSnapshot, reply: ReplySpan?): List<String> {
        val cards = snapshot.cards
        val origin = snapshot.origin
        val shown = cards.filter { it.shown }.map { it.shownAt }.sorted()
        val lines = ArrayList<String>(3)
        if (cards.isNotEmpty() || snapshot.droppedCards > 0) {
            val notRecorded = if (snapshot.droppedCards > 0) " (+${snapshot.droppedCards} not recorded)" else ""
            val firstAsked = cards.minOfOrNull { it.askedAt }?.let { " from ${secs(it - origin)}" } ?: ""
            val marks = listOf(1, 10, 20).filter { it <= shown.size }
                .joinToString("") { " · ${ordinal(it)} ${secs(shown[it - 1] - origin)}" }
            lines += "posters: ${if (cards.size == 1) "1 card" else "${cards.size} cards"} asked$notRecorded$firstAsked" +
                " · ${shown.size} shown$marks"
            val waits = cards.filter { it.shown }.map { it.shownAt - it.askedAt }
            trio(waits)?.let {
                val empty = cards.size - shown.size
                lines += "posters: a card waited, asked to shown, $it s (median/p90/longest)" +
                    if (empty > 0) " · $empty still without one" else ""
            }
        }
        lines += replyLine(origin, reply, shown)
        return lines
    }

    private fun replyLine(origin: Long, reply: ReplySpan?, shown: List<Long>): String {
        if (reply == null) return "posters: no reply of this load was measured, so every call is told together"
        val began = "began ${secs(reply.startedAt - origin)}"
        val ended = reply.endedAt ?: return "posters: reply ${megabytes(reply.bytes)} so far · $began · still arriving"
        val twentieth = shown.getOrNull(TWENTIETH - 1)
        val against = when {
            twentieth == null -> " · ${shown.size} of $TWENTIETH posters shown so far"
            twentieth <= ended -> " · the 20th poster came ${secs(ended - twentieth)} before it ended"
            else -> " · the 20th poster came ${secs(twentieth - ended)} after it ended"
        }
        return "posters: reply ${megabytes(reply.bytes)} · $began · ended ${secs(ended - origin)}$against"
    }

    /* -------------------------------------------------------------------- the calls */

    private fun callLines(snapshot: PosterTimelineSnapshot, pictures: List<CallFigures>, nowAt: Long): List<String> {
        val dropped = if (snapshot.droppedCalls > 0) " (+${snapshot.droppedCalls} not recorded)" else ""
        if (pictures.isEmpty()) return listOf("posters: no picture call yet$dropped")
        val kinds = pictures.groupingBy { kindOf(it) }.eachCount()
        val counts = CALL_KINDS.joinToString(" · ") { "$it ${kinds[it] ?: 0}" }
        val open = peak(pictures.map { Span(it.begunAt, it.endOr(nowAt)) })
        val running = peak(pictures.mapNotNull { call -> call.runningFrom()?.let { Span(it, call.endOr(nowAt)) } })
        val waiting = peak(pictures.map { call -> Span(call.begunAt, call.runningFrom() ?: call.endOr(nowAt)) })
        val first = pictures.minOf { it.begunAt }
        val lines = ArrayList<String>(2)
        lines += "posters: ${calls(pictures.size)}$dropped · $counts · first began ${secs(first - snapshot.origin)}" +
            " · peak at once: open $open, running $running, waiting $waiting"

        val connections = pictures.filter { it.hasConnection }.map { it.connection }.toSet().size
        val protocols = pictures.mapNotNull { it.protocol }.toSortedSet().joinToString(", ")
        val bodies = pictures.filter { it.hasBody }.map { it.bytes }.sorted()
        val size = if (bodies.isEmpty()) {
            "no body yet"
        } else {
            "${bodies.size} bodies · median ${kilobytes(nearest(bodies, MEDIAN))} · largest ${kilobytes(bodies.last())}" +
                " · ${megabytes(bodies.sum())} in all"
        }
        val sockets = if (connections > 0) "$connections connections${if (protocols.isEmpty()) "" else " ($protocols)"} · " else ""
        lines += "posters: $sockets$size"
        return lines
    }

    /** What a call came to, in one word each call has exactly one of: the five add up to the calls. */
    private fun kindOf(call: CallFigures): String = when {
        call.fromCache -> "cache"
        !call.ended -> "going"
        call.outcome == ArtworkOutcome.CANCELLED -> "cancelled"
        call.outcome == ArtworkOutcome.OK -> "ok"
        else -> "failed"
    }

    /* -------------------------------------------------------------------- against the reply */

    private enum class Moment(val label: String) {
        BEFORE("before the reply"),
        DURING("during the reply"),
        AFTER("after the reply"),
    }

    private fun bucketLines(
        snapshot: PosterTimelineSnapshot,
        pictures: List<CallFigures>,
        reply: ReplySpan?,
        nowAt: Long,
    ): List<String> {
        if (pictures.isEmpty()) return emptyList()
        val groups: List<Pair<String, List<CallFigures>>> = if (reply == null) {
            listOf("all calls" to pictures)
        } else {
            val byMoment = pictures.groupBy { momentOf(it, reply) }
            Moment.values().mapNotNull { moment -> byMoment[moment]?.let { moment.label to it } }
        }
        val lines = ArrayList<String>(groups.size * 3 + 2)
        lines += "posters, each call filed by when it began · trios are median/p90/slowest in seconds"
        for ((label, calls) in groups) lines += callFigures(label, calls, nowAt)
        for ((label, calls) in groups) hostLine(label, calls)?.let { lines += it }
        pickUpLine(pictures)?.let { lines += it }
        return lines
    }

    private fun momentOf(call: CallFigures, reply: ReplySpan): Moment {
        val ended = reply.endedAt
        return when {
            call.begunAt < reply.startedAt -> Moment.BEFORE
            ended == null || call.begunAt <= ended -> Moment.DURING
            else -> Moment.AFTER
        }
    }

    /**
     * One group's five times, in two lines. The calls the device's own cache answered are counted and left out of the times: they
     * made no connection, and their nought would pull every median toward the cache and away from the road.
     */
    private fun callFigures(label: String, calls: List<CallFigures>, nowAt: Long): List<String> {
        val network = calls.filter { !it.fromCache }
        val cached = calls.size - network.size
        val head = "$label (${calls(network.size)}${if (cached > 0) ", $cached from the cache" else ""})"
        val still = network.filter { !it.ended && it.runningFrom() == null }
        val stillWaiting = if (still.isEmpty()) "" else " · waiting now ${still.size} (oldest ${secs(nowAt - still.minOf { it.begunAt })})"
        return listOf(
            "$head: queue ${trioOrDash(network.mapNotNull { it.queuedNanos() })}" +
                " · connect ${trioOrDash(network.mapNotNull { it.connectingNanos() })}" +
                " · 1st byte ${trioOrDash(network.mapNotNull { it.firstByteNanos() })}",
            "$label: wait ${trioOrDash(network.mapNotNull { it.beforeConnectionNanos() })}" +
                " · body ${trioOrDash(network.mapNotNull { it.bodyNanos() })}" +
                " · whole ${trioOrDash(network.mapNotNull { it.wholeNanos() })}$stillWaiting",
        )
    }

    /**
     * The median first byte of each host kind in one group. A host the reply does not share -- the public picture host -- told
     * beside the provider's, which it does: if both slow down while the reply arrives the road is the cause, and if only the
     * provider's does, the provider is.
     */
    private fun hostLine(label: String, calls: List<CallFigures>): String? {
        val parts = ArtworkHost.values().mapNotNull { host ->
            val mine = calls.filter { it.host == host && !it.fromCache }
            if (mine.isEmpty()) return@mapNotNull null
            val firsts = mine.mapNotNull { it.firstByteNanos() }.sorted()
            val median = if (firsts.isEmpty()) DASH else secs(nearest(firsts, MEDIAN))
            "${host.name.lowercase(Locale.ROOT)} $median (${mine.size})"
        }
        if (parts.isEmpty()) return null
        return "posters: 1st byte median by host, $label: ${parts.joinToString(" · ")}"
    }

    /** From the headers to the application reading the body: time that is the application's, and that a slow road cannot explain. */
    private fun pickUpLine(pictures: List<CallFigures>): String? {
        val values = pictures.filter { !it.fromCache }.mapNotNull { it.pickUpNanos() }
        val trio = trio(values) ?: return null
        return "posters: the application began reading a body $trio s after its headers"
    }

    /* -------------------------------------------------------------------- arithmetic */

    private class Span(val from: Long, val to: Long)

    /** The most spans that overlap at one instant. Touching spans do not: one that ends where another begins frees its place first. */
    private fun peak(spans: List<Span>): Int {
        val events = ArrayList<LongArray>(spans.size * 2)
        for (span in spans) {
            if (span.to <= span.from) continue
            events += longArrayOf(span.from, 1L)
            events += longArrayOf(span.to, -1L)
        }
        events.sortWith(compareBy<LongArray>({ it[0] }, { it[1] }))
        var now = 0
        var best = 0
        for (event in events) {
            now += event[1].toInt()
            if (now > best) best = now
        }
        return best
    }

    /**
     * Nearest rank: the smallest value that at least [percent] of them are not above -- a time a call really had. Whole numbers,
     * so that no rounding of a fraction moves a rank by one.
     */
    private fun nearest(sorted: List<Long>, percent: Int): Long {
        val rank = ((sorted.size * percent + PERCENT - 1) / PERCENT).coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    /** `0.30/2.00/2.90`: the median, the 90th percentile and the slowest, or null for nothing to make them of. */
    private fun trio(nanos: List<Long>): String? {
        if (nanos.isEmpty()) return null
        val sorted = nanos.sorted()
        return "${plain(nearest(sorted, MEDIAN))}/${plain(nearest(sorted, NINETIETH))}/${plain(sorted.last())}"
    }

    private fun trioOrDash(nanos: List<Long>): String = trio(nanos) ?: DASH

    /* -------------------------------------------------------------------- words and figures */

    /** A time in seconds with no unit, for a trio that says its unit once. */
    private fun plain(nanos: Long): String = String.format(Locale.ROOT, "%.2f", nanos.coerceAtLeast(0L) / NANOS_PER_S)

    private fun secs(nanos: Long): String = plain(nanos) + " s"

    private fun megabytes(bytes: Long): String = String.format(Locale.ROOT, "%.1f MB", bytes / BYTES_PER_MB)

    private fun kilobytes(bytes: Long): String = String.format(Locale.ROOT, "%.1f KB", bytes / BYTES_PER_KB)

    private fun calls(count: Int): String = if (count == 1) "1 call" else "$count calls"

    private fun ordinal(n: Int): String = when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${n}th"
    }

    private const val DASH = "-"
    private const val TWENTIETH = 20
    private const val MEDIAN = 50
    private const val NINETIETH = 90
    private const val PERCENT = 100
    private const val NANOS_PER_S = 1_000_000_000.0
    private const val BYTES_PER_KB = 1_024.0
    private const val BYTES_PER_MB = 1_048_576.0
    private val CALL_KINDS = listOf("ok", "cache", "cancelled", "failed", "going")
}
