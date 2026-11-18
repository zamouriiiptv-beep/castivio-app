package com.castivio.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.castivio.domain.AutoLoadSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the films, the series and the radio are brought in by themselves (Settings → Library), between launches. On until
 * the viewer turns it off. `SharedPreferences`, read once, with the value held in a flow for `AutoLoads` to follow.
 */
@Singleton
class AutoLoadStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AutoLoadSetting {

    private val prefs: SharedPreferences by lazy { context.getSharedPreferences(FILE, Context.MODE_PRIVATE) }

    private val state by lazy { MutableStateFlow(prefs.getBoolean(KEY, true)) }

    override val enabled: Flow<Boolean> get() = state.asStateFlow()

    override fun set(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
        state.value = on
    }

    private companion object {
        const val FILE = "castivio_auto_load"
        const val KEY = "on"
    }
}
