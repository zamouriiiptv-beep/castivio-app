package com.castivio.core.platform

/**
 * Whether this device is on a network at all.
 *
 * Asked once a provider has failed to answer, to tell two causes apart that need opposite
 * advice: a server that does not answer is the provider's to put right, and a device with
 * no connection is the viewer's. Telling somebody to call their provider while their own
 * router is off sends them to the wrong person.
 */
interface NetworkState {

    /**
     * False only when the platform says there is no connection: no active network, or one
     * that cannot reach the internet. Anything it cannot tell reads as online, so the
     * viewer's own network is never blamed on a guess.
     */
    fun isOnline(): Boolean

    /**
     * True on a network that costs nothing to use -- Wi-Fi, a cable -- and false on one the platform calls metered, such as
     * a phone's data, where nothing is downloaded that the viewer did not ask for. Anything it cannot tell reads as
     * unmetered: a television box on a cable is the common case.
     */
    fun isUnmetered(): Boolean = true
}
