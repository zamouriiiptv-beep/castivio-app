package com.castivio.data.parsing

import com.castivio.domain.entitlement.EntitlementRecord
import com.castivio.domain.entitlement.Plan
import com.castivio.domain.identity.MacAddress
import java.io.Reader
import java.io.StringReader

/**
 * What the licence server says about a device, read from `POST /api/v1/device/licence`
 * (the portal's `docs/licence-api.md`).
 *
 * The answer is an envelope, `{"payload": base64url, "signature": base64url}`, around a statement:
 *
 * ```
 * {"v":1,"mac":"AA:BB:…","plan":"trial"|"annual"|"lifetime","trialStartedAt":…,"trialExpiresAt":…,
 *  "expiresAt":…|null,"revokedAt":…|null,"establishedAt":…,"serverTime":…}
 * ```
 *
 * This file reads both and turns the statement into an [EntitlementRecord]. It does **not** decide whether
 * to believe it: the signature over the payload's exact bytes is checked by the caller against the key the
 * app pins, before [statement] is ever asked — a parse of an unverified payload is a parse of anybody's text.
 *
 * Strict where the rest of this package is tolerant, on purpose. A provider's catalogue loses one element to
 * a bad field; a licence that cannot be read whole is not read at all, because "plan missing, assume trial"
 * is exactly the kind of guess that turns a revoked device back on.
 */
object LicenceCodec {

    /** The version of the statement this build reads. A newer one is refused rather than half-understood. */
    const val VERSION = 1

    /** The signed envelope, still encoded: the payload is checked as the bytes the server signed. */
    data class Envelope(val payload: String, val signature: String)

    /** A statement as the server made it. Times are milliseconds since the epoch. */
    data class Statement(
        val mac: String,
        val plan: Plan,
        val trialStartedAtMs: Long?,
        val trialExpiresAtMs: Long?,
        val expiresAtMs: Long?,
        val revokedAtMs: Long?,
        val establishedAtMs: Long,
        val serverTimeMs: Long,
    )

    class Unreadable(message: String) : IllegalArgumentException(message)

    /** `POST /device/licence`: the device's address and key, and what the owner's statistics count. */
    fun requestBody(
        mac: String,
        key: String,
        appVersion: String?,
        platform: String?,
        modelName: String?,
    ): String = buildString {
        append('{')
        append("\"macAddress\":").append(PortalInboxCodec.quote(mac))
        append(",\"deviceKey\":").append(PortalInboxCodec.quote(key))
        appVersion?.let { append(",\"appVersion\":").append(PortalInboxCodec.quote(it)) }
        platform?.let { append(",\"platform\":").append(PortalInboxCodec.quote(it)) }
        modelName?.let { append(",\"modelName\":").append(PortalInboxCodec.quote(it)) }
        append('}')
    }

    fun envelope(reader: Reader): Envelope {
        var payload: String? = null
        var signature: String? = null
        val scanner = JsonScanner(reader)
        scanner.readObject { field ->
            when (field) {
                "payload" -> payload = scanner.string()
                "signature" -> signature = scanner.string()
            }
        }
        return Envelope(
            payload = payload ?: throw Unreadable("no payload"),
            signature = signature ?: throw Unreadable("no signature"),
        )
    }

    /** The statement inside a payload whose signature has already been checked. */
    fun statement(json: String): Statement {
        var version: Long? = null
        var mac: String? = null
        var plan: String? = null
        var trialStartedAt: Long? = null
        var trialExpiresAt: Long? = null
        var expiresAt: Long? = null
        var revokedAt: Long? = null
        var establishedAt: Long? = null
        var serverTime: Long? = null

        val scanner = JsonScanner(StringReader(json))
        scanner.readObject { field ->
            when (field) {
                "v" -> version = scanner.number()
                "mac" -> mac = scanner.string()
                "plan" -> plan = scanner.string()
                "trialStartedAt" -> trialStartedAt = scanner.number()
                "trialExpiresAt" -> trialExpiresAt = scanner.number()
                "expiresAt" -> expiresAt = scanner.number()
                "revokedAt" -> revokedAt = scanner.number()
                "establishedAt" -> establishedAt = scanner.number()
                "serverTime" -> serverTime = scanner.number()
            }
        }

        if (version != VERSION.toLong()) throw Unreadable("statement version $version")
        val chosen = when (plan) {
            "trial" -> Plan.TRIAL
            "annual" -> Plan.ANNUAL
            "lifetime" -> Plan.LIFETIME
            else -> throw Unreadable("plan $plan")
        }
        if (chosen == Plan.TRIAL && (trialStartedAt == null || trialExpiresAt == null)) throw Unreadable("a trial without its dates")
        if (chosen == Plan.ANNUAL && expiresAt == null) throw Unreadable("a year without its end")

        return Statement(
            mac = mac ?: throw Unreadable("no address"),
            plan = chosen,
            trialStartedAtMs = trialStartedAt,
            trialExpiresAtMs = trialExpiresAt,
            expiresAtMs = expiresAt,
            revokedAtMs = revokedAt,
            establishedAtMs = establishedAt ?: throw Unreadable("no establishment"),
            serverTimeMs = serverTime ?: throw Unreadable("no server time"),
        )
    }

    /**
     * The record the repository stores. The identity is the device's own, not the server's echo of it: the
     * repository overwrites it again, and a statement about another address is refused before it gets here.
     */
    fun record(statement: Statement, mac: MacAddress, identityVersion: Int): EntitlementRecord = EntitlementRecord(
        macAddress = mac,
        identityVersion = identityVersion,
        plan = statement.plan,
        trialStartedAtMs = statement.trialStartedAtMs,
        trialExpiresAtMs = statement.trialExpiresAtMs,
        subscriptionExpiresAtMs = if (statement.plan == Plan.ANNUAL) statement.expiresAtMs else null,
        establishedAtMs = statement.establishedAtMs,
        lastVerifiedAtMs = statement.serverTimeMs,
        revokedAtMs = statement.revokedAtMs,
        maxObservedTimeMs = statement.serverTimeMs,
    )

    /**
     * Base64url without padding, as the server writes the payload and the signature. Here rather than
     * `java.util.Base64`, which Android only has from API 26, and the app runs from 23.
     */
    fun base64Url(text: String): ByteArray {
        val out = java.io.ByteArrayOutputStream(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in text) {
            val value = when (c) {
                in 'A'..'Z' -> c - 'A'
                in 'a'..'z' -> c - 'a' + 26
                in '0'..'9' -> c - '0' + 52
                '-', '+' -> 62
                '_', '/' -> 63
                '=' -> continue
                else -> throw Unreadable("not base64url")
            }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    /** A number, or null for `null` or an absent value. */
    private fun JsonScanner.number(): Long? = string()?.let { it.toLongOrNull() ?: throw Unreadable("not a number: $it") }
}
