package com.grinch.rivo4.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grinch.rivo4.R

@Composable
fun SimPickerDialog(
    onDismissRequest: () -> Unit,
    onSimSelected: (PhoneAccountHandle) -> Unit
) {
    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    
    val phoneAccounts = remember {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                telecomManager.callCapablePhoneAccounts
            } catch (e: SecurityException) {
                emptyList()
            }
        } else emptyList()
    }

    if (phoneAccounts.isNotEmpty()) {
        val unknownSimLabel = stringResource(R.string.sim_picker_unknown_sim)
        RivoSelectionDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(R.string.sim_picker_title),
            items = phoneAccounts,
            itemLabel = { handle ->
                telecomManager.getPhoneAccount(handle)?.label?.toString() ?: unknownSimLabel
            },
            onItemSelected = onSimSelected,
            itemSupporting = { handle ->
                telecomManager.getPhoneAccount(handle)?.shortDescription?.toString() ?: ""
            },
            icon = Icons.Default.SimCard,
            itemIcon = { Icons.Default.SimCard }
        )
    } else {
        SideEffect {
            onDismissRequest()
        }
    }
}
