package paspo.id.ssoprovider.shared.models.internal

import paspo.id.ssoprovider.shared.PaspoInternalApi

@PaspoInternalApi
public data class KeyPairResult(
    val alias: String,
    val publicKeyBytes: SecureBytes
)
