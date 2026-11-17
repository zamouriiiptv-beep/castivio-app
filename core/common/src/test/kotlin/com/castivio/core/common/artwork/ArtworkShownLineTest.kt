package com.castivio.core.common.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What a title's page prints of its large poster and its wide picture. */
class ArtworkShownLineTest {

    @Test
    fun `both pictures are told with their time, source and size`() {
        assertEquals(
            "poster 0.42 s net 500x750 · backdrop 0.88 s disk 1280x720",
            ArtworkShownLine.describe(
                ArtworkShown(420, ArtworkSource.NETWORK, 500, 750), true,
                ArtworkShown(880, ArtworkSource.DISK, 1280, 720), true,
            ),
        )
    }

    @Test
    fun `a picture still on its way is said to be, and one the page does not draw is left out`() {
        assertEquals("poster …", ArtworkShownLine.describe(null, true, null, false))
        assertEquals(
            "poster 0.05 s mem 342x513 · backdrop …",
            ArtworkShownLine.describe(ArtworkShown(50, ArtworkSource.MEMORY, 342, 513), true, null, true),
        )
    }

    @Test
    fun `nothing asked is nothing said`() {
        assertNull(ArtworkShownLine.describe(null, false, null, false))
    }

    @Test
    fun `a clock that went back is never a negative time`() {
        assertEquals("poster 0.00 s net 1x1", ArtworkShownLine.describe(ArtworkShown(-5, ArtworkSource.NETWORK, 1, 1), true, null, false))
    }
}
