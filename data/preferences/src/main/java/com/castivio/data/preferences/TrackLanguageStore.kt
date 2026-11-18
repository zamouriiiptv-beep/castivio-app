package com.castivio.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.castivio.domain.TrackLanguage
import com.castivio.domain.TrackLanguagePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The languages the viewer prefers for sound and subtitles (Settings → Player), between launches.
 *
 * `SharedPreferences`, as the subtitles' look is kept: two values read when a film opens and written when a person picks a
 * row -- no flow, no migration. Kept by the language's two-letter code, so a language added to [TrackLanguage] later
 * changes nothing that is already stored, and a code that is no longer listed reads as no preference.
 */
@Singleton
class TrackLanguageStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TrackLanguagePreferences {

    private val prefs: SharedPreferences by lazy { context.getSharedPreferences(FILE, Context.MODE_PRIVATE) }

    override fun audio(): TrackLanguage? = TrackLanguage.of(prefs.getString(AUDIO, null))

    override fun subtitle(): TrackLanguage? = TrackLanguage.of(prefs.getString(SUBTITLE, null))

    override fun setAudio(language: TrackLanguage?) {
        prefs.edit().putString(AUDIO, language?.code).apply()
    }

    override fun setSubtitle(language: TrackLanguage?) {
        prefs.edit().putString(SUBTITLE, language?.code).apply()
    }

    private companion object {
        const val FILE = "castivio_track_languages"
        const val AUDIO = "audio"
        const val SUBTITLE = "subtitle"
    }
}
