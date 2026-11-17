package com.castivio.data.networking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The picture dispatcher's limits while a section of films arrives: lowered when an import begins, and given back when -- and only
 * when -- every import that lowered them has ended, however it ended.
 *
 * What these hold is what the application relies on and a poster wall would only show late: a limit that is lowered and is never
 * given back leaves every poster after it waiting for a place that does not come.
 */
class ArtworkConcurrencyTest {

    private val normal = ArtworkConcurrency.Limits(maxRequests = 32, maxRequestsPerHost = 12)
    private val low = ArtworkConcurrency.Limits(maxRequests = 3, maxRequestsPerHost = 3)

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun concurrency() = ArtworkConcurrency(normal, low)

    private fun Dispatcher.limits() = ArtworkConcurrency.Limits(maxRequests, maxRequestsPerHost)

    // ----------------------------------------------------------------------------------------------- the limits

    @Test
    fun `a dispatcher with no hold open has the normal limits, and a hold lowers them`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()

        concurrency.attach(dispatcher)
        assertEquals(normal, dispatcher.limits())
        assertEquals(normal, concurrency.limits)

        concurrency.hold()

        assertEquals(low, dispatcher.limits())
        assertEquals(low, concurrency.limits)
    }

    @Test
    fun `closing the hold gives the normal limits back`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        val hold = concurrency.hold()
        hold.close()

        assertEquals(normal, dispatcher.limits())
        assertEquals(0, concurrency.holds)
    }

    @Test
    fun `the per-host limit is never above the total, in either state`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        for (limits in listOf(dispatcher.limits(), concurrency.hold().let { dispatcher.limits() })) {
            assertTrue("$limits", limits.maxRequestsPerHost <= limits.maxRequests)
        }
    }

    @Test
    fun `a limit set up the wrong way round is refused when it is made`() {
        try {
            ArtworkConcurrency.Limits(maxRequests = 3, maxRequestsPerHost = 12)
            fail("a host cannot be allowed more calls than the dispatcher is")
        } catch (expected: IllegalArgumentException) {
            // as it should be
        }
        try {
            ArtworkConcurrency.Limits(maxRequests = 0, maxRequestsPerHost = 0)
            fail("a dispatcher that allows no call would never start one")
        } catch (expected: IllegalArgumentException) {
            // as it should be
        }
    }

    // ------------------------------------------------------------------------------------- imports that overlap

    @Test
    fun `the normal limits come back only when the last of two overlapping holds is closed`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        val first = concurrency.hold()
        val second = concurrency.hold()
        assertEquals(2, concurrency.holds)

        first.close()
        assertEquals("the second import is still running", low, dispatcher.limits())
        assertEquals(1, concurrency.holds)

        second.close()
        assertEquals(normal, dispatcher.limits())
        assertEquals(0, concurrency.holds)
    }

    @Test
    fun `a hold closed twice is given back once, and not the other import's`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        val ended = concurrency.hold()
        val running = concurrency.hold()

        ended.close()
        ended.close()

        assertEquals("the one that is running still holds them", low, dispatcher.limits())
        assertEquals(1, concurrency.holds)

        running.close()
        assertEquals(normal, dispatcher.limits())
    }

    // ------------------------------------------------------------------------ the dispatcher arriving later

    @Test
    fun `a dispatcher that arrives while an import runs is held from the start, and freed when it ends`() {
        val concurrency = concurrency()
        val hold = concurrency.hold()
        val dispatcher = Dispatcher()

        concurrency.attach(dispatcher)
        assertEquals(low, dispatcher.limits())

        hold.close()
        assertEquals(normal, dispatcher.limits())
    }

    @Test
    fun `a second dispatcher takes the first one's place, and the first is left at the normal limits`() {
        val concurrency = concurrency()
        val first = Dispatcher()
        val second = Dispatcher()
        concurrency.attach(first)
        val hold = concurrency.hold()

        concurrency.attach(second)

        assertEquals("the first is not left held with nothing to free it", normal, first.limits())
        assertEquals(low, second.limits())

        hold.close()
        assertEquals(normal, second.limits())
        assertEquals(normal, first.limits())
    }

    @Test
    fun `an import that begins before any dispatcher exists is counted, and nothing fails`() {
        val concurrency = concurrency()

        val hold = concurrency.hold()
        assertEquals(1, concurrency.holds)
        assertEquals(low, concurrency.limits)

        hold.close()
        assertEquals(0, concurrency.holds)
        assertEquals(normal, concurrency.limits)
    }

    // ------------------------------------------------------------------------------- the import's own flow

    @Test
    fun `a flow holds the pictures while it is collected, and gives them back when it completes`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)
        val seen = mutableListOf<ArtworkConcurrency.Limits>()

        val got = runBlocking {
            concurrency.whileCollecting(
                flow {
                    seen += dispatcher.limits()
                    emit(1)
                    seen += dispatcher.limits()
                    emit(2)
                },
            ).toList()
        }

        assertEquals(listOf(1, 2), got)
        assertEquals("held from the first of it to the last", listOf(low, low), seen)
        assertEquals(normal, dispatcher.limits())
        assertEquals(0, concurrency.holds)
    }

    @Test
    fun `a flow that fails gives the pictures back`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        try {
            runBlocking {
                concurrency.whileCollecting(
                    flow<Int> {
                        emit(1)
                        throw IOException("connection reset")
                    },
                ).toList()
            }
            fail("the failure is the importer's to tell, and is not swallowed here")
        } catch (expected: IOException) {
            // passed through as it was
        }

        assertEquals(normal, dispatcher.limits())
        assertEquals(0, concurrency.holds)
    }

    @Test
    fun `a flow that is cancelled gives the pictures back`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        runBlocking {
            val begun = CompletableDeferred<Unit>()
            val collecting = launch {
                concurrency.whileCollecting(
                    flow<Int> {
                        begun.complete(Unit)
                        awaitCancellation()
                    },
                ).toList()
            }
            begun.await()
            assertEquals("held while it runs", low, dispatcher.limits())

            collecting.cancelAndJoin()
        }

        assertEquals(normal, dispatcher.limits())
        assertEquals(0, concurrency.holds)
    }

    @Test
    fun `a flow nobody collects holds nothing`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        concurrency.whileCollecting(flow { emit(1) })

        assertEquals(0, concurrency.holds)
        assertEquals(normal, dispatcher.limits())
    }

    @Test
    fun `two flows that overlap give the pictures back when the last of them ends`() {
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)

        runBlocking {
            val oneBegun = CompletableDeferred<Unit>()
            val twoBegun = CompletableDeferred<Unit>()
            val one = launch {
                concurrency.whileCollecting(flow<Int> { oneBegun.complete(Unit); awaitCancellation() }).toList()
            }
            oneBegun.await()
            val two = launch {
                concurrency.whileCollecting(flow<Int> { twoBegun.complete(Unit); awaitCancellation() }).toList()
            }
            twoBegun.await()
            assertEquals(2, concurrency.holds)

            one.cancelAndJoin()
            assertEquals("the second is still arriving", low, dispatcher.limits())

            two.cancelAndJoin()
        }

        assertEquals(normal, dispatcher.limits())
    }

    // ------------------------------------------------------------------------------------- what OkHttp does

    /**
     * A limit that falls does nothing to a call that is running, and keeps the calls that come after it from starting until the
     * running ones are fewer than it. The four that are on their way finish as they were going to; the two that are asked for
     * once the limit is three wait their turn, and run when there is a place.
     */
    @Test
    fun `calls already running are left to finish, and the calls that come after wait for a place`() {
        val reached = CountDownLatch(4)
        val release = CountDownLatch(1)
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                reached.countDown()
                release.await(10, TimeUnit.SECONDS)
                return MockResponse().setBody("poster")
            }
        }
        val concurrency = concurrency()
        val dispatcher = Dispatcher()
        concurrency.attach(dispatcher)
        val client = OkHttpClient.Builder().dispatcher(dispatcher).build()
        val ended = CountDownLatch(6)

        fun ask(n: Int) = client.newCall(Request.Builder().url(server.url("/p$n.jpg")).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = ended.countDown()
            override fun onResponse(call: Call, response: Response) {
                response.close()
                ended.countDown()
            }
        })

        repeat(4) { ask(it) }
        assertTrue("four calls are on their way", reached.await(10, TimeUnit.SECONDS))
        assertEquals(4, dispatcher.runningCallsCount())

        val hold = concurrency.hold()
        repeat(2) { ask(4 + it) }

        assertEquals("the calls that were running are not stopped", 4, dispatcher.runningCallsCount())
        assertEquals("the two that came after wait: four are more than the three allowed", 2, dispatcher.queuedCallsCount())

        release.countDown()
        assertTrue("every call ended, the two that waited included", ended.await(10, TimeUnit.SECONDS))
        hold.close()
        assertEquals(0, dispatcher.runningCallsCount())
        assertEquals(0, dispatcher.queuedCallsCount())
    }

    @Test
    fun `the dispatcher keeps the threads it was given, and none is made`() {
        val shared = OkHttpClient()
        val pictures = ArtworkCalls.deriveClient(
            shared = shared,
            listenerFactory = okhttp3.EventListener.Factory { okhttp3.EventListener.NONE },
            maxRequests = normal.maxRequests,
            maxRequestsPerHost = normal.maxRequestsPerHost,
            callTimeoutMs = 6_000L,
        )
        val concurrency = concurrency()

        concurrency.attach(pictures.dispatcher)
        concurrency.hold()

        assertEquals(low, pictures.dispatcher.limits())
        assertSame("no second pool of threads is made for the pictures", shared.dispatcher.executorService, pictures.dispatcher.executorService)
        assertEquals("the application's own dispatcher is not touched", 5, shared.dispatcher.maxRequestsPerHost)
        assertEquals(64, shared.dispatcher.maxRequests)
    }
}
