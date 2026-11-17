package com.castivio.core.design.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.castivio.core.common.artwork.ArtworkKick
import com.castivio.core.common.artwork.ArtworkMetrics
import com.castivio.core.common.artwork.PosterRetry
import com.castivio.core.common.artwork.ArtworkShown
import com.castivio.core.common.artwork.ArtworkSource
import com.castivio.core.common.artwork.PosterSize
import com.castivio.core.common.artwork.PrefetchWindow
import com.castivio.core.common.artwork.PosterTimeline
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.CastivioType
import com.castivio.core.design.theme.Radius
import com.castivio.core.design.theme.posterPlaceholderBrush
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A poster a provider shipped, drawn to fill the card it is in.
 *
 * The sibling of [ProviderArtwork] for the one place a picture is meant to *fill* its slot: a
 * poster is cropped to the card's 2:3, where a channel logo is fitted inside a box because cropping
 * would cut the mark. Like it, this file is where the image loader is seen -- a feature composes a
 * card and hands it a url, and does not import Coil.
 *
 * Nothing is drawn while the picture is on its way, or when it failed, or when there is no url:
 * the card's own placeholder is under it and stays the answer, so a poster that never arrives
 * leaves a card, not a hole -- and no stand-in claims to be the film's artwork.
 *
 * **A failure is not the end of it.** A poster is asked of a host the application does not control, often over a phone's
 * mobile data, and while a section's first load is running the connection is busy with a reply of tens of megabytes. A call
 * that gave up for want of a few seconds is not a picture that does not exist, and a card that drew nothing the first time and
 * was never asked again showed an empty frame for as long as it stayed on the screen. So a poster that failed is asked for
 * again -- after the waits [PosterRetry] gives it, and at once when [ArtworkKick] says the connection has been given back --
 * unless the host said, in so many words, that it will not: a 404 is not a thing that mends in ten seconds.
 */
@Composable
internal fun PosterArtwork(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** Called once the picture is on screen, so the card can take its placeholder away. */
    onLoaded: () -> Unit = {},
    /**
     * Asked for at a card's size ([PosterSize.forCard]) rather than at the size the provider chose. True for the cards of a
     * wall; false for a film's page, whose one large poster is drawn several times the card's size.
     */
    cardSize: Boolean = false,
    /** Told how the picture came, once it is on screen: a title's page prints it in a development build. */
    onShown: ((ArtworkShown) -> Unit)? = null,
) {
    val context = LocalContext.current
    val askedAt = remember(url) { android.os.SystemClock.elapsedRealtime() }
    // All of this is keyed to the url, because a cell of a lazy grid is recycled: a poster that failed must not hand
    // its failure to whichever title scrolls into its place next.
    //
    // How many times the picture has been asked for and failed; whether the last failure is one that will not change; and
    // whether the picture is, just now, a failure that nobody has asked about again.
    var failures by remember(url) { mutableStateOf(0) }
    var refused by remember(url) { mutableStateOf(false) }
    var failedNow by remember(url) { mutableStateOf(false) }

    // The card that failed waits, and asks again. Declared before the early return below, which would otherwise end the
    // composition before this was ever reached.
    LaunchedEffect(url, failedNow, failures, refused) {
        if (!failedNow || refused) return@LaunchedEffect
        val seen = ArtworkKick.count.value
        val wait = PosterRetry.waitBeforeRetry(failures)
        if (wait != null) {
            // The wait, or the kick, whichever comes first.
            withTimeoutOrNull(wait) { ArtworkKick.count.first { it != seen } }
        } else {
            // Tried as often as it is on its own: it waits to be told the connection is free.
            ArtworkKick.count.first { it != seen }
        }
        ArtworkMetrics.retried()
        failedNow = false
    }

    // Told to the poster timeline -- which holds nothing until a load of a debuggable build begins -- so that when a card asked and
    // when it had its picture can be set against what the connection was doing. The first time counts; a card asking again is one.
    SideEffect { PosterTimeline.cardAsked(url) }
    // And when the card leaves the screen, so that a card scrolled past and back waits from its return, and a card left empty is
    // counted as one. Keyed to the url: a recycled cell that shows another title has left the first.
    DisposableEffect(url) { onDispose { PosterTimeline.cardLeft(url) } }

    if (failedNow) return
    AsyncImage(
        model = pictureRequest(context, url, cardSize),
        contentDescription = null,
        contentScale = contentScale,
        onSuccess = { success ->
            PosterTimeline.cardShown(url)
            onShown?.invoke(artworkShown(askedAt, success.result))
            onLoaded()
        },
        onError = { error ->
            failures += 1
            refused = !PosterRetry.retryable(PosterRetry.statusOf(error.result.throwable.message))
            failedNow = true
        },
        modifier = modifier,
    )
}

/**
 * The request for a poster or a backdrop: the address, and a word on the request that gives it a picture's limit -- twenty
 * seconds, not a logo's six -- from the image loader's call factory, which takes the word off before the call is made.
 *
 * A card's poster is asked for at the card's size: the provider's 600-pixel or original picture was three times the bytes a
 * card draws, over the connection and on the processor the film list was arriving on, and a scrolled wall showed it late.
 */
private fun pictureRequest(context: Context, url: String, cardSize: Boolean): ImageRequest =
    ImageRequest.Builder(context)
        .data(if (cardSize) PosterSize.forCard(url) else PosterSize.forPage(url))
        // A title's page draws its large poster where the viewer has just pressed its card: the card's picture is in the
        // loader's memory under its own address, and is drawn in the frame at once until the large one has come. A memory
        // miss -- a page reached without its card, an episode's still -- draws nothing, as before.
        .apply { if (!cardSize) placeholderMemoryCacheKey(PosterSize.forCard(url)) }
        .addHeader(ArtworkPolicy.KIND_HEADER, ArtworkPolicy.KIND_PICTURE)
        .crossfade(false)
        .build()

/**
 * A poster's own colours, as the ground it stands on: the same picture asked for at a few dozen pixels and
 * drawn to fill the frame, so the loader hands back a small bitmap and the enlargement is what blurs it.
 *
 * Where a poster is fitted whole into a frame wider than itself, the bare ground beside it is a hole in the
 * card. This fills it with the film's own colours, the way a cinema's lobby card would, and the poster
 * stands on it uncropped. It is asked for **after** the poster itself is on screen -- the caller draws it
 * then -- so the original is already in the disk cache and this is a decode of a file, not a second
 * download of the same picture.
 *
 * Not the platform's blur: `Modifier.blur` does nothing under Android 12, which is most television boxes,
 * and costs a render pass per card where this costs a bitmap the size of a thumbnail. Nothing is drawn when
 * the picture fails; the card's own backdrop is under it.
 */
@Composable
internal fun PosterBackdrop(
    url: String,
    modifier: Modifier = Modifier,
    /** Called once the small picture is on screen, so the card can bring the backdrop in. */
    onLoaded: () -> Unit = {},
) {
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) return
    AsyncImage(
        model = backdropRequest(LocalContext.current, url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { onLoaded() },
        onError = { failed = true },
        modifier = modifier,
    )
}

/**
 * The one request for a backdrop, so the card and [PosterPrefetch] ask for it in the same words and share an answer. At the
 * card's size, like the card's poster, so the file it brings into the disk cache is the one the card then reads.
 */
private fun backdropRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(PosterSize.forCard(url))
        // The disk's file is the card's (the same address), but the bitmap in memory is 24 by 36 and must not stand under
        // the card's own key, where a title's page looks for the card's picture to draw while its large one comes.
        .memoryCacheKey(PosterSize.forCard(url) + BACKDROP_KEY_SUFFIX)
        // It brings the whole picture into the disk cache on the way, so it is asked for as a picture and given a picture's limit.
        .addHeader(ArtworkPolicy.KIND_HEADER, ArtworkPolicy.KIND_PICTURE)
        .size(BACKDROP_WIDTH, BACKDROP_HEIGHT)
        .precision(Precision.EXACT)
        .scale(Scale.FILL)
        .crossfade(false)
        .build()

/**
 * Asks for the posters just below a wall before the cards reach them -- **while the wall scrolls**, and not only once it is still.
 *
 * The window is the [ahead] posters after the last card drawn ([PrefetchWindow]). As the wall moves, the posters that came into
 * the window are asked for and the ones that left it are let go: a poster whose card has come on screen is that card's to fetch,
 * and one the viewer has scrolled back past is not wanted. So no more than the window is ever in flight, however fast the thumb
 * goes, and a card on screen never queues behind posters for cards that are not.
 *
 * It used to ask only after the wall had been still for a quarter of a second, and to cancel everything the moment a scroll began,
 * so that a request for a row the thumb was about to pass would not stand in front of the cards on screen. That was the right
 * trade for a poster of a hundred kilobytes and more; at the card's size ([PosterSize.forCard], 44 KB median) the window costs
 * little, and a scrolling viewer -- the one the wall is for -- was the one it left with nothing ahead: every card asked for its own
 * poster as it arrived, and while a section's first load was running each ask waited 0.72 s (median), 2.10 s (p90) for its first
 * byte, against 0.16 s once the list had come.
 *
 * The request is the backdrop's, which is the small one, but it brings the whole picture into the disk cache on the way, and the
 * poster is then read from there. A poster already fetched is not asked for again while it stays in the window.
 *
 * The feature hands over its list as two lookups and does not see the loader. [itemCount] bounds the lookup, which is
 * how many items the list holds now; [urlAt] answers the poster's address without loading anything.
 */
@Composable
fun PosterPrefetch(
    state: LazyGridState,
    ahead: Int,
    itemCount: () -> Int,
    urlAt: (Int) -> String?,
) {
    val context = LocalContext.current
    val count by rememberUpdatedState(itemCount)
    val lookup by rememberUpdatedState(urlAt)
    LaunchedEffect(state, ahead) {
        val loader = context.imageLoader
        // Every position asked for and still in the window, finished or not: a finished one is in the cache, and is not asked
        // for again until it has left the window.
        val asked = HashMap<Int, Disposable>()
        try {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                .distinctUntilChanged()
                .collect { last ->
                    val plan = PrefetchWindow.plan(asked.keys, last, ahead, count())
                    for (index in plan.drop) asked.remove(index)?.dispose()
                    for (index in plan.ask) {
                        val url = lookup(index) ?: continue
                        asked[index] = loader.enqueue(backdropRequest(context, url))
                    }
                }
        } finally {
            asked.values.forEach { it.dispose() }
            asked.clear()
        }
    }
}

/**
 * The positions of the posters to ask for: the [ahead] after [lastShown], and none past the end of a list of [count].
 *
 * Empty when nothing is shown yet -- [lastShown] is -1 -- because there is no "after" to measure from, and the cards
 * on screen ask for themselves.
 */
internal fun postersAhead(lastShown: Int, ahead: Int, count: Int): IntRange = PrefetchWindow.window(lastShown, ahead, count)

/** What the small backdrop's bitmap is kept in memory under, apart from the card's own picture: see [backdropRequest]. */
private const val BACKDROP_KEY_SUFFIX = "#backdrop"

/**
 * The size the backdrop is asked for, in pixels, at the poster's own 2:3. Small enough that enlarging it to a
 * card reads as a blur and not as a picture: at 16 the shapes of a poster's lettering were still legible
 * in the preview, at 24 they are soft colour, and 32 is the most that still reads as one.
 */
private const val BACKDROP_WIDTH = 24
private const val BACKDROP_HEIGHT = 36

/**
 * A poster at any size, framed: the placeholder, the provider's picture over it, and the card's own edge.
 *
 * What a film's page draws its one large poster with -- the same picture, ratio and edge as the card in
 * the grid it was opened from, so the page reads as that card come closer. The caller sets the size
 * and the ratio; [seed] picks the placeholder, as the card's position does.
 */
@Composable
fun PosterFrame(url: String?, seed: Int, modifier: Modifier = Modifier, onShown: ((ArtworkShown) -> Unit)? = null) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier
            .clip(shape)
            .background(posterPlaceholderBrush(seed))
            .border(1.dp, CastivioTheme.colors.glassBorderSoft, shape),
    ) {
        if (!url.isNullOrBlank()) PosterArtwork(url, Modifier.fillMaxSize(), onShown = onShown)
    }
}

/**
 * What a development build is told of a picture a title's page drew: how long after [askedAt] it came, from where, and at what
 * size it was decoded (`ArtworkShownLine`). Read from Coil's own answer.
 */
internal fun artworkShown(askedAt: Long, result: coil.request.SuccessResult): ArtworkShown {
    val now = android.os.SystemClock.elapsedRealtime()
    return artworkShownAt(now, askedAt, result)
}

private fun artworkShownAt(now: Long, askedAt: Long, result: coil.request.SuccessResult): ArtworkShown = ArtworkShown(
    millis = now - askedAt,
    shownAt = now,
    source = when (result.dataSource) {
        coil.decode.DataSource.MEMORY_CACHE, coil.decode.DataSource.MEMORY -> ArtworkSource.MEMORY
        coil.decode.DataSource.DISK -> ArtworkSource.DISK
        coil.decode.DataSource.NETWORK -> ArtworkSource.NETWORK
    },
    width = result.drawable.intrinsicWidth,
    height = result.drawable.intrinsicHeight,
)


/**
 * One short fact on a poster's lower edge: a running time.
 *
 * Over a picture of any brightness, so it carries its own dim ground -- the scrim, held back a
 * little from black -- and nothing else. Drawn where the card says to, and not at all when the
 * provider shipped no fact to put in it. A rating is not one of these: it has its own mark,
 * [PosterRating], on the same ground.
 */
@Composable
internal fun PosterTag(text: String, modifier: Modifier = Modifier) {
    val colors = CastivioTheme.colors
    Text(
        text = text,
        style = CastivioType.labelSmall,
        color = colors.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .posterMarkGround(colors.scrim)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * A film's rating on its poster: the gold star and the figure, at the corner the card's other marks leave free.
 *
 * The same pill as [PosterTag] -- the same dim ground, the same 11sp label, the same inset from the poster's edge
 * -- so a card has one kind of mark on its artwork, and the gold is the film page's own: a rating is the one
 * thing gold means in the product, so the figure reads as the page's rating without carrying the page's whole
 * badge, which would take two thirds of a poster's width. The star is decoration: the card says the rating in
 * words, and a reader that met the star as well would say it twice.
 *
 * [rating] arrives formatted -- `7.1` -- by the caller, who owns the digits, as the film page's badge does.
 */
@Composable
internal fun PosterRating(rating: String, modifier: Modifier = Modifier) {
    val colors = CastivioTheme.colors
    Row(
        modifier
            .posterMarkGround(colors.scrim)
            .padding(start = RATING_START_PAD, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RATING_GAP),
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(RATING_STAR))
        Text(
            text = rating,
            style = CastivioType.labelSmall,
            color = colors.onBackground,
            maxLines = 1,
        )
    }
}

/** The ground every mark on a poster stands on: a dim pill, so it reads over a picture of any brightness. */
private fun Modifier.posterMarkGround(scrim: Color): Modifier =
    this
        .clip(RoundedCornerShape(Radius.pill))
        .background(scrim.copy(alpha = TAG_GROUND))

/** How dark the tag's ground is: enough to read over a white poster, and still a tint, not a plate. */
private const val TAG_GROUND = 0.78f

/** The rating's star, a little larger than the label's capital height so the gold reads at a handset's size. */
private val RATING_STAR = 12.dp

/** The star's side of the pill is padded less than the figure's: the star's own box has air in it. */
private val RATING_START_PAD = 6.dp

/** The air between the star and the figure. */
private val RATING_GAP = 3.dp
