package paspo.id.ssoprovider.shared.crypto

import paspo.id.ssoprovider.shared.PaspoInternalApi
import paspo.id.ssoprovider.shared.models.internal.SecureBytes
import paspo.id.ssoprovider.shared.models.internal.toSecureBytes
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for deriving cryptographic keys using an HKDF-like approach with HMAC-SHA256.
 */

@PaspoInternalApi
public object KeyDerivation {
    private const val HMAC_ALGO = "HmacSHA256"

    /**
     * Derives a 32-byte (256-bit) AES key from a shared secret, info string, and an optional salt.
     *
     * @param sharedSecret (e.g., ECDH shared secret)
     * @param info Context-specific information
     * @param salt Optional salt
     * @return Derived 32-byte secure key
     */

    public fun deriveAesKey(
        sharedSecret: SecureBytes,
        info: String,
        salt: SecureBytes = SecureBytes(ByteArray(32))
    ): SecureBytes {
        val prk = hmacSha256(salt.bytes, sharedSecret.bytes)
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        val okm = hmacSha256(prk, infoBytes + byteArrayOf(0x01))
        return okm.copyOf(32).toSecureBytes()
    }

    private fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(key, HMAC_ALGO))
        return mac.doFinal(data)
    }
}
