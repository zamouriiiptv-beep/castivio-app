package com.castivio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.castivio.core.common.prefetch.DwellPrefetcher
import com.castivio.domain.SeriesEpisodes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Asks a show's provider for its episodes and details while the remote rests on its card on the series wall, so the page that
 * opens finds them kept (see [DwellPrefetcher]). A television's only: a handset has no resting before the press.
 *
 * Measured: a provider that took `ask 9.8 s · read 0.0 s` for a show it had not built yet, and was quick the second time. The
 * same [SeriesEpisodes.load] the show's page calls, so what it keeps is what the page draws.
 */
@HiltViewModel
class ShowPrefetchViewModel @Inject constructor(loader: SeriesEpisodes) : ViewModel() {

    private val prefetcher = DwellPrefetcher(viewModelScope) { seriesId -> loader.load(seriesId) }

    /** The remote has come to the card of [seriesId]. */
    fun focused(seriesId: String) = prefetcher.focused(seriesId)

    /** The remote has left the card of [seriesId]. */
    fun left(seriesId: String) = prefetcher.left(seriesId)
}
