package com.grinch.rivo4.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun RivoExpressiveCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null || icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            icon,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun RivoSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
fun RivoExpressiveButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 64.dp,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (size / 4) else (size / 2.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonShape"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
            }
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RivoListItem(
    headline: String,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    avatarName: String? = null,
    photoUri: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarName != null || photoUri != null) {
                RivoAvatar(
                    name = avatarName ?: "",
                    photoUri = photoUri,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else if (leadingIcon != null) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            if (trailingIcon != null) {
                Icon(
                    trailingIcon, 
                    null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RivoSwitchListItem(
    headline: String,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (supporting != null) {
                    Text(text = supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}
