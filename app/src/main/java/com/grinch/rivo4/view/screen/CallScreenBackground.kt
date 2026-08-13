package com.grinch.rivo4.view.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface

    val hasCustomBackground = !backgroundUri.isNullOrEmpty()
    var customFailed by remember(backgroundUri) { mutableStateOf(false) }

    val customVisible = hasCustomBackground && !customFailed
    val photoVisible = !photoUri.isNullOrEmpty() && !customVisible

    val infiniteTransition = rememberInfiniteTransition(label = "ambientBg")
    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val blobAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ba"
    )

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

    Box(modifier = Modifier.fillMaxSize().background(surface)) {
        // Animated Ambient Morphing Gradient Orbs
        if (!customVisible) {
            Box(
                modifier = Modifier
                    .offset(x = blob1Offset.dp, y = (-80).dp)
                    .size(280.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer.copy(alpha = blobAlpha))
                    .blur(72.dp)
            )
            Box(
                modifier = Modifier
                    .offset(x = blob2Offset.dp, y = 100.dp)
                    .size(320.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(scheme.tertiaryContainer.copy(alpha = blobAlpha))
                    .blur(84.dp)
            )
        }

        if (photoVisible) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(56.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.42f
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
                            0f to surface.copy(alpha = 0.88f),
                            0.5f to surface.copy(alpha = 0.60f),
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
                            0.34f to surface.copy(alpha = 0.42f),
                            0.72f to surface.copy(alpha = 0.88f),
                            1f to surface.copy(alpha = 0.88f)
                        )
                    )
            )
        }
    }
}
