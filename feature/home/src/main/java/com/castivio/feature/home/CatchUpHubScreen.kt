package com.castivio.feature.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.castivio.core.design.components.CastivioSpinner
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.ProviderArtwork
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.icons.CastivioIcons
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Sizing
import com.castivio.core.design.theme.Spacing
import com.castivio.domain.ArchiveGroup
import com.castivio.domain.CatchUpPolicy
import com.castivio.domain.Channel

/**
 * **Catch-up, a place of its own**, under the band every board has.
 *
 * ## The first page: three columns (owner, 2026-11-26, design/mockups/catchup-19-days-column-ar.png)
 *
 * The bouquets that keep an archive, with their own search; the open bouquet's channels, which the band's search narrows;
 * and the chosen channel's days -- today, yesterday, then dates. A day opens the second page: that day's programmes beside
 * the bouquet's channels ([CatchUpProgrammes]), the page approved before. Everything reads from the left.
 *
 * ## Back, one place at a time
 *
 * The programmes, then the three columns with the channel still chosen; there a search that holds something is closed
 * first, and then Catch-up is left -- [CatchUpHubViewModel.back] says which.
 *
 * ## What it asks of the network
 *
 * The bouquets and the channels read the catalogue. A provider is asked for a channel's listing when that channel is
 * chosen, by [CatchUpViewModel], once: its days come from it, and so do the programmes the day opens.
 */
@Composable
fun CatchUpHubScreen(
    onPlay: (CatalogSelection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The player is open over this screen: a programme was started from it, and Back from the player returns here. */
    playing: Boolean = false,
    hub: CatchUpHubViewModel = hiltViewModel(),
    programmes: CatchUpViewModel = hiltViewModel(),
) {
    val tv = CastivioTheme.device.isTv
    val stage by hub.stage.collectAsStateWithLifecycle()
    val groups by hub.shownGroups.collectAsStateWithLifecycle()
    val allGroups by hub.groups.collectAsStateWithLifecycle()
    val supported by hub.supported.collectAsStateWithLifecycle()
    val searchText by hub.searchText.collectAsStateWithLifecycle()
    val groupSearch by hub.groupSearchText.collectAsStateWithLifecycle()
    val channels = hub.channels.collectAsLazyPagingItems()

    // Leaving Catch-up for Home ends this visit: the next one begins at "all", and the listing in hand is let go.
    DisposableEffect(Unit) {
        onDispose {
            hub.reset()
            programmes.close()
        }
    }
    BackHandler { if (!hub.back()) onBack() }

    // Written left to right in every language, as the channels' board is: the names in it are the provider's.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        ) {
            val m = channelsMetricsFor(tv = tv, width = maxWidth, height = maxHeight)

            when (val at = stage) {
                is HubStage.Programmes -> {
                    val programmeSearch by programmes.searchText.collectAsStateWithLifecycle()
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = m.edge)
                            .padding(top = m.boardTop, bottom = m.boardBottom),
                    ) {
                        BoardHeader(
                            m = m,
                            searchText = programmeSearch,
                            onSearch = programmes::search,
                            hint = stringResource(R.string.catchup_search_programmes),
                            // Programmes are in the order they were on: there is no other order to offer.
                            order = null,
                        )
                        Spacer(Modifier.height(m.headerGap))
                        // The chosen day's programmes across the page (owner, 2026-11-26,
                        // design/mockups/catchup-22-one-day-ar.png): no channels beside them and no other days; both are on the
                        // first page, one Back away.
                        CatchUpProgrammes(
                            channel = at.channel,
                            m = m,
                            model = programmes,
                            onPlay = onPlay,
                            onClose = { hub.back() },
                            playing = playing,
                            modifier = Modifier.fillMaxWidth().weight(1f).homeCard(m).padding(m.wellPad),
                        )
                    }
                }

                is HubStage.Browse -> {
                    // The chosen channel's listing, asked for once: its days are the third column.
                    LaunchedEffect(at.channel?.id) { at.channel?.let(programmes::open) }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = m.edge)
                            .padding(top = m.boardTop, bottom = m.boardBottom),
                    ) {
                        BoardHeader(
                            m = m,
                            searchText = searchText,
                            onSearch = hub::search,
                            hint = stringResource(R.string.channels_search_hint),
                            order = null,
                        )
                        Spacer(Modifier.height(m.headerGap))
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(m.railGap),
                        ) {
                            GroupsColumn(
                                groups = groups,
                                total = allGroups?.total,
                                open = at.group,
                                supported = supported,
                                searchText = groupSearch,
                                onSearch = hub::searchGroups,
                                m = m,
                                onOpen = hub::openGroup,
                                modifier = Modifier.fillMaxWidth(GROUPS_SHARE).fillMaxHeight().homeCard(m).padding(m.wellPad),
                            )
                            Column(
                                Modifier.fillMaxWidth(CHANNELS_SHARE).fillMaxHeight().homeCard(m).padding(m.wellPad),
                                verticalArrangement = Arrangement.spacedBy(m.guideGap),
                            ) {
                                Text(
                                    text = at.group?.name ?: stringResource(R.string.catchup_all),
                                    style = castivioChipStyle(m.frame.fsChip),
                                    color = CastivioTheme.colors.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                ChannelSide(
                                    searchText = searchText,
                                    onSearch = hub::search,
                                    withSearch = false,
                                    channels = channels,
                                    selected = at.channel,
                                    m = m,
                                    onOpen = hub::openChannel,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                )
                            }
                            DaysColumn(
                                channel = at.channel,
                                model = programmes,
                                m = m,
                                onDay = { index ->
                                    programmes.selectDay(index)
                                    hub.openDay()
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight().homeCard(m).padding(m.wellPad),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The bouquets that keep an archive, under their own search: "all" first, the open one in the chosen colour. */
@Composable
private fun GroupsColumn(
    groups: HubGroups?,
    total: Int?,
    open: ArchiveGroup?,
    supported: Boolean,
    searchText: String,
    onSearch: (String) -> Unit,
    m: ChannelsMetrics,
    onOpen: (ArchiveGroup?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(groups != null) { if (groups != null) runCatching { first.requestFocus() } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(m.guideGap)) {
        HeaderSearchField(
            text = searchText,
            m = m,
            onType = onSearch,
            hint = stringResource(R.string.catchup_search_groups),
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            groups == null -> Waiting(m)
            total == 0 -> Empty(m, if (supported) R.string.catchup_hub_empty else R.string.catchup_unsupported)
            else -> LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(m.guideGap)) {
                if (searchText.isBlank()) {
                    item(key = ALL_KEY) {
                        GroupCard(
                            name = stringResource(R.string.catchup_all),
                            count = total ?: groups.total,
                            lead = open == null,
                            m = m,
                            modifier = Modifier.focusRequester(first),
                            onClick = { onOpen(null) },
                        )
                    }
                }
                items(groups.groups, key = { it.id }) { group ->
                    GroupCard(
                        name = group.name,
                        count = group.channels,
                        lead = open?.id == group.id,
                        m = m,
                        onClick = { onOpen(group) },
                    )
                }
            }
        }
    }
}

/**
 * The chosen channel's archive, a day to a line: today, yesterday, then the dates, each with its date. A press opens that
 * day's programmes. Before a channel is chosen the column says what to do; while the listing is asked for, it says so.
 */
@Composable
private fun DaysColumn(
    channel: Channel?,
    model: CatchUpViewModel,
    m: ChannelsMetrics,
    onDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val state by model.state.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(m.guideGap)) {
        if (channel == null) {
            Empty(m, R.string.catchup_pick_channel)
            return@Column
        }
        Text(
            text = stringResource(R.string.catchup_archive_of, channel.title),
            style = castivioChipStyle(m.frame.fsChip),
            color = colors.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (state.phase) {
            CatchUpPhase.IDLE, CatchUpPhase.LOADING -> Waiting(m)
            CatchUpPhase.EMPTY -> Empty(m, R.string.catchup_empty)
            CatchUpPhase.UNSUPPORTED -> Empty(m, R.string.catchup_unsupported)
            CatchUpPhase.FAILED -> Empty(m, R.string.catchup_failed)
            CatchUpPhase.READY -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(m.guideGap),
            ) {
                itemsIndexed(state.days, key = { index, day -> "${index}_${day.date}" }) { index, day ->
                    DayEntry(day, m) { onDay(index) }
                }
            }
        }
    }
}

/** One day: its name -- today, yesterday -- or its date, the date under a name, and the arrow that says it opens. */
@Composable
private fun DayEntry(day: CatchUpDayView, m: ChannelsMetrics, onClick: () -> Unit) {
    val colors = CastivioTheme.colors
    val tv = CastivioTheme.device.isTv
    InteractiveGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(m.panelRadius),
        fill = SolidColor(colors.glassFill),
    ) {
        Box {
        // The archive's picture, only just there, as on the programmes' cards.
        CatchUpArt(Modifier.matchParentSize())
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sizing.minTarget(tv))
                .padding(horizontal = m.controlPadH, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(m.controlPadH),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = colors.hueAzure,
                modifier = Modifier.size(Sizing.iconMd),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (day.label) {
                        DayLabel.TODAY -> stringResource(R.string.channels_guide_today)
                        DayLabel.YESTERDAY -> stringResource(R.string.catchup_yesterday)
                        DayLabel.DATE -> day.date
                    },
                    style = castivioChipStyle(m.frame.fsLabel),
                    color = colors.onBackgroundStrong,
                    maxLines = 1,
                )
                if (day.label != DayLabel.DATE) {
                    Text(
                        text = day.date,
                        style = castivioBodyStyle(m.frame.fsBody),
                        color = colors.onBackgroundMuted,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                CastivioIcons.Chevron,
                contentDescription = null,
                tint = colors.onBackgroundMuted,
                modifier = Modifier.size(Sizing.iconSm),
            )
        }
        }
    }
}

@Composable
private fun GroupCard(
    name: String,
    count: Int,
    lead: Boolean,
    m: ChannelsMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val tv = CastivioTheme.device.isTv
    val pill = RoundedCornerShape(m.panelRadius)
    InteractiveGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(m.panelRadius),
        fill = if (lead) colors.primaryBrush else SolidColor(colors.glassFill),
    ) {
        // The floor under the card's height is the row's, not the card's: a card holds its content at the top, so a floor
        // set on the card left the content above its middle and a bare strip under it.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sizing.minTarget(tv))
                .padding(horizontal = m.controlPadH, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = name,
                style = castivioChipStyle(m.frame.fsLabel),
                color = colors.onBackgroundStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // A sky blue beside the white of the name, on a plate of the azure: it was violet, which was not liked, and white,
            // which was not either -- white figures next to a white name read as one grey run. On the azure card itself the
            // figure stays white, as the card is the colour.
            Text(
                text = count.toString(),
                style = castivioChipStyle(m.frame.fsChip),
                color = if (lead) colors.onBackgroundStrong else lerp(colors.hueAzure, colors.onBackgroundStrong, COUNT_INK_LIFT),
                maxLines = 1,
                modifier = Modifier
                    .clip(pill)
                    .background(
                        if (lead) colors.onBackgroundStrong.copy(alpha = BADGE_ON_LEAD)
                        else colors.hueAzure.copy(alpha = COUNT_PLATE_ALPHA),
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
            Icon(
                CastivioIcons.Chevron,
                contentDescription = null,
                tint = colors.onBackgroundMuted,
                modifier = Modifier.size(Sizing.iconSm),
            )
        }
    }
}

/**
 * The list beside a channel's programmes: its own search at its head, then the category's channels with an archive, the open
 * one in the chosen colour. A press on another opens its programmes in the pane beside, and the list stays where it was
 * scrolled.
 */
@Composable
private fun ChannelSide(
    searchText: String,
    onSearch: (String) -> Unit,
    /** Whether the list carries its own search at its head: beside the programmes; on the first page the band has it. */
    withSearch: Boolean,
    channels: LazyPagingItems<Channel>,
    selected: Channel?,
    m: ChannelsMetrics,
    onOpen: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(m.guideGap)) {
        if (withSearch) {
            HeaderSearchField(
                text = searchText,
                m = m,
                onType = onSearch,
                hint = stringResource(R.string.channels_search_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (channels.itemCount == 0 && channels.loadState.refresh is LoadState.Loading && searchText.isBlank()) {
            Waiting(m)
        }
        if (channels.itemCount == 0 && searchText.isNotBlank() && channels.loadState.refresh !is LoadState.Loading) {
            Text(
                text = stringResource(R.string.catchup_search_none),
                style = castivioBodyStyle(m.frame.fsBody),
                color = colors.onBackgroundMuted,
            )
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(m.guideGap)) {
            items(channels.itemCount, key = channels.itemKey { it.id }) { index ->
                channels[index]?.let { channel ->
                    ChannelEntry(
                        channel = channel,
                        m = m,
                        selected = channel.id == selected?.id,
                        // The side is narrow: the number gives its room to the name.
                        numbered = false,
                        onClick = { onOpen(channel) },
                    )
                }
            }
        }
    }
}

/** One channel: its number and logo and name, and how many days the provider says it keeps. */
@Composable
private fun ChannelEntry(
    channel: Channel,
    m: ChannelsMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The channel whose programmes are open beside the list. */
    selected: Boolean = false,
    numbered: Boolean = true,
) {
    val colors = CastivioTheme.colors
    val tv = CastivioTheme.device.isTv
    val days = CatchUpPolicy.daysOf(channel.catchUpHours)
    InteractiveGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(m.panelRadius),
        fill = if (selected) SolidColor(colors.primaryContainer) else SolidColor(colors.glassFill),
    ) {
        // The floor is the row's, so what is in it is centred: see [GroupCard].
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sizing.minTarget(tv))
                .padding(horizontal = m.controlPadH, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(m.controlPadH),
        ) {
            // A plate only for a channel that has a number: a provider that numbers none would otherwise show an empty one.
            channel.number?.takeIf { numbered }?.let { number ->
                Text(
                    text = number.toString(),
                    style = castivioChipStyle(m.frame.fsChip),
                    color = colors.onBackground,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.panelRadius))
                        .background(colors.onBackground.copy(alpha = NUMBER_PLATE_ALPHA))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
            ProviderArtwork(
                url = channel.artworkUrl,
                description = null,
                modifier = Modifier.size(m.frame.fsLabel * LOGO_IN_ENTRY),
                seed = channel.title,
            )
            Text(
                text = channel.title,
                style = castivioChipStyle(m.frame.fsLabel),
                color = colors.onBackgroundStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (days != null) {
                Text(
                    text = stringResource(R.string.catchup_days, days),
                    style = castivioChipStyle(m.frame.fsChip),
                    color = colors.success,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.panelRadius))
                        .background(colors.success.copy(alpha = BADGE_ALPHA))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun Waiting(m: ChannelsMetrics) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.guideGap, Alignment.CenterVertically),
    ) {
        CastivioSpinner()
        Text(
            text = stringResource(R.string.catchup_loading),
            style = castivioBodyStyle(m.frame.fsBody),
            color = CastivioTheme.colors.onBackgroundMuted,
        )
    }
}

@Composable
private fun Empty(m: ChannelsMetrics, @StringRes message: Int = R.string.catchup_hub_empty) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(message),
            style = castivioBodyStyle(m.frame.fsBody),
            color = CastivioTheme.colors.onBackgroundMuted,
        )
    }
}


/**
 * The first page's columns: the bouquets take [GROUPS_SHARE] of the width, the channels [CHANNELS_SHARE] of what is left,
 * and the days the rest. The bouquets wider and the days narrower (owner, 2026-11-26): bouquet names run long, and a day
 * is a word and a date -- about 34%, 36% and 30% of the width.
 */
private const val GROUPS_SHARE = 0.34f
private const val CHANNELS_SHARE = 0.55f

/**
 * A category's card is at least this wide: four in a row on a handset's board (770dp wide, 4dp between the cards: 188dp a card), and still
 * four on a television's, which is the grid's own rule -- as many as fit.
 *
 * It was 150, which put five in a row, and a name has the room a card leaves it after the figure and the arrow: 71dp, about ten
 * letters, so `FR| FRANCE` ended in an ellipsis. Four give each card 38dp more and a name of fifteen letters its whole width; a long name
 * still ends in one, and that is the next thing to find an answer for. If four are not enough, the number is the one to raise again.
 */
internal val GROUP_MIN_WIDTH = 184.dp

/** A channel's entry is at least this wide: two in a row on the board. */
internal val CHANNEL_MIN_WIDTH = 220.dp

/** The channel's logo in an entry, about two lines of the label's own size. */
private const val LOGO_IN_ENTRY = 1.8f

/** How much of its colour a day's pill is filled with, and how much of white on the azure card. */
private const val BADGE_ALPHA = 0.16f
private const val BADGE_ON_LEAD = 0.22f

/**
 * A category's count, and a channel's number.
 *
 * The count is the azure lifted [COUNT_INK_LIFT] of the way to white -- the sky blue that sits beside a white name -- on a
 * plate of the azure at [COUNT_PLATE_ALPHA]: five to one and over against the plate it stands on, in the drawing the owner
 * chose from (`design/mockups/catchup-count-colours-ar.html`, option A). A channel's number stays white, on a plate of
 * white at [NUMBER_PLATE_ALPHA], as it is on the channels' board.
 */
private const val COUNT_INK_LIFT = 0.35f
private const val COUNT_PLATE_ALPHA = 0.22f
private const val NUMBER_PLATE_ALPHA = 0.10f

/** The first card's key: "all", which is no category's id. */
private const val ALL_KEY = "catchup.all"
