package com.grinch.rivo4.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grinch.rivo4.R

@Composable
fun SimPickerDialog(
    onDismissRequest: () -> Unit,
    onSimSelected: (PhoneAccountHandle) -> Unit,
    selectedAccount: PhoneAccountHandle? = null,
    showRememberOption: Boolean = false,
    onSimSelectedWithRemember: ((PhoneAccountHandle, Boolean) -> Unit)? = null
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

    var rememberForContact by remember { mutableStateOf(false) }
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
        onItemSelected = { handle ->
            if (onSimSelectedWithRemember != null) {
                onSimSelectedWithRemember(handle, rememberForContact)
            } else {
                onSimSelected(handle)
            }
        },
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
        isSelected = { handle -> selectedAccount != null && handle == selectedAccount },
        footer = if (showRememberOption) {
            {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 4.dp)
                        .clickable { rememberForContact = !rememberForContact },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberForContact,
                        onCheckedChange = { rememberForContact = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sim_remember_for_contact),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else null
    )
}
