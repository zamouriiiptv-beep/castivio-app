package com.castivio.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Motion

/**
 * One button of the header band: a glyph with its word under it.
 *
 * It was Home's own until the licence screen took the same band (owner, 2026-11-26):
 * the logo, the buttons with their words, a rule, the clock. Two screens wearing one
 * band is one component, so it lives here and both call it.
 *
 * @param labelled whether the word is drawn under the glyph. Every caller passes true
 *   today — a glyph alone was not read (owner, 2026-11-25) — and the flag stays so a
 *   cramped band can drop the words without a second component.
 * @param hue a tone for the glyph and the word together, for an answer (done, failed).
 * @param spoken what a screen reader says, when the drawn word is not the whole of it.
 * @param busy a ring in place of the glyph; the press does nothing while it turns.
 */
@Composable
fun CastivioHeaderAction(
    icon: ImageVector,
    label: String,
    labelled: Boolean,
    frame: CastivioMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hue: Color? = null,
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

/** The thin upright rule that sets the buttons apart from the logo and the clock. */
@Composable
fun CastivioHeaderRule(frame: CastivioMetrics) {
    Box(
        Modifier
            .fillMaxHeight(HEADER_RULE_HEIGHT)
            .width(1.dp)
            .background(CastivioTheme.colors.edgeQuiet),
    )
    Spacer(Modifier.width(frame.chipPad / 4))
}

/** The rule against the band's height: a divider, not a wall. */
private const val HEADER_RULE_HEIGHT = 0.46f

/** The glyph against the touch target, which is what keeps it the same on every frame. */
private const val GLYPH_OF_TARGET = 0.42f

/** The word under the glyph, a step under the chip type. */
private const val LABEL_OF_CHIP = 0.78f

private val ACTION_PAD_V = 4.dp
private val ACTION_GAP = 2.dp
private val BUSY_RING = 2.dp
