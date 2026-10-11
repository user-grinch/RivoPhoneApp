package com.grinch.rivo4.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.grinch.rivo4.R
import com.grinch.rivo4.view.theme.RivoDurations

private const val TopScrimFraction = 0.36f
private const val BottomScrimFraction = 0.54f

@Composable
fun ExpressiveBackground(photoUri: String?, backgroundUri: String? = null) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface

    val hasCustomBackground = !backgroundUri.isNullOrEmpty()
    var customFailed by remember(backgroundUri) { mutableStateOf(false) }

    val customVisible = hasCustomBackground && !customFailed
    val photoVisible = !photoUri.isNullOrEmpty() && !customVisible

    val request = remember(backgroundUri, context) {
        if (backgroundUri.isNullOrEmpty()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(backgroundUri)
                .crossfade(RivoDurations.Long1)
                .build()
        }
    }

    val baseModifier = if (isDark) {
        Modifier.fillMaxSize().background(surface)
    } else {
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to scheme.surface,
                0.5f to scheme.surfaceContainerLowest,
                1f to scheme.surfaceContainerLow
            )
        )
    }

    Box(modifier = baseModifier) {

        if (photoVisible) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(56.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDark) 0.42f else 0.22f
            )
        }

        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { customFailed = false },
                onError = { customFailed = true },
                contentScale = ContentScale.Crop
            )
        }

        if (customVisible || photoVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(TopScrimFraction)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to surface.copy(alpha = if (isDark) 0.88f else 0.95f),
                            0.5f to surface.copy(alpha = if (isDark) 0.60f else 0.75f),
                            1f to surface.copy(alpha = 0f)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(BottomScrimFraction)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to surface.copy(alpha = 0f),
                            0.34f to surface.copy(alpha = if (isDark) 0.42f else 0.65f),
                            0.72f to surface.copy(alpha = if (isDark) 0.88f else 0.94f),
                            1f to surface.copy(alpha = if (isDark) 0.88f else 0.98f)
                        )
                    )
            )
        }
    }
}
