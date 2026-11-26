package com.castivio.feature.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.Sizing
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.CastivioLockup
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.castivio.core.design.components.ButtonWeight
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.components.CastivioIconButton
import com.castivio.core.design.components.QrPlate
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.components.castivioTitleStyle
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.MotionLevel
import com.castivio.core.design.theme.Radius
import com.castivio.core.design.theme.Spacing
import com.castivio.core.design.theme.castivioBackdrop
import com.castivio.core.design.theme.castivioStage
import com.castivio.core.design.theme.rememberMetrics
import com.castivio.domain.TrackLanguage

/**
 * A row only a development build draws -- the design board, the device's class -- handed in by the application, which
 * alone knows which build it is. A null [onClick] is a line of information.
 */
data class DeveloperRow(val label: String, val value: String? = null, val onClick: (() -> Unit)? = null)

/**
 * Settings, as `design/mockups/settings-ar.html` draws it: two columns of section cards.
 *
 * Only what has no other place, and does something. Theme and language are at the top of Home; the subscriptions and the
 * device are on Home; the player's speed, picture shape, quality, sound and subtitles' look are in its own gear panel; a
 * channel is locked from the channels board. What is here:
 *  - **Player**: the language preferred for the sound and for subtitles, picked from a list over the page. The player takes
 *    the track in that language when a film or channel has one (see `PlayerViewModel.preferLanguages`).
 *  - **Hidden items**: bringing back the bouquets and the channels hidden on the channels board, with how many; none
 *    hidden is a line of information with nothing to press.
 *  - **Channel lock**: the lock's password.
 *  - **Application**: the motion level, emptying the pictures' cache (with its size), and About -- the licence screen.
 *  - **Help**: support, the privacy policy and the terms, which are pages of Castivio's site ([SettingsLinks]). A
 *    television opens no link, so each is a QR code a phone scans, with the address written out; a phone opens them.
 *
 * Everything the application owns -- the motion level, the hidden items, the password, the licence -- comes in as values
 * and callbacks; what is Settings' own is [SettingsViewModel]'s.
 */
@Composable
fun SettingsScreen(
    motion: MotionLevel,
    onMotion: (MotionLevel) -> Unit,
    hiddenBouquets: Int,
    hiddenChannels: Int,
    onRestoreBouquets: () -> Unit,
    onRestoreChannels: () -> Unit,
    onChangePin: () -> Unit,
    onAbout: () -> Unit,
    version: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    developerTitle: String? = null,
    developer: List<DeveloperRow> = emptyList(),
    /**
     * The measurement panels are drawn in this build -- a development or a measurement build, never a viewer's
     * (`PerformanceLog.isVisibleIn`): Settings then offers the smoothness panel.
     */
    measuring: Boolean = false,
    model: SettingsViewModel = hiltViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    // What is open over the page, if anything: a list of languages, or a page of the site. Back closes it first.
    var choosing by rememberSaveable { mutableStateOf<LanguageKind?>(null) }
    var reading by rememberSaveable { mutableStateOf<SitePage?>(null) }
    var smoothness by rememberSaveable { mutableStateOf(false) }
    // The section open beside the list: the player's first, as the list is in that order.
    var pane by rememberSaveable { mutableStateOf(SettingsPane.PLAYER) }
    val panes = remember(measuring, developerTitle, developer.isNotEmpty()) {
        SettingsPane.entries.filter {
            when (it) {
                SettingsPane.MEASURE -> measuring
                SettingsPane.DEVELOPER -> developerTitle != null && developer.isNotEmpty()
                else -> true
            }
        }
    }
    val firstRow = remember { FocusRequester() }
    val tv = CastivioTheme.device.isTv

    BoxWithConstraints(modifier.fillMaxSize().castivioBackdrop()) {
        val m = rememberMetrics(maxWidth, maxHeight)
        // Left out while a page of the site covers it, so the remote cannot wander into rows nobody can see.
        //
        // **A side list of the sections and the open one beside it** (owner, 2026-11-26,
        // design/mockups/settings-sidebar-ar.png): the channels' board's own way -- a column to choose from at the start, what
        // was chosen beside it, on Home's cards. No band: this is a place to change something and leave.
        if (reading == null && !smoothness) Column(
            Modifier
                .fillMaxSize()
                .castivioStage(m),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            TitleRow(stringResource(R.string.prefs_title), Icons.Rounded.Settings, m, onBack)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                SettingsNav(
                    panes = panes,
                    open = pane,
                    developerTitle = developerTitle,
                    m = m,
                    first = firstRow,
                    onOpen = { pane = it },
                    modifier = Modifier.fillMaxWidth(NAV_SHARE).fillMaxHeight(),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .testTag(SETTINGS_PAGE_TAG),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    when (pane) {
                    SettingsPane.PLAYER -> {
                        Section(Icons.Rounded.PlayCircle, stringResource(R.string.prefs_player), m) {
                            SettingRow(
                                Icons.AutoMirrored.Rounded.VolumeUp,
                                stringResource(R.string.prefs_audio_language),
                                state.audio?.endonym ?: stringResource(R.string.prefs_audio_original),
                                m,
                                onClick = { choosing = LanguageKind.AUDIO },
                            )
                            SettingRow(
                                Icons.Rounded.ClosedCaption,
                                stringResource(R.string.prefs_subtitle_language),
                                state.subtitle?.endonym ?: stringResource(R.string.prefs_subtitles_none),
                                m,
                                onClick = { choosing = LanguageKind.SUBTITLE },
                            )
                        }
                    }
                    SettingsPane.LIBRARY -> {
                        Section(Icons.Rounded.VideoLibrary, stringResource(R.string.prefs_library), m) {
                            SwitchRow(
                                Icons.Rounded.Download,
                                stringResource(R.string.prefs_auto_load),
                                stringResource(R.string.prefs_auto_load_detail),
                                on = state.autoLoad,
                                m = m,
                                onChange = model::setAutoLoad,
                            )
                            SettingRow(
                                Icons.AutoMirrored.Rounded.ListAlt,
                                stringResource(R.string.prefs_restore_bouquets),
                                hiddenBouquets.toString(),
                                m,
                                onClick = onRestoreBouquets.takeIf { hiddenBouquets > 0 },
                            )
                            SettingRow(
                                Icons.Rounded.Tv,
                                stringResource(R.string.prefs_restore_channels),
                                hiddenChannels.toString(),
                                m,
                                onClick = onRestoreChannels.takeIf { hiddenChannels > 0 },
                            )
                        }
                    }
                    SettingsPane.LOCK -> {
                        Section(Icons.Rounded.Lock, stringResource(R.string.prefs_lock), m) {
                            SettingRow(Icons.Rounded.Key, stringResource(R.string.prefs_change_pin), null, m, onClick = onChangePin)
                        }
                    }
                    SettingsPane.APPLICATION -> {
                        Section(Icons.Rounded.Apps, stringResource(R.string.prefs_application), m) {
                            MotionRow(motion, onMotion, m)
                            val context = LocalContext.current
                            SettingRow(
                                Icons.Rounded.Image,
                                stringResource(R.string.prefs_clear_pictures),
                                state.pictureBytes?.let { Formatter.formatShortFileSize(context, it) },
                                m,
                                onClick = if (state.clearing) null else model::clearPictures,
                                ltrValue = true,
                            )
                            SettingRow(Icons.Rounded.Info, stringResource(R.string.prefs_about), version, m, onClick = onAbout, ltrValue = true)
                        }
                    }
                    SettingsPane.HELP -> {
                        Section(Icons.AutoMirrored.Rounded.Help, stringResource(R.string.prefs_help), m) {
                            SettingRow(Icons.Rounded.HeadsetMic, stringResource(R.string.prefs_support), null, m, onClick = { reading = SitePage.SUPPORT })
                            SettingRow(Icons.Rounded.Shield, stringResource(R.string.prefs_privacy), null, m, onClick = { reading = SitePage.PRIVACY })
                            SettingRow(Icons.Rounded.Description, stringResource(R.string.prefs_terms), null, m, onClick = { reading = SitePage.TERMS })
                        }
                    }
                    SettingsPane.MEASURE -> {
                        // A measurement tool, in English like the strips it sits beside: never in a viewer's build.
                        Section(Icons.Rounded.Speed, SMOOTHNESS_TITLE, m) {
                            SettingRow(Icons.Rounded.Timeline, SMOOTHNESS_ROW, null, m, onClick = { smoothness = true })
                        }
                    }
                    SettingsPane.DEVELOPER -> {
                        Section(Icons.Rounded.Code, developerTitle.orEmpty(), m) {
                            developer.forEach { SettingRow(Icons.Rounded.Code, it.label, it.value, m, onClick = it.onClick) }
                        }
                    }
                    }
                }
            }
        }
        // The remote starts on the first row, as every screen of the application starts on its first control.
        LaunchedEffect(tv, reading, smoothness) { if (tv && reading == null && !smoothness) runCatching { firstRow.requestFocus() } }

        choosing?.let { kind ->
            LanguageSheet(
                kind = kind,
                chosen = if (kind == LanguageKind.AUDIO) state.audio else state.subtitle,
                m = m,
                onChoose = { language ->
                    if (kind == LanguageKind.AUDIO) model.setAudio(language) else model.setSubtitle(language)
                    choosing = null
                },
                onClose = { choosing = null },
            )
        }
        reading?.let { page -> SitePageView(page, tv, m, onClose = { reading = null }) }
        if (smoothness) SmoothnessPanel(m, onClose = { smoothness = false })
    }
}

/** The sections of Settings, in the side list's order; the last two only in a measuring or a development build. */
internal enum class SettingsPane { PLAYER, LIBRARY, LOCK, APPLICATION, HELP, MEASURE, DEVELOPER }

/** A section's entry in the side list carries this tag, so a test can open it. */
internal fun settingsNavTag(pane: SettingsPane): String = "castivio.settings.nav.${pane.name.lowercase()}"

/**
 * The side list (owner, 2026-11-26): the mark at its head, then each section -- its glyph and its name -- with the open one in
 * the chosen colour, as the channels' board marks its open bouquet. A press opens the section beside it; the remote starts on
 * the open entry.
 */
@Composable
private fun SettingsNav(
    panes: List<SettingsPane>,
    open: SettingsPane,
    developerTitle: String?,
    m: CastivioMetrics,
    first: FocusRequester,
    onOpen: (SettingsPane) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Column(
        modifier
            .clip(shape)
            .background(colors.homeCardSurface)
            .border(1.dp, colors.cardSurfaceEdge, shape)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CastivioLockup(
            markSize = m.brand * NAV_MARK_OF_BRAND,
            wordSize = (m.fsTitle.value * NAV_WORD_OF_TITLE).sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.sm),
        )
        panes.forEach { pane ->
            val selected = pane == open
            val (icon, label) = when (pane) {
                SettingsPane.PLAYER -> Icons.Rounded.PlayCircle to stringResource(R.string.prefs_player)
                SettingsPane.LIBRARY -> Icons.Rounded.VideoLibrary to stringResource(R.string.prefs_library)
                SettingsPane.LOCK -> Icons.Rounded.Lock to stringResource(R.string.prefs_lock)
                SettingsPane.APPLICATION -> Icons.Rounded.Apps to stringResource(R.string.prefs_application)
                SettingsPane.HELP -> Icons.AutoMirrored.Rounded.Help to stringResource(R.string.prefs_help)
                SettingsPane.MEASURE -> Icons.Rounded.Speed to SMOOTHNESS_TITLE
                SettingsPane.DEVELOPER -> Icons.Rounded.Code to developerTitle.orEmpty()
            }
            InteractiveGlassCard(
                onClick = { onOpen(pane) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(settingsNavTag(pane))
                    .semantics { this.selected = selected }
                    .then(if (selected) Modifier.focusRequester(first) else Modifier),
                shape = RoundedCornerShape(Radius.sm),
                fill = SolidColor(if (selected) colors.secondary else colors.glassFill),
                restBorder = if (selected) null else colors.cardSurfaceEdge,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizing.minTarget(CastivioTheme.device.isTv))
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        icon,
                        null,
                        tint = if (selected) colors.onSecondary else colors.secondary,
                        modifier = Modifier.size(SECTION_ICON),
                    )
                    Text(
                        label,
                        style = castivioChipStyle(m.fsLabel),
                        color = if (selected) colors.onSecondary else colors.onBackgroundStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The two lists of languages a viewer can prefer: the sound's, whose "none" is the original sound, and the subtitles'. */
internal enum class LanguageKind { AUDIO, SUBTITLE }

/** The way back, the screen's mark and its name. */
@Composable
private fun TitleRow(title: String, icon: ImageVector, m: CastivioMetrics, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        CastivioIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.prefs_back),
            onClick = onBack,
        )
        Icon(icon, null, tint = CastivioTheme.colors.onBackground, modifier = Modifier.size(m.fsTitle))
        Text(title, style = castivioTitleStyle(m.fsTitle), color = CastivioTheme.colors.onBackground, maxLines = 1)
    }
}

/** A section: its mark and name in the secondary hue, then its rows, on one card. */
@Composable
private fun Section(icon: ImageVector, title: String, m: CastivioMetrics, rows: @Composable () -> Unit) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Column(
        Modifier
            .fillMaxWidth()
            // Home's own card (owner, 2026-11-26): its calm indigo and violet edge, as every board's columns now are.
            .clip(shape)
            .background(colors.homeCardSurface)
            .border(1.dp, colors.cardSurfaceEdge, shape)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(icon, null, tint = colors.secondary, modifier = Modifier.size(SECTION_ICON))
            Text(
                title,
                style = castivioChipStyle(m.fsLabel).copy(fontWeight = FontWeight.Bold),
                color = colors.secondary,
                maxLines = 1,
            )
        }
        rows()
    }
}

/**
 * One row: its mark, its name, what it is now, and the arrow into it. With nothing to press ([onClick] null) it is a line of
 * information -- quieter, without the arrow, and the remote passes over it.
 */
@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    value: String?,
    m: CastivioMetrics,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    ltrValue: Boolean = false,
) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    var focused by remember { mutableStateOf(false) }
    val live = onClick != null
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(shape)
            .background(if (focused) colors.glassFillStrong else if (live) colors.glassFill else colors.glassFill.copy(alpha = 0f))
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> colors.focusRing
                    live -> colors.glassFill.copy(alpha = 0f)
                    else -> colors.glassBorderSoft
                },
                shape,
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            icon,
            null,
            tint = if (live) colors.onBackgroundVariant else colors.onBackgroundMuted,
            modifier = Modifier.size(ROW_ICON),
        )
        Text(
            label,
            style = castivioBodyStyle(m.fsBody),
            color = if (live) colors.onBackground else colors.onBackgroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(
                it,
                style = castivioBodyStyle(m.fsBody).let { style -> if (ltrValue) style.copy(textDirection = TextDirection.Ltr) else style },
                color = if (live) colors.onBackgroundVariant else colors.onBackgroundMuted,
                maxLines = 1,
            )
        }
        if (live) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = if (focused) colors.onBackground else colors.onBackgroundVariant,
                modifier = Modifier.size(ROW_ICON),
            )
        }
    }
}

/**
 * A row that is a switch: its mark, its name with a line under it, and the switch at its end. The whole row is the control,
 * so the remote and a thumb both turn it over, and a reader hears it as one switch.
 */
@Composable
private fun SwitchRow(icon: ImageVector, label: String, detail: String, on: Boolean, m: CastivioMetrics, onChange: (Boolean) -> Unit) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(shape)
            .background(if (focused) colors.glassFillStrong else colors.glassFill)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.glassFill.copy(alpha = 0f), shape)
            .onFocusChanged { focused = it.isFocused }
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, null, tint = colors.onBackgroundVariant, modifier = Modifier.size(ROW_ICON))
        Column(Modifier.weight(1f)) {
            Text(label, style = castivioBodyStyle(m.fsBody), color = colors.onBackground, maxLines = 2)
            Text(detail, style = castivioChipStyle(m.fsChip), color = colors.onBackgroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = on,
            // The row is the control: the switch only shows where it stands.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.onPrimary,
                uncheckedTrackColor = colors.glassFillStrong,
                uncheckedThumbColor = colors.onBackgroundVariant,
                uncheckedBorderColor = colors.glassBorder,
            ),
        )
    }
}

/** The motion level: its name, and its three levels in the row itself; left and right on the remote move between them. */
@Composable
private fun MotionRow(motion: MotionLevel, onMotion: (MotionLevel) -> Unit, m: CastivioMetrics) {
    val colors = CastivioTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(RoundedCornerShape(Radius.sm))
            .background(colors.glassFill)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(Icons.Rounded.Waves, null, tint = colors.onBackgroundVariant, modifier = Modifier.size(ROW_ICON))
        Text(
            stringResource(R.string.prefs_motion),
            style = castivioBodyStyle(m.fsBody),
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        MotionLevel.entries.forEach { level ->
            LevelChip(
                label = stringResource(level.labelRes()),
                selected = level == motion,
                m = m,
                onClick = { onMotion(level) },
            )
        }
    }
}

@Composable
private fun LevelChip(label: String, selected: Boolean, m: CastivioMetrics, onClick: () -> Unit) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.xs)
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .heightIn(min = CHIP_HEIGHT)
            .clip(shape)
            .background(if (selected) colors.primary else colors.glassFillStrong)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.glassBorderSoft, shape)
            .onFocusChanged { focused = it.isFocused }
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = castivioChipStyle(m.fsChip).copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) colors.onPrimary else colors.onBackgroundVariant,
            maxLines = 1,
        )
    }
}

private fun MotionLevel.labelRes(): Int = when (this) {
    MotionLevel.FULL -> R.string.prefs_motion_full
    MotionLevel.REDUCED -> R.string.prefs_motion_reduced
    MotionLevel.DISABLED -> R.string.prefs_motion_none
}

/**
 * The languages to prefer, as a list over the page at its end edge: "none" first -- the original sound, or no subtitles
 * -- then each language named in itself, the one in use ticked and under the remote when the list opens.
 */
@Composable
private fun LanguageSheet(
    kind: LanguageKind,
    chosen: TrackLanguage?,
    m: CastivioMetrics,
    onChoose: (TrackLanguage?) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val colors = CastivioTheme.colors
    val current = remember { FocusRequester() }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            // Over the page: a touch outside the list closes it, and nothing underneath is reached.
            .pointerInput(Unit) { detectTapAnywhere(onClose) },
        // At the end edge, as the mockup draws it: the page's titles and first rows stay readable at the start.
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(SHEET_WIDTH)
                .background(colors.surfaceWell)
                .border(1.dp, colors.glassBorder)
                .pointerInput(Unit) { detectTapAnywhere {} }
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(if (kind == LanguageKind.AUDIO) R.string.prefs_audio_language else R.string.prefs_subtitle_language),
                style = castivioTitleStyle(m.fsLabel),
                color = colors.onBackground,
            )
            Text(
                stringResource(if (kind == LanguageKind.AUDIO) R.string.prefs_audio_detail else R.string.prefs_subtitle_detail),
                style = castivioBodyStyle(m.fsBody),
                color = colors.onBackgroundMuted,
            )
            val none = stringResource(if (kind == LanguageKind.AUDIO) R.string.prefs_audio_original else R.string.prefs_subtitles_none)
            (listOf<TrackLanguage?>(null) + TrackLanguage.entries).forEach { language ->
                val on = language == chosen
                LanguageOption(
                    label = language?.endonym ?: none,
                    on = on,
                    m = m,
                    onClick = { onChoose(language) },
                    modifier = if (on) Modifier.focusRequester(current) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { current.requestFocus() } }
}

@Composable
private fun LanguageOption(label: String, on: Boolean, m: CastivioMetrics, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(shape)
            .background(if (on) colors.selectedFill else colors.glassFill)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.glassFill.copy(alpha = 0f), shape)
            .onFocusChanged { focused = it.isFocused }
            .semantics { selected = on }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = castivioBodyStyle(m.fsBody).copy(textDirection = TextDirection.Content),
            color = colors.onBackground,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (on) Icon(Icons.Rounded.Check, null, tint = colors.primary, modifier = Modifier.size(ROW_ICON))
    }
}

/**
 * A page of Castivio's site. On a television, its QR code to scan with a phone, and the address -- and for support, the
 * e-mail -- written out beside it. On a phone, a button that opens the browser, and for support one that opens the mail
 * application.
 */
@Composable
private fun SitePageView(page: SitePage, tv: Boolean, m: CastivioMetrics, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val colors = CastivioTheme.colors
    val uri = LocalUriHandler.current
    val title = stringResource(
        when (page) {
            SitePage.SUPPORT -> R.string.prefs_support
            SitePage.PRIVACY -> R.string.prefs_privacy
            SitePage.TERMS -> R.string.prefs_terms
        },
    )
    val icon = when (page) {
        SitePage.SUPPORT -> Icons.Rounded.HeadsetMic
        SitePage.PRIVACY -> Icons.Rounded.Shield
        SitePage.TERMS -> Icons.Rounded.Gavel
    }
    val detail = stringResource(if (page == SitePage.SUPPORT) R.string.prefs_support_detail else R.string.prefs_on_site)
    Column(
        Modifier
            .fillMaxSize()
            .castivioBackdrop()
            .pointerInput(Unit) { detectTapAnywhere {} }
            .castivioStage(m)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        TitleRow(title, icon, m, onClose)
        val shape = RoundedCornerShape(Radius.lg)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.glassFill)
                .border(1.dp, colors.glassBorderSoft, shape)
                .padding(Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tv) QrSymbol(page.url, QR_SIDE)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (tv) {
                    Text(stringResource(R.string.prefs_scan), style = castivioTitleStyle(m.fsLabel), color = colors.onBackground)
                    Text(detail, style = castivioBodyStyle(m.fsBody), color = colors.onBackgroundVariant)
                    Address(Icons.Rounded.Language, SettingsLinks.shown(page.url), m)
                    if (page == SitePage.SUPPORT) Address(Icons.Rounded.Email, SettingsLinks.EMAIL, m)
                } else {
                    Text(detail, style = castivioBodyStyle(m.fsBody), color = colors.onBackgroundVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CastivioButton(
                            text = stringResource(R.string.prefs_open_browser),
                            onClick = { runCatching { uri.openUri(page.url) } },
                            weight = ButtonWeight.Primary,
                            icon = Icons.Rounded.Language,
                        )
                        if (page == SitePage.SUPPORT) {
                            CastivioButton(
                                text = SettingsLinks.EMAIL,
                                onClick = { runCatching { uri.openUri(SettingsLinks.MAIL) } },
                                icon = Icons.Rounded.Email,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** An address written out to be typed or read: what a viewer who will not scan has. */
@Composable
private fun Address(icon: ImageVector, text: String, m: CastivioMetrics) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(shape)
            .background(colors.glassFill)
            .border(1.dp, colors.glassBorderSoft, shape)
            // Read, not pressed: the remote may still rest on it, so the address can be read under the focus.
            .focusable()
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, null, tint = colors.secondary, modifier = Modifier.size(ROW_ICON))
        Text(
            text,
            style = castivioTitleStyle(m.fsLabel).copy(textDirection = TextDirection.Ltr),
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A page's QR code on the shared white plate (`QrPlate`), at the size the screen gives it. */
@Composable
private fun QrSymbol(url: String, side: Dp) {
    val pixels = with(LocalDensity.current) { side.roundToPx() }
    val code = remember(url, pixels) { siteQrBitmap(url, pixels).asImageBitmap() }
    QrPlate(code = code, plate = side, padding = Spacing.md)
}

/** Takes every tap in this area: to close what is open over the page, or ([onTap] doing nothing) to keep it from the page. */
private suspend fun PointerInputScope.detectTapAnywhere(onTap: () -> Unit) {
    detectTapGestures(onTap = { onTap() })
}

/** The page's scrolling column, for a test to find and scroll. */
const val SETTINGS_PAGE_TAG = "castivio.settings.page"

private val ROW_HEIGHT: Dp = 48.dp
private val CHIP_HEIGHT: Dp = 36.dp
private val ROW_ICON: Dp = 20.dp
private val SECTION_ICON: Dp = 18.dp

/** The side list, of the page's width. */
private const val NAV_SHARE = 0.3f

/** The mark and the word at the side list's head, against Home's lockup: a little smaller, in a narrower place. */
private const val NAV_MARK_OF_BRAND = 0.9f
private const val NAV_WORD_OF_TITLE = 0.6f
private val SHEET_WIDTH: Dp = 360.dp
private val QR_SIDE: Dp = 220.dp
