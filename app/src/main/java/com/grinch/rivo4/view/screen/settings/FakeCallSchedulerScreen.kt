package com.grinch.rivo4.view.screen.settings

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.fakecall.FakeCallManager
import com.grinch.rivo4.controller.fakecall.FakeCallSchedule
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoAvatarShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun FakeCallSchedulerScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val schedules by FakeCallManager.schedules.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        FakeCallManager.init(context)
    }

    var isCreatingSchedule by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // State for creating new fake call
    var newCallerName by remember { mutableStateOf("Mom") }
    var newPhoneNumber by remember { mutableStateOf("+1 (555) 019-2834") }
    var newPhotoUri by remember { mutableStateOf<String?>(null) }
    var newVibrate by remember { mutableStateOf(FakeCallManager.shouldVibrateOnRing(context)) }
    var logFakeCalls by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_LOG_FAKE_CALLS, true)) }
    var targetTimestamp by remember {
        mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }.timeInMillis)
    }

    fun resetNewScheduleForm() {
        newCallerName = "Mom"
        newPhoneNumber = "+1 (555) 019-2834"
        newPhotoUri = null
        newVibrate = FakeCallManager.shouldVibrateOnRing(context)
        targetTimestamp = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }.timeInMillis
    }

    // Live countdown update ticker
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val avatarShapeIndex = remember { prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0) }
    val avatarShape = rivoAvatarShape(avatarShapeIndex)

    BackHandler(enabled = isCreatingSchedule) {
        isCreatingSchedule = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCreatingSchedule) {
                            stringResource(R.string.fake_call_new_schedule)
                        } else {
                            stringResource(R.string.fake_call_title)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isCreatingSchedule) {
                                isCreatingSchedule = false
                            } else {
                                navigator.navigateUp()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (isCreatingSchedule) {
                        IconButton(
                            onClick = {
                                val schedule = FakeCallSchedule(
                                    callerName = newCallerName.trim().ifEmpty { "Mom" },
                                    phoneNumber = newPhoneNumber.trim().ifEmpty { "+1 (555) 019-2834" },
                                    photoUri = newPhotoUri,
                                    triggerTimestampMillis = targetTimestamp,
                                    vibrate = newVibrate
                                )
                                val success = FakeCallManager.scheduleCall(context, schedule)
                                if (success) {
                                    isCreatingSchedule = false
                                    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(targetTimestamp))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.fake_call_success_scheduled, timeText)
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "Could not schedule alarm. Check exact alarm permission.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.fake_call_schedule_btn),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val defaultSchedule = FakeCallSchedule(
                                    callerName = "Mom",
                                    phoneNumber = "+1 (555) 019-2834",
                                    triggerTimestampMillis = System.currentTimeMillis(),
                                    vibrate = FakeCallManager.shouldVibrateOnRing(context)
                                )
                                FakeCallManager.triggerCallNow(context, defaultSchedule)
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.PhoneCallback,
                                contentDescription = stringResource(R.string.fake_call_test_now),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isCreatingSchedule && schedules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        resetNewScheduleForm()
                        isCreatingSchedule = true
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fake_call_new_schedule), fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (isCreatingSchedule) {
            // Dedicated Full-page Schedule View (MD3 Expressive)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Caller Information
                item {
                    RivoExpressiveCard(
                        title = stringResource(R.string.fake_call_caller_details),
                        icon = Icons.Outlined.Person
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = avatarShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    if (!newPhotoUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = newPhotoUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { showContactPicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.fake_call_pick_contact))
                                }
                            }

                            OutlinedTextField(
                                value = newCallerName,
                                onValueChange = { newCallerName = it },
                                label = { Text(stringResource(R.string.fake_call_name)) },
                                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newPhoneNumber,
                                onValueChange = { newPhoneNumber = it },
                                label = { Text(stringResource(R.string.fake_call_number)) },
                                leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Card 2: Schedule Date & Time
                item {
                    RivoExpressiveCard(
                        title = stringResource(R.string.fake_call_schedule_time),
                        icon = Icons.Outlined.Schedule
                    ) {
                        val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(targetTimestamp))
                        val is24 = DateFormat.is24HourFormat(context)
                        val timeFormatPattern = if (is24) "HH:mm" else "hh:mm a"
                        val timeStr = SimpleDateFormat(timeFormatPattern, Locale.getDefault()).format(Date(targetTimestamp))

                        RivoListItem(
                            headline = dateStr,
                            supporting = stringResource(R.string.fake_call_select_date),
                            leadingIcon = Icons.Outlined.CalendarToday,
                            onClick = { showDatePicker = true }
                        )

                        RivoListItem(
                            headline = timeStr,
                            supporting = stringResource(R.string.fake_call_select_time),
                            leadingIcon = Icons.Outlined.AccessTime,
                            onClick = { showTimePicker = true }
                        )
                    }
                }

                // Card 3: Call Options
                item {
                    RivoExpressiveCard(
                        title = stringResource(R.string.fake_call_options),
                        icon = Icons.Outlined.Settings
                    ) {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.fake_call_vibrate),
                            supporting = stringResource(R.string.fake_call_vibrate_desc),
                            leadingIcon = Icons.Outlined.Vibration,
                            checked = newVibrate,
                            onCheckedChange = { newVibrate = it }
                        )
                    }
                }

                // Card 4: Action Buttons
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val schedule = FakeCallSchedule(
                                    callerName = newCallerName.trim().ifEmpty { "Mom" },
                                    phoneNumber = newPhoneNumber.trim().ifEmpty { "+1 (555) 019-2834" },
                                    photoUri = newPhotoUri,
                                    triggerTimestampMillis = targetTimestamp,
                                    vibrate = newVibrate
                                )
                                val success = FakeCallManager.scheduleCall(context, schedule)
                                if (success) {
                                    isCreatingSchedule = false
                                    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(targetTimestamp))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.fake_call_success_scheduled, timeText)
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "Could not schedule alarm. Check exact alarm permission.", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.fake_call_schedule_btn),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val instant = FakeCallSchedule(
                                    callerName = newCallerName.trim().ifEmpty { "Mom" },
                                    phoneNumber = newPhoneNumber.trim().ifEmpty { "+1 (555) 019-2834" },
                                    photoUri = newPhotoUri,
                                    triggerTimestampMillis = System.currentTimeMillis(),
                                    vibrate = newVibrate
                                )
                                isCreatingSchedule = false
                                FakeCallManager.triggerCallNow(context, instant)
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.fake_call_test_now),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else if (schedules.isEmpty()) {
            // Empty State (Expressive MD3)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val emptyMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.3f }
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = emptyMorph,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.PhoneCallback,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.fake_call_no_schedules_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.fake_call_no_schedules_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                resetNewScheduleForm()
                                isCreatingSchedule = true
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.fake_call_schedule_btn))
                        }

                        FilledTonalButton(
                            onClick = {
                                val instant = FakeCallSchedule(
                                    callerName = "Mom",
                                    phoneNumber = "+1 (555) 019-2834",
                                    triggerTimestampMillis = System.currentTimeMillis(),
                                    vibrate = FakeCallManager.shouldVibrateOnRing(context)
                                )
                                FakeCallManager.triggerCallNow(context, instant)
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.fake_call_test_now))
                        }
                    }
                }
            }
        } else {
            // Multiple Schedules List (Expressive MD3)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Next upcoming hero banner
                item {
                    RivoExpressiveCard(
                        title = "Call History Settings",
                        icon = Icons.Outlined.History
                    ) {
                        RivoSwitchListItem(
                            headline = "Save to Call History",
                            supporting = "Answered and missed fake calls will show up in Recents",
                            leadingIcon = Icons.Outlined.History,
                            checked = logFakeCalls,
                            onCheckedChange = {
                                logFakeCalls = it
                                prefs.setBoolean(PreferenceManager.KEY_LOG_FAKE_CALLS, it)
                            }
                        )
                    }
                }

                val nextSchedule = schedules.firstOrNull()
                if (nextSchedule != null) {
                    val remainingSeconds = ((nextSchedule.triggerTimestampMillis - currentTimeMillis) / 1000L).coerceAtLeast(0L)
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    val countdownStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                    val formattedTime = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault()).format(Date(nextSchedule.triggerTimestampMillis))

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Alarm,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.fake_call_scheduled_banner),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = stringResource(R.string.fake_call_time_calling_in, countdownStr),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(52.dp),
                                        shape = avatarShape,
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        if (!nextSchedule.photoUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = nextSchedule.photoUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = nextSchedule.callerName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = nextSchedule.phoneNumber,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            FakeCallManager.removeSchedule(context, nextSchedule.id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.fake_call_deleted))
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.action_cancel))
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            FakeCallManager.triggerCallNow(context, nextSchedule)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.fake_call_now))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section header for all schedules
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.fake_call_schedules_title)} (${schedules.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (schedules.size > 1) {
                            TextButton(onClick = { showClearAllConfirm = true }) {
                                Text(
                                    stringResource(R.string.fake_call_clear_all),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Items list
                items(schedules, key = { it.id }) { itemSchedule ->
                    val remainingSeconds = ((itemSchedule.triggerTimestampMillis - currentTimeMillis) / 1000L).coerceAtLeast(0L)
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    val countdownStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                    val formattedDate = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault()).format(Date(itemSchedule.triggerTimestampMillis))

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = avatarShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (!itemSchedule.photoUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = itemSchedule.photoUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemSchedule.callerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = itemSchedule.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$formattedDate ($countdownStr)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { FakeCallManager.triggerCallNow(context, itemSchedule) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.fake_call_now),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        FakeCallManager.removeSchedule(context, itemSchedule.id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.fake_call_deleted))
                                        }
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.fake_call_delete_schedule),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Material Design 3 Expressive Date Picker Dialog
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

    // Material Design 3 Expressive Time Picker Dialog
    if (showTimePicker) {
        val initialCal = remember(targetTimestamp) {
            Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = targetTimestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        targetTimestamp = cal.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.fake_call_select_time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Contact Picker Dialog
    if (showContactPicker) {
        ContactPickerDialog(
            contacts = allContacts,
            onDismissRequest = { showContactPicker = false },
            onContactSelected = { contact ->
                newCallerName = contact.name
                val num = contact.phoneNumbers.firstOrNull()
                if (!num.isNullOrEmpty()) {
                    newPhoneNumber = num
                }
                newPhotoUri = contact.photoUri
                showContactPicker = false
            }
        )
    }

    // Confirmation dialog for clearing all schedules
    if (showClearAllConfirm) {
        RivoDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = stringResource(R.string.fake_call_clear_all),
            supportingText = stringResource(R.string.fake_call_clear_all_confirm),
            icon = Icons.Outlined.Delete,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.action_clear),
                destructive = true,
                onClick = {
                    FakeCallManager.clearAllSchedules(context)
                    showClearAllConfirm = false
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.fake_call_cancelled))
                    }
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_cancel),
                onClick = { showClearAllConfirm = false }
            )
        ) {}
    }
}
