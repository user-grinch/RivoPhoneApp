package com.grinch.rivo4.view.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

private val FabDefaultSize = 56.dp

@Composable
fun RivoFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(
        defaultElevation = 4.dp,
        pressedElevation = 6.dp
    ),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val isBlurEnabled = remember(settingsState) { prefs.isUiBlurEnabled() }

    if (isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val density = LocalDensity.current
        val blurRadiusPx = with(density) { 36.dp.toPx() }
        val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

        Box(
            modifier = modifier
                .shadow(elevation = 6.dp, shape = shape, clip = false)
                .defaultMinSize(minWidth = FabDefaultSize, minHeight = FabDefaultSize),
            contentAlignment = Alignment.Center
        ) {
            // Dense frosted glass blur backdrop layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .graphicsLayer {
                        renderEffect = RenderEffect.createBlurEffect(
                            blurRadiusPx, blurRadiusPx,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                    .background(containerColor.copy(alpha = 0.85f))
            )

            // Foreground interactive content layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .clickable(
                        interactionSource = resolvedInteractionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }
            }
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
            content = content
        )
    }
}
