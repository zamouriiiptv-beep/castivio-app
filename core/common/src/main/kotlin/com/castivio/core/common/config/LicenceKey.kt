package com.castivio.core.common.config

/**
 * The licence server's public key, pinned in the app: what a licence statement must be signed with to be
 * believed.
 *
 * ECDSA P-256, as an X.509 SubjectPublicKeyInfo in base64 (the portal's `docs/licence-api.md` prints it). The
 * private half is a secret of the server and is nowhere in this repository. A statement signed with anything
 * else -- a proxy, a forged answer, an edited cache -- does not verify, and the app keeps what it had.
 *
 * One key for every build, because every build asks the same server: a debug APK on a test television reads
 * the licence the owner's panel really set.
 */
class LicenceKey private constructor(val spkiBase64: String) {

    override fun equals(other: Any?): Boolean = other is LicenceKey && other.spkiBase64 == spkiBase64

    override fun hashCode(): Int = spkiBase64.hashCode()

    override fun toString(): String = "LicenceKey(${spkiBase64.takeLast(12)})"

    companion object {

        /** The production key. Changing it is a deliberate edit here, made with the server's own. */
        const val PRODUCTION: String =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsPGw9SopQJfN1cRZTqv6DLBYh0lGi53wrZrbgAd7B4Xd8rDXfNqTXcjy63P2mY+U0oc2k3Y/Dprh11nTKVThAA=="

        /** @param configured what the build says, or null/blank for [PRODUCTION]. */
        fun of(configured: String?): LicenceKey {
            val trimmed = configured?.trim().orEmpty()
            return LicenceKey(trimmed.ifEmpty { PRODUCTION })
        }
    }
}
