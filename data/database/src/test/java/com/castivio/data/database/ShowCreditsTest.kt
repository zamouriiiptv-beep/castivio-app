package com.castivio.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.castivio.domain.SeriesDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A show's director and added date, kept with its details: `series_detail.director` and `added_epoch`, version 14
 * (`CastivioMigrations.ADD_SHOW_CREDITS`), for the show's information screen and its "added on" chip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowCreditsTest {

    private lateinit var database: CastivioDatabase
    private lateinit var shows: RoomSeriesDetailsStore

    @Before
    fun setUp() {
        database = CastivioDatabase.inMemory(RuntimeEnvironment.getApplication())
        shows = RoomSeriesDetailsStore(database.seriesDetailDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a show's director and added date are kept and read back`() = runBlocking {
        shows.put("show", SeriesDetails(plot = "A story.", director = "Gustavo Lipsztein", addedEpochSeconds = 1773446400L), emptyMap(), 1L)

        val read = shows.details("show").first()!!
        assertEquals("Gustavo Lipsztein", read.director)
        assertEquals(1773446400L, read.addedEpochSeconds)
    }

    @Test
    fun `a show the provider gave neither is read back with neither`() = runBlocking {
        shows.put("show", SeriesDetails(plot = "A story."), emptyMap(), 1L)

        val read = shows.details("show").first()!!
        assertNull(read.director)
        assertNull(read.addedEpochSeconds)
    }

    @Test
    fun `13 to 14 adds the two columns as the entity declares them, and leaves the row as it was`() = runBlocking {
        val db = database.openHelper.writableDatabase
        val director = columnOf(db, "director")
        val added = columnOf(db, "added_epoch")
        assertEquals(Column("TEXT", 0, null), director)
        assertEquals(Column("INTEGER", 0, null), added)

        db.execSQL("DROP TABLE `series_detail`")
        db.execSQL(
            "CREATE TABLE `series_detail` (`series_id` TEXT NOT NULL, `rating` REAL, `plot` TEXT, " +
                "`cast_names` TEXT, `genres` TEXT, `release_year` INTEGER, `backdrop_url` TEXT, " +
                "`fetched_at` INTEGER NOT NULL, `tmdb_id` INTEGER, `imdb_id` TEXT, PRIMARY KEY(`series_id`))",
        )
        db.execSQL("INSERT INTO `series_detail` (`series_id`, `plot`, `fetched_at`) VALUES ('show', 'Kept before.', 1000)")

        CastivioMigrations.ALL.single { it.startVersion == 13 }.migrate(db)

        assertEquals(director, columnOf(db, "director"))
        assertEquals(added, columnOf(db, "added_epoch"))
        val kept = shows.details("show").first()!!
        assertEquals("Kept before.", kept.plot)
        assertNull(kept.director)
        assertNull(kept.addedEpochSeconds)
    }

    @Test
    fun `the migration to version 14 is the last of the chain, and the database is at 14`() {
        val last = CastivioMigrations.ALL.maxByOrNull { it.startVersion }!!

        assertEquals(13, last.startVersion)
        assertEquals(14, last.endVersion)
        assertEquals(14, database.openHelper.readableDatabase.version)
    }

    private data class Column(val type: String, val notNull: Int, val default: String?)

    private fun columnOf(db: SupportSQLiteDatabase, name: String): Column? =
        db.query("PRAGMA table_info(`series_detail`)").use { cursor ->
            var found: Column? = null
            while (found == null && cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == name) {
                    found = Column(
                        cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("notnull")),
                        cursor.getString(cursor.getColumnIndexOrThrow("dflt_value")),
                    )
                }
            }
            found
        }
}
