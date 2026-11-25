package com.castivio.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
 * "Continue watching": a row of small cards under the four sections (owner, 2026-11-25), one for each section something was
 * watched in -- the channel last on, the film, the show and the station -- and none for a section nothing was watched in.
 * The four large cards above it always open their section; carrying on is this row's alone.
 *
 * Each card is its picture (the channel's or the station's logo, the film's or the show's poster), its name, a line under it
 * -- what is on the channel now, or the episode -- and, where there is one, the aqua bar of how far in it the viewer was.
 * A press does what the large cards' resume faces did: the shell opens the section with that item started inside it.
 *
 * Drawn only when there is at least one card; with nothing watched yet the board takes its room.
 */
@Composable
internal fun ContinueRow(
    resume: Resume?,
    resumes: Map<MediaKind, SectionResume>,
    frame: CastivioMetrics,
    onResume: (Channel) -> Unit,
    onResumeSection: (SectionResume) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val sections = listOf(MediaKind.MOVIE, MediaKind.SERIES, MediaKind.RADIO).mapNotNull { resumes[it] }
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
                resume?.let { live ->
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
                }
                for (offer in sections) {
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

/** True when the row has a card to draw. */
internal fun hasContinue(resume: Resume?, resumes: Map<MediaKind, SectionResume>): Boolean =
    resume != null || listOf(MediaKind.MOVIE, MediaKind.SERIES, MediaKind.RADIO).any { resumes[it] != null }

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

/** The row always lays out four slots. */
private const val SLOTS = 4

/** A card's picture against the card's inner height. */
private const val THUMB_OF_CARD = 0.92f

/** A poster's width over its height. */
private const val POSTER_ASPECT = 0.68f

/** The line under a card's name against the frame's chip text. */
private const val LINE_OF_CHIP = 0.85f
