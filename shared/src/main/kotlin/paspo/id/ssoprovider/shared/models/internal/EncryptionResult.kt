package paspo.id.ssoprovider.shared.models.internal

import paspo.id.ssoprovider.shared.PaspoInternalApi

@PaspoInternalApi
public data class EncryptionResult(
    val iv: SecureBytes,
    val ciphertext: SecureBytes
)
