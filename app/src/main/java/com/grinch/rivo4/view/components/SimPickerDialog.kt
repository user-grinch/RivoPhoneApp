package com.grinch.rivo4.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SimCard
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

    val phoneAccounts = remember(telecomManager, context) {
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
            val account = telecomManager.getPhoneAccount(handle)
            val labelStr = account?.label?.toString()?.takeIf { it.isNotBlank() }
            if (labelStr != null) {
                labelStr
            } else {
                val index = phoneAccounts.indexOf(handle) + 1
                "SIM $index ($unknownSimLabel)"
            }
        },
        onItemSelected = onSimSelected,
        itemSupporting = { handle ->
            val account = telecomManager.getPhoneAccount(handle)
            val address = account?.address?.schemeSpecificPart
            val desc = account?.shortDescription?.toString()
            if (!address.isNullOrBlank()) {
                address
            } else if (!desc.isNullOrBlank()) {
                desc
            } else {
                "Slot ${phoneAccounts.indexOf(handle) + 1}"
            }
        },
        icon = Icons.Outlined.SimCard,
        itemIcon = { Icons.Outlined.SimCard },
        isSelected = { handle -> selectedAccount != null && handle == selectedAccount }
    )
}
