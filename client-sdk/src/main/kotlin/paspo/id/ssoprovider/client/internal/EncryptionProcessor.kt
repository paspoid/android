package paspo.id.ssoprovider.client.internal

import kotlinx.serialization.json.Json
import paspo.id.ssoprovider.shared.crypto.AesGcmCipher
import paspo.id.ssoprovider.shared.crypto.ECDHKeyManager
import paspo.id.ssoprovider.shared.crypto.KeyDerivation
import paspo.id.ssoprovider.shared.models.PaspoClientError
import paspo.id.ssoprovider.shared.models.PaspoSsoProviderException
import paspo.id.ssoprovider.shared.models.PaspoSsoResult
import paspo.id.ssoprovider.shared.models.internal.EncryptedEnvelope
import paspo.id.ssoprovider.shared.models.internal.KdfContext
import paspo.id.ssoprovider.shared.models.internal.KeyPairResult
import paspo.id.ssoprovider.shared.models.internal.SecureBytes
import paspo.id.ssoprovider.shared.models.internal.toSecureBytes

internal class EncryptionProcessor(
    private val json: Json,
) {
    private var activeKeyAlias: String? = null

    fun generateKeyPair(): KeyPairResult {
        val result = ECDHKeyManager.generateEphemeralKeyPair("sso_client_auth")
        activeKeyAlias = result.alias
        return result
    }

    fun decryptAuthResult(data: EncryptedEnvelope): PaspoSsoResult {
        val ourKeyAlias = activeKeyAlias
            ?: throw PaspoSsoProviderException(PaspoClientError.CRYPTO_ERROR, "No active key found")

        val sharedSecret = ECDHKeyManager.computeSharedSecret(
            ourKeyAlias,
            data.senderPublicKey
        )

        val aesKey = KeyDerivation.deriveAesKey(sharedSecret, KdfContext.AUTH_RESPONSE)

        val plaintext = decrypt(data, aesKey)

        cleanup()

        return json.decodeFromString<PaspoSsoResult>(plaintext.bytes.decodeToString())
    }

    fun cleanup() {
        activeKeyAlias?.let { ECDHKeyManager.deleteKey(it) }
        activeKeyAlias = null
        ECDHKeyManager.clearStore()
    }

    private fun decrypt(
        envelope: EncryptedEnvelope,
        aesKey: SecureBytes
    ): SecureBytes {
        return AesGcmCipher
            .decrypt(
                data = envelope.iv.bytes + envelope.ciphertext.bytes,
                key = aesKey.bytes
            )?.toSecureBytes()
            ?: throw PaspoSsoProviderException(PaspoClientError.CRYPTO_ERROR, "Decryption failed")
    }
}
