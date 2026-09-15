package com.grinch.rivo4.view.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.controller.permission.PermissionActionType
import com.grinch.rivo4.controller.permission.PermissionCheckItem
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.rivoCornerDp

@Composable
fun PermissionsChecklistCard(
    items: List<PermissionCheckItem>,
    onItemClick: (PermissionCheckItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val roundness = LocalCardRoundness.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(rivoCornerDp(24, roundness)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items.forEachIndexed { index, item ->
                PermissionItemRow(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                if (index < items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionItemRow(
    item: PermissionCheckItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundness = LocalCardRoundness.current
    val iconBgColor by animateColorAsState(
        targetValue = if (item.isGranted) {
            MaterialTheme.colorScheme.primaryContainer
        } else if (!item.isEnabled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "iconBgColor"
    )
    val iconTintColor by animateColorAsState(
        targetValue = if (item.isGranted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else if (!item.isEnabled) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "iconTintColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (!item.isEnabled && !item.isGranted) 0.65f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(rivoCornerDp(14, roundness)))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTintColor
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.statusNote != null && !item.isGranted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(rivoCornerDp(6, roundness)),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = item.statusNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        AnimatedContent(
            targetState = item.isGranted,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "statusBadge"
        ) { isGranted ->
            if (isGranted) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF386A20),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                val label = item.actionLabel ?: when (item.actionType) {
                    PermissionActionType.ROLE_DIALER -> "Set"
                    PermissionActionType.OVERLAY -> "Enable"
                    PermissionActionType.BATTERY_OPTIMIZATION -> "Allow"
                    PermissionActionType.STORAGE -> "Allow"
                    PermissionActionType.SHIZUKU -> "Grant"
                    else -> "Grant"
                }
                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(rivoCornerDp(12, roundness)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    colors = if (!item.isEnabled) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
