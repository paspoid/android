package paspo.id.ssoprovider.client.internal

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReferralManager(
    private val appContext: Context,
) {
    private val prefs by lazy {
        appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun saveReferralNonce(nonce: String) =
        withContext(Dispatchers.IO) {
            prefs.edit(commit = true) { putString(REFERRAL_NONCE_KEY, nonce) }
        }

    suspend fun getReferralNonce(): String? =
        withContext(Dispatchers.IO) {
            prefs.getString(REFERRAL_NONCE_KEY, null)
        }

    suspend fun clearReferralNonce() =
        withContext(Dispatchers.IO) {
            prefs.edit(commit = true) { remove(REFERRAL_NONCE_KEY) }
        }

    private companion object {
        const val SHARED_PREFS_NAME = "paspoid_sso_referral"
        const val REFERRAL_NONCE_KEY = "referral_nonce"
    }
}
