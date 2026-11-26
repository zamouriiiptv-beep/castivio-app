package com.castivio.feature.licence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.castivio.core.common.AppDispatchers
import com.castivio.core.common.Outcome
import com.castivio.core.common.config.ActivationDestination
import com.castivio.domain.entitlement.EntitlementRepository
import com.castivio.domain.entitlement.EntitlementState
import com.castivio.domain.entitlement.PlanOffer
import com.castivio.domain.entitlement.PricingDefaults
import com.castivio.domain.identity.DeviceIdentity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Everything the licence screen renders, as one value.
 *
 * `licence` is null only while the sealed record is being read — that is the
 * Loading state, and it is a fact about this screen rather than about the
 * entitlement, which is why it is a nullable field here and not a tenth case in
 * `EntitlementState`.
 */
internal data class LicenceUiState(
    val licence: EntitlementState? = null,
    val address: String? = null,
    val deviceKey: String? = null,
    val qr: android.graphics.Bitmap? = null,

    /** The plans on offer, from `PricingConfig` and never from a literal. */
    val plans: List<PlanOffer> = emptyList(),

    /** The plan whose portal is being opened, if any. Drives the Working state. */
    val opening: String? = null,

    /** A handoff or a refresh that did not work. Cleared by the next attempt. */
    val failed: Boolean = false,
) {
    /** The header's Refresh is asking the licence server, and turns while it does. */
    val refreshing: Boolean get() = opening == REFRESHING

    /** A plan's portal is being opened. */
    val handingOff: Boolean get() = opening != null && opening != REFRESHING
}

/** The busy state that is not a plan: [LicenceViewModel.refresh] is in flight. */
internal const val REFRESHING = "refresh"

/**
 * How often a screen waiting on a purchase asks the licence server again.
 *
 * The user pays on their phone and the television is meant to notice by itself —
 * "the TV activates automatically" is the third step the screen promises. Fifteen
 * seconds is the owner's figure (2026-11-26): a payment lands and the screen turns
 * within the time it takes to look back up at it, and a screen left open all evening
 * asks 240 times an hour, which a licence server answers without noticing.
 */
internal const val POLL_MS = 15_000L

/**
 * The licence screen's state holder.
 *
 * ## What it does not do
 *
 * It does not decide whether the device may be used, it does not count days, it
 * does not know what a trial costs and it never writes an entitlement. All four
 * belong to `EntitlementRepository` and the policy behind it, which holds the
 * sealed record and the high-water clock mark that stops a rolled-back device
 * buying a second free week. This subscribes and renders.
 *
 * It does not take money either. Castivio is portal-first on every platform: the
 * app presents the plans and hands the user to the portal, which owns
 * authentication, payment, licence creation and MAC binding. `redeem()` stays the
 * single integration point for whatever proof comes back, so adding Play Billing
 * later changes nothing above this line.
 */
@HiltViewModel
internal class LicenceViewModel @Inject constructor(
    private val entitlement: EntitlementRepository,
    private val identity: DeviceIdentity,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(
        // The plans are known before anything is read: they are configuration,
        // not state. `purchasable` filters out the trial and anything a region
        // does not sell, so the screen renders what the list says and never a
        // hardcoded pair.
        LicenceUiState(plans = PricingDefaults.config.purchasable),
    )
    val state: StateFlow<LicenceUiState> = _state.asStateFlow()

    init {
        // Collected rather than read once: the repository re-evaluates against
        // the clock, so a device left on this screen past midnight sees the trial
        // count fall instead of showing yesterday's number.
        viewModelScope.launch {
            entitlement.state.collect { licence ->
                _state.update { it.copy(licence = licence) }
            }
        }

        viewModelScope.launch {
            val resolved = withContext(dispatchers.io) {
                val record = identity.current()
                // The QR carries the bare portal address and nothing else -- no
                // MAC, no device key. See LicenceQrTest, which decodes it.
                Triple(
                    record.macAddress.value,
                    identity.key().value,
                    runCatching { licenceQrBitmap(QR_PIXELS) }.getOrNull(),
                )
            }
            _state.update {
                it.copy(
                    address = resolved.first,
                    qr = resolved.third,
                    // The same `DeviceIdentity` the address comes from, so this screen
                    // and the activation screen and Home cannot disagree about what
                    // this device's key is. It replaced a debug constant that was the
                    // same on every device and absent in release.
                    deviceKey = resolved.second,
                )
            }
        }
    }

    /**
     * The address to open for a plan, and the state change that goes with it.
     *
     * The screen performs the navigation — an `Intent` is `:app`'s and the
     * platform's business, not a view model's — so this returns the link and
     * records that a handoff is in flight.
     */
    fun portalFor(plan: PlanOffer): String {
        val id = plan.plan.name.lowercase()
        _state.update { it.copy(opening = id, failed = false) }

        // The portal is a browser away, and a user who never completes the
        // purchase must not be left staring at a spinner. The Working state
        // clears itself; returning to the screen re-reads the entitlement.
        openTimer?.cancel()
        openTimer = viewModelScope.launch {
            delay(HANDOFF_MS)
            _state.update { if (it.opening == id) it.copy(opening = null) else it }
        }
        return ActivationDestination.portalUrl(plan = id, macAddress = _state.value.address)
    }

    /**
     * Where a user goes when nothing on this screen can help them.
     *
     * Carries the address so support does not begin by asking somebody to read
     * six pairs of hex off a television. The two states that offer this are the
     * two whose message already says "contact support with your device key".
     */
    fun supportUrl(): String = ActivationDestination.supportUrl(_state.value.address)

    /** No browser, no portal, nothing happened. Say so rather than spinning. */
    fun handoffFailed() {
        openTimer?.cancel()
        _state.update { it.copy(opening = null, failed = true) }
    }

    /**
     * Back, while a handoff or a refresh is in flight: stop waiting for it.
     *
     * Not an error — nothing failed, the user changed their mind — so [failed]
     * stays false and the screen returns to exactly the state it left.
     */
    fun cancelHandoff() {
        openTimer?.cancel()
        _state.update { it.copy(opening = null) }
    }

    /** Back, on a failure notice: dismiss it and give the plans back. */
    fun dismissFailure() {
        _state.update { it.copy(failed = false) }
    }

    /** Ask the licence server again: the header's Refresh, and the Retry the unavailable state offers. */
    fun refresh() {
        if (_state.value.opening != null) return
        // Held in the same slot the handoff uses, so that `cancelHandoff` stops
        // whichever one is running. Two jobs and one busy flag would let back
        // clear the flag while the work carried on and set it again.
        openTimer = viewModelScope.launch {
            _state.update { it.copy(opening = REFRESHING, failed = false) }
            val outcome = entitlement.refresh()
            _state.update {
                it.copy(opening = null, failed = outcome.isFailure())
            }
        }
    }

    /**
     * Ask the licence server again, quietly: the screen's own fifteen-second check.
     *
     * Not [refresh]. Nothing turns, nothing is reported and a failure says nothing —
     * the user did not ask, so an unreachable server every fifteen seconds is not
     * news to put in front of them. A good answer arrives through
     * `EntitlementRepository.state`, which this screen already collects, so the
     * screen turns by itself. It stands aside while the user's own refresh or a
     * handoff is in flight, and while its own previous question is unanswered.
     */
    fun poll() {
        if (_state.value.opening != null || pollJob?.isActive == true) return
        pollJob = viewModelScope.launch { entitlement.refresh() }
    }

    private var openTimer: Job? = null
    private var pollJob: Job? = null

    private companion object {
        const val QR_PIXELS = 512

        /**
         * How long the handoff spinner lives before the screen gives the user
         * their controls back.
         *
         * Not a guess about how long paying takes -- the app is backgrounded for
         * that and re-reads the entitlement on return. This is how long the
         * *launch* is allowed to look like it is happening.
         */
        const val HANDOFF_MS = 4_000L
    }
}

/** `Outcome` has no `isFailure`; this keeps the call site readable. */
private fun <T> Outcome<T>.isFailure(): Boolean = this !is Outcome.Success
