package com.castivio.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.castivio.core.design.components.ButtonWeight
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.theme.CastivioTheme

/**
 * The favourite button of a film's page and of a show's: one button, drawn by one function, so that the two pages cannot
 * part again -- as they did in the colour of the heart. Marked, the heart is filled and takes the accent colour, and the
 * label and the button's surface stay as they were; not marked, it is an outline in the button's own ink.
 *
 * The words are the caller's, because "Remove from favourites" names a film on one page and a show on the other, and so
 * is what a press does.
 */
@Composable
internal fun PageFavorite(favorite: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CastivioButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        weight = ButtonWeight.Secondary,
        icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        iconTint = if (favorite) CastivioTheme.colors.secondary else null,
    )
}
