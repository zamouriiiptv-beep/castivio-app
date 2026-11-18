package com.castivio.feature.settings

import com.castivio.domain.PictureCache
import com.castivio.domain.TrackLanguage
import com.castivio.domain.TrackLanguagePreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** What Settings keeps of its own: the preferred languages, and the pictures' cache measured and emptied. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Kept(var sound: TrackLanguage? = null, var text: TrackLanguage? = null) : TrackLanguagePreferences {
        override fun audio() = sound
        override fun subtitle() = text
        override fun setAudio(language: TrackLanguage?) { sound = language }
        override fun setSubtitle(language: TrackLanguage?) { text = language }
    }

    private class Pictures(var bytes: Long = 48L * 1024 * 1024) : PictureCache {
        var clears = 0
        var failMeasure = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun sizeBytes(): Long {
            if (failMeasure) error("unreadable")
            return bytes
        }
        override suspend fun clear() {
            gate?.await()
            clears++
            bytes = 0
        }
    }

    @Test
    fun `the languages kept are the ones shown, and a choice is kept`() = runTest(dispatcher) {
        val kept = Kept(sound = TrackLanguage.ARABIC)
        val model = SettingsViewModel(kept, Pictures())

        assertEquals(TrackLanguage.ARABIC, model.state.value.audio)
        assertNull(model.state.value.subtitle)

        model.setSubtitle(TrackLanguage.FRENCH)
        model.setAudio(null)

        assertEquals(TrackLanguage.FRENCH, kept.text)
        assertNull(kept.sound)
        assertEquals(TrackLanguage.FRENCH, model.state.value.subtitle)
        assertNull(model.state.value.audio)
    }

    @Test
    fun `the cache is measured when the screen opens, and emptied on a press`() = runTest(dispatcher) {
        val pictures = Pictures()
        val model = SettingsViewModel(Kept(), pictures)
        advanceUntilIdle()
        assertEquals(48L * 1024 * 1024, model.state.value.pictureBytes)

        model.clearPictures()
        advanceUntilIdle()

        assertEquals(1, pictures.clears)
        assertEquals(0L, model.state.value.pictureBytes)
        assertFalse(model.state.value.clearing)
    }

    @Test
    fun `a second press while it is being emptied empties it once`() = runTest(dispatcher) {
        val pictures = Pictures().apply { gate = CompletableDeferred() }
        val model = SettingsViewModel(Kept(), pictures)
        advanceUntilIdle()

        model.clearPictures()
        advanceUntilIdle()
        assertTrue(model.state.value.clearing)
        model.clearPictures()
        pictures.gate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, pictures.clears)
        assertFalse(model.state.value.clearing)
    }

    @Test
    fun `a cache that cannot be measured shows no size, and the screen stands`() = runTest(dispatcher) {
        val model = SettingsViewModel(Kept(), Pictures().apply { failMeasure = true })
        advanceUntilIdle()

        assertNull(model.state.value.pictureBytes)
    }
}
