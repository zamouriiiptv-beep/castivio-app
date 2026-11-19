package com.castivio.data.networking

import com.castivio.core.common.AppDispatchers
import com.castivio.core.common.AppError
import com.castivio.core.common.Outcome
import com.castivio.core.common.config.LicenceKey
import com.castivio.core.common.config.PortalConfig
import com.castivio.data.parsing.LicenceCodec
import com.castivio.domain.entitlement.Attestation
import com.castivio.domain.entitlement.EntitlementSource
import com.castivio.domain.entitlement.RedemptionRequest
import com.castivio.domain.entitlement.VerificationRequest
import com.castivio.domain.identity.DeviceIdentity
import com.castivio.domain.identity.MacAddress
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Castivio's licence server, over HTTPS: `POST /api/v1/device/licence` on the portal.
 *
 * The device proves itself with its address **and key**, in the body, as it does for its mailbox; the portal
 * refuses a wrong key and counts it. That key is the device secret `RELEASE_CHECKLIST.md` §1b asks for: an
 * address alone reads nothing.
 *
 * The answer is believed only when its signature verifies against [LicenceKey], over the payload's exact bytes,
 * and only when it is about this device. Anything else -- a refusal, a gateway's page, a payload signed by
 * someone else, a statement about another address -- is a failure, and a failure is silence: the repository
 * keeps what it had (rule 1 of `DefaultEntitlementRepository`). Nothing here can take a licence away by not
 * answering; only a signed statement with a revocation in it can.
 */
internal class HttpEntitlementSource(
    private val http: OkHttpClient,
    private val config: PortalConfig,
    key: LicenceKey,
    private val identity: DeviceIdentity,
    private val dispatchers: AppDispatchers,
    private val device: PortalDevice = PortalDevice(),
) : EntitlementSource {

    private val publicKey: PublicKey = KeyFactory.getInstance("EC")
        .generatePublic(X509EncodedKeySpec(LicenceCodec.base64Url(key.spkiBase64)))

    override suspend fun verify(request: VerificationRequest): Outcome<Attestation> = withContext(dispatchers.io) {
        val deviceKey = identity.key().value
        val body = LicenceCodec.requestBody(
            mac = request.macAddress.value,
            key = deviceKey,
            appVersion = device.appVersion,
            platform = device.platform,
            modelName = device.modelName,
        )
        try {
            val call = Request.Builder()
                .url(config.api("device/licence"))
                .header("Accept", "application/json")
                .post(body.toRequestBody(JSON))
                .build()
            http.newCall(call).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val reader = response.body?.charStream() ?: return@use Outcome.Failure(AppError.SERVER_ERROR)
                        read(reader, request)
                    }
                    response.code == 403 -> Outcome.Failure(AppError.UNAUTHORIZED)
                    response.code == 429 || response.code in 500..599 -> Outcome.Failure(AppError.SERVER_ERROR)
                    else -> Outcome.Failure(AppError.UNKNOWN)
                }
            }
        } catch (timeout: InterruptedIOException) {
            Outcome.Failure(AppError.TIMEOUT, timeout)
        } catch (unreachable: IOException) {
            Outcome.Failure(AppError.NETWORK_UNAVAILABLE, unreachable)
        }
    }

    /**
     * There is no purchase receipt or recovery code to present to this server: a licence is sold in the owner's
     * or a reseller's panel against the device's address, and reaches the device through [verify]. Answering
     * "nothing to redeem against" is the truth, and it changes nothing on the device.
     */
    override suspend fun redeem(request: RedemptionRequest): Outcome<Attestation> =
        Outcome.Failure(AppError.NOT_CONFIGURED)

    private fun read(reader: java.io.Reader, request: VerificationRequest): Outcome<Attestation> = try {
        val envelope = LicenceCodec.envelope(reader)
        val payload = LicenceCodec.base64Url(envelope.payload)
        if (!signedByTheServer(payload, LicenceCodec.base64Url(envelope.signature))) {
            Outcome.Failure(AppError.SERVER_ERROR)
        } else {
            val statement = LicenceCodec.statement(String(payload, Charsets.UTF_8))
            if (MacAddress.parse(statement.mac) != request.macAddress) {
                // A true statement about another device is still not this device's licence.
                Outcome.Failure(AppError.SERVER_ERROR)
            } else {
                Outcome.Success(
                    Attestation(
                        record = LicenceCodec.record(statement, request.macAddress, request.identityVersion),
                        serverTimeMs = statement.serverTimeMs,
                        signature = envelope.signature,
                    ),
                )
            }
        }
    } catch (unreadable: RuntimeException) {
        // Not the server's envelope, or a statement this build cannot read whole: nothing is believed.
        Outcome.Failure(AppError.SERVER_ERROR, unreadable)
    }

    private fun signedByTheServer(payload: ByteArray, signature: ByteArray): Boolean = try {
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(payload)
            verify(signature)
        }
    } catch (malformed: java.security.SignatureException) {
        false
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
