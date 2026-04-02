package com.grinch.rivo4.view.components

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grinch.rivo4.controller.util.formatDate
import com.grinch.rivo4.modal.data.CallLogEntry
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.grinch.rivo4.controller.util.makeCall
import android.Manifest
@Composable
fun CallLogTileSimple(log: CallLogEntry) {
    val icon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        else -> Icons.Default.Call
    }

    RivoListItem(
        headline = when (log.type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            else -> "Call"
        },
        supporting = "${formatDate(log.date)}${if (log.duration > 0) " • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}" else ""}",
        leadingIcon = icon,
        onClick = { }
    )
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit
) {
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.weight(1f)) {
            RivoListItem(
                headline = buildString {
                    append(log.name ?: log.number)
                    if (log.count > 1) append(" (${log.count})")
                },
                supporting = buildString {
                    if (log.name != null && log.name != log.number) {
                        append(log.number)
                    }
                },
                avatarName = log.name ?: log.number,
                photoUri = log.photoUri,
                trailingIcon = when (log.type) {
                    CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                    CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                    CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                    else -> Icons.Default.Call
                },
                onClick = {onTileClick(log)}
            )
        }
    }
}