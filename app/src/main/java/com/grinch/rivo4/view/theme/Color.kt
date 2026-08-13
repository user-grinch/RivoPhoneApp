package com.grinch.rivo4.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val RIVO_BRAND_SEED: Int = 0xFF0061A4.toInt()

val RivoLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0461A3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE4FF),
    onPrimaryContainer = Color(0xFF001C38),
    inversePrimary = Color(0xFF9BCAFF),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3F7),
    onSecondaryContainer = Color(0xFF101C2A),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3D9FF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF0461A3),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFEFF0F6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceContainer = Color(0xFFECEDF3),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8),
    surfaceContainerLow = Color(0xFFF2F4FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD8DAE0),
    primaryFixed = Color(0xFFCFE4FF),
    primaryFixedDim = Color(0xFF9BCAFF),
    onPrimaryFixed = Color(0xFF001C38),
    onPrimaryFixedVariant = Color(0xFF004880),
    secondaryFixed = Color(0xFFD6E3F7),
    secondaryFixedDim = Color(0xFFBBC8DB),
    onSecondaryFixed = Color(0xFF101C2A),
    onSecondaryFixedVariant = Color(0xFF3C4858),
    tertiaryFixed = Color(0xFFF3D9FF),
    tertiaryFixedDim = Color(0xFFD7BEE5),
    onTertiaryFixed = Color(0xFF251432),
    onTertiaryFixedVariant = Color(0xFF523F5F)
)

val RivoDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9BCAFF),
    onPrimary = Color(0xFF00325B),
    primaryContainer = Color(0xFF004880),
    onPrimaryContainer = Color(0xFFCFE4FF),
    inversePrimary = Color(0xFF0461A3),
    secondary = Color(0xFFBBC8DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD6E3F7),
    tertiary = Color(0xFFD7BEE5),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3D9FF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0),
    surfaceTint = Color(0xFF9BCAFF),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF36393E),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceDim = Color(0xFF111418),
    primaryFixed = Color(0xFFCFE4FF),
    primaryFixedDim = Color(0xFF9BCAFF),
    onPrimaryFixed = Color(0xFF001C38),
    onPrimaryFixedVariant = Color(0xFF004880),
    secondaryFixed = Color(0xFFD6E3F7),
    secondaryFixedDim = Color(0xFFBBC8DB),
    onSecondaryFixed = Color(0xFF101C2A),
    onSecondaryFixedVariant = Color(0xFF3C4858),
    tertiaryFixed = Color(0xFFF3D9FF),
    tertiaryFixedDim = Color(0xFFD7BEE5),
    onTertiaryFixed = Color(0xFF251432),
    onTertiaryFixedVariant = Color(0xFF523F5F)
)

private const val AMOLED_OUTLINE_VARIANT = 0xFF5A5E66

fun ColorScheme.toAmoledColorScheme(): ColorScheme = copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceBright = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFC9CCD4),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF9096A0),
    outlineVariant = Color(AMOLED_OUTLINE_VARIANT)
)

private const val D65_XN = 0.95047f
private const val D65_YN = 1.00000f
private const val D65_ZN = 1.08883f
private const val LAB_EPSILON = 0.008856f
private const val LAB_KAPPA = 7.787f

private fun srgbToLinear(channel: Float): Float =
    if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

private fun linearToSrgb(channel: Float): Float =
    if (channel <= 0.0031308f) channel * 12.92f else 1.055f * channel.pow(1f / 2.4f) - 0.055f

private fun labF(t: Float): Float =
    if (t > LAB_EPSILON) t.pow(1f / 3f) else LAB_KAPPA * t + 16f / 116f

private fun labFInverse(t: Float): Float {
    val cubed = t * t * t
    return if (cubed > LAB_EPSILON) cubed else (t - 16f / 116f) / LAB_KAPPA
}

private fun colorToLch(color: Color): FloatArray {
    val r = srgbToLinear(color.red)
    val g = srgbToLinear(color.green)
    val b = srgbToLinear(color.blue)

    val x = (0.4124564f * r + 0.3575761f * g + 0.1804375f * b) / D65_XN
    val y = (0.2126729f * r + 0.7151522f * g + 0.0721750f * b) / D65_YN
    val z = (0.0193339f * r + 0.1191920f * g + 0.9503041f * b) / D65_ZN

    val fx = labF(x)
    val fy = labF(y)
    val fz = labF(z)

    val lightness = 116f * fy - 16f
    val aStar = 500f * (fx - fy)
    val bStar = 200f * (fy - fz)

    val chroma = sqrt(aStar * aStar + bStar * bStar)
    var hue = Math.toDegrees(atan2(bStar.toDouble(), aStar.toDouble())).toFloat()
    if (hue < 0f) hue += 360f

    return floatArrayOf(lightness.coerceIn(0f, 100f), chroma, hue)
}

private fun lchToLinearRgb(lightness: Float, chroma: Float, hueDegrees: Float): FloatArray {
    val hueRadians = Math.toRadians(hueDegrees.toDouble())
    val aStar = (chroma * cos(hueRadians)).toFloat()
    val bStar = (chroma * sin(hueRadians)).toFloat()

    val fy = (lightness + 16f) / 116f
    val fx = fy + aStar / 500f
    val fz = fy - bStar / 200f

    val x = labFInverse(fx) * D65_XN
    val y = labFInverse(fy) * D65_YN
    val z = labFInverse(fz) * D65_ZN

    return floatArrayOf(
        3.2404542f * x - 1.5371385f * y - 0.4985314f * z,
        -0.9692660f * x + 1.8760108f * y + 0.0415560f * z,
        0.0556434f * x - 0.2040259f * y + 1.0572252f * z
    )
}

private fun toneColor(lightness: Float, chroma: Float, hueDegrees: Float): Color {
    var currentChroma = chroma.coerceAtLeast(0f)
    while (true) {
        val linear = lchToLinearRgb(lightness, currentChroma, hueDegrees)
        val inGamut = linear.all { it >= -0.0015f && it <= 1.0015f }
        if (inGamut || currentChroma <= 0f) {
            return Color(
                red = linearToSrgb(linear[0].coerceIn(0f, 1f)),
                green = linearToSrgb(linear[1].coerceIn(0f, 1f)),
                blue = linearToSrgb(linear[2].coerceIn(0f, 1f))
            )
        }
        currentChroma = (currentChroma - 0.5f).coerceAtLeast(0f)
    }
}

private fun harmonizeHue(sourceHue: Float, targetHue: Float, maxShiftDegrees: Float): Float {
    val delta = ((targetHue - sourceHue + 540f) % 360f) - 180f
    val magnitude = kotlin.math.min(abs(delta) * 0.5f, maxShiftDegrees)
    val signed = if (delta < 0f) -magnitude else magnitude
    return (sourceHue + signed + 360f) % 360f
}

private const val NEUTRAL_CHROMA_CAP = 4f
private const val NEUTRAL_VARIANT_CHROMA_CAP = 8f

fun rivoColorSchemeFromSeed(seedArgb: Int, darkTheme: Boolean): ColorScheme {
    val seed = colorToLch(Color(seedArgb))
    val hue = seed[2]
    val primaryChroma = seed[1].coerceIn(0f, 80f)
    val secondaryChroma = primaryChroma / 3f
    val tertiaryHue = (hue + 60f) % 360f
    val tertiaryChroma = primaryChroma / 2f
    val neutralChroma = kotlin.math.min(primaryChroma / 8f, NEUTRAL_CHROMA_CAP)
    val neutralVariantChroma = kotlin.math.min(primaryChroma / 6f, NEUTRAL_VARIANT_CHROMA_CAP)

    fun p(tone: Float) = toneColor(tone, primaryChroma, hue)
    fun s(tone: Float) = toneColor(tone, secondaryChroma, hue)
    fun t(tone: Float) = toneColor(tone, tertiaryChroma, tertiaryHue)
    fun n(tone: Float) = toneColor(tone, neutralChroma, hue)
    fun nv(tone: Float) = toneColor(tone, neutralVariantChroma, hue)

    val primaryFixed = p(90f)
    val primaryFixedDim = p(80f)
    val onPrimaryFixed = p(10f)
    val onPrimaryFixedVariant = p(30f)
    val secondaryFixed = s(90f)
    val secondaryFixedDim = s(80f)
    val onSecondaryFixed = s(10f)
    val onSecondaryFixedVariant = s(30f)
    val tertiaryFixed = t(90f)
    val tertiaryFixedDim = t(80f)
    val onTertiaryFixed = t(10f)
    val onTertiaryFixedVariant = t(30f)

    return if (darkTheme) {
        darkColorScheme(
            primary = p(80f),
            onPrimary = p(20f),
            primaryContainer = p(30f),
            onPrimaryContainer = p(90f),
            inversePrimary = p(40f),
            secondary = s(80f),
            onSecondary = s(20f),
            secondaryContainer = s(30f),
            onSecondaryContainer = s(90f),
            tertiary = t(80f),
            onTertiary = t(20f),
            tertiaryContainer = t(30f),
            onTertiaryContainer = t(90f),
            background = n(6f),
            onBackground = n(90f),
            surface = n(6f),
            onSurface = n(90f),
            surfaceVariant = nv(30f),
            onSurfaceVariant = nv(80f),
            surfaceTint = p(80f),
            inverseSurface = n(90f),
            inverseOnSurface = n(20f),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            outline = nv(60f),
            outlineVariant = nv(40f),
            scrim = Color(0xFF000000),
            surfaceBright = n(24f),
            surfaceContainer = n(12f),
            surfaceContainerHigh = n(17f),
            surfaceContainerHighest = n(22f),
            surfaceContainerLow = n(10f),
            surfaceContainerLowest = n(4f),
            surfaceDim = n(6f),
            primaryFixed = primaryFixed,
            primaryFixedDim = primaryFixedDim,
            onPrimaryFixed = onPrimaryFixed,
            onPrimaryFixedVariant = onPrimaryFixedVariant,
            secondaryFixed = secondaryFixed,
            secondaryFixedDim = secondaryFixedDim,
            onSecondaryFixed = onSecondaryFixed,
            onSecondaryFixedVariant = onSecondaryFixedVariant,
            tertiaryFixed = tertiaryFixed,
            tertiaryFixedDim = tertiaryFixedDim,
            onTertiaryFixed = onTertiaryFixed,
            onTertiaryFixedVariant = onTertiaryFixedVariant
        )
    } else {
        lightColorScheme(
            primary = p(40f),
            onPrimary = p(100f),
            primaryContainer = p(90f),
            onPrimaryContainer = p(10f),
            inversePrimary = p(80f),
            secondary = s(40f),
            onSecondary = s(100f),
            secondaryContainer = s(90f),
            onSecondaryContainer = s(10f),
            tertiary = t(40f),
            onTertiary = t(100f),
            tertiaryContainer = t(90f),
            onTertiaryContainer = t(10f),
            background = n(98f),
            onBackground = n(10f),
            surface = n(98f),
            onSurface = n(10f),
            surfaceVariant = nv(90f),
            onSurfaceVariant = nv(30f),
            surfaceTint = p(40f),
            inverseSurface = n(20f),
            inverseOnSurface = n(95f),
            error = Color(0xFFB3261E),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = nv(50f),
            outlineVariant = nv(80f),
            scrim = Color(0xFF000000),
            surfaceBright = n(98f),
            surfaceContainer = n(94f),
            surfaceContainerHigh = n(92f),
            surfaceContainerHighest = n(90f),
            surfaceContainerLow = n(96f),
            surfaceContainerLowest = n(100f),
            surfaceDim = n(87f),
            primaryFixed = primaryFixed,
            primaryFixedDim = primaryFixedDim,
            onPrimaryFixed = onPrimaryFixed,
            onPrimaryFixedVariant = onPrimaryFixedVariant,
            secondaryFixed = secondaryFixed,
            secondaryFixedDim = secondaryFixedDim,
            onSecondaryFixed = onSecondaryFixed,
            onSecondaryFixedVariant = onSecondaryFixedVariant,
            tertiaryFixed = tertiaryFixed,
            tertiaryFixedDim = tertiaryFixedDim,
            onTertiaryFixed = onTertiaryFixed,
            onTertiaryFixedVariant = onTertiaryFixedVariant
        )
    }
}

@Immutable
data class CallColors(
    val answer: Color,
    val onAnswer: Color,
    val answerContainer: Color,
    val onAnswerContainer: Color,
    val decline: Color,
    val onDecline: Color,
    val declineContainer: Color,
    val onDeclineContainer: Color,
    val hold: Color,
    val onHold: Color,
    val holdContainer: Color,
    val onHoldContainer: Color
)

private val ANSWER_SEED_LCH = colorToLch(Color(0xFF1B8B3A))
private const val ANSWER_CHROMA = 52f
private const val ANSWER_MAX_HUE_SHIFT = 15f
private const val MIN_MEANINGFUL_CHROMA = 4f

fun rivoCallColors(colorScheme: ColorScheme, darkTheme: Boolean): CallColors {
    val primaryLch = colorToLch(colorScheme.primary)
    val baseHue = ANSWER_SEED_LCH[2]
    val hue = if (primaryLch[1] < MIN_MEANINGFUL_CHROMA) {
        baseHue
    } else {
        harmonizeHue(baseHue, primaryLch[2], ANSWER_MAX_HUE_SHIFT)
    }

    return if (darkTheme) {
        CallColors(
            answer = toneColor(80f, ANSWER_CHROMA, hue),
            onAnswer = toneColor(20f, ANSWER_CHROMA, hue),
            answerContainer = toneColor(30f, ANSWER_CHROMA, hue),
            onAnswerContainer = toneColor(90f, ANSWER_CHROMA, hue),
            decline = colorScheme.error,
            onDecline = colorScheme.onError,
            declineContainer = colorScheme.errorContainer,
            onDeclineContainer = colorScheme.onErrorContainer,
            hold = colorScheme.tertiary,
            onHold = colorScheme.onTertiary,
            holdContainer = colorScheme.tertiaryContainer,
            onHoldContainer = colorScheme.onTertiaryContainer
        )
    } else {
        CallColors(
            answer = toneColor(40f, ANSWER_CHROMA, hue),
            onAnswer = toneColor(100f, ANSWER_CHROMA, hue),
            answerContainer = toneColor(90f, ANSWER_CHROMA, hue),
            onAnswerContainer = toneColor(10f, ANSWER_CHROMA, hue),
            decline = colorScheme.error,
            onDecline = colorScheme.onError,
            declineContainer = colorScheme.errorContainer,
            onDeclineContainer = colorScheme.onErrorContainer,
            hold = colorScheme.tertiary,
            onHold = colorScheme.onTertiary,
            holdContainer = colorScheme.tertiaryContainer,
            onHoldContainer = colorScheme.onTertiaryContainer
        )
    }
}

val LocalCallColors: ProvidableCompositionLocal<CallColors> = staticCompositionLocalOf {
    rivoCallColors(RivoLightColorScheme, darkTheme = false)
}

val MaterialTheme.callColors: CallColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCallColors.current
