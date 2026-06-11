package paspo.id.ssoprovider.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import paspo.id.ssoprovider.shared.PaspoInternalApi

/** Wire payload of the encrypted SSO response. Client code receives `PaspoAuthResult` instead. */
@PaspoInternalApi
@Serializable
public sealed interface PaspoSsoResult {
    @Serializable
    @SerialName("success")
    public data class Success(
        @SerialName("nonce") val nonce: String,
    ) : PaspoSsoResult

    @Serializable
    @SerialName("failure")
    public data class Failure(
        @SerialName("error_code") val errorCode: Int,
    ) : PaspoSsoResult
}
