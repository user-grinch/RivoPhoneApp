package com.grinch.rivo4.view.components

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
import com.grinch.rivo4.controller.util.formatDate
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.formatTime
import com.grinch.rivo4.modal.data.CallLogEntry

@Composable
fun CallLogTileSimple(
    log: CallLogEntry,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    selected: Boolean = false
) {
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val showSim = prefs.getBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_SHOW_SIM_ICON_HISTORY, true)

    val icon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        else -> Icons.Default.Call
    }

    val badgeColor = if (log.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val headlineColor = if (log.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else Color.Unspecified

    val context = LocalContext.current

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
                    headline = when (log.type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Call"
                    },
                    supporting = buildString {
                        append(formatDate(context, log.date))
                        if (log.duration > 0) append(" • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}")
                    },
                    supporting2 = if (showSim) log.simLabel else null,
                    avatarName = "", 
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
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit,
    onLongClick: (CallLogEntry) -> Unit = {},
    selected: Boolean = false,
    displayOrder: Int = 0
) {
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val showSim = prefs.getBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_SHOW_SIM_ICON_HISTORY, true)

    val icon = when (log.type) {
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.Default.Call
    }
    
    val badgeColor = if (log.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val headlineColor = if (log.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else Color.Unspecified
    
    val favNum = log.contactId?.let { prefs.getFavoriteNumber(it) }
    val isFavorite = com.grinch.rivo4.controller.util.areNumbersEqual(log.number, favNum)
    val context = LocalContext.current

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
                val displayName = remember(log.name, displayOrder) {
                    log.name?.let { 
                        if (it.isNotEmpty()) com.grinch.rivo4.controller.util.ContactUtils.formatContactName(it, displayOrder) else null
                    } ?: formatPhoneNumber(log.number)
                }

                RivoListItem(
                    headline = buildString {
                        append(displayName)
                        if (log.count > 1) append(" (${log.count})")
                    },
                    supporting = buildString {
                        if (log.name != null && log.name != log.number) {
                            append(formatPhoneNumber(log.number))
                        }
                    },
                    supporting2 = buildString {
                        if (showSim && log.simLabel != null) {
                            append(log.simLabel)
                            append(" • ")
                        }
                        append(formatTime(context, log.date))
                    },
                    avatarName = log.name ?: formatPhoneNumber(log.number),
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
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BatchCallLogActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onClearAll: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.Close, "Clear selection")
            }
            Text(
                text = "$selectedCount Selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = { showClearAllConfirm = true }) {
                Icon(Icons.Default.DeleteSweep, "Clear all logs")
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete selected")
            }
        }
    }

    if (showDeleteConfirm) {
        RivoDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Delete Call Logs",
            icon = Icons.Default.Delete,
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        ) {
            Text(
                "Are you sure you want to delete $selectedCount selected call logs?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showClearAllConfirm) {
        RivoDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = "Clear All Logs",
            icon = Icons.Default.DeleteSweep,
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearAllConfirm = false
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        ) {
            Text(
                "This will delete your entire call history. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
