package paspo.id.ssoprovider.client

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.PatternMatcher
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import paspo.id.ssoprovider.client.internal.EncryptionProcessor
import paspo.id.ssoprovider.client.internal.NonceRefresher
import paspo.id.ssoprovider.client.internal.RawResult
import paspo.id.ssoprovider.client.internal.ReferralManager
import paspo.id.ssoprovider.shared.models.PaspoClientError
import paspo.id.ssoprovider.shared.models.PaspoScope
import paspo.id.ssoprovider.shared.models.PaspoSsoConstants
import paspo.id.ssoprovider.shared.models.PaspoSsoProviderException
import paspo.id.ssoprovider.shared.models.PaspoSsoResult
import paspo.id.ssoprovider.shared.models.internal.EncryptedEnvelope
import paspo.id.ssoprovider.shared.toIpcReceiver
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Entry point of the PaspoID SDK: authenticates the user via the Paspo app.
 *
 * The instance keeps a reference to [activity]; create it per screen and do not store it
 * in objects that outlive the activity (view models, singletons).
 *
 * @param activity Activity that launches the Paspo consent screen and receives its result.
 */
public class PaspoID(
    private val activity: ComponentActivity
) {
    public companion object {
        /**
         * Intent action the Paspo app uses to launch your application when the user starts
         * sign-in from the Paspo side, and after a new user, who came from your app, finishes
         * signing up in Paspo.
         *
         * Declare an intent-filter with this action on the activity that hosts your sign-in
         * flow and start the flow when the activity is launched with it. Supporting the action
         * is required: when the flow started by [authenticate] can no longer continue (for example,
         * the system killed your app while the user was signing up in Paspo), a user who has just
         * signed up lands on your main screen and has to tap the sign-in button again.
         */
        public const val ACTION_SIGN_IN: String = PaspoSsoConstants.ACTION_AUTH

        /**
         * Boolean extra set on the intent your activity receives in `onNewIntent` when the SDK
         * closes the Google Play screens left on top of it after Paspo is installed. The intent
         * carries nothing else: skip your usual intent handling for it.
         */
        public const val EXTRA_RETURNED_FROM_STORE: String = PaspoSsoConstants.EXTRA_RETURNED_FROM_STORE

        @Volatile
        private var activeFlow: Any? = null
    }

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    private val encryptionProcessor by lazy { EncryptionProcessor(json) }
    private val appContext: Context by lazy { activity.applicationContext }

    private val referralManager: ReferralManager by lazy { ReferralManager(appContext) }

    /**
     * Runs the SSO flow: opens the Paspo consent screen and returns its outcome.
     *
     * Call from a coroutine bound to the activity (e.g. `lifecycleScope`) while the activity
     * is resumed, one flow at a time: a new call resets the state of the previous one.
     * All outcomes, including cancellation and missing Paspo app, are reported via
     * [PaspoAuthResult]; the method does not throw.
     *
     * If Paspo is not installed, the SDK opens its store page and waits: Google Play as a sheet
     * on top of your app, or the store available on the device (RuStore, AppGallery, GetApps)
     * without Google Play. Once Paspo is installed, the flow continues by itself: the user signs
     * up in Paspo and comes back to the consent screen. [PaspoAuthResult.NotInstalled] is returned
     * only when the user comes back to your app without installing Paspo.
     *
     * The [nonce] lives for a short time on the server. Use the overload with `refreshNonce`,
     * so an expired nonce does not end the flow with [PaspoClientError.SESSION_EXPIRED].
     *
     * @param scope Profile data to request from the user.
     * @param nonce One-time anti-replay value generated by your backend for this attempt.
     * @return Outcome of the flow; on [PaspoAuthResult.Success] pass
     * [PaspoAuthResult.Success.authCode] to your backend.
     */
    public suspend fun authenticate(
        scope: PaspoScope,
        nonce: String,
    ): PaspoAuthResult = runFlow(scope, nonce, refreshNonce = null)

    /**
     * Runs the SSO flow like the overload without `refreshNonce` and renews an expired nonce
     * on the way.
     *
     * Paspo calls [refreshNonce] when [nonce] expires: after the user installs Paspo and signs up,
     * or when the consent screen stays open for too long. The flow goes on with the new value, the
     * user does not have to start over.
     *
     * @param scope Profile data to request from the user.
     * @param nonce One-time anti-replay value generated by your backend for this attempt.
     * @param refreshNonce Requests a new nonce from your backend. Return `null` when a new nonce
     * cannot be obtained; the flow then ends with [PaspoClientError.SESSION_EXPIRED].
     * @return Outcome of the flow; on [PaspoAuthResult.Success] pass
     * [PaspoAuthResult.Success.authCode] to your backend.
     */
    public suspend fun authenticate(
        scope: PaspoScope,
        nonce: String,
        refreshNonce: suspend () -> String?,
    ): PaspoAuthResult = runFlow(scope, nonce, refreshNonce)

    private suspend fun runFlow(
        scope: PaspoScope,
        nonce: String,
        refreshNonce: (suspend () -> String?)?,
    ): PaspoAuthResult {
        val flow = Any().also { activeFlow = it }
        if (!checkInstallation()) {
            referralManager.saveReferralNonce(nonce)
            val installed = withContext(Dispatchers.Main) { installFromStore(scope) }
            if (activeFlow !== flow) return PaspoAuthResult.Cancelled
            if (!installed) return PaspoAuthResult.NotInstalled
        }

        cleanup()

        val keyPair = withContext(Dispatchers.Default) { encryptionProcessor.generateKeyPair() }

        val referralNonce = referralManager.getReferralNonce()
        val refresher = refreshNonce?.let { NonceRefresher() to it }
        val intent = Intent(PaspoSsoConstants.ACTION_AUTH).apply {
            setPackage(PaspoSsoConstants.TARGET_PACKAGE)
            putExtra(PaspoSsoConstants.EXTRA_CLIENT_PUBLIC_KEY, keyPair.publicKeyBytes.bytes)
            putExtra(PaspoSsoConstants.EXTRA_REQUEST_NONCE, nonce)
            putExtra(PaspoSsoConstants.EXTRA_SCOPE, scope.key)
            referralNonce?.let { putExtra(PaspoSsoConstants.EXTRA_REFERRAL_NONCE, it) }
            refresher?.let { (receiver, _) ->
                putExtra(PaspoSsoConstants.EXTRA_NONCE_REFRESHER, receiver.toIpcReceiver())
            }
        }

        val rawResult = coroutineScope {
            val serving = refresher?.let { (receiver, refresh) -> launch { receiver.serve(refresh) } }
            launchForResult(intent).also { serving?.cancel() }
        }
        return withContext(Dispatchers.Default) { handleResult(rawResult) }
    }

    /**
     * Returns `true` if the Paspo app is installed.
     *
     * Optional: [authenticate] performs this check itself. Useful to adapt UI upfront,
     * e.g. the sign-in button label.
     */
    public suspend fun checkInstallation(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                appContext.packageManager.getPackageInfo(PaspoSsoConstants.TARGET_PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

    /**
     * Destroys the ephemeral encryption keys of the current flow.
     *
     * Called automatically when the flow completes or fails; call manually only when
     * abandoning a flow midway (e.g. the user leaves the sign-in screen).
     */
    public fun cleanup(): Unit = encryptionProcessor.cleanup()

    private suspend fun launchForResult(intent: Intent): RawResult =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val registryKey = "paspo_sso_auth_${UUID.randomUUID()}"
                var launcher: ActivityResultLauncher<Intent>? = null

                launcher = activity.activityResultRegistry.register(
                    registryKey,
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    launcher?.unregister()
                    if (continuation.isActive) continuation.resume(RawResult(result.resultCode, result.data))
                }

                continuation.invokeOnCancellation { launcher.unregister() }

                launcher.launch(intent)
            }
        }

    private fun handleResult(rawResult: RawResult): PaspoAuthResult {
        if (rawResult.resultCode == Activity.RESULT_CANCELED) {
            cleanup()
            return PaspoAuthResult.Cancelled
        }

        val data = rawResult.data ?: return failure(PaspoClientError.INVALID_REQUEST, "No data in response")

        val errorCode = data.getIntExtra(PaspoSsoConstants.EXTRA_ERROR_CODE, -1)
        if (errorCode != -1) {
            val error = PaspoClientError.fromCode(errorCode)
            return if (error == PaspoClientError.CANCELLED) {
                cleanup()
                PaspoAuthResult.Cancelled
            } else {
                failure(error, null)
            }
        }

        val responseBytes = data.getByteArrayExtra(PaspoSsoConstants.EXTRA_ENCRYPTED_RESPONSE)
            ?: return failure(PaspoClientError.INVALID_REQUEST, "No encrypted response")

        return try {
            val envelope = json.decodeFromString<EncryptedEnvelope>(responseBytes.decodeToString())
            when (val result = encryptionProcessor.decryptAuthResult(envelope)) {
                is PaspoSsoResult.Success -> PaspoAuthResult.Success(authCode = result.nonce)
                is PaspoSsoResult.Failure -> failure(PaspoClientError.fromCode(result.errorCode), null)
            }
        } catch (e: PaspoSsoProviderException) {
            failure(e.code, e.message)
        } catch (e: Exception) {
            failure(PaspoClientError.CRYPTO_ERROR, e.message ?: "Decryption failed")
        }
    }

    private fun failure(
        error: PaspoClientError,
        message: String?,
    ): PaspoAuthResult.Failure {
        cleanup()
        return PaspoAuthResult.Failure(error, message)
    }

    private suspend fun installFromStore(scope: PaspoScope): Boolean {
        val installed = CompletableDeferred<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                installed.complete(Unit)
            }
        }
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme(PACKAGE_SCHEME)
            addDataSchemeSpecificPart(PaspoSsoConstants.TARGET_PACKAGE, PatternMatcher.PATTERN_LITERAL)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        return try {
            if (!openStore(scope)) return false
            awaitReturnFromStore(installed)
            closeStoreSheet()
            checkInstallation()
        } finally {
            appContext.unregisterReceiver(receiver)
        }
    }

    private suspend fun awaitReturnFromStore(installed: Deferred<Unit>) {
        val lifecycle = activity.lifecycle
        withTimeoutOrNull(STORE_OPEN_TIMEOUT) {
            lifecycle.states().first { !it.isAtLeast(Lifecycle.State.RESUMED) }
        } ?: return
        coroutineScope {
            val resumed = async { lifecycle.states().first { it.isAtLeast(Lifecycle.State.RESUMED) } }
            select {
                installed.onAwait {}
                resumed.onAwait {}
            }
            resumed.cancel()
        }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycle.states().first { it.isAtLeast(Lifecycle.State.RESUMED) }
        }
    }

    private fun closeStoreSheet() {
        activity.finishActivity(INLINE_INSTALL_REQUEST_CODE)
        if (activity.lifecycle.currentState != Lifecycle.State.STARTED) return
        runCatching {
            activity.startActivity(
                Intent(activity, activity.javaClass)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_RETURNED_FROM_STORE, true)
            )
        }
    }

    private fun Lifecycle.states(): Flow<Lifecycle.State> =
        callbackFlow {
            val observer = LifecycleEventObserver { _, _ -> trySend(currentState) }
            addObserver(observer)
            awaitClose { removeObserver(observer) }
        }

    private fun openStore(scope: PaspoScope): Boolean {
        val target = PaspoSsoConstants.TARGET_PACKAGE
        val referrer = listOf(PaspoSsoConstants.REFERRER_SSO_PREFIX, appContext.packageName, scope.key)
            .joinToString(PaspoSsoConstants.REFERRER_SSO_SEPARATOR)
        return openPlayInlineInstall(target, referrer) ||
            listOf(
                "market://details?id=$target&referrer=$referrer",
                "https://paspo.id/mob/s/$referrer"
            ).any(::openUri)
    }

    @Suppress("DEPRECATION")
    private fun openPlayInlineInstall(
        target: String,
        referrer: String,
    ): Boolean =
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/d?id=$target&referrer=$referrer".toUri())
                .setPackage(PLAY_STORE_PACKAGE)
                .putExtra(EXTRA_OVERLAY, true)
                .putExtra(EXTRA_CALLER_ID, appContext.packageName)
            activity.startActivityForResult(intent, INLINE_INSTALL_REQUEST_CODE)
        }.isSuccess

    private fun openUri(uri: String): Boolean =
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
}

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val EXTRA_OVERLAY = "overlay"
private const val EXTRA_CALLER_ID = "callerId"
private const val PACKAGE_SCHEME = "package"
private const val INLINE_INSTALL_REQUEST_CODE = 0x5A50
private val STORE_OPEN_TIMEOUT = 5.seconds
