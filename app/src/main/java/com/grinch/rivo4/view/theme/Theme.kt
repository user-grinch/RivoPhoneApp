package com.grinch.rivo4.view.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun Rivo4Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsState by prefs.settingsChanged.collectAsState()
    
    val dynamicColor = prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    val amoledMode = prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
    val customPrimaryInt = prefs.getInt("custom_primary_color", -1)

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    var colorScheme = baseColorScheme

    if (darkTheme && amoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF151515),
            surfaceContainerHighest = Color(0xFF1A1A1A),
            surfaceContainerLowest = Color.Black,
            surfaceVariant = Color(0xFF1A1A1A)
        )
    }

    if (!dynamicColor && customPrimaryInt != -1) {
        val primaryColor = Color(customPrimaryInt)
        val surfaceColor = colorScheme.surface
        
        val onPrimaryColor = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White
        
        val secondaryColor = primaryColor.shiftColorForSecondary(isDark = darkTheme)
        val onSecondaryColor = if (secondaryColor.luminance() > 0.5f) Color.Black else Color.White

        fun blendWithSurface(color: Color, alpha: Float): Color =
            color.copy(alpha = alpha).compositeOver(surfaceColor)

        colorScheme = colorScheme.copy(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            
            primaryContainer = if (darkTheme) primaryColor.withLightness(0.30f) else primaryColor.withLightness(0.90f),
            onPrimaryContainer = if (darkTheme) primaryColor.withLightness(0.90f) else primaryColor.withLightness(0.10f),
            
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            
            secondaryContainer = if (darkTheme) secondaryColor.withLightness(0.25f) else secondaryColor.withLightness(0.85f),
            onSecondaryContainer = if (darkTheme) secondaryColor.withLightness(0.85f) else secondaryColor.withLightness(0.10f),
            
            outline = blendWithSurface(primaryColor, 0.50f),
            outlineVariant = blendWithSurface(primaryColor, 0.20f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// --- Helper Extension Functions ---
private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

private fun Color.withLightness(targetLightness: Float): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.RGBToHSL(
        (red * 255).toInt(), 
        (green * 255).toInt(), 
        (blue * 255).toInt(), 
        hsl
    )
    hsl[2] = targetLightness.coerceIn(0f, 1f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

private fun Color.shiftColorForSecondary(isDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (red * 255).toInt(), 
        (green * 255).toInt(), 
        (blue * 255).toInt(), 
        hsl
    )
    
    // Drop saturation by 15% to make it less aggressive than the primary color
    hsl[1] = (hsl[1] * 0.85f).coerceIn(0f, 1f) 
    
    // Shift lightness up or down slightly depending on the theme
    hsl[2] = if (isDark) {
        (hsl[2] * 1.1f).coerceIn(0f, 1f)
    } else {
        (hsl[2] * 0.9f).coerceIn(0f, 1f)
    }
    
    return Color(ColorUtils.HSLToColor(hsl))
}