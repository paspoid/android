package paspo.id.ssoprovider.shared.crypto

import paspo.id.ssoprovider.shared.PaspoInternalApi
import paspo.id.ssoprovider.shared.models.internal.KeyPairResult
import paspo.id.ssoprovider.shared.models.internal.SecureBytes
import paspo.id.ssoprovider.shared.models.internal.toSecureBytes
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyAgreement

/**
 * Manages Ephemeral Elliptic Curve Diffie-Hellman (ECDH) key pairs in memory.
 */

@PaspoInternalApi
public object ECDHKeyManager {
    private const val EC_ALGORITHM = "EC"
    private const val ECDH_ALGORITHM = "ECDH"
    private const val CURVE = "secp256r1"
    private val keyStore = ConcurrentHashMap<String, KeyPair>()

    /**
     * Generates and stores a new key pair using the secp256r1 curve.
     *
     * @param alias Base name for the key
     * @return Unique alias and the public key
     */

    public fun generateEphemeralKeyPair(alias: String): KeyPairResult {
        val fullAlias = "${alias}_${System.nanoTime()}"

        val keyPair = KeyPairGenerator.getInstance(EC_ALGORITHM).run {
            initialize(ECGenParameterSpec(CURVE))
            generateKeyPair()
        }

        keyStore[fullAlias] = keyPair

        return KeyPairResult(fullAlias, keyPair.public.encoded.toSecureBytes())
    }

    /**
     * Computes the ECDH shared secret using the stored private key and the peer's public key.
     *
     * @param alias Unique alias of the stored key
     * @param peerPublicKey The public key of the other party
     */

    public fun computeSharedSecret(
        alias: String,
        peerPublicKey: SecureBytes
    ): SecureBytes {
        val keyPair = keyStore[alias] ?: error("Key not found: $alias")

        val keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM).apply {
            init(keyPair.private)
            doPhase(deserializePublicKey(peerPublicKey), true)
        }

        return keyAgreement.generateSecret().toSecureBytes()
    }

    public fun deleteKey(alias: String) {
        keyStore.remove(alias)
    }

    private fun deserializePublicKey(encoded: SecureBytes): ECPublicKey =
        KeyFactory
            .getInstance(EC_ALGORITHM)
            .run { generatePublic(X509EncodedKeySpec(encoded.bytes)) as ECPublicKey }

    public fun clearStore(): Unit = keyStore.clear()
}
