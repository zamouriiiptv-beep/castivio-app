package com.castivio.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** The addresses as they are written on a screen, and the mail a phone opens. */
class SettingsLinksTest {

    @Test
    fun `an address is written without its scheme`() {
        assertEquals("castivio.app/support", SettingsLinks.shown(SettingsLinks.SUPPORT))
        assertEquals("castivio.app/terms", SettingsLinks.shown("http://castivio.app/terms"))
        assertEquals("castivio.app", SettingsLinks.shown(SettingsLinks.SITE))
    }

    @Test
    fun `the mail is the owner's support address`() {
        assertEquals("support@castivio.app", SettingsLinks.EMAIL)
        assertEquals("mailto:support@castivio.app", SettingsLinks.MAIL)
    }
}
