package paspo.id.ssoprovider.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import paspo.id.ssoprovider.client.PaspoAuthResult
import paspo.id.ssoprovider.client.PaspoID
import paspo.id.ssoprovider.shared.models.PaspoClientError
import paspo.id.ssoprovider.shared.models.PaspoScope
import paspo.id.ssoprovider.ui.PaspoButtonTheme
import paspo.id.ssoprovider.ui.R

private const val DISABLED_ALPHA = 0.6f
private val ICON_SIZE = 30.dp
private val MIN_HEIGHT = 48.dp

/**
 * Drop-in "Sign in with Paspo ID" composable button.
 *
 * Stateless and theme-independent: it does not read `MaterialTheme`, so it looks identical in any
 * app. On click it runs [PaspoID.authenticate], blocks repeated taps while in flight and delivers
 * the outcome to [onResult].
 *
 * @param onResult receives the [PaspoAuthResult]. If [nonceProvider] fails, a
 * [PaspoAuthResult.Failure] with [PaspoClientError.SERVICE_UNAVAILABLE] is delivered.
 * @param nonceProvider suspend block that fetches a one-time nonce from your server.
 * @param scope profile data requested from the user.
 * @param cornerRadius corner radius; the default renders a pill, pass a smaller value to round less.
 */
@Composable
public fun PaspoSignInButton(
    onResult: (PaspoAuthResult) -> Unit,
    nonceProvider: suspend () -> String,
    modifier: Modifier = Modifier,
    scope: PaspoScope = PaspoScope.PHONES,
    theme: PaspoButtonTheme = PaspoButtonTheme.BRAND,
    cornerRadius: Dp = PaspoSignInButtonDefaults.CornerRadius,
    iconOnly: Boolean = false,
    showLogo: Boolean = true,
) {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("PaspoSignInButton must be hosted in a ComponentActivity")
    val paspoId = remember(activity) { PaspoID(activity) }
    val coroutineScope = rememberCoroutineScope()
    var inFlight by remember { mutableStateOf(false) }

    PaspoSignInButtonContent(
        onClick = {
            inFlight = true
            coroutineScope.launch {
                try {
                    onResult(paspoId.authenticate(scope, nonceProvider()))
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Exception) {
                    onResult(PaspoAuthResult.Failure(PaspoClientError.SERVICE_UNAVAILABLE, t.message))
                } finally {
                    inFlight = false
                }
            }
        },
        modifier = modifier,
        theme = theme,
        cornerRadius = cornerRadius,
        iconOnly = iconOnly,
        showLogo = showLogo,
        enabled = !inFlight
    )
}

/**
 * Render-only variant: draws the branded button and reports clicks via [onClick], leaving the
 * SSO flow to the caller. Use it for full control, custom [colors] or Compose previews.
 */
@Composable
public fun PaspoSignInButtonContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: PaspoButtonTheme = PaspoButtonTheme.BRAND,
    cornerRadius: Dp = PaspoSignInButtonDefaults.CornerRadius,
    iconOnly: Boolean = false,
    showLogo: Boolean = true,
    enabled: Boolean = true,
    colors: PaspoButtonColors = theme.colors(),
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val labelText = stringResource(R.string.paspo_sso_button_sign_in)

    val base = modifier
        .clip(shape)
        .background(colors.background)
        .then(if (colors.border != null) Modifier.border(1.dp, colors.border, shape) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = labelText, onClick = onClick)
        .alpha(if (enabled) 1f else DISABLED_ALPHA)

    if (iconOnly) {
        Box(modifier = base.size(MIN_HEIGHT), contentAlignment = Alignment.Center) {
            PaspoLogo(colors.content)
        }
    } else {
        Row(
            modifier = base
                .defaultMinSize(minHeight = MIN_HEIGHT)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLogo) {
                PaspoLogo(colors.content)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = labelText,
                color = colors.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaspoLogo(tint: Color) {
    Image(
        painter = painterResource(R.drawable.paspo_id_logo),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(ICON_SIZE)
    )
}
