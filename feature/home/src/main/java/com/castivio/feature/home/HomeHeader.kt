package com.castivio.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioHeaderAction
import com.castivio.core.design.components.CastivioHeaderRule
import com.castivio.core.design.components.CastivioLockup
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme

/**
 * Home's header, as the owner drew it (design/mockups/home-header-ar.html, frame `v4`, approved 2026-11-24): one band across
 * the whole width with the lockup at one end, the clock and the date at the other, and between them the page's seven
 * buttons -- refresh, switch playlist, catch-up, account, settings, language and exit -- each a glyph with its word
 * under it, standing on the application's own backdrop.
 *
 * ## The band keeps its order in every language
 *
 * As the header always has: the signature does not swap sides per locale, so the subtree is pinned left to right and the
 * words inside each button are short enough that their own direction does not matter to the order. A screen reader is
 * given each button's word as its name.
 *
 * ## Every button carries its word
 *
 * On every device (owner, 2026-11-25): on a handset the glyphs alone were not read. [labelled] stays a parameter so a
 * frame too narrow for seven words can still ask for glyphs; a screen reader is given each word as the button's name.
 *
 * ## What each button does
 *
 * Refresh asks the provider again ([HomeViewModel.refresh]) and says how it went on itself -- a spinner while it asks, the
 * success green or the failure red after, in the look [refreshLook] decides. Switch playlist opens the menu of saved
 * playlists ([PlaylistPicker]) hanging from the button. Catch-up, settings, language and exit are the shell's doors. The
 * application has one appearance (owner, 2026-11-25), so there is no appearance button.
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
    /** Opens the device's account: its address, key, state, licence and subscription. */
    onAccount: () -> Unit,
    onSettings: () -> Unit,
    onLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            // No band and no line of their own (owner, 2026-11-25): the buttons stand on the application's own backdrop.
            modifier
                .padding(horizontal = frame.edge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(frame.chipPad),
        ) {
            CastivioLockup(
                markSize = frame.brand,
                wordSize = (frame.fsTitle.value * LOCKUP_WORD_RATIO).sp,
            )

            Spacer(Modifier.weight(1f))
            CastivioHeaderRule(frame)

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
                    CastivioHeaderAction(
                        icon = Icons.Rounded.Shuffle,
                        label = stringResource(R.string.home_change),
                        labelled = labelled,
                        frame = frame,
                        onClick = open,
                        modifier = anchor,
                    )
                }
                CastivioHeaderAction(Icons.Rounded.Schedule, stringResource(R.string.catchup_title), labelled, frame, onCatchUp)
                CastivioHeaderAction(Icons.Rounded.Person, stringResource(R.string.home_account), labelled, frame, onAccount)
                CastivioHeaderAction(Icons.Rounded.Settings, stringResource(R.string.home_settings), labelled, frame, onSettings)
                CastivioHeaderAction(Icons.Rounded.Language, stringResource(R.string.home_language), labelled, frame, onLanguage)
                CastivioHeaderAction(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.home_exit), labelled, frame, onExit)
            }

            CastivioHeaderRule(frame)
            CastivioClock(fsTime = frame.fsTitle, fsDate = frame.fsChip)
        }
    }
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
        RefreshLook.Busy -> CastivioHeaderAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.home_refresh),
            labelled = labelled,
            frame = frame,
            onClick = {},
            busy = true,
        )
        is RefreshLook.Failed -> CastivioHeaderAction(
            icon = Icons.Rounded.ErrorOutline,
            label = stringResource(R.string.home_refresh_failed),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
            hue = colors.danger,
            spoken = stringResource(refreshSentence(look.fault)),
        )
        RefreshLook.Answered -> CastivioHeaderAction(
            icon = Icons.Rounded.CheckCircle,
            label = stringResource(R.string.home_refresh_done),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
            hue = colors.success,
        )
        RefreshLook.Idle -> CastivioHeaderAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.home_refresh),
            labelled = labelled,
            frame = frame,
            onClick = onRefresh,
        )
    }
}


/** The wordmark beside the mark, against the frame's title size: the drawing's 22 against 30. */
private const val LOCKUP_WORD_RATIO = 0.75f
