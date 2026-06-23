package com.grinch.rivo4.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grinch.rivo4.modal.data.Contact
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun IPhoneFavoritesRow(
    favorites: List<Contact>,
    isEditing: Boolean,
    onUnfavorite: (Contact) -> Unit,
    onSaveOrder: (List<String>) -> Unit,
    onClick: (Contact) -> Unit
) {
    val favoritesList = remember(favorites) {
        mutableStateListOf<Contact>().apply {
            addAll(favorites)
        }
    }

    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val itemWidth = 90.dp
    val spacing = 12.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val itemTotalWidthPx = itemWidthPx + spacingPx

    // iOS Wiggle animation variables
    val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleRotation"
    )
    val translationY by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleTranslation"
    )

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        favoritesList.forEachIndexed { index, contact ->
            Column(
                modifier = Modifier
                    .width(itemWidth)
                    .zIndex(if (index == draggedIndex) 1f else 0f)
                    .graphicsLayer {
                        // Drag translation for visual continuity
                        if (index == draggedIndex) {
                            translationX = dragOffsetX
                            scaleX = 1.1f
                            scaleY = 1.1f
                            shadowElevation = 8.dp.toPx()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = itemWidth, height = 120.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // iOS-like Wiggle when editing
                                if (isEditing && index != draggedIndex) {
                                    rotationZ = rotation
                                    this.translationY = translationY * density.density
                                }
                            }
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        val shift = (dragOffsetX / itemTotalWidthPx).roundToInt()
                                        if (shift != 0) {
                                            val targetIndex = (draggedIndex + shift).coerceIn(0, favoritesList.lastIndex)
                                            if (targetIndex != draggedIndex) {
                                                val temp = favoritesList[draggedIndex]
                                                favoritesList.removeAt(draggedIndex)
                                                favoritesList.add(targetIndex, temp)
                                                draggedIndex = targetIndex
                                                dragOffsetX -= shift * itemTotalWidthPx
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        onSaveOrder(favoritesList.map { it.id })
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                    }
                                )
                            }
                            .clickable(
                                enabled = !isEditing,
                                onClick = { onClick(contact) }
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (index == draggedIndex) 8.dp else 2.dp
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!contact.photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val avatarColors = listOf(
                                    Pair(Color(0xFFEF5350), Color(0xFFC62828)),
                                    Pair(Color(0xFFEC407A), Color(0xFFAD1457)),
                                    Pair(Color(0xFFAB47BC), Color(0xFF6A1B9A)),
                                    Pair(Color(0xFF7E57C2), Color(0xFF4527A0)),
                                    Pair(Color(0xFF5C6BC0), Color(0xFF283593)),
                                    Pair(Color(0xFF42A5F5), Color(0xFF1565C0)),
                                    Pair(Color(0xFF29B6F6), Color(0xFF0277BD)),
                                    Pair(Color(0xFF26C6DA), Color(0xFF00838F)),
                                    Pair(Color(0xFF26A69A), Color(0xFF00695C)),
                                    Pair(Color(0xFF66BB6A), Color(0xFF2E7D32)),
                                    Pair(Color(0xFF9CCC65), Color(0xFF558B2F)),
                                    Pair(Color(0xFFFF7043), Color(0xFFD84315))
                                )
                                val colorPair = avatarColors[abs(contact.name.hashCode()) % avatarColors.size]
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(colorPair.first, colorPair.second))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.trim().take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // iOS-like red delete badge
                    if (isEditing) {
                        IconButton(
                            onClick = { onUnfavorite(contact) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-8).dp, y = (-8).dp)
                                .size(24.dp)
                                .background(Color(0xFFD32F2F), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove Favorite",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contact.name.split(" ").firstOrNull() ?: "",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
