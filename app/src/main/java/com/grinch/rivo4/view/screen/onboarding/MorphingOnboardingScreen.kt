package com.grinch.rivo4.view.screen.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class MorphingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val shapeCornerPercent: Int,
    val rotation: Float,
    val scale: Float
)

private val pages = listOf(
    MorphingPage(
        icon = Icons.Default.Palette,
        title = "Expressive Design",
        description = "Experience a modern interface powered by Material 3 and smooth Jetpack Compose animations.",
        shapeCornerPercent = 50,
        rotation = 0f,
        scale = 1f
    ),
    MorphingPage(
        icon = Icons.Default.Dialpad,
        title = "Lightning-Fast Dialing",
        description = "Find contacts in an instant with our intelligent T9 search and organized call history.",
        shapeCornerPercent = 30,
        rotation = 45f,
        scale = 1.2f
    ),
    MorphingPage(
        icon = Icons.Default.Security,
        title = "Built for You",
        description = "A fully open-source, proximity-aware calling experience designed for efficiency.",
        shapeCornerPercent = 10,
        rotation = 0f,
        scale = 1f
    )
)

@Composable
fun MorphingOnboardingScreen(onFinished: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }

    val cornerPercent by animateFloatAsState(
        targetValue = pages[currentPage].shapeCornerPercent.toFloat(),
        animationSpec = tween(500),
        label = "corner"
    )
    val rotation by animateFloatAsState(
        targetValue = pages[currentPage].rotation,
        animationSpec = tween(500),
        label = "rotation"
    )
    val scale by animateFloatAsState(
        targetValue = pages[currentPage].scale,
        animationSpec = tween(500),
        label = "scale"
    )
    val shapeSize by animateDpAsState(
        targetValue = (140 * pages[currentPage].scale).dp,
        animationSpec = tween(500),
        label = "size"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background morphing shapes
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = (-50).dp, y = 100.dp)
                    .rotate(rotation * 2)
                    .clip(RoundedCornerShape(cornerPercent.toInt()))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = 200.dp)
                    .rotate(-rotation)
                    .clip(RoundedCornerShape((50 - cornerPercent / 2).toInt()))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            )

            // Skip button
            TextButton(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Skip")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Main morphing shape with icon
                Box(
                    modifier = Modifier
                        .size(shapeSize)
                        .rotate(rotation)
                        .clip(RoundedCornerShape(cornerPercent.toInt()))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pages[currentPage].icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .rotate(-rotation), // Counter-rotate to keep icon upright
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Title
                Text(
                    text = pages[currentPage].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = pages[currentPage].description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Morphing indicators
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        val indicatorCorner by animateFloatAsState(
                            targetValue = if (index == currentPage) 4f else 50f,
                            animationSpec = tween(300),
                            label = "indicatorCorner"
                        )
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (index == currentPage) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "indicatorWidth"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(indicatorWidth, 8.dp)
                                .clip(RoundedCornerShape(indicatorCorner.toInt()))
                                .background(
                                    if (index == currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentPage > 0) {
                        TextButton(onClick = { currentPage-- }) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onFinished()
                            }
                        },
                        shape = RoundedCornerShape(cornerPercent.toInt().coerceIn(10, 50))
                    ) {
                        Text(
                            text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
