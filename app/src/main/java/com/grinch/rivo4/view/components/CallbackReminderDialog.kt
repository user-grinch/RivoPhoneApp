package com.grinch.rivo4.view.components

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.reminder.CallbackReminderManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var isCustom by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var targetTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000L) }

    val formattedNumber = remember(phoneNumber) { formatPhoneNumber(phoneNumber) }
    val displayTarget = remember(contactName, formattedNumber) {
        if (!contactName.isNullOrBlank()) contactName else formattedNumber
    }

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.dialog_callback_reminder_title),
        icon = Icons.Outlined.Alarm,
        confirmAction = RivoDialogAction(
            label = stringResource(R.string.dialog_callback_reminder_set),
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
                text = "Remind to call $displayTarget in:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, minutes) ->
                    val isSelected = !isCustom && selectedMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            isCustom = false
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

                // Custom Date & Time Option
                FilterChip(
                    selected = isCustom,
                    onClick = {
                        showDatePicker = true
                    },
                    label = {
                        Text(
                            text = if (isCustom) selectedLabel else stringResource(R.string.reminder_custom_preset),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (isCustom) Icons.Outlined.Check else Icons.Outlined.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.dialog_callback_note_optional)) },
                placeholder = { Text(stringResource(R.string.dialog_callback_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val year = utcCal.get(Calendar.YEAR)
                            val month = utcCal.get(Calendar.MONTH)
                            val day = utcCal.get(Calendar.DAY_OF_MONTH)

                            val localCal = Calendar.getInstance().apply {
                                timeInMillis = targetTimestamp
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            targetTimestamp = localCal.timeInMillis
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialCal = remember(targetTimestamp) {
            Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context)
        )

        RivoDialog(
            onDismissRequest = { showTimePicker = false },
            title = stringResource(R.string.reminder_pick_date_time),
            icon = Icons.Outlined.Alarm,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.action_done),
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = targetTimestamp
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    targetTimestamp = cal.timeInMillis
                    val diff = (targetTimestamp - System.currentTimeMillis()) / (60 * 1000L)
                    selectedMinutes = diff.coerceAtLeast(1)
                    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    selectedLabel = formatter.format(Date(targetTimestamp))
                    isCustom = true
                    showTimePicker = false
                }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}
