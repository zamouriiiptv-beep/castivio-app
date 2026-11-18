package com.castivio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.castivio.domain.PictureCache
import com.castivio.domain.TrackLanguage
import com.castivio.domain.TrackLanguagePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What Settings holds of its own: the preferred languages and the picture cache. The rest of the screen -- the motion
 * level, the hidden items, the lock's password, the licence -- belongs to the application and is handed in by it.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languages: TrackLanguagePreferences,
    private val pictures: PictureCache,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(audio = languages.audio(), subtitle = languages.subtitle()))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        measure()
    }

    fun setAudio(language: TrackLanguage?) {
        languages.setAudio(language)
        _state.value = _state.value.copy(audio = language)
    }

    fun setSubtitle(language: TrackLanguage?) {
        languages.setSubtitle(language)
        _state.value = _state.value.copy(subtitle = language)
    }

    /** Empties the pictures' cache, and says how much it holds after -- nothing, or what a screen drew meanwhile. */
    fun clearPictures() {
        if (_state.value.clearing) return
        _state.value = _state.value.copy(clearing = true)
        viewModelScope.launch {
            try {
                pictures.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A cache that could not be emptied is measured as it is: the size says what happened.
            }
            _state.value = _state.value.copy(clearing = false)
            measure()
        }
    }

    private fun measure() {
        viewModelScope.launch {
            val bytes = try {
                pictures.sizeBytes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.value = _state.value.copy(pictureBytes = bytes)
        }
    }
}

/** Settings' own part of the screen. [pictureBytes] is null until it is measured, or when it could not be. */
data class SettingsState(
    val audio: TrackLanguage? = null,
    val subtitle: TrackLanguage? = null,
    val pictureBytes: Long? = null,
    val clearing: Boolean = false,
)
