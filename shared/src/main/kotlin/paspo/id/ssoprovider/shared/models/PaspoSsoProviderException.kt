package paspo.id.ssoprovider.shared.models

import paspo.id.ssoprovider.shared.PaspoInternalApi

/** Internal signalling between the crypto layer and the SDK facade; never thrown to client code. */
@PaspoInternalApi
public open class PaspoSsoProviderException(
    public val code: PaspoClientError,
    override val message: String,
) : Exception(message)
