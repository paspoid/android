package paspo.id.ssoprovider.client.internal

import android.os.Bundle
import android.os.ResultReceiver
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import paspo.id.ssoprovider.shared.models.PaspoSsoConstants
import kotlin.time.Duration.Companion.seconds

internal class NonceRefresher : ResultReceiver(null) {
    private val requests = Channel<ResultReceiver>(Channel.UNLIMITED, onUndeliveredElement = { it.reply(null) })

    override fun onReceiveResult(
        resultCode: Int,
        resultData: Bundle?,
    ) {
        if (resultCode != PaspoSsoConstants.NONCE_REFRESH_REQUEST) return
        val replyTo = resultData
            ?.let { BundleCompat.getParcelable(it, PaspoSsoConstants.KEY_NONCE_REPLY_TO, ResultReceiver::class.java) }
            ?: return
        if (requests.trySend(replyTo).isFailure) replyTo.reply(null)
    }

    suspend fun serve(refreshNonce: suspend () -> String?) {
        requests.consumeEach { replyTo ->
            var nonce: String? = null
            try {
                nonce = requestNonce(refreshNonce)
            } finally {
                replyTo.reply(nonce)
            }
        }
    }

    private fun ResultReceiver.reply(nonce: String?) {
        if (nonce == null) {
            send(PaspoSsoConstants.NONCE_REFRESH_FAILURE, null)
        } else {
            send(PaspoSsoConstants.NONCE_REFRESH_SUCCESS, bundleOf(PaspoSsoConstants.KEY_REFRESHED_NONCE to nonce))
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private suspend fun requestNonce(refreshNonce: suspend () -> String?): String? =
    try {
        withTimeoutOrNull(REFRESH_TIMEOUT) { refreshNonce() }?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }

private val REFRESH_TIMEOUT = 8.seconds
