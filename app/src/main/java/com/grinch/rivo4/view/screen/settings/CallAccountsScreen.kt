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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

import com.ramcosta.composedestinations.generated.destinations.SpeedDialScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VoicemailScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallAccountsScreen(
    navigator: DestinationsNavigator
) {
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
    var incomingCallPopup by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INCOMING_CALL_POPUP, false)) }
    var incomingCallUI by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)) }
    var dialpadStyle by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_STYLE, 0)) }
    var autoRedial by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, false)) }
    var redialAttempts by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, 3)) }
    var redialDelay by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_REDIAL_DELAY, 3000)) }
    var defaultSim by remember(settingsState) { mutableStateOf(prefs.getInt("default_sim", 0)) }
    
    var showSimDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Speed dial",
                        supporting = if (speedDial) "Enabled" else "Disabled",
                        leadingIcon = Icons.Outlined.Speed,
                        onClick = { navigator.navigate(SpeedDialScreenDestination) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoListItem(
                        headline = "Default SIM",
                        supporting = when(defaultSim) {
                            0 -> "Ask every time"
                            1 -> "SIM 1"
                            2 -> "SIM 2"
                            else -> "Ask every time"
                        },
                        leadingIcon = Icons.Outlined.SimCard,
                        onClick = { showSimDialog = true }
                    )
                }
            }

            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "T9 Dialing",
                        supporting = "Predicts words from numeric keypad inputs",
                        leadingIcon = Icons.Outlined.Dialpad,
                        checked = t9Dialing,
                        onCheckedChange = {
                            t9Dialing = it
                            prefs.setBoolean(PreferenceManager.KEY_T9_DIALING, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSwitchListItem(
                        headline = "Proximity Sensor",
                        supporting = "Turn off screen during calls when near ear",
                        leadingIcon = Icons.Outlined.Sensors,
                        checked = proximitySensor,
                        onCheckedChange = {
                            proximitySensor = it
                            prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSwitchListItem(
                        headline = "Incoming Call Popup",
                        supporting = "Show incoming calls as a popup when phone is in use",
                        leadingIcon = Icons.Outlined.PictureInPicture,
                        checked = incomingCallPopup,
                        onCheckedChange = {
                            incomingCallPopup = it
                            prefs.setBoolean(PreferenceManager.KEY_INCOMING_CALL_POPUP, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
                        headline = "Incoming Call UI",
                        supporting = "Choose how to answer incoming calls",
                        leadingIcon = Icons.Outlined.PhoneInTalk,
                        options = listOf(
                            "Horizontal Swipe" to 0,
                            "Buttons" to 1
                        ),
                        selectedValue = incomingCallUI,
                        onValueChange = {
                            incomingCallUI = it
                            prefs.setInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
                        headline = "Dialpad Style",
                        supporting = "Visual appearance of the phone dialer",
                        leadingIcon = Icons.Outlined.Dialpad,
                        options = listOf(
                            "Modern" to 0,
                            "Classic" to 1,
                            "Minimal" to 2
                        ),
                        selectedValue = dialpadStyle,
                        onValueChange = {
                            dialpadStyle = it
                            prefs.setInt(PreferenceManager.KEY_DIALPAD_STYLE, it)
                        }
                    )
                }
            }

            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Auto Redial",
                        supporting = "Automatically redial if the line is busy",
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
                            headline = "Redial Attempts",
                            supporting = "Maximum number of redial attempts",
                            leadingIcon = Icons.Outlined.Refresh,
                            options = listOf(
                                "1 attempt" to 1,
                                "2 attempts" to 2,
                                "3 attempts" to 3,
                                "5 attempts" to 5,
                                "10 attempts" to 10
                            ),
                            selectedValue = redialAttempts,
                            onValueChange = {
                                redialAttempts = it
                                prefs.setInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "Redial Delay",
                            supporting = "Delay between redial attempts",
                            leadingIcon = Icons.Outlined.Timer,
                            options = listOf(
                                "1 second" to 1000,
                                "2 seconds" to 2000,
                                "3 seconds" to 3000,
                                "5 seconds" to 5000,
                                "10 seconds" to 10000
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
                        headline = "Voicemail",
                        supporting = "Configure your mailbox",
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
            title = "Default SIM",
            icon = Icons.Outlined.SimCard,
            dismissButton = {
                TextButton(onClick = { showSimDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            listOf("Ask every time", "SIM 1", "SIM 2").forEachIndexed { index, label ->
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
