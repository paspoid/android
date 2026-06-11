package paspo.id.ssoprovider.shared.models

import paspo.id.ssoprovider.shared.PaspoInternalApi

/** Intent contract between the client SDK and the Paspo host app. */
@PaspoInternalApi
public object PaspoSsoConstants {
    private const val EXTRAS_PREFIX = "paspo.id.ssoprovider.extras"
    private const val ACTION_PREFIX = "paspo.id.ssoprovider.action"
    public const val TARGET_PACKAGE: String = "paspo.id"

    public const val ACTION_AUTH: String = "$ACTION_PREFIX.AUTHENTICATE"

    public const val EXTRA_CLIENT_PUBLIC_KEY: String = "$EXTRAS_PREFIX:client_public_key"
    public const val EXTRA_REQUEST_NONCE: String = "$EXTRAS_PREFIX:request_nonce"
    public const val EXTRA_SCOPE: String = "$EXTRAS_PREFIX:scope"
    public const val EXTRA_ENCRYPTED_PAYLOAD: String = "$EXTRAS_PREFIX:encrypted_payload"

    public const val EXTRA_ENCRYPTED_RESPONSE: String = "$EXTRAS_PREFIX:encrypted_response"
    public const val EXTRA_ERROR_CODE: String = "$EXTRAS_PREFIX:error_code"

    public const val EXTRA_REFERRAL_NONCE: String = "$EXTRAS_PREFIX:referral_nonce"
}
