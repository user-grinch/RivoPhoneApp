package com.grinch.rivo4.view.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.view.theme.LocalCardRoundness
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RivoSwipeToActionBox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    swipeRightAction: SwipeActionType = SwipeActionType.CALL,
    swipeLeftAction: SwipeActionType = SwipeActionType.MESSAGE,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onTriggerAction: (SwipeActionType) -> Unit,
    content: @Composable () -> Unit
) {
    if (!enabled || (swipeRightAction == SwipeActionType.NONE && swipeLeftAction == SwipeActionType.NONE)) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cardRoundness = LocalCardRoundness.current

    val thresholdPx = with(density) { 54.dp.toPx() }
    val maxDragPx = with(density) { 110.dp.toPx() }
    val initialDeadzonePx = with(density) { 4.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val outerShape = RoundedCornerShape(cardRoundness.dp)
    val slidingShape = RoundedCornerShape(16.dp)

    val isSwipingRight by remember {
        derivedStateOf { offsetX > 0.5f && swipeRightAction != SwipeActionType.NONE }
    }
    val isSwipingLeft by remember {
        derivedStateOf { offsetX < -0.5f && swipeLeftAction != SwipeActionType.NONE }
    }
    val isDragging by remember {
        derivedStateOf { abs(offsetX) > 1f }
    }
    val isArmed by remember {
        derivedStateOf { abs(offsetX) >= thresholdPx }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
    ) {
        // Revealed Background Track (Right Swipe Action)
        if (isSwipingRight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(swipeRightAction.containerColor()),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .graphicsLayer {
                            val current = abs(offsetX)
                            val progress = (current / thresholdPx).coerceIn(0f, 1f)
                            val armed = current >= thresholdPx
                            val scale = if (armed) 1.12f else (0.8f + (progress * 0.2f))
                            scaleX = scale
                            scaleY = scale
                            alpha = if (armed) 1f else 0.70f + (progress * 0.30f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val armed = isArmed
                    Surface(
                        shape = CircleShape,
                        color = swipeRightAction.contentColor().copy(alpha = if (armed) 0.30f else 0.18f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = swipeRightAction.icon,
                                contentDescription = stringResource(swipeRightAction.titleRes),
                                tint = swipeRightAction.contentColor(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else if (isSwipingLeft) {
            // Revealed Background Track (Left Swipe Action)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(swipeLeftAction.containerColor()),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .graphicsLayer {
                            val current = abs(offsetX)
                            val progress = (current / thresholdPx).coerceIn(0f, 1f)
                            val armed = current >= thresholdPx
                            val scale = if (armed) 1.12f else (0.8f + (progress * 0.2f))
                            scaleX = scale
                            scaleY = scale
                            alpha = if (armed) 1f else 0.70f + (progress * 0.30f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val armed = isArmed
                    Surface(
                        shape = CircleShape,
                        color = swipeLeftAction.contentColor().copy(alpha = if (armed) 0.30f else 0.18f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = swipeLeftAction.icon,
                                contentDescription = stringResource(swipeLeftAction.titleRes),
                                tint = swipeLeftAction.contentColor(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Foreground Sliding Card (Solid opaque container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .then(
                    if (isDragging) {
                        Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = slidingShape,
                                clip = false
                            )
                            .clip(slidingShape)
                    } else {
                        Modifier
                    }
                )
                .background(
                    color = containerColor,
                    shape = if (isDragging) slidingShape else RoundedCornerShape(0.dp)
                )
                .pointerInput(enabled, swipeRightAction, swipeLeftAction) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredHaptic = false
                        },
                        onDragEnd = {
                            val finalOffset = offsetX
                            if (finalOffset >= thresholdPx && swipeRightAction != SwipeActionType.NONE) {
                                onTriggerAction(swipeRightAction)
                            } else if (finalOffset <= -thresholdPx && swipeLeftAction != SwipeActionType.NONE) {
                                onTriggerAction(swipeLeftAction)
                            }
                            hasTriggeredHaptic = false
                            scope.launch {
                                Animatable(finalOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                ) {
                                    offsetX = value
                                }
                            }
                        },
                        onDragCancel = {
                            hasTriggeredHaptic = false
                            val current = offsetX
                            scope.launch {
                                Animatable(current).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                ) {
                                    offsetX = value
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()

                            val currentMag = abs(offsetX)
                            val friction = if (currentMag < initialDeadzonePx) 0.90f else 1.0f
                            val rawNext = offsetX + (dragAmount * friction)

                            val next = when {
                                rawNext > 0 && swipeRightAction == SwipeActionType.NONE -> 0f
                                rawNext < 0 && swipeLeftAction == SwipeActionType.NONE -> 0f
                                else -> rawNext
                            }

                            val direction = if (next >= 0) 1f else -1f
                            val mag = abs(next)
                            val dampedMag = if (mag > thresholdPx) {
                                thresholdPx + ((mag - thresholdPx) * 0.28f)
                            } else {
                                mag
                            }
                            val clamped = (dampedMag * direction).coerceIn(-maxDragPx, maxDragPx)
                            offsetX = clamped

                            if (abs(clamped) >= thresholdPx && !hasTriggeredHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredHaptic = true
                            } else if (abs(clamped) < thresholdPx && hasTriggeredHaptic) {
                                hasTriggeredHaptic = false
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
