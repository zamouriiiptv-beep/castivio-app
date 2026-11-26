package com.castivio.feature.licence

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.castivio.core.common.config.ActivationDestination
import com.castivio.core.common.format.CastivioDates
import com.castivio.core.design.components.ButtonWeight
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioFittedText
import com.castivio.core.design.components.CastivioHeaderAction
import com.castivio.core.design.components.CastivioHeaderRule
import com.castivio.core.design.components.CastivioLockup
import com.castivio.core.design.components.PlanCard
import com.castivio.core.design.components.QrPlate
import com.castivio.core.design.components.Skeleton
import com.castivio.core.design.components.ltrIsolate
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Motion
import com.castivio.core.design.theme.MotionLevel
import com.castivio.core.design.theme.castivioStage
import com.castivio.domain.entitlement.EntitlementState
import com.castivio.domain.entitlement.Plan
import com.castivio.domain.entitlement.PlanOffer
import java.util.Locale

/**
 * The licence screen: what this device is entitled to, and how to change it.
 *
 * ## The drawing is the screen
 *
 * `design/mockups/licence-expired-header-ar.png`, applied literally (owner,
 * 2026-11-26), top to bottom:
 *
 *  - **Home's header band** — the lockup, Refresh, Support, Language, Exit, a rule
 *    and the clock — so the screen a lapsed device lands on wears the same band as
 *    the one it was turned away from.
 *  - **The hero**: the condition in a toned badge, one large line, and one line
 *    under it that says what to do or what is happening.
 *  - **Three cards**: the two plans — the lifetime one lit and tagged "Best value",
 *    as on the portal — and the activation card, the QR at the activation screen's
 *    own size with three numbered steps and the portal's address.
 *  - **The identifiers**, small, in one line under the cards.
 *
 * Every size is the drawing's own, through one unit — see [LicenceMetrics].
 *
 * ## The plan card is the button
 *
 * Pressing a card opens the portal for that plan. No radio, no Continue. Castivio
 * never takes the money: the portal owns payment, licence creation and MAC binding,
 * and this screen's part in a purchase is choosing which link to open. It then asks
 * the licence server every fifteen seconds on its own (see `LicenceRoute`), so the
 * third step it promises — the device activates by itself — is true.
 */
@Composable
internal fun LicenceScreen(
    state: LicenceUiState,
    onPlan: (PlanOffer) -> Unit,
    onRetry: () -> Unit,
    onSupport: () -> Unit,
    onOpenLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tv = CastivioTheme.device.isTv

    BoxWithConstraints(modifier.fillMaxSize()) {
        val m = licenceMetricsFor(tv, maxWidth, maxHeight)
        Column(
            Modifier
                .fillMaxSize()
                .testTag(LicenceTags.STAGE)
                .castivioStage(m.frame),
        ) {
            Header(
                m = m,
                state = state,
                onRefresh = onRetry,
                onSupport = onSupport,
                onOpenLanguage = onOpenLanguage,
                onExit = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(m.header)
                    .testTag(LicenceTags.HEADER),
            )

            // What is under the header is drawn at the drawing's size and centred in
            // the room the band leaves; `licenceMetricsFor` chose the unit so that it
            // fits, and `LicenceBudgetTest` holds it to that on every frame.
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Hero(state = state, m = m)
                Cards(
                    state = state,
                    m = m,
                    onPlan = onPlan,
                    onRetry = onRetry,
                    onSupport = onSupport,
                    modifier = Modifier.padding(top = m.cardsTop),
                )
                Identifiers(state = state, m = m, modifier = Modifier.padding(top = m.idsTop))
            }
        }
    }
}

/**
 * Home's band: the lockup at the left edge, the buttons with their words, a rule, the
 * clock at the right edge, in that order in every language (the band is pinned left to
 * right, as Home's is; each word still reads in its own direction).
 *
 * Refresh is the header's, as on Home: it asks the licence server now, and turns while
 * it asks. Exit is the caller's leaving — out of the application from the gate, back to
 * where the user came from when the screen was opened over Home.
 */
@Composable
private fun Header(
    m: LicenceMetrics,
    state: LicenceUiState,
    onRefresh: () -> Unit,
    onSupport: () -> Unit,
    onOpenLanguage: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame = m.frame
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier,
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
                    icon = Icons.Rounded.Refresh,
                    label = stringResource(R.string.licence_refresh),
                    labelled = true,
                    frame = frame,
                    onClick = onRefresh,
                    busy = state.refreshing,
                    modifier = Modifier.testTag(LicenceTags.REFRESH),
                )
                CastivioHeaderAction(
                    icon = Icons.Rounded.HeadsetMic,
                    label = stringResource(R.string.licence_support_short),
                    labelled = true,
                    frame = frame,
                    onClick = onSupport,
                    spoken = stringResource(R.string.licence_support),
                    modifier = Modifier.testTag(LicenceTags.SUPPORT),
                )
                CastivioHeaderAction(
                    icon = Icons.Rounded.Language,
                    label = stringResource(R.string.licence_language),
                    labelled = true,
                    frame = frame,
                    onClick = onOpenLanguage,
                    modifier = Modifier.testTag(LicenceTags.LANGUAGE),
                )
                CastivioHeaderAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    label = stringResource(R.string.licence_exit),
                    labelled = true,
                    frame = frame,
                    onClick = onExit,
                    modifier = Modifier.testTag(LicenceTags.EXIT),
                )
            }
            CastivioHeaderRule(frame)
            CastivioClock(fsTime = frame.fsTitle, fsDate = frame.fsChip)
        }
    }
}

/** The wordmark against the frame's title size: Home's figure. */
private const val LOCKUP_WORD_RATIO = 0.75f

/**
 * The badge, the large line, and the line under it.
 *
 * ## What each says
 *
 *  - **The badge** names the condition, in its tone: "Trial ended" in amber. Absent
 *    for the instant the entitlement is still being read — an instant with no badge is
 *    honest, an instant of the wrong one is not. Its height is kept either way.
 *  - **The large line** is "Keep watching on this device" wherever a plan is offered,
 *    and the condition's own sentence where none is ("Your Castivio licence is
 *    active").
 *  - **The line under it** is reserved — it keeps its height whether or not it has
 *    anything to say, so nothing below moves at the instant a card is pressed. It
 *    says, first to last: that the portal did not open; that it is opening; how many
 *    days a trial has left; the day an annual licence lapsed; otherwise, where plans
 *    are offered, "Choose a licence and activate it in under a minute".
 *
 * ## The fade
 *
 * A change of condition — a payment landing, the screen turning active — crossfades in
 * 220ms. A transient line (opening, failed) does not: it answers a press, and a fade
 * between a press and its answer is a delay. Keyed on [fadeKey], which carries the
 * entitlement and nothing transient.
 */
@Composable
private fun Hero(state: LicenceUiState, m: LicenceMetrics) {
    val colors = CastivioTheme.colors
    LicenceFade(fadeKey(state)) { condition ->
        val view = condition?.view
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(LicenceTags.HERO),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.height(m.badgeStyle.lineHeight.value.dp + m.badgePadV * 2 + m.badgeStroke * 2),
                contentAlignment = Alignment.Center,
            ) {
                if (view != null) Badge(view, m)
            }

            CastivioFittedText(
                text = heroTitle(view),
                style = m.titleStyle,
                color = colors.onBackgroundStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = m.titleTop),
            )

            val line = subtitle(state, view)
            Box(
                Modifier
                    .padding(top = m.subtitleTop)
                    .height(m.subtitleHeight)
                    .fillMaxWidth()
                    .testTag(LicenceTags.STATUS),
                contentAlignment = Alignment.Center,
            ) {
                if (line != null) {
                    CastivioFittedText(
                        text = line.first,
                        style = m.subtitleStyle,
                        color = line.second,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** The condition, in a pill of its own tone: a dot, the words, a wash and an edge. */
@Composable
private fun Badge(view: LicenceView, m: LicenceMetrics) {
    val tone = badgeTone(view.tone)
    val shape = RoundedCornerShape(percent = 50)
    Row(
        Modifier
            .testTag(LicenceTags.BADGE)
            .clip(shape)
            .background(tone.copy(alpha = BADGE_WASH))
            .border(BorderStroke(m.badgeStroke, tone.copy(alpha = BADGE_EDGE)), shape)
            .padding(horizontal = m.badgePadH, vertical = m.badgePadV),
        horizontalArrangement = Arrangement.spacedBy(m.badgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(m.badgeDot)
                .clip(RoundedCornerShape(percent = 50))
                .background(tone),
        )
        Text(text = stringResource(view.chip), style = m.badgeStyle, color = tone, maxLines = 1)
    }
}

/** The drawing's badge: the tone at 14% under the words and at 45% round them. */
private const val BADGE_WASH = 0.14f
private const val BADGE_EDGE = 0.45f

@Composable
private fun badgeTone(tone: LicenceTone): Color = with(CastivioTheme.colors) {
    when (tone) {
        // A running trial is neither good news nor bad: the brand's violet.
        LicenceTone.Neutral -> hueViolet
        LicenceTone.Good -> success
        // Expiry lives here, not in danger. A trial that ended is not a fault --
        // nothing broke, there is a decision to make -- and red would say otherwise.
        LicenceTone.Warning -> warning
        LicenceTone.Bad -> danger
    }
}

@Composable
private fun heroTitle(view: LicenceView?): String = when {
    view == null || view.offersPlans -> stringResource(R.string.licence_hero_title)
    else -> view.status?.let { statusSentence(view, it) } ?: stringResource(R.string.licence_hero_title)
}

/** The reserved line's words and ink, or null when it has nothing to say. See [Hero]. */
@Composable
private fun subtitle(state: LicenceUiState, view: LicenceView?): Pair<String, Color>? {
    val colors = CastivioTheme.colors
    return when {
        // A handoff that did not start. Said plainly rather than left as a card
        // that stopped looking busy for no visible reason.
        state.failed -> stringResource(R.string.licence_status_failed) to colors.danger
        state.handingOff -> stringResource(R.string.licence_status_opening) to colors.onBackgroundVariant
        view == null || !view.offersPlans -> null
        // The trial's line is a plural, composed here where the day count is in hand.
        view.countsDays -> trialCountdown(state.licence) to colors.onBackgroundVariant
        // A lapsed annual licence says so, with the day it lapsed where that is known.
        state.licence is EntitlementState.AnnualExpired ->
            view.status?.let { statusSentence(view, it) to colors.onBackgroundVariant }
        else -> stringResource(R.string.licence_hero_choose) to colors.onBackgroundVariant
    }
}

/**
 * Fade one condition into another, at the level of motion the device allows.
 *
 * `Crossfade` composes the incoming content immediately and only animates its alpha,
 * so the new state is hittable and focusable from the first frame. With motion off the
 * swap is instant — a user who turned animation off has said what they want.
 */
@Composable
private fun <T> LicenceFade(target: T, content: @Composable (T) -> Unit) {
    val millis = stateFadeMillis(CastivioTheme.motionLevel)
    if (millis == 0) {
        content(target)
        return
    }
    Crossfade(
        targetState = target,
        animationSpec = tween(millis, easing = Motion.standard),
        label = "licenceState",
        content = content,
    )
}

/**
 * How long the condition takes to cross over, at a given level of motion; 0 is a swap.
 *
 * A function rather than a constant read at the call site, because it is the part of
 * this that can be checked without a device — `LicenceMotionTest`.
 */
internal fun stateFadeMillis(level: MotionLevel): Int =
    if (level == MotionLevel.DISABLED) 0 else STATE_FADE_MS

/** Long enough to be seen, short enough not to be waited on: inside the design's 200-250ms. */
internal const val STATE_FADE_MS = 220

/**
 * What the crossfade watches: the entitlement, and nothing transient.
 *
 * A licence turning active is a change of condition and worth marking; "opening the
 * portal" answers a press and must appear at once.
 */
internal fun fadeKey(state: LicenceUiState): LicenceCondition? = state.licence?.let { licence ->
    LicenceCondition(
        view = licenceView(licence),
        // An *active* annual licence, and only that one: it is the one state that
        // draws a "valid until" line. A lapsed licence says its date inside its own
        // sentence, which `LicenceView.statusDateMs` already carries.
        expiresAtMs = (licence as? EntitlementState.AnnualActive)?.expiresAtMs,
    )
}

/**
 * The condition, and the one fact about it that is not in [LicenceView].
 *
 * The expiry travels with the view so the *outgoing* half of a fade keeps the date it
 * was drawn with, rather than blanking a fifth of a second early.
 */
internal data class LicenceCondition(val view: LicenceView, val expiresAtMs: Long?)

/**
 * The row: the plan pair (or what stands in its place) and the activation card, at the
 * drawing's proportions, all one height.
 *
 * In an Arabic interface the annual plan takes the right-hand end and the activation
 * card the left, as drawn; in a left-to-right language the row mirrors, which is the
 * order a reader of that language expects.
 */
@Composable
private fun Cards(
    state: LicenceUiState,
    m: LicenceMetrics,
    onPlan: (PlanOffer) -> Unit,
    onRetry: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = m.cardHeight)
            .testTag(LicenceTags.FIELD),
        horizontalArrangement = Arrangement.spacedBy(m.cardGap),
    ) {
        val pair = Modifier
            .weight(PLAN_WIDTH * 2 + m.cardGap / m.unit)
            .fillMaxHeight()
        val licence = state.licence
        val view = licence?.let(::licenceView)
        when {
            licence == null -> Row(
                pair.testTag(LicenceTags.PLANS),
                horizontalArrangement = Arrangement.spacedBy(m.cardGap),
            ) {
                // The shape the content will take, so nothing jumps when it arrives.
                repeat(SKELETON_CARDS) {
                    Skeleton(
                        modifier = Modifier.weight(1f),
                        height = m.cardHeight,
                        cornerRadius = m.cardRadius,
                    )
                }
            }
            view != null && view.offersPlans -> Plans(state, m, onPlan, pair)
            view != null -> StatusCard(state, view, m, onRetry, onSupport, pair)
        }
        ActivationCard(
            state = state,
            m = m,
            modifier = Modifier
                .weight(ACT_WIDTH)
                .fillMaxHeight(),
        )
    }
}

/** Two, because two plans are offered and the skeleton is their shape. */
private const val SKELETON_CARDS = 2

/**
 * The plans on offer, each a card that opens the portal for itself.
 *
 * `purchasable` is a list, so three plans is expressible in the domain today. This row
 * was drawn for two, and `LicenceLayoutTest` asserts the rendered count equals the
 * offered count, so a third arrives as a deliberate change rather than an overrun.
 *
 * The remote starts on the lit card — the one the portal ranks first — so the first
 * press of a D-pad is spent deciding, not travelling.
 */
@Composable
private fun Plans(
    state: LicenceUiState,
    m: LicenceMetrics,
    onPlan: (PlanOffer) -> Unit,
    modifier: Modifier,
) {
    val focus = remember { FocusRequester() }
    val lead = state.plans.firstOrNull { isBestValue(it) } ?: state.plans.firstOrNull()
    Row(
        modifier.testTag(LicenceTags.PLANS),
        horizontalArrangement = Arrangement.spacedBy(m.cardGap),
    ) {
        for (offer in state.plans) {
            val id = offer.plan.name.lowercase()
            PlanCard(
                metrics = m.plan,
                name = stringResource(planName(offer.plan)),
                price = price(offer),
                period = stringResource(planPeriod(offer.plan)),
                features = planFeatures(offer.plan).map { stringResource(it) },
                description = planDescription(offer),
                highlight = if (isBestValue(offer)) stringResource(R.string.licence_best_value) else null,
                working = state.opening == id,
                onClick = { onPlan(offer) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(LicenceTags.plan(id))
                    .then(if (offer == lead) Modifier.focusRequester(focus) else Modifier),
            )
        }
    }
    LaunchedFocus(focus, enabled = lead != null)
}

/**
 * The plan the portal ranks first, and the app with it: the one that never needs
 * renewing. A pure function of the offer, so the tag cannot land on a plan by position.
 */
internal fun isBestValue(offer: PlanOffer): Boolean = offer.plan == Plan.LIFETIME

/**
 * Where the plans would stand, in the states that offer none: the condition's mark,
 * the "valid until" line for an active annual licence, and the one action the blocked
 * states offer.
 *
 * Nothing is sold here. A licence that is active needs nothing; one that was withdrawn,
 * will not open, or could not be confirmed is not fixed by money, and selling a second
 * licence to somebody who already paid is the worst thing this screen could do.
 */
@Composable
private fun StatusCard(
    state: LicenceUiState,
    view: LicenceView,
    m: LicenceMetrics,
    onRetry: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier,
) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(m.cardRadius)
    val focus = remember { FocusRequester() }
    Column(
        modifier
            .testTag(LicenceTags.STATUS_CARD)
            .clip(shape)
            .background(colors.homeCardSurface)
            .border(BorderStroke(m.cardStroke, colors.cardSurfaceEdge), shape)
            .padding(horizontal = m.actPadH, vertical = m.actPadV),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.statusGap, Alignment.CenterVertically),
    ) {
        Icon(
            when (view.tone) {
                LicenceTone.Good -> Icons.Rounded.CheckCircle
                LicenceTone.Neutral -> Icons.Rounded.Info
                LicenceTone.Warning, LicenceTone.Bad -> Icons.Rounded.ErrorOutline
            },
            contentDescription = null,
            tint = badgeTone(view.tone),
            modifier = Modifier.size(m.statusIcon),
        )
        ExpiryLine(fadeKey(state)?.expiresAtMs, m)
        val action = view.action
        if (action != null) {
            CastivioButton(
                modifier = Modifier
                    .testTag(LicenceTags.ACTION)
                    .focusRequester(focus),
                text = stringResource(
                    when (action) {
                        LicenceAction.Retry -> R.string.licence_retry
                        LicenceAction.Support -> R.string.licence_support
                    },
                ),
                weight = ButtonWeight.Secondary,
                enabled = state.opening == null,
                onClick = when (action) {
                    LicenceAction.Retry -> onRetry
                    LicenceAction.Support -> onSupport
                },
            )
            LaunchedFocus(focus, enabled = true)
        }
    }
}

/**
 * Put the remote somewhere useful, once, without fighting the user for it: on first
 * composition only, so a line changing above does not drag focus back.
 */
@Composable
private fun LaunchedFocus(focus: FocusRequester, enabled: Boolean) {
    LaunchedEffect(enabled) {
        if (enabled) runCatching { focus.requestFocus() }
    }
}

/**
 * "Valid until 19-07-2027", for an active annual licence and nothing else.
 *
 * A lapsed licence names its day inside its own sentence; lifetime has no expiry; a
 * trial counts days in a sentence of its own, and a date beside it would be two
 * countdowns disagreeing about their units. Nothing at all is composed for them — not
 * an empty box.
 */
@Composable
private fun ExpiryLine(expiresAtMs: Long?, m: LicenceMetrics) {
    if (expiresAtMs == null || expiresAtMs <= 0L) return
    Text(
        text = stringResource(R.string.licence_valid_until, expiryDate(expiresAtMs)),
        style = m.subtitleStyle,
        color = CastivioTheme.colors.onBackgroundVariant,
        maxLines = 1,
        modifier = Modifier.testTag(LicenceTags.EXPIRY),
    )
}

/**
 * The activation card: what to do with a phone, and the code to do it with.
 *
 * The QR is the activation screen's own symbol — the bare portal address, no device
 * identifier in it — at the activation screen's own size (owner, 2026-11-26). The
 * steps are numbered in the brand's gradient, and the address is spelled out under
 * them for a phone without a camera.
 */
@Composable
private fun ActivationCard(state: LicenceUiState, m: LicenceMetrics, modifier: Modifier) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(m.cardRadius)
    Row(
        modifier
            .testTag(LicenceTags.CODE_ZONE)
            .clip(shape)
            .background(colors.homeCardSurface)
            .border(BorderStroke(m.cardStroke, colors.cardSurfaceEdge), shape)
            .padding(horizontal = m.actPadH, vertical = m.actPadV),
        horizontalArrangement = Arrangement.spacedBy(m.actGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.licence_phone_title),
                style = m.actTitleStyle,
                color = colors.onBackgroundStrong,
                maxLines = 1,
            )
            Column(
                Modifier.padding(top = m.actTitleGap),
                verticalArrangement = Arrangement.spacedBy(m.stepGap),
            ) {
                Step(1, stringResource(R.string.licence_step_scan), m)
                Step(2, stringResource(R.string.licence_step_pay), m)
                Step(3, stringResource(R.string.licence_step_auto), m)
            }
            Row(
                Modifier.padding(top = m.urlTop),
                horizontalArrangement = Arrangement.spacedBy(m.urlGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.licence_or_visit),
                    style = m.urlStyle,
                    color = colors.onBackgroundVariant,
                    maxLines = 1,
                )
                Text(
                    text = ltrIsolate(ActivationDestination.HOST),
                    style = m.urlLinkStyle,
                    color = colors.hueAzure,
                    maxLines = 1,
                )
            }
        }

        val bitmap = remember(state.qr) { state.qr?.asImageBitmap() }
        if (bitmap == null) {
            // The plate's shape while the symbol is being drawn, so the card does not
            // change width under the user when it arrives.
            Skeleton(height = m.plate, width = m.plate, cornerRadius = m.cardRadius)
        } else {
            QrPlate(
                code = bitmap,
                plate = m.plate,
                padding = m.platePadding,
                modifier = Modifier.testTag(LicenceTags.QR),
            )
        }
    }
}

/** One numbered step: the number on the brand's gradient, then the words. */
@Composable
private fun Step(number: Int, text: String, m: LicenceMetrics) {
    val colors = CastivioTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(m.stepBadgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(m.stepBadge)
                .clip(RoundedCornerShape(m.stepBadgeRadius))
                .background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = m.stepNumberStyle, color = colors.onPrimary, maxLines = 1)
        }
        Text(text = text, style = m.stepStyle, color = colors.onBackground, maxLines = 2)
    }
}

/**
 * The MAC address and the device key, small, in one line under the cards.
 *
 * Selectable, so a phone can copy either with a long press — the account dialog's way
 * (owner, 2026-11-25) — without a copy button the drawing does not have. Each value is
 * isolated left to right: a MAC drawn unisolated in an Arabic line is reordered by the
 * bidirectional algorithm.
 */
@Composable
private fun Identifiers(state: LicenceUiState, m: LicenceMetrics, modifier: Modifier = Modifier) {
    val address = state.address
    SelectionContainer(modifier.testTag(LicenceTags.IDS)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(m.idsGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Identifier(
                label = stringResource(R.string.licence_mac_label),
                value = address ?: ADDRESS_PLACEHOLDER,
                spoken = address?.let { stringResource(R.string.licence_mac_spoken, spaced(it)) },
                m = m,
                modifier = Modifier.testTag(LicenceTags.MAC),
            )
            state.deviceKey?.let { key ->
                Identifier(
                    label = stringResource(R.string.licence_key_label),
                    value = key,
                    spoken = null,
                    m = m,
                    modifier = Modifier.testTag(LicenceTags.KEY),
                )
            }
        }
    }
}

@Composable
private fun Identifier(label: String, value: String, spoken: String?, m: LicenceMetrics, modifier: Modifier) {
    val colors = CastivioTheme.colors
    Row(
        modifier.then(
            if (spoken != null) Modifier.semantics(mergeDescendants = true) { contentDescription = spoken } else Modifier,
        ),
        horizontalArrangement = Arrangement.spacedBy(m.idValueGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = m.idLabelStyle, color = colors.onBackgroundMuted, maxLines = 1)
        Text(
            text = ltrIsolate(value),
            style = m.idValueStyle,
            color = colors.onBackground,
            maxLines = 1,
            modifier = if (spoken != null) Modifier.clearAndSetSemantics { } else Modifier,
        )
    }
}

/**
 * The condition's sentence, with its date in it where it has one.
 *
 * Two resources and one of them takes an argument: the choice is made once, in
 * `licenceView`, which hands back the resource **and** the timestamp that belongs to
 * it. Nothing here re-inspects the entitlement to work that out.
 */
@Composable
private fun statusSentence(view: LicenceView, status: Int): String {
    val at = view.statusDateMs ?: return stringResource(status)
    return stringResource(status, expiryDate(at))
}

/** A timestamp as the interface's own date, memoised on both of its inputs. */
@Composable
private fun expiryDate(atMs: Long): String {
    val locale = interfaceLocale()
    return remember(atMs, locale) { formatExpiry(atMs, locale) }
}

/**
 * The expiry, in the one date shape Castivio draws: day-month-year in digits, the same
 * in every language. `CastivioDates` carries the shape.
 *
 * The locale is unused and kept on purpose: `LicencePolishTest` hands the screen's own
 * locale in so that it asserts the wiring rather than agreeing with a hard-coded one.
 */
@Suppress("UNUSED_PARAMETER")
internal fun formatExpiry(expiresAtMs: Long, locale: Locale): String =
    CastivioDates.date(expiresAtMs)

/**
 * "3 days remaining in your trial.", in the plural form the language uses: quantity and
 * argument are the same number, which is what Arabic's and Polish's forms need.
 */
@Composable
private fun trialCountdown(licence: EntitlementState?): String {
    val days = (licence as? EntitlementState.TrialActive)?.daysRemaining?.coerceAtLeast(0) ?: 0
    return pluralStringResource(R.plurals.licence_trial_days, days, days)
}

/** The formatted amount, never from a resource. See [formatPrice]. */
@Composable
private fun price(offer: PlanOffer): String {
    val locale = interfaceLocale()
    return remember(offer, locale) { formatPrice(offer.priceMinor, offer.currency, locale) }
}

/**
 * The locale the interface is in, which is not always the device's: Castivio's
 * language is chosen in the app, so `Locale.getDefault()` would be the system's.
 */
@Composable
private fun interfaceLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    }
}

/** The card as one sentence, for a reader that cannot see it as a card. */
@Composable
private fun planDescription(offer: PlanOffer): String {
    val name = stringResource(planName(offer.plan))
    val period = stringResource(planPeriod(offer.plan))
    return "$name, ${price(offer)}, $period"
}

private fun planName(plan: Plan): Int = when (plan) {
    Plan.ANNUAL -> R.string.licence_plan_annual
    Plan.LIFETIME -> R.string.licence_plan_lifetime
    // Unreachable: `PricingConfig.purchasable` filters the trial out, because a trial
    // is granted and not sold. Named rather than defaulted, so that adding a fourth
    // plan stops this compiling instead of labelling it "Annual".
    Plan.TRIAL -> R.string.licence_chip_trial
}

private fun planPeriod(plan: Plan): Int = when (plan) {
    Plan.ANNUAL -> R.string.licence_per_year
    Plan.LIFETIME -> R.string.licence_once
    Plan.TRIAL -> R.string.licence_chip_trial
}

/** The promises at the foot of a card, as drawn: three each, the middle one its own. */
internal fun planFeatures(plan: Plan): List<Int> = when (plan) {
    Plan.ANNUAL -> listOf(
        R.string.licence_feature_all,
        R.string.licence_feature_renews,
        R.string.licence_feature_instant,
    )
    Plan.LIFETIME -> listOf(
        R.string.licence_feature_all,
        R.string.licence_feature_forever,
        R.string.licence_feature_instant,
    )
    Plan.TRIAL -> emptyList()
}

/** `2F:19:EB:20:44:7C` read as separated pairs rather than one long number. */
private fun spaced(address: String): String = address.replace(":", " ")

/** Shown for the instant before the identity resolves. Never a real address. */
private const val ADDRESS_PLACEHOLDER = "··:··:··:··:··:··"
