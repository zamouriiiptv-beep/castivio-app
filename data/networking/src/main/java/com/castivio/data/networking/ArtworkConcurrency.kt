package com.castivio.data.networking

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import okhttp3.Dispatcher
import java.util.concurrent.atomic.AtomicBoolean

/**
 * How many of the pictures' requests may be on their way at once -- and the few they are held to while a section of films arrives.
 *
 * ## What is changed, and what is not
 *
 * The image loader has a dispatcher of its own ([ArtworkCalls.deriveClient]), with limits of its own and the application's threads.
 * This takes that dispatcher and changes its two limits, `maxRequests` and `maxRequestsPerHost`, while a films import runs. Nothing
 * else: not the application's dispatcher, which is the API's, not the client's settings, and not the threads -- no pool is made.
 *
 * OkHttp's rule for a limit that changes is the whole of how it behaves. A limit that falls does nothing to a call that is running,
 * which finishes as it was going to; it only keeps a call that has not started from starting until the running ones are fewer than
 * the limit. A limit that rises lets the calls that were waiting start, up to it.
 *
 * ## Holds, and why they are counted
 *
 * A films import takes a [hold] when it begins and gives it back when it ends. The lower limits are in force while **any** hold is
 * open, and the normal ones return only when the last is given back. Counted, because two imports can overlap: a load that is
 * cancelled because the subscription changed ends a little after the load for the new one has begun, and the one that ends first
 * must not give the pictures their places back under the one that has not. A hold is given back once however many times it is
 * closed, so a `finally` and a completion handler that both close it cannot give back a hold that belongs to another import.
 *
 * ## When the dispatcher arrives
 *
 * The image loader is built the first time a picture is asked for, and the films' import can begin before that. The holds are
 * counted from the start, and [attach] sets a dispatcher that arrives later to the limits that are in force when it does.
 *
 * Built once for the process: the loader that owns the dispatcher and the importer that holds are built apart and share only this.
 */
class ArtworkConcurrency(
    private val normal: Limits,
    private val whileFilmsArrive: Limits,
) {

    /** A dispatcher's two limits: how many calls at once in all, and to any one host, which can be no more than in all. */
    data class Limits(val maxRequests: Int, val maxRequestsPerHost: Int) {
        init {
            require(maxRequests >= 1) { "maxRequests is $maxRequests: a dispatcher that allows none would never start a call" }
            require(maxRequestsPerHost in 1..maxRequests) { "maxRequestsPerHost is $maxRequestsPerHost: not within 1..$maxRequests" }
        }

        internal fun applyTo(dispatcher: Dispatcher) {
            // Each setter lets the calls that are waiting start when its limit rose, and leaves the ones running alone when it fell.
            dispatcher.maxRequests = maxRequests
            dispatcher.maxRequestsPerHost = maxRequestsPerHost
        }
    }

    private val lock = Any()
    private var openHolds = 0
    private var dispatcher: Dispatcher? = null

    /** The limits in force now: the lower ones while a hold is open, the normal ones otherwise. */
    val limits: Limits get() = synchronized(lock) { current() }

    /** How many holds are open now. */
    val holds: Int get() = synchronized(lock) { openHolds }

    /**
     * Puts the pictures' dispatcher under this, and sets it to the limits in force at once: the lower ones when a hold is open.
     *
     * One dispatcher is governed at a time. A second one replaces the first, which is given the normal limits back, so that no
     * dispatcher is ever left held with nothing to give it back.
     */
    fun attach(dispatcher: Dispatcher) {
        synchronized(lock) {
            val previous = this.dispatcher
            if (previous != null && previous !== dispatcher) normal.applyTo(previous)
            this.dispatcher = dispatcher
            current().applyTo(dispatcher)
        }
    }

    /**
     * A section of films has begun to arrive: the pictures are held to the lower limits until the returned hold is closed.
     *
     * Closing it is what gives them back, and it may be closed more than once: only the first close counts.
     */
    fun hold(): AutoCloseable {
        synchronized(lock) {
            openHolds += 1
            dispatcher?.let { whileFilmsArrive.applyTo(it) }
        }
        val closed = AtomicBoolean(false)
        return object : AutoCloseable {
            override fun close() {
                if (closed.compareAndSet(false, true)) giveBack()
            }
        }
    }

    /**
     * [upstream], with a hold open for as long as it is being collected: taken as the collection begins -- which is as the import
     * begins -- and given back as it ends. A flow ends in three ways, it completes or it fails or it is cancelled, and the hold is
     * given back in each, by the `finally`. A flow nobody collects holds nothing.
     */
    fun <T> whileCollecting(upstream: Flow<T>): Flow<T> = flow {
        val taken = hold()
        try {
            emitAll(upstream)
        } finally {
            taken.close()
        }
    }

    private fun giveBack() {
        synchronized(lock) {
            if (openHolds > 0) openHolds -= 1
            if (openHolds == 0) dispatcher?.let { normal.applyTo(it) }
        }
    }

    /** Called holding [lock]. */
    private fun current(): Limits = if (openHolds > 0) whileFilmsArrive else normal
}
