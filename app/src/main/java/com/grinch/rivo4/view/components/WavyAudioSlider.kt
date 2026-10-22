package com.grinch.rivo4.view.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyAudioSlider(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    handleColor: Color = MaterialTheme.colorScheme.primary,
    sliderHeight: Dp = 24.dp
) {
    // Continuous infinite phase: travels to the right smoothly during playback
    val infiniteTransition = rememberInfiniteTransition(label = "WavyPhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyPhaseAnimation"
    )

    val density = LocalDensity.current
    val baseAmplitudePx = with(density) { 3.2.dp.toPx() }

    // Smooth swelling/flattening of wave amplitude (no sudden jumps when pausing/resuming)
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) baseAmplitudePx else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "WavyAmplitudeAnimation"
    )

    val wavelengthPx = with(density) { 20.dp.toPx() }
    val strokeWidthPx = with(density) { 3.5.dp.toPx() }
    val handleHeightPx = with(density) { 14.dp.toPx() }
    val handleWidthPx = with(density) { 4.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(sliderHeight)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val centerY = size.height / 2f
            val currentX = (value.coerceIn(0f, 1f) * width)

            // 1. Inactive Straight Line (from currentX to width)
            if (currentX < width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(currentX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Active Wave Line (from 0 to currentX)
            if (currentX > 0f) {
                if (animatedAmplitude > 0.05f) {
                    val path = Path()
                    path.moveTo(0f, centerY)

                    // Taper distance at both ends so the wave connects perfectly to (0, centerY) and (currentX, centerY)
                    val taperLength = (wavelengthPx * 0.75f).coerceAtMost(currentX * 0.5f)
                    val stepPx = with(density) { 1.5.dp.toPx() }

                    var x = 0f
                    while (x < currentX) {
                        val startDist = x
                        val endDist = currentX - x
                        val minEdgeDist = minOf(startDist, endDist)
                        val taperFactor = if (taperLength > 0f) {
                            (minEdgeDist / taperLength).coerceIn(0f, 1f)
                        } else 1f
                        // Smooth Hermite interpolation: 3t^2 - 2t^3
                        val smoothTaper = taperFactor * taperFactor * (3f - 2f * taperFactor)

                        // Traveling sine wave: minus phase ensures left-to-right propagation
                        val waveY =
                            centerY + animatedAmplitude * smoothTaper * sin((x / wavelengthPx) * 2 * PI - phase).toFloat()
                        path.lineTo(x, waveY)
                        x += stepPx
                    }
                    // Explicitly anchor exact endpoint at (currentX, centerY)
                    path.lineTo(currentX, centerY)

                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                } else {
                    // Paused / flat active line
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(currentX, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Scrubber Handle Pill | (at currentX, perfectly centered at centerY)
            drawRoundRect(
                color = handleColor,
                topLeft = Offset(currentX - handleWidthPx / 2f, centerY - handleHeightPx / 2f),
                size = Size(handleWidthPx, handleHeightPx),
                cornerRadius = CornerRadius(handleWidthPx / 2f, handleWidthPx / 2f)
            )
        }
    }
}
