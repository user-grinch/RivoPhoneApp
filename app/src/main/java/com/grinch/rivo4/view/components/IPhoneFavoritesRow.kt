package com.grinch.rivo4.view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.data.Contact
import kotlinx.coroutines.launch
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
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val itemWidth = 140.dp
    val itemHeight = 180.dp
    val spacing = 10.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val itemTotalWidthPx = itemWidthPx + spacingPx

    // iOS Wiggle animation
    val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleRotation"
    )
    val translationY by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleTranslation"
    )

    LaunchedEffect(draggedIndex, dragOffsetX) {
        if (draggedIndex != -1) {
            val layoutInfo = lazyListState.layoutInfo
            val containerWidth = layoutInfo.viewportEndOffset
            val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }
            
            if (draggedItemInfo != null) {
                val currentPos = draggedItemInfo.offset + dragOffsetX
                val scrollThreshold = 120f
                
                if (currentPos < scrollThreshold) {
                    scope.launch { lazyListState.animateScrollBy(-500f) }
                } else if (currentPos > containerWidth - itemWidthPx - scrollThreshold) {
                    scope.launch { lazyListState.animateScrollBy(500f) }
                }
            }
        }
    }

    LazyRow(
        state = lazyListState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        itemsIndexed(
            items = favoritesList,
            key = { _, contact -> contact.id }
        ) { index, contact ->
            val isDragged = index == draggedIndex
            
            val animatedScale by animateFloatAsState(
                targetValue = if (isDragged) 1.08f else 1.0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "scale"
            )
            
            val animatedElevation by animateDpAsState(
                targetValue = if (isDragged) 16.dp else 2.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "elevation"
            )

            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemHeight)
                    // The fix: only animate placement when NOT being dragged to avoid weird jumps
                    .then(if (!isDragged) Modifier.animateItem(
                        placementSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) else Modifier)
                    .zIndex(if (isDragged) 100f else 0f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isDragged) {
                                translationX = dragOffsetX
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .graphicsLayer {
                                if (isEditing && !isDragged) {
                                    rotationZ = rotation
                                    this.translationY = translationY * density.density
                                }
                            }
                            .shadow(
                                elevation = animatedElevation,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            )
                            .pointerInput(contact.id) { // Use stable key
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = favoritesList.indexOfFirst { it.id == contact.id }
                                        dragOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        
                                        val currentIndex = favoritesList.indexOfFirst { it.id == contact.id }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                        
                                        val swapThreshold = itemTotalWidthPx * 0.55f
                                        
                                        if (dragOffsetX > swapThreshold && currentIndex < favoritesList.lastIndex) {
                                            val targetIndex = currentIndex + 1
                                            val item = favoritesList[currentIndex]
                                            favoritesList.removeAt(currentIndex)
                                            favoritesList.add(targetIndex, item)
                                            draggedIndex = targetIndex
                                            dragOffsetX -= itemTotalWidthPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (dragOffsetX < -swapThreshold && currentIndex > 0) {
                                            val targetIndex = currentIndex - 1
                                            val item = favoritesList[currentIndex]
                                            favoritesList.removeAt(currentIndex)
                                            favoritesList.add(targetIndex, item)
                                            draggedIndex = targetIndex
                                            dragOffsetX += itemTotalWidthPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    onDragEnd = {
                                        onSaveOrder(favoritesList.map { it.id })
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        tonalElevation = animatedElevation / 3
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Background
                            if (!contact.photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val colorPair = getModernGradient(contact.name)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(colorPair.first, colorPair.second))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.trim().take(1).uppercase(),
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }
                            
                            // Label Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.45f)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.label_mobile),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Delete Button (Fixed & Scaled)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditing,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .zIndex(110f) // Ensure it's above everything
                    ) {
                        Surface(
                            onClick = { onUnfavorite(contact) },
                            modifier = Modifier
                                .size(22.dp)
                                .offset(x = (-6).dp, y = (-6).dp),
                            shape = CircleShape,
                            color = Color(0xFFE53935),
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.5.dp, Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.content_desc_remove_favorite),
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getModernGradient(seed: String): Pair<Color, Color> {
    val gradients = listOf(
        Color(0xFF8E2DE2) to Color(0xFF4A00E0),
        Color(0xFF00c6ff) to Color(0xFF0072ff),
        Color(0xFFf857a6) to Color(0xFFff5858),
        Color(0xFF11998E) to Color(0xFF38EF7D),
        Color(0xFFf7971e) to Color(0xFFffd200),
        Color(0xFFeb3349) to Color(0xFFf45c43),
        Color(0xFF1fa2ff) to Color(0xFF12d8fa),
        Color(0xFF70e1f5) to Color(0xFFffd194)
    )
    return gradients[abs(seed.hashCode()) % gradients.size]
}
