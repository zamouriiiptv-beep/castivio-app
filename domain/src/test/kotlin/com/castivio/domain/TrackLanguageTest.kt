package com.castivio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which tracks are in a preferred language, in every spelling a container or an engine gives one. */
class TrackLanguageTest {

    @Test
    fun `a two-letter code, with or without a region, is the language`() {
        assertTrue(TrackLanguage.ARABIC.matches("ar", null))
        assertTrue(TrackLanguage.ARABIC.matches("ar-SA", null))
        assertTrue(TrackLanguage.PORTUGUESE.matches("pt_BR", null))
        assertTrue(TrackLanguage.ENGLISH.matches(" EN ", null))
    }

    @Test
    fun `both three-letter codes of a language are the language`() {
        assertTrue(TrackLanguage.ARABIC.matches("ara", null))
        assertTrue(TrackLanguage.FRENCH.matches("fra", null))
        assertTrue(TrackLanguage.FRENCH.matches("fre", null))
        assertTrue(TrackLanguage.GERMAN.matches("ger", null))
        assertTrue(TrackLanguage.DUTCH.matches("dut", null))
    }

    @Test
    fun `another language's code is not the language, whatever the label says`() {
        assertFalse(TrackLanguage.ARABIC.matches("en", "Arabic"))
        assertFalse(TrackLanguage.FRENCH.matches("es", null))
    }

    @Test
    fun `with no language, the label is read as whole words`() {
        assertTrue(TrackLanguage.ARABIC.matches(null, "Arabic"))
        assertTrue(TrackLanguage.ARABIC.matches(null, "Arabic 5.1"))
        assertTrue(TrackLanguage.ARABIC.matches("und", "العربية"))
        assertTrue(TrackLanguage.ENGLISH.matches("", "English (SDH)"))
        assertFalse(TrackLanguage.ENGLISH.matches(null, "Englishman"))
        assertFalse(TrackLanguage.ARABIC.matches(null, "Original"))
        assertFalse(TrackLanguage.ARABIC.matches(null, "Track 2"))
        assertFalse(TrackLanguage.ARABIC.matches(null, null))
    }

    @Test
    fun `a choice is kept and found again by its code, and none is null`() {
        for (language in TrackLanguage.entries) assertEquals(language, TrackLanguage.of(language.code))
        assertNull(TrackLanguage.of(null))
        assertNull(TrackLanguage.of("xx"))
    }

    @Test
    fun `no two languages share a code`() {
        assertEquals(TrackLanguage.entries.size, TrackLanguage.entries.map { it.code }.toSet().size)
    }
}
