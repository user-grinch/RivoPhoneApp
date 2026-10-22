package com.grinch.rivo4.view.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.controller.util.PreferenceManager

/** Named system font resolved through [Typeface.create]; keeps a single mapping for the whole app. */
private fun systemFontFamily(name: String): FontFamily =
    FontFamily(Typeface.create(name, Typeface.NORMAL))

fun rivoFontFamilyFromId(id: String?): FontFamily = when (id) {
    PreferenceManager.FONT_FAMILY_SERIF -> FontFamily.Serif
    PreferenceManager.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
    PreferenceManager.FONT_FAMILY_CURSIVE -> FontFamily.Cursive
    PreferenceManager.FONT_FAMILY_CASUAL -> systemFontFamily("casual")
    PreferenceManager.FONT_FAMILY_CONDENSED -> systemFontFamily("sans-serif-condensed")
    PreferenceManager.FONT_FAMILY_LIGHT -> systemFontFamily("sans-serif-light")
    PreferenceManager.FONT_FAMILY_MEDIUM -> systemFontFamily("sans-serif-medium")
    PreferenceManager.FONT_FAMILY_BLACK -> systemFontFamily("sans-serif-black")
    else -> FontFamily.SansSerif
}

private fun rivoTextStyle(
    fontSize: Float,
    lineHeight: Float,
    letterSpacing: Float,
    fontWeight: FontWeight,
    fontFamily: FontFamily,
    scale: Float
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = (fontSize * scale).sp,
    lineHeight = (lineHeight * scale).sp,
    letterSpacing = letterSpacing.sp
)

fun rivoTypography(
    fontFamily: FontFamily = FontFamily.Default,
    scale: Float = 1f
): Typography = Typography(
    displayLarge = rivoTextStyle(57f, 64f, -0.2f, FontWeight.Normal, fontFamily, scale),
    displayMedium = rivoTextStyle(45f, 52f, 0f, FontWeight.Normal, fontFamily, scale),
    displaySmall = rivoTextStyle(36f, 44f, 0f, FontWeight.Normal, fontFamily, scale),
    headlineLarge = rivoTextStyle(32f, 40f, 0f, FontWeight.Normal, fontFamily, scale),
    headlineMedium = rivoTextStyle(28f, 36f, 0f, FontWeight.Normal, fontFamily, scale),
    headlineSmall = rivoTextStyle(24f, 32f, 0f, FontWeight.Normal, fontFamily, scale),
    titleLarge = rivoTextStyle(22f, 28f, 0f, FontWeight.Normal, fontFamily, scale),
    titleMedium = rivoTextStyle(16f, 24f, 0.2f, FontWeight.Medium, fontFamily, scale),
    titleSmall = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Medium, fontFamily, scale),
    bodyLarge = rivoTextStyle(16f, 24f, 0.5f, FontWeight.Normal, fontFamily, scale),
    bodyMedium = rivoTextStyle(14f, 20f, 0.2f, FontWeight.Normal, fontFamily, scale),
    bodySmall = rivoTextStyle(12f, 16f, 0.4f, FontWeight.Normal, fontFamily, scale),
    labelLarge = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Medium, fontFamily, scale),
    labelMedium = rivoTextStyle(12f, 16f, 0.5f, FontWeight.Medium, fontFamily, scale),
    labelSmall = rivoTextStyle(11f, 16f, 0.5f, FontWeight.Medium, fontFamily, scale),
    displayLargeEmphasized = rivoTextStyle(57f, 64f, 0f, FontWeight.Medium, fontFamily, scale),
    displayMediumEmphasized = rivoTextStyle(45f, 52f, 0f, FontWeight.Medium, fontFamily, scale),
    displaySmallEmphasized = rivoTextStyle(36f, 44f, 0f, FontWeight.Medium, fontFamily, scale),
    headlineLargeEmphasized = rivoTextStyle(32f, 40f, 0f, FontWeight.Medium, fontFamily, scale),
    headlineMediumEmphasized = rivoTextStyle(28f, 36f, 0f, FontWeight.Medium, fontFamily, scale),
    headlineSmallEmphasized = rivoTextStyle(24f, 32f, 0f, FontWeight.Medium, fontFamily, scale),
    titleLargeEmphasized = rivoTextStyle(22f, 28f, 0f, FontWeight.Medium, fontFamily, scale),
    titleMediumEmphasized = rivoTextStyle(16f, 24f, 0.15f, FontWeight.Bold, fontFamily, scale),
    titleSmallEmphasized = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Bold, fontFamily, scale),
    bodyLargeEmphasized = rivoTextStyle(16f, 24f, 0.15f, FontWeight.Medium, fontFamily, scale),
    bodyMediumEmphasized = rivoTextStyle(14f, 20f, 0.25f, FontWeight.Medium, fontFamily, scale),
    bodySmallEmphasized = rivoTextStyle(12f, 16f, 0.4f, FontWeight.Medium, fontFamily, scale),
    labelLargeEmphasized = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Bold, fontFamily, scale),
    labelMediumEmphasized = rivoTextStyle(12f, 16f, 0.5f, FontWeight.Bold, fontFamily, scale),
    labelSmallEmphasized = rivoTextStyle(11f, 16f, 0.5f, FontWeight.Bold, fontFamily, scale)
)

val RivoTypography: Typography = rivoTypography()
