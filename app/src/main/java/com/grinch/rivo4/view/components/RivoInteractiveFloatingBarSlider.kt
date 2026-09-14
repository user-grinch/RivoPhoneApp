package com.grinch.rivo4.view.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import kotlin.math.roundToInt

@Composable
fun RivoInteractiveFloatingBarSlider(
    headline: String,
    supporting: String?,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 12f..40f,
    steps: Int = 28,
    iconOnly: Boolean = false,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewSelectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = RivoListItemDefaults.HorizontalPadding,
                vertical = RivoListItemDefaults.VerticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RivoLeadingIconTile(icon = Icons.Outlined.DashboardCustomize)
            Spacer(modifier = Modifier.width(RivoListItemDefaults.Spacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = RivoListItemDefaults.headlineStyle()
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = RivoListItemDefaults.supportingStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${value.roundToInt()} dp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Live Floating Bar Shape Interactive Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f))
                .padding(vertical = 14.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewBarShape = RoundedCornerShape(value.roundToInt().dp)
            val previewBarColor = MaterialTheme.colorScheme.surfaceContainerHigh

            Surface(
                shape = previewBarShape,
                color = previewBarColor,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                val previewTabs = listOf(
                    Triple(stringResource(R.string.nav_recents), Icons.Filled.History, Icons.Outlined.History),
                    Triple(stringResource(R.string.nav_contacts), Icons.Filled.Person, Icons.Outlined.Person),
                    Triple(stringResource(R.string.nav_favorites), Icons.Filled.Star, Icons.Outlined.Star),
                    Triple(stringResource(R.string.nav_call_recordings), Icons.Filled.Mic, Icons.Outlined.MicNone)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewTabs.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
                        val isSelected = previewSelectedIndex == index
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "previewItemContent"
                        )
                        val itemScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "previewItemScale"
                        )

                        val itemCornerRadius = (value.roundToInt() - 6).coerceAtLeast(10).dp
                        val itemShape = RoundedCornerShape(itemCornerRadius)

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .clip(itemShape)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { previewSelectedIndex = index }
                        ) {
                                if (iconOnly) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                            contentDescription = label,
                                            tint = contentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                            contentDescription = label,
                                            tint = contentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
