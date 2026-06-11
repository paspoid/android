package paspo.id.ssoprovider.shared.models.internal

import paspo.id.ssoprovider.shared.PaspoInternalApi

@PaspoInternalApi
public object KdfContext {
    private const val VERSION = "v1"
    private const val PREFIX = "paspo_id"

    public const val AUTH_REQUEST: String = "${PREFIX}_auth_req_$VERSION"
    public const val AUTH_RESPONSE: String = "${PREFIX}_auth_res_$VERSION"
}
