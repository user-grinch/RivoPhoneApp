package com.grinch.rivo4.view.screen.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.getSystemVoicemailNumber
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun VoicemailScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    
    var voicemailNumber by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_VOICEMAIL_NUMBER, "") ?: "") }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOICEMAIL_VIBRATION, true)) }
    var ringtoneUri by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_VOICEMAIL_RINGTONE, null)) }

    LaunchedEffect(Unit) {
        if (voicemailNumber.isEmpty()) {
            val detected = getSystemVoicemailNumber(context)
            if (!detected.isNullOrEmpty()) {
                voicemailNumber = detected
                prefs.setString(PreferenceManager.KEY_VOICEMAIL_NUMBER, detected)
            }
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            ringtoneUri = uri?.toString()
            prefs.setString(PreferenceManager.KEY_VOICEMAIL_RINGTONE, ringtoneUri)
        }
    }

    val currentRingtoneName = remember(ringtoneUri) {
        if (ringtoneUri == null) "Default"
        else {
            try {
                RingtoneManager.getRingtone(context, Uri.parse(ringtoneUri))?.getTitle(context) ?: "Custom"
            } catch (e: Exception) { "Custom" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voicemail", fontWeight = FontWeight.Bold) },
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
                RivoExpressiveCard(
                    title = "Carrier Settings",
                    icon = Icons.Outlined.Settings
                ) {
                    OutlinedTextField(
                        value = voicemailNumber,
                        onValueChange = { 
                            voicemailNumber = it
                            prefs.setString(PreferenceManager.KEY_VOICEMAIL_NUMBER, it)
                        },
                        label = { Text("Voicemail Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        trailingIcon = {
                            IconButton(onClick = {
                                val detected = getSystemVoicemailNumber(context)
                                if (!detected.isNullOrEmpty()) {
                                    voicemailNumber = detected
                                    prefs.setString(PreferenceManager.KEY_VOICEMAIL_NUMBER, detected)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.SimCard,
                                    contentDescription = "Auto-detect from SIM"
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your carrier's voicemail access number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    RivoListItem(
                        headline = "Call Voicemail",
                        supporting = "Dial your voicemail service directly",
                        leadingIcon = Icons.Outlined.Voicemail,
                        onClick = {
                            val num = voicemailNumber.ifEmpty { "voicemail:" }
                            makeCall(context, num)
                        }
                    )
                }
            }

            item {
                RivoExpressiveCard(
                    title = "Notifications",
                    icon = Icons.Outlined.Notifications
                ) {
                    RivoListItem(
                        headline = "Voicemail Ringtone",
                        supporting = currentRingtoneName,
                        leadingIcon = Icons.Outlined.Notifications,
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Voicemail Notification")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri?.let { Uri.parse(it) })
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                    
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    RivoSwitchListItem(
                        headline = "Vibration",
                        supporting = "Vibrate for new voicemail notifications",
                        leadingIcon = Icons.Outlined.Vibration,
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            prefs.setBoolean(PreferenceManager.KEY_VOICEMAIL_VIBRATION, it)
                        }
                    )
                }
            }

            item {
                RivoExpressiveCard(
                    title = "Visual Voicemail",
                    icon = Icons.Outlined.Voicemail
                ) {
                    Text(
                        "Visual Voicemail is currently managed by your carrier. Ensure your carrier app is installed for the best experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
