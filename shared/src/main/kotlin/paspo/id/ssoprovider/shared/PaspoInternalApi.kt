package paspo.id.ssoprovider.shared

/**
 * Marks declarations that are internal to the Paspo SSO SDK and the Paspo host app.
 *
 * Such declarations are part of the wire protocol or crypto implementation, not the public SDK
 * contract: they may change or disappear in any release without notice or deprecation cycle.
 * Client applications must not use them.
 */
@RequiresOptIn(
    message = "Internal Paspo SSO SDK API: may change without notice. Do not use from client code.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class PaspoInternalApi
