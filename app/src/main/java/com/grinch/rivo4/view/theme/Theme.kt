package com.grinch.rivo4.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

const val KEY_CUSTOM_PRIMARY_COLOR: String = "custom_primary_color"
const val CUSTOM_PRIMARY_COLOR_UNSET: Int = -1

@Composable
fun Rivo4Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val context = LocalContext.current

    val dynamicColor = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    }
    val amoledMode = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
    }
    val customPrimaryInt = remember(settingsVersion) {
        prefs.getInt(KEY_CUSTOM_PRIMARY_COLOR, CUSTOM_PRIMARY_COLOR_UNSET)
    }
    val cardRoundness = remember(settingsVersion) {
        prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, RivoShapeDefaults.DefaultRoundness)
    }

    val colorScheme = remember(darkTheme, dynamicColor, amoledMode, customPrimaryInt) {
        val base = when {
            dynamicColor ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            customPrimaryInt != CUSTOM_PRIMARY_COLOR_UNSET ->
                rivoColorSchemeFromSeed(customPrimaryInt, darkTheme)
            darkTheme -> RivoDarkColorScheme
            else -> RivoLightColorScheme
        }
        if (darkTheme && amoledMode) base.toAmoledColorScheme() else base
    }

    val shapes = remember(cardRoundness) { rivoShapes(cardRoundness) }

    val callColors = remember(colorScheme, darkTheme) { rivoCallColors(colorScheme, darkTheme) }

    CompositionLocalProvider(
        LocalCallColors provides callColors,
        LocalCardRoundness provides cardRoundness
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = shapes,
            typography = RivoTypography,
            content = content
        )
    }
}
