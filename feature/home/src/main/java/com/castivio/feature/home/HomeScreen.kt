package com.castivio.feature.home

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.castivio.core.common.format.CastivioDates
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioInfoDialog
import com.castivio.core.design.components.EmptyState
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.components.castivioTitleStyle
import com.castivio.core.design.components.formatCount
import com.castivio.core.design.components.ltrIsolate
import com.castivio.core.design.components.ltrToken
import com.castivio.core.design.theme.CASTIVIO_ARTWORK_ASPECT
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.MotionLevel
import com.castivio.core.design.theme.Sizing
import com.castivio.core.design.theme.castivioBackdrop
import com.castivio.core.design.theme.rememberMetrics
import com.castivio.core.platform.PerformanceLog
import com.castivio.domain.Channel
import com.castivio.domain.MediaKind
import com.castivio.domain.Recorded
import com.castivio.domain.entitlement.EntitlementState

/**
 * Home: the dashboard, composed to the frame it is handed rather than to a scroll.
 *
 * ## It fits, and that is a rule rather than a hope
 *
 * Every band on this screen is measured out of the stage before anything is drawn:
 * the header, the four cards, the actions, the device strip and — only if what is
 * left still gives a card a usable height — the disclaimer. Nothing scrolls, because
 * a dashboard that scrolls has stopped answering its own question. That is the defect
 * this revision exists to fix: fixed card heights meant a 393dp handset drew the
 * television's card and pushed half the screen past the fold.
 *
 * ## Four across, on every frame
 *
 * The approved screen is four cards in one row, and it stays four cards in one row on
 * a handset: 873dp gives each about 185, which is wider than the longest of the four
 * names needs. A revision of this screen folded them to two by two below a width
 * threshold, and the result was a phone that looked like a different product from the
 * set. What changes with the frame is the *height* each card is given, which is
 * measured out of the stage, not the number of them in a row.
 *
 * ## The plate, and why it is not a picture
 *
 * A section card carried four coloured rectangles standing in for artwork. On a
 * device they do not read as "artwork is still loading" — they read as four squares
 * of colour presented as if they were covers, which is worse than admitting there is
 * no picture. Castivio has no image loader in this build, so the card says what it
 * *is* instead: the section's own glyph on the section's own hue, drawn with
 * `discFill`/`discBorder`/`discGlyph` — the disc recipe already in the theme, at plate
 * size. Nothing here invents a colour, and nothing pretends to be a cover.
 *
 * ## Two different nothings
 *
 * "No provider yet" and "a provider that carried nothing" look identical if you only
 * check whether the counts are zero, and they need opposite advice.
 * [HomeState.hasSource] separates them; [HomeState.carriesNothing] is only true once
 * every section has actually been asked for.
 *
 * ## The clock
 *
 * `ACTION_TIME_TICK` through a `DisposableEffect`: a broadcast the platform already
 * sends, a receiver unregistered when the screen goes away, and no coroutine of ours
 * left running. The `LaunchedEffect` delay loop that was refused would hang a Compose
 * test until CI killed it; this leaves nothing pending.
 *
 * ## What is deliberately not drawn
 *
 * Time shift and catch-up. Both act on what is playing, so they belong to the player
 * and will be built there; the rail card that used to carry them is the newest
 * additions now. Nothing on this screen fetches anything: the sections are still
 * brought onto the device by the section that was opened, and the subscription pair in
 * the header is what the provider said when it was last asked rather than a fresh call.
 */
@Composable
fun HomeScreen(
    onSeeSection: (CatalogSection) -> Unit,
    /** Add a first subscription, or change the one showing. Opens the activation flow. */
    onAddSource: () -> Unit,
    /**
     * Add another playlist, from the menu of the ones already here.
     *
     * Separate from [onAddSource] because the two are different questions asked by
     * different users. The empty state's button is "I have nothing, set me up", and it
     * opens where the flow has always opened. This one is pressed by somebody who is
     * already watching, already knows what a subscription is, and has just been shown
     * their list — so it opens on the *chooser*, which is the screen that asks how.
     */
    onAddPlaylist: () -> Unit,
    /**
     * Open the saved-subscriptions screen: the one that renames, deletes and shows
     * which is in use. The same activation flow [onAddSource] opens, at the step that
     * screen already occupies — this screen does not know how to get there, only that
     * somebody else does.
     */
    onManageSources: () -> Unit,
    onSettings: () -> Unit,
    /**
     * Opens Catch-up: the channels whose provider keeps an archive, and the programmes that have aired on them. It took
     * the place of the button for the newest additions, a screen that was never built.
     */
    onCatchUp: () -> Unit,
    /**
     * Carry on with the channel watched last: the live card's own press, once something was watched. The shell opens the
     * channels' board with that channel playing in its small player and named in its well -- asking for the password first,
     * where it is locked, as any press on a channel does -- so the viewer lands where they were and the rest of the list is
     * one press away.
     */
    onResume: (Channel) -> Unit,
    /**
     * Carry on from a small card: the film, the show or the station watched last in its section. The shell opens the section with
     * that item started inside it -- the film's page over the films' board, the show's over the shows', the station playing on the
     * radio's -- so Back lands on the section and not on Home.
     */
    onResumeSection: (SectionResume) -> Unit,
    /** Opens the language chooser. */
    onLanguage: () -> Unit,
    /** Ask to leave. The confirmation is the application's, not this screen's. */
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    model: HomeViewModel = hiltViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()

    // **Where the remote is when this board appears.**
    //
    // Home had no initial focus of its own and did not need one while it was never taken
    // out of the composition: the shell drew the subscription flow over it, so coming back
    // found the highlight where it was left. The flow replaces this screen now — it paints
    // no ground, and drawn over a composed Home the two read as one screen — so Home is
    // built afresh on the way back and arrives with nothing focused. On a remote that is a
    // board with no way in until a direction key has been spent entering it.
    //
    // Requested rather than restored, which is the deliberate half. The highlight lands in
    // the same place every time the board appears — the live channels' card, what most visits
    // are for — instead of wherever the last visit ended, so returning is a position a user
    // can learn. The header's buttons are one Up away.
    val boardFocus = remember { FocusRequester() }

    // Not while loading. That branch draws an empty box with nothing focusable in it, and
    // the request would be thrown away by the `runCatching` inside — quietly, and for good,
    // because the effect does not run again on a key it has already seen. Keyed on the
    // state instead, it is asked once the board is really there.
    LaunchedFocus(boardFocus, enabled = !state.loading)

    // **Live TV opens by itself with its first category**, pressed once here: the rest of
    // it arrives behind the board. Only while Home is what the viewer is looking at --
    // leaving it, or the app, ends the wait, so nothing opens on the way back.
    val openSection by rememberUpdatedState(onSeeSection)
    LaunchedEffect(model) { model.opens.collect { openSection(it) } }
    LifecycleStartEffect(model) {
        model.homeShown()
        onStopOrDispose { model.leftHome() }
    }

    // `safeDrawing`, and the metrics are read from what is left after it: turned
    // sideways the system's own navigation sits on one *side*, so a screen that
    // measured the display would size itself to room it does not have and draw its
    // trailing column underneath the navigation.
    //
    // **Each side keeps its own inset** (owner, 2026-11-25). It used to be symmetrised -- the larger of the two spent on both
    // edges, to keep the block centred -- which left, beside a camera cutout on one side, an empty band of the cutout's width
    // on the other. Now each edge is inset by its own obstruction, and the width that band held goes to the radio's narrow
    // card ([SectionBoard]'s `radioExtra`); the metrics are still read as if the inset were symmetric, so every other card,
    // the header's buttons and the footer's facts keep the size they had. Start and end rather than left and right, so
    // turning the device over moves the margin with the cutout.
    val safeArea = WindowInsets.safeDrawing.asPaddingValues()
    val reading = LocalLayoutDirection.current
    val startInset = safeArea.calculateStartPadding(reading)
    val endInset = safeArea.calculateEndPadding(reading)
    val radioExtra = maxOf(startInset, endInset) * 2 - (startInset + endInset)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(
                top = safeArea.calculateTopPadding(),
                bottom = safeArea.calculateBottomPadding(),
            )
            .padding(start = startInset, end = endInset),
    ) {
        val frame = rememberMetrics(maxWidth - radioExtra, maxHeight)
        val plan = Plan.of(frame)
        val rhythm = Rhythm.of(frame)
        // The device's account, opened from the header (owner, 2026-11-25); its facts were the footer's.
        var showAccount by rememberSaveable { mutableStateOf(false) }

        Column(Modifier.fillMaxSize()) {
            // The header is a band across the whole width (`HomeHeader`): the logo, the page's seven buttons with their
            // words, and the clock. The board keeps the stage's own margins under it.
            HomeHeader(
                state = state,
                frame = frame,
                // Every button carries its word, a handset too (owner, 2026-11-25): a glyph alone was not read.
                labelled = true,
                onRefresh = model::refresh,
                onChoosePlaylist = model::choosePlaylist,
                onAddPlaylist = onAddPlaylist,
                onManageSources = onManageSources,
                onCatchUp = onCatchUp,
                onAccount = { showAccount = true },
                onSettings = onSettings,
                onLanguage = onLanguage,
                onExit = onExit,
                modifier = Modifier.fillMaxWidth().height(plan.headerHeight + rhythm.stage),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = frame.edge)
                    .padding(top = rhythm.group, bottom = rhythm.stage),
                verticalArrangement = Arrangement.spacedBy(rhythm.group),
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize())

                    // The empty states carry the requester too, and for the same reason the
                    // board does: their button is the only control on the screen, and this
                    // one is a door into the very flow the user is coming back from.
                    !state.hasSource -> Box(
                        Modifier.fillMaxSize().focusRequester(boardFocus),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            title = stringResource(R.string.home_no_source_title),
                            detail = stringResource(R.string.home_no_source_detail),
                            actionLabel = stringResource(R.string.home_add_source),
                            onAction = onAddSource,
                            icon = Icons.Rounded.PlaylistAdd,
                            secondaryActionLabel = stringResource(R.string.home_settings),
                            onSecondaryAction = onSettings,
                        )
                    }

                    state.carriesNothing -> Box(
                        Modifier.fillMaxSize().focusRequester(boardFocus),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            title = stringResource(R.string.home_empty_title),
                            detail = stringResource(R.string.home_empty_detail),
                            actionLabel = stringResource(R.string.home_change_source),
                            onAction = onAddSource,
                        )
                    }

                    else -> {
                        SectionBoard(
                            state = state,
                            frame = frame,
                            rhythm = rhythm,
                            boardFocus = boardFocus,
                        radioExtra = radioExtra,
                            // The holder decides: a section on the device opens, one that is not
                            // is asked for and loads on its own card while the viewer stays here.
                            onSeeSection = { section -> if (model.press(section)) onSeeSection(section) },
                            // The same press on the live section, and the same decision: what differs is where it goes -- on to the
                            // channel last watched, playing, and not to the board with nothing playing.
                            onResume = { channel -> if (model.press(CatalogSection.Live)) onResume(channel) },
                            // The same press on the section of the card, and the same decision: only where it goes differs.
                            onResumeSection = { offer ->
                                val section = CatalogSection.entries.first { it.kind == offer.kind }
                                if (model.press(section)) onResumeSection(offer)
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        // Carrying on is this row's alone (owner, 2026-11-25): the cards above always open their section.
                        // Always there (owner, 2026-11-25): a section with nothing watched yet keeps its slot, empty.
                        ContinueRow(
                            resume = state.resume,
                            resumes = state.resumes,
                            frame = frame,
                            onResume = { channel -> if (model.press(CatalogSection.Live)) onResume(channel) },
                            onResumeSection = { offer ->
                                val section = CatalogSection.entries.first { it.kind == offer.kind }
                                if (model.press(section)) onResumeSection(offer)
                            },
                            onOpenSection = { section -> if (model.press(section)) onSeeSection(section) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(frame.chip * CONTINUE_OF_CHIP)
                                .padding(bottom = rhythm.group),
                        )
                        FooterLine(state, frame, rhythm)
                    }
                }
            }
        }

        if (showAccount) AccountDialog(state, frame) { showAccount = false }

        // Temporary, for development: where the press on a section has got to -- see `LoadStrip`. Nothing in a build that
        // is not debuggable. Over the foot of the stage and not in the column: the board keeps every dp it has, and what
        // the card covers is the footer's device facts and its disclaimer, which are not controls. Only where there is a
        // board, as the strip always was.
        if (!state.loading && state.hasSource && !state.carriesNothing) {
            LoadStrip(
                section = null,
                runningOnly = false,
                fontSize = frame.fsLabel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = frame.edge)
                    .padding(bottom = rhythm.stage),
            )
        }
    }
}

// ------------------------------------------------------------------ the plan

/**
 * How the canvas is spent: the two bands whose height is not a frame token.
 *
 * There is nothing conditional left in it. On the reference canvas the stage is
 * always 720dp tall, so every band is the same on every device and this arithmetic
 * produces one answer — which is the point of the canvas rather than a simplification
 * of it. What it still does is make the bands *add up*: the cards take what the
 * header, the actions, the strip, the disclaimer and the five gaps between them
 * leave, so a change to any token moves the cards instead of overflowing the stage.
 */
private data class Plan(
    /** The header band: the frame's row plus the strapline this screen sets under it. */
    val headerHeight: Dp,
) {
    companion object {
        /**
         * The header band is the lockup's row and nothing more.
         *
         * It used to be that row *plus* a small line, because a strapline sat under
         * the wordmark. The strapline is gone — it described the application to
         * somebody already using it — and the extra line went with it rather than
         * being left behind as slack the two term cards would silently grow into.
         *
         * **And nothing else is measured out any more.** The board used to be handed a
         * computed height because five bands had to add up: header, cards, actions,
         * device strip, disclaimer. There are two bands now — the board and one footer
         * line — so the board takes what is left with `weight(1f)`. That cannot
         * disagree with the column it lives in the way a second copy of the arithmetic
         * can, and it is why the stage height is no longer a parameter here.
         */
        fun of(frame: CastivioMetrics): Plan = Plan(headerHeight = frame.header)
    }
}

/**
 * The vertical rhythm: four steps, and the relationships between them are the point.
 *
 * ## Why this is not `bandTop` four times
 *
 * It was. One token spaced the header from the board, the board from the footer, *and*
 * the section cards from one another — so the distance between two **groups** was the
 * same as the distance between two **members of a group**, and nothing told a reader
 * where the header ended and the content began. Beside it the utility rail ran at
 * `chipPad / 2`, a third of the cards' step, so two columns standing side by side kept
 * two different time signatures.
 *
 * Four steps now, and each is defined by its relation to the others rather than
 * chosen:
 *
 *  - [group] separates groups, and is the largest. Nothing inside a group may equal it.
 *  - [item] separates members of a group — the three section cards.
 *  - [rail] is exactly **half** [item], so the two columns of the board are the same
 *    rhythm at two densities rather than two unrelated ones. Six controls in the
 *    height of three cards is a 2:1 count; a 2:1 gap is what makes that read as
 *    deliberate.
 *  - [foot] holds the footer's own sentence off its cards, and is the quietest.
 *
 * ## Why the stage padding is [item] and not its own token
 *
 * `stageTop` and `stageBottom` are 17.1 and 15.7 at this frame — different from each
 * other for no reason a viewer could name, and generous on a screen this short. Home
 * is the densest surface in the application and it spends that difference on content.
 * Using [item] for both makes the block symmetric top to bottom, which is what the
 * horizontal gutter already does side to side.
 *
 * The shared tokens are deliberately **not** touched. `stageTop`, `stageBottom` and
 * `bandTop` are read by the shell, the activation flow, the pickers and their tests;
 * moving them to suit one screen would move seven, and none of those was reviewed.
 * This screen states its own rhythm and derives it from the frame, so it still scales
 * with every other size rather than pinning a number to one phone.
 *
 * At the owner's 833×385dp: [group] 20, [item] 12, [rail] 6, [foot] 8, stage 12.
 */
private data class Rhythm(
    /** Above and below the whole block, equal on both sides. */
    val stage: Dp,
    /** Header ↔ board ↔ footer. */
    val group: Dp,
    /** Between the three section cards. */
    val item: Dp,
    /** Between the utility controls. */
    val rail: Dp,
    /** Inside the footer: the identity cards ↔ the sentence under them. */
    val foot: Dp,
) {
    companion object {
        fun of(frame: CastivioMetrics): Rhythm {
            val item = frame.bandTop * ITEM_RATIO
            return Rhythm(
                stage = item,
                group = frame.bandTop * GROUP_RATIO,
                item = item,
                rail = item / 2,
                foot = frame.bandTop * FOOT_RATIO,
            )
        }
    }
}

/**
 * The three ratios, off `bandTop`, that produce the approved drawing's figures.
 *
 * Ratios rather than sizes because a television is not a handset: expressed this way
 * the whole rhythm rides the frame's own scale, so a 1280×720 set gets 37.3 / 22.4 /
 * 11.2 / 15.0 in place of 20 / 12 / 6 / 8 and keeps every relationship between them.
 */
private const val GROUP_RATIO = 1.274f
private const val ITEM_RATIO = 0.764f
private const val FOOT_RATIO = 0.509f

// ---------------------------------------------------------------- the header

// The clock and its ticker were both here, private to this file, until the activation
// header wanted the same pair in the same corner. They are `CastivioClock` and
// `rememberMinute` in `:core:design` now — one declaration, one isolation rule for the
// digits, one receiver. See the note on the component.

/** Whether Castivio's own licence permits use — the one question every screen asks. */
private val HomeState.licenceHolds: Boolean
    get() = entitlement?.allowsUse == true

/**
 * What the provider said about the subscription: the card's word, and the date it runs
 * out where there is one to show.
 *
 * Four answers and not two. A provider that has never been asked is not the same as one
 * that answered "no": the first is a blank the user can fill by refreshing, the second is
 * a fact about their subscription, and a card that showed "Inactive" for both would be
 * accusing a working provider of being dead. And a subscription that ran out is not one
 * refused for another reason: it is the one a user puts right by renewing, so it says so.
 *
 * **Always in the reader's language.** The card used to show the panel's own word where
 * it gave one -- "Active", "Expired", "Banned" -- and panels send it in English whatever
 * the reader reads, so an Arabic screen said "Expired". The words are ours now.
 *
 * **No date for a subscription that ran out**, although the answer may carry one: the
 * date slot means "runs out on", which is future tense, and the word says what happened
 * -- the reasoning [licenceTerm] gives for an expired licence.
 */
internal fun providerTerm(status: Recorded?): Term = when {
    status == null -> Term(R.string.home_account_unknown, null)
    status.usable -> Term(R.string.home_account_active, status.expiresAtMs)
    status.ranOut -> Term(R.string.home_account_expired, null)
    else -> Term(R.string.home_account_inactive, null)
}

/**
 * How the provider card is drawn, for the answers [providerTerm] tells apart.
 *
 * "Not checked" is not "active". Drawn in the active green with a tick, it read as a
 * subscription vouched for by a question nobody had asked. In the muted ink, with an
 * empty circle where the tick goes, it reads as what it is: a blank, which the first
 * answer fills -- and Home asks that question by itself, see
 * [com.castivio.domain.RefreshProvider.ask].
 *
 * Nor is a refusal a tick in red. Expired or inactive, it carries the mark every
 * failure on Home carries -- the circled exclamation of a section that could not load.
 */
internal enum class ProviderTone { Usable, Refused, Unchecked }

internal fun providerTone(status: Recorded?): ProviderTone = when {
    status == null -> ProviderTone.Unchecked
    status.usable -> ProviderTone.Usable
    else -> ProviderTone.Refused
}

/**
 * What the licence card says: one word, and a date only where one is owed.
 *
 * A word and a resource id rather than a formatted string, so the nine states of
 * [EntitlementState] collapse in one pure function that a JVM test can enumerate.
 * Getting this wrong is not a cosmetic defect — telling somebody who paid that their
 * licence has expired is a refund — so it is the part of this screen that is tested,
 * and it is separated from the drawing for exactly that reason.
 *
 * Two of the mappings are deliberate and would each be a bug read the obvious way:
 *
 * **`VerificationUnavailable` carries no date.** It holds `lastKnownExpiresAtMs`, and
 * printing it would say "valid until March" on the strength of a record we have just
 * admitted we cannot confirm. The word says we could not check; a date beside it
 * would quietly contradict the word.
 *
 * **`Lifetime` has no date and that is not a gap.** It was bought outright and does
 * not run out, so the card stops after the word. A dash there would read as a missing
 * value in something that was paid for.
 *
 * `null` is the state before the first answer arrives, and it reads the same as
 * [EntitlementState.Unknown]: nothing has been established. Neither is an accusation.
 */
internal fun licenceTerm(state: EntitlementState?): Term = when (state) {
    is EntitlementState.TrialActive -> Term(R.string.home_licence_trial, state.expiresAtMs)
    is EntitlementState.AnnualActive -> Term(R.string.home_licence_annual, state.expiresAtMs)
    // **No date, although the state carries one.** `expiredAtMs` is when it lapsed,
    // and this card's date slot means "runs out on", which is future tense. "Expired
    // · expires on 4 March" is the screen contradicting itself in four words. The
    // licence screen is where a lapse says *when*; here it says *that*.
    is EntitlementState.AnnualExpired -> Term(R.string.home_licence_expired, null)
    EntitlementState.TrialExpired -> Term(R.string.home_licence_expired, null)
    EntitlementState.Lifetime -> Term(R.string.home_licence_lifetime, null)
    is EntitlementState.Revoked -> Term(R.string.home_licence_revoked, null)
    is EntitlementState.VerificationUnavailable -> Term(R.string.home_licence_unverified, null)
    is EntitlementState.ServiceUnavailable -> Term(R.string.home_licence_unavailable, null)
    EntitlementState.Unknown, null -> Term(R.string.home_licence_unknown, null)
}

/** A term's two halves: the word for it, and when it runs out if it ever does. */
internal data class Term(val word: Int, val atMs: Long?)

// ------------------------------------------------------------- the board

/**
 * The four sections, weighted by how often they are opened.
 *
 * ## Why one of them is large
 *
 * The four were one row of equal cards, and the equality was the defect. Live
 * television is what this application is opened for; Radio is a section most
 * subscriptions do not even carry. Four cards of one size say those are the same
 * decision, so the board offered no path — the eye had to read all four names to
 * find the one it wanted, every time, and a launcher whose items are all equally
 * important is a settings list wearing a launcher's clothes.
 *
 * So Live takes a panel and the other three take a line each. That is not decoration:
 * the room Live gains is what lets it carry its count at a size readable across a
 * living room, and the room the other three give up is room they were spending on
 * nothing — a glyph floating above two short words, with a third of the card empty
 * beneath it.
 *
 * ## The utilities are in the header now
 *
 * They were a quiet column at the board's leading edge. The owner's header (`HomeHeader`, 2026-11-24) carries all seven --
 * refresh, switch playlist, catch-up, settings, language, the appearance and exit -- each with its word, so the board is
 * the four sections and nothing else.
 *
 * ## Which column sits where
 *
 * The owner's second picture (2026-11-24): Live takes the **leading** edge, large; films over series in the middle; the
 * radio a narrow column at the **trailing** edge. Start and end rather than left and right, so a `Row` lays it out in both
 * directions from one declaration.
 *
 * ## The traversal
 *
 * Three columns, with up and down staying inside whichever has focus, and Up from the board reaching the header. The
 * remote starts on Live (see [HomeScreen]). Compose's own focus search resolves all of it from the geometry.
 */
@Composable
private fun SectionBoard(
    state: HomeState,
    frame: CastivioMetrics,
    rhythm: Rhythm,
    /**
     * Where the remote lands when this board appears: the live channels' card, the section most visits are for. Held by the
     * screen because the screen is what knows the board has just been built; see its use in [HomeScreen].
     */
    boardFocus: FocusRequester,
    onSeeSection: (CatalogSection) -> Unit,
    onResume: (Channel) -> Unit,
    onResumeSection: (SectionResume) -> Unit,
    modifier: Modifier = Modifier,
    /** Width the screen gained on the side without a cutout, given whole to the radio's card. */
    radioExtra: Dp = 0.dp,
) {
    val colors = CastivioTheme.colors
    val live = Section(
        CatalogSection.Live, Icons.Rounded.LiveTv, colors.hueAzure,
        R.string.browse_live, state.liveCount, R.string.home_count_live,
        R.string.home_section_no_live, state.sections[MediaKind.LIVE],
        state.loads[MediaKind.LIVE] ?: CardLoad.Idle,
        R.drawable.home_art_live,
    )
    val rest = listOf(
        Section(
            CatalogSection.Movies, Icons.Rounded.Movie, colors.hueViolet,
            R.string.browse_movies, state.movieCount, R.string.home_count_movies,
            R.string.home_section_no_movies, state.sections[MediaKind.MOVIE],
            state.loads[MediaKind.MOVIE] ?: CardLoad.Idle,
            R.drawable.home_art_movies,
        ),
        Section(
            CatalogSection.Series, Icons.Rounded.Tv, colors.hueGreen,
            R.string.browse_series, state.seriesCount, R.string.home_count_series,
            R.string.home_section_no_series, state.sections[MediaKind.SERIES],
            state.loads[MediaKind.SERIES] ?: CardLoad.Idle,
            R.drawable.home_art_series,
        ),
        Section(
            CatalogSection.Radio, Icons.Rounded.Radio, colors.hueAmber,
            R.string.browse_radio, state.radioCount, R.string.home_count_radio,
            R.string.home_section_no_radio, state.sections[MediaKind.RADIO],
            state.loads[MediaKind.RADIO] ?: CardLoad.Idle,
            R.drawable.home_art_radio,
        ),
    )

    // The owner's second picture (2026-11-24): live at the start, large; films over series in the middle; the radio a narrow
    // column at the end. The pictures inside the cards are the owner's to send; until then each card is its glyph and words.
    val (lines, radio) = rest.partition { it.section != CatalogSection.Radio }
    BoxWithConstraints(modifier) {
    // The three shares are of the board as it would be with a symmetric inset; the radio takes its share and the extra.
    val shared = maxWidth - radioExtra - frame.bandTop * 2
    val radioWidth = shared * (RADIO_SHARE / (HERO_SHARE + STACK_SHARE + RADIO_SHARE)) + radioExtra
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(frame.bandTop)) {
        HeroCard(
            section = live,
            frame = frame,
            // The large cards always open their section; carrying on is the row under them.
            resume = null,
            onClick = { onSeeSection(live.section) },
            onResume = onResume,
            modifier = Modifier.weight(HERO_SHARE).fillMaxHeight().focusRequester(boardFocus),
        )

        Column(
            Modifier.weight(STACK_SHARE).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(rhythm.item),
        ) {
            for (section in lines) {
                SectionLine(
                    section = section,
                    frame = frame,
                    resume = null,
                    onClick = { onSeeSection(section.section) },
                    onResume = onResumeSection,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        for (section in radio) {
            SectionLine(
                section = section,
                frame = frame,
                resume = null,
                onClick = { onSeeSection(section.section) },
                onResume = onResumeSection,
                upright = true,
                modifier = Modifier.width(radioWidth).fillMaxHeight(),
            )
        }
    }
    }
}

/** One section's worth of decisions, so the lists above read as lists rather than walls. */
private data class Section(
    val section: CatalogSection,
    val icon: ImageVector,
    val hue: Color,
    val name: Int,
    val count: Int,
    /** "%s channels" — used only when there is a number worth printing. */
    val unit: Int,
    /** What this section says when the provider was asked and sent nothing. */
    val empty: Int,
    /** When this section was brought onto the device, or null if it never has been. */
    val fetchedAtMs: Long?,
    /** What the card says about the section's load, under its headline. */
    val load: CardLoad,
    /** The owner's picture for the section (2026-11-24), behind the card's words. */
    val art: Int,
)

/**
 * What a section says about itself, which is three different sentences.
 *
 * Never a zero. "0 channels" is the sentence a viewer reads as a fault in the
 * application, and it is indistinguishable at a glance from a section that has not
 * been asked yet — the two states this screen exists to keep apart. A section the
 * provider answered with nothing says so in words, and it stays pressable, because
 * pressing it is how it is asked again.
 */
private enum class Tally { Unfetched, Empty, Counted }

private val Section.tally: Tally
    get() = when {
        // Rows on the device are a count, whatever the marks say: a provider refresh
        // forgets every mark and leaves the rows, and the card must not then say the
        // section was never downloaded while it opens onto them.
        count > 0 -> Tally.Counted
        fetchedAtMs != null -> Tally.Empty
        else -> Tally.Unfetched
    }

@Composable
private fun Section.sentence(): String = when (tally) {
    Tally.Unfetched -> stringResource(R.string.home_not_fetched)
    Tally.Empty -> stringResource(empty)
    Tally.Counted -> stringResource(unit, formatCount(count))
}

@Composable
private fun Section.ink(): Color {
    val colors = CastivioTheme.colors
    return when (tally) {
        // A little brighter than the primary (owner, 2026-11-25): beside the violet button it read faint.
        Tally.Unfetched -> lerp(colors.primary, colors.onBackgroundStrong, NOT_FETCHED_LIFT)
        Tally.Empty -> colors.onBackgroundMuted
        // White, as in the owner's drawing (2026-11-25): the card's colour is its picture now, not its figure.
        Tally.Counted -> colors.onBackgroundStrong
    }
}

/**
 * A line's sentence with its figure set larger than the words round it: "**120,654** films" and not "120,654 films" in one
 * size, because the figure is what the eye is looking for. Found in the sentence by its text, so that a language that puts the
 * unit before the number or after it, or writes the figure in its own digits, is set the same way; a sentence the figure is
 * not found in is left as it is.
 */
@Composable
private fun Section.figuredSentence(frame: CastivioMetrics): AnnotatedString {
    val text = sentence()
    if (tally != Tally.Counted) return AnnotatedString(text)
    val figure = formatCount(count)
    val at = text.indexOf(figure)
    if (at < 0) return AnnotatedString(text)
    val large = castivioTitleStyle(frame.fsBody * LINE_FIGURE_RATIO).toSpanStyle().copy(color = Color.Unspecified)
    return buildAnnotatedString {
        append(text)
        addStyle(large, at, at + figure.length)
    }
}

/**
 * Live television, at the size its use deserves.
 *
 * The count is the largest figure on the screen and the only one set in the title
 * step, because it is the one fact a returning viewer checks: that the catalogue on
 * this device is the size it was. It is `tabular` for the same reason every other
 * changing figure in this application is — a number that shifts width as it rises
 * reads as instability in the thing counting.
 *
 * The glyph is drawn on the panel rather than inside a plate of its own. Every
 * section used to be two visible rectangles, one nested in the other, and two edges
 * around one idea is the detail that makes a screen look assembled rather than
 * designed.
 *
 * ## The middle of the panel was empty, and a spacer is not a composition
 *
 * A small glyph at the top and two lines at the bottom left a third of the largest
 * object on the screen as nothing — the void the owner pointed at on a device. The
 * fix is not to centre the words, which only moves the hole; it is to spend the room.
 * The section's own glyph is drawn again at [HERO_GLYPH_SHARE] of the panel's height,
 * centred at [HERO_GLYPH_CENTRE] of it and at [HERO_GLYPH_ALPHA] of its strength, filling
 * the upper part with the words resting on the floor beneath it -- and the frame of that
 * glyph echoed outward in thin lines, and ribbons across the lower half, so that the panel
 * is full without anything being said in the space (see [drawHeroLines]).
 *
 * It is ornament, and that is the point: it says nothing the panel does not already
 * say. A fact fills that space only where the device holds one: the channel watched last,
 * which the card shows instead once there is one (see [HeroFace.Continue]), and a load's
 * progress, said under the name while there is a load; see [HeroLoad]. Nothing is
 * invented to fill it.
 *
 * The watermark is sized from the panel rather than from a token because it is a
 * proportion of a box whose height is decided by the frame; the card clips it, so an
 * unusually short panel crops the glyph instead of overflowing the board.
 */
@Composable
private fun HeroCard(
    section: Section,
    frame: CastivioMetrics,
    resume: Resume?,
    onClick: () -> Unit,
    onResume: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    val arrived = section.load is CardLoad.Arrived
    val face = heroFace(
        unfetched = section.tally == Tally.Unfetched,
        counted = section.tally == Tally.Counted,
        load = section.load,
        resume = resume,
    )
    // The channel watched last is carried on from: the shell opens the board with it in the small player. Every other face
    // keeps the press it always had.
    val resumeChannel = if (face == HeroFace.Continue) resume?.channel else null
    InteractiveGlassCard(
        onClick = if (resumeChannel != null) ({ onResume(resumeChannel) }) else onClick,
        modifier = modifier,
        shape = RoundedCornerShape(frame.radius),
        // Just loaded: a breath of the success green on the glass and its edge, for the
        // few seconds the card says so. Focus still draws its own ring over it.
        //
        // Otherwise it is the same dark glass every card of this screen is, with no light round it: the colour is in the
        // glyph, the lines and the count (see [glassFill]).
        fill = if (arrived) arrivedFill() else CastivioTheme.colors.homeCardSurface,
        restBorder = if (arrived) colors.success.copy(alpha = ARRIVED_EDGE_ALPHA) else colors.cardSurfaceEdge,
    ) {
        Box(Modifier.fillMaxSize()) {
        SectionArt(section.art)
        // The stadium's pitch and goal are white and bright where the name and the count stand (owner, 2026-11-25,
        // design/mockups/home-live-scrim-ar.png): a fade into the page's own ground from the middle of the card down, so
        // the words read and the goal stays seen behind them.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        HERO_SCRIM_FROM to colors.background.copy(alpha = 0f),
                        1f to colors.background.copy(alpha = HERO_SCRIM_ALPHA),
                    ),
                ),
        )
        when (face) {
            HeroFace.Plain -> PlainFace(section, frame)
            HeroFace.NotLoaded -> NotLoadedFace(
                name = stringResource(section.name),
                sentence = section.sentence(),
                sentenceInk = section.ink(),
                frame = frame,
            )
            // Continue is only ever chosen with something to continue from -- see [heroFace] -- so there is always an offer here.
            HeroFace.Continue -> resume?.let { offer ->
                ContinueFace(
                    resume = offer,
                    name = stringResource(section.name),
                    sentence = section.sentence(),
                    sentenceInk = section.ink(),
                    frame = frame,
                )
            }
        }
        }
    }
}

/**
 * The live card whenever it has nothing else to say: over its picture, the name and the count -- or the load, its bar and
 * its failure -- see [HeroLoad].
 */
@Composable
private fun PlainFace(section: Section, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    // The section's picture is behind the card now (see [SectionArt]): no glyph and no lines over it, only the words.
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(frame.chipPad),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(section.name),
                style = castivioTitleStyle(frame.fsLabel),
                color = colors.onBackgroundStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HeroLoad(section, frame)
        }
    }
}

/**
 * Under the panel's name: its count or its sentence, and what its load says.
 *
 * The approved drawing's frames 1, 3, 6 and 7 (`design/mockups/home-loading.html`). A first
 * load has no count to keep, so its words, its bar and where it comes from take the line; a
 * refresh keeps the count above them, because the section is here and opens.
 */
@Composable
private fun HeroLoad(section: Section, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    when (val load = section.load) {
        CardLoad.Idle -> HeroSentence(section, frame)

        is CardLoad.Waiting -> {
            if (section.tally == Tally.Counted) HeroSentence(section, frame)
            WaitingLine(load, frame)
        }

        is CardLoad.Arriving -> {
            if (load.refresh) HeroSentence(section, frame)
            ArrivingLine(section, load, R.string.home_load_loading_live, frame)
            LoadBar(load.percent, colors.warning, HERO_BAR)
            if (!load.refresh) LoadCaption(stringResource(R.string.home_load_source), frame)
        }

        is CardLoad.Arrived -> {
            LoadLine(
                word = stringResource(R.string.home_load_done),
                tone = colors.success,
                frame = frame,
                icon = Icons.Rounded.CheckCircle,
                figure = loadSeconds(load),
                figureTone = colors.onBackgroundMuted,
            )
            HeroSentence(section, frame)
            LoadBar(FULL, colors.success, HERO_BAR)
        }

        is CardLoad.Failed -> if (load.refresh) {
            HeroSentence(section, frame)
            LoadLine(stringResource(R.string.home_load_refresh_failed), colors.danger, frame, Icons.Rounded.ErrorOutline)
        } else {
            LoadLine(stringResource(R.string.home_load_failed_live), colors.danger, frame, Icons.Rounded.ErrorOutline)
            LoadCaption(stringResource(R.string.home_load_retry), frame, colors.onBackgroundVariant)
        }
    }
}

/** The panel's headline: the count in the title step, or a sentence in the body's. */
@Composable
private fun HeroSentence(section: Section, frame: CastivioMetrics) {
    Text(
        text = section.sentence(),
        style = when (section.tally) {
            Tally.Counted -> castivioTitleStyle(frame.fsTitle * HERO_COUNT_RATIO)
            else -> castivioBodyStyle(frame.fsBody)
        },
        color = section.ink(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * One of the other three, on one line.
 *
 * Glyph, name, and what the section says — read in that order and in one pass. The
 * name and the sentence are stacked so the row survives a long translation without
 * the count being pushed off the end, which is what a three-column row of fixed
 * shares would have done to Arabic.
 */
@Composable
private fun SectionLine(
    section: Section,
    frame: CastivioMetrics,
    resume: SectionResume?,
    onClick: () -> Unit,
    onResume: (SectionResume) -> Unit,
    modifier: Modifier = Modifier,
    /** The narrow column's face: glyph, name and figure stacked and centred, as the radio's card in the owner's picture. */
    upright: Boolean = false,
) {
    val colors = CastivioTheme.colors
    val arrived = section.load is CardLoad.Arrived
    // The same three faces as the large card, decided the same way: a section never downloaded invites its download, one here with
    // something watched offers to carry on, and a load of any kind -- queued, running, arrived, failed -- leaves the card as it was.
    val face = tileFace(
        unfetched = section.tally == Tally.Unfetched,
        counted = section.tally == Tally.Counted,
        load = section.load,
        resumed = resume != null,
    )
    val offer = if (face == HeroFace.Continue) resume else null
    InteractiveGlassCard(
        onClick = if (offer != null) ({ onResume(offer) }) else onClick,
        modifier = modifier,
        shape = RoundedCornerShape(frame.radius / 2),
        // The panel's few seconds of success green, on a line as on the panel -- and otherwise the same dark glass.
        fill = if (arrived) arrivedFill() else CastivioTheme.colors.homeCardSurface,
        restBorder = if (arrived) colors.success.copy(alpha = ARRIVED_EDGE_ALPHA) else colors.cardSurfaceEdge,
    ) {
        // The picture is behind the words always (owner, 2026-11-25); while the card carries the poster of what was watched
        // last it steps back to [ART_ALPHA_BEHIND_POSTER], so the poster is the one picture that stands out.
        SectionArt(
            section.art,
            mirrored = !upright,
            alpha = if (offer != null && !upright) ART_ALPHA_BEHIND_POSTER else ART_ALPHA,
        )
        if (upright) {
            UprightFace(section, frame, offer, face)
        } else BoxWithConstraints(Modifier.fillMaxSize()) {
            // Read here, in the box's own scope: the Row below is a scope of its own and does not answer for the box's size.
            val lineHeight = maxHeight
            Row(
                Modifier.fillMaxSize().padding(horizontal = frame.chipPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(frame.chipPad),
            ) {
                Column(Modifier.weight(1f)) {
                    if (offer != null) {
                        TileResumeBody(section, offer, frame)
                    } else {
                        Text(
                            text = stringResource(section.name),
                            style = castivioChipStyle(frame.fsLabel),
                            color = colors.onBackgroundStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LineLoad(section, frame)
                        // Under its words, on their side, as on the radio's card (owner, 2026-11-25): not over the picture.
                        if (face == HeroFace.NotLoaded) {
                            Spacer(Modifier.height(frame.chipPad / 2))
                            TileLoadPill(frame)
                        }
                    }
                }
                if (offer != null) TileThumb(offer, height = lineHeight)
            }
        }
    }
}

/**
 * The radio's narrow card: over its picture, at its foot, its name and under it what its load says -- or, once something
 * was played, the station last played. Centred, as the owner drew it.
 */
@Composable
private fun UprightFace(section: Section, frame: CastivioMetrics, offer: SectionResume?, face: HeroFace) {
    val colors = CastivioTheme.colors
    Column(
        Modifier.fillMaxSize().padding(frame.chipPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(frame.chipPad / 2, Alignment.Bottom),
    ) {
        Text(
            text = stringResource(section.name),
            style = castivioTitleStyle(frame.fsLabel),
            color = colors.onBackgroundStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (offer != null) {
            Text(
                text = offer.title,
                style = castivioBodyStyle(frame.fsBody).copy(textDirection = TextDirection.Content),
                color = colors.onBackgroundVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { LineLoad(section, frame) }
            if (face == HeroFace.NotLoaded) TileLoadPill(frame)
        }
    }
}

/**
 * A section's picture, as the card's background (owner, 2026-11-25): across the whole card, at [ART_ALPHA] over the card's own
 * dark ground, so it colours the card without competing with its words. The films' and the series' pictures have their
 * objects on one side and an empty dark side for the words, drawn for Arabic, where the words are on the right; in a language
 * read left to right the picture is [mirrored] so the words stay on its empty side. There is no text in the pictures, so the
 * mirror is not seen.
 */
@Composable
private fun SectionArt(art: Int, mirrored: Boolean = false, alpha: Float = ART_ALPHA) {
    val flip = mirrored && LocalLayoutDirection.current == LayoutDirection.Ltr
    Image(
        painter = painterResource(art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { if (flip) scaleX = -1f },
    )
}
/**
 * The three lines of a card with something to carry on from: its name with its count -- which is not lost for the sake of the
 * offer -- then what was watched last, then how far in it the viewer was, in the aqua the live programme's bar is drawn in.
 */
@Composable
private fun TileResumeBody(section: Section, offer: SectionResume, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 2)) {
        Text(
            text = stringResource(section.name),
            style = castivioChipStyle(frame.fsLabel),
            color = colors.onBackgroundStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "·",
            style = castivioBodyStyle(frame.fsBody),
            color = colors.onBackgroundMuted,
            maxLines = 1,
        )
        Text(
            text = formatCount(section.count),
            style = castivioTitleStyle(frame.fsBody * LINE_FIGURE_RATIO),
            color = section.ink(),
            maxLines = 1,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 2)) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = colors.discGlyph(section.hue),
            modifier = Modifier.size(Sizing.iconSm),
        )
        val quiet = colors.onBackgroundMuted
        Text(
            text = remember(offer, quiet) {
                buildAnnotatedString {
                    append(offer.title)
                    offer.detail?.let { detail ->
                        withStyle(SpanStyle(color = quiet)) { append(" · $detail") }
                    }
                }
            },
            style = castivioBodyStyle(frame.fsBody).copy(textDirection = TextDirection.Content),
            color = colors.onBackgroundStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    offer.progress?.let { ProgrammeBar(fraction = it, modifier = Modifier.padding(top = frame.chipPad / 4)) }
}

/**
 * Under a line's name: its sentence, or what its load says, in the one line there is room
 * for -- with the bar under it while the load runs. Frames 2 to 6 of the approved drawing.
 */
@Composable
private fun LineLoad(section: Section, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    when (val load = section.load) {
        CardLoad.Idle -> Text(
            text = section.figuredSentence(frame),
            style = castivioBodyStyle(frame.fsBody),
            color = section.ink(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        is CardLoad.Waiting -> WaitingLine(load, frame)

        is CardLoad.Arriving -> {
            ArrivingLine(section, load, R.string.home_load_loading, frame)
            LoadBar(load.percent, colors.warning, LINE_BAR)
        }

        is CardLoad.Arrived -> {
            LoadLine(
                word = stringResource(section.unit, formatCount(load.items)),
                tone = colors.success,
                frame = frame,
                icon = Icons.Rounded.CheckCircle,
            )
            LoadBar(FULL, colors.success, LINE_BAR)
        }

        is CardLoad.Failed -> if (load.refresh) {
            LoadLine(stringResource(R.string.home_load_refresh_failed), colors.danger, frame, Icons.Rounded.ErrorOutline)
        } else {
            LoadLine(
                word = stringResource(R.string.home_load_failed),
                tone = colors.danger,
                frame = frame,
                icon = Icons.Rounded.ErrorOutline,
                figure = stringResource(R.string.home_load_retry_short),
            )
        }
    }
}

/** Asked for, behind another load -- or, pressed while it waits, when it will open. */
@Composable
private fun WaitingLine(load: CardLoad.Waiting, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    if (load.hinted) {
        LoadLine(stringResource(R.string.home_load_opens), colors.focusRing, frame)
    } else {
        LoadLine(stringResource(R.string.home_load_waiting), colors.onBackgroundMuted, frame, Icons.Rounded.Schedule)
    }
}

/**
 * A load on its way: the working word -- [loadingWord] for a first load, "refreshing" for a
 * section that is already here -- and at its end how far along it is, as a percent where
 * the load knows its whole and, for a first load, as the rows so far where it does not.
 *
 * A refresh with no whole says no figure: the count above it is the section's, and a
 * second, smaller count of rows rewritten so far beside it reads as the section shrinking.
 */
@Composable
private fun ArrivingLine(section: Section, load: CardLoad.Arriving, loadingWord: Int, frame: CastivioMetrics) {
    val colors = CastivioTheme.colors
    val figure = when {
        load.percent != null -> stringResource(R.string.home_load_percent, load.percent)
        load.items > 0 && !load.refresh -> stringResource(section.unit, formatCount(load.items))
        else -> null
    }
    when {
        load.hinted -> LoadLine(stringResource(R.string.home_load_opens), colors.focusRing, frame, figure = figure)
        load.refresh -> LoadLine(stringResource(R.string.home_load_refreshing), colors.warning, frame, Icons.Rounded.Refresh, figure)
        else -> LoadLine(stringResource(loadingWord), colors.warning, frame, figure = figure)
    }
}

/**
 * One line of a card's load: a word in its state's colour (`UI_ARCHITECTURE.md` §5.1 --
 * working amber, success green, danger red, never colour alone), and a figure at its end.
 */
@Composable
private fun LoadLine(
    word: String,
    tone: Color,
    frame: CastivioMetrics,
    icon: ImageVector? = null,
    figure: String? = null,
    figureTone: Color = CastivioTheme.colors.onBackgroundVariant,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 2),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(Sizing.iconSm))
        }
        Text(
            text = word,
            style = castivioBodyStyle(frame.fsBody),
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (figure != null) {
            Text(
                text = figure,
                style = castivioBodyStyle(frame.fsBody),
                color = figureTone,
                maxLines = 1,
            )
        }
    }
}

/** Where a first load comes from, or what pressing a failed one does: small, under the bar. */
@Composable
private fun LoadCaption(text: String, frame: CastivioMetrics, tone: Color = CastivioTheme.colors.onBackgroundMuted) {
    Text(
        text = text,
        style = castivioBodyStyle(frame.fsChip),
        color = tone,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = HERO_BAR),
    )
}

/**
 * The hairline under a load's words: filled to [percent] where the load knows its whole.
 *
 * Where it does not, a short segment travels the track -- not progress, and not pretending
 * to be: the rows counted beside the words are the progress. With motion off it stands
 * still and the count carries it alone. Never a bar that fills on a timer.
 */
@Composable
private fun LoadBar(percent: Int?, tone: Color, thickness: Dp) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(thickness / 2)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(top = thickness)
            .height(thickness)
            .clip(shape)
            .background(colors.glassBorderSoft),
    ) {
        if (percent != null) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent.coerceIn(0, FULL) / FULL.toFloat())
                    .background(tone, shape),
            )
        } else {
            val segment = maxWidth * SEGMENT_SHARE
            val room = maxWidth - segment
            val travel = travellingSegment()
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(segment)
                    // `offset` follows the reading direction, so the segment starts from the
                    // start edge in Arabic as in English. Read in layout, not composition.
                    .offset { IntOffset((room * travel()).roundToPx(), 0) }
                    .background(tone, shape),
            )
        }
    }
}

/** Where the segment is, from 0 (the start edge) to 1 (the end). Still with motion off. */
@Composable
private fun travellingSegment(): () -> Float {
    if (CastivioTheme.motionLevel == MotionLevel.DISABLED) return { SEGMENT_REST }
    val travel by rememberInfiniteTransition(label = "loadSegment").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SEGMENT_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "loadSegmentTravel",
    )
    return { travel }
}

/** How long the load took, in the builds that print measurements; null in the rest. */
@Composable
private fun loadSeconds(load: CardLoad.Arrived): String? {
    val context = LocalContext.current
    val instrumented = remember(context) { PerformanceLog.isVisibleIn(context) }
    val seconds = load.seconds ?: return null
    return if (instrumented) stringResource(R.string.home_load_seconds, seconds.toInt()) else null
}

/** The success green on the glass, for the moments a card says it has just loaded. */
@Composable
private fun arrivedFill(): Brush {
    val success = CastivioTheme.colors.success
    return Brush.verticalGradient(listOf(success.copy(alpha = ARRIVED_FILL_TOP), success.copy(alpha = ARRIVED_FILL_BOTTOM)))
}

// --------------------------------------------------------------- the playlists

/**
 * The header's control that changes which playlist is showing.
 *
 * ## Why it is a menu and not a screen
 *
 * It used to open the activation flow, which is three presses from here to the list
 * of saved subscriptions and a full-screen departure from Home to perform what is,
 * for a user with two playlists, a toggle. The subscriptions were always there —
 * `SourceRepository` has carried a list, an active one and a switch since the
 * saved-sources screen shipped — and the only thing missing was somewhere near the
 * user to spend them. The saved-sources screen is not replaced: it still owns adding
 * and managing, and the menu's last entry is a door to it — which is what separates
 * the two. This menu does one thing to a playlist, immediately: it switches. Renaming
 * one, deleting one, or reading what a subscription actually is are not menu-sized
 * jobs, and they already have a screen.
 *
 * ## Why it costs the layout nothing
 *
 * The rail is five cards in a fixed column and this is still one of them: the picker
 * is a `Box` that takes exactly the weight the card took, with the same card drawn to
 * fill it. A [DropdownMenu] draws in a popup window of its own, so an open menu does
 * not participate in this layout at all — nothing reflows, nothing is measured twice,
 * and the board behind it keeps every dimension it had.
 *
 * ## The names
 *
 * What the user typed when they added the subscription, and `Playlist 1`, `Playlist
 * 2` when they typed nothing — the numbering is the repository's order, which is the
 * order they were added, so the number beside a playlist does not move when another
 * is added after it. They may be long and they are one line each: a playlist called
 * after a provider's full marketing name is a name to recognise at a glance, not to
 * read to the end, and the menu is wider than the rail card it hangs off precisely so
 * that glance usually succeeds.
 */
@Composable
internal fun PlaylistPicker(
    state: HomeState,
    frame: CastivioMetrics,
    onChoose: (String) -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
    /** The control the menu hangs from: given the modifier that anchors the menu and returns the remote to it, and the press. */
    anchor: @Composable (Modifier, () -> Unit) -> Unit,
) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(frame.radius / 2)
    // `rememberSaveable`: a rotation with the menu open should not silently close it,
    // and the boolean is the whole of this control's state — what is *in* the menu
    // comes from the store, so there is nothing else here that could go stale.
    var open by rememberSaveable { mutableStateOf(false) }

    // **Where the remote goes, and where it comes back to.**
    //
    // A menu that opens without taking focus is a menu a D-pad cannot reach: the
    // highlight stays on the rail behind it, Down moves to the card *below* the
    // button rather than into the list, and the only way to choose a playlist is a
    // touchscreen. So the first entry asks for focus when the menu opens, and the
    // button asks for it back when the menu closes — otherwise the highlight is left
    // pointing at a popup that no longer exists and the next press goes nowhere.
    //
    // Two requesters and not one, because they belong to two different windows: the
    // menu is a popup of its own, and a requester attached to a composable that has
    // left the composition throws rather than moving anything. The `runCatching` in
    // [LaunchedFocus] is what makes a request arriving a frame too early harmless.
    val anchorFocus = remember { FocusRequester() }
    val firstItemFocus = remember { FocusRequester() }

    // **Where the application's backdrop is, seen from inside the menu's own window.**
    //
    // The menu is a popup, so `castivioBackdrop()` applied to it would size all four
    // layers to a 200dp panel: the cool glow at a 104dp radius instead of 433, which
    // is a bright patch rather than ambient light, and the whole three-stop gradient
    // compressed into a box that then starts at the darkest stop. `ground` is what
    // lets it draw its *slice* of the one backdrop instead.
    //
    // ## Why there are two of them
    //
    // The menu only learns where it sits during its own layout pass. The rail card
    // does not: it is laid out with the board, long before anything can be pressed,
    // so its ground is known and correct while the menu is still closed. It seeds
    // the menu's, and the menu refines it once its own position arrives.
    //
    // That seeding is the answer to the flash. Without it the first drawn frame would
    // fall back to `Rect(0, 0, size)` — the compressed miniature — and correct itself
    // immediately after, which is a visible jump at exactly the moment a user is
    // looking at the menu appearing. With it the first frame is already within a few
    // dp of the final one, because the menu opens *at* the anchor.
    val view = LocalView.current
    // The backdrop's rectangle on screen, and the anchor's own view of it. Both are
    // known while the menu is still closed, because the rail card is laid out with
    // the board — which is what lets the menu's first drawn frame already be right.
    var window by remember { mutableStateOf<Rect?>(null) }
    var anchorGround by remember { mutableStateOf<Rect?>(null) }
    val menuGround = remember { MutableGround() }

    // **Focus is returned deliberately, not as a side effect of the press.** Choosing
    // a playlist, adding one, opening the subscriptions screen and dismissing with
    // Back all end in the same place, so the rule is written once instead of at each
    // of the four exits.
    //
    // A flag rather than an effect keyed on `open`, because `open` starts false: an
    // effect on it would grab the remote on the first composition of Home and pull it
    // onto this button before the user had touched anything.
    var returning by remember { mutableStateOf(false) }
    LaunchedFocus(anchorFocus, enabled = returning)
    LaunchedEffect(returning) { if (returning) returning = false }

    Box(modifier) {
        anchor(
            Modifier
                .focusRequester(anchorFocus)
                .onGloballyPositioned { coords ->
                    val onScreen = coords.backdropWindowOnScreen(view)
                    window = onScreen
                    anchorGround = onScreen?.let { coords.groundFor(it, view) }
                },
        ) { open = true }

        // **Material's own surface is switched off, not painted over.**
        //
        // `DropdownMenu` draws a `Surface` filled from `colorScheme.surfaceContainer`
        // with a tonal overlay on top, and on this backdrop that is the flat grey
        // rectangle the owner photographed — an application-shaped hole in a screen
        // made of glass. This build's Material 3 has no `containerColor` parameter on
        // the menu to set instead, so the three tokens it reaches for are made
        // transparent for the subtree and the panel paints its own fill — see the
        // modifier below for what, and why it is two brushes rather than one.
        //
        // Scoped to this composable, so nothing else on the screen sees the override.
        // It costs a `MaterialTheme` node while the menu is open and nothing at all
        // while it is closed, and it keeps everything `DropdownMenu` is worth having
        // for: it clamps itself inside the window, takes D-pad focus, dismisses on
        // back and on an outside press, and animates in.
        // **The locale has to be carried into the popup by hand.**
        //
        // `MainActivity` applies the chosen language by providing a `Context` wrapped
        // in it, and every `stringResource` on this screen resolves against that. A
        // `DropdownMenu` does not draw in this composition though — it draws in a
        // `Popup`, which is a window of its own, and Compose re-provides `LocalContext`
        // there from *that window's* context. The wrapper is gone by then, so the menu
        // resolved its strings against the device's language while the board behind it
        // was in the user's: Spanish everywhere and `Add playlist` in English, which is
        // exactly what the owner photographed.
        //
        // Nothing was missing from the translations — `values-es` has had `Añadir
        // lista` since the menu shipped. So these three are captured *here*, in the
        // composition where the activity's providers are still in scope, and handed
        // back inside the popup. The layout direction goes with them for the same
        // reason: a popup that reset the context would reset the direction too, and an
        // Arabic menu laid out left to right is the same defect wearing a different hat.
        val localised = LocalContext.current
        val configuration = LocalConfiguration.current
        val reading = LocalLayoutDirection.current

        val scheme = MaterialTheme.colorScheme
        MaterialTheme(
            colorScheme = scheme.copy(
                surface = Color.Transparent,
                surfaceContainer = Color.Transparent,
                surfaceTint = Color.Transparent,
            ),
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            DropdownMenu(
                expanded = open,
                // Back on a remote, and a press outside on a touchscreen, both arrive
                // here. The popup is focusable, so the system routes Back to it rather
                // than to Home — the menu closes and the board behind it stays exactly
                // where it was, which is the whole of requirement 2.
                onDismissRequest = {
                    open = false
                    returning = true
                },
                // Bounds only for the width. Material 3's own menu content already
                // sizes its column to `IntrinsicSize.Max`, so the menu is as wide as
                // its widest name by default and repeating that here would be two
                // rules deciding one width.
                modifier = Modifier
                    .widthIn(min = PICKER_MIN_WIDTH, max = PICKER_MAX_WIDTH)
                    .clip(shape)
                    // **The ground is the application's, not a second copy of it.**
                    //
                    // `glassFillBrush` is 8% white over 5%: it is *lift*, not a colour,
                    // and it works on every other card because it sits on the board's
                    // backdrop and borrows it. A popup has no backdrop to borrow — it
                    // is its own window — so the glass alone left the menu 92%
                    // transparent and the rail's buttons read straight through the
                    // playlist names.
                    //
                    // `castivioBackdrop()` is what an overlay is supposed to use, and
                    // its own documentation says so: the four layers behind whatever
                    // it is applied to, opaque from the first one. Painting the aurora
                    // gradient here by hand was a *second* implementation of the ground
                    // — it reproduced one of the four layers and missed the two glows
                    // and the mesh, so the panel came out darker and its gradient ran
                    // the other way from the board's. This is the one system, and the
                    // day those four layers change the menu changes with them.
                    .castivioBackdrop { menuGround.rect ?: anchorGround }
                    .background(colors.cardSurface)
                    .border(BorderStroke(1.dp, colors.cardSurfaceEdge), shape),
            ) {
                // The popup's own view, which is what knows where the popup window
                // sits on the screen. `LocalView` outside the menu is Home's view and
                // would put the menu's ground in the wrong window.
                val menuView = LocalView.current

                // Handed back what the popup's own window took away. See above.
                CompositionLocalProvider(
                    LocalContext provides localised,
                    LocalConfiguration provides configuration,
                    LocalLayoutDirection provides reading,
                ) {
                    // **The remote cannot leave the menu while it is open.**
                    //
                    // `focusGroup` makes the column one stop rather than a handful of
                    // siblings the focus search can wander out of, and the popup is a
                    // window of its own so there is nothing outside it to wander *to*.
                    // Up and Down then move between entries and stop at the ends
                    // instead of silently handing the highlight back to the board.
                    Column(
                        Modifier
                            .focusGroup()
                            // **Written during layout, read during draw, same frame.**
                            //
                            // That is why `castivioBackdrop` takes a lambda and why
                            // this holder is a plain field rather than snapshot state:
                            // `onGloballyPositioned` fires at the end of the layout
                            // pass, before drawing, so a value written here is visible
                            // to *this* frame's draw. A `mutableStateOf` would schedule
                            // another frame instead, and the one frame in between —
                            // drawn against the compressed miniature — is the flash
                            // this whole arrangement exists to prevent.
                            //
                            // It measures the content column rather than the menu's
                            // own surface, which differs by the menu's internal
                            // padding: a few dp, against a glow 433dp across.
                            .onGloballyPositioned { coords ->
                                menuGround.rect = window?.let { coords.groundFor(it, menuView) }
                            },
                    ) {
                        // Asked for as the menu opens, once. Without it the popup takes
                        // window focus but nothing inside it is focused, so the first Down
                        // is spent arriving rather than moving.
                        LaunchedFocus(firstItemFocus, enabled = open)

                        for ((index, playlist) in state.playlists.withIndex()) {
                            val name = playlist.name.ifBlank {
                                stringResource(R.string.home_playlist_numbered, playlist.position)
                            }
                            val isActive = playlist.id == state.activePlaylistId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        name,
                                        style = castivioChipStyle(frame.fsLabel),
                                        color = if (isActive) colors.onBackgroundStrong else colors.onBackgroundVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                // The tick leads, and the row keeps its indent when there is no
                                // tick to draw: a list whose names shift sideways as the active
                                // one moves is a list that looks like it reflowed rather than
                                // one that answered.
                                leadingIcon = {
                                    if (isActive) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.hueGreen,
                                            modifier = Modifier.size(Sizing.iconSm),
                                        )
                                    } else {
                                        Spacer(Modifier.size(Sizing.iconSm))
                                    }
                                },
                                onClick = {
                                    open = false
                                    returning = true
                                    // Guarded, not because a second write would corrupt anything —
                                    // `setActive` is idempotent and transactional — but because
                                    // re-activating the current source makes the store re-emit and
                                    // every flow hanging off it recompute for no change.
                                    if (!isActive) onChoose(playlist.id)
                                },
                                // Only the first: it is where the remote lands when the menu
                                // opens, and every other entry is one Down away from it.
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstItemFocus)
                                } else {
                                    Modifier
                                },
                            )
                        }

                        // Separated, because it is not one of the things above: the list answers
                        // "which one", and this answers "another one". Drawn only when there is a
                        // list to separate it from — on a device with a single playlist the menu
                        // is one entry and a rule above it would be a divider dividing nothing.
                        if (state.playlists.isNotEmpty()) {
                            HorizontalDivider(color = colors.edgeQuiet)
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.home_add_playlist),
                                    style = castivioChipStyle(frame.fsLabel),
                                    color = colors.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = colors.hueViolet,
                                    modifier = Modifier.size(Sizing.iconSm),
                                )
                            },
                            onClick = {
                                open = false
                                // **No `returning` here, and that is the fix.** This exit
                                // leaves Home: the activation flow opens over the board
                                // and owns the remote from that moment. Asking Home's
                                // rail button for focus at the same instant put the
                                // highlight on a control *underneath* a full-screen
                                // overlay, so the D-pad was driving the screen nobody
                                // could see and the flow looked like it had not opened.
                                //
                                // Only the two exits that stay on Home return focus:
                                // choosing a playlist, and dismissing the menu.
                                onAdd()
                            },
                            // The landing place when there are no playlists to land on.
                            // A menu whose only entries are "add" and "manage" still
                            // has to be reachable by remote, and the requester above
                            // is attached to a row that does not exist.
                            modifier = if (state.playlists.isEmpty()) {
                                Modifier.focusRequester(firstItemFocus)
                            } else {
                                Modifier
                            },
                        )

                        // **The menu switches; this opens the place that manages.**
                        //
                        // Both of the entries above are one-press actions on the playlists
                        // themselves. Renaming one, deleting one, or reading what a subscription
                        // actually is are not menu-sized jobs and they already have a screen —
                        // the saved-subscriptions screen this flow has always carried, reached
                        // until now only by going through the source chooser. So this is a door
                        // to that screen, not a second implementation of it: `onManage` opens the
                        // same `ActivationRoute`, at the step that screen already occupies.
                        //
                        // No divider above it, because it belongs with "add" rather than with the
                        // list: the two together are "do something about my subscriptions", and
                        // the one rule in this menu separates that group from the names.
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.home_my_subscriptions),
                                    style = castivioChipStyle(frame.fsLabel),
                                    color = colors.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = colors.hueViolet,
                                    modifier = Modifier.size(Sizing.iconSm),
                                )
                            },
                            onClick = {
                                open = false
                                // Leaves Home, so no focus return. See the entry above.
                                onManage()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Which look the refresh card wears: at rest, asking, failed, or just answered.
 *
 * Asking wins over everything, because a press in flight is the newest fact -- a failure
 * from before it is being retried. A failure stays until that press; an answer is said for
 * a few seconds and goes by itself. One press never leaves both set.
 */
internal sealed interface RefreshLook {
    data object Idle : RefreshLook
    data object Busy : RefreshLook
    data class Failed(val fault: RefreshFault) : RefreshLook
    data object Answered : RefreshLook
}

internal fun refreshLook(state: HomeState): RefreshLook {
    val fault = state.refreshFault
    return when {
        state.refreshing -> RefreshLook.Busy
        fault != null -> RefreshLook.Failed(fault)
        state.refreshed -> RefreshLook.Answered
        else -> RefreshLook.Idle
    }
}

/**
 * What a failed refresh is read out as: the sentence the card has no room for.
 *
 * Two, because the two failures need opposite ones: a provider that was never there is
 * asking for a subscription, and a provider that did not answer is asking for another
 * press in a minute. The same split `Refreshed` draws in the domain.
 */
internal fun refreshSentence(fault: RefreshFault): Int = when (fault) {
    RefreshFault.NoProvider -> R.string.home_refresh_no_provider
    RefreshFault.Unreachable -> R.string.home_refresh_unreachable
}

/**
 * A ground rectangle written during layout and read during drawing.
 *
 * Deliberately *not* snapshot state. `onGloballyPositioned` runs at the end of the
 * layout pass and drawing runs after it in the same frame, so a plain field written
 * there is visible to that frame's draw. A `mutableStateOf` would invalidate the
 * composition instead and the corrected ground would land one frame later — and that
 * one frame, drawn against a miniature of the backdrop, is exactly the flash a menu
 * must not have at the moment it appears.
 *
 * The cost of the plain field is that a *change* of position does not invalidate the
 * drawing by itself. It does not need to: a popup is laid out when it opens and does
 * not move while it is open, and the layout that would move it redraws it anyway.
 */
private class MutableGround {
    var rect: Rect? = null
}

/**
 * The rectangle the application's backdrop occupies, **in screen coordinates**.
 *
 * Read from the board, whose composition root *is* the activity's window — the same
 * box `castivioBackdrop()` fills for the shell. The screen is the common frame
 * between two windows, which is the whole reason this is expressed in it: a popup is
 * a second window on the same screen, and nothing inside it can describe the first
 * one in its own terms.
 *
 * Null until the node is attached and measured, which the caller reads as "not yet".
 */
private fun LayoutCoordinates.backdropWindowOnScreen(view: View): Rect? {
    if (!isAttached) return null
    val window = rootCoordinates()
    val width = window.size.width
    val height = window.size.height
    if (width == 0 || height == 0) return null
    val origin = IntArray(2).also { view.getLocationOnScreen(it) }
    return Rect(
        left = origin[0].toFloat(),
        top = origin[1].toFloat(),
        right = (origin[0] + width).toFloat(),
        bottom = (origin[1] + height).toFloat(),
    )
}

/**
 * The outermost node of this composition, reached by walking parents.
 *
 * `parentLayoutCoordinates` is a member of `LayoutCoordinates`, which is the whole
 * reason the walk is written out rather than delegated: the library's own helper for
 * this is an extension function, and an extension is only as available as its import.
 * A member cannot be missed the same way.
 */
private fun LayoutCoordinates.rootCoordinates(): LayoutCoordinates {
    var node: LayoutCoordinates = this
    while (true) {
        val parent = node.parentLayoutCoordinates ?: return node
        if (!parent.isAttached) return node
        node = parent
    }
}

/**
 * That same rectangle, moved into this node's own coordinates.
 *
 * This node's origin on screen is where *its* window sits plus where it sits inside
 * that window, and the backdrop's rectangle translated by the negative of that is
 * where the backdrop begins as far as this node is concerned. Handed to
 * `castivioBackdrop`, it makes the node draw its slice of the one ground.
 *
 * `getLocationOnScreen` rather than a Compose coordinate call, because the Compose
 * ones stop at the window: `positionInWindow` inside a popup is a position inside
 * *the popup*, which is the confusion this resolves rather than inherits.
 */
private fun LayoutCoordinates.groundFor(windowOnScreen: Rect, view: View): Rect? {
    if (!isAttached) return null
    val origin = IntArray(2).also { view.getLocationOnScreen(it) }
    val here = positionInRoot()
    return windowOnScreen.translate(-(origin[0] + here.x), -(origin[1] + here.y))
}

/**
 * Put the remote somewhere useful, once, without fighting the user for it.
 *
 * Requested when [enabled] turns true and not on every recomposition, which would drag
 * the highlight back each time anything else on the screen changed — exactly when the
 * user is reading somewhere else. `runCatching` because a `FocusRequester` whose
 * composable has not been attached yet, or has just left, throws rather than doing
 * nothing: the popup and the board are two windows and their frames do not line up.
 *
 * The same helper the licence screen uses, deliberately written again rather than
 * shared: it is four lines, and moving it to `:core:design` would make a focus policy
 * out of what is currently a local decision each screen is free to make differently.
 */
@Composable
private fun LaunchedFocus(focus: FocusRequester, enabled: Boolean) {
    LaunchedEffect(enabled) {
        if (enabled) runCatching { focus.requestFocus() }
    }
}

// ---------------------------------------------------------------- the footer

/**
 * What this device is and where its two terms stand, as five facts on the page (the owner's drawing, 2026-11-24): the MAC
 * address, the device key, the device's state, Castivio's licence and the provider's subscription.
 *
 * ## No frame, and the value before everything
 *
 * Each fact is its name, small and quiet, over its value, large and in the ink: the address and the key are what a viewer
 * reads out to a reseller or photographs, so they are the largest text on the line, isolated for direction because a
 * colon-separated hex address reordered by an Arabic paragraph is not the address any more. A state is said in its colour
 * with its mark: the success green, the amber in a licence's last week, the failure red.
 *
 * ## The two terms came here from the header
 *
 * The provider's subscription and Castivio's licence both run out, and a user who confuses the two rings the wrong support
 * line; each keeps its own name and its own date. Nothing is invented: the provider's date is what it last stated, the
 * licence's what [EntitlementState] carries (see [licenceTerm]).
 *
 * ## And the subscription's trouble, while there is one
 *
 * A server that does not answer, a device with no connection, a subscription run out or refused: the line under the facts
 * says why nothing plays and what to do (see [SubscriptionTrouble]), in the failure red, and is gone once the provider
 * answers.
 *
 * ## The device key
 *
 * `DeviceKeyV1`: derived from the same seed as the address, under its own label, stable across launches and reinstalls.
 * Not drawn until there is one.
 */
@Composable
private fun FooterLine(
    state: HomeState,
    frame: CastivioMetrics,
    rhythm: Rhythm,
    modifier: Modifier = Modifier,
) {
    val colors = CastivioTheme.colors
    // Nothing at rest (owner, 2026-11-25: the disclaimer is the terms' business, in Settings); the subscription's trouble,
    // while there is one, says why nothing plays.
    val trouble = state.trouble ?: return
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 2, Alignment.CenterHorizontally),
    ) {
        Icon(trouble.icon, contentDescription = null, tint = colors.danger, modifier = Modifier.size(Sizing.iconSm))
        Text(
            stringResource(troubleSentence(trouble)),
            style = castivioBodyStyle(frame.fsChip),
            color = colors.danger,
            textAlign = TextAlign.Center,
            maxLines = DISCLAIMER_LINES,
        )
    }
}

/**
 * The device's account, as the header's Account button opens it (owner, 2026-11-25): the MAC address, the device key,
 * the device's state, Castivio's licence and the provider's subscription, each its name and its value, the states in
 * their colours. These were the footer's; the footer carries the disclaimer now.
 */
@Composable
internal fun AccountDialog(state: HomeState, frame: CastivioMetrics, onClose: () -> Unit) {
    val colors = CastivioTheme.colors
    BackHandler(onBack = onClose)
    // Copying is for a phone (owner, 2026-11-25): the address and the key are selected and copied with the system's own
    // long press, not a button beside them -- the buttons made those two rows taller than the others. A television has
    // nothing to paste them into, so there they are only read out.
    val canCopy = !CastivioTheme.device.isTv
    CastivioInfoDialog(
        title = stringResource(R.string.home_account),
        closeLabel = stringResource(R.string.home_close),
        onClose = onClose,
        // Narrower (owner, 2026-11-25): the rows are short, and the full share left an empty middle.
        widthShare = ACCOUNT_WIDTH_SHARE,
    ) {
        AccountRow(stringResource(R.string.home_label_mac), frame) {
            Copyable(canCopy) { FactValue(state.mac, colors.onBackgroundStrong, frame, ltr = true) }
        }
        if (state.deviceKey.isNotBlank()) {
            AccountRow(stringResource(R.string.home_label_key), frame) {
                Copyable(canCopy) { FactValue(state.deviceKey, colors.onBackgroundStrong, frame, ltr = true) }
            }
        }
        AccountRow(stringResource(R.string.home_label_state), frame) {
            val hue = if (state.licenceHolds) colors.success else colors.danger
            StateMark(if (state.licenceHolds) Icons.Rounded.CheckCircle else Icons.Rounded.Lock, hue)
            FactValue(
                stringResource(if (state.licenceHolds) R.string.home_device_activated else R.string.home_device_locked),
                hue,
                frame,
            )
        }
        AccountRow(stringResource(R.string.home_term_licence), frame) {
            TermValue(licenceTerm(state.entitlement), licenceHue(state), frame, stringResource(R.string.home_licence_ends))
        }
        AccountRow(stringResource(R.string.home_term_provider), frame) {
            TermValue(
                providerTerm(state.subscription),
                when (providerTone(state.subscription)) {
                    ProviderTone.Usable -> colors.success
                    ProviderTone.Refused -> colors.danger
                    ProviderTone.Unchecked -> colors.onBackgroundMuted
                },
                frame,
                stringResource(R.string.home_subscription_ends),
            )
        }
    }
}

/** A value the system's long press can select and copy, where [enabled]; as it is, elsewhere. */
@Composable
private fun Copyable(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) SelectionContainer(content = content) else content()
}

/** One row of the account: its name at the start, its value at the end. */
@Composable
private fun AccountRow(label: String, frame: CastivioMetrics, value: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = frame.chipPad / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(frame.chipPad),
    ) {
        Text(
            label,
            style = castivioChipStyle(frame.fsChip * ACCOUNT_TEXT_SCALE),
            color = CastivioTheme.colors.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // A column of its own, the same width on every row (owner, 2026-11-26, design/mockups/home-account-2-ar.png): the
            // values then start on one line beside it, and the three marks stand one above the other.
            modifier = Modifier.fillMaxWidth(ACCOUNT_LABEL_SHARE),
        )
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(frame.chipPad / 2),
            content = value,
        )
    }
}

/**
 * The licence's colour: the success green while it holds, the amber in its last [LICENCE_SOON_DAYS] days so the viewer
 * renews before it stops, and the failure red once it does not hold.
 */
@Composable
private fun licenceHue(state: HomeState): Color {
    val colors = CastivioTheme.colors
    val days = when (val e = state.entitlement) {
        is EntitlementState.TrialActive -> e.daysRemaining
        is EntitlementState.AnnualActive -> e.daysRemaining
        else -> null
    }
    return when {
        !state.licenceHolds -> colors.danger
        days != null && days <= LICENCE_SOON_DAYS -> colors.hueAmber
        else -> colors.success
    }
}

/** A term's value: its mark and its word in its colour, then "ends on" and its date when it has one. */
@Composable
private fun RowScope.TermValue(term: Term, hue: Color, frame: CastivioMetrics, ends: String) {
    StateMark(if (hue == CastivioTheme.colors.danger) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle, hue)
    val at = term.atMs
    if (at != null) {
        // The state beside its mark, then when it ends (owner, 2026-11-26): "✓ trial · ends on 26-11-2026", so the word that
        // says what the date is about is next to the mark that colours it, and not at the far end of the line.
        Text(
            stringResource(term.word),
            style = castivioChipStyle(frame.fsChip * ACCOUNT_TEXT_SCALE),
            color = hue,
            maxLines = 1,
        )
        Text(
            "· $ends",
            style = castivioChipStyle(frame.fsChip * FACT_LABEL_RATIO * ACCOUNT_TEXT_SCALE),
            color = CastivioTheme.colors.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FactValue(rememberDate(at), CastivioTheme.colors.onBackgroundStrong, frame)
    } else {
        FactValue(stringResource(term.word), hue, frame)
    }
}

@Composable
private fun FactValue(
    text: String,
    color: Color,
    frame: CastivioMetrics,
    /**
     * Left to right by the style rather than by isolating marks round the text: what is copied from it is then the
     * value alone, with no invisible characters to break the form it is pasted into.
     */
    ltr: Boolean = false,
) {
    val style = castivioTitleStyle(frame.fsLabel * FACT_VALUE_RATIO * ACCOUNT_TEXT_SCALE)
    Text(
        text,
        style = if (ltr) style.copy(textDirection = TextDirection.Ltr) else style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StateMark(icon: ImageVector, hue: Color) {
    Icon(icon, contentDescription = null, tint = hue, modifier = Modifier.size(Sizing.iconMd))
}

/**
 * The mark each trouble is said with: the server out of reach, the device off the
 * network, or the circled exclamation every refusal on Home carries.
 */
private val SubscriptionTrouble.icon: ImageVector
    get() = when (this) {
        SubscriptionTrouble.Unreachable -> Icons.Rounded.CloudOff
        SubscriptionTrouble.Offline -> Icons.Rounded.WifiOff
        SubscriptionTrouble.Expired, SubscriptionTrouble.Refused -> Icons.Rounded.ErrorOutline
    }


/** A term's word against the frame's chip text, and a value against the label step. */
private const val FACT_LABEL_RATIO = 0.85f

/** The account's words, larger than the footer's they came from (owner, 2026-11-25: «الكتابة صغيرة»). */
private const val ACCOUNT_TEXT_SCALE = 1.15f

/** The account panel's width, of a dialog's usual share. */
private const val ACCOUNT_WIDTH_SHARE = 0.9f

/** The account's names, a column of this share of a row; the values start beside it. */
private const val ACCOUNT_LABEL_SHARE = 0.34f
private const val FACT_VALUE_RATIO = 0.8f

/** The licence turns amber in its last week. */
private const val LICENCE_SOON_DAYS = 7

// ----------------------------------------------------------------- utilities

/**
 * An expiry date, as figures, the same shape in every language.
 *
 * ## Why this is not the reader's own date format
 *
 * It was `DateFormat.MEDIUM`, which is the right default nearly everywhere and the
 * wrong one here. Medium is a *worded* format in most locales — "8 abr 2027" in
 * Spanish, "8 avr. 2027" in French, "8. Apr. 2027" in German — and the term cards it
 * feeds are two of the narrowest surfaces in the application. On a device the Spanish
 * card read "Expires on 8 abr 2…": the month name spent the width the year needed,
 * and the year is the part a reader is actually checking.
 *
 * ## Where the shape is decided now
 *
 * In `CastivioDates`, and not here. This screen held one of five patterns scattered
 * across the application that each produced a different date for the same instant;
 * they are one pattern now, and the reasoning about digits, calendars and why the
 * reader's locale is not consulted lives beside it.
 *
 * The sentence around it still runs in the reader's direction; only this token is
 * fixed. [ltrToken] is what keeps it that way — an unisolated `8-4-2027` inside an
 * Arabic paragraph can be reordered by the bidirectional algorithm, which is a wrong
 * date rather than an ugly one.
 *
 * `remember` keyed on the value, because building a formatter and running it is real
 * work and composition runs whenever anything on this screen moves.
 */
@Composable
internal fun rememberDate(atMs: Long): String = remember(atMs) {
    ltrToken(CastivioDates.date(atMs))
}

/**
 * How the board's width is divided, as in the owner's second picture (2026-11-24): live the largest, films and series in the
 * middle, the radio a narrow column. Weights rather than widths.
 */
/**
 * How wide the playlist menu is allowed to be.
 *
 * Fixed dp rather than a share of the frame, and deliberately: the menu is sized by
 * its *contents*, which are names the user typed and this screen cannot measure in
 * advance. Material 3's menu already draws as wide as its widest item, and these two
 * bound that answer — a floor so a menu of one-word names is still a comfortable
 * target on a remote, and a ceiling so a provider's forty-character marketing name
 * cannot grow the menu across the board behind it.
 *
 * The floor is above the rail card it hangs off (108dp on the reference handset) on
 * purpose. A menu the width of its anchor would truncate names the anchor never had
 * to hold, and a popup is the one thing on this screen that may be wider than the
 * thing that opened it without costing the layout anything.
 */
private val PICKER_MIN_WIDTH = 180.dp
private val PICKER_MAX_WIDTH = 320.dp

private const val STACK_SHARE = 0.40f
private const val HERO_SHARE = 0.47f
private const val RADIO_SHARE = 0.15f

/** The section pictures' opacity over the card's ground: 0.5 tried, then lighter at the owner's word, 2026-11-25. */
private const val ART_ALPHA = 0.4f

/** How far "not downloaded yet" is lifted from the primary toward the strong ink. */
private const val NOT_FETCHED_LIFT = 0.3f

/** Where the live card's fade starts, of its height, and how dark it is at the foot. */
private const val HERO_SCRIM_FROM = 0.42f
private const val HERO_SCRIM_ALPHA = 0.72f

/** The same picture behind a poster of what was watched last: lighter, so the two do not crowd (owner, 2026-11-25). */
private const val ART_ALPHA_BEHIND_POSTER = 0.3f



/**
 * The watermark: how much of the hero panel's height its glyph takes, and how far it
 * is faded.
 *
 * Just under two thirds of the height and centred, so the panel has a subject rather
 * than a corner with something in it, and the glyph still ends above the words at the
 * floor.
 *
 * **Eighty per cent, and it was eighteen.** At eighteen it was a watermark, and on a television in a lit room it
 * was not seen at all: a photograph of a showroom screen showed the largest object on the page as an empty grey
 * rectangle with a smudge in it, which the room's reflections then filled in. The drawing the owner approved
 * (`design/mockups/home-vivid-ar.html`) has the glyph as the panel's subject, lit, with the count under it; the
 * count is still the larger figure and in the hue's full ink, so the glyph is the picture and the count is the fact.
 */
private const val HERO_GLYPH_SHARE = 0.6f
private const val HERO_GLYPH_ALPHA = 1f

/**
 * How much larger the hero's count is than the screen's title step.
 *
 * The one figure a returning viewer checks is the size of their catalogue, and it is
 * the largest thing on the board on purpose. Expressed as a ratio of `fsTitle` rather
 * than a size of its own, so it rides the frame's own type scale up and down instead
 * of being a second, independent opinion about how big text should be — the same
 * device [WORD_RATIO] uses for the wordmark.
 */
private const val HERO_COUNT_RATIO = 1.26f

/** A load's hairline: a little heavier on the panel than on a line, as in the drawing. */
private val HERO_BAR = 4.dp
private val LINE_BAR = 3.dp

/** A bar's whole, in percent. */
private const val FULL = 100

/** The travelling segment: its share of the track, one crossing's time, and where it rests with motion off. */
private const val SEGMENT_SHARE = 0.36f
private const val SEGMENT_MS = 1_400
private const val SEGMENT_REST = 0.375f

/** The success green a just-loaded card wears: on its edge, and fading down its glass. */
private const val ARRIVED_EDGE_ALPHA = 0.55f

/**
 * What the surfaces of Home are drawn with -- the drawing is `design/mockups/home-dark-ar.html`.
 *
 * Every card is the design system's `homeCardSurface` and `cardSurfaceEdge` -- the indigo, in `:core:design`, at the calmer weight
 * Home has (the edge is the one every other card in the application has). A line's figure is [LINE_FIGURE_RATIO] times the size of its words. No card has a
 * glow or a shadow of its own -- the owner did not want one.
 *
 * The live channels' glyph is centred at [HERO_GLYPH_CENTRE] of the panel and [HERO_GLYPH_SHARE] of its height, and the
 * panel's lines are drawn from it: its frame, which is [HERO_BODY_OF_GLYPH] of the glyph's box, echoed at the scales and the inks
 * of [HERO_ECHOES] at a ratio of [HERO_FRAME_RATIO] and a corner of [HERO_ECHO_RADIUS], and the ribbons of [HERO_RIBBONS] -- each
 * the fractions of the panel its two curves pass through, and its ink -- brightest at [HERO_RIBBON_PEAK] of their length.
 */
private const val LINE_FIGURE_RATIO = 1.5f
internal const val HERO_GLYPH_CENTRE = 0.38f
private const val HERO_BODY_OF_GLYPH = 0.83f
private const val HERO_FRAME_RATIO = 0.653f
private const val HERO_ECHO_RADIUS = 0.113f
private const val HERO_LINE_DP = 1.2f
private const val HERO_RIBBON_DP = 1.4f
private const val HERO_RIBBON_PEAK = 0.45f

/** The echoes of the glyph's frame: how many times its width, and the ink each is drawn at. */
private val HERO_ECHOES = listOf(1.32f to 0.34f, 1.71f to 0.22f, 2.15f to 0.13f)

/**
 * The ribbons: for each, the fractions of the panel's width and height of its start, the two controls and the end of its first
 * curve, then the two controls and the end of its second, and last its ink.
 */
private val HERO_RIBBONS = listOf(
    floatArrayOf(0.0f, 0.786f, 0.171f, 0.616f, 0.349f, 0.92f, 0.555f, 0.714f, 0.76f, 0.509f, 0.872f, 0.598f, 1.0f, 0.688f, 0.75f),
    floatArrayOf(0.0f, 0.857f, 0.187f, 0.688f, 0.368f, 0.991f, 0.579f, 0.786f, 0.791f, 0.58f, 0.885f, 0.67f, 1.0f, 0.759f, 0.6f),
    floatArrayOf(0.0f, 0.714f, 0.162f, 0.545f, 0.336f, 0.848f, 0.536f, 0.643f, 0.735f, 0.438f, 0.86f, 0.527f, 1.0f, 0.616f, 0.5f),
    floatArrayOf(0.0f, 0.929f, 0.199f, 0.768f, 0.386f, 1.054f, 0.598f, 0.857f, 0.81f, 0.661f, 0.897f, 0.741f, 1.0f, 0.83f, 0.42f),
    floatArrayOf(0.0f, 0.643f, 0.156f, 0.482f, 0.324f, 0.777f, 0.517f, 0.58f, 0.71f, 0.384f, 0.847f, 0.464f, 1.0f, 0.554f, 0.34f),
)

private const val ARRIVED_FILL_TOP = 0.13f
private const val ARRIVED_FILL_BOTTOM = 0.04f

/**
 * How many lines the disclaimer may use.
 *
 * Two, not one. It shared a line with the device facts and was ellipsised mid-clause,
 * which is the failure this fixes: a legal sentence that stops early has not been
 * shown. One line is enough for it in English and Arabic at the frames Castivio
 * targets; the second exists so a longer translation wraps rather than gets cut.
 */
private const val DISCLAIMER_LINES = 2

/** The continue row's height, the label over its cards, against the frame's chip. */
private const val CONTINUE_OF_CHIP = 2.3f



/** The wordmark's share of the frame's title step, as the licence screen sets it. */
private const val WORD_RATIO = 0.8f
