package com.castivio.feature.home

import androidx.compose.runtime.Composable
import com.castivio.domain.SortOrder

/**
 * The band over every board -- the channels', the films', the shows' and the radio's, the subscription's and the world's --
 * written once. [BoardBand] lays out what is on it: the mark and the name, the field, the control for the order, the clock and
 * the way back. What a board's band carries is decided here, so a change to it -- a control added or taken off, the field moved --
 * is made in this function and every board has it. A board gives only what is its own: the words its field shows, what the field
 * searches, and which order the board is in.
 *
 * **The breadcrumb is off on every board that calls this.** The rail says which list is open and the band says the product's
 * name; the cell the breadcrumb stood in is kept, so the field starts where it always did. Catch-up, whose band has no field and
 * no way back of its own, calls [BoardBand] itself and keeps its breadcrumb.
 *
 * **The order control is the holder's to say, not the board's.** A board hands over the order it is in -- the holder's
 * `BrowseState.bandOrder`, which is the bouquets' on the live board and the rows' on the others -- and what a press does
 * (`BrowseViewModel.nextOrder`), and this draws it with [SortChip]. The world directory has no order to offer, and a control
 * that did nothing would be a promise, so its band has none.
 */
@Composable
internal fun BoardHeader(
    m: ChannelsMetrics,
    searchText: String,
    onSearch: (String) -> Unit,
    /** What the field says it searches: the board's own words, short -- the hint cannot wrap. */
    hint: String,
    /** The order the board is in, which the control names; null for a board with no order to offer, whose band has no control. */
    order: SortOrder?,
    /** A press on the control. */
    onOrder: () -> Unit = {},
    /** False where the board is being rearranged and there is nothing to narrow. */
    searchEnabled: Boolean = true,
    onSearchFocus: (Boolean) -> Unit = {},
    /**
     * Whether the field starts at the pane's leading edge -- the films', the shows' and the radio's -- or after the breadcrumb's
     * cell, as the channels' band always had it.
     */
    alignSearchToPane: Boolean = true,
) {
    val control: (@Composable () -> Unit)? = if (order == null) null else ({ SortChip(order, m, onOrder) })
    BoardBand(
        m = m,
        searchText = searchText,
        onSearch = onSearch,
        onSearchFocus = onSearchFocus,
        searchEnabled = searchEnabled,
        hint = hint,
        trailing = control,
        hideLeading = true,
        alignSearchToPane = alignSearchToPane,
    )
}
