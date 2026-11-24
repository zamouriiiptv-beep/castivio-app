package com.castivio.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioLockup
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.components.castivioFocusScale
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.LocalThemeSwitch
import com.castivio.core.design.theme.Motion

/**
 * Home's header, as the owner drew it (design/mockups/home-header-ar.html, frame `v4`, approved 2026-11-24): one band across
 * the whole width with the lockup at one end, the clock and the date at the other, and between them the page's seven
 * buttons -- refresh, switch playlist, catch-up, settings, language, the appearance and exit -- each a glyph with its word
 * under it. A violet light behind the mark, and a line from violet to azure under the band.
 *
 * ## The band keeps its order in every language
 *
 * As the header always has: the signature does not swap sides per locale, so the subtree is pinned left to right and the
 * words inside each button are short enough that their own direction does not matter to the order. A screen reader is
 * given each button's word as its name.
 *
 * ## The words go where there is no room for them
 *
 * On a television and a tablet ([labelled]) every button carries its word; on a handset held sideways (833x385dp) seven
 * words do not fit beside the lockup and the clock, so the buttons are their glyphs and the word is what a screen reader
 * says. The licence and the subscription dates that sat here are in the footer now (`FooterLine`).
 *
 * ## What each button does
 *
 * Refresh asks the provider again ([HomeViewModel.refresh]) and says how it went on itself -- a spinner while it asks, the
 * success green or the failure red after, in the look [refreshLook] decides. Switch playlist opens the menu of saved
 * playlists ([PlaylistPicker]) hanging from the button. Catch-up, settings, language and exit are the shell's doors. The
 * appearance button turns the theme over through [LocalThemeSwitch], and is not drawn where no switch was provided.
 */
@Composable
internal fun HomeHeader(
    state: HomeState,
    frame: CastivioMetrics,
    labelled: Boolean,
    onRefresh: () -> Unit,
    onChoosePlaylist: (String) -> Unit,
    onAddPlaylist: () -> Unit,
    onManageSources: () -> Unit,
    onCatchUp: () -> Unit,
    onSettings: () -> Unit,
    onLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val themeSwitch = LocalThemeSwitch.current
    val ruleBrush = colors.headerRuleBrush
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier
                .background(colors.headerBandBrush)
                .drawBehind {
                    val rule = HEADER_RULE.toPx()
                    drawRect(ruleBrush, topLeft = Offset(0f, size.height - rule), size = Size(size.width, rule))
                }
                .padding(horizontal = frame.edge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(frame.chipPad),
        ) {
            CastivioLockup(
                markSize = frame.brand,
                wordSize = (frame.fsTitle.value * LOCKUP_WORD_RATIO).sp,
            )

            Spacer(Modifier.weight(1f))
            HeaderRule(frame)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 4),
            ) {
                RefreshAction(state, frame, labelled, onRefresh)
                PlaylistPicker(
                    state = state,
                    frame = frame,
                    onChoose = onChoosePlaylist,
                    onAdd = onAddPlaylist,
                    onManage = onManageSources,
                ) { anchor, open ->
                    HeaderAction(
                        icon = Icons.Rounded.Shuffle,
                        label = stringResource(R.string.home_change),
                        labelled = labelled,
                        frame = frame,
                        onClick = open,
                        modifier = anchor,
                    )
                }
                HeaderAction(Icons.Rounded.Schedule, stringResource(R.string.catchup_title), labelled, frame, onCatchUp)
                HeaderAction(Icons.Rounded.Settings, stringResource(R.string.home_settings), labelled, frame, onSettings)
                HeaderAction(Icons.Rounded.Language, stringResource(R.string.home_language), labelled, frame, onLanguage)
                if (themeSwitch != null) {
                    HeaderAction(
                        icon = if (themeSwitch.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        label = stringResource(R.string.home_theme),
                        labelled = labelled,
                        frame = frame,
                        onClick = themeSwitch::toggle,
                        spoken = stringResource(
                            if (themeSwitch.isDark) R.string.home_theme_lighter else R.string.home_theme_darker,
                        ),
                    )
                }
                HeaderAction(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.home_exit), labelled, frame, onExit)
            }

            HeaderRule(frame)
            CastivioClock(fsTime = frame.fsTitle, fsDate = frame.fsChip)
        }
    }
}

/** The thin upright line between the lockup, the buttons and the clock. */
@Composable
private fun HeaderRule(frame: CastivioMetrics) {
    Box(
        Modifier
            .fillMaxHeight(HEADER_RULE_HEIGHT)
            .width(1.dp)
            .background(CastivioTheme.colors.edgeQuiet),
    )
    // The rule takes no part in the spacing beyond its own width: the row's gap is on both sides of it.
    Spacer(Modifier.width(frame.chipPad / 4))
}

/**
 * Refresh, which says on itself whether it is asking and how it went: a spinner while the request is in flight (a press
 * then does nothing, because the answer of a second request would overwrite the first's), the success green when it was
 * answered, the failure red when it was not -- until the next press -- with the sentence a screen reader is given.
 */
@Composable
private fun RefreshAction(state: HomeState, frame: CastivioMetrics, labelled: Boolean, onRefresh: () -> Unit) {
    val colors = CastivioTheme.colors
    when (val look = refreshLook(state)) {
        RefreshLook.Busy -> HeaderAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.home_refresh),
            labelled = labelled,
            frame = frame,
            onClick = {},
            busy = true,
        )
        is RefreshLook.Failed -> HeaderAction(
            icon = Icons.Rounded.ErrorOutline,
            label = stringResource(R.string.home_refresh_failed),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
            hue = colors.danger,
            spoken = stringResource(refreshSentence(look.fault)),
        )
        RefreshLook.Answered -> HeaderAction(
            icon = Icons.Rounded.CheckCircle,
            label = stringResource(R.string.home_refresh_done),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
            hue = colors.success,
        )
        RefreshLook.Idle -> HeaderAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.home_refresh),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
        )
    }
}

/**
 * One button of the header: its glyph, and its word under it where there is room. Bare at rest; under the remote, the
 * brand ramp carried with the focus ring round it, so the button in hand is the one lit thing in the band.
 */
@Composable
private fun HeaderAction(
    icon: ImageVector,
    label: String,
    labelled: Boolean,
    frame: CastivioMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The glyph's colour when the button reports something; the ink otherwise. */
    hue: Color? = null,
    /** What a screen reader says, when the word alone is not the whole of it. */
    spoken: String = label,
    busy: Boolean = false,
) {
    val colors = CastivioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(frame.radius / 2)
    val glyph = frame.touchTarget * GLYPH_OF_TARGET
    Column(
        modifier
            .castivioFocusScale(Motion.focusScaleButton, interaction)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .then(
                if (focused) {
                    Modifier
                        .background(colors.headerActionFocusBrush)
                        .border(BorderStroke(1.dp, colors.focusRing), shape)
                } else {
                    Modifier
                },
            )
            .clickable(interaction, indication = null, onClick = { if (!busy) onClick() })
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .widthIn(min = frame.touchTarget)
            .heightIn(min = frame.touchTarget)
            .padding(horizontal = frame.chipPad / 2, vertical = ACTION_PAD_V),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ACTION_GAP, Alignment.CenterVertically),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(glyph),
                color = colors.hueViolet,
                strokeWidth = BUSY_RING,
            )
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = hue ?: colors.onBackgroundStrong,
                modifier = Modifier.size(glyph),
            )
        }
        if (labelled) {
            Text(
                label,
                style = castivioChipStyle(frame.fsChip * LABEL_OF_CHIP),
                color = when {
                    hue != null -> hue
                    focused -> colors.onBackgroundStrong
                    else -> colors.onBackgroundVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** From this height a handset or a tablet has room for the buttons' words; a television always has. */
internal val LABELLED_HEADER_MIN_HEIGHT: Dp = 480.dp

/** The wordmark beside the mark, against the frame's title size: the drawing's 22 against 30. */
private const val LOCKUP_WORD_RATIO = 0.75f

/** The line under the band. */
private val HEADER_RULE = 2.dp

/** The upright rules' height, of the band's. */
private const val HEADER_RULE_HEIGHT = 0.46f

/** A button's glyph, of the touch target: 24dp of a television's 48. */
private const val GLYPH_OF_TARGET = 0.5f

/** A button's word, of the frame's chip text: the drawing's 10.5 against 13. */
private const val LABEL_OF_CHIP = 0.85f

private val ACTION_PAD_V = 4.dp
private val ACTION_GAP = 3.dp
private val BUSY_RING = 2.dp
