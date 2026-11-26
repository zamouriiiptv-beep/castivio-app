package com.castivio.feature.licence

/**
 * Handles for the elements a test has to find by something other than its text.
 *
 * The sibling screen's entire middle band once collapsed to zero height on a device
 * while every gate stayed green — the code compiled, the strings resolved, the mockup
 * measured. Nothing was asserting that the composition Compose actually places has the
 * shape the design approved. A tag on each band is what makes "this band is 0dp tall"
 * expressible.
 */
internal object LicenceTags {
    /** The whole screen, for diagnosis: which band took the space. */
    const val STAGE = "licence.stage"
    const val HEADER = "licence.header"

    /** The header's buttons. */
    const val REFRESH = "licence.refresh"
    const val SUPPORT = "licence.support"
    const val LANGUAGE = "licence.language"
    const val EXIT = "licence.exit"

    /** The badge, the title and the subtitle above the cards. */
    const val HERO = "licence.hero"
    const val BADGE = "licence.badge"

    /** The subtitle: reserved height whether or not it has anything to say. */
    const val STATUS = "licence.status"

    /** The row of cards, and the thing whose height everything else depends on. */
    const val FIELD = "licence.field"

    /**
     * The plan pair, and each card in it by plan.
     *
     * The pair is tagged as well as the cards because "two cards are present" and
     * "the place they are in has a height" are different claims.
     */
    const val PLANS = "licence.plans"
    fun plan(id: String): String = "licence.plan.$id"

    /** The card that stands where the plans would, in the states that offer none. */
    const val STATUS_CARD = "licence.statusCard"

    /** The one action the blocked states offer instead of a purchase. */
    const val ACTION = "licence.action"

    /** The expiry date, which exists for one state and must not for the rest. */
    const val EXPIRY = "licence.expiry"

    /** The activation card and the code on it. */
    const val CODE_ZONE = "licence.codeZone"
    const val QR = "licence.qr"

    /** The line of identifiers under the cards, and each of the two. */
    const val IDS = "licence.ids"
    const val MAC = "licence.mac"
    const val KEY = "licence.key"
}
