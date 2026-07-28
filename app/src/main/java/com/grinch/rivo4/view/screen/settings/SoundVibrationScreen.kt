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

    var hapticListScroll by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)) }

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RivoExpressiveCard {
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
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
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
            }

            item {
                RivoExpressiveCard {
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
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
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
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
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

            item {
                RivoExpressiveCard {
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
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
