package com.castivio.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Elevation
import com.castivio.core.design.theme.Motion
import com.castivio.core.design.theme.MotionLevel

/**
 * The numbers a plan card needs, which differ per frame.
 *
 * Told, not inferred: the component has no opinion about televisions, and the caller
 * owns its own per-frame table. Same arrangement as [CapsuleMetrics], for the same
 * reason.
 */
data class PlanCardMetrics(
    /** The floor. Cards side by side are drawn the same height by their row. */
    val minHeight: Dp,
    val paddingH: Dp,
    val paddingTop: Dp,
    val paddingBottom: Dp,
    val radius: Dp,
    /** The resting edge; focus draws it heavier. */
    val stroke: Dp,
    val nameStyle: TextStyle,
    val priceStyle: TextStyle,
    val periodStyle: TextStyle,
    val featureStyle: TextStyle,
    val featureGap: Dp,
    val checkGap: Dp,
    val tagStyle: TextStyle,
    val tagPaddingH: Dp,
    val tagPaddingV: Dp,
    /** How far in from the card's start edge the tag sits. */
    val tagInset: Dp,
    /** How far the tag rises above the card's top edge: it sits across the line. */
    val tagRise: Dp,
)

/**
 * A plan, and pressing it buys that plan.
 *
 * ## The card is the button
 *
 * There is no select-then-continue: no radio, no selected state, no Continue. On a
 * television that removes an entire interaction, and it keeps *focused ≠ selected*
 * trivially true, because nothing is ever selected. The states are resting, focused
 * and working.
 *
 * ## The drawing (owner, 2026-11-26)
 *
 * The name, the price large under it, the period, and the plan's promises at the
 * foot, each after a green check. One plan may wear a tag across its top edge —
 * "Best value" — and the brand's violet: [highlight]. That is the portal's own
 * ranking, shown the same way in the app, and it is a matter of fill and edge only:
 * focus still draws the same ring on either card, so "the one we suggest" and "the
 * one the remote is on" stay two different things.
 *
 * @param description the whole card as one sentence — name, price and period —
 *   because a screen reader walking disconnected nodes reads a price with nothing
 *   attached to it. The card is a single focusable node.
 * @param highlight the tag's words, or null for a card without one.
 * @param working true while this card's portal is being opened. Dimmed and
 *   unfocusable, so a second press cannot start a second handoff.
 */
@Composable
fun PlanCard(
    metrics: PlanCardMetrics,
    name: String,
    price: String,
    period: String,
    features: List<String>,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: String? = null,
    working: Boolean = false,
) {
    val colors = CastivioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(metrics.radius)
    val lit = highlight != null

    // One spec for every property, so the card arrives as a single thing: 200ms, in
    // the middle of the 180-220ms the design asks for, and a snap with motion off.
    val level = CastivioTheme.motionLevel
    val spec: FiniteAnimationSpec<Float> = remember(level) { focusSpec(level) }
    val colourSpec: FiniteAnimationSpec<Color> = remember(level) { focusSpec(level) }
    val sizeSpec: FiniteAnimationSpec<Dp> = remember(level) { focusSpec(level) }

    val edge by animateColorAsState(
        when {
            focused -> colors.focusRing
            lit -> colors.hueViolet
            else -> colors.cardSurfaceEdge
        },
        colourSpec, label = "planEdge",
    )
    val stroke by animateDpAsState(
        if (focused) maxOf(metrics.stroke * 2, FOCUSED_STROKE) else maxOf(metrics.stroke, RESTING_STROKE),
        sizeSpec, label = "planStroke",
    )
    // The glow: the lit card always casts a little, a focused one more.
    val lift by animateDpAsState(
        when {
            focused -> FOCUSED_LIFT
            lit -> LIT_LIFT
            else -> Elevation.level0
        },
        sizeSpec, label = "planLift",
    )
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        when {
            pressed -> Motion.pressScale
            focused -> FOCUSED_SCALE
            else -> 1f
        },
        spec, label = "planScale",
    )

    Box(
        modifier
            .alpha(if (working) WORKING_ALPHA else 1f)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = metrics.minHeight)
                .focusProperties { canFocus = !working }
                .onFocusChanged { focused = it.isFocused || it.hasFocus }
                .shadow(lift, shape, ambientColor = Elevation.ambient, spotColor = colors.selectedGlow)
                .clip(shape)
                .background(colors.homeCardSurface)
                .then(if (lit) Modifier.background(colors.primaryBrush, alpha = LIT_TINT) else Modifier)
                .border(BorderStroke(stroke, edge), shape)
                .clickable(interaction, indication = null, enabled = !working, onClick = onClick)
                .padding(
                    start = metrics.paddingH,
                    end = metrics.paddingH,
                    top = metrics.paddingTop,
                    bottom = metrics.paddingBottom,
                )
                .clearAndSetSemantics { contentDescription = description },
        ) {
            Text(text = name, style = metrics.nameStyle, color = colors.onBackgroundVariant, maxLines = 1)
            // Not monospace: a price is read, not copied.
            Text(text = price, style = metrics.priceStyle, color = colors.onBackgroundStrong, maxLines = 1)
            Text(text = period, style = metrics.periodStyle, color = colors.onBackgroundVariant, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(metrics.featureGap)) {
                for (feature in features) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(metrics.checkGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(metrics.featureStyle.fontSize.value.dp),
                        )
                        Text(text = feature, style = metrics.featureStyle, color = colors.onBackground, maxLines = 2)
                    }
                }
            }
        }

        if (highlight != null) {
            Text(
                text = highlight,
                style = metrics.tagStyle,
                color = colors.onPrimary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = metrics.tagInset, y = -metrics.tagRise)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.primaryBrush)
                    .padding(horizontal = metrics.tagPaddingH, vertical = metrics.tagPaddingV),
            )
        }
    }
}

private fun <T> focusSpec(level: MotionLevel): FiniteAnimationSpec<T> =
    planFocusMillis(level).let { if (it == 0) snap() else tween(it, easing = Motion.standard) }

/** How long focus takes to arrive on a card, at a given level of motion; 0 is a snap. */
internal fun planFocusMillis(level: MotionLevel): Int =
    if (level == MotionLevel.DISABLED) 0 else FOCUS_MS

internal const val FOCUS_MS = 200

/** Slight: the card comes nearer, it does not jump. */
internal const val FOCUSED_SCALE = 1.03f

/** The floors under the frame's own edge, resting and focused. */
internal val RESTING_STROKE = 1.dp
internal val FOCUSED_STROKE = 2.dp

internal val FOCUSED_LIFT = Elevation.level2

/** The lit card's standing glow, under the focused one. */
private val LIT_LIFT = Elevation.level1

/** The brand gradient laid over the lit card's surface: a violet wash, not a fill. */
private const val LIT_TINT = 0.26f

private const val WORKING_ALPHA = 0.5f
