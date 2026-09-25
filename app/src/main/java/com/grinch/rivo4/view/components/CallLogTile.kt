package com.grinch.rivo4.view.components

import com.grinch.rivo4.R
import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.modal.data.SwipeActionType
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

@Composable
fun CallLogTileSimple(
    log: CallLogEntry,
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
    val icon = remember(log.type) {
        when (log.type) {
            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
            CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
            CallLog.Calls.BLOCKED_TYPE -> Icons.Default.Block
            else -> Icons.Default.Call
        }
    }

    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE
    val badgeColor = if (isMissedOrRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val headlineColor = if (isMissedOrRejected) MaterialTheme.colorScheme.error else Color.Unspecified

    val displayName = remember(log.name, log.number) {
        log.name?.takeIf { it.isNotBlank() } ?: formatPhoneNumber(log.number)
    }

    val headlineText = remember(displayName, log.count) {
        if (log.count > 1) "$displayName (${log.count})" else displayName
    }

    val context = LocalContext.current
    val callTypeLabel = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
        CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.call_type_missed)
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
                        supporting = supportingText,
                        supporting2 = if (showSim) log.simLabel else null,
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
    val icon = remember(log.type) {
        when (log.type) {
            CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
            CallLog.Calls.BLOCKED_TYPE -> Icons.Default.Block
            else -> Icons.Default.Call
        }
    }

    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE
    val badgeColor = if (isMissedOrRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val headlineColor = if (isMissedOrRejected) MaterialTheme.colorScheme.error else Color.Unspecified

    val displayName = remember(log.name, log.number, displayOrder) {
        log.name?.let {
            if (it.isNotEmpty()) com.grinch.rivo4.controller.util.ContactUtils.formatContactName(it, displayOrder) else null
        } ?: formatPhoneNumber(log.number)
    }

    val headlineText = remember(displayName, log.count) {
        if (log.count > 1) "$displayName (${log.count})" else displayName
    }

    val context = LocalContext.current
    val timeSimText = remember(log.date, log.simLabel, showSim) {
        buildString {
            if (showSim && log.simLabel != null) {
                append(log.simLabel)
                append(" • ")
            }
            append(formatTime(context, log.date))
        }
    }

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
                        supporting = timeSimText,
                        supporting2 = null,
                        avatarName = displayName,
                        photoUri = log.photoUri,
                        badgeIcon = icon,
                        badgeColor = badgeColor,
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
