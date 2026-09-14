package com.grinch.rivo4.view.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.reminder.CallbackReminderManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallbackReminderDialog(
    phoneNumber: String,
    contactName: String?,
    onDismissRequest: () -> Unit,
    onReminderScheduled: (() -> Unit)? = null,
    reminderManager: CallbackReminderManager = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tomorrowMorningMinutes = remember {
        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = tomorrow.timeInMillis - now.timeInMillis
        (diffMillis / (1000 * 60)).coerceAtLeast(15)
    }

    val presets = remember(tomorrowMorningMinutes) {
        listOf(
            "15 mins" to 15L,
            "30 mins" to 30L,
            "1 hour" to 60L,
            "3 hours" to 180L,
            "Tomorrow 9 AM" to tomorrowMorningMinutes
        )
    }

    var selectedMinutes by remember { mutableLongStateOf(15L) }
    var selectedLabel by remember { mutableStateOf("15 mins") }
    var noteText by remember { mutableStateOf("") }

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = "Callback Reminder",
        icon = Icons.Outlined.Alarm,
        confirmAction = RivoDialogAction(
            label = "Set Reminder",
            onClick = {
                scope.launch {
                    reminderManager.scheduleReminder(
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        delayMinutes = selectedMinutes,
                        note = noteText.trim().ifEmpty { null }
                    )
                    Toast.makeText(
                        context,
                        "Reminder set for $selectedLabel",
                        Toast.LENGTH_SHORT
                    ).show()
                    onReminderScheduled?.invoke()
                    onDismissRequest()
                }
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Remind to call ${contactName?.ifBlank { null } ?: phoneNumber} in:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, minutes) ->
                    val isSelected = selectedMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMinutes = minutes
                            selectedLabel = label
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Call about the proposal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
