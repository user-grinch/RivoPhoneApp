package com.grinch.rivo4.view.components

import com.grinch.rivo4.R
import android.provider.CallLog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.formatDate
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.formatTime
import com.grinch.rivo4.modal.data.CallLogEntry

@Immutable
data class CallLogTileConfig(
    val showSim: Boolean = true,
    val swipeEnabled: Boolean = false,
    val swipeRightAction: SwipeActionType = SwipeActionType.CALL,
    val swipeLeftAction: SwipeActionType = SwipeActionType.MESSAGE,
    val displayOrder: Int = 0
)

val LocalCallLogTileConfig: ProvidableCompositionLocal<CallLogTileConfig> =
    staticCompositionLocalOf { CallLogTileConfig() }

fun resolveSimNumber(log: CallLogEntry): Int? {
    if (log.simNumber != null) return log.simNumber
    val label = log.simLabel ?: return null
    val lower = label.lowercase()
    return when {
        lower.contains("sim 1") || lower.contains("sim1") || lower.contains("slot 1") || lower.contains("[1]") -> 1
        lower.contains("sim 2") || lower.contains("sim2") || lower.contains("slot 2") || lower.contains("[2]") -> 2
        lower.contains("1") && !lower.contains("2") -> 1
        lower.contains("2") && !lower.contains("1") -> 2
        else -> 1
    }
}

@Composable
fun SimBadge(
    simNumber: Int,
    modifier: Modifier = Modifier
) {
    val isSim1 = simNumber == 1
    val containerColor = if (isSim1) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    }
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SimCard,
                contentDescription = if (isSim1) stringResource(R.string.sim_slot_1) else stringResource(R.string.sim_slot_2),
                modifier = Modifier.size(10.5.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "$simNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                )
            )
        }
    }
}

@Composable
fun CallLogTileSimple(
    log: CallLogEntry,
    contact: Contact? = null,
    displayOrder: Int = LocalCallLogTileConfig.current.displayOrder,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    selected: Boolean = false,
    showSim: Boolean = LocalCallLogTileConfig.current.showSim,
    swipeEnabled: Boolean = LocalCallLogTileConfig.current.swipeEnabled && !selected,
    swipeRightAction: SwipeActionType = LocalCallLogTileConfig.current.swipeRightAction,
    swipeLeftAction: SwipeActionType = LocalCallLogTileConfig.current.swipeLeftAction,
    onSwipeAction: ((SwipeActionType, CallLogEntry) -> Unit)? = null
) {
    val isBlocked = log.isBlocked || log.type == CallLog.Calls.BLOCKED_TYPE
    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE

    val icon = remember(log.type, isBlocked) {
        if (isBlocked) {
            Icons.Default.Block
        } else {
            when (log.type) {
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                else -> Icons.Default.Call
            }
        }
    }

    val badgeColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val headlineColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    val displayName = remember(log.name, log.number, contact, displayOrder) {
        if (contact != null) {
            ContactUtils.formatContactName(contact, displayOrder)
        } else {
            log.name?.let {
                if (it.isNotEmpty()) ContactUtils.formatContactName(it, displayOrder) else null
            } ?: formatPhoneNumber(log.number)
        }
    }

    val headlineText = displayName

    val context = LocalContext.current
    val callTypeLabel = when {
        isBlocked -> stringResource(R.string.call_type_blocked)
        log.type == CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
        log.type == CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
        log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.call_type_missed)
        else -> stringResource(R.string.action_call)
    }

    val supportingText = remember(log.date, log.duration, callTypeLabel) {
        buildString {
            append(callTypeLabel)
            append(" • ")
            append(formatDate(context, log.date))
            if (log.duration > 0) {
                append(" • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}")
            }
        }
    }

    val resolvedSimNumber = log.simNumber ?: resolveSimNumber(log)

    @Composable
    fun ContentBox() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RivoListItem(
                        headline = headlineText,
                        supporting = if (!showSim || resolvedSimNumber == null) supportingText else null,
                        supportingContent = if (showSim && resolvedSimNumber != null) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    SimBadge(simNumber = resolvedSimNumber)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = supportingText,
                                        style = RivoListItemDefaults.supportingStyle(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else null,
                        supporting2 = null,
                        avatarName = displayName,
                        photoUri = log.photoUri,
                        badgeIcon = icon,
                        badgeColor = badgeColor,
                        headlineColor = headlineColor,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        selected = selected
                    )
                }

                if (!selected) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.action_call),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    if (swipeEnabled && onSwipeAction != null) {
        RivoSwipeToActionBox(
            enabled = true,
            swipeRightAction = swipeRightAction,
            swipeLeftAction = swipeLeftAction,
            onTriggerAction = { action -> onSwipeAction(action, log) }
        ) {
            ContentBox()
        }
    } else {
        ContentBox()
    }
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    contact: Contact? = null,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit,
    onLongClick: (CallLogEntry) -> Unit = {},
    selected: Boolean = false,
    displayOrder: Int = LocalCallLogTileConfig.current.displayOrder,
    showSim: Boolean = LocalCallLogTileConfig.current.showSim,
    isFavorite: Boolean = false,
    swipeEnabled: Boolean = LocalCallLogTileConfig.current.swipeEnabled && !selected,
    swipeRightAction: SwipeActionType = LocalCallLogTileConfig.current.swipeRightAction,
    swipeLeftAction: SwipeActionType = LocalCallLogTileConfig.current.swipeLeftAction,
    onSwipeAction: ((SwipeActionType, CallLogEntry) -> Unit)? = null
) {
    val isBlocked = log.isBlocked || log.type == CallLog.Calls.BLOCKED_TYPE
    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE

    val isMergedMixed = remember(log.types) {
        if (log.types.size > 1) {
            val distinctTypes = log.types.toSet()
            distinctTypes.size > 1
        } else false
    }

    val icon = remember(log.type, isBlocked, isMergedMixed) {
        if (isBlocked) {
            Icons.Default.Block
        } else if (isMergedMixed) {
            Icons.AutoMirrored.Outlined.CallMerge
        } else {
            when (log.type) {
                CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else -> Icons.Default.Call
            }
        }
    }

    val badgeColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val headlineColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    val displayName = remember(log.name, log.number, contact, displayOrder) {
        if (contact != null) {
            ContactUtils.formatContactName(contact, displayOrder)
        } else {
            log.name?.let {
                if (it.isNotEmpty()) ContactUtils.formatContactName(it, displayOrder) else null
            } ?: formatPhoneNumber(log.number)
        }
    }

    val headlineText = displayName

    val context = LocalContext.current
    val timeText = remember(log.date) {
        formatTime(context, log.date)
    }
    val resolvedSimNumber = log.simNumber ?: resolveSimNumber(log)

    val isStacked = log.count > 1
    var isExpanded by remember { mutableStateOf(false) }

    @Composable
    fun ContentBox() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RivoListItem(
                        headline = headlineText,
                        supporting = if (!showSim || resolvedSimNumber == null) timeText else null,
                        supportingContent = if (showSim && resolvedSimNumber != null) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    SimBadge(simNumber = resolvedSimNumber)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeText,
                                        style = RivoListItemDefaults.supportingStyle(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null,
                        supporting2 = null,
                        avatarName = if (isStacked) null else displayName,
                        photoUri = if (isStacked) null else log.photoUri,
                        badgeIcon = if (isStacked) null else icon,
                        badgeColor = if (isStacked) null else badgeColor,
                        leadingContent = if (isStacked) {
                            {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .size(RivoListItemDefaults.AvatarSize)
                                        .clickable { isExpanded = !isExpanded }
                                ) {
                                    RivoAvatar(
                                        name = displayName,
                                        photoUri = log.photoUri,
                                        badgeIcon = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset(x = (-4).dp, y = 8.dp)
                                            .height(20.dp)
                                            .widthIn(min = 28.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shadowElevation = RivoElevation.Raised
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${log.count}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(1.dp))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 4.dp, y = 8.dp)
                                            .size(20.dp),
                                        shape = CircleShape,
                                        color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shadowElevation = RivoElevation.Raised
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isExpanded) MaterialTheme.colorScheme.onPrimary else badgeColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else null,
                        headlineColor = headlineColor,
                        trailingIcon = if (isFavorite) Icons.Default.Star else null,
                        onClick = { onTileClick(log) },
                        onLongClick = { onLongClick(log) },
                        selected = selected
                    )
                }

                if (!selected) {
                    IconButton(
                        onClick = { onButtonClick(log) },
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.action_call),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded && !selected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val subItems = if (log.subLogs.isNotEmpty()) log.subLogs else listOf(log)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
                ) {
                    subItems.forEachIndexed { idx, subLog ->
                        val subBlocked = subLog.isBlocked || subLog.type == CallLog.Calls.BLOCKED_TYPE
                        val subMissed = subLog.type == CallLog.Calls.MISSED_TYPE || subLog.type == CallLog.Calls.REJECTED_TYPE
                        val subIcon = when {
                            subBlocked -> Icons.Default.Block
                            subLog.type == CallLog.Calls.MISSED_TYPE || subLog.type == CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                            subLog.type == CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            subLog.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.Default.Call
                        }
                        val subColor = when {
                            subBlocked || subMissed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val subTypeLabel = when {
                            subBlocked -> stringResource(R.string.call_type_blocked)
                            subLog.type == CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
                            subLog.type == CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
                            subMissed -> stringResource(R.string.call_type_missed)
                            else -> stringResource(R.string.action_call)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTileClick(subLog) }
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = subIcon,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subTypeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = subColor
                            )
                            val subSimNumber = subLog.simNumber ?: resolveSimNumber(subLog)
                            if (showSim && subSimNumber != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                SimBadge(simNumber = subSimNumber)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatTime(context, subLog.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (subLog.duration > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${android.text.format.DateUtils.formatElapsedTime(subLog.duration)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (swipeEnabled && onSwipeAction != null) {
        RivoSwipeToActionBox(
            enabled = true,
            swipeRightAction = swipeRightAction,
            swipeLeftAction = swipeLeftAction,
            onTriggerAction = { action -> onSwipeAction(action, log) }
        ) {
            ContentBox()
        }
    } else {
        ContentBox()
    }
}

@Composable
fun BatchCallLogActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onAddContact: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, stringResource(R.string.action_clear_selection))
            }
            Text(
                text = pluralStringResource(R.plurals.selection_count_selected, selectedCount, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (onAddContact != null) {
                IconButton(onClick = onAddContact) {
                    Icon(Icons.Default.PersonAdd, stringResource(R.string.contact_add_to_contacts))
                }
            }
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, stringResource(R.string.action_copy_number))
                }
            }
            IconButton(onClick = { showBlockConfirm = true }) {
                Icon(Icons.Default.Block, stringResource(R.string.action_block_number))
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, stringResource(R.string.content_desc_delete_selected))
            }
        }
    }

    if (showDeleteConfirm) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = stringResource(R.string.call_log_delete_title),
            message = pluralStringResource(R.plurals.call_log_delete_confirm, selectedCount, selectedCount),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }

    if (showBlockConfirm) {
        RivoConfirmationDialog(
            onDismissRequest = { showBlockConfirm = false },
            onConfirm = onBlock,
            title = stringResource(R.string.call_log_block_title),
            message = pluralStringResource(R.plurals.call_log_block_message, selectedCount, selectedCount),
            confirmLabel = stringResource(R.string.action_block),
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Default.Block,
            isDestructive = true
        )
    }
}
