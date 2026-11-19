package com.castivio.data.parsing

import com.castivio.domain.entitlement.Plan
import com.castivio.domain.identity.MacAddress
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.StringReader

/**
 * The licence server's statement, read whole or not at all.
 *
 * The examples are what the portal sends (its `test/licence-route-tests.ts` checks the same shapes from
 * the other side).
 */
class LicenceCodecTest {

    private val mac = MacAddress.parse("02:00:00:00:03:01")!!

    private fun statement(body: String) = LicenceCodec.statement(body)

    private fun refused(body: String) {
        try {
            statement(body)
            fail("read: $body")
        } catch (expected: LicenceCodec.Unreadable) {
            // the whole statement is refused
        }
    }

    @Test
    fun `a trial is read with its dates`() {
        val s = statement(
            """{"v":1,"mac":"02:00:00:00:03:01","plan":"trial","trialStartedAt":1000,"trialExpiresAt":605801000,""" +
                """"expiresAt":null,"revokedAt":null,"establishedAt":1000,"serverTime":5000}""",
        )
        assertEquals(Plan.TRIAL, s.plan)
        assertEquals(1000L, s.trialStartedAtMs)
        assertEquals(605_801_000L, s.trialExpiresAtMs)
        assertNull(s.expiresAtMs)
        assertEquals(5000L, s.serverTimeMs)

        val record = LicenceCodec.record(s, mac, identityVersion = 2)
        assertEquals(Plan.TRIAL, record.plan)
        assertEquals(605_801_000L, record.expiresAtMs)
        assertEquals(5000L, record.lastVerifiedAtMs)
        assertEquals(5000L, record.maxObservedTimeMs)
        assertEquals(2, record.identityVersion)
    }

    @Test
    fun `a year carries its end, lifetime none, and a revoke its date`() {
        val year = LicenceCodec.record(
            statement("""{"v":1,"mac":"02:00:00:00:03:01","plan":"annual","trialStartedAt":1,"trialExpiresAt":2,"expiresAt":900,"revokedAt":null,"establishedAt":1,"serverTime":10}"""),
            mac,
            1,
        )
        assertEquals(Plan.ANNUAL, year.plan)
        assertEquals(900L, year.subscriptionExpiresAtMs)

        val lifetime = LicenceCodec.record(
            statement("""{"v":1,"mac":"02:00:00:00:03:01","plan":"lifetime","trialStartedAt":1,"trialExpiresAt":2,"expiresAt":null,"revokedAt":77,"establishedAt":1,"serverTime":10}"""),
            mac,
            1,
        )
        assertEquals(Plan.LIFETIME, lifetime.plan)
        assertNull(lifetime.expiresAtMs)
        assertEquals(77L, lifetime.revokedAtMs)
    }

    @Test
    fun `nothing is guessed - a missing plan, a year without its end, a newer version are refused`() {
        refused("""{"v":1,"mac":"02:00:00:00:03:01","establishedAt":1,"serverTime":10}""")
        refused("""{"v":1,"mac":"02:00:00:00:03:01","plan":"annual","establishedAt":1,"serverTime":10}""")
        refused("""{"v":1,"mac":"02:00:00:00:03:01","plan":"trial","establishedAt":1,"serverTime":10}""")
        refused("""{"v":2,"mac":"02:00:00:00:03:01","plan":"lifetime","establishedAt":1,"serverTime":10}""")
        refused("""{"v":1,"mac":"02:00:00:00:03:01","plan":"forever","establishedAt":1,"serverTime":10}""")
        refused("""{"v":1,"mac":"02:00:00:00:03:01","plan":"lifetime","establishedAt":"soon","serverTime":10}""")
    }

    @Test
    fun `the envelope is read as it was sent, and an error page is not an envelope`() {
        val envelope = LicenceCodec.envelope(StringReader("""{"payload":"eyJ2IjoxfQ","signature":"MEUCIQ"}"""))
        assertEquals("eyJ2IjoxfQ", envelope.payload)
        assertEquals("""{"v":1}""", String(LicenceCodec.base64Url(envelope.payload)))
        try {
            LicenceCodec.envelope(StringReader("""{"error":"licence_unavailable"}"""))
            fail("an answer without a payload was read")
        } catch (expected: LicenceCodec.Unreadable) {
            // refused
        }
    }

    @Test
    fun `base64url decodes every byte value, with and without padding`() {
        val bytes = ByteArray(256) { it.toByte() }
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        assertArrayEquals(bytes, LicenceCodec.base64Url(encoded))
        assertArrayEquals(bytes, LicenceCodec.base64Url(java.util.Base64.getEncoder().encodeToString(bytes)))
    }

    @Test
    fun `the request carries the key in its body, and survives what can be typed`() {
        val body = LicenceCodec.requestBody("02:00:00:00:03:01", "123456", "1.0.4", "Android TV", "Box \"Pro\"")
        assertTrue(body.contains("\"deviceKey\":\"123456\""))
        assertTrue(body.contains("\"modelName\":\"Box \\\"Pro\\\"\""))
        assertEquals(
            """{"macAddress":"02:00:00:00:03:01","deviceKey":"1"}""",
            LicenceCodec.requestBody("02:00:00:00:03:01", "1", null, null, null),
        )
    }
}
