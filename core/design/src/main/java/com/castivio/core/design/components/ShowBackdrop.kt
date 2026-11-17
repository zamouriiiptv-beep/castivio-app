package com.castivio.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.castivio.core.common.artwork.ArtworkShown
import com.castivio.core.common.artwork.PosterSize
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Motion

/**
 * A title's wide picture, behind the whole of its page: a film's or a show's (`design/mockups/details-backdrop-ar.html`).
 *
 * **The whole screen, with no edge.** It used to be a box two thirds of the show's page wide, ending in a hard edge where
 * the page went on; the owner set the film's page and the show's side by side and chose one page over the picture.
 *
 * **The words never stand on the raw picture.** Over it lie the ground at [DIM], then a gradient that is deepest under
 * the text -- the end side, where the name, the facts and the plot run (the left in Arabic, the right in English) -- and
 * opens toward the poster at the start, and a fade that closes to the ground at the foot, so what follows (a show's seasons
 * and episodes) stands on the application's own ground. The very bright picture is the case the drawing proves it on.
 *
 * **The page never waits for it.** Nothing is drawn while it is on its way or when it failed: the ground is under it, so
 * the page is the page it always was. When it comes it fades in over [Motion.fast], or at once at the lowest motion level.
 *
 * **Asked for at a screen's size** ([PosterSize.forScreen]): `w1280` in place of TMDB's `original` of several megabytes.
 * Like [PosterArtwork] this file is where the loader is seen: a feature hands over a url and does not import Coil.
 */
@Composable
fun ShowBackdrop(
    url: String,
    modifier: Modifier = Modifier,
    /** Told how the picture came, once it is on screen: a title's page prints it in a development build. */
    onShown: ((ArtworkShown) -> Unit)? = null,
) {
    var failed by remember(url) { mutableStateOf(false) }
    val askedAt = remember(url) { android.os.SystemClock.elapsedRealtime() }
    var arrived by remember(url) { mutableStateOf(false) }
    if (failed) return
    val ground = CastivioTheme.colors.background
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val millis = backdropFadeMillis(CastivioTheme.motionLevel)
    val shown by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = if (millis == 0) snap() else tween(millis, easing = Motion.standard),
        label = "titleBackdrop",
    )
    Box(modifier.graphicsLayer { alpha = shown }) {
        AsyncImage(
            // A wide picture: asked for as a picture, and given a picture's limit and not a logo's.
            model = ImageRequest.Builder(LocalContext.current)
                .data(PosterSize.forScreen(url))
                .addHeader(ArtworkPolicy.KIND_HEADER, ArtworkPolicy.KIND_PICTURE)
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onSuccess = { success ->
                arrived = true
                onShown?.invoke(artworkShown(askedAt, success.result))
            },
            onError = { failed = true },
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(ground.copy(alpha = DIM)))
        Box(
            Modifier
                .fillMaxSize()
                // The gradient is deepest at its left edge. In Arabic the poster stands at the start (the right) and the
                // text at the left, so it stands as it is; in a left-to-right language the text is on the right, and it
                // is turned round.
                .then(if (rtl) Modifier else Modifier.graphicsLayer(scaleX = -1f))
                .background(
                    Brush.horizontalGradient(
                        0f to ground.copy(alpha = START_ALPHA),
                        START_HALF to ground.copy(alpha = HALF_ALPHA),
                        OPENS to ground.copy(alpha = OPEN_ALPHA),
                        1f to ground.copy(alpha = END_ALPHA),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(FOOT_BEGINS to ground.copy(alpha = 0f), 1f to ground)),
        )
    }
}

/** The gradient's stops, from the text's side to the poster's: deep under the words, open toward the poster. */
private const val START_ALPHA = 0.90f
private const val START_HALF = 0.40f
private const val HALF_ALPHA = 0.70f
private const val OPENS = 0.75f
private const val OPEN_ALPHA = 0.25f
private const val END_ALPHA = 0.05f

/** The ground over the whole picture, under the gradient. */
private const val DIM = 0.15f

/** Where the foot's fade to the ground begins, as a share of the height. */
private const val FOOT_BEGINS = 0.55f
