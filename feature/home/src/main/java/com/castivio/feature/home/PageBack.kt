package com.castivio.feature.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.castivio.core.design.components.CastivioIconButton
import com.castivio.core.design.theme.CastivioTheme

/**
 * The way back of a film's page and of a show's: one arrow, one size, drawn by one function, so the two pages cannot
 * drift apart. Both pages put it in the same place -- at the end of the name's row, in the top corner opposite the
 * poster -- and the arrow follows the reader's direction, so in Arabic it stands at the left and points the other way.
 *
 * A handset's button is smaller than its 48dp floor, by the owner's leave: the way back is also the hardware Back, and
 * the page is not a place a thumb has to find it. A television keeps the remote's floor.
 */
@Composable
internal fun PageBack(onBack: () -> Unit, modifier: Modifier = Modifier) {
    CastivioIconButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(R.string.show_back),
        onClick = onBack,
        modifier = if (CastivioTheme.device.isTv) modifier else modifier.size(HANDSET_BACK),
    )
}

/** The way back's side on a handset: under the 48dp floor, which a television keeps. */
private val HANDSET_BACK = 40.dp
