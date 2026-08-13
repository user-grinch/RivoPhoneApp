package com.grinch.rivo4.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.grinch.rivo4.R

@Composable
fun SimPickerDialog(
    onDismissRequest: () -> Unit,
    onSimSelected: (PhoneAccountHandle) -> Unit,
    selectedAccount: PhoneAccountHandle? = null
) {
    val context = LocalContext.current
    val telecomManager = remember(context) {
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    val phoneAccounts = remember(telecomManager) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                telecomManager.callCapablePhoneAccounts
            } catch (e: SecurityException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    if (phoneAccounts.isEmpty()) {
        LaunchedEffect(Unit) { onDismissRequest() }
        return
    }

    val unknownSimLabel = stringResource(R.string.sim_picker_unknown_sim)

    RivoSelectionDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.sim_picker_title),
        items = phoneAccounts,
        itemLabel = { handle ->
            telecomManager.getPhoneAccount(handle)?.label?.toString()?.takeIf { it.isNotBlank() }
                ?: unknownSimLabel
        },
        onItemSelected = onSimSelected,
        itemSupporting = { handle ->
            telecomManager.getPhoneAccount(handle)?.shortDescription?.toString().orEmpty()
        },
        icon = Icons.Default.SimCard,
        itemIcon = { Icons.Default.SimCard },
        isSelected = { handle -> selectedAccount != null && handle == selectedAccount }
    )
}
