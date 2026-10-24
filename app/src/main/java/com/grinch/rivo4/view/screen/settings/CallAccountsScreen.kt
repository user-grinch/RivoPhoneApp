package com.grinch.rivo4.view.screen.settings
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.PhoneMissed
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.automirrored.outlined.ContactSupport

import com.grinch.rivo4.view.components.MenuTopAppBar

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.ContactSupport
import android.content.Intent
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.CallBackgroundStore
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.QuickResponsesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SpeedDialScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VoicemailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import android.net.Uri
import androidx.compose.material.icons.outlined.PictureInPicture

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallAccountsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    
    val settingsState by prefs.settingsChanged.collectAsState()
    var speedDial by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SPEED_DIAL, true)) }
    var showCarrierInRecents by remember(settingsState) { mutableStateOf(prefs.isShowCarrierInRecents()) }
    var showCountryCode by remember(settingsState) { mutableStateOf(prefs.isShowCountryCodeEnabled()) }
    var t9Dialing by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_T9_DIALING, true)) }
    var proximitySensor by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) }
    var autoSpeakerProximity by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_SPEAKER_PROXIMITY, false)) }
    var incomingCallUI by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)) }
    var autoRedial by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, false)) }
    var redialAttempts by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, 3)) }
    var redialDelay by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_DELAY, 3000)) }
    var defaultSim by remember(settingsState) { mutableStateOf(prefs.getInt("default_sim", 0)) }
    var alwaysFullScreenCalls by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, false)) }
    var openCallScreenOnAnswer by remember(settingsState) { mutableStateOf(prefs.isOpenCallScreenOnAnswerEnabled()) }
    var pocketMode by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE, false)) }
    var postCallSummary by remember(settingsState) { mutableStateOf(prefs.isPostCallScreenEnabled()) }
    var missedCallCard by remember(settingsState) { mutableStateOf(prefs.isMissedCallCardEnabled()) }
    var floatingCallBubble by remember(settingsState) { mutableStateOf(prefs.isFloatingCallBubbleEnabled()) }
    var autoDeclineUnknown by remember(settingsState) { mutableStateOf(prefs.isAutoDeclineUnknownEnabled()) }
    var autoDeclineNonContacts by remember(settingsState) { mutableStateOf(prefs.isAutoDeclineNonContactsEnabled()) }
    var autoPasteClipboard by remember(settingsState) { mutableStateOf(prefs.isAutoPasteClipboardEnabled()) }
    var callLogLimit by remember(settingsState) { mutableStateOf(prefs.getCallLogLimit()) }
    var groupCallLogs by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GROUP_CALL_LOGS, true))
    }

    var defaultCallBg by remember(settingsState) { mutableStateOf(CallBackgroundStore.defaultModel(context)) }
    var unknownCallBg by remember(settingsState) { mutableStateOf(CallBackgroundStore.unknownModel(context)) }
    var savingDefaultBg by remember { mutableStateOf(false) }
    var savingUnknownBg by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        floatingCallBubble = prefs.isFloatingCallBubbleEnabled()
    }

    var showDefaultBgDialog by remember { mutableStateOf(false) }
    var showUnknownBgDialog by remember { mutableStateOf(false) }

    val defaultCallBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                savingDefaultBg = true
                val ok = CallBackgroundStore.saveDefault(context, uri)
                savingDefaultBg = false
                if (ok) defaultCallBg = CallBackgroundStore.defaultModelAsync(context)
            }
        }
    }

    val unknownCallBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                savingUnknownBg = true
                val ok = CallBackgroundStore.saveUnknown(context, uri)
                savingUnknownBg = false
                if (ok) unknownCallBg = CallBackgroundStore.unknownModelAsync(context)
            }
        }
    }

    var showSimDialog by remember { mutableStateOf(false) }
    var showCallWaitingDialog by remember { mutableStateOf(false) }
    var showDefaultVideoAppDialog by remember { mutableStateOf(false) }
    var defaultVideoApp by remember(settingsState) { mutableStateOf(prefs.getDefaultVideoApp()) }

    LaunchedEffect(SettingsHighlightManager.highlightEpoch) {
        if (SettingsHighlightManager.highlightEpoch == 0L) return@LaunchedEffect
        val target = SettingsHighlightManager.highlightedSettingTitle ?: return@LaunchedEffect
        val targetIndex = when {
            target.contains("Carrier", ignoreCase = true) ||
            target.contains("Speed Dial", ignoreCase = true) ||
            target.contains("Default SIM", ignoreCase = true) ||
            target.contains("Calling Accounts", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_show_carrier_in_recents)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_speed_dial)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_default_sim)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_calling_accounts)) -> 0

            target.contains("Country Code", ignoreCase = true) ||
            target.contains("T9", ignoreCase = true) ||
            target.contains("Auto-Paste", ignoreCase = true) ||
            target.contains("Clipboard", ignoreCase = true) ||
            target.contains("Proximity", ignoreCase = true) ||
            target.contains("Speaker", ignoreCase = true) ||
            target.contains("Incoming", ignoreCase = true) ||
            target.contains("Fullscreen", ignoreCase = true) ||
            target.contains("Answer", ignoreCase = true) ||
            target.contains("Pocket", ignoreCase = true) ||
            target.contains("Log Limit", ignoreCase = true) ||
            target.contains("Summary", ignoreCase = true) ||
            target.contains("Missed Call Card", ignoreCase = true) ||
            target.contains("Bubble", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_show_country_code)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_t9_dialing)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_auto_paste_clipboard)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_proximity_sensor)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_auto_speaker_proximity)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_incoming_ui)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_always_fullscreen)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_open_call_screen_on_answer_title)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_log_limit)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_missed_call_card_title)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_floating_call_bubble)) -> 1

            target.contains("Screening", ignoreCase = true) ||
            target.contains("Decline Unknown", ignoreCase = true) ||
            target.contains("Decline Non", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_auto_decline_unknown_title)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_auto_decline_non_contacts_title)) -> 2

            target.contains("Redial", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_auto_redial)) -> 3

            target.contains("Voicemail", ignoreCase = true) ||
            target.contains("Waiting", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_voicemail)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_waiting)) -> 4

            target.contains("Background", ignoreCase = true) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_default_background)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_call_unknown_background)) -> 5

            else -> -1
        }
        if (targetIndex >= 0) {
            delay(150L)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_call_title),
                navigator = navigator
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    val askEveryTimeLabel = stringResource(R.string.sim_ask_every_time)
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_sim_calling_prefs), icon = Icons.Outlined.SimCard) {
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_speed_dial),
                                supporting = if (speedDial) stringResource(R.string.settings_call_enabled) else stringResource(R.string.settings_call_disabled),
                                leadingIcon = Icons.Outlined.Speed,
                                onClick = { navigator.navigate(SpeedDialScreenDestination) }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_show_carrier_in_recents),
                                supporting = stringResource(R.string.settings_show_carrier_in_recents_supporting),
                                leadingIcon = Icons.Outlined.SimCard,
                                checked = showCarrierInRecents,
                                onCheckedChange = {
                                    showCarrierInRecents = it
                                    prefs.setShowCarrierInRecents(it)
                                }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_default_sim),
                                supporting = when(defaultSim) {
                                    0 -> askEveryTimeLabel
                                    1 -> stringResource(R.string.sim_slot_1)
                                    2 -> stringResource(R.string.sim_slot_2)
                                    else -> askEveryTimeLabel
                                },
                                leadingIcon = Icons.Outlined.SimCard,
                                onClick = { showSimDialog = true }
                            )
                        }
                        item {
                            val availableVideoApps = remember(context) { com.grinch.rivo4.view.components.VideoCallHelper.getAvailableVideoApps(context) }
                            val defaultVideoAppName = when {
                                defaultVideoApp.isBlank() || defaultVideoApp == "always_ask" -> stringResource(R.string.video_call_always_ask)
                                defaultVideoApp == "system" -> stringResource(R.string.video_call_carrier)
                                else -> availableVideoApps.find { it.id == defaultVideoApp }?.name ?: stringResource(R.string.video_call_always_ask)
                            }
                            RivoListItem(
                                headline = stringResource(R.string.video_call_default_app),
                                supporting = defaultVideoAppName,
                                leadingIcon = Icons.Default.VideoCall,
                                onClick = { showDefaultVideoAppDialog = true }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_calling_accounts),
                                supporting = stringResource(R.string.settings_call_calling_accounts_supporting),
                                leadingIcon = Icons.Outlined.Settings,
                                onClick = {
                                    try {
                                        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent("android.telecom.action.SHOW_CALL_SETTINGS").apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e3: Exception) {}
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_dialer_screen_exp), icon = Icons.Outlined.Dialpad) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_show_country_code),
                                supporting = stringResource(R.string.settings_show_country_code_supporting),
                                leadingIcon = Icons.Outlined.Public,
                                checked = showCountryCode,
                                onCheckedChange = {
                                    showCountryCode = it
                                    prefs.setShowCountryCodeEnabled(it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_call_t9_dialing),
                                supporting = stringResource(R.string.settings_call_t9_dialing_supporting),
                                leadingIcon = Icons.Outlined.Dialpad,
                                checked = t9Dialing,
                                onCheckedChange = {
                                    t9Dialing = it
                                    prefs.setBoolean(PreferenceManager.KEY_T9_DIALING, it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_auto_paste_clipboard),
                                supporting = stringResource(R.string.settings_auto_paste_clipboard_supporting),
                                leadingIcon = Icons.Outlined.ContentPaste,
                                checked = autoPasteClipboard,
                                onCheckedChange = {
                                    autoPasteClipboard = it
                                    prefs.setAutoPasteClipboardEnabled(it)
                                }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_quick_responses),
                                supporting = stringResource(R.string.settings_quick_responses_supporting),
                                leadingIcon = Icons.Outlined.Quickreply,
                                onClick = {
                                    navigator.navigate(QuickResponsesScreenDestination())
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_group_call_logs),
                                supporting = stringResource(R.string.settings_group_call_logs_supporting),
                                leadingIcon = Icons.AutoMirrored.Outlined.CallMerge,
                                checked = groupCallLogs,
                                onCheckedChange = { enabled ->
                                    groupCallLogs = enabled
                                    prefs.setBoolean(PreferenceManager.KEY_GROUP_CALL_LOGS, enabled)
                                }
                            )
                        }
                        item {
                            RivoSelectListItem(
                                headline = stringResource(R.string.settings_call_log_limit),
                                supporting = stringResource(R.string.settings_call_log_limit_supporting),
                                leadingIcon = Icons.Outlined.History,
                                options = listOf(
                                    stringResource(R.string.settings_call_log_limit_100) to 100,
                                    stringResource(R.string.settings_call_log_limit_250) to 250,
                                    stringResource(R.string.settings_call_log_limit_500) to 500,
                                    stringResource(R.string.settings_call_log_limit_1000) to 1000,
                                    stringResource(R.string.settings_call_log_limit_unlimited) to 0
                                ),
                                selectedValue = callLogLimit,
                                onValueChange = {
                                    callLogLimit = it
                                    prefs.setCallLogLimit(it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_post_call_summary),
                                supporting = stringResource(R.string.settings_post_call_summary_supporting),
                                leadingIcon = Icons.Outlined.CallEnd,
                                checked = postCallSummary,
                                onCheckedChange = {
                                    postCallSummary = it
                                    prefs.setPostCallScreenEnabled(it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_missed_call_card_title),
                                supporting = stringResource(R.string.settings_missed_call_card_supporting),
                                leadingIcon = Icons.AutoMirrored.Outlined.PhoneMissed,
                                checked = missedCallCard,
                                onCheckedChange = {
                                    missedCallCard = it
                                    prefs.setMissedCallCardEnabled(it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_floating_call_bubble),
                                supporting = if (floatingCallBubble && !android.provider.Settings.canDrawOverlays(context)) {
                                    stringResource(R.string.floating_bubble_overlay_permission_desc)
                                } else {
                                    stringResource(R.string.settings_floating_call_bubble_supporting)
                                },
                                leadingIcon = Icons.Outlined.PictureInPicture,
                                checked = floatingCallBubble,
                                onCheckedChange = { isChecked ->
                                    floatingCallBubble = isChecked
                                    prefs.setFloatingCallBubbleEnabled(isChecked)
                                    if (isChecked && !android.provider.Settings.canDrawOverlays(context)) {
                                        try {
                                            val intent = Intent(
                                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            overlayPermissionLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            try {
                                                overlayPermissionLauncher.launch(
                                                    Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_call_screening_prot), icon = Icons.Outlined.Security) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_auto_decline_unknown_title),
                                supporting = stringResource(R.string.settings_auto_decline_unknown_supporting),
                                leadingIcon = Icons.Outlined.PhoneDisabled,
                                checked = autoDeclineUnknown,
                                onCheckedChange = {
                                    autoDeclineUnknown = it
                                    prefs.setAutoDeclineUnknownEnabled(it)
                                }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_auto_decline_non_contacts_title),
                                supporting = stringResource(R.string.settings_auto_decline_non_contacts_supporting),
                                leadingIcon = Icons.Outlined.PersonOff,
                                checked = autoDeclineNonContacts,
                                onCheckedChange = {
                                    autoDeclineNonContacts = it
                                    prefs.setAutoDeclineNonContactsEnabled(it)
                                }
                            )
                        }
                    }
                }

                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_auto_redial), icon = Icons.Outlined.Replay) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_call_auto_redial),
                                supporting = stringResource(R.string.settings_call_auto_redial_supporting),
                                leadingIcon = Icons.Outlined.Replay,
                                checked = autoRedial,
                                onCheckedChange = {
                                    autoRedial = it
                                    prefs.setBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, it)
                                }
                            )
                        }
                        if (autoRedial) {
                            item {
                                RivoSelectListItem(
                                    headline = stringResource(R.string.settings_call_redial_attempts),
                                    supporting = stringResource(R.string.settings_call_redial_attempts_supporting),
                                    leadingIcon = Icons.Outlined.Refresh,
                                    options = listOf(
                                        stringResource(R.string.settings_call_redial_attempts_1) to 1,
                                        stringResource(R.string.settings_call_redial_attempts_2) to 2,
                                        stringResource(R.string.settings_call_redial_attempts_3) to 3,
                                        stringResource(R.string.settings_call_redial_attempts_5) to 5,
                                        stringResource(R.string.settings_call_redial_attempts_10) to 10
                                    ),
                                    selectedValue = redialAttempts,
                                    onValueChange = {
                                        redialAttempts = it
                                        prefs.setInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, it)
                                    }
                                )
                            }
                            item {
                                RivoSelectListItem(
                                    headline = stringResource(R.string.settings_call_redial_delay),
                                    supporting = stringResource(R.string.settings_call_redial_delay_supporting),
                                    leadingIcon = Icons.Outlined.Timer,
                                    options = listOf(
                                        stringResource(R.string.settings_call_redial_delay_1s) to 1000,
                                        stringResource(R.string.settings_call_redial_delay_2s) to 2000,
                                        stringResource(R.string.settings_call_redial_delay_3s) to 3000,
                                        stringResource(R.string.settings_call_redial_delay_5s) to 5000,
                                        stringResource(R.string.settings_call_redial_delay_10s) to 10000
                                    ),
                                    selectedValue = redialDelay,
                                    onValueChange = {
                                        redialDelay = it
                                        prefs.setInt(PreferenceManager.KEY_REDIAL_DELAY, it)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    RivoExpressiveGroup(
                        title = stringResource(R.string.settings_work_in_progress),
                        icon = Icons.Outlined.Construction,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ) {
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_voicemail),
                                supporting = stringResource(R.string.settings_call_voicemail_supporting),
                                leadingIcon = Icons.Outlined.Voicemail,
                                onClick = { navigator.navigate(VoicemailScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_waiting),
                                supporting = stringResource(R.string.settings_call_waiting_supporting),
                                leadingIcon = Icons.AutoMirrored.Outlined.PhoneCallback,
                                onClick = { showCallWaitingDialog = true }
                            )
                        }
                    }
                }

                item {
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            ScrollToTopButton(
                visible = showButton,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }

        if (showDefaultBgDialog) {
            CallBackgroundDialog(
                title = stringResource(R.string.settings_call_default_background),
                icon = Icons.Outlined.Wallpaper,
                backgroundModel = defaultCallBg,
                supportingText = stringResource(R.string.settings_call_default_background_supporting),
                isUnknown = false,
                saving = savingDefaultBg,
                onDismissRequest = { showDefaultBgDialog = false },
                onChoose = {
                    showDefaultBgDialog = false
                    defaultCallBgLauncher.launch(arrayOf("image/*"))
                },
                onRemove = {
                    showDefaultBgDialog = false
                    CallBackgroundStore.clearDefault(context)
                    defaultCallBg = null
                }
            )
        }

        if (showUnknownBgDialog) {
            CallBackgroundDialog(
                title = stringResource(R.string.settings_call_unknown_background),
                icon = Icons.AutoMirrored.Outlined.ContactSupport,
                backgroundModel = unknownCallBg,
                supportingText = stringResource(R.string.settings_call_unknown_background_supporting),
                isUnknown = true,
                saving = savingUnknownBg,
                onDismissRequest = { showUnknownBgDialog = false },
                onChoose = {
                    showUnknownBgDialog = false
                    unknownCallBgLauncher.launch(arrayOf("image/*"))
                },
                onRemove = {
                    showUnknownBgDialog = false
                    CallBackgroundStore.clearUnknown(context)
                    unknownCallBg = null
                }
            )
        }

        if (showCallWaitingDialog) {
            val callWaitingOptions = listOf(
                Triple(stringResource(R.string.settings_call_waiting_enable), "*43#", Icons.AutoMirrored.Outlined.PhoneCallback),
                Triple(stringResource(R.string.settings_call_waiting_disable), "#43#", Icons.Outlined.PhoneDisabled),
                Triple(stringResource(R.string.settings_call_waiting_check), "*#43#", Icons.Outlined.Info),
                Triple(stringResource(R.string.settings_call_waiting_system_settings), "SYSTEM_SETTINGS", Icons.Outlined.Settings)
            )
            RivoSelectionDialog(
                onDismissRequest = { showCallWaitingDialog = false },
                title = stringResource(R.string.settings_call_waiting),
                icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                items = callWaitingOptions,
                itemLabel = { option -> option.first },
                itemSupporting = { option -> if (option.second == "SYSTEM_SETTINGS") "" else option.second },
                itemIcon = { option -> option.third },
                onItemSelected = { option ->
                    showCallWaitingDialog = false
                    if (option.second == "SYSTEM_SETTINGS") {
                        try {
                            val intent = Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent("android.telecom.action.SHOW_CALL_SETTINGS").apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    } else {
                        makeCall(context, option.second)
                    }
                }
            )
        }

        if (showSimDialog) {
            val simOptions = listOf(
                stringResource(R.string.sim_ask_every_time),
                stringResource(R.string.sim_slot_1),
                stringResource(R.string.sim_slot_2)
            )
            RivoSelectionDialog(
                onDismissRequest = { showSimDialog = false },
                title = stringResource(R.string.settings_call_default_sim),
                icon = Icons.Outlined.SimCard,
                items = simOptions,
                itemLabel = { option -> option },
                itemIcon = { Icons.Outlined.SimCard },
                isSelected = { option -> simOptions.indexOf(option) == defaultSim },
                onItemSelected = { selectedLabel ->
                    val index = simOptions.indexOf(selectedLabel)
                    defaultSim = index
                    prefs.setInt("default_sim", index)
                    showSimDialog = false
                }
            )
        }

        if (showDefaultVideoAppDialog) {
            val availableApps = com.grinch.rivo4.view.components.VideoCallHelper.getAvailableVideoApps(context)
            val options = listOf("always_ask" to stringResource(R.string.video_call_always_ask)) +
                availableApps.map { it.id to it.name }
            val currentSelected = if (defaultVideoApp.isBlank()) "always_ask" else defaultVideoApp

            RivoSelectionDialog(
                onDismissRequest = { showDefaultVideoAppDialog = false },
                title = stringResource(R.string.video_call_default_app),
                icon = Icons.Default.VideoCall,
                items = options,
                itemLabel = { it.second },
                itemIcon = { Icons.Default.VideoCall },
                isSelected = { it.first == currentSelected },
                onItemSelected = { selectedOption ->
                    val newId = if (selectedOption.first == "always_ask") "" else selectedOption.first
                    defaultVideoApp = newId
                    prefs.setDefaultVideoApp(newId)
                    showDefaultVideoAppDialog = false
                }
            )
        }
    }
}


@Composable
private fun CallBackgroundSettingTile(
    headline: String,
    supporting: String,
    backgroundModel: Any?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    RivoListItem(
        headline = headline,
        supporting = supporting,
        leadingIcon = icon,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (backgroundModel != null) {
                    AsyncImage(
                        model = backgroundModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun CallBackgroundDialog(
    title: String,
    icon: ImageVector,
    backgroundModel: Any?,
    supportingText: String,
    isUnknown: Boolean,
    saving: Boolean,
    onDismissRequest: () -> Unit,
    onChoose: () -> Unit,
    onRemove: () -> Unit
) {
    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (backgroundModel != null) {
                    AsyncImage(
                        model = backgroundModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isUnknown) Icons.Outlined.PersonOff else Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isUnknown) "Unknown Caller" else "John Doe",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Incoming call...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isUnknown) Icons.AutoMirrored.Outlined.ContactSupport else Icons.Outlined.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_call_background_no_preview),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (saving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            RivoListItem(
                headline = if (backgroundModel != null) {
                    stringResource(R.string.settings_call_background_change_gallery)
                } else {
                    stringResource(R.string.settings_call_background_choose_gallery)
                },
                leadingIcon = Icons.Outlined.PhotoLibrary,
                onClick = onChoose
            )

            if (backgroundModel != null) {
                RivoListItem(
                    headline = stringResource(R.string.settings_call_background_remove),
                    leadingIcon = Icons.Outlined.Delete,
                    headlineColor = MaterialTheme.colorScheme.error,
                    onClick = onRemove
                )
            }
        }
    }
}
