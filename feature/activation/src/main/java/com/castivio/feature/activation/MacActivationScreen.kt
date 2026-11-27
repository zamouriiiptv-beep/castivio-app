package com.castivio.feature.activation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.VpnKey
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.castivio.core.common.config.ActivationDestination
import com.castivio.core.design.components.ButtonWeight
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioFittedText
import com.castivio.core.design.components.CastivioHeaderAction
import com.castivio.core.design.components.CastivioHeaderRule
import com.castivio.core.design.components.CastivioLockup
import com.castivio.core.design.components.CopyButton
import com.castivio.core.design.components.QrPlate
import com.castivio.core.design.components.castivioFocusScale
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.CastivioType
import com.castivio.core.design.theme.Motion
import com.castivio.core.design.theme.castivioMetrics
import com.castivio.core.design.theme.castivioStage
import com.castivio.domain.identity.IdentityProvenance

/**
 * Every number on the first screen, for one frame.
 *
 * ## One unit, from the drawing
 *
 * The approved drawing is `design/mockups/add-playlist-ar.png`, 2340×1080 pixels — a
 * landscape phone — and the screen is that drawing (owner, 2026-11-26). Every size
 * below is the drawing's own pixel count times [unit], one drawing pixel in dp on this
 * frame: by the height (1080 pixels), unless the row of cards (2120 pixels) or what is
 * under the header ([CONTENT] pixels) is the tighter fit. The header is Home's band at
 * Home's numbers, so the two screens wear one header — the licence screen's arrangement.
 */
internal data class Metrics(
    val frame: CastivioMetrics,
    val unit: Dp,
    /** The header's own height: the frame's, never under the touch target. */
    val header: Dp,
    val headerBand: Dp,
    val titleSize: Dp,
    val titleIcon: Dp,

    val cardsTop: Dp,
    val cardHeight: Dp,
    val cardGap: Dp,
    val radius: Dp,

    /* the send card, left */
    val sendTitleBand: Dp,
    val sendTitleSize: Dp,
    val sendIcon: Dp,
    val innerInset: Dp,
    val innerPad: Dp,
    val innerGap: Dp,
    val plate: Dp,
    val stepSize: Dp,
    val stepGap: Dp,
    val stepBadge: Dp,
    val stepNumber: Dp,
    val pillHeight: Dp,
    val pillSize: Dp,
    val pillIcon: Dp,

    /* the identifiers, right, top */
    val idsHeight: Dp,
    val idsGap: Dp,
    val idIcon: Dp,
    val idLabelSize: Dp,
    val idValueSize: Dp,
    val copy: Dp,

    /* adding here, right, below */
    val headingTop: Dp,
    val headingSize: Dp,
    val headingIcon: Dp,
    val subSize: Dp,
    val kindsTop: Dp,
    val kindHeight: Dp,
    val kindDisc: Dp,
    val kindIcon: Dp,
    val kindNameSize: Dp,
    val kindSubSize: Dp,
    val ctaTop: Dp,
    val ctaHeight: Dp,
    val ctaSize: Dp,

    /* the line at the foot */
    val noteTop: Dp,
    val noteSize: Dp,
    val noteRule: Dp,
) {
    val target: Dp get() = frame.touchTarget
}

internal fun metricsFor(tv: Boolean, width: Dp, height: Dp): Metrics {
    val frame = castivioMetrics(width, height, tv)
    val header = maxOf(frame.header, frame.touchTarget)
    val headerBand = header + frame.stageTop
    val below = height - headerBand - frame.stageBottom
    val unit = minOf(height / DRAWING_HEIGHT, (width - frame.edge * 2) / ROW_WIDTH, below / CONTENT)
    fun px(drawing: Float): Dp = unit * drawing
    return Metrics(
        frame = frame,
        unit = unit,
        header = header,
        headerBand = headerBand,
        titleSize = px(54f),
        titleIcon = px(56f),
        cardsTop = px(40f),
        cardHeight = px(CARD_HEIGHT),
        cardGap = px(CARD_GAP),
        radius = px(30f),
        sendTitleBand = px(150f),
        sendTitleSize = px(46f),
        sendIcon = px(52f),
        innerInset = px(20f),
        innerPad = px(28f),
        innerGap = px(26f),
        plate = px(PLATE),
        stepSize = px(27f),
        stepGap = px(30f),
        stepBadge = px(52f),
        stepNumber = px(28f),
        pillHeight = px(72f),
        pillSize = px(28f),
        pillIcon = px(34f),
        idsHeight = maxOf(px(116f), frame.touchTarget),
        idsGap = px(50f),
        idIcon = px(52f),
        idLabelSize = px(30f),
        idValueSize = px(34f),
        copy = maxOf(px(66f), frame.touchTarget),
        headingTop = px(44f),
        headingSize = px(52f),
        headingIcon = px(60f),
        subSize = px(32f),
        kindsTop = px(34f),
        kindHeight = maxOf(px(150f), frame.touchTarget),
        kindDisc = px(96f),
        kindIcon = px(48f),
        kindNameSize = px(40f),
        kindSubSize = px(26f),
        ctaTop = px(40f),
        ctaHeight = maxOf(px(88f), frame.touchTarget),
        ctaSize = px(36f),
        noteTop = px(NOTE_TOP),
        noteSize = px(28f),
        noteRule = px(300f),
    )
}

/** What the content under the header stacks: the cards, the gap, the note's line. */
internal fun Metrics.contentHeight(): Dp = cardsTop + cardHeight + noteTop + (noteSize.value * LEADING).dp

/** The row of the two cards and the gap between them. */
internal fun Metrics.rowWidth(): Dp = unit * ROW_WIDTH

/** What the right-hand column stacks under the identifiers, which must fit the card's height. */
internal fun Metrics.rightColumnHeight(): Dp =
    idsHeight + headingTop + (headingSize.value * LEADING).dp + (subSize.value * LEADING).dp +
        kindsTop + kindHeight + ctaTop + ctaHeight

private const val DRAWING_HEIGHT = 1080f
internal const val SEND_WIDTH = 724.5f
internal const val ADD_WIDTH = 1345.5f
private const val CARD_GAP = 50f
private const val ROW_WIDTH = SEND_WIDTH + CARD_GAP + ADD_WIDTH
private const val CARD_HEIGHT = 700f
private const val NOTE_TOP = 80f
/**
 * The QR: the activation screen's own size on the reference phone, 138dp, so a phone
 * camera reads it at the distance it always has (owner, 2026-11-26) -- a little over the
 * drawing's 320, which `ActivationQrTest` holds to three dp a module on every frame.
 */
private const val PLATE = 380f

/** Cards from the header, the cards, the note: 40 + 700 + 80 + 38. */
internal const val CONTENT = 858f

private const val LEADING = 1.35f

/** The plate's white margin round the symbol, as the activation screen has always drawn it. */
internal const val QUIET_ZONE = 0.062f

/**
 * The first screen, where a source is set up: "Set up a viewing source".
 *
 * ## The drawing (owner, 2026-11-26)
 *
 *  - **Home's header band**, with the screen's name in its middle: the lockup,
 *    Refresh, Language, Exit, a rule and the clock.
 *  - **Send it from your phone**, on the one side by itself: the QR — the portal's
 *    send page, castivio.app/connect — three numbered steps and the address.
 *  - **On the other side**, with no frame round it: the MAC address and the device key,
 *    each with its copy button, because they are what the send page asks for; then
 *    "Add your playlist", its two kinds — M3U and Xtream, each opening its own form —
 *    and "Add a new source", which opens the full choice.
 *  - The player-only line at the foot.
 *
 * Every size is the drawing's own, through one unit — see [Metrics].
 */
@Composable
internal fun MacActivationScreen(
    identity: ActivationIdentityState,
    onAddPlaylist: () -> Unit,
    onPlaylistUrl: () -> Unit,
    onXtream: () -> Unit,
    onRefresh: () -> Unit,
    onCopied: (Copied) -> Unit,
    onOpenLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tv = CastivioTheme.device.isTv

    BoxWithConstraints(modifier.fillMaxSize()) {
        val m = metricsFor(tv, maxWidth, maxHeight)
        Column(
            Modifier
                .fillMaxSize()
                .testTag(ActivationTags.STAGE)
                .castivioStage(m.frame),
        ) {
            Header(
                m = m,
                refresh = identity.refresh,
                onRefresh = onRefresh,
                onOpenLanguage = onOpenLanguage,
                onExit = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(m.header)
                    .testTag(ActivationTags.HEADER),
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    Modifier
                        .padding(top = m.cardsTop)
                        .fillMaxWidth()
                        .height(m.cardHeight)
                        .testTag(ActivationTags.FIELD),
                    horizontalArrangement = Arrangement.spacedBy(m.cardGap),
                ) {
                    // Start first: in Arabic the identifiers and the choice take the
                    // right, and the phone's card the left, as drawn.
                    AddHere(
                        identity = identity,
                        m = m,
                        onAddPlaylist = onAddPlaylist,
                        onPlaylistUrl = onPlaylistUrl,
                        onXtream = onXtream,
                        onCopied = onCopied,
                        modifier = Modifier
                            .weight(ADD_WIDTH)
                            .fillMaxHeight(),
                    )
                    SendCard(
                        identity = identity,
                        m = m,
                        modifier = Modifier
                            .weight(SEND_WIDTH)
                            .fillMaxHeight(),
                    )
                }
                Note(m, Modifier.padding(top = m.noteTop))
            }
        }
    }
}

/* -------------------------------------------------------------------- header */

/**
 * Home's band, with the screen's name in the middle of it. Pinned left to right as
 * Home's is: the lockup at the left edge, the clock at the right, each word still read
 * in its own direction.
 *
 * Refresh asks the portal whether something was sent from a phone, and says on itself
 * how that went: a ring while it asks, the success green when a source arrived, the
 * failure red when the portal could not be reached.
 */
@Composable
private fun Header(
    m: Metrics,
    refresh: RefreshState,
    onRefresh: () -> Unit,
    onOpenLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame = m.frame
    val colors = CastivioTheme.colors
    val reading = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(frame.chipPad),
            ) {
                CastivioLockup(markSize = frame.brand, wordSize = (frame.fsTitle.value * LOCKUP_WORD_RATIO).sp)
                Spacer(Modifier.weight(1f))
                CastivioHeaderRule(frame)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 4),
                ) {
                    CastivioHeaderAction(
                        icon = when (refresh) {
                            RefreshState.Found -> Icons.Rounded.CheckCircle
                            RefreshState.Error -> Icons.Rounded.ErrorOutline
                            else -> Icons.Rounded.Refresh
                        },
                        label = stringResource(R.string.refresh),
                        labelled = true,
                        frame = frame,
                        onClick = onRefresh,
                        busy = refresh == RefreshState.Checking,
                        hue = when (refresh) {
                            RefreshState.Found -> colors.success
                            RefreshState.Error -> colors.danger
                            else -> null
                        },
                        spoken = stringResource(
                            when (refresh) {
                                RefreshState.Found -> R.string.refresh_found
                                RefreshState.Error -> R.string.refresh_error
                                RefreshState.None -> R.string.refresh_none
                                RefreshState.Checking -> R.string.refresh_checking
                                RefreshState.Idle -> R.string.refresh
                            },
                        ),
                        modifier = Modifier.testTag(ActivationTags.ACT_REFRESH),
                    )
                    CastivioHeaderAction(
                        icon = Icons.Rounded.Language,
                        label = stringResource(R.string.language),
                        labelled = true,
                        frame = frame,
                        onClick = onOpenLanguage,
                        modifier = Modifier.testTag(ActivationTags.ACT_LANGUAGE),
                    )
                    CastivioHeaderAction(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        label = stringResource(R.string.activation_exit),
                        labelled = true,
                        frame = frame,
                        onClick = onExit,
                        modifier = Modifier.testTag(ActivationTags.ACT_EXIT),
                    )
                }
                CastivioHeaderRule(frame)
                CastivioClock(fsTime = frame.fsTitle, fsDate = frame.fsChip)
            }

            // The name, in the middle of the band whatever the two ends measure.
            CompositionLocalProvider(LocalLayoutDirection provides reading) {
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(m.unit * TITLE_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = colors.hueViolet,
                        modifier = Modifier.size(m.titleIcon),
                    )
                    Text(
                        text = stringResource(R.string.setup_title),
                        style = text(CastivioType.headlineMedium, m.titleSize, FontWeight.Bold),
                        color = colors.onBackgroundStrong,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private const val LOCKUP_WORD_RATIO = 0.75f
private const val TITLE_GAP = 22f

/* ------------------------------------------------------------------ add here */

/**
 * The identifiers, then adding a source on this device — with no frame round it, so
 * the two kinds and the button are the surface's own (owner, 2026-11-26).
 */
@Composable
private fun AddHere(
    identity: ActivationIdentityState,
    m: Metrics,
    onAddPlaylist: () -> Unit,
    onPlaylistUrl: () -> Unit,
    onXtream: () -> Unit,
    onCopied: (Copied) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Identifiers(identity, m, onCopied)

        if (identity.provenance == IdentityProvenance.INSTALLATION) {
            Text(
                text = stringResource(R.string.mac_installation_note),
                style = text(CastivioType.bodySmall, m.kindSubSize, FontWeight.Normal),
                color = colors.onBackgroundMuted,
                maxLines = 1,
                modifier = Modifier.testTag(ActivationTags.MAC_INSTALLATION_NOTE),
            )
        }

        Row(
            Modifier.padding(top = m.headingTop),
            horizontalArrangement = Arrangement.spacedBy(m.unit * TITLE_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Layers,
                contentDescription = null,
                tint = colors.hueViolet,
                modifier = Modifier.size(m.headingIcon),
            )
            Text(
                text = stringResource(R.string.add_heading),
                style = text(CastivioType.headlineMedium, m.headingSize, FontWeight.Bold),
                color = colors.onBackgroundStrong,
                maxLines = 1,
            )
        }
        CastivioFittedText(
            text = stringResource(R.string.add_sub),
            style = text(CastivioType.bodyLarge, m.subSize, FontWeight.Normal),
            color = colors.onBackgroundVariant,
            textAlign = TextAlign.Center,
        )

        Row(
            Modifier
                .padding(top = m.kindsTop)
                .fillMaxWidth()
                .height(m.kindHeight),
            horizontalArrangement = Arrangement.spacedBy(m.unit * KIND_GAP),
        ) {
            Kind(
                name = "M3U",
                sub = stringResource(R.string.kind_m3u_sub),
                icon = Icons.Rounded.SlowMotionVideo,
                m = m,
                onClick = onPlaylistUrl,
                modifier = Modifier
                    .weight(1f)
                    .testTag(ActivationTags.ACT_M3U),
            )
            Kind(
                name = "Xtream",
                sub = stringResource(R.string.kind_xtream_sub),
                icon = Icons.Rounded.Dns,
                m = m,
                onClick = onXtream,
                modifier = Modifier
                    .weight(1f)
                    .testTag(ActivationTags.ACT_XTREAM),
            )
        }

        CastivioButton(
            text = stringResource(R.string.add_new_source),
            weight = ButtonWeight.Primary,
            icon = Icons.Rounded.Add,
            onClick = onAddPlaylist,
            modifier = Modifier
                .padding(top = m.ctaTop)
                .fillMaxWidth(CTA_SHARE)
                .testTag(ActivationTags.ACTIONS),
            fill = colors.primaryBrush,
            corner = m.radius,
            labelStyle = text(CastivioType.titleLarge, m.ctaSize, FontWeight.Bold),
            minHeight = m.ctaHeight,
        )
    }
}

/** The button against the column: narrower than the two kinds above it, as drawn. */
private const val CTA_SHARE = 0.7f
private const val KIND_GAP = 30f

/**
 * The MAC address and the device key, each in one line with no frame, read left to right
 * in every language (owner, 2026-11-27): the mark, the name, the value, and the copy
 * button at the right-hand end; the MAC first. The marks are the first screen's own --
 * the chip in the muted ink, the key in amber. Selectable as well, so a long press
 * copies on a phone without the button.
 */
@Composable
private fun Identifiers(identity: ActivationIdentityState, m: Metrics, onCopied: (Copied) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val address = identity.address
    val colors = CastivioTheme.colors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(m.idsHeight)
                .testTag(ActivationTags.IDENTITY),
            horizontalArrangement = Arrangement.spacedBy(m.idsGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Identifier(
                label = MAC_LABEL,
                value = address ?: ADDRESS_PLACEHOLDER,
                valueTint = colors.onBackgroundStrong,
                icon = Icons.Rounded.Memory,
                iconTint = colors.onBackgroundMuted,
                copyLabel = stringResource(R.string.copy_mac),
                spoken = address?.let { stringResource(R.string.mac_spoken, spaced(it)) },
                isCopied = identity.addressCopied,
                enabled = address != null,
                m = m,
                onCopy = {
                    clipboard.setText(AnnotatedString(address.orEmpty()))
                    onCopied(Copied.Address)
                },
                modifier = Modifier
                    .weight(MAC_SHARE)
                    .testTag(ActivationTags.MAC_CAPSULE),
            )
            identity.deviceKey?.let { key ->
                Identifier(
                    label = KEY_LABEL,
                    value = key,
                    valueTint = colors.hueAmber,
                    icon = Icons.Rounded.VpnKey,
                    iconTint = colors.hueAmber,
                    copyLabel = stringResource(R.string.copy_key),
                    spoken = null,
                    isCopied = identity.keyCopied,
                    enabled = true,
                    m = m,
                    onCopy = {
                        clipboard.setText(AnnotatedString(key))
                        onCopied(Copied.Key)
                    },
                    modifier = Modifier
                        .weight(KEY_SHARE)
                        .testTag(ActivationTags.KEY_CAPSULE),
                )
            }
        }
    }
}

/** The names the portal's form gives the two, the same in every language. */
private const val MAC_LABEL = "Mac"
private const val KEY_LABEL = "Device key"
private const val MAC_SHARE = 660f
private const val KEY_SHARE = 480f

@Composable
private fun Identifier(
    label: String,
    value: String,
    valueTint: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    copyLabel: String,
    spoken: String?,
    isCopied: Boolean,
    enabled: Boolean,
    m: Metrics,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(m.unit * ID_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(m.idIcon))
        SelectionContainer(Modifier.weight(1f)) {
            Row(
                Modifier.then(
                    if (spoken != null) Modifier.semantics(mergeDescendants = true) { contentDescription = spoken } else Modifier,
                ),
                horizontalArrangement = Arrangement.spacedBy(m.unit * ID_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = text(CastivioType.titleMedium, m.idLabelSize, FontWeight.Bold),
                    color = colors.onBackgroundVariant,
                    maxLines = 1,
                )
                CastivioFittedText(
                    text = value,
                    style = text(CastivioType.titleLarge, m.idValueSize, FontWeight.Bold).copy(letterSpacing = 0.04.em),
                    color = valueTint,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        CopyButton(
            size = m.copy,
            label = copyLabel,
            isCopied = isCopied,
            onClick = { if (enabled) onCopy() },
            // No fill (owner, 2026-11-27): the glyph alone, with the quiet edge so a
            // remote can see where the control is; focus still draws its ring.
            ring = colors.edgeQuiet,
            iconTint = colors.onBackgroundStrong,
        )
    }
}

private const val ID_GAP = 24f

/** One kind of source: its name, what it asks for, and its mark in a disc. It opens that kind's form. */
@Composable
private fun Kind(
    name: String,
    sub: String,
    icon: ImageVector,
    m: Metrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(m.radius)
    Row(
        modifier
            .fillMaxHeight()
            .castivioFocusScale(Motion.focusScaleCard, interaction)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(colors.homeCardSurface)
            .background(colors.primaryBrush, alpha = KIND_TINT)
            .border(
                BorderStroke(if (focused) m.unit * 5f else m.unit * 2.5f, if (focused) colors.focusRing else colors.hueViolet.copy(alpha = KIND_EDGE)),
                shape,
            )
            .clickable(interaction, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = m.unit * KIND_PAD),
        horizontalArrangement = Arrangement.spacedBy(m.unit * KIND_PAD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = text(CastivioType.titleLarge, m.kindNameSize, FontWeight.Bold),
                color = colors.onBackgroundStrong,
                maxLines = 1,
            )
            CastivioFittedText(
                text = sub,
                style = text(CastivioType.bodyMedium, m.kindSubSize, FontWeight.Normal),
                color = colors.onBackgroundVariant,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            Modifier
                .size(m.kindDisc)
                .clip(CircleShape)
                .background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(m.kindIcon))
        }
    }
}

private const val KIND_TINT = 0.18f
private const val KIND_EDGE = 0.6f
private const val KIND_PAD = 30f

/* ---------------------------------------------------------------- send card */

/**
 * Send it from your phone: the QR on the portal's send page, three steps, and the
 * address for a phone without a camera. A thin violet line round it, no glow.
 */
@Composable
private fun SendCard(identity: ActivationIdentityState, m: Metrics, modifier: Modifier = Modifier) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(m.radius)
    val inner = RoundedCornerShape(m.radius - m.innerInset / 2)
    Column(
        modifier
            .clip(shape)
            .background(colors.homeCardSurface)
            .background(colors.primaryBrush, alpha = SEND_TINT)
            .border(BorderStroke(1.dp, colors.hueViolet.copy(alpha = SEND_EDGE)), shape)
            .testTag(ActivationTags.CODE_ZONE),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(m.sendTitleBand),
            horizontalArrangement = Arrangement.spacedBy(m.unit * TITLE_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.send_title),
                style = text(CastivioType.headlineMedium, m.sendTitleSize, FontWeight.Bold),
                color = colors.onBackgroundStrong,
                maxLines = 1,
            )
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint = colors.onBackgroundStrong,
                modifier = Modifier.size(m.sendIcon),
            )
        }
        Row(
            Modifier
                .padding(start = m.innerInset, end = m.innerInset, bottom = m.innerInset)
                .fillMaxSize()
                .clip(inner)
                .background(colors.surfaceWell)
                .border(BorderStroke(1.dp, colors.edgeQuiet), inner)
                .padding(horizontal = m.innerPad),
            horizontalArrangement = Arrangement.spacedBy(m.innerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(m.stepGap),
            ) {
                Step(1, stringResource(R.string.send_step_scan), m)
                Step(2, stringResource(R.string.send_step_ids), m)
                Step(3, stringResource(R.string.send_step_play), m)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(m.pillHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.hueAzure.copy(alpha = PILL_WASH))
                        .border(BorderStroke(1.dp, colors.hueAzure.copy(alpha = PILL_EDGE)), RoundedCornerShape(percent = 50))
                        .testTag(ActivationTags.ADDRESS),
                    horizontalArrangement = Arrangement.spacedBy(m.unit * PILL_GAP, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(m.unit * PILL_GAP),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.hueAzure, modifier = Modifier.size(m.pillIcon))
                            Text(
                                text = ActivationDestination.CONNECT_DISPLAY,
                                style = text(CastivioType.titleMedium, m.pillSize, FontWeight.Bold),
                                color = colors.hueAzure,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxHeight(DIVIDER_SHARE)
                    .width(1.dp)
                    .background(colors.edgeQuiet),
            )
            val bitmap = remember(identity.qr) { identity.qr?.asImageBitmap() }
            if (bitmap != null) {
                QrPlate(
                    code = bitmap,
                    plate = m.plate,
                    padding = m.plate * QUIET_ZONE,
                    modifier = Modifier.testTag(ActivationTags.QR),
                )
            } else {
                Spacer(Modifier.size(m.plate))
            }
        }
    }
}

private const val SEND_TINT = 0.22f
private const val SEND_EDGE = 0.7f
private const val PILL_WASH = 0.12f
private const val PILL_EDGE = 0.5f
private const val PILL_GAP = 16f
private const val DIVIDER_SHARE = 0.75f

@Composable
private fun Step(number: Int, words: String, m: Metrics) {
    val colors = CastivioTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(m.unit * STEP_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(m.stepBadge)
                .clip(RoundedCornerShape(m.unit * STEP_RADIUS))
                .background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = text(CastivioType.labelLarge, m.stepNumber, FontWeight.Bold, 1f),
                color = colors.onPrimary,
                maxLines = 1,
            )
        }
        CastivioFittedText(
            text = words,
            style = text(CastivioType.bodyLarge, m.stepSize, FontWeight.Normal),
            color = colors.onBackground,
        )
    }
}

private const val STEP_GAP = 18f
private const val STEP_RADIUS = 14f

/* -------------------------------------------------------------------- footer */

/** The player-only line, between two rules, at the foot. */
@Composable
private fun Note(m: Metrics, modifier: Modifier = Modifier) {
    val colors = CastivioTheme.colors
    Row(
        modifier.testTag(ActivationTags.FOOTER),
        horizontalArrangement = Arrangement.spacedBy(m.unit * NOTE_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(m.noteRule).height(1.dp).background(colors.edgeQuiet))
        CastivioFittedText(
            text = stringResource(R.string.legal_player_only),
            style = text(CastivioType.bodyMedium, m.noteSize, FontWeight.Normal),
            color = colors.onBackgroundVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(Modifier.width(m.noteRule).height(1.dp).background(colors.edgeQuiet))
    }
}

private const val NOTE_GAP = 30f

/** A step of the type scale at the drawing's size, weight and leading. */
private fun text(base: TextStyle, size: Dp, weight: FontWeight, leading: Float = LEADING): TextStyle =
    base.copy(fontSize = size.value.sp, lineHeight = (size.value * leading).sp, fontWeight = weight)

/** `2F:19:EB:20:44:7C` read as separated pairs rather than one long number. */
private fun spaced(address: String): String = address.replace(":", " ")

/** Shown for the instant before the identity resolves. Never a real address. */
private const val ADDRESS_PLACEHOLDER = "··:··:··:··:··:··"
