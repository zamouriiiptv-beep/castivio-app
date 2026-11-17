package com.castivio.data.networking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the posters' host is asked to open its connection, and that a failure costs nothing. */
class PictureHostWarmUpTest {

    private var now = 1_000_000L
    private var sent = 0
    private val warmUp = PictureHostWarmUp(clock = { now }, send = { sent++ })

    @Test
    fun `the first load opens the connection`() {
        assertTrue(warmUp.warm())
        assertEquals(1, sent)
    }

    @Test
    fun `a second load within the minute finds it open and asks nothing`() {
        warmUp.warm()
        now += PictureHostWarmUp.MIN_INTERVAL_MS - 1

        assertFalse(warmUp.warm())
        assertEquals(1, sent)
    }

    @Test
    fun `a load a minute later asks again`() {
        warmUp.warm()
        now += PictureHostWarmUp.MIN_INTERVAL_MS

        assertTrue(warmUp.warm())
        assertEquals(2, sent)
    }

    @Test
    fun `a clock that went back is not a reason to stay quiet`() {
        warmUp.warm()
        now -= 5_000L

        assertTrue(warmUp.warm())
        assertEquals(2, sent)
    }

    @Test
    fun `a failure to send is the posters' own connection, as before`() {
        val failing = PictureHostWarmUp(clock = { now }, send = { throw IllegalStateException("no network") })

        assertTrue(failing.warm())
    }
}
