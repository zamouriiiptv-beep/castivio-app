package com.castivio.data.parsing

import com.castivio.domain.CatalogItem
import com.castivio.domain.CatalogWriter
import com.castivio.domain.ImportMode
import com.castivio.domain.ImportSummary
import com.castivio.domain.MediaGroup
import com.castivio.domain.MediaKind
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Reader
import java.io.StringReader
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * The categories a viewer asks for while a walk of the categories is bringing a section, which is how the radio is loaded and
 * how any section is loaded when its one reply fails.
 *
 * The walk is a fixed number of workers pulling from one list, and a viewer's category is not a request of its own: it goes to
 * the front of that list. What is proved is that it is fetched next, that it is fetched once and counted once, that the walk
 * never has more requests open than it has workers, and that an ask that means nothing is nothing.
 */
class XtreamWalkAsksTest {

    private fun group(kind: MediaKind, name: String) = StableIds.group("s", kind, name)

    private fun asks(vararg groups: String): Channel<String> =
        Channel<String>(Channel.UNLIMITED).also { channel -> groups.forEach { channel.trySend(it) } }

    @Test
    fun `a category asked for during the walk is fetched next, ahead of the ones before it in the provider's order`() {
        val api = OrderedApi(categories = 8, label = "Category")
        val writer = RecordingWriter()

        val summary = XtreamImportEngine(writer, concurrency = 1).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.LIVE),
            mode = ImportMode.APPEND,
            earlyAsks = asks(group(MediaKind.LIVE, "Category 7")),
        )

        // The worker may have taken the first category before the ask was heard; the next one it takes is the asked one.
        assertTrue("fetched in the order ${api.calls}", api.calls.indexOf("7") in 0..1)
        assertEquals("each category fetched once", (0 until 8).map(Int::toString), api.calls.sortedBy(String::toInt))
        assertEquals("and counted once", 8 * OrderedApi.ITEMS, summary.items)
        assertEquals(8 * OrderedApi.ITEMS, writer.items.size)
    }

    @Test
    fun `the walk never has more requests open than its workers, asks or no asks`() {
        val api = OrderedApi(categories = 24, label = "Category")
        val writer = RecordingWriter()
        val viewer = (0 until 24 step 5).map { group(MediaKind.LIVE, "Category $it") }.toTypedArray()

        XtreamImportEngine(writer, concurrency = 4).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.LIVE),
            mode = ImportMode.APPEND,
            earlyAsks = asks(*viewer),
        )

        assertTrue("peak ${api.peak} is over the limit of 4", api.peak <= 4)
        assertEquals("every category once", 24, api.calls.size)
        assertEquals(24, api.calls.toSet().size)
    }

    @Test
    fun `the radio's categories are served like any other, and its walk touches no channel`() {
        val api = OrderedApi(categories = 6, label = "| AR | RADIO")
        val writer = RecordingWriter()

        val summary = XtreamImportEngine(writer, concurrency = 1).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.RADIO),
            mode = ImportMode.APPEND,
            earlyAsks = asks(group(MediaKind.RADIO, "| AR | RADIO 5")),
        )

        assertTrue("fetched in the order ${api.calls}", api.calls.indexOf("5") in 0..1)
        assertEquals(6 * OrderedApi.ITEMS, summary.count(MediaKind.RADIO))
        assertEquals(0, summary.count(MediaKind.LIVE))
    }

    @Test
    fun `an ask for a category the walk does not have, or has already taken, changes nothing`() {
        val api = OrderedApi(categories = 5, label = "Category")
        val writer = RecordingWriter()

        val summary = XtreamImportEngine(writer, concurrency = 1).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.LIVE),
            mode = ImportMode.APPEND,
            earlyAsks = asks("not-a-group", group(MediaKind.LIVE, "Category 99")),
        )

        assertEquals(listOf("0", "1", "2", "3", "4"), api.calls)
        assertEquals(5 * OrderedApi.ITEMS, summary.items)
    }

    @Test
    fun `a walk with no one asking is the walk it was`() {
        val api = OrderedApi(categories = 6, label = "Category")
        val writer = RecordingWriter()

        val summary = XtreamImportEngine(writer, concurrency = 1).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.LIVE),
            mode = ImportMode.APPEND,
        )

        assertEquals((0 until 6).map(Int::toString), api.calls)
        assertEquals(6 * OrderedApi.ITEMS, summary.items)
    }

    @Test
    fun `asks that come while the walk is running are served too, and the walk ends when its categories do`() {
        val api = OrderedApi(categories = 10, label = "Category", pauseMs = 40L)
        val writer = RecordingWriter()
        val channel = Channel<String>(Channel.UNLIMITED)
        val asker = Thread {
            Thread.sleep(60)
            channel.trySend(group(MediaKind.LIVE, "Category 9"))
        }.also { it.start() }

        val summary = XtreamImportEngine(writer, concurrency = 1).importCatalogue(
            sourceId = "s",
            api = api,
            kinds = setOf(MediaKind.LIVE),
            mode = ImportMode.APPEND,
            earlyAsks = channel,
        )
        asker.join()

        assertTrue("9 was fetched before the end of the order: ${api.calls}", api.calls.indexOf("9") < 9)
        assertEquals(10 * OrderedApi.ITEMS, summary.items)
    }

    /** Categories `0 until categories`, named `"$label $n"`, each with [ITEMS] channels; records the order of the requests. */
    private class OrderedApi(
        val categories: Int,
        val label: String,
        val pauseMs: Long = 30L,
    ) : XtreamImportEngine.Api {

        companion object {
            const val ITEMS = 3
        }

        val calls: MutableList<String> = Collections.synchronizedList(ArrayList())
        private val inFlight = AtomicInteger()

        @Volatile
        var peak = 0

        override fun categories(kind: MediaKind): Reader {
            val body = (0 until categories).joinToString(",") { """{"category_id":"$it","category_name":"$label $it"}""" }
            return StringReader("[$body]")
        }

        override fun streams(kind: MediaKind, categoryId: String): Reader {
            calls += categoryId
            val now = inFlight.incrementAndGet()
            synchronized(this) { if (now > peak) peak = now }
            try {
                Thread.sleep(pauseMs)
                val body = (0 until ITEMS).joinToString(",") { n ->
                    """{"num":0,"name":"Channel $categoryId-$n","stream_id":"$categoryId-$n",""" +
                        """"stream_icon":"http://cdn/$categoryId-$n.png","epg_channel_id":"epg.$categoryId-$n",""" +
                        """"category_id":"$categoryId"}"""
                }
                return StringReader("[$body]")
            } finally {
                inFlight.decrementAndGet()
            }
        }

        override fun series(categoryId: String): Reader = StringReader("[]")
        override fun seriesInfo(seriesId: String): Reader = StringReader("{}")
        override fun streamUrl(kind: MediaKind, streamId: String, extension: String?): String = "http://example.invalid/$streamId"
    }

    /** Collects what reached SQLite, from whichever thread the engine writes on. */
    private class RecordingWriter : CatalogWriter {
        val items: MutableList<CatalogItem> = Collections.synchronizedList(ArrayList())
        val groups: MutableList<MediaGroup> = Collections.synchronizedList(ArrayList())

        override fun begin(sourceId: String, mode: ImportMode, sections: Set<MediaKind>) = Unit
        override fun writeGroups(groups: List<MediaGroup>) { this.groups += groups }
        override fun writeItems(items: List<CatalogItem>) { this.items += items }
        override fun commit() = Unit
        override fun finish(summary: ImportSummary) = Unit
        override fun abort(cause: Throwable?) = Unit
    }
}
