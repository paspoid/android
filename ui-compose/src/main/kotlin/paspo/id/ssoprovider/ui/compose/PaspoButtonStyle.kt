package paspo.id.ssoprovider.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import paspo.id.ssoprovider.ui.PaspoButtonTheme

/**
 * Colors of [PaspoSignInButton]. Pass a custom instance to fully override the palette,
 * or use [PaspoButtonTheme] for the predefined ones.
 */
@Immutable
public data class PaspoButtonColors(
    val background: Color,
    val content: Color,
    val border: Color?,
)

/** Default values for [PaspoSignInButton]. */
public object PaspoSignInButtonDefaults {
    /** Large radius that renders a pill at any height. Pass a smaller [Dp] to round less. */
    public val CornerRadius: Dp = 1000.dp
}

internal fun PaspoButtonTheme.colors(): PaspoButtonColors =
    when (this) {
        PaspoButtonTheme.BRAND -> PaspoButtonColors(
            background = Color(0xFF1677FF),
            content = Color.White,
            border = null
        )

        PaspoButtonTheme.LIGHT -> PaspoButtonColors(
            background = Color.White,
            content = Color(0xFF1F1F1F),
            border = Color(0xFFE0E0E0)
        )

        PaspoButtonTheme.DARK -> PaspoButtonColors(
            background = Color(0xFF131314),
            content = Color(0xFFE3E3E3),
            border = null
        )

        PaspoButtonTheme.NEUTRAL -> PaspoButtonColors(
            background = Color(0xFFF2F2F2),
            content = Color(0xFF1F1F1F),
            border = null
        )
    }
