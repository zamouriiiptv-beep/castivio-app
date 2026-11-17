package com.castivio.data.database

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The shows' Recently watched list, against real SQLite: a show is watched through its episodes, the list is the shows with
 * an episode watched with the last first, a show watched again goes back to the top and is listed once however many of its
 * episodes were watched, and what it says of the show is what the list of all the shows says.
 *
 * The fixture is a playlist whose shows are two: Breaking Bad, three episodes in two seasons, and Chernobyl, one -- made of
 * episodes alone, as a playlist's are, so there is no row of a show's own for the list to stop at -- with a film and a
 * channel beside them, which are not shows however often they are watched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentShowsTest {

    private lateinit var database: CastivioDatabase

    /** The clock the records are written with: each watch below is a second after the one before. */
    private var now = 1_000L

    @Before
    fun open() {
        database = CastivioDatabase.inMemory(RuntimeEnvironment.getApplication())
        Fixtures.import(database, Fixtures.mixedPlaylist())
    }

    @After
    fun close() {
        database.close()
    }

    private fun progress() = RoomProgressRepository(database.progressDao()) { now }

    /** Read from the table, which is what the player is given. */
    private fun episodeId(show: String, season: Int, episode: Int, source: String = "src"): String =
        database.openHelper.readableDatabase
            .query(
                "SELECT id FROM media WHERE series_title = ? AND season_number = ? AND episode_number = ? AND source_id = ?",
                arrayOf<Any>(show, season, episode, source),
            )
            .use { it.moveToFirst(); it.getString(0) }

    private fun idOf(title: String): String = database.openHelper.readableDatabase
        .query("SELECT id FROM media WHERE title = ?", arrayOf<Any>(title))
        .use { it.moveToFirst(); it.getString(0) }

    private fun watch(id: String) = runBlocking {
        now += 1_000
        progress().watched(id)
    }

    private fun recent(offset: Int = 0, limit: Int = 60) = runBlocking { database.mediaDao().windowRecentSeries(offset, limit) }

    private fun titles() = recent().map { it.title }

    @Test
    fun `nothing watched is an empty list`() {
        assertEquals(emptyList<String?>(), titles())
    }

    @Test
    fun `a show with an episode watched is listed, with the figures of the whole show`() {
        watch(episodeId("Breaking Bad", 1, 2))

        val shows = recent()

        assertEquals(listOf("Breaking Bad"), shows.map { it.title })
        // The show's, and not the episode's: every episode it has, in every season.
        assertEquals(3, shows.single().episodeCount)
        assertEquals(2, shows.single().seasonCount)
    }

    @Test
    fun `what the list says of a show is what the list of all the shows says`() {
        watch(episodeId("Chernobyl", 1, 1))

        val all = runBlocking { database.mediaDao().windowSeries(null, "PROVIDER", 0, 60) }

        assertEquals(all.single { it.title == "Chernobyl" }, recent().single())
    }

    @Test
    fun `the show watched last is first, and a show is listed once however many of its episodes were watched`() {
        watch(episodeId("Breaking Bad", 1, 1))
        watch(episodeId("Chernobyl", 1, 1))
        watch(episodeId("Breaking Bad", 2, 1))
        watch(episodeId("Breaking Bad", 1, 2))

        // Breaking Bad was watched last, three times, and Chernobyl once between: two shows, not four rows.
        assertEquals(listOf("Breaking Bad", "Chernobyl"), titles())
    }

    @Test
    fun `a show watched again goes back to the top`() {
        watch(episodeId("Breaking Bad", 1, 1))
        watch(episodeId("Chernobyl", 1, 1))
        assertEquals(listOf("Chernobyl", "Breaking Bad"), titles())

        watch(episodeId("Breaking Bad", 2, 1))

        assertEquals(listOf("Breaking Bad", "Chernobyl"), titles())
    }

    @Test
    fun `a film and a channel that were watched are not shows`() {
        watch(idOf("Alpha"))
        watch(idOf("Nova Sports 1"))

        assertTrue(recent().isEmpty())

        watch(episodeId("Chernobyl", 1, 1))

        assertEquals(listOf("Chernobyl"), titles())
    }

    @Test
    fun `the list is read a window at a time`() {
        watch(episodeId("Breaking Bad", 1, 1))
        watch(episodeId("Chernobyl", 1, 1))

        assertEquals(listOf("Chernobyl"), recent(offset = 0, limit = 1).map { it.title })
        assertEquals(listOf("Breaking Bad"), recent(offset = 1, limit = 1).map { it.title })
        assertTrue(recent(offset = 2, limit = 1).isEmpty())
    }

    @Test
    fun `only the active subscription's shows are listed`() {
        Fixtures.import(
            database,
            listOf(
                "#EXTM3U",
                """#EXTINF:-1 group-title="Series",Fargo S01E01 - The Crocodile's Dilemma""",
                "http://host/series/u/p/90.mkv",
            ),
            sourceId = "other",
        )
        watch(episodeId("Fargo", 1, 1, source = "other"))
        watch(episodeId("Chernobyl", 1, 1))

        // "src" is the active one: the other subscription's show is kept in its record and not shown.
        assertEquals(listOf("Chernobyl"), titles())

        Fixtures.activate(database, "other")

        assertEquals(listOf("Fargo"), titles())
    }
}
