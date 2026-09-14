package com.grinch.rivo4.view.screen.settings

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
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.QuickResponsesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SpeedDialScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VoicemailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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
    var t9Dialing by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_T9_DIALING, true)) }
    var proximitySensor by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) }
    var incomingCallUI by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)) }
    var autoRedial by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, false)) }
    var redialAttempts by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, 3)) }
    var redialDelay by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_DELAY, 3000)) }
    var defaultSim by remember(settingsState) { mutableStateOf(prefs.getInt("default_sim", 0)) }
    var alwaysFullScreenCalls by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, false)) }
    var pocketMode by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE, false)) }
    var floatingBubble by remember(settingsState) { mutableStateOf(prefs.isFloatingCallBubbleEnabled()) }
    var showRecentsStats by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_RECENTS_STATS, true)) }
    var postCallSummary by remember(settingsState) { mutableStateOf(prefs.isPostCallScreenEnabled()) }

    var defaultCallBg by remember(settingsState) { mutableStateOf(CallBackgroundStore.defaultModel(context)) }
    var savingCallBg by remember { mutableStateOf(false) }
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
                savingCallBg = true
                val ok = CallBackgroundStore.saveDefault(context, uri)
                savingCallBg = false
                if (ok) defaultCallBg = CallBackgroundStore.defaultModelAsync(context)
            }
        }
    }

    var showSimDialog by remember { mutableStateOf(false) }
    var showCallWaitingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_call_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    val askEveryTimeLabel = stringResource(R.string.sim_ask_every_time)
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_speed_dial),
                            supporting = if (speedDial) stringResource(R.string.settings_call_enabled) else stringResource(R.string.settings_call_disabled),
                            leadingIcon = Icons.Outlined.Speed,
                            onClick = { navigator.navigate(SpeedDialScreenDestination) }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

                item {
                    RivoExpressiveCard {
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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_call_proximity_sensor),
                            supporting = stringResource(R.string.settings_call_proximity_sensor_supporting),
                            leadingIcon = Icons.Outlined.Sensors,
                            checked = proximitySensor,
                            onCheckedChange = {
                                proximitySensor = it
                                prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, it)
                            }
                        )
                        RivoVisualOptionSelectorRow(
                            headline = stringResource(R.string.settings_call_incoming_ui),
                            supporting = stringResource(R.string.settings_call_incoming_ui_supporting),
                            leadingIcon = Icons.Outlined.PhoneInTalk,
                            options = listOf(
                                stringResource(R.string.settings_call_incoming_ui_horizontal_swipe) to 0,
                                stringResource(R.string.settings_call_incoming_ui_buttons) to 1,
                                stringResource(R.string.settings_call_incoming_ui_slide_ios) to 2,
                                stringResource(R.string.settings_call_incoming_ui_vertical_swipe) to 3
                            ),
                            selectedValue = incomingCallUI,
                            onValueChange = {
                                incomingCallUI = it
                                prefs.setInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, it)
                            },
                            tileWidth = 110.dp,
                            tileHeight = 76.dp
                        ) { value, _ ->
                            IncomingCallUiPreview(value)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_call_always_fullscreen),
                            supporting = stringResource(R.string.settings_call_always_fullscreen_supporting),
                            leadingIcon = Icons.Outlined.Fullscreen,
                            checked = alwaysFullScreenCalls,
                            onCheckedChange = {
                                alwaysFullScreenCalls = it
                                prefs.setBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Pocket Mode",
                            supporting = "Prevent accidental touches during incoming calls when phone is in pocket",
                            leadingIcon = Icons.Outlined.ScreenLockPortrait,
                            checked = pocketMode,
                            onCheckedChange = {
                                pocketMode = it
                                prefs.setBoolean(PreferenceManager.KEY_POCKET_MODE, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoListItem(
                            headline = "Quick Responses",
                            supporting = "Manage canned SMS decline responses for incoming calls",
                            leadingIcon = Icons.Outlined.Quickreply,
                            onClick = {
                                navigator.navigate(QuickResponsesScreenDestination())
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Floating Ongoing Calls",
                            supporting = "Show a movable bubble with call timer and quick controls when leaving call",
                            leadingIcon = Icons.Outlined.PictureInPicture,
                            checked = floatingBubble,
                            onCheckedChange = { enable ->
                                if (enable && !android.provider.Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                floatingBubble = enable
                                prefs.setFloatingCallBubbleEnabled(enable)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Show Daily Stats in Recents",
                            supporting = "Display summary cards for calls, talk time, and missed calls in the recents screen",
                            leadingIcon = Icons.Outlined.Analytics,
                            checked = showRecentsStats,
                            onCheckedChange = {
                                showRecentsStats = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_RECENTS_STATS, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Post-Call Summary",
                            supporting = "Show call duration and quick actions (call back, SMS, note) after ending a call",
                            leadingIcon = Icons.Outlined.CallEnd,
                            checked = postCallSummary,
                            onCheckedChange = {
                                postCallSummary = it
                                prefs.setPostCallScreenEnabled(it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
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
                        if (autoRedial) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

                item {
                    RivoSectionHeader(
                        title = "Work in Progress",
                        icon = Icons.Outlined.Construction
                    )
                    Spacer(Modifier.height(8.dp))
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ) {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_voicemail),
                            supporting = stringResource(R.string.settings_call_voicemail_supporting),
                            leadingIcon = Icons.Outlined.Voicemail,
                            onClick = { navigator.navigate(VoicemailScreenDestination) }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_waiting),
                            supporting = stringResource(R.string.settings_call_waiting_supporting),
                            leadingIcon = Icons.Outlined.PhoneCallback,
                            onClick = { showCallWaitingDialog = true }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_default_background),
                            supporting = if (defaultCallBg != null) {
                                stringResource(R.string.settings_call_default_background_set)
                            } else {
                                stringResource(R.string.settings_call_default_background_none)
                            },
                            leadingIcon = Icons.Outlined.Wallpaper,
                            onClick = { defaultCallBgLauncher.launch(arrayOf("image/*")) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .aspectRatio(2f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (defaultCallBg != null) {
                                AsyncImage(
                                    model = defaultCallBg,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            if (savingCallBg) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { defaultCallBgLauncher.launch(arrayOf("image/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (defaultCallBg != null) {
                                        stringResource(R.string.settings_call_default_background_change)
                                    } else {
                                        stringResource(R.string.settings_call_default_background_choose)
                                    }
                                )
                            }
                            if (defaultCallBg != null) {
                                OutlinedButton(
                                    onClick = {
                                        CallBackgroundStore.clearDefault(context)
                                        defaultCallBg = null
                                    }
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_remove))
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.settings_call_default_background_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item {
                    com.grinch.rivo4.view.components.ad.BannerAd()
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

        if (showCallWaitingDialog) {
            val callWaitingOptions = listOf(
                Triple(stringResource(R.string.settings_call_waiting_enable), "*43#", Icons.AutoMirrored.Outlined.PhoneCallback),
                Triple(stringResource(R.string.settings_call_waiting_disable), "#43#", Icons.Outlined.PhoneDisabled),
                Triple(stringResource(R.string.settings_call_waiting_check), "*#43#", Icons.Outlined.Info)
            )
            RivoSelectionDialog(
                onDismissRequest = { showCallWaitingDialog = false },
                title = stringResource(R.string.settings_call_waiting),
                icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                items = callWaitingOptions,
                itemLabel = { option -> option.first },
                itemSupporting = { option -> option.second },
                itemIcon = { option -> option.third },
                onItemSelected = { option ->
                    showCallWaitingDialog = false
                    makeCall(context, option.second)
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
    }
}
