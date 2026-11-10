package com.grinch.rivo4.view.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.formatPhoneNumber
import kotlinx.coroutines.delay

@Composable
fun NumberTagDialog(
    phoneNumber: String,
    initialTag: String = "",
    onDismissRequest: () -> Unit,
    onSaveTag: (String) -> Unit,
    onDeleteTag: (() -> Unit)? = null
) {
    val view = LocalView.current
    var tagText by remember { mutableStateOf(initialTag) }
    val focusRequester = remember { FocusRequester() }
    val formattedNumber = remember(phoneNumber) { formatPhoneNumber(phoneNumber) }

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.tag_dialog_title),
        icon = Icons.AutoMirrored.Outlined.Label,
        supportingText = formattedNumber,
        confirmAction = RivoDialogAction(
            label = stringResource(R.string.action_save),
            enabled = tagText.trim().isNotEmpty() && tagText.trim() != initialTag.trim(),
            onClick = {
                val clean = tagText.trim().take(30)
                if (clean.isNotEmpty()) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSaveTag(clean)
                    onDismissRequest()
                }
            }
        ),
        dismissAction = RivoDialogAction(
            label = stringResource(R.string.action_cancel),
            onClick = onDismissRequest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.tag_dialog_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = tagText,
                onValueChange = {
                    if (it.length <= 30) {
                        tagText = it
                    }
                },
                placeholder = {
                    Text(
                        stringResource(R.string.tag_dialog_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                trailingIcon = {
                    if (tagText.isNotBlank()) {
                        IconButton(onClick = { tagText = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialTag.isNotBlank() && onDeleteTag != null) {
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onDeleteTag()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_remove_tag),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Text(
                    text = "${tagText.length}/30",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tagText.length >= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
