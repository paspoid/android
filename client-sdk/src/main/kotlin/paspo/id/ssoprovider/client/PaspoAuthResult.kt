package paspo.id.ssoprovider.client

import paspo.id.ssoprovider.shared.models.PaspoClientError

/**
 * Outcome of [PaspoID.authenticate]. The single channel for all results of the SSO flow:
 * `authenticate` never throws for flow-level outcomes, it always returns one of these.
 */
public sealed interface PaspoAuthResult {
    /**
     * The user confirmed authentication in Paspo.
     *
     * @property authCode One-time authorization code. Pass it to your backend, which exchanges
     * it for the user data via the Paspo server API. The code contains no personal data itself.
     */
    public data class Success(
        val authCode: String,
    ) : PaspoAuthResult

    /** The user dismissed the Paspo consent screen. Usually requires no error UI. */
    public data object Cancelled : PaspoAuthResult

    /**
     * The Paspo app is not installed. The SDK has already opened its store page;
     * after signing up, Paspo returns the user to your app with [PaspoID.ACTION_SIGN_IN].
     */
    public data object NotInstalled : PaspoAuthResult

    /**
     * The flow failed.
     *
     * @property error Failure reason, see [PaspoClientError].
     * @property message Optional diagnostic detail. Not localized, do not show to the user.
     */
    public data class Failure(
        val error: PaspoClientError,
        val message: String? = null,
    ) : PaspoAuthResult
}
