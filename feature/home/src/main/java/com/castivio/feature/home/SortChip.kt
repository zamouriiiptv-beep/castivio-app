package com.castivio.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.castivio.core.design.components.CastivioChip
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.domain.SortOrder

/**
 * The control a board's band carries for its order -- "Sort: Provider" -- drawn by one function, so that the boards' bands are
 * one band and cannot part as the pages' arrows and hearts did. [BoardHeader] puts it between the field and the clock
 * ([BoardBand]'s `trailing`). What a press does is the holder's -- see `BrowseViewModel.nextOrder`: the films, the shows and
 * the radio go round four orders of their rows, and the channels go between two orders of their bouquets.
 */
@Composable
internal fun SortChip(order: SortOrder, m: ChannelsMetrics, onClick: () -> Unit) {
    CastivioChip(
        text = stringResource(R.string.browse_sort, stringResource(order.label)),
        onClick = onClick,
        icon = Icons.Rounded.Tune,
        labelStyle = castivioChipStyle(m.frame.fsChip),
        padH = m.controlPadH,
        padV = m.badgePadV,
        trailingIcon = Icons.Rounded.KeyboardArrowDown,
    )
}
