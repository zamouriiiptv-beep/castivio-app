package com.castivio.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.castivio.domain.MovieDetails
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A film's wide picture, kept with its details: `vod_detail.backdrop_url`, version 13 (`CastivioMigrations.ADD_FILM_BACKDROP`).
 *
 * The cache writes and reads it; and the migration adds the column as the entity declares it and sets every film kept before it
 * back to `ids_read` = 0, so that each is asked of its provider once more, behind its page, and gets its picture.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilmBackdropTest {

    private lateinit var database: CastivioDatabase
    private lateinit var films: RoomMovieDetailsCache

    @Before
    fun setUp() {
        database = CastivioDatabase.inMemory(RuntimeEnvironment.getApplication())
        films = RoomMovieDetailsCache(database.vodDetailDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a film's wide picture is kept with its details and read back`() = runBlocking {
        films.put("src", "42", MovieDetails(plot = "A story.", backdropUrl = "https://image.tmdb.org/t/p/original/b.jpg"), now = 1L)

        assertEquals("https://image.tmdb.org/t/p/original/b.jpg", films.get("src", "42")!!.backdropUrl)
    }

    @Test
    fun `a film with no wide picture reads back none`() = runBlocking {
        films.put("src", "42", MovieDetails(plot = "A story."), now = 1L)

        assertNull(films.get("src", "42")!!.backdropUrl)
    }

    @Test
    fun `12 to 13 adds the column as the entity declares it, and asks every kept film once more`() = runBlocking {
        val db = database.openHelper.writableDatabase
        val declared = columnOf(db, "vod_detail", "backdrop_url")
        assertEquals(Column("TEXT", notNull = 0, default = null), declared)

        toVersionTwelve(db)
        db.execSQL(
            "INSERT INTO `vod_detail` (`source_id`, `provider_ref`, `plot`, `fetched_at`, `tmdb_id`, `ids_read`) " +
                "VALUES ('src', '7', 'Kept before.', 1000, 603, 1)",
        )
        assertNull("a version 12 table has no such column", columnOf(db, "vod_detail", "backdrop_url"))

        CastivioMigrations.ALL.single { it.startVersion == 12 }.migrate(db)

        assertEquals("as Room checks for it", declared, columnOf(db, "vod_detail", "backdrop_url"))
        val kept = films.get("src", "7")!!
        assertEquals("the film is as it was", "Kept before.", kept.plot)
        assertEquals(603L, kept.tmdbId)
        assertNull("with no picture yet", kept.backdropUrl)
        assertFalse("and asked of its provider once more, behind its page", kept.idsRead)
    }

    @Test
    fun `the migration to version 13 is the last of the chain, and the database is at 13`() {
        val last = CastivioMigrations.ALL.maxByOrNull { it.startVersion }!!

        assertEquals(12, last.startVersion)
        assertEquals(13, last.endVersion)
        assertEquals(13, database.openHelper.readableDatabase.version)
    }

    // ------------------------------------------------------------------ helpers

    private data class Column(val type: String, val notNull: Int, val default: String?)

    private fun columnOf(db: SupportSQLiteDatabase, table: String, name: String): Column? =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val columnName = cursor.getColumnIndexOrThrow("name")
            val type = cursor.getColumnIndexOrThrow("type")
            val notNull = cursor.getColumnIndexOrThrow("notnull")
            val default = cursor.getColumnIndexOrThrow("dflt_value")
            var found: Column? = null
            while (found == null && cursor.moveToNext()) {
                if (cursor.getString(columnName) == name) {
                    found = Column(cursor.getString(type), cursor.getInt(notNull), cursor.getString(default))
                }
            }
            found
        }

    /** `vod_detail` as a version 12 database has it: the table Room makes less the column this migration adds. */
    private fun toVersionTwelve(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE `vod_detail`")
        db.execSQL(
            "CREATE TABLE `vod_detail` (`source_id` TEXT NOT NULL, `provider_ref` TEXT NOT NULL, " +
                "`rating` REAL, `plot` TEXT, `director` TEXT, `cast_names` TEXT, `genres` TEXT, " +
                "`release_year` INTEGER, `duration_seconds` INTEGER, `added_epoch` INTEGER, " +
                "`fetched_at` INTEGER NOT NULL, `tmdb_id` INTEGER, `imdb_id` TEXT, " +
                "`ids_read` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`source_id`, `provider_ref`))",
        )
    }
}
