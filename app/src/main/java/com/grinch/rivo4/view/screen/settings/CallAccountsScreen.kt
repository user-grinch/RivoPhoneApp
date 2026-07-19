package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.telecom.TelecomManager

import com.ramcosta.composedestinations.generated.destinations.SpeedDialScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VoicemailScreenDestination

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
    var dialpadStyle by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_STYLE, 0)) }
    var dialpadLayout by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_LAYOUT, 0)) }
    var autoRedial by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, false)) }
    var redialAttempts by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, 3)) }
    var redialDelay by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_DELAY, 3000)) }
    var defaultSim by remember(settingsState) { mutableStateOf(prefs.getInt("default_sim", 0)) }
    
    var showSimDialog by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showButton,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
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
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
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
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
                        headline = stringResource(R.string.settings_call_dialpad_style),
                        supporting = stringResource(R.string.settings_call_dialpad_style_supporting),
                        leadingIcon = Icons.Outlined.Dialpad,
                        options = listOf(
                            stringResource(R.string.settings_call_dialpad_style_modern) to 0,
                            stringResource(R.string.settings_call_dialpad_style_classic) to 1,
                            stringResource(R.string.settings_call_dialpad_style_minimal) to 2
                        ),
                        selectedValue = dialpadStyle,
                        onValueChange = {
                            dialpadStyle = it
                            prefs.setInt(PreferenceManager.KEY_DIALPAD_STYLE, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
                        headline = stringResource(R.string.settings_call_dialpad_layout),
                        supporting = stringResource(R.string.settings_call_dialpad_layout_supporting),
                        leadingIcon = Icons.Outlined.AlignHorizontalLeft,
                        options = listOf(
                            stringResource(R.string.option_standard) to 0,
                            stringResource(R.string.settings_call_dialpad_layout_left) to 1,
                            stringResource(R.string.settings_call_dialpad_layout_right) to 2
                        ),
                        selectedValue = dialpadLayout,
                        onValueChange = {
                            dialpadLayout = it
                            prefs.setInt(PreferenceManager.KEY_DIALPAD_LAYOUT, it)
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
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoListItem(
                        headline = stringResource(R.string.settings_call_voicemail),
                        supporting = stringResource(R.string.settings_call_voicemail_supporting),
                        leadingIcon = Icons.Outlined.Voicemail,
                        onClick = { navigator.navigate(VoicemailScreenDestination) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showSimDialog) {
        RivoDialog(
            onDismissRequest = { showSimDialog = false },
            title = stringResource(R.string.settings_call_default_sim),
            icon = Icons.Outlined.SimCard,
            dismissButton = {
                TextButton(onClick = { showSimDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            listOf(stringResource(R.string.sim_ask_every_time), stringResource(R.string.sim_slot_1), stringResource(R.string.sim_slot_2)).forEachIndexed { index, label ->
                Surface(
                    onClick = {
                        defaultSim = index
                        prefs.setInt("default_sim", index)
                        showSimDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (defaultSim == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultSim == index,
                            onClick = {
                                defaultSim = index
                                prefs.setInt("default_sim", index)
                                showSimDialog = false
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = if (defaultSim == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
