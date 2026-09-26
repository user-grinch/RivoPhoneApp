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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
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
    var holdZeroForPlus by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HOLD_ZERO_FOR_PLUS, true)) }
    var vibrateOnAnswer by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VIBRATE_ON_ANSWER, true)) }
    var vibrateOnHangup by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VIBRATE_ON_HANGUP, false)) }
    val settingsState by prefs.settingsChanged.collectAsState()

    var hapticListScroll by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)) }
    var missedCallNotifications by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATIONS, true)) }
    var flipToSilence by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FLIP_TO_SILENCE, false)) }
    var volumeSqueezeDnd by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_SQUEEZE_DND, false)) }
    var dndDuringCalls by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DND_DURING_CALLS, false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_sound_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RivoExpressiveGroup(title = "Dialpad & Tones") {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_dtmf_tone),
                            supporting = stringResource(R.string.settings_sound_dtmf_tone_supporting),
                            leadingIcon = Icons.Outlined.Audiotrack,
                            checked = dtmfTone,
                            onCheckedChange = {
                                dtmfTone = it
                                prefs.setBoolean(PreferenceManager.KEY_DTMF_TONE, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_dialpad_vibration),
                            supporting = stringResource(R.string.settings_sound_dialpad_vibration_supporting),
                            leadingIcon = Icons.Outlined.Vibration,
                            checked = dialpadVibration,
                            onCheckedChange = {
                                dialpadVibration = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_hold_zero_for_plus),
                            supporting = stringResource(R.string.settings_sound_hold_zero_for_plus_supporting),
                            leadingIcon = Icons.Outlined.Add,
                            checked = holdZeroForPlus,
                            onCheckedChange = {
                                holdZeroForPlus = it
                                prefs.setBoolean(PreferenceManager.KEY_HOLD_ZERO_FOR_PLUS, it)
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(title = "Call Vibration & Haptics") {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_vibrate_on_answer),
                            supporting = stringResource(R.string.settings_sound_vibrate_on_answer_supporting),
                            leadingIcon = Icons.Outlined.Vibration,
                            checked = vibrateOnAnswer,
                            onCheckedChange = {
                                vibrateOnAnswer = it
                                prefs.setBoolean(PreferenceManager.KEY_VIBRATE_ON_ANSWER, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_vibrate_on_hangup),
                            supporting = stringResource(R.string.settings_sound_vibrate_on_hangup_supporting),
                            leadingIcon = Icons.Outlined.Vibration,
                            checked = vibrateOnHangup,
                            onCheckedChange = {
                                vibrateOnHangup = it
                                prefs.setBoolean(PreferenceManager.KEY_VIBRATE_ON_HANGUP, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_haptic_scroll),
                            supporting = stringResource(R.string.settings_sound_haptic_scroll_supporting),
                            leadingIcon = Icons.Outlined.Gesture,
                            checked = hapticListScroll,
                            onCheckedChange = {
                                hapticListScroll = it
                                prefs.setBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, it)
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(title = "Gestures & Do Not Disturb") {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_flip_to_silence),
                            supporting = stringResource(R.string.settings_sound_flip_to_silence_supporting),
                            leadingIcon = Icons.Outlined.ScreenRotation,
                            checked = flipToSilence,
                            onCheckedChange = {
                                flipToSilence = it
                                prefs.setBoolean(PreferenceManager.KEY_FLIP_TO_SILENCE, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_dnd_during_calls),
                            supporting = stringResource(R.string.settings_sound_dnd_during_calls_supporting),
                            leadingIcon = Icons.Outlined.DoNotDisturbOn,
                            checked = dndDuringCalls,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                                        !notificationManager.isNotificationPolicyAccessGranted
                                    ) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                            android.widget.Toast.makeText(context, "Grant Do Not Disturb permission to use this feature", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (_: Exception) {}
                                    }
                                }
                                dndDuringCalls = enabled
                                prefs.setBoolean(PreferenceManager.KEY_DND_DURING_CALLS, enabled)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = "Volume Squeeze for DND",
                            supporting = "Press both volume buttons together to toggle Do Not Disturb",
                            leadingIcon = Icons.Outlined.DoNotDisturbOn,
                            checked = volumeSqueezeDnd,
                            onCheckedChange = {
                                volumeSqueezeDnd = it
                                prefs.setBoolean(PreferenceManager.KEY_VOLUME_SQUEEZE_DND, it)
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(title = "Alerts & Ringtones") {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_sound_missed_call_notifications),
                            supporting = stringResource(R.string.settings_sound_missed_call_notifications_supporting),
                            leadingIcon = Icons.Outlined.PhoneMissed,
                            checked = missedCallNotifications,
                            onCheckedChange = {
                                missedCallNotifications = it
                                prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATIONS, it)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_sound_ringtone_settings),
                            supporting = stringResource(R.string.settings_sound_ringtone_settings_supporting),
                            leadingIcon = Icons.Outlined.MusicNote,
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                            }
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
    }
}
