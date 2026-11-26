package com.castivio.feature.licence

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.castivio.core.design.components.PlanCardMetrics
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioType
import com.castivio.core.design.theme.castivioMetrics

/**
 * Every number on the licence screen, for one frame.
 *
 * ## One unit, from the drawing
 *
 * The approved drawing is `design/mockups/licence-expired-header-ar.png`, 2340×1080
 * pixels — a landscape phone — and the screen is that drawing, literally (owner,
 * 2026-11-26). So every size below is the drawing's own pixel count times [unit], one
 * drawing pixel in dp on this frame, and the screen is the drawing scaled rather than
 * re-proportioned:
 *
 *  - by the height, 1080 pixels to the frame's height — the reference phone, 393dp
 *    tall, is the drawing at 2.75 pixels to the dp, which is the phone it was drawn for;
 *  - unless the width is the tighter one: the row of three cards is 2120 pixels and
 *    must fit between the stage's edges (a 16:9 television is narrower than the
 *    drawing for its height);
 *  - unless what is under the header is: [CONTENT] pixels of hero, cards and
 *    identifiers must fit under the header band the frame keeps.
 *
 * The header is not scaled with the rest. It is Home's band, at Home's numbers, so the
 * two screens wear one header.
 */
internal data class LicenceMetrics(
    val frame: CastivioMetrics,
    /** One drawing pixel, in dp, on this frame. */
    val unit: Dp,
    /**
     * The header's own height: the frame's, but never under the touch target, so its
     * buttons are never cut short (48dp on a phone, where the frame says 40).
     */
    val header: Dp,
    /** The header band: [header] and the stage's top margin above it. */
    val headerBand: Dp,

    /* the hero */
    val badgeStyle: TextStyle,
    val badgePadH: Dp,
    val badgePadV: Dp,
    val badgeDot: Dp,
    val badgeGap: Dp,
    val badgeStroke: Dp,
    val titleTop: Dp,
    val titleStyle: TextStyle,
    val subtitleTop: Dp,
    val subtitleStyle: TextStyle,
    /** The subtitle's reserved height: the line keeps it whether or not it has anything to say. */
    val subtitleHeight: Dp,

    /* the cards */
    val cardsTop: Dp,
    val cardHeight: Dp,
    val cardGap: Dp,
    val cardRadius: Dp,
    val cardStroke: Dp,
    val plan: PlanCardMetrics,

    /* the activation card */
    val actPadH: Dp,
    val actPadV: Dp,
    val actGap: Dp,
    val plate: Dp,
    val platePadding: Dp,
    val actTitleStyle: TextStyle,
    val actTitleGap: Dp,
    val stepStyle: TextStyle,
    val stepGap: Dp,
    val stepBadge: Dp,
    val stepBadgeRadius: Dp,
    val stepNumberStyle: TextStyle,
    val stepBadgeGap: Dp,
    val urlTop: Dp,
    val urlStyle: TextStyle,
    val urlLinkStyle: TextStyle,
    val urlGap: Dp,

    /* the status card, in the states that offer no plan */
    val statusIcon: Dp,
    val statusGap: Dp,

    /* the identifiers */
    val idsTop: Dp,
    val idsGap: Dp,
    val idLabelStyle: TextStyle,
    val idValueStyle: TextStyle,
    val idValueGap: Dp,
)

internal fun licenceMetricsFor(tv: Boolean, width: Dp, height: Dp): LicenceMetrics {
    val frame = castivioMetrics(width, height, tv)
    val header = maxOf(frame.header, frame.touchTarget)
    val headerBand = header + frame.stageTop
    val below = height - headerBand - frame.stageBottom
    val unit = minOf(
        height / DRAWING_HEIGHT,
        (width - frame.edge * 2) / ROW_WIDTH,
        below / CONTENT,
    )
    fun px(drawing: Float): Dp = unit * drawing

    return LicenceMetrics(
        frame = frame,
        unit = unit,
        header = header,
        headerBand = headerBand,

        badgeStyle = text(CastivioType.labelLarge, px(28f), FontWeight.SemiBold),
        badgePadH = px(22f),
        badgePadV = px(8f),
        badgeDot = px(14f),
        badgeGap = px(12f),
        badgeStroke = px(2f),
        titleTop = px(22f),
        titleStyle = text(CastivioType.headlineLarge, px(66f), FontWeight.Bold, LEADING_TITLE),
        subtitleTop = px(8f),
        subtitleStyle = text(CastivioType.bodyLarge, px(34f), FontWeight.Normal),
        subtitleHeight = px(34f * LEADING),

        cardsTop = px(44f),
        cardHeight = px(CARD_HEIGHT),
        cardGap = px(CARD_GAP),
        cardRadius = px(30f),
        cardStroke = px(3f),
        plan = PlanCardMetrics(
            minHeight = px(CARD_HEIGHT),
            paddingH = px(44f),
            paddingTop = px(30f),
            paddingBottom = px(32f),
            radius = px(30f),
            stroke = px(3f),
            nameStyle = text(CastivioType.titleLarge, px(36f), FontWeight.SemiBold),
            priceStyle = text(CastivioType.displayLarge, px(96f), FontWeight.Bold, LEADING_PRICE),
            periodStyle = text(CastivioType.bodyLarge, px(32f), FontWeight.Medium),
            featureStyle = text(CastivioType.bodyMedium, px(28f), FontWeight.Normal),
            featureGap = px(6f),
            checkGap = px(14f),
            tagStyle = text(CastivioType.labelLarge, px(26f), FontWeight.SemiBold),
            tagPaddingH = px(20f),
            tagPaddingV = px(6f),
            tagInset = px(40f),
            tagRise = px(24f),
        ),

        actPadH = px(44f),
        actPadV = px(36f),
        actGap = px(40f),
        plate = px(372f),
        platePadding = px(23f),
        actTitleStyle = text(CastivioType.titleLarge, px(40f), FontWeight.SemiBold),
        actTitleGap = px(22f),
        stepStyle = text(CastivioType.bodyLarge, px(30f), FontWeight.Normal),
        stepGap = px(18f),
        stepBadge = px(48f),
        stepBadgeRadius = px(14f),
        stepNumberStyle = text(CastivioType.labelLarge, px(26f), FontWeight.Bold, 1f),
        stepBadgeGap = px(18f),
        urlTop = px(26f),
        urlStyle = text(CastivioType.bodyMedium, px(28f), FontWeight.Normal),
        urlLinkStyle = text(CastivioType.titleMedium, px(32f), FontWeight.Bold).copy(letterSpacing = 0.02.em),
        urlGap = px(14f),

        statusIcon = px(96f),
        statusGap = px(24f),

        idsTop = px(55f),
        idsGap = px(60f),
        idLabelStyle = text(CastivioType.bodyMedium, px(28f), FontWeight.Medium),
        idValueStyle = text(CastivioType.titleMedium, px(30f), FontWeight.SemiBold).copy(letterSpacing = 0.06.em),
        idValueGap = px(12f),
    )
}

/** A step of the type scale at the drawing's size, with the drawing's weight and leading. */
private fun text(base: TextStyle, size: Dp, weight: FontWeight, leading: Float = LEADING): TextStyle =
    base.copy(
        fontSize = size.value.sp,
        lineHeight = (size.value * leading).sp,
        fontWeight = weight,
    )

/**
 * What the content under the header needs, in drawing pixels, from the badge to the
 * identifiers: the hero, the gap, the cards, the gap, the line of identifiers.
 *
 * The same sum [contentHeight] makes from the frame's own numbers; `LicenceBudgetTest`
 * holds the two together.
 */
internal fun LicenceMetrics.contentHeight(): Dp =
    badgeStyle.lineHeight.value.dp + badgePadV * 2 + badgeStroke * 2 +
        titleTop + titleStyle.lineHeight.value.dp +
        subtitleTop + subtitleHeight +
        cardsTop + cardHeight +
        idsTop + idValueStyle.lineHeight.value.dp

/** The width the row of three cards needs: two plans, the activation card, two gaps. */
internal fun LicenceMetrics.rowWidth(): Dp = unit * ROW_WIDTH

/** The drawing's height, in its own pixels. */
private const val DRAWING_HEIGHT = 1080f

/** A plan card and the activation card, as drawn. */
internal const val PLAN_WIDTH = 560f
internal const val ACT_WIDTH = 900f
private const val CARD_HEIGHT = 440f
private const val CARD_GAP = 50f

/** Two plans, the activation card and the two gaps between them. */
private const val ROW_WIDTH = PLAN_WIDTH * 2 + ACT_WIDTH + CARD_GAP * 2

/**
 * Hero, cards and identifiers, as drawn: badge 58 (its line, padding and edge), 22,
 * title 86, 8, subtitle 46, 44, cards 440, 55, identifiers 41.
 */
internal const val CONTENT = 800f

/** Line height against size: Arabic's marks need the room, and Latin does not mind it. */
private const val LEADING = 1.35f
private const val LEADING_TITLE = 1.3f
private const val LEADING_PRICE = 1.15f
