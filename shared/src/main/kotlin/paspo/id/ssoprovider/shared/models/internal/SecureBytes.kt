package paspo.id.ssoprovider.shared.models.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import paspo.id.ssoprovider.shared.PaspoInternalApi

@PaspoInternalApi
@Serializable
public data class SecureBytes(
    @SerialName("bytes") val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureBytes

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String = "[Bytes: size=${bytes.size}]"
}

@PaspoInternalApi
public fun ByteArray.toSecureBytes(): SecureBytes = SecureBytes(this)
