package paspo.id.ssoprovider.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import paspo.id.ssoprovider.client.PaspoAuthResult
import paspo.id.ssoprovider.client.PaspoID
import paspo.id.ssoprovider.shared.models.PaspoClientError
import paspo.id.ssoprovider.shared.models.PaspoScope
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Drop-in "Sign in with Paspo ID" button.
 *
 * Rendered as a single self-drawing [View] (no nested views, no allocations during draw) so it is
 * cheap to inflate and repaint. Styling is independent of the host theme, which keeps the brand
 * look consistent across applications.
 *
 * Wire the SSO flow with [setAuthHandler]; the button then runs [PaspoID.authenticate] on click,
 * blocks double taps while in flight and delivers the outcome to your callback.
 */
public class PaspoSignInButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val logo: Drawable? =
        ResourcesCompat.getDrawable(resources, R.drawable.paspo_id_logo, context.theme)?.mutate()

    private val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(TEXT_SIZE_SP)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private var textLayout: StaticLayout? = null
    private var labelText: String = ""

    private val iconSizePx = dp(ICON_SIZE_DP).roundToInt()
    private val gapPx = dp(GAP_DP)
    private val hPaddingPx = dp(H_PADDING_DP)
    private val minHeightPx = dp(MIN_HEIGHT_DP)

    public var buttonTheme: PaspoButtonTheme = PaspoButtonTheme.BRAND
        set(value) {
            field = value
            applyStyle()
        }

    /** Corner radius in pixels. A large value (the default) renders a pill. */
    public var cornerRadius: Float = dp(PILL_RADIUS_DP)
        set(value) {
            field = value
            applyStyle()
        }

    public var iconOnly: Boolean = false
        set(value) {
            field = value
            applyStyle()
        }

    private var inFlight = false

    init {
        isClickable = true
        isFocusable = true
        context.withStyledAttributes(attrs, R.styleable.PaspoSignInButton) {
            buttonTheme = PaspoButtonTheme.entries[getInt(R.styleable.PaspoSignInButton_paspoTheme, 0)]
            cornerRadius = getDimension(R.styleable.PaspoSignInButton_paspoCornerRadius, dp(PILL_RADIUS_DP))
            iconOnly = getBoolean(R.styleable.PaspoSignInButton_paspoIconOnly, false)
        }
        applyStyle()
    }

    /**
     * Binds the SSO flow to this button.
     *
     * @param activity host activity that launches the Paspo consent screen.
     * @param scope profile data requested from the user.
     * @param nonceProvider suspend block that fetches a one-time nonce from your server.
     * @param onResult receives the [PaspoAuthResult]. If [nonceProvider] fails, a
     * [PaspoAuthResult.Failure] with [PaspoClientError.SERVICE_UNAVAILABLE] is delivered.
     */
    public fun setAuthHandler(
        activity: ComponentActivity,
        scope: PaspoScope,
        nonceProvider: suspend () -> String,
        onResult: (PaspoAuthResult) -> Unit,
    ) {
        val paspoId = PaspoID(activity)
        setOnClickListener {
            if (inFlight) return@setOnClickListener
            inFlight = true
            isEnabled = false
            activity.lifecycleScope.launch {
                try {
                    onResult(paspoId.authenticate(scope, nonceProvider()))
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Exception) {
                    onResult(PaspoAuthResult.Failure(PaspoClientError.SERVICE_UNAVAILABLE, t.message))
                } finally {
                    inFlight = false
                    isEnabled = true
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun applyStyle() {
        val colors = colorsFor(buttonTheme)

        logo?.let { DrawableCompat.setTint(it, colors.content) }
        textPaint.color = colors.content

        val text = context.getString(R.string.paspo_sso_button_sign_in)
        labelText = if (iconOnly) "" else text
        contentDescription = text

        background = buildBackground(colors)

        requestLayout()
        invalidate()
    }

    private fun buildBackground(colors: ButtonColors): Drawable {
        val content = GradientDrawable().apply {
            setColor(colors.background)
            cornerRadius = this@PaspoSignInButton.cornerRadius
            if (colors.stroke != Color.TRANSPARENT) setStroke(dp(STROKE_DP).roundToInt(), colors.stroke)
        }
        val mask = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = this@PaspoSignInButton.cornerRadius
        }
        val ripple = ColorStateList.valueOf(withAlpha(colors.content, RIPPLE_ALPHA))
        return RippleDrawable(ripple, content, mask)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val height = resolveSize(max(minHeightPx, iconSizePx.toFloat()).roundToInt(), heightMeasureSpec)

        val desiredWidth = if (iconOnly) {
            height
        } else {
            val textWidth = Layout.getDesiredWidth(labelText, textPaint)
            (hPaddingPx * 2 + iconSizePx + gapPx + textWidth).roundToInt()
        }
        val width = resolveSize(desiredWidth, widthMeasureSpec)

        if (!iconOnly) {
            val available = (width - hPaddingPx * 2 - iconSizePx - gapPx).roundToInt().coerceAtLeast(0)
            textLayout = StaticLayout.Builder
                .obtain(labelText, 0, labelText.length, textPaint, available)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setMaxLines(1)
                .build()
        } else {
            textLayout = null
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val drawable = logo ?: return

        if (iconOnly) {
            val left = (width - iconSizePx) / 2
            val top = (height - iconSizePx) / 2
            drawable.setBounds(left, top, left + iconSizePx, top + iconSizePx)
            drawable.draw(canvas)
            return
        }

        val layout = textLayout ?: return
        val textWidth = layout.getLineWidth(0)
        val groupWidth = iconSizePx + gapPx + textWidth
        val startX = (width - groupWidth) / 2f

        val iconTop = (height - iconSizePx) / 2
        val iconLeft = startX.roundToInt()
        drawable.setBounds(iconLeft, iconTop, iconLeft + iconSizePx, iconTop + iconSizePx)
        drawable.draw(canvas)

        val textX = startX + iconSizePx + gapPx
        val textY = (height - layout.height) / 2f
        canvas.save()
        canvas.translate(textX, textY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun colorsFor(theme: PaspoButtonTheme): ButtonColors =
        when (theme) {
            PaspoButtonTheme.BRAND -> ButtonColors(
                background = BRAND_COLOR,
                content = Color.WHITE,
                stroke = Color.TRANSPARENT
            )

            PaspoButtonTheme.LIGHT -> ButtonColors(
                background = Color.WHITE,
                content = 0xFF1F1F1F.toInt(),
                stroke = 0xFFE0E0E0.toInt()
            )

            PaspoButtonTheme.DARK -> ButtonColors(
                background = 0xFF131314.toInt(),
                content = 0xFFE3E3E3.toInt(),
                stroke = Color.TRANSPARENT
            )

            PaspoButtonTheme.NEUTRAL -> ButtonColors(
                background = 0xFFF2F2F2.toInt(),
                content = 0xFF1F1F1F.toInt(),
                stroke = Color.TRANSPARENT
            )
        }

    private fun withAlpha(
        color: Int,
        alpha: Int
    ): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private data class ButtonColors(
        val background: Int,
        val content: Int,
        val stroke: Int,
    )

    private companion object {
        const val ICON_SIZE_DP = 30f
        const val GAP_DP = 12f
        const val H_PADDING_DP = 16f
        const val MIN_HEIGHT_DP = 48f
        const val TEXT_SIZE_SP = 16f
        const val STROKE_DP = 1f
        const val PILL_RADIUS_DP = 1000f
        const val DISABLED_ALPHA = 0.6f
        const val RIPPLE_ALPHA = 40
        const val BRAND_COLOR = 0xFF1677FF.toInt()
    }
}
