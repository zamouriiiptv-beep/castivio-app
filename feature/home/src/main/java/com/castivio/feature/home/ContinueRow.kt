package com.castivio.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.PosterFrame
import com.castivio.core.design.components.ProviderArtwork
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.domain.Channel
import com.castivio.domain.MediaKind

/**
 * "Continue watching": a row of small cards under the four sections (owner, 2026-11-25), one slot for each section in the
 * board's order -- live, films, series, radio -- holding the channel last on, the film, the show and the station.
 * The four large cards above it always open their section; carrying on is this row's alone.
 *
 * A filled card is its picture (the channel's or the station's logo, the film's or the show's poster), its name, a line under it
 * -- what is on the channel now, or the episode -- and, where there is one, the aqua bar of how far in it the viewer was.
 * A press does what the large cards' resume faces did: the shell opens the section with that item started inside it.
 *
 * The row is always there (owner, 2026-11-25, design/mockups/home-continue-empty-ar.png): a slot with nothing watched in it
 * yet is its section's own picture, far fainter than on the large cards, its name, and "nothing watched yet" -- whether or not
 * the section is on the device, which the large card above says -- and a press on it opens the section, as the large card does.
 */
@Composable
internal fun ContinueRow(
    resume: Resume?,
    resumes: Map<MediaKind, SectionResume>,
    frame: CastivioMetrics,
    onResume: (Channel) -> Unit,
    onResumeSection: (SectionResume) -> Unit,
    /** A press on an empty slot: its section, as the large card opens it. */
    onOpenSection: (CatalogSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(frame.chipPad / 2)) {
        Text(
            stringResource(R.string.home_resume_label),
            style = castivioChipStyle(frame.fsChip),
            color = colors.onBackgroundVariant,
            maxLines = 1,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            // Four slots always, so a card is the same width whether one or four are shown; empty slots stay empty.
            val gap = frame.chipPad
            val slot = (maxWidth - gap * (SLOTS - 1)) / SLOTS
            val height = maxHeight
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                val live = resume
                if (live != null) {
                    val programme = live.programme
                    ContinueCard(
                        width = slot,
                        height = height,
                        title = live.channel.title,
                        line = programme?.title ?: stringResource(R.string.browse_live),
                        progress = if (programme != null) live.progress else null,
                        frame = frame,
                        onClick = { onResume(live.channel) },
                    ) { side ->
                        ProviderArtwork(
                            url = live.channel.artworkUrl,
                            description = null,
                            modifier = Modifier.size(side),
                            seed = live.channel.title,
                        )
                    }
                } else {
                    EmptySlot(MediaKind.LIVE, slot, height, frame) {
                        onOpenSection(CatalogSection.Live)
                    }
                }
                for (kind in listOf(MediaKind.MOVIE, MediaKind.SERIES, MediaKind.RADIO)) {
                    val offer = resumes[kind]
                    if (offer == null) {
                        EmptySlot(kind, slot, height, frame) {
                            onOpenSection(CatalogSection.entries.first { it.kind == kind })
                        }
                        continue
                    }
                    ContinueCard(
                        width = slot,
                        height = height,
                        title = offer.title,
                        line = offer.detail ?: stringResource(sectionName(offer.kind)),
                        progress = offer.progress,
                        frame = frame,
                        onClick = { onResumeSection(offer) },
                    ) { side ->
                        when (offer.open) {
                            is ResumeOpen.Station -> ProviderArtwork(
                                url = offer.artworkUrl,
                                description = null,
                                modifier = Modifier.size(side),
                                seed = offer.title,
                            )
                            is ResumeOpen.Film, is ResumeOpen.Show -> PosterFrame(
                                url = offer.artworkUrl,
                                seed = offer.title.hashCode(),
                                modifier = Modifier.size(width = side * POSTER_ASPECT, height = side),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sectionName(kind: MediaKind): Int = when (kind) {
    MediaKind.MOVIE -> R.string.browse_movies
    MediaKind.SERIES -> R.string.browse_series
    MediaKind.RADIO -> R.string.browse_radio
    else -> R.string.browse_live
}

/** One small card: its picture at the start, its name and line beside it, and its bar along the foot where it has one. */
@Composable
private fun ContinueCard(
    width: Dp,
    height: Dp,
    title: String,
    line: String,
    progress: Float?,
    frame: CastivioMetrics,
    onClick: () -> Unit,
    picture: @Composable (side: Dp) -> Unit,
) {
    val colors = CastivioTheme.colors
    val pad = frame.chipPad / 2
    InteractiveGlassCard(
        onClick = onClick,
        modifier = Modifier.size(width = width, height = height),
        shape = RoundedCornerShape(frame.radius / 2),
        fill = colors.homeCardSurface,
        restBorder = colors.cardSurfaceEdge,
    ) {
        // Left to right in every language (owner, 2026-11-25): the picture at the left and the name from the left, as in
        // English -- the names are the provider's, mostly Latin, and read from the left in an Arabic interface too.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = pad, vertical = pad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(pad),
            ) {
                picture((height - pad * 2) * THUMB_OF_CARD)
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = castivioChipStyle(frame.fsChip).copy(textDirection = TextDirection.Content, textAlign = TextAlign.Left),
                        color = colors.onBackgroundStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        line,
                        style = castivioBodyStyle(frame.fsChip * LINE_OF_CHIP).copy(textDirection = TextDirection.Content, textAlign = TextAlign.Left),
                        color = colors.onBackgroundMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (progress != null) {
                        Spacer(Modifier.height(pad / 2))
                        ProgrammeBar(fraction = progress)
                    }
                }
            }
        }
    }
}

/**
 * A slot with nothing watched in it yet: its section's picture across the card, only just seen, with its
 * objects on the side away from the words, and the section's name over "nothing watched yet".
 *
 * Unlike a filled card this one follows the interface's direction: its words are the application's own, not a provider's
 * name, so in Arabic they stand on the right with the picture on the left, as on the large cards, and the other way round in
 * a language read from the left -- where the picture, drawn for Arabic, is mirrored. There is no text in the pictures.
 */
@Composable
private fun EmptySlot(
    kind: MediaKind,
    width: Dp,
    height: Dp,
    frame: CastivioMetrics,
    onClick: () -> Unit,
) {
    val colors = CastivioTheme.colors
    val pad = frame.chipPad / 2
    val flip = LocalLayoutDirection.current == LayoutDirection.Ltr
    InteractiveGlassCard(
        onClick = onClick,
        modifier = Modifier.size(width = width, height = height),
        shape = RoundedCornerShape(frame.radius / 2),
        fill = colors.homeCardSurface,
        restBorder = colors.cardSurfaceEdge,
    ) {
        Box(Modifier.fillMaxSize()) {
            // The picture is placed from the left whatever the language -- the objects are on its left -- and turned over as a
            // whole where the words are on the left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Image(
                    painter = painterResource(sectionArt(kind)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    alpha = EMPTY_ART_ALPHA,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { if (flip) scaleX = -1f },
                )
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = pad * 2, vertical = pad),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    stringResource(sectionName(kind)),
                    style = castivioChipStyle(frame.fsChip),
                    color = colors.onBackgroundStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(nothingYet(kind)),
                    style = castivioBodyStyle(frame.fsChip * LINE_OF_CHIP),
                    color = colors.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun sectionArt(kind: MediaKind): Int = when (kind) {
    MediaKind.MOVIE -> R.drawable.home_art_movies
    MediaKind.SERIES -> R.drawable.home_art_series
    MediaKind.RADIO -> R.drawable.home_art_radio
    else -> R.drawable.home_art_live
}

private fun nothingYet(kind: MediaKind): Int = when (kind) {
    MediaKind.MOVIE -> R.string.home_continue_none_movies
    MediaKind.SERIES -> R.string.home_continue_none_series
    MediaKind.RADIO -> R.string.home_continue_none_radio
    else -> R.string.home_continue_none_live
}

/**
 * An empty slot's picture: far fainter than the large cards' 40% (owner, 2026-11-25) -- the same four pictures twice on one
 * screen was a repetition, so here they are only just there.
 */
private const val EMPTY_ART_ALPHA = 0.12f

/** The row always lays out four slots. */
private const val SLOTS = 4

/** A card's picture against the card's inner height. */
private const val THUMB_OF_CARD = 0.92f

/** A poster's width over its height. */
private const val POSTER_ASPECT = 0.68f

/** The line under a card's name against the frame's chip text. */
private const val LINE_OF_CHIP = 0.85f
