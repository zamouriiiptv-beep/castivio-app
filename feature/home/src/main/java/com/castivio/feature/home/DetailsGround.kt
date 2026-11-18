package com.castivio.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Radius

/**
 * The ground a title's page stands its small things on -- the facts bar, the favourite, the rating, the date -- over the
 * wide picture: a dark, near-opaque one, no longer the thin glass that let a bright picture through
 * (design/mockups/details-template-ar.html). Black at [DETAILS_GROUND_ALPHA], one value for the film's page and the show's.
 */
@Composable
@ReadOnlyComposable
internal fun detailsGround(): Color = CastivioTheme.colors.scrim.copy(alpha = DETAILS_GROUND_ALPHA)

/** How opaque the details' small grounds are. */
internal const val DETAILS_GROUND_ALPHA = 0.72f

/**
 * A small fact of a title's page on the details' dark ground: the day the provider added it ("أُضيف في ..."), drawn as the
 * film page draws it, for the show's page and its information screen.
 */
@Composable
internal fun DetailsDateChip(text: String, m: CatalogMetrics) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.pill)
    val iconSize = m.frame.fsBody * DATE_ICON_OF_BODY
    Row(
        Modifier
            .clip(shape)
            .background(detailsGround())
            .border(1.dp, colors.glassBorderSoft, shape)
            .padding(horizontal = m.entryPadH, vertical = m.entryPadV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(m.rowGap / 2),
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = colors.onBackgroundVariant, modifier = Modifier.size(iconSize))
        Text(text = text, style = castivioChipStyle(m.frame.fsChip), color = colors.onBackground, maxLines = 1)
    }
}

/** The calendar mark's size, as a share of the page's body type: the film page's facts' own. */
private const val DATE_ICON_OF_BODY = 1.25f
