package com.grinch.rivo4.view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject
import java.util.Locale


@Composable
fun RivoExpressiveCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    shape: androidx.compose.ui.graphics.Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    isCompact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val showCards = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_CARDS, true) }
    val roundness = remember(settingsState) { prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28) }
    val resolvedShape = shape ?: RoundedCornerShape(roundness.dp)

    val padding = if (isCompact) 12.dp else 16.dp
    val spacing = if (isCompact) 8.dp else 12.dp

    if (showCards) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = resolvedShape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing)
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
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title != null || icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun RivoDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val showDividers = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_DIVIDERS, true) }

    if (showDividers) {
        HorizontalDivider(modifier = modifier, color = color)
    }
}

@Composable
fun RivoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun RivoExpressiveButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
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
            modifier = Modifier.height(size).width(size * 1.3f),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
                } else if (painter != null) {
                    Icon(painter, contentDescription = label, modifier = Modifier.size(iconSize), tint = Color.Unspecified)
                }
            }
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RivoListItem(
    headline: String,
    supporting: String? = null,
    supporting2: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    avatarName: String? = null,
    photoUri: String? = null,
    avatarShape: Shape? = null,
    badgeIcon: ImageVector? = null,
    badgeColor: Color? = null,
    headlineColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val verticalPadding = if (isCompact) 6.dp else 10.dp
    val horizontalPadding = if (isCompact) 10.dp else 12.dp
    val avatarSize = if (isCompact) 42.dp else 44.dp
    val spacing = if (isCompact) 14.dp else 16.dp

    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val shapeVal = prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0)
    val resolvedShape = avatarShape ?: when (shapeVal) {
        0 -> RoundedCornerShape(16.dp) // Squircle
        1 -> CircleShape // Circle
        2 -> RoundedCornerShape(0.dp) // Square
        else -> CircleShape
    }

    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Surface(
                    modifier = Modifier.size(avatarSize),
                    shape = resolvedShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(spacing))
            } else if (avatarName != null || photoUri != null) {
                RivoAvatar(
                    name = avatarName ?: "",
                    photoUri = photoUri,
                    badgeIcon = badgeIcon,
                    badgeColor = badgeColor,
                    shape = resolvedShape,
                    modifier = Modifier.size(avatarSize)
                )
                Spacer(modifier = Modifier.width(spacing))
            } else if (leadingIcon != null) {
                Surface(
                    modifier = Modifier.size(avatarSize),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(spacing))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = headlineColor,
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
                if (supporting2 != null) {
                    Text(
                        text = supporting2,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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

@Composable
fun RivoSelectListItem(
    headline: String,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSelectionScreen by remember { mutableStateOf(false) }

    Surface(
        onClick = { showSelectionScreen = true },
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
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Select option",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showSelectionScreen) {
        RivoSelectionDialog(
            onDismissRequest = { showSelectionScreen = false },
            title = headline,
            icon = leadingIcon,
            items = options,
            itemLabel = { it.first },
            onItemSelected = { onValueChange(it.second) },
            isSelected = { it.second == selectedValue }
        )
    }
}

@Composable
fun RivoFilterChip(
    label: String,
    selected: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = { onClick(label) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        leadingIcon = leadingIcon ?: if (label == "All") {
            { Icon(Icons.Default.FilterList, null, Modifier.size(18.dp), tint = if (selected)  MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
        } else null,
        border = null,
        elevation = FilterChipDefaults.filterChipElevation(elevation = 0.dp)
    )
}

