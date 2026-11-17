package com.castivio.feature.home

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.castivio.core.common.artwork.ArtworkShown
import com.castivio.core.common.artwork.PageOpen
import com.castivio.core.common.artwork.PageOpenLine
import com.castivio.core.common.artwork.PageOpenTimes
import com.castivio.core.design.components.RatingBadge
import com.castivio.core.design.components.ButtonWeight
import com.castivio.core.design.components.CardShape
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.components.CastivioIconButton
import com.castivio.core.design.components.CastivioNoticeDialog
import com.castivio.core.design.components.EmptyState
import com.castivio.core.design.components.MediaCard
import com.castivio.core.design.components.PosterFrame
import com.castivio.core.design.components.ShowBackdrop
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.components.castivioTitleStyle
import com.castivio.core.design.components.ltrToken
import com.castivio.core.design.components.startEdgeAlign
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Radius
import com.castivio.core.design.theme.Sizing
import com.castivio.core.design.theme.castivioBackdrop
import com.castivio.core.design.theme.castivioStage
import com.castivio.core.platform.PerformanceLog
import com.castivio.domain.Episode
import com.castivio.domain.EpisodeMark
import com.castivio.domain.RequestTimes
import com.castivio.domain.Season
import com.castivio.domain.SeriesSummary
import com.castivio.domain.externalIdsLine
import kotlinx.coroutines.launch

/**
 * One show's page: a rail of its sections beside one column that scrolls.
 *
 * The column opens on the show -- its poster, name, a row of facts (year, genre, rating, seasons), the plot, and the
 * two buttons, Play and the favourite -- over the provider's wide picture of it when it sent one. Under that, the
 * season chips with how many episodes each holds, and the chosen season's episodes: number, the provider's still, name,
 * a line of its plot, and its running time. Then the cast, and other shows of the same category. The rail names these
 * parts on a television and keeps their icons on a handset, takes the viewer to one with a press, and lights the one
 * the page has reached.
 *
 * The way back is the film page's, and no part of the rail: the same arrow, drawn by [PageBack], at the end of the
 * show's name row, in the top corner opposite the poster.
 *
 * Play continues the show from the episode last started, or starts its first. A press on an episode plays it.
 *
 * Nothing is drawn that the provider did not send, and nothing is made up. The first of what it says arrives after the
 * page is up, with the episodes, so until the provider has answered a quiet placeholder stands where the facts, the
 * plot and the episodes' plots will be; once it has, only what came is drawn, and a section with nothing in it is not
 * in the rail.
 *
 * It follows the language's direction, as the film's page does: in Arabic the rail stands at the right, the poster and the
 * words begin at the right, and every row begins where the language does. The provider's own words -- the show's and the
 * episodes' names, the plot, the people -- are not the page's language and are not turned round: each keeps its order and is
 * set against the start edge ([startEdgeAlign]), and the figures are left-to-right tokens.
 */
@Composable
fun ShowScreen(
    show: SeriesSummary,
    onPlay: (CatalogSelection) -> Unit,
    onBack: () -> Unit,
    onOpenShow: (SeriesSummary) -> Unit,
    modifier: Modifier = Modifier,
    model: ShowViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(show.seriesId) { model.open(show.seriesId) }
    // The player has been on top of this page, or has not: either way, what was watched is read again.
    LaunchedEffect(Unit) { model.reread() }

    val held by model.state.collectAsStateWithLifecycle()
    // The model keeps the state of the show it held last, and a page opened after another is first handed that one:
    // it draws the show it was given and nothing of the other, from the first frame, until its own state arrives.
    val state = if (held.seriesId == show.seriesId) held else ShowState(asking = true)
    val target = state.target
    // Whether this APK may print what the request cost: debuggable, as for the failure reason -- a property of the package.
    val context = LocalContext.current
    val instrumented = remember(context) { PerformanceLog.isVisibleIn(context) }

    // The season the viewer picked, else the one the next episode is in, else the first.
    var picked by remember(show.seriesId) { mutableStateOf<Int?>(null) }
    val season = state.seasons.firstOrNull { it.number == (picked ?: target?.episode?.seasonNumber) }
        ?: state.seasons.firstOrNull()

    val playFocus = remember { FocusRequester() }
    LaunchedEffect(target != null) { if (target != null) runCatching { playFocus.requestFocus() } }

    val start: (Episode) -> Unit = { episode ->
        episode.asSelection()?.let { selection ->
            model.started(episode.id)
            onPlay(selection)
        }
    }

    // The plot, whole: a link under it and a small dialog, offered only where the page had to cut it.
    var plotOpen by remember(show.seriesId) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val details = state.details
    // The page's opening, counted from the press that asked for it (`PageOpen`) -- or from its first drawing when it was
    // reached another way: the page on screen, the details, the two pictures and the episodes. A development build only.
    val firstDrawn = remember(show.seriesId) { SystemClock.elapsedRealtime() }
    val pressedAt = remember(show.seriesId) { PageOpen.take(firstDrawn) }
    val openStart = pressedAt ?: firstDrawn
    var openMs by remember(show.seriesId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(show.seriesId) {
        if (instrumented && pressedAt != null) {
            withFrameNanos { }
            openMs = SystemClock.elapsedRealtime() - pressedAt
        }
    }
    var detailsMs by remember(show.seriesId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(show.seriesId, details != null) {
        if (instrumented && details != null && detailsMs == null) detailsMs = SystemClock.elapsedRealtime() - openStart
    }
    var episodesMs by remember(show.seriesId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(show.seriesId, state.seasons.isNotEmpty()) {
        if (instrumented && state.seasons.isNotEmpty() && episodesMs == null) {
            episodesMs = SystemClock.elapsedRealtime() - openStart
        }
    }
    var posterShown by remember(show.seriesId) { mutableStateOf<ArtworkShown?>(null) }
    var backdropShown by remember(show.seriesId) { mutableStateOf<ArtworkShown?>(null) }
    val sections = showSections(
        hasSeasons = state.seasons.isNotEmpty(),
        episodes = season?.episodes?.size ?: 0,
        hasCast = details?.cast?.isNotEmpty() == true,
        hasSimilar = state.similar.isNotEmpty(),
    )
    val lit = litSection(sections, listState.firstVisibleItemIndex, atEnd = !listState.canScrollForward)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            // Drawn over the destination underneath it, so it brings the application's own ground.
            .castivioBackdrop()
            .statusBarsPadding(),
    ) {
        val m = catalogMetricsFor(tv = CastivioTheme.device.isTv, width = maxWidth, height = maxHeight)
        // Read here: the constraints' own scope is a layout scope, and the lists below are others.
        val surfaceHeight = maxHeight
        val tv = CastivioTheme.device.isTv
        val railWidth = if (tv) (maxWidth * RAIL_OF_WIDTH).coerceIn(MIN_RAIL, MAX_RAIL) else HANDSET_RAIL
        val posterHeight = (surfaceHeight * POSTER_OF_HEIGHT).coerceIn(MIN_POSTER, MAX_POSTER)
        // The wall's metrics, for the poster cards of the similar shows: the same card, the same sizes.
        val board = channelsMetricsFor(tv = tv, width = maxWidth, height = maxHeight)

        // The show's wide picture behind the whole page, under its scrim: see `ShowBackdrop` and
        // design/mockups/details-backdrop-ar.html. Nothing until it comes; the page never waits for it.
        details?.backdropUrl?.let { url -> ShowBackdrop(url, Modifier.fillMaxSize(), onShown = { backdropShown = it }) }

        Row(
            Modifier.fillMaxSize().castivioStage(m.frame),
            horizontalArrangement = Arrangement.spacedBy(m.paneGap),
        ) {
            ShowRail(
                sections = sections.map { it.section },
                lit = lit,
                tv = tv,
                m = m,
                onSection = { section ->
                    val at = sections.firstOrNull { it.section == section } ?: return@ShowRail
                    scope.launch { listState.animateScrollToItem(at.index) }
                },
                modifier = Modifier.width(railWidth).fillMaxHeight(),
            )
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(m.rowGap),
                contentPadding = PaddingValues(bottom = m.listBottom),
            ) {
                item(key = "top") {
                    ShowTop(
                        show = show,
                        state = state,
                        m = m,
                        tv = tv,
                        posterHeight = posterHeight,
                        playFocus = playFocus,
                        onPlay = { target?.let { start(it.episode) } },
                        onFavorite = model::toggleFavorite,
                        onMorePlot = { plotOpen = true },
                        onBack = onBack,
                        onPosterShown = { posterShown = it },
                    )
                }
                if (instrumented) {
                    PageOpenLine.describe(
                        PageOpenTimes(
                            open = openMs,
                            details = detailsMs,
                            detailsPending = details == null && state.asking,
                            episodes = episodesMs,
                            episodesPending = state.seasons.isEmpty() && state.load == ShowLoad.Loading,
                            poster = posterShown?.since(openStart),
                            posterAsked = !show.artworkUrl.isNullOrBlank(),
                            backdrop = backdropShown?.since(openStart),
                            backdropAsked = details?.backdropUrl != null,
                        ),
                    )?.let { line ->
                        item(key = "pictures") {
                            Text(
                                text = line,
                                style = castivioBodyStyle(m.frame.fsLabel),
                                color = CastivioTheme.colors.onBackgroundMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
                state.times?.takeIf { instrumented }?.let { cost ->
                    // With the ids the provider gave the show, once its details are here -- see `RequestTimesLine`.
                    val ids = state.details?.let { externalIdsLine(it.tmdbId, it.imdbId) }
                    item(key = "times") { RequestTimesLine(cost, ids, m) }
                }
                if (state.seasons.isEmpty()) {
                    item(key = "placeholder") {
                        EpisodesPlaceholder(
                            load = state.load,
                            preparing = state.preparing,
                            failure = state.failure,
                            m = m,
                            onRetry = model::retry,
                            onBack = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item(key = "episodes-heading") {
                        val episodes = season?.episodes.orEmpty()
                        val panelShape = RoundedCornerShape(Radius.md)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(panelShape)
                                .background(CastivioTheme.colors.glassFill)
                                .border(1.dp, CastivioTheme.colors.glassBorderSoft, panelShape)
                                .padding(horizontal = m.cardPad, vertical = m.rowGap),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SectionTitle(plural(R.plurals.show_episodes, episodes.size), m)
                            // A show of one season has nothing to choose: it is named, and is no control that opens a list of one.
                            if (state.seasons.size > 1) {
                                SeasonSelector(state.seasons, season, m, tv) { picked = it }
                            } else {
                                SeasonLabel(season, m)
                            }
                        }
                    }
                    items(season?.episodes.orEmpty(), key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            mark = state.marks[episode.id] ?: EpisodeMark.None,
                            plot = state.plots[episode.id],
                            waiting = state.asking && state.plots.isEmpty(),
                            m = m,
                            onStart = start,
                        )
                    }
                }
                details?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                    item(key = "cast") { CastSection(cast, m) }
                }
                if (state.similar.isNotEmpty()) {
                    item(key = "similar") { SimilarSection(state.similar, m, board, surfaceHeight, onOpenShow) }
                }
            }
        }

        // The dialog is the application's own, and follows its language as the page does -- the plot inside it takes the
        // direction of its words and is set against the start edge.
        if (plotOpen) {
            CastivioNoticeDialog(
                title = show.title,
                body = details?.plot.orEmpty(),
                closeLabel = stringResource(R.string.movie_plot_close),
                onClose = { plotOpen = false },
                maxFraction = PLOT_DIALOG_FRACTION,
            )
        }
    }
}

/** The rail: the page's sections, the one the page is at lit. The way back is not in it: see [PageBack]. */
@Composable
private fun ShowRail(
    sections: List<ShowSection>,
    lit: ShowSection,
    tv: Boolean,
    m: CatalogMetrics,
    onSection: (ShowSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(RAIL_GAP)) {
        for (section in sections) {
            val label = stringResource(section.label)
            RailEntry(
                icon = section.icon,
                label = label,
                m = m,
                showLabel = tv,
                selected = section == lit,
                onClick = { onSection(section) },
            )
        }
    }
}

private val ShowSection.label: Int
    get() = when (this) {
        ShowSection.Overview -> R.string.show_nav_overview
        ShowSection.Seasons -> R.string.show_nav_seasons
        ShowSection.Cast -> R.string.show_nav_cast
        ShowSection.Similar -> R.string.show_nav_similar
    }

private val ShowSection.icon: ImageVector
    get() = when (this) {
        ShowSection.Overview -> Icons.Rounded.Info
        ShowSection.Seasons -> Icons.Rounded.VideoLibrary
        ShowSection.Cast -> Icons.Rounded.Person
        ShowSection.Similar -> Icons.Rounded.ThumbUp
    }

/** One section in the rail: its icon, and on a television its name; violet when the page is at it. */
@Composable
private fun RailEntry(icon: ImageVector, label: String, m: CatalogMetrics, showLabel: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    var focused by remember { mutableStateOf(false) }
    val tv = CastivioTheme.device.isTv
    Row(
        Modifier
            .then(if (showLabel) Modifier.fillMaxWidth() else Modifier.size(HANDSET_ENTRY))
            .height(if (tv) Sizing.minTarget(true) else HANDSET_ENTRY)
            .clip(shape)
            .background(if (selected) colors.selectedFill else colors.glassFill.copy(alpha = 0f))
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> colors.focusRing
                    selected -> colors.selectedBorder
                    else -> colors.glassFill.copy(alpha = 0f)
                },
                shape,
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (showLabel) RAIL_PAD else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showLabel) Arrangement.spacedBy(RAIL_PAD) else Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = if (showLabel) null else label,
            tint = if (selected) colors.onBackground else colors.onBackgroundVariant,
            modifier = Modifier.size(RAIL_ICON),
        )
        if (showLabel) {
            Text(
                text = label,
                style = castivioChipStyle(m.frame.fsLabel),
                color = if (selected) colors.onBackground else colors.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The show: its poster and name, the facts, the plot, and the buttons, over the provider's wide picture. */
@Composable
private fun ShowTop(
    show: SeriesSummary,
    state: ShowState,
    m: CatalogMetrics,
    tv: Boolean,
    posterHeight: Dp,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onMorePlot: () -> Unit,
    onBack: () -> Unit,
    onPosterShown: (ArtworkShown) -> Unit,
) {
    val colors = CastivioTheme.colors
    val details = state.details
    // The provider has not answered yet, and may have something to say: quiet bars stand where it would go.
    val waiting = state.asking && details == null
    val target = state.target
    var plotCut by remember(details?.plot) { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        val heroShape = RoundedCornerShape(Radius.md)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .padding(horizontal = m.cardPad, vertical = m.rowGap),
            horizontalArrangement = Arrangement.spacedBy(m.paneGap),
        ) {
            PosterFrame(
                url = show.artworkUrl,
                seed = 0,
                modifier = Modifier.height(posterHeight).aspectRatio(CATALOG_POSTER_ASPECT),
                onShown = onPosterShown,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(m.rowGap)) {
                // The name, and at the end of the same row the way back: the film page's arrow, in the film page's
                // corner -- opposite the poster, at the top of the page.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(m.rowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = show.title,
                        style = castivioTitleStyle(m.frame.fsTitle * TITLE_OF_PANE),
                        color = colors.onBackground,
                        maxLines = SHOW_TITLE_LINES,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = startEdgeAlign(show.title),
                        modifier = Modifier.weight(1f),
                    )
                    PageBack(onBack)
                }
                ShowFacts(state, waiting, m)
                // The film page's order: the facts, the two buttons, then the plot (details-backdrop-ar.html).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(m.rowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CastivioButton(
                        text = when {
                            target == null -> stringResource(R.string.show_play)
                            target.resume -> stringResource(R.string.show_continue, episodeText(target.episode))
                            else -> stringResource(R.string.show_play_episode, episodeText(target.episode))
                        },
                        onClick = onPlay,
                        weight = ButtonWeight.Primary,
                        icon = Icons.Rounded.PlayArrow,
                        enabled = target != null,
                        modifier = Modifier.focusRequester(playFocus),
                    )
                    PageFavorite(
                        favorite = state.favorite,
                        text = stringResource(if (state.favorite) R.string.show_favorite_remove else R.string.show_favorite_add),
                        onClick = onFavorite,
                    )
                }
                // The rating on a row of its own, as the film page draws it (details-template-ar.html).
                details?.rating?.let {
                    RatingBadge(
                        rating = ratingText(it),
                        labelStyle = castivioChipStyle(m.frame.fsChip),
                        iconSize = m.frame.fsBody * FACT_ICON_OF_BODY,
                        padH = m.entryPadH,
                        padV = m.entryPadV,
                        modifier = Modifier.clip(RoundedCornerShape(Radius.pill)).background(detailsGround()),
                    )
                }
                val plot = details?.plot
                if (plot != null) {
                    // The plot's heading, and at its end the link to the whole of it where the page had to cut it: the
                    // film page's own row.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.movie_plot_title),
                            style = castivioChipStyle(m.frame.fsLabel).copy(fontWeight = FontWeight.Bold),
                            color = colors.onBackground,
                        )
                        if (plotCut) PlotMore(m, onClick = onMorePlot)
                    }
                    Text(
                        text = plot,
                        // The strong ink, and the plot's own direction from its first letter, so a Dutch or English
                        // plot on an Arabic page keeps its full stop at its end.
                        style = castivioBodyStyle(m.frame.fsBody).copy(textDirection = TextDirection.Content),
                        color = colors.onBackground,
                        maxLines = if (tv) PLOT_LINES_TV else PLOT_LINES,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = startEdgeAlign(plot),
                        onTextLayout = { plotCut = it.hasVisualOverflow },
                        modifier = if (plotCut) {
                            Modifier.focusProperties { canFocus = false }.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = stringResource(R.string.movie_plot_more),
                                onClick = onMorePlot,
                            )
                        } else {
                            Modifier
                        },
                    )
                } else if (waiting) {
                    PlotPlaceholder(m, tv)
                }
                // The cast in one line, as the film page sets a credit: the label quieter, the names in the strong ink.
                details?.cast?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let { cast ->
                    Text(
                        text = creditText(
                            stringResource(R.string.movie_cast),
                            creditNames(cast, rtl = LocalLayoutDirection.current == LayoutDirection.Rtl),
                        ),
                        style = castivioBodyStyle(m.frame.fsBody).copy(textDirection = TextDirection.Content),
                        color = colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The facts in one row, wrapping where the width is short: year, genre, rating, seasons -- each only if it is known. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowFacts(state: ShowState, waiting: Boolean, m: CatalogMetrics) {
    val details = state.details
    val iconSize = m.frame.fsBody * FACT_ICON_OF_BODY
    val style = castivioBodyStyle(m.frame.fsBody)
    val anything = details?.releaseYear != null || details?.genres?.any { it.isNotBlank() } == true ||
        state.seasons.isNotEmpty()
    // On the film page's ground: one pill of glass, as its facts bar is, so the facts read over the wide picture.
    val shape = RoundedCornerShape(Radius.pill)
    val colors = CastivioTheme.colors
    FlowRow(
        modifier = if (anything) {
            Modifier
                .clip(shape)
                .background(detailsGround())
                .border(1.dp, colors.glassBorderSoft, shape)
                .padding(horizontal = m.entryPadH, vertical = m.entryPadV)
        } else {
            Modifier
        },
        horizontalArrangement = Arrangement.spacedBy(m.paneGap),
        verticalArrangement = Arrangement.spacedBy(m.rowGap / 2),
    ) {
        details?.releaseYear?.let { Fact(Icons.Rounded.CalendarToday, ltrToken(it.toString()), iconSize, style) }
        details?.genres?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let { genres ->
            Fact(Icons.Rounded.TheaterComedy, genres.joinToString(" | "), iconSize, style)
        }
        if (state.seasons.isNotEmpty()) {
            val count = state.seasons.size
            Fact(Icons.Rounded.Layers, plural(R.plurals.show_seasons, count), iconSize, style)
        }
        if (waiting) Bar(Modifier.width(m.frame.fsBody * FACTS_BAR).height(m.frame.fsBody))
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String, iconSize: Dp, style: androidx.compose.ui.text.TextStyle) {
    val colors = CastivioTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FACT_GAP)) {
        Icon(icon, contentDescription = null, tint = colors.onBackgroundMuted, modifier = Modifier.size(iconSize))
        Text(text = text, style = style, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlotPlaceholder(m: CatalogMetrics, tv: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(m.rowGap / 2)) {
        Bar(Modifier.fillMaxWidth().height(m.frame.fsBody))
        Bar(Modifier.fillMaxWidth(PLOT_BAR_SECOND).height(m.frame.fsBody))
        if (tv) Bar(Modifier.fillMaxWidth(PLOT_BAR_THIRD).height(m.frame.fsBody))
    }
}

/** A quiet bar: where something will be, once the provider has said it. */
@Composable
private fun Bar(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(Radius.xs)).background(CastivioTheme.colors.glassFill))
}

/** The seasons as a row of two-line chips; the chosen one is lit, and kept in view when there are many. */
@Composable
private fun SeasonChips(seasons: List<Season>, chosen: Season?, m: CatalogMetrics, tv: Boolean, onSeason: (Int) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val index = seasons.indexOfFirst { it.number == chosen?.number }
    LaunchedEffect(index) { if (index >= 0) listState.animateScrollToItem(index) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(m.rowGap)) {
        LazyRow(Modifier.weight(1f), state = listState, horizontalArrangement = Arrangement.spacedBy(m.rowGap)) {
            items(seasons, key = { it.number }) { season ->
                SeasonChip(season, selected = season.number == chosen?.number, m = m, tv = tv) { onSeason(season.number) }
            }
        }
        // A television's remote has no swipe: an arrow at the end of the row takes it on.
        if (tv && listState.canScrollForward) {
            CastivioIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.show_nav_seasons),
                onClick = {
                    scope.launch { listState.animateScrollToItem(listState.firstVisibleItemIndex + CHIPS_STEP) }
                },
            )
        }
    }
}

/**
 * The open season and what it holds -- "Season 1" and "(12 episodes)" -- in the two steps the selector and the plain label
 * share, so a show of one season and a show of many read the same.
 */
@Composable
private fun SeasonName(chosen: Season?, m: CatalogMetrics) {
    val colors = CastivioTheme.colors
    Text(
        text = chosen?.let { stringResource(R.string.show_season, it.number) }
            ?: stringResource(R.string.show_nav_seasons),
        style = castivioChipStyle(m.frame.fsChip).copy(fontWeight = FontWeight.Bold),
        color = colors.onBackground,
        maxLines = 1,
    )
    chosen?.let {
        Text(
            text = "(${plural(R.plurals.show_episodes, it.episodes.size)})",
            style = castivioBodyStyle(m.frame.fsBody),
            color = colors.onBackgroundVariant,
            maxLines = 1,
        )
    }
}

/** The season of a show that has only one, as text: no frame, no arrow and no press, since there is nothing to pick. */
@Composable
private fun SeasonLabel(chosen: Season?, m: CatalogMetrics) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(m.rowGap)) {
        SeasonName(chosen, m)
    }
}

@Composable
private fun SeasonSelector(
    seasons: List<Season>,
    chosen: Season?,
    m: CatalogMetrics,
    tv: Boolean,
    onSeason: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val current = chosen
    Box {
        Row(
            Modifier
                .clip(shape)
                .background(colors.selectedFill)
                .border(1.5.dp, colors.focusRing, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = m.entryPadH, vertical = m.rowGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(m.rowGap),
        ) {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = colors.onBackground,
                modifier = Modifier.size(m.frame.fsBody * 1.25f),
            )
            SeasonName(current, m)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            seasons.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${stringResource(R.string.show_season, item.number)} · ${plural(R.plurals.show_episodes, item.episodes.size)}",
                            style = castivioChipStyle(m.frame.fsChip),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSeason(item.number)
                    },
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(season: Season, selected: Boolean, m: CatalogMetrics, tv: Boolean, onClick: () -> Unit) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    var focused by remember { mutableStateOf(false) }
    val episodes = season.episodes.size
    Column(
        Modifier
            .height(Sizing.minTarget(tv).coerceAtLeast(m.frame.fsChip * CHIP_OF_TYPE))
            .clip(shape)
            .background(if (selected) colors.selectedFill else colors.glassFill)
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> colors.focusRing
                    selected -> colors.selectedBorder
                    else -> colors.glassBorder
                },
                shape,
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = m.entryPadH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.show_season, season.number),
            style = castivioChipStyle(m.frame.fsChip).copy(fontWeight = FontWeight.Bold),
            color = colors.onBackground,
            maxLines = 1,
        )
        Text(
            text = plural(R.plurals.show_episodes, episodes),
            style = castivioChipStyle(m.frame.fsChip * CHIP_SUB_OF_CHIP),
            color = if (selected) colors.onBackground else colors.onBackgroundMuted,
            maxLines = 1,
        )
    }
}

/** What stands in the list's place while no episode is held: placeholders, a way to ask again, or the plain fact. */
@Composable
private fun EpisodesPlaceholder(
    load: ShowLoad,
    preparing: Boolean,
    failure: String?,
    m: CatalogMetrics,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    // Whether this APK may print why: debuggable, as for the board's failure figures -- a property of the package.
    val context = LocalContext.current
    val instrumented = remember(context) { PerformanceLog.isVisibleIn(context) }
    when (load) {
        ShowLoad.Loading -> Column(modifier, verticalArrangement = Arrangement.spacedBy(m.rowGap)) {
            // The provider answered with no episode, and is asked again: it is likely still building the show.
            if (preparing) {
                Text(
                    text = stringResource(R.string.show_preparing),
                    style = castivioBodyStyle(m.frame.fsBody),
                    color = CastivioTheme.colors.onBackgroundVariant,
                )
            }
            repeat(PLACEHOLDER_ROWS) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(m.rowMin)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(CastivioTheme.colors.glassFill),
                )
            }
        }
        ShowLoad.Failed -> Box(modifier.heightIn(min = m.rowMin * PLACEHOLDER_ROWS), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.show_failed_title),
                // In a debuggable build, the reason in place of the sentence: a provider that refused is told
                // apart from a page that broke, which the sentence cannot do from a television.
                detail = failure?.takeIf { instrumented } ?: stringResource(R.string.show_failed_detail),
                actionLabel = stringResource(R.string.show_retry),
                onAction = onRetry,
                secondaryActionLabel = stringResource(R.string.show_back),
                onSecondaryAction = onBack,
            )
        }
        ShowLoad.Ready -> Box(modifier.heightIn(min = m.rowMin * PLACEHOLDER_ROWS), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.show_no_episodes_title),
                detail = stringResource(R.string.show_no_episodes_detail),
                // Asking again is here, so a viewer does not have to leave the page and come back to do it.
                actionLabel = stringResource(R.string.show_retry),
                onAction = onRetry,
                secondaryActionLabel = stringResource(R.string.show_back),
                onSecondaryAction = onBack,
            )
        }
    }
}

/**
 * What the request for this page cost, in one muted line: a debuggable build says where a wait went -- the provider
 * answering, its answer arriving, the catalogue keeping it -- and how many times it was asked. It ends with the ids the
 * provider gave the show ([ids]), when its details have come.
 */
@Composable
private fun RequestTimesLine(times: RequestTimes, ids: String?, m: CatalogMetrics) {
    Text(
        text = times.line() + ids?.let { " · $it" }.orEmpty(),
        style = castivioBodyStyle(m.frame.fsLabel),
        color = CastivioTheme.colors.onBackgroundMuted,
        maxLines = 1,
    )
}

/**
 * One episode: its number, its picture, its name and a line of its plot, and its running time in an outline at the
 * end; checked when watched, barred when part-watched. The row the remote is on is violet with its edge.
 */
@Composable
private fun EpisodeRow(
    episode: Episode,
    mark: EpisodeMark,
    plot: String?,
    waiting: Boolean,
    m: CatalogMetrics,
    onStart: (Episode) -> Unit,
) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    var focused by remember { mutableStateOf(false) }
    val thumbHeight = m.rowMin
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) colors.selectedFill else colors.glassFill)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.selectedBorder else colors.glassBorderSoft, shape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onStart(episode) }
            .padding(m.cardPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(m.rowGap),
    ) {
        Text(
            text = episode.episodeNumber.takeIf { it > 0 }?.let { ltrToken(it.toString()) }.orEmpty(),
            style = castivioChipStyle(m.frame.fsLabel * NUMBER_OF_LABEL).copy(fontWeight = FontWeight.SemiBold),
            color = colors.onBackgroundVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(m.frame.fsLabel * NUMBER_WIDTH_OF_LABEL),
        )
        Box(Modifier.height(thumbHeight).aspectRatio(THUMB_ASPECT)) {
            PosterFrame(url = episode.artworkUrl, seed = episode.episodeNumber, modifier = Modifier.fillMaxSize())
            when (mark) {
                EpisodeMark.Watched -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.align(Alignment.TopEnd).padding(MARK_PAD).size(MARK_SIZE),
                )
                is EpisodeMark.Partly -> Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(mark.percent.coerceIn(0f, 1f))
                        .height(BAR_HEIGHT)
                        .background(colors.primary),
                )
                EpisodeMark.None -> Unit
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(m.rowGap / 4)) {
            Text(
                text = episode.title,
                style = castivioChipStyle(m.frame.fsLabel).copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = startEdgeAlign(episode.title),
            )
            when {
                plot != null -> Text(
                    text = plot,
                    style = castivioBodyStyle(m.frame.fsBody),
                    color = colors.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = startEdgeAlign(plot),
                )
                waiting -> Bar(Modifier.fillMaxWidth(PLOT_BAR_SECOND).height(m.frame.fsBody))
            }
        }
        movieDuration(episode.durationMinutes)?.let { duration ->
            Text(
                text = ltrToken(duration),
                style = castivioBodyStyle(m.frame.fsBody),
                color = colors.onBackground,
                maxLines = 1,
                modifier = Modifier
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(Radius.xs))
                    .padding(horizontal = m.rowGap, vertical = m.rowGap / 4),
            )
        }
    }
}

/** The cast, as the providers send it: names, with no photographs, in a row that wraps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CastSection(cast: List<String>, m: CatalogMetrics) {
    val colors = CastivioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(m.rowGap)) {
        SectionTitle(stringResource(R.string.show_nav_cast), m)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(m.rowGap),
            verticalArrangement = Arrangement.spacedBy(m.rowGap),
        ) {
            for (name in cast) {
                Text(
                    text = name,
                    style = castivioChipStyle(m.frame.fsChip),
                    color = colors.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(colors.glassFill)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = m.entryPadH, vertical = m.rowGap / 2),
                )
            }
        }
    }
}

/** Other shows filed under the same category, as the poster wall draws a show: a press opens its page. */
@Composable
private fun SimilarSection(
    shows: List<SeriesSummary>,
    m: CatalogMetrics,
    board: ChannelsMetrics,
    surfaceHeight: Dp,
    onOpenShow: (SeriesSummary) -> Unit,
) {
    val titleStep = filmTitleStep(board)
    val poster = (surfaceHeight * SIMILAR_POSTER_OF_HEIGHT).coerceIn(MIN_POSTER, MAX_POSTER)
    Column(verticalArrangement = Arrangement.spacedBy(m.rowGap)) {
        SectionTitle(stringResource(R.string.show_similar_title), m)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(board.nameGap)) {
            items(shows, key = { it.seriesId }) { show ->
                MediaCard(
                    title = show.title,
                    shape = CardShape.Poster,
                    artworkSeed = show.seriesId.hashCode(),
                    onClick = { onOpenShow(show) },
                    width = poster * CATALOG_POSTER_ASPECT,
                    gap = board.nameGap,
                    titleStyle = castivioChipStyle(titleStep),
                    captionStyle = castivioBodyStyle(board.frame.fsBody),
                    artworkUrl = show.artworkUrl,
                    posterHeight = poster,
                    titleLines = TITLE_LINES,
                    titleHeight = titleStep * LABEL_LINE * TITLE_LINES,
                    focusBrush = focusBrush(),
                    focusScale = 1f,
                    artworkScale = ContentScale.Fit,
                    cardFill = CastivioTheme.colors.glassFill,
                    cinematicBackdrop = true,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, m: CatalogMetrics) {
    Text(
        text = text,
        style = castivioChipStyle(m.frame.fsLabel * SECTION_TITLE_OF_LABEL).copy(fontWeight = FontWeight.Bold),
        color = CastivioTheme.colors.onBackground,
        maxLines = 1,
    )
}

/** A count with its noun, in the number the language gives it. */
@Composable
private fun plural(id: Int, count: Int): String = LocalContext.current.resources.getQuantityString(id, count, count)

/** `S2 E5` for the button, or the half the provider numbered. */
internal fun episodeText(episode: Episode): String = when {
    episode.seasonNumber > 0 && episode.episodeNumber > 0 -> "S${episode.seasonNumber} E${episode.episodeNumber}"
    episode.episodeNumber > 0 -> "E${episode.episodeNumber}"
    episode.seasonNumber > 0 -> "S${episode.seasonNumber}"
    else -> episode.title
}

/** A handset's rail entries' side: under the 48dp floor, which a television keeps. */
private val HANDSET_ENTRY = 40.dp

/** The rail's width on a handset, icons only; a television's is a share of its width, between a floor and a ceiling. */
private val HANDSET_RAIL = 56.dp
private const val RAIL_OF_WIDTH = 0.17f
private val MIN_RAIL = 150.dp
private val MAX_RAIL = 240.dp
private val RAIL_GAP = 8.dp
private val RAIL_PAD = 10.dp
private val RAIL_ICON = 20.dp

/** An episode's picture is 16:9. */
private const val THUMB_ASPECT = 16f / 9f

/** The show's name, as a share of the screen's own title step: it stands beside a poster, not over a page. */
private const val TITLE_OF_PANE = 0.82f

private const val SHOW_TITLE_LINES = 3

/** The plot's lines: a television has the room for three, a handset two. */
private const val PLOT_LINES = 2
private const val PLOT_LINES_TV = 3

/** A quiet glass ground that groups the show's identity and actions without obscuring its backdrop. */
private const val HERO_PANEL_ALPHA = 0.72f

/** How tall the show's poster is, as a share of the screen: beside the page's other things, not all of it. */
private const val POSTER_OF_HEIGHT = 0.42f
private const val SIMILAR_POSTER_OF_HEIGHT = 0.3f
private val MIN_POSTER = 96.dp
private val MAX_POSTER = 300.dp

private const val FACT_ICON_OF_BODY = 1.25f
private val FACT_GAP = 6.dp
private const val FACTS_BAR = 9f
private const val PLOT_BAR_SECOND = 0.92f
private const val PLOT_BAR_THIRD = 0.6f

private const val CHIP_OF_TYPE = 3.4f
private const val CHIP_SUB_OF_CHIP = 0.82f
private const val CHIPS_STEP = 3

private const val NUMBER_OF_LABEL = 1.2f
private const val NUMBER_WIDTH_OF_LABEL = 2.2f
private const val SECTION_TITLE_OF_LABEL = 1.1f

private const val PLACEHOLDER_ROWS = 5
private val MARK_PAD = 4.dp
private val MARK_SIZE = 18.dp
private val BAR_HEIGHT = 3.dp
