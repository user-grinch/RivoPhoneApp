package com.grinch.rivo4.view.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun rivoTextStyle(
    fontSize: Float,
    lineHeight: Float,
    letterSpacing: Float,
    fontWeight: FontWeight
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val RivoTypography: Typography = Typography(
    displayLarge = rivoTextStyle(57f, 64f, -0.2f, FontWeight.Normal),
    displayMedium = rivoTextStyle(45f, 52f, 0f, FontWeight.Normal),
    displaySmall = rivoTextStyle(36f, 44f, 0f, FontWeight.Normal),
    headlineLarge = rivoTextStyle(32f, 40f, 0f, FontWeight.Normal),
    headlineMedium = rivoTextStyle(28f, 36f, 0f, FontWeight.Normal),
    headlineSmall = rivoTextStyle(24f, 32f, 0f, FontWeight.Normal),
    titleLarge = rivoTextStyle(22f, 28f, 0f, FontWeight.Normal),
    titleMedium = rivoTextStyle(16f, 24f, 0.2f, FontWeight.Medium),
    titleSmall = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Medium),
    bodyLarge = rivoTextStyle(16f, 24f, 0.5f, FontWeight.Normal),
    bodyMedium = rivoTextStyle(14f, 20f, 0.2f, FontWeight.Normal),
    bodySmall = rivoTextStyle(12f, 16f, 0.4f, FontWeight.Normal),
    labelLarge = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Medium),
    labelMedium = rivoTextStyle(12f, 16f, 0.5f, FontWeight.Medium),
    labelSmall = rivoTextStyle(11f, 16f, 0.5f, FontWeight.Medium),
    displayLargeEmphasized = rivoTextStyle(57f, 64f, 0f, FontWeight.Medium),
    displayMediumEmphasized = rivoTextStyle(45f, 52f, 0f, FontWeight.Medium),
    displaySmallEmphasized = rivoTextStyle(36f, 44f, 0f, FontWeight.Medium),
    headlineLargeEmphasized = rivoTextStyle(32f, 40f, 0f, FontWeight.Medium),
    headlineMediumEmphasized = rivoTextStyle(28f, 36f, 0f, FontWeight.Medium),
    headlineSmallEmphasized = rivoTextStyle(24f, 32f, 0f, FontWeight.Medium),
    titleLargeEmphasized = rivoTextStyle(22f, 28f, 0f, FontWeight.Medium),
    titleMediumEmphasized = rivoTextStyle(16f, 24f, 0.15f, FontWeight.Bold),
    titleSmallEmphasized = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Bold),
    bodyLargeEmphasized = rivoTextStyle(16f, 24f, 0.15f, FontWeight.Medium),
    bodyMediumEmphasized = rivoTextStyle(14f, 20f, 0.25f, FontWeight.Medium),
    bodySmallEmphasized = rivoTextStyle(12f, 16f, 0.4f, FontWeight.Medium),
    labelLargeEmphasized = rivoTextStyle(14f, 20f, 0.1f, FontWeight.Bold),
    labelMediumEmphasized = rivoTextStyle(12f, 16f, 0.5f, FontWeight.Bold),
    labelSmallEmphasized = rivoTextStyle(11f, 16f, 0.5f, FontWeight.Bold)
)
