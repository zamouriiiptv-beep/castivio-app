package com.castivio.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.castivio.core.design.theme.CastivioTheme

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
