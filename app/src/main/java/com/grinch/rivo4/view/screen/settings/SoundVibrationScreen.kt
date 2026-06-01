package com.grinch.rivo4.view.screen.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SoundVibrationScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    
    var dtmfTone by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, true)) }
    var dialpadVibration by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, true)) }
    var vibrateOnAnswer by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VIBRATE_ON_ANSWER, true)) }
    var vibrateOnHangup by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VIBRATE_ON_HANGUP, false)) }
    val settingsState by prefs.settingsChanged.collectAsState()

    var dtmfToneVolume by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DTMF_TONE_VOLUME, 1)) }
    var dialpadVibrationStrength by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_VIBRATION_STRENGTH, 1)) }
    var hapticListScroll by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound & Vibration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "DTMF tone",
                        supporting = "Dialpad tone that plays during keypress",
                        leadingIcon = Icons.Outlined.Audiotrack,
                        checked = dtmfTone,
                        onCheckedChange = {
                            dtmfTone = it
                            prefs.setBoolean(PreferenceManager.KEY_DTMF_TONE, it)
                        }
                    )
                    if (dtmfTone) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "DTMF Tone Volume",
                            supporting = "Volume level for dialpad keypress tones",
                            leadingIcon = Icons.Outlined.VolumeUp,
                            options = listOf(
                                "Low" to 0,
                                "Normal" to 1,
                                "High" to 2
                            ),
                            selectedValue = dtmfToneVolume,
                            onValueChange = {
                                dtmfToneVolume = it
                                prefs.setInt(PreferenceManager.KEY_DTMF_TONE_VOLUME, it)
                            }
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSwitchListItem(
                        headline = "Dialpad vibration",
                        supporting = "Dialpad vibration that plays during keypress",
                        leadingIcon = Icons.Outlined.Vibration,
                        checked = dialpadVibration,
                        onCheckedChange = {
                            dialpadVibration = it
                            prefs.setBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, it)
                        }
                    )
                    if (dialpadVibration) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "Vibration Feedback Strength",
                            supporting = "Haptic feedback strength for keypresses",
                            leadingIcon = Icons.Outlined.Speed,
                            options = listOf(
                                "Light" to 0,
                                "Medium" to 1,
                                "Strong" to 2
                            ),
                            selectedValue = dialpadVibrationStrength,
                            onValueChange = {
                                dialpadVibrationStrength = it
                                prefs.setInt(PreferenceManager.KEY_DIALPAD_VIBRATION_STRENGTH, it)
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Vibrate on Answer",
                        supporting = "Vibrate when the other party answers",
                        leadingIcon = Icons.Outlined.Vibration,
                        checked = vibrateOnAnswer,
                        onCheckedChange = {
                            vibrateOnAnswer = it
                            prefs.setBoolean(PreferenceManager.KEY_VIBRATE_ON_ANSWER, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSwitchListItem(
                        headline = "Vibrate on Hang up",
                        supporting = "Vibrate when the call ends",
                        leadingIcon = Icons.Outlined.Vibration,
                        checked = vibrateOnHangup,
                        onCheckedChange = {
                            vibrateOnHangup = it
                            prefs.setBoolean(PreferenceManager.KEY_VIBRATE_ON_HANGUP, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSwitchListItem(
                        headline = "Haptic Scrolling feedback",
                        supporting = "Vibrate subtly when scrolling lists",
                        leadingIcon = Icons.Outlined.Gesture,
                        checked = hapticListScroll,
                        onCheckedChange = {
                            hapticListScroll = it
                            prefs.setBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, it)
                        }
                    )
                }
            }

            item {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Ringtone Settings",
                        supporting = "Open system sound settings",
                        leadingIcon = Icons.Outlined.MusicNote,
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
