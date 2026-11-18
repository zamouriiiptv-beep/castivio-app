package com.castivio.feature.settings

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Each help page's QR code reads back as its address, exactly, and as nothing else. */
@RunWith(RobolectricTestRunner::class)
class SettingsQrTest {

    private fun decode(url: String, pixels: Int = 440): String {
        val bitmap = siteQrBitmap(url, pixels)
        val buffer = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(buffer, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, buffer)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test
    fun `every page's code is its address and nothing more`() {
        for (page in SitePage.entries) {
            val read = decode(page.url)
            assertEquals(page.url, read)
            assertFalse("a query string reached the code of ${page.name}", read.contains("?"))
        }
    }

    @Test
    fun `the pages are the owner's site, over https`() {
        assertEquals("https://castivio.app/support", SitePage.SUPPORT.url)
        assertEquals("https://castivio.app/privacy", SitePage.PRIVACY.url)
        assertEquals("https://castivio.app/terms", SitePage.TERMS.url)
    }

    @Test
    fun `a code drawn small still reads`() {
        assertEquals(SitePage.PRIVACY.url, decode(SitePage.PRIVACY.url, pixels = 160))
    }
}
