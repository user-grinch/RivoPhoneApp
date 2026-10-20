package com.grinch.rivo4.view.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.getDeviceImeiInfo

@Composable
fun ImeiDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val info = remember { getDeviceImeiInfo(context) }

    val items = remember(info) {
        val list = mutableListOf<Pair<String, String>>()
        info.imei1?.takeIf { it.isNotBlank() }?.let {
            list.add(context.getString(R.string.imei_label, 1) to it)
        }
        info.imei2?.takeIf { it.isNotBlank() }?.let {
            list.add(context.getString(R.string.imei_label, 2) to it)
        }
        info.meid?.takeIf { it.isNotBlank() }?.let {
            list.add(context.getString(R.string.meid_label) to it)
        }
        info.serial?.takeIf { it.isNotBlank() }?.let {
            list.add(context.getString(R.string.serial_label) to it)
        }
        list
    }

    val copyItem = { value: String ->
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, context.getString(R.string.imei_copied), Toast.LENGTH_SHORT).show()
    }

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.imei_dialog_title),
        icon = Icons.Outlined.PhoneAndroid,
        confirmAction = RivoDialogAction(
            label = stringResource(R.string.action_done),
            onClick = onDismissRequest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.imei_not_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { (label, value) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyItem(value) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { copyItem(value) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(R.string.contact_copy_to_clipboard),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
