package paspo.id.ssoprovider.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Set of profile data the client application requests from the user.
 *
 * The user sees the requested scope on the Paspo consent screen and can decline it.
 */
@Serializable
public enum class PaspoScope(
    public val key: String
) {
    /** Phone numbers of the profile. */
    @SerialName("phones")
    PHONES(key = "phones"),

    /** E-mail addresses of the profile. */
    @SerialName("emails")
    EMAILS(key = "emails"),

    /** National identity document number. */
    @SerialName("national_id")
    NATIONAL_ID(key = "national_id"),

    /** Permanent user identifier in Paspo. */
    @SerialName("paspo_id")
    PASPO_ID(key = "paspo_id");

    public companion object {
        /** Resolves a scope from its wire [key], or `null` if unknown. */
        public fun fromKey(key: String): PaspoScope? = PaspoScope.entries.find { it.key == key }
    }
}
