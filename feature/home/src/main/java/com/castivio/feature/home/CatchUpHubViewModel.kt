package com.castivio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.castivio.domain.ArchiveGroup
import com.castivio.domain.CatalogPager
import com.castivio.domain.CatalogQuery
import com.castivio.domain.CatalogRepository
import com.castivio.domain.CatchUpGateway
import com.castivio.domain.Channel
import com.castivio.domain.MediaKind
import com.castivio.domain.searchTermOf
import com.castivio.domain.time.TrustedTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Where the viewer is in Catch-up: two places, and Back leaves the second for the first.
 *
 * The first page is three columns (owner, 2026-11-26, design/mockups/catchup-19-days-column-ar.png): the bouquets, the open
 * bouquet's channels, and the chosen channel's days. A day opens the second place, that day's programmes beside the channels.
 */
sealed interface HubStage {

    /** The three columns: [group] is the open bouquet (null is "all"), [channel] the one whose days are shown, if any. */
    data class Browse(val group: ArchiveGroup? = null, val channel: Channel? = null) : HubStage

    /** One channel's programmes, beside its bouquet's channels: the pane [CatchUpProgrammes] draws. */
    data class Programmes(val group: ArchiveGroup?, val channel: Channel) : HubStage
}

/** What the first page lists: the categories that hold a channel with an archive, and how many such channels there are. */
data class HubGroups(val groups: List<ArchiveGroup>) {
    val total: Int get() = groups.sumOf { it.channels }
}

/**
 * Catch-up as a place of its own: the categories that keep an archive, a category's channels, and -- through
 * [CatchUpProgrammes] -- a channel's programmes.
 *
 * ## What it reads, and how much
 *
 * The categories are one aggregate over the live rows, with their counts: hundreds of rows at most, read whole like the
 * rail's. The channels are the catalogue's own pages -- [CatalogQuery.archive] -- a window at a time, so "all" of a
 * subscription that has hundreds of them is read the way every other list is, and never whole. Nothing here asks a
 * provider for anything: that is [CatchUpViewModel]'s, and only when a channel's programmes are opened.
 *
 * ## A channel the provider claims and does not keep
 *
 * A provider can mark a channel as keeping an archive and list nothing for it: a channel whose listing the gateway
 * remembers as empty is left out of the list the next time the list is read. A single programme the provider has no
 * recording of is not that. It is the player's card and nothing more: the channel stays, and its other programmes may play.
 *
 * ## The search
 *
 * One box, in every place there is a list. On the first page it looks for categories and channels together -- the
 * categories by their names, in memory, and the channels through the index, a window at a time like every list; inside a
 * category it looks among that category's channels. Typing is the search: the words are folded, held back until the viewer
 * pauses and a superseded question is cancelled, as the channels' own are. Back closes it before it leaves the page.
 *
 * One instance for the activity, as the other screens' are: the place the viewer is in is still there when a programme's
 * player closes over it.
 */
@HiltViewModel
class CatchUpHubViewModel @Inject constructor(
    catalog: CatalogRepository,
    private val pager: CatalogPager,
    private val gateway: CatchUpGateway,
    private val clock: TrustedTime,
) : ViewModel() {

    private val _stage = MutableStateFlow<HubStage>(HubStage.Browse())
    val stage: StateFlow<HubStage> = _stage.asStateFlow()

    /** Null until the first answer: the page says it is loading, and a null is not an empty answer. */
    val groups: StateFlow<HubGroups?> = catalog.archiveGroups()
        .map { HubGroups(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /**
     * Whether the active subscription is of a kind that has an archive -- see [CatchUpGateway.supported]. True until it is
     * known, so the page never says a subscription offers nothing before it has asked. Read again each time the page is
     * shown, since the active subscription can be another one.
     */
    val supported: StateFlow<Boolean> = flow { emit(gateway.supported()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), true)

    /** What the search box holds, exactly as typed: read straight off the flow, so the field never lags the keys. */
    private val typed = MutableStateFlow("")

    val searchText: StateFlow<String> = typed.asStateFlow()

    /** What the lists are narrowed by: [typed], folded, held back until the viewer pauses; empty is no search. */
    @OptIn(FlowPreview::class)
    private val term: Flow<String> = typed
        .map(::searchTermOf)
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else CATCHUP_SEARCH_PAUSE_MS }
        .distinctUntilChanged()

    /** What the bouquets' own box holds, exactly as typed. */
    private val groupTyped = MutableStateFlow("")

    val groupSearchText: StateFlow<String> = groupTyped.asStateFlow()

    /**
     * The bouquets the left column lists: all of them, or those whose names match what is typed in its box. Null until
     * the first answer. Read in memory -- there are hundreds at most -- so no pause is needed.
     */
    val shownGroups: StateFlow<HubGroups?> = combine(groups, groupTyped) { all, typedNow ->
        val words = searchTermOf(typedNow)
        if (all == null || words.isEmpty()) all else HubGroups(matchingGroups(all.groups, words))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /**
     * The channels of the category that is open, with the ones known to have nothing archived left out.
     *
     * Keyed to the **category and the search**, not to the stage: opening a channel's programmes over the list does not read
     * the list again, and Back lands on the same rows.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: Flow<PagingData<Channel>> = combine(_stage, term) { stage, words -> keyOf(stage, words) }
        .distinctUntilChanged()
        .flatMapLatest { key ->
            if (key == null) {
                emptyFlow()
            } else {
                pager.items(
                    CatalogQuery(kind = MediaKind.LIVE, groupId = key.groupId, search = key.term, archive = true),
                ).map { data ->
                    data.filter { it is Channel && !gateway.knownEmpty(it.id, clock.nowMs()) }
                        .map { it as Channel }
                }
            }
        }
        .cachedIn(viewModelScope)

    /** The list a place reads: the open bouquet's channels, narrowed by the channels' search -- the same in both places. */
    private fun keyOf(stage: HubStage, words: String): ListKey? = when (stage) {
        is HubStage.Browse -> ListKey(stage.group?.id, words)
        is HubStage.Programmes -> ListKey(stage.group?.id, words)
    }

    /** Another bouquet: its channels, with no channel chosen and nothing typed among the channels. */
    fun openGroup(group: ArchiveGroup?) {
        typed.value = ""
        _stage.value = HubStage.Browse(group)
    }

    /** A channel: its days in the third column, or -- beside the programmes -- its programmes. */
    fun openChannel(channel: Channel) {
        _stage.value = when (val at = _stage.value) {
            is HubStage.Browse -> at.copy(channel = channel)
            is HubStage.Programmes -> if (at.channel.id == channel.id) return else at.copy(channel = channel)
        }
    }

    /** A day of the chosen channel: its programmes. Nothing happens with no channel chosen. */
    fun openDay() {
        val at = _stage.value as? HubStage.Browse ?: return
        val channel = at.channel ?: return
        _stage.value = HubStage.Programmes(at.group, channel)
    }

    /** The viewer typed in the channels' box, in the band. */
    fun search(text: String) {
        typed.value = text
    }

    /** The viewer typed in the bouquets' box. */
    fun searchGroups(text: String) {
        groupTyped.value = text
    }

    /**
     * One step back: from the programmes to the three columns, the channel still chosen; on the columns a search that holds
     * something is closed first. False when there is nothing left to close: the caller leaves Catch-up.
     */
    fun back(): Boolean {
        when (val at = _stage.value) {
            is HubStage.Programmes -> _stage.value = HubStage.Browse(at.group, at.channel)
            is HubStage.Browse -> when {
                typed.value.isNotEmpty() -> typed.value = ""
                groupTyped.value.isNotEmpty() -> groupTyped.value = ""
                else -> return false
            }
        }
        return true
    }

    /** The viewer left Catch-up: the next time begins at "all", with nothing chosen and nothing typed. */
    fun reset() {
        typed.value = ""
        groupTyped.value = ""
        _stage.value = HubStage.Browse()
    }

    private data class ListKey(val groupId: String?, val term: String)

    private companion object {
        /** How long the categories are kept after the page is left, for a quick return. */
        const val STOP_MS = 5_000L

    }
}
