package com.castivio.feature.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    // Asked when the screen opens, cancelled when it is left: the screen owns the request for exactly as long as it is on.
    DisposableEffect(channel.id) {
        model.open(channel)
        onDispose { model.close() }
    }
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
                    // The days, newest first, from the left. Only the days the archive has: nothing is padded out to a week. A few
                    // share the width as one bar, as drawn; more than fit that way scroll.
                    if (state.days.size <= DAYS_AS_BAR) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.guideGap)) {
                            state.days.forEachIndexed { index, day ->
                                DayTab(day, index == state.dayIndex, m, Modifier.weight(1f)) { model.selectDay(index) }
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(m.guideGap)) {
                            itemsIndexed(state.days, key = { index, day -> "${index}_${day.date}" }) { index, day ->
                                DayTab(day, index == state.dayIndex, m) { model.selectDay(index) }
                            }
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
                                columns = GridCells.Adaptive(CARD_MIN_WIDTH),
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

/** One day of the archive: its name or its date, filled with the chosen colour when it is the one shown. */
@Composable
private fun DayTab(day: CatchUpDayView, selected: Boolean, m: ChannelsMetrics, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = CastivioTheme.colors
    InteractiveGlassCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(m.panelRadius),
        fill = if (selected) SolidColor(colors.primaryContainer) else SolidColor(colors.glassFill),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (day.label == DayLabel.DATE) day.date else dayName(day.label),
                style = castivioChipStyle(m.frame.fsLabel),
                color = colors.onBackgroundStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = m.controlPadH, vertical = Spacing.sm),
            )
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

/** A card is at least this wide: the grid fits as many columns as the panel holds, three on a television, fewer on a phone. */
private val CARD_MIN_WIDTH = 150.dp

/** Rows of programmes shown whole at a time; the rest are a scroll away. */
private const val VISIBLE_ROWS = 3

/** The floor under a card's height, so a very short panel still draws a card that can be read and scrolls instead. */
private val CARD_MIN_HEIGHT = 76.dp

/** Days up to this many share the width as one bar; more scroll in a row. */
private const val DAYS_AS_BAR = 4
