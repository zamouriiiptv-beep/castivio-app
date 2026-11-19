package com.castivio.data.entitlement.di

import android.content.Context
import android.content.SharedPreferences
import com.castivio.core.common.AppDispatchers
import com.castivio.data.entitlement.AesGcmVault
import com.castivio.data.entitlement.BuildConfig
import com.castivio.data.entitlement.DefaultEntitlementRepository
import com.castivio.data.entitlement.LocalEntitlementSource
import com.castivio.data.entitlement.SealedClockStore
import com.castivio.data.entitlement.SealedEntitlementStore
import com.castivio.data.entitlement.SealedStore
import com.castivio.data.entitlement.VaultKeys
import com.castivio.domain.entitlement.EntitlementRepository
import com.castivio.domain.entitlement.EntitlementSource
import com.castivio.domain.entitlement.EntitlementStore
import com.castivio.domain.entitlement.Licensing
import com.castivio.domain.entitlement.PricingConfig
import com.castivio.domain.entitlement.PricingDefaults
import com.castivio.domain.identity.DeviceIdentity
import com.castivio.domain.time.ClockSignalSource
import com.castivio.domain.time.ClockStore
import com.castivio.domain.time.MonotonicClock
import com.castivio.domain.time.TrustedTime
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The licence, wired.
 *
 * [EntitlementSource] — Castivio's licence server, the only thing that can establish a
 * purchase or withdraw one — is the portal's `device/licence`, implemented and bound in
 * `:data:networking` (`HttpEntitlementSource`, `PortalModule`). Binding it was the single
 * change this module needed: no domain type changed, and every rule that governs what the
 * server's answer means was already written and tested.
 */
@Module
@InstallIn(SingletonComponent::class)
object EntitlementModule {

    @Provides
    @Singleton
    @Sealed
    fun sealedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(SealedStore.FILE, Context.MODE_PRIVATE)

    /**
     * The prices, the trial length and the grace, in one object nothing else may invent.
     *
     * Injected rather than read from [PricingDefaults] at the point of use so that a
     * future remote configuration is a change to this method — the policy already takes
     * the configuration as an argument and has never known a default.
     */
    @Provides
    @Singleton
    fun pricing(): PricingConfig = PricingDefaults.config

    @Provides
    @Singleton
    fun entitlementStore(
        @Sealed prefs: SharedPreferences,
        dispatchers: AppDispatchers,
    ): EntitlementStore = SealedEntitlementStore(sealed(prefs), dispatchers)

    @Provides
    @Singleton
    fun clockStore(@Sealed prefs: SharedPreferences): ClockStore = SealedClockStore(sealed(prefs))

    @Provides
    @Singleton
    fun trustedTime(signals: ClockSignalSource, store: ClockStore): TrustedTime =
        MonotonicClock(signals, store)

    /**
     * The one decision in this file that must never be got wrong.
     *
     * A debug build gets [Licensing.Development] and with it a local trial, so the app
     * can be installed on a phone and a television and used before the licence server
     * exists. A release build gets [Licensing.Production], which has **nowhere to put a
     * trial grantor** — the mistake is not guarded against, it is unrepresentable.
     *
     * `source` is Castivio's licence server in both (the portal's `device/licence`, bound in
     * `PortalModule`). A debug build still grants its local trial on a first launch with no
     * connection, and the server's signed answer replaces it as soon as it is had; a release
     * build establishes nothing on its own, the trial included.
     */
    @Provides
    @Singleton
    fun licensing(config: PricingConfig, source: EntitlementSource): Licensing =
        if (BuildConfig.DEBUG) {
            Licensing.Development(trials = LocalEntitlementSource(config), source = source)
        } else {
            Licensing.Production(source = source)
        }

    @Provides
    @Singleton
    fun entitlementRepository(
        store: EntitlementStore,
        identity: DeviceIdentity,
        clock: TrustedTime,
        config: PricingConfig,
        licensing: Licensing,
    ): EntitlementRepository = DefaultEntitlementRepository(
        store = store,
        identity = identity,
        clock = clock,
        config = config,
        licensing = licensing,
    )

    /**
     * Built at the point of use rather than bound, so the sealing stays an
     * implementation detail of this module rather than something anywhere else in the
     * app could ask the graph for.
     *
     * Two of them exist — one for the record, one for the clock — reading the same file
     * with the same key. Each resolves that key once and holds it, so the cost is two
     * keystore lookups in the life of the process rather than two per read.
     */
    private fun sealed(prefs: SharedPreferences) =
        SealedStore(prefs, AesGcmVault { VaultKeys.sealingKey(prefs) })
}
