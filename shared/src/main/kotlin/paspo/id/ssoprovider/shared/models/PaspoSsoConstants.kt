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

    public const val EXTRA_RETURNED_FROM_STORE: String = "$EXTRAS_PREFIX:returned_from_store"
    public const val EXTRA_NONCE_REFRESHER: String = "$EXTRAS_PREFIX:nonce_refresher"
    public const val KEY_NONCE_REPLY_TO: String = "$EXTRAS_PREFIX:nonce_reply_to"
    public const val KEY_REFRESHED_NONCE: String = "$EXTRAS_PREFIX:refreshed_nonce"
    public const val NONCE_REFRESH_REQUEST: Int = 2
    public const val NONCE_REFRESH_SUCCESS: Int = 1
    public const val NONCE_REFRESH_FAILURE: Int = 0

    public const val REFERRER_SSO_PREFIX: String = "sso"
    public const val REFERRER_SSO_SEPARATOR: String = "-"
}
