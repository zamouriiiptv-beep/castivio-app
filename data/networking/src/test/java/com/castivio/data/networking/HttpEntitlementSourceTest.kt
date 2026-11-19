package com.castivio.data.networking

import com.castivio.core.common.AppDispatchers
import com.castivio.core.common.AppError
import com.castivio.core.common.Outcome
import com.castivio.core.common.config.LicenceKey
import com.castivio.core.common.config.PortalConfig
import com.castivio.domain.entitlement.Plan
import com.castivio.domain.entitlement.VerificationRequest
import com.castivio.domain.identity.DeviceIdentity
import com.castivio.domain.identity.DeviceIdentityRecord
import com.castivio.domain.identity.DeviceKey
import com.castivio.domain.identity.IdentityProvenance
import com.castivio.domain.identity.MacAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * The licence server over a real socket, signed with a key made for the test.
 *
 * What matters most is held in its own tests: a statement is believed only when its signature verifies
 * over the bytes that were sent, and only when it is about this device; and every way of not getting
 * one is a failure, which the repository treats as silence.
 */
class HttpEntitlementSourceTest {

    private val mac = MacAddress.parse("AA:BB:CC:00:11:22")!!
    private val serverKeys: KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val strangerKeys: KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

    private lateinit var server: MockWebServer

    private class Identity : DeviceIdentity {
        override fun current() = DeviceIdentityRecord(MacAddress.parse("AA:BB:CC:00:11:22")!!, 2, IdentityProvenance.DEVICE)
        override fun legacy(): List<DeviceIdentityRecord> = emptyList()
        override fun key() = DeviceKey("482731")
    }

    private object Inline : AppDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun source() = HttpEntitlementSource(
        OkHttpClient(),
        PortalConfig.of(server.url("/").toString()),
        LicenceKey.of(Base64.getEncoder().encodeToString(serverKeys.public.encoded)),
        Identity(),
        Inline,
        PortalDevice(modelName = "SHIELD", platform = "Android TV", appVersion = "1.0.4"),
    )

    private val request = VerificationRequest(macAddress = mac, identityVersion = 2, provenance = IdentityProvenance.DEVICE)

    private fun b64url(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun signed(statement: String, keys: KeyPair = serverKeys, sentPayload: String = statement): MockResponse {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keys.private)
            update(statement.toByteArray())
            sign()
        }
        return MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
            .setBody("""{"payload":"${b64url(sentPayload.toByteArray())}","signature":"${b64url(signature)}"}""")
    }

    private fun statement(mac: String = "AA:BB:CC:00:11:22", plan: String = "annual") =
        """{"v":1,"mac":"$mac","plan":"$plan","trialStartedAt":1000,"trialExpiresAt":605801000,""" +
            """"expiresAt":40000000000,"revokedAt":null,"establishedAt":1000,"serverTime":2000000}"""

    @Test
    fun `a statement signed by the server is this device's licence, at the server's time`() = runBlocking {
        server.enqueue(signed(statement()))

        val answer = source().verify(request)

        assertTrue(answer is Outcome.Success)
        val attestation = (answer as Outcome.Success).value
        assertEquals(Plan.ANNUAL, attestation.record.plan)
        assertEquals(40_000_000_000L, attestation.record.subscriptionExpiresAtMs)
        assertEquals(2_000_000L, attestation.serverTimeMs)
        assertEquals(mac, attestation.record.macAddress)
    }

    @Test
    fun `the key travels in the body, with the version the statistics count, and never in the address`() = runBlocking {
        server.enqueue(signed(statement()))
        source().verify(request)

        val sent = server.takeRequest()
        assertEquals("/api/v1/device/licence", sent.path)
        assertFalse(sent.path!!.contains("482731"))
        val body = sent.body.readUtf8()
        assertTrue(body.contains("\"deviceKey\":\"482731\""))
        assertTrue(body.contains("\"appVersion\":\"1.0.4\""))
        assertTrue(body.contains("\"platform\":\"Android TV\""))
    }

    @Test
    fun `a payload changed after it was signed is not believed`() = runBlocking {
        server.enqueue(signed(statement(plan = "trial"), sentPayload = statement(plan = "lifetime")))
        assertEquals(AppError.SERVER_ERROR, (source().verify(request) as Outcome.Failure).error)
    }

    @Test
    fun `a statement signed by anyone else is not believed`() = runBlocking {
        server.enqueue(signed(statement(plan = "lifetime"), keys = strangerKeys))
        assertEquals(AppError.SERVER_ERROR, (source().verify(request) as Outcome.Failure).error)
    }

    @Test
    fun `a true statement about another device is not this device's licence`() = runBlocking {
        server.enqueue(signed(statement(mac = "AA:BB:CC:00:11:99", plan = "lifetime")))
        assertEquals(AppError.SERVER_ERROR, (source().verify(request) as Outcome.Failure).error)
    }

    @Test
    fun `a refusal, an outage and a gateway's page are failures, each named`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"device_not_verified"}"""))
        assertEquals(AppError.UNAUTHORIZED, (source().verify(request) as Outcome.Failure).error)

        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"licence_unavailable"}"""))
        assertEquals(AppError.SERVER_ERROR, (source().verify(request) as Outcome.Failure).error)

        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>gateway</html>"))
        assertEquals(AppError.SERVER_ERROR, (source().verify(request) as Outcome.Failure).error)
    }

    @Test
    fun `the pinned production key is a key this build can read`() {
        // Constructing the source parses the key; a typo in it would stop every launch, so it is read here.
        HttpEntitlementSource(OkHttpClient(), PortalConfig.of(null), LicenceKey.of(null), Identity(), Inline)
    }
}
