package com.castivio.feature.settings

/**
 * Where Settings sends a viewer for help, the privacy policy and the terms: Castivio's site and its support address.
 *
 * Pages of the site rather than text in the application, by the owner's decision (2026-11-18): the telephone, the hours and
 * the legal texts change on the site without an update, and the store asks for the privacy address in any case. A
 * television opens no link, so each is shown there as a QR code a phone scans, with the address written out; a phone opens
 * them in its browser and its mail application.
 */
internal object SettingsLinks {
    const val SITE = "castivio.app"
    const val SUPPORT = "https://castivio.app/support"
    const val PRIVACY = "https://castivio.app/privacy"
    const val TERMS = "https://castivio.app/terms"
    const val EMAIL = "support@castivio.app"

    /** What a mail application is opened with. */
    const val MAIL = "mailto:$EMAIL"

    /** An address as it is written on screen: without its scheme, which nobody types. */
    fun shown(url: String): String = url.removePrefix("https://").removePrefix("http://")
}

/** A page of the site that Settings shows: its address, and whether it is the support page with the e-mail beside it. */
internal enum class SitePage(val url: String) {
    SUPPORT(SettingsLinks.SUPPORT),
    PRIVACY(SettingsLinks.PRIVACY),
    TERMS(SettingsLinks.TERMS),
}
