package paspo.id.ssoprovider.shared.models.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import paspo.id.ssoprovider.shared.PaspoInternalApi

@PaspoInternalApi
@Serializable
public data class EncryptedEnvelope(
    @SerialName("sender_public_key") val senderPublicKey: SecureBytes,
    @SerialName("iv") val iv: SecureBytes,
    @SerialName("ciphertext") val ciphertext: SecureBytes,
)
