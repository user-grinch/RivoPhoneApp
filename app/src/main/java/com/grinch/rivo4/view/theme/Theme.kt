package com.grinch.rivo4.view.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.LocalRivoSurfaceStyle
import com.grinch.rivo4.view.components.rememberRivoSurfaceStyle
import org.koin.compose.koinInject

const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color"
const val CUSTOM_PRIMARY_COLOR_UNSET: Int = -1

val LocalNavBarStyle: ProvidableCompositionLocal<Int> =
    staticCompositionLocalOf { PreferenceManager.NAV_BAR_STYLE_STANDARD }

@Composable
fun Rivo4Theme(
    darkThemeOverride: Boolean? = null,
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val dynamicColor = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    }
    val amoledMode = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
    }
    val themeMode = remember(settingsVersion) {
        prefs.getInt(PreferenceManager.KEY_THEME_MODE, PreferenceManager.THEME_MODE_SYSTEM)
    }
    val paletteStyle = remember(settingsVersion) {
        rivoPaletteStyleFromId(
            prefs.getString(
                PreferenceManager.KEY_DYNAMIC_VARIANT,
                RivoPaletteStyle.TonalSpot.id
            )
        )
    }
    val darkTheme = darkThemeOverride ?: when (themeMode) {
        PreferenceManager.THEME_MODE_LIGHT -> false
        PreferenceManager.THEME_MODE_DARK -> true
        else -> systemDark
    }
    val customPrimaryInt = remember(settingsVersion) {
        prefs.getInt(KEY_CUSTOM_PRIMARY_COLOR, CUSTOM_PRIMARY_COLOR_UNSET)
    }
    val cardRoundness = remember(settingsVersion) {
        prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, RivoShapeDefaults.DefaultRoundness).coerceAtLeast(5)
    }

    val colorScheme = remember(dynamicColor, amoledMode, customPrimaryInt, darkTheme, paletteStyle) {
        val base = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val systemScheme = if (darkTheme) {
                    androidx.compose.material3.dynamicDarkColorScheme(context)
                } else {
                    androidx.compose.material3.dynamicLightColorScheme(context)
                }
                if (paletteStyle == RivoPaletteStyle.TonalSpot) {
                    systemScheme
                } else {
                    rivoColorSchemeFromSeed(systemScheme.primary.toArgb(), darkTheme, paletteStyle)
                }
            }

            customPrimaryInt != CUSTOM_PRIMARY_COLOR_UNSET ->
                rivoColorSchemeFromSeed(customPrimaryInt, darkTheme)

            darkTheme -> RivoDarkColorScheme
            else -> RivoLightColorScheme
        }
        if (darkTheme && amoledMode) base.toAmoledColorScheme() else base
    }

    val shapes = remember(cardRoundness) { rivoShapes(cardRoundness) }
    val callColors = remember(colorScheme, darkTheme) { rivoCallColors(colorScheme, darkTheme) }

    val typography = remember(settingsVersion) {
        val useSystemFont = prefs.getBoolean(PreferenceManager.KEY_USE_SYSTEM_FONT, true)
        val fontFamily = if (useSystemFont) {
            FontFamily.Default
        } else {
            rivoFontFamilyFromId(
                prefs.getString(PreferenceManager.KEY_FONT_FAMILY, PreferenceManager.FONT_FAMILY_SANS_SERIF)
            )
        }
        val scale = when (prefs.getInt(PreferenceManager.KEY_FONT_SIZE, PreferenceManager.FONT_SIZE_MEDIUM)) {
            PreferenceManager.FONT_SIZE_SMALL -> 0.9f
            PreferenceManager.FONT_SIZE_LARGE -> 1.12f
            else -> 1f
        }
        rivoTypography(fontFamily, scale)
    }

    val navBarStyle = remember(settingsVersion) {
        prefs.getInt(PreferenceManager.KEY_NAV_BAR_STYLE, PreferenceManager.NAV_BAR_STYLE_STANDARD)
    }

    val surfaceStyle = rememberRivoSurfaceStyle(prefs)

    CompositionLocalProvider(
        LocalCallColors provides callColors,
        LocalCardRoundness provides cardRoundness,
        LocalNavBarStyle provides navBarStyle,
        LocalRivoSurfaceStyle provides surfaceStyle
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}
