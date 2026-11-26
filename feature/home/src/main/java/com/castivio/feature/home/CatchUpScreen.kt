package com.castivio.feature.home

import androidx.activity.compose.BackHandler
import com.castivio.core.design.components.ProviderArtwork
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.castivio.core.design.components.CastivioChip
import com.castivio.core.design.components.CastivioSpinner
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Spacing
import com.castivio.domain.Channel

/**
 * One channel's archive, by day, and a programme to watch: the pane beside Catch-up's list of channels
 * (owner, 2026-11-25, design/mockups/catchup-17-ltr-ar.png). The band over the page -- the board's own, shared with every board --
 * holds its search, which looks among this channel's programmes; the pane is the days and the programmes, left to right.
 *
 * **Leaving is what ends it**: the request for the archive is cancelled with the pane, and opening another channel from the
 * list beside it asks for that one's.
 *
 * ## What it shows, and what it does not make up
 *
 * The programmes the provider's own archive lists, with the day, the start and the end. There is no programme artwork
 * because an archive listing has none, and the card does not borrow one: every card here is the channel highlighted beside
 * it, so its logo on each card said nothing. A day with nothing in it is not drawn, and a week is not padded out: the
 * provider says how far back it goes, and that is how many days there are.
 *
 * ## What choosing does
 *
 * The programme's address is made -- no request -- and handed to the same `onPlay` a channel row uses, so the player opens
 * over this page. Nothing here plays anything itself.
 */
@Composable
internal fun CatchUpProgrammes(
    channel: Channel,
    m: ChannelsMetrics,
    model: CatchUpViewModel,
    onPlay: (CatalogSelection) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The player is open over this page. The page stays composed under it -- that is what lets Back land on the same
     * programmes, on the same day, scrolled to the same place -- and takes the cursor back when this turns false.
     */
    playing: Boolean = false,
) {
    val colors = CastivioTheme.colors
    val state by model.state.collectAsStateWithLifecycle()
    val view by model.view.collectAsStateWithLifecycle()
    // The card the cursor goes to when the programmes appear and when the player closes over them: the programme the viewer
    // started last if this day still shows it, and the first one otherwise.
    val landing = remember { FocusRequester() }
    var played by remember { mutableStateOf<String?>(null) }
    val landOn = remember(view.cards, played) {
        played?.takeIf { key -> view.cards.any { it.programme.locator == key } }
            ?: view.cards.firstOrNull()?.programme?.locator
    }
    // Back closes a search that holds something before it leaves the screen, as it does on the pages under this one.
    val back = { if (!model.clearSearch()) onClose() }

    // The channel's listing: usually in hand already, opened for its days on the first page; another channel from the list
    // beside asks for its own. Catch-up as a whole closes it when it is left.
    LaunchedEffect(channel.id) { model.open(channel) }
    BackHandler(onBack = back)
    LaunchedEffect(state.phase, state.dayIndex, playing) {
        if (state.phase == CatchUpPhase.READY && !playing) runCatching { landing.requestFocus() }
    }

    Column(modifier.fillMaxHeight()) {
            when (state.phase) {
                CatchUpPhase.IDLE, CatchUpPhase.LOADING -> Message(m) {
                    CastivioSpinner()
                    Text(
                        text = stringResource(R.string.catchup_loading),
                        style = castivioBodyStyle(m.frame.fsBody),
                        color = colors.onBackgroundMuted,
                    )
                }

                CatchUpPhase.EMPTY -> Message(m) { Say(m, R.string.catchup_empty) }
                CatchUpPhase.UNSUPPORTED -> Message(m) { Say(m, R.string.catchup_unsupported) }
                CatchUpPhase.FAILED -> Message(m) {
                    Say(m, R.string.catchup_failed)
                    CastivioChip(text = stringResource(R.string.catchup_retry), onClick = model::retry)
                }

                CatchUpPhase.READY -> {
                    // The day the viewer chose on the first page, and only it (owner, 2026-11-26,
                    // design/mockups/catchup-22-one-day-ar.png): the channel and the day over its programmes. Another day is
                    // chosen where this one was, in the days' column.
                    state.days.getOrNull(state.dayIndex)?.let { shownDay ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(m.controlPadH),
                        ) {
                            ProviderArtwork(
                                url = channel.artworkUrl,
                                description = null,
                                modifier = Modifier.size(m.frame.fsLabel * LOGO_IN_TITLE),
                                seed = channel.title,
                            )
                            Text(
                                text = channel.title,
                                style = castivioChipStyle(m.frame.fsLabel),
                                color = colors.onBackgroundStrong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(text = "·", style = castivioChipStyle(m.frame.fsLabel), color = colors.onBackgroundMuted)
                            if (shownDay.label != DayLabel.DATE) {
                                Text(
                                    text = dayName(shownDay.label),
                                    style = castivioChipStyle(m.frame.fsLabel),
                                    color = colors.hueAzure,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = shownDay.date,
                                style = castivioBodyStyle(m.frame.fsBody),
                                color = colors.onBackgroundVariant,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(Modifier.height(m.guideGap))

                    val day = state.days.getOrNull(state.dayIndex)
                    if (day != null && view.cards.isEmpty() && view.searching) {
                        // Nothing on this day matches: said in the room the cards would have, and the days stay as they are.
                        Message(m) { Say(m, R.string.catchup_search_none) }
                    } else if (day != null) {
                        // The programmes fill what is left of the panel, two rows at a time, and scroll up and down for the rest:
                        // the cards are sized from the room there is, not from a width, so that two rows are always whole.
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                            val cardHeight = ((maxHeight - m.guideGap * (VISIBLE_ROWS - 1)) / VISIBLE_ROWS)
                                .coerceAtLeast(CARD_MIN_HEIGHT)
                            // Another day, or another search, begins at the top, not at wherever the last one was scrolled to.
                            val grid = remember(state.dayIndex, view.term) { LazyGridState() }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(PROGRAMME_COLUMNS),
                                state = grid,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(m.guideGap),
                                verticalArrangement = Arrangement.spacedBy(m.guideGap),
                            ) {
                                gridItemsIndexed(view.cards, key = { _, card -> card.programme.locator }) { _, card ->
                                    ProgrammeCard(
                                        card = card,
                                        m = m,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(cardHeight)
                                            .then(
                                                if (card.programme.locator == landOn) Modifier.focusRequester(landing) else Modifier,
                                            ),
                                        onClick = {
                                            model.choose(card) { address, channelTitle ->
                                                // Nothing is closed: the player opens over this screen and Back from it returns
                                                // here, to the programmes of this channel and not to the list of channels.
                                                played = card.programme.locator
                                                onPlay(
                                                    CatalogSelection(
                                                        url = address,
                                                        title = card.title,
                                                        live = false,
                                                        // The time first: a subtitle that began with the channel's name would be
                                                        // read as a show's season and episode when the name began with S or E.
                                                        subtitle = "${card.timeText} · $channelTitle",
                                                        archiveOf = channel.id,
                                                    ),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (state.opening) {
                        Spacer(Modifier.height(m.guideGap))
                        CastivioSpinner()
                    }
                    if (state.openFailed) {
                        Spacer(Modifier.height(m.guideGap))
                        Text(
                            text = stringResource(R.string.catchup_open_failed),
                            style = castivioBodyStyle(m.frame.fsBody),
                            color = colors.danger,
                        )
                    }
                }
            }
    }
}

/**
 * One programme, small enough for three rows of them to share the pane: when it was on, and the title under it, standing on
 * the card's floor. No logo: the channel is the one highlighted beside the pane.
 *
 * The card's size is the caller's. The title is the one part that can be long, so it is the part that gives: it holds two
 * lines and then ends in an ellipsis, and the card never grows to make room for it. It is in a band that takes whatever
 * the top line leaves, with the text at the bottom, so the names of a row stand on one line.
 */
@Composable
private fun ProgrammeCard(
    card: CatchUpCard,
    m: ChannelsMetrics,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = CastivioTheme.colors
    InteractiveGlassCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(m.panelRadius),
        fill = SolidColor(colors.glassFill),
    ) {
        Box(Modifier.fillMaxSize()) {
        CatchUpArt(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = card.timeText,
                style = castivioBodyStyle(m.frame.fsBody),
                color = colors.onBackgroundMuted,
                maxLines = 1,
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                Text(
                    text = card.title,
                    style = castivioChipStyle(m.frame.fsLabel),
                    color = colors.onBackgroundStrong,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
    }
}

@Composable
private fun Message(m: ChannelsMetrics, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.guideGap, Alignment.CenterVertically),
    ) { content() }
}

@Composable
private fun Say(m: ChannelsMetrics, text: Int) {
    Text(
        text = stringResource(text),
        style = castivioBodyStyle(m.frame.fsBody),
        color = CastivioTheme.colors.onBackgroundMuted,
    )
}

@Composable
private fun dayName(label: DayLabel): String = when (label) {
    DayLabel.TODAY -> stringResource(R.string.channels_guide_today)
    DayLabel.YESTERDAY -> stringResource(R.string.catchup_yesterday)
    DayLabel.DATE -> ""
}

/** Rows of programmes shown whole at a time; the rest are a scroll away. */
private const val VISIBLE_ROWS = 3

/** The floor under a card's height, so a very short panel still draws a card that can be read and scrolls instead. */
private val CARD_MIN_HEIGHT = 76.dp

/** The programmes of the day, this many to a row across the page's width: four, as drawn. */
private const val PROGRAMME_COLUMNS = 4

/** The channel's logo over the day's programmes, a little taller than the label beside it. */
private const val LOGO_IN_TITLE = 1.6f

/**
 * The archive's picture behind a card (owner, 2026-11-26): only just there, at the weight the empty "continue" slots on
 * Home carry theirs, so a page of them says "archive" without a picture repeated loudly twelve times.
 */
internal const val CATCHUP_ART_ALPHA = 0.12f

/** The archive's picture, filling what it is given, at [CATCHUP_ART_ALPHA]. */
@Composable
internal fun CatchUpArt(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.catchup_art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = CATCHUP_ART_ALPHA,
        modifier = modifier,
    )
}
