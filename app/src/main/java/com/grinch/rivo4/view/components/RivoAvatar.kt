package com.grinch.rivo4.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.theme.RIVO_AVATAR_SHAPE_SQUIRCLE
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoMotion
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoAvatarShape
import org.koin.compose.koinInject
import kotlin.math.abs

@Immutable
data class RivoAvatarStyle(
    val showPicture: Boolean,
    val showFirstLetter: Boolean,
    val colorful: Boolean,
    val gradient: Boolean,
    val shapeIndex: Int,
    val shape: Shape
)

@Immutable
data class RivoAvatarColors(
    val container: Color,
    val content: Color
)

object RivoAvatarDefaults {
    const val HueCount: Int = 12

    val IconSize: Dp = 24.dp
    val BadgeSize: Dp = 18.dp
    val BadgeIconSize: Dp = 12.dp

    const val LightContainerSaturation: Float = 0.62f
    const val LightContainerLightness: Float = 0.82f
    const val LightContentSaturation: Float = 0.88f
    const val LightContentLightness: Float = 0.22f

    const val DarkContainerSaturation: Float = 0.34f
    const val DarkContainerLightness: Float = 0.26f
    const val DarkContentSaturation: Float = 0.80f
    const val DarkContentLightness: Float = 0.88f
}

val LocalRivoAvatarStyle: ProvidableCompositionLocal<RivoAvatarStyle?> =
    staticCompositionLocalOf { null }

@Composable
fun rememberRivoAvatarStyle(prefs: PreferenceManager = koinInject()): RivoAvatarStyle {
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val showPicture = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)
    }
    val showFirstLetter = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)
    }
    val colorful = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)
    }
    val gradient = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_GRADIENT_AVATARS, false)
    }
    val shapeIndex = remember(settingsVersion) {
        prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, RIVO_AVATAR_SHAPE_SQUIRCLE)
    }
    val shape = rivoAvatarShape(shapeIndex)
    return remember(showPicture, showFirstLetter, colorful, gradient, shapeIndex, shape) {
        RivoAvatarStyle(showPicture, showFirstLetter, colorful, gradient, shapeIndex, shape)
    }
}

@Composable
fun rivoAvatarStyle(): RivoAvatarStyle {
    val provided = LocalRivoAvatarStyle.current
    if (provided != null) return provided
    return rememberRivoAvatarStyle()
}

fun rivoAvatarHueIndex(name: String): Int =
    (abs(name.hashCode().toLong()) % RivoAvatarDefaults.HueCount).toInt()

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))

private fun rivoTintedAvatarColors(name: String, dark: Boolean): RivoAvatarColors {
    val hue = rivoAvatarHueIndex(name) * (360f / RivoAvatarDefaults.HueCount)
    return if (dark) {
        RivoAvatarColors(
            container = hslColor(
                hue,
                RivoAvatarDefaults.DarkContainerSaturation,
                RivoAvatarDefaults.DarkContainerLightness
            ),
            content = hslColor(
                hue,
                RivoAvatarDefaults.DarkContentSaturation,
                RivoAvatarDefaults.DarkContentLightness
            )
        )
    } else {
        RivoAvatarColors(
            container = hslColor(
                hue,
                RivoAvatarDefaults.LightContainerSaturation,
                RivoAvatarDefaults.LightContainerLightness
            ),
            content = hslColor(
                hue,
                RivoAvatarDefaults.LightContentSaturation,
                RivoAvatarDefaults.LightContentLightness
            )
        )
    }
}

@Composable
fun rivoAvatarColors(name: String, colorful: Boolean = true): RivoAvatarColors {
    val scheme = MaterialTheme.colorScheme
    val tinted = colorful && name.any { it.isLetter() }
    val dark = scheme.surface.luminance() < 0.5f
    val neutralContainer = scheme.secondaryContainer
    val neutralContent = scheme.onSecondaryContainer
    return remember(name, tinted, dark, neutralContainer, neutralContent) {
        if (tinted) {
            rivoTintedAvatarColors(name, dark)
        } else {
            RivoAvatarColors(neutralContainer, neutralContent)
        }
    }
}

private fun gradientAvatarColors(name: String, dark: Boolean): List<Color> {
    val baseHue = rivoAvatarHueIndex(name) * (360f / RivoAvatarDefaults.HueCount)
    val accent1 = (baseHue + 25f) % 360f
    val accent2 = (baseHue + 175f) % 360f
    val accent3 = (baseHue + 280f) % 360f
    return if (dark) {
        listOf(
            hslColor(accent1, 0.65f, 0.38f),
            hslColor(accent2, 0.55f, 0.24f),
            hslColor(baseHue, 0.75f, 0.42f),
            hslColor(accent3, 0.60f, 0.30f)
        )
    } else {
        listOf(
            hslColor(accent1, 0.78f, 0.82f),
            hslColor(accent2, 0.65f, 0.58f),
            hslColor(baseHue, 0.85f, 0.88f),
            hslColor(accent3, 0.70f, 0.68f)
        )
    }
}

private fun contactInitials(name: String, useTwo: Boolean): String {
    val letters = name.filter { it.isLetter() }
    if (letters.isEmpty()) return ""
    if (!useTwo) return letters.first().uppercase()
    val words = name.trim().split(Regex("\\s+")).filter { it.any { c -> c.isLetter() } }
    return if (words.size >= 2) {
        words.take(2).joinToString("") { word ->
            word.first { it.isLetter() }.uppercase()
        }
    } else {
        letters.take(2).uppercase()
    }
}

@Composable
fun RivoAvatar(
    name: String,
    photoUri: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    badgeIcon: ImageVector? = null,
    badgeColor: Color? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    style: RivoAvatarStyle = rivoAvatarStyle(),
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    morphOnPress: Boolean = false,
    morphSelected: Boolean = false,
    morphStart: RoundedPolygon = RivoMaterialShapes.AvatarMorphStart,
    morphEnd: RoundedPolygon = RivoMaterialShapes.AvatarMorphEnd
) {
    val ownInteractionSource = remember { MutableInteractionSource() }
    val resolvedInteractionSource = interactionSource ?: ownInteractionSource
    val pressed by resolvedInteractionSource.collectIsPressedAsState()

    val morphEnabled = morphOnPress || morphSelected
    val morphTarget = if (morphSelected || (morphOnPress && pressed)) 1f else 0f
    val morphProgress by animateFloatAsState(
        targetValue = morphTarget,
        animationSpec = RivoMotion.shapeMorph(),
        label = "RivoAvatarMorph"
    )
    val morphShape = rememberRivoMorphShape(morphStart, morphEnd) { morphProgress }

    val avatarShape = when {
        morphEnabled -> morphShape
        shape != null -> shape
        else -> style.shape
    }

    val colors = rivoAvatarColors(name, style.colorful)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val gradientBrush = if (style.gradient) {
        remember(name, dark) {
            Brush.radialGradient(
                colors = gradientAvatarColors(name, dark),
                center = Offset(0.3f, 0.2f),
                radius = 1.15f
            )
        }
    } else null
    val hasLetters = name.any { it.isLetter() }
    val description = contentDescription

    val rootModifier = modifier
        .then(
            if (description != null) {
                Modifier.semantics { this.contentDescription = description }
            } else {
                Modifier
            }
        )
        .then(
            if (onClick != null) {
                Modifier
                    .clip(avatarShape)
                    .clickable(
                        interactionSource = resolvedInteractionSource,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        )

    val backgroundModifier = if (gradientBrush != null) {
        Modifier.background(gradientBrush, avatarShape)
    } else {
        Modifier.background(colors.container, avatarShape)
    }

    Box(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backgroundModifier)
                .clip(avatarShape),
            contentAlignment = Alignment.Center
        ) {
            if (style.showPicture && !photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(RivoAvatarDefaults.IconSize)
                )
            } else if (style.showFirstLetter && hasLetters) {
                Text(
                    text = contactInitials(name, style.gradient),
                    style = textStyle,
                    color = colors.content
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(RivoAvatarDefaults.IconSize)
                )
            }
        }

        if (badgeIcon != null) {
            Surface(
                modifier = Modifier
                    .size(RivoAvatarDefaults.BadgeSize)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = RivoElevation.Raised
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(RivoAvatarDefaults.BadgeIconSize)
                    )
                }
            }
        }
    }
}
