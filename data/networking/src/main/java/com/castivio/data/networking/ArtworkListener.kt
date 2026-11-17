package com.castivio.data.networking

import com.castivio.core.common.artwork.ArtworkHost
import com.castivio.core.common.artwork.ArtworkMetrics
import com.castivio.core.common.artwork.ArtworkOutcome
import com.castivio.core.common.artwork.ArtworkTally
import com.castivio.core.common.artwork.PictureCall
import com.castivio.core.common.artwork.PosterTimeline
import com.castivio.core.common.artwork.PosterTimelineRecorder
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Tells [ArtworkTally] what the image loader's calls did, and [PosterTimelineRecorder] when each did it, and does nothing else.
 *
 * Like [CallMetricsListener], an observer: an [EventListener] is handed events after the fact and has no way to change a
 * request, a response, a cache or a timeout. It is the image loader's client's own -- the pictures are not the provider's
 * calls, and counted among them they made the provider's figures say what the posters did.
 *
 * What the tally gets is how each call **ended** -- a picture, a refusal with its status, a timeout, a road that failed, or a call
 * nobody wanted any more because its card had left the screen -- how long it took, how long it waited before it had a
 * connection to use (for a slot in the dispatcher, a name, a handshake), how large its body was, and which kind of host it
 * asked: the provider's, the public picture host the playlists name most, or another. Never an address: see [ArtworkHost].
 *
 * What the timeline gets is the **instant** of each step of the call -- the dispatcher's queue, the name, the socket, the
 * connection, the request, the host's first byte, the application's first read of the body, its last byte, the end -- and whether
 * it was a picture, which the request says by the tag [ArtworkCalls] puts on it. The timeline is off until a load begins in a
 * build whose instruments are on, and until then [PosterTimelineRecorder.callStarted] answers null at its first line and every
 * event below is a comparison with null.
 *
 * One listener per call, through [FACTORY], which is what makes the per-call fields safe.
 */
class ArtworkListener(
    private val tally: ArtworkTally = ArtworkMetrics,
    private val timeline: PosterTimelineRecorder = PosterTimeline,
) : EventListener() {

    private var beganAt = 0L
    private var connectedAt = 0L
    private var status: Int? = null
    private var bytes = 0L
    private var finished = false

    /** This call on the timeline, or null where nothing is being recorded. Read by whichever thread the next event comes on. */
    @Volatile
    private var record: PictureCall? = null

    override fun callStart(call: Call) {
        beganAt = System.nanoTime()
        val request = call.request()
        val host = hostKind(request.url)
        tally.started(host)
        record = timeline.callStarted(host, picture = ArtworkCalls.isPicture(request))
    }

    override fun dnsStart(call: Call, domainName: String) {
        record?.dnsStarted()
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        record?.connectStarted()
    }

    /** The call has a connection to use: what it waited for before this -- a slot, a name, a handshake -- is over. */
    override fun connectionAcquired(call: Call, connection: Connection) {
        if (connectedAt == 0L) connectedAt = System.nanoTime()
        record?.acquired(connection.protocol().toString(), System.identityHashCode(connection))
    }

    /** OkHttp's own cache answered, and no connection will be made. */
    override fun cacheHit(call: Call, response: Response) {
        record?.cached()
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        record?.requestSent()
    }

    /** The host has begun to answer: OkHttp says so when the answer's headers are in. */
    override fun responseHeadersStart(call: Call) {
        record?.firstByte()
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        // The last one wins: a redirect answers once for the hop and once for the reply that has the body.
        status = response.code
        record?.headersEnded(response.code)
    }

    /** The application has read the first of the body. */
    override fun responseBodyStart(call: Call) {
        record?.bodyStarted()
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        bytes = byteCount
        record?.bodyEnded(byteCount)
    }

    override fun callEnd(call: Call) = finish(call, failure = null)

    override fun callFailed(call: Call, ioe: IOException) = finish(call, failure = ioe)

    private fun finish(call: Call, failure: IOException?) {
        if (finished) return
        finished = true
        val now = System.nanoTime()
        val took = if (beganAt > 0) now - beganAt else 0L
        val waited = if (beganAt > 0 && connectedAt >= beganAt) connectedAt - beganAt else NO_WAIT
        val answered = status
        val outcome = when {
            failure != null -> outcomeOf(call, failure)
            answered != null && answered >= HTTP_ERRORS -> ArtworkOutcome.HTTP
            else -> ArtworkOutcome.OK
        }
        tally.ended(
            outcome = outcome,
            httpStatus = if (outcome == ArtworkOutcome.HTTP) answered else null,
            bytes = bytes,
            callNanos = took,
            waitNanos = waited,
        )
        record?.ended(outcome)
    }

    companion object {
        /** One listener per call, which is what makes the per-call fields safe. */
        val FACTORY = EventListener.Factory { ArtworkListener() }

        private const val HTTP_ERRORS = 400
        private const val NO_WAIT = -1L

        /**
         * How a failed call failed.
         *
         * A call whose time ran out is an [InterruptedIOException] -- OkHttp's own call timeout and a socket timeout both are
         * -- and is asked **first**, because OkHttp's call timer ends the call by cancelling it: `isCanceled()` is true for a
         * call that timed out, and read before the exception it would file every timeout as a card that left the screen.
         *
         * A call that was cancelled and did not time out is not a failure: the card left the screen, and the loader says so
         * by cancelling. Anything else is the road: refused, reset, no route, no name.
         */
        internal fun outcomeOf(call: Call, failure: IOException): ArtworkOutcome = when {
            failure is InterruptedIOException -> ArtworkOutcome.TIMEOUT
            call.isCanceled() -> ArtworkOutcome.CANCELLED
            else -> ArtworkOutcome.NETWORK
        }

        /** The three words for a host: the public picture host by its name, the provider's by having been its API's. */
        internal fun hostKind(url: HttpUrl): ArtworkHost {
            val host = url.host
            return when {
                host == TMDB_HOST || host.endsWith(".$TMDB_HOST") -> ArtworkHost.TMDB
                CallMetrics.isApiHost(host) -> ArtworkHost.PROVIDER
                else -> ArtworkHost.OTHER
            }
        }

        private const val TMDB_HOST = "tmdb.org"
    }
}
