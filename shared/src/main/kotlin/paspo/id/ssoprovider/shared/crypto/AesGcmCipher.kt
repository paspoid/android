package paspo.id.ssoprovider.shared.crypto

import paspo.id.ssoprovider.shared.PaspoInternalApi
import paspo.id.ssoprovider.shared.models.internal.EncryptionResult
import paspo.id.ssoprovider.shared.models.internal.SecureBytes
import paspo.id.ssoprovider.shared.models.internal.toSecureBytes
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@PaspoInternalApi
public object AesGcmCipher {
    public const val AES_GSM_TRANSFORMATION: String = "AES/GCM/NoPadding"
    public const val GCM_TAG_LENGTH: Int = 128
    public const val GCM_IV_SIZE: Int = 12
    public const val AES_KEY_SIZE: Int = 256
    private const val AES_KEY_SIZE_BYTES = 32

    public fun encrypt(
        data: SecureBytes,
        key: SecureBytes
    ): EncryptionResult {
        val keyBytes = key.bytes
        require(keyBytes.size == AES_KEY_SIZE_BYTES) { "Key must be $AES_KEY_SIZE_BYTES B for AES-256" }

        val cipher = Cipher.getInstance(AES_GSM_TRANSFORMATION)
        val iv = ByteArray(GCM_IV_SIZE).apply { SecureRandom().nextBytes(this) }
        val keySpec = getSecretKeyFromBytes(keyBytes)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return EncryptionResult(iv = iv.toSecureBytes(), ciphertext = cipher.doFinal(data.bytes).toSecureBytes())
    }

    public fun decrypt(
        data: ByteArray,
        key: ByteArray
    ): ByteArray? {
        require(key.size == AES_KEY_SIZE_BYTES) { "Key must be $AES_KEY_SIZE_BYTES B for AES-256" }

        val iv = data.sliceArray(0 until GCM_IV_SIZE)
        val cipherText = data.sliceArray(GCM_IV_SIZE until data.size)
        val keySpec = getSecretKeyFromBytes(key)

        val cipher = Cipher.getInstance(AES_GSM_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

        return cipher.doFinal(cipherText)
    }

    private fun getSecretKeyFromBytes(bytes: ByteArray): SecretKey {
        return SecretKeySpec(bytes, "AES")
    }
}
