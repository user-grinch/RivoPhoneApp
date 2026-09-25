package com.grinch.rivo4.view.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
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

    val thresholdPx = with(density) { 96.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val outerShape = RoundedCornerShape(cardRoundness.dp)

    val currentOffset = offsetX.value
    val isSwipingRight = currentOffset > 0 && swipeRightAction != SwipeActionType.NONE
    val isSwipingLeft = currentOffset < 0 && swipeLeftAction != SwipeActionType.NONE
    val isDragging = abs(currentOffset) > 1f

    val isArmed = abs(currentOffset) >= thresholdPx

    val animatedElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipeElevation"
    )

    val slidingCornerRadius by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "slidingCorner"
    )
    val slidingShape = RoundedCornerShape(slidingCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
    ) {
        // Revealed Background Track
        if (isSwipingRight) {
            val progress = (abs(currentOffset) / thresholdPx).coerceIn(0f, 1f)
            val badgeScale by animateFloatAsState(
                targetValue = if (isArmed) 1.12f else (0.8f + (progress * 0.2f)),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "badgeScaleRight"
            )

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
                            scaleX = badgeScale
                            scaleY = badgeScale
                            alpha = if (isArmed) 1f else 0.70f + (progress * 0.30f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = swipeRightAction.contentColor().copy(alpha = if (isArmed) 0.30f else 0.18f),
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
            val progress = (abs(currentOffset) / thresholdPx).coerceIn(0f, 1f)
            val badgeScale by animateFloatAsState(
                targetValue = if (isArmed) 1.12f else (0.8f + (progress * 0.2f)),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "badgeScaleLeft"
            )

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
                            scaleX = badgeScale
                            scaleY = badgeScale
                            alpha = if (isArmed) 1f else 0.70f + (progress * 0.30f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = swipeLeftAction.contentColor().copy(alpha = if (isArmed) 0.30f else 0.18f),
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
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .then(
                    if (isDragging) {
                        Modifier
                            .shadow(
                                elevation = animatedElevation,
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
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredHaptic = false
                        },
                        onDragEnd = {
                            val finalOffset = offsetX.value
                            if (finalOffset >= thresholdPx && swipeRightAction != SwipeActionType.NONE) {
                                onTriggerAction(swipeRightAction)
                            } else if (finalOffset <= -thresholdPx && swipeLeftAction != SwipeActionType.NONE) {
                                onTriggerAction(swipeLeftAction)
                            }
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                )
                            }
                            hasTriggeredHaptic = false
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                )
                            }
                            hasTriggeredHaptic = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()

                            // Intent-based resistance: deliberate drag inertia so accidental swipes don't trigger easily
                            val currentMag = abs(offsetX.value)
                            val initialDeadzonePx = with(density) { 24.dp.toPx() }
                            val friction = if (currentMag < initialDeadzonePx) 0.45f else 0.62f
                            val rawNext = offsetX.value + (dragAmount * friction)

                            val next = when {
                                rawNext > 0 && swipeRightAction == SwipeActionType.NONE -> 0f
                                rawNext < 0 && swipeLeftAction == SwipeActionType.NONE -> 0f
                                else -> rawNext
                            }

                            // Damped spring resistance past threshold
                            val direction = if (next >= 0) 1f else -1f
                            val mag = abs(next)
                            val dampedMag = if (mag > thresholdPx) {
                                thresholdPx + ((mag - thresholdPx) * 0.28f)
                            } else {
                                mag
                            }
                            val clamped = (dampedMag * direction).coerceIn(-maxDragPx, maxDragPx)

                            scope.launch {
                                offsetX.snapTo(clamped)
                            }

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
