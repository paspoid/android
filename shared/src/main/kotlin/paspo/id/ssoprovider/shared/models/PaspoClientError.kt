package paspo.id.ssoprovider.shared.models

import kotlinx.serialization.Serializable

/**
 * Error reasons of the SSO flow, shared between the Paspo app and the client SDK.
 *
 * Delivered to client code in `PaspoAuthResult.Failure`.
 */
@Serializable
public enum class PaspoClientError(
    public val code: Int
) {
    /** General */
    UNKNOWN(100),
    INVALID_REQUEST(101),
    SERVICE_UNAVAILABLE(102),
    CANCELLED(103),

    /** Session / Integration */
    SESSION_EXPIRED(110),
    PACKAGE_VERIFICATION_FAILED(111),

    /** Access */
    ACCESS_DENIED(120),
    IDENTITY_NOT_VERIFIED(121),
    ATTEMPTS_EXCEEDED(122),
    ACTION_NOT_ALLOWED(123),

    /** Method */
    AUTH_METHOD_UNAVAILABLE(130),
    UNSUPPORTED_AUTH_METHOD(131),

    /** Client-side */
    PASPO_NOT_INSTALLED(140),
    CRYPTO_ERROR(141);

    public companion object {
        /** Resolves an error from its wire [code], falling back to [UNKNOWN]. */
        public fun fromCode(code: Int): PaspoClientError = entries.find { it.code == code } ?: UNKNOWN
    }
}
