package com.castivio.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the measurement panels are drawn: a debug build, the measurement build, and never a viewer's build. */
class MeasureFlagTest {

    private var asked = 0

    private fun flag(says: Boolean) = MeasureFlag { asked++; says }

    @Test
    fun `a debuggable build measures, without the manifest being asked`() {
        assertTrue(flag(says = false).isOn(debuggable = true))
        assertEquals(0, asked)
    }

    @Test
    fun `the measurement build measures, its manifest asked once for the process`() {
        val flag = flag(says = true)

        assertTrue(flag.isOn(debuggable = false))
        assertTrue(flag.isOn(debuggable = false))
        assertEquals(1, asked)
    }

    @Test
    fun `a viewer's build does not measure`() {
        val flag = flag(says = false)

        assertFalse(flag.isOn(debuggable = false))
        assertFalse(flag.isOn(debuggable = false))
        assertEquals(1, asked)
    }

    @Test
    fun `a manifest that cannot be read is a build that does not measure`() {
        assertFalse(MeasureFlag { throw SecurityException("no") }.isOn(debuggable = false))
    }
}
