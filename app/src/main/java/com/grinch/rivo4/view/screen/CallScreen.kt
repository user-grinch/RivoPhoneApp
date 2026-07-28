package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.media.AudioManager
import android.media.ToneGenerator
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.components.RivoSelectionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
private fun audioRouteLabel(audioRoute: Int, audioState: CallAudioState?): String {
    val bluetoothShortLabel = stringResource(R.string.audio_route_bluetooth_short)
    return when (audioRoute) {
        CallAudioState.ROUTE_SPEAKER -> stringResource(R.string.audio_route_speaker)
        CallAudioState.ROUTE_BLUETOOTH -> try { audioState?.activeBluetoothDevice?.name ?: bluetoothShortLabel } catch (e: Exception) { bluetoothShortLabel }
        CallAudioState.ROUTE_WIRED_HEADSET -> stringResource(R.string.audio_route_headset)
        else -> stringResource(R.string.audio_route_handset)
    }
}

@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    phoneNumber: String,
    photoUri: String?,
    audioState: CallAudioState?,
    initialConnectTime: Long = 0L
) {
    val view = LocalView.current
    val context = LocalContext.current
    val preferenceManager = koinInject<PreferenceManager>()
    val contactsRepo = koinInject<IContactsRepository>()
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    
    val allCalls by CallService.allCalls.collectAsState()
    val otherCall = remember(allCalls, call) {
        @Suppress("DEPRECATION")
        allCalls.find { it != call && it.state != Call.STATE_DISCONNECTED }
    }

    val accountHandle = call.details.accountHandle
    val simLabelFallback = accountHandle?.let { stringResource(R.string.call_screen_sim_label, it.id) }
    val simLabel = remember(accountHandle, simLabelFallback) {
        if (accountHandle != null) {
            val account = try {
                telecomManager.getPhoneAccount(accountHandle)
            } catch (e: Exception) {
                null
            }

            val label = account?.label?.toString()
            if (!label.isNullOrEmpty()) {
                label
            } else {
                simLabelFallback
            }
        } else {
            null
        }
    }
    val isMuted = audioState?.isMuted ?: false

    var callDuration by rememberSaveable { mutableLongStateOf(0L) }
    var showKeypad by remember { mutableStateOf(false) }
    var showAudioPicker by remember { mutableStateOf(false) }
    var typedDigits by remember { mutableStateOf("") }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val settingsState by preferenceManager.settingsChanged.collectAsState()
    val showCallScreenAvatar = remember(settingsState) {
        preferenceManager.getBoolean(PreferenceManager.KEY_SHOW_CALL_SCREEN_AVATAR, true)
    }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(callState, call.details.connectTimeMillis, initialConnectTime) {
        if (callState == Call.STATE_ACTIVE) {
            val connectTime = when {
                initialConnectTime > 0 -> initialConnectTime
                call.details.connectTimeMillis > 0 -> call.details.connectTimeMillis
                else -> System.currentTimeMillis()
            }
            while (true) {
                callDuration = (System.currentTimeMillis() - connectTime) / 1000
                delay(1.seconds)
            }
        }
    }

    BackHandler(showKeypad) {
        showKeypad = false
    }

    if (showAudioPicker) {
        val supported = audioState?.supportedRouteMask ?: 0
        val handsetLabel = stringResource(R.string.audio_route_handset)
        val speakerLabel = stringResource(R.string.audio_route_speaker)
        val headsetLabel = stringResource(R.string.audio_route_headset)
        val bluetoothLabel = stringResource(R.string.audio_route_bluetooth)
        val options = remember(supported, handsetLabel, speakerLabel, headsetLabel, bluetoothLabel) {
            mutableListOf<Pair<String, Int>>().apply {
                if ((supported and CallAudioState.ROUTE_EARPIECE) != 0) add(handsetLabel to CallAudioState.ROUTE_EARPIECE)
                if ((supported and CallAudioState.ROUTE_SPEAKER) != 0) add(speakerLabel to CallAudioState.ROUTE_SPEAKER)
                if ((supported and CallAudioState.ROUTE_WIRED_HEADSET) != 0) add(headsetLabel to CallAudioState.ROUTE_WIRED_HEADSET)
                if ((supported and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                    val deviceName = try {
                        audioState?.activeBluetoothDevice?.name
                    } catch (e: SecurityException) {
                        null
                    }
                    add((deviceName ?: bluetoothLabel) to CallAudioState.ROUTE_BLUETOOTH)
                }
            }
        }

        RivoSelectionDialog<Pair<String, Int>>(
            onDismissRequest = { showAudioPicker = false },
            title = stringResource(R.string.audio_output_title),
            items = options,
            itemLabel = { option -> option.first },
            onItemSelected = { option ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                CallService.setAudioRoute(option.second)
            },
            isSelected = { option -> option.second == audioState?.route },
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            itemIcon = { option ->
                when (option.second) {
                    CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                    CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
                    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
                    else -> Icons.Default.Phone
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ExpressiveBackground(photoUri)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = if (isLandscape) 8.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Other Call Card
            AnimatedVisibility(
                visible = otherCall != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                otherCall?.let { oc ->
                    val unknownLabel = stringResource(R.string.label_unknown)
                    var ocName by remember(oc, unknownLabel) { mutableStateOf(oc.details.handle?.schemeSpecificPart ?: unknownLabel) }
                    LaunchedEffect(oc) {
                        val number = oc.details.handle?.schemeSpecificPart ?: ""
                        if (number.isNotEmpty()) {
                            val contact = try { contactsRepo.getContactByNumber(number) } catch (_: Exception) { null }
                            if (contact != null) ocName = (contact as? com.grinch.rivo4.modal.data.Contact)?.name ?: number
                        }
                    }
                    
                    Surface(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            try {
                                CallService.setPreferredCall(oc)
                                if (call.state != Call.STATE_HOLDING) {
                                    call.hold()
                                }
                                oc.unhold()
                            } catch (e: Exception) {
                                try { oc.unhold() } catch (e2: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = ocName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.call_status_on_hold),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { oc.disconnect() }) {
                                Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.action_end), tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // --- HERO SECTION ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.weight(if (showKeypad) 0.7f else if (isLandscape && callState == Call.STATE_RINGING) 0.5f else 1f)
            ) {
                if (!isLandscape) Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(1000)) + expandVertically(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = contactName,
                            style = if (isLandscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        val statusText = when (callState) {
                            Call.STATE_DISCONNECTED -> stringResource(R.string.call_status_ended)
                            Call.STATE_HOLDING -> stringResource(R.string.call_status_on_hold)
                            Call.STATE_ACTIVE -> formatDuration(callDuration)
                            Call.STATE_DIALING -> stringResource(R.string.call_status_calling)
                            Call.STATE_RINGING -> stringResource(R.string.call_status_incoming)
                            Call.STATE_DISCONNECTING -> ""
                            Call.STATE_CONNECTING -> stringResource(R.string.call_status_connecting)
                            else -> stringResource(R.string.call_status_connecting)
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (callState == Call.STATE_HOLDING) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (simLabel != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = simLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (!showKeypad) {
                    AnimatedVisibility(
                        visible = showCallScreenAvatar && (!isLandscape || callState != Call.STATE_RINGING),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            if (!isLandscape) Spacer(modifier = Modifier.height(24.dp))
                            if (callState == Call.STATE_RINGING) {
                                PulsingAvatar(photoUri)
                            } else {
                                HeroAvatar(photoUri, isLandscape)
                            }
                        }
                    }
                }
            }

            if (showKeypad) {
                Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.TopStart) {
                    InCallKeypad(
                        call = call,
                        typedDigits = typedDigits,
                        onDigitClick = { digit -> typedDigits += digit }
                    )
                }
            }

            // --- UI CONTROLS ---
            if (callState != Call.STATE_RINGING) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(if (isLandscape) 1f else 1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 24.dp)
                ) {
                    if (isLandscape) {
                        // Compact row for landscape
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallActionButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                isActive = isMuted,
                                label = stringResource(R.string.action_mute)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                CallService.mute(!isMuted)
                            }

                            CallActionButton(
                                icon = Icons.Default.Dialpad,
                                isActive = showKeypad,
                                label = stringResource(R.string.action_keypad)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showKeypad = !showKeypad
                            }

                            val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
                            val audioIcon = when (audioRoute) {
                                CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                                CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
                                CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
                                else -> Icons.Default.Phone
                            }
                            val audioLabel = audioRouteLabel(audioRoute, audioState)
                            val hasBluetooth = (audioState?.supportedRouteMask ?: 0 and CallAudioState.ROUTE_BLUETOOTH) != 0

                            CallActionButton(
                                icon = audioIcon,
                                isActive = audioRoute == CallAudioState.ROUTE_SPEAKER || audioRoute == CallAudioState.ROUTE_BLUETOOTH,
                                label = audioLabel
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (hasBluetooth) showAudioPicker = true else CallService.cycleAudioRoute()
                            }

                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    try { call.disconnect() } catch (e: Exception) {}
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFF44336), CircleShape)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.action_end), tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallActionButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                isActive = isMuted,
                                label = stringResource(R.string.action_mute)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                CallService.mute(!isMuted)
                            }

                            CallActionButton(
                                icon = Icons.Default.Dialpad,
                                isActive = showKeypad,
                                label = stringResource(R.string.action_keypad)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showKeypad = !showKeypad
                            }

                            CallActionButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                isActive = false,
                                label = stringResource(R.string.action_message)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$phoneNumber")
                                }
                                context.startActivity(intent)
                            }
                        }

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
                            val audioIcon = when (audioRoute) {
                                CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                                CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
                                CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
                                else -> Icons.Default.Phone
                            }

                            val audioLabel = audioRouteLabel(audioRoute, audioState)

                            val hasBluetooth = (audioState?.supportedRouteMask ?: 0 and CallAudioState.ROUTE_BLUETOOTH) != 0

                            CallActionButton(
                                icon = audioIcon,
                                isActive = audioRoute == CallAudioState.ROUTE_SPEAKER || audioRoute == CallAudioState.ROUTE_BLUETOOTH,
                                label = audioLabel
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (hasBluetooth) showAudioPicker = true else CallService.cycleAudioRoute()
                            }

                            CallActionButton(
                                icon = Icons.Default.Add,
                                isActive = false,
                                label = stringResource(R.string.action_add_call)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (callState != Call.STATE_HOLDING) {
                                    try { call.hold() } catch (e: Exception) {}
                                }
                                val intent = Intent(Intent.ACTION_DIAL)
                                context.startActivity(intent)
                            }

                            CallActionButton(
                                icon = if (callState == Call.STATE_HOLDING) Icons.Default.PlayArrow else Icons.Default.Pause,
                                isActive = callState == Call.STATE_HOLDING,
                                label = if (callState == Call.STATE_HOLDING) stringResource(R.string.action_resume) else stringResource(R.string.action_hold)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (callState == Call.STATE_HOLDING) call.unhold() else call.hold()
                            }
                        }

                        // End Call Button
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                try { call.disconnect() } catch (e: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(64.dp)
                                .scale(if (isPressed) 0.96f else 1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336),
                                contentColor = Color.White
                            ),
                            interactionSource = interactionSource
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.action_end_call), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            } else {
                val useCustomUI = preferenceManager.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(if (isLandscape) 1f else 1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 24.dp)
                ) {
                    if (useCustomUI != 2 && useCustomUI != 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallActionButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                isActive = false,
                                label = stringResource(R.string.action_message)
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                try { call.disconnect() } catch (e: Exception) {}
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$phoneNumber")
                                }
                                context.startActivity(intent)
                            }
                        }
                    }

                    when (useCustomUI) {
                        1 -> IncomingCallButtons(
                            onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                            onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                        )
                        2 -> IPhoneSwipeToAnswer(
                            onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                            onDecline = { try { call.disconnect() } catch (e: Exception) {} },
                            onMessage = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                try { call.disconnect() } catch (e: Exception) {}
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$phoneNumber")
                                }
                                context.startActivity(intent)
                            }
                        )
                        3 -> VerticalSwipeToAnswer(
                            onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                            onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                        )
                        else -> HorizontalSwipeToAnswer(
                            onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                            onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InCallKeypad(
    call: Call,
    typedDigits: String,
    onDigitClick: (Char) -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = typedDigits,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val keys = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf('*', '0', '#')
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeypadButton(key) {
                        onDigitClick(key)
                        call.playDtmfTone(key)
                        call.stopDtmfTone()
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: Char, style: Int = 0, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.15f else 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpressiveBackground(photoUri: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )
        }
        
        val isDark = isSystemInDarkTheme()
        val gradient = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent, Color.Black.copy(alpha = 0.9f))
            } else {
                listOf(Color.White.copy(alpha = 0.6f), Color.Transparent, Color.White.copy(alpha = 0.8f))
            }
        )
        Box(modifier = Modifier.fillMaxSize().background(gradient))
        FloatingParticles()
    }
}

@Composable
fun PulsingAvatar(photoUri: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale * 1.2f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f), CircleShape)
        )
        HeroAvatar(photoUri)
    }
}

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    Box(modifier = Modifier.fillMaxSize()) {
        repeat(10) { index ->
            val startX = (index * 100f) % 1000f
            val startY = (index * 150f) % 1500f

            val animX by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 100f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 10000 + index * 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "x_$index"
            )

            val animY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -150f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 12000 + index * 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "y_$index"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_$index"
            )

            Box(
                modifier = Modifier
                    .offset(x = (startX + animX).dp, y = (startY + animY).dp)
                    .size((10 + index % 20).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    .blur(2.dp)
            )
        }
    }
}

@Composable
fun HeroAvatar(photoUri: String?, isLandscape: Boolean = false) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val avatarShape = remember(settingsState) {
        val shapeVal = prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0)
        when (shapeVal) {
            0 -> RoundedCornerShape(20.dp)
            1 -> CircleShape
            2 -> RoundedCornerShape(0.dp)
            else -> CircleShape
        }
    }

    val size = if (isLandscape) 120.dp else 200.dp
    val iconSize = if (isLandscape) 72.dp else 120.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(avatarShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(avatarShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    isActive: Boolean,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isActive) 1.1f else 1f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp).alpha(if (enabled) 1f else 0.5f)
    ) {
        val containerColor by animateColorAsState(
            if (isActive) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            label = "color"
        )
        val contentColor = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                          else MaterialTheme.colorScheme.onSurfaceVariant

        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .background(containerColor, CircleShape)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@Composable
fun HorizontalSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val trackHeight = 96.dp // Increased from 88.dp
    val handleWidth = 110.dp
    val handleHeight = 72.dp // Increased from 64.dp
    val handleWidthPx = with(density) { handleWidth.toPx() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    
    val maxDrag by remember(trackWidthPx, handleWidthPx) {
        derivedStateOf {
            if (trackWidthPx > 0f) (trackWidthPx / 2f) - (handleWidthPx / 2f) - with(density) { 12.dp.toPx() }
            else 0f
        }
    }
    val triggerThreshold = maxDrag * 0.85f

    val dragProgress = remember { derivedStateOf { if (maxDrag > 0f) offsetX.value / maxDrag else 0f } }
    val dragNormal = remember { derivedStateOf { abs(dragProgress.value) } }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val handlePulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handlePulse"
    )

    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    val answerGreen = Color(0xFF4CAF50)
    val declineRed = Color(0xFFF44336)
    val idleColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                   else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    val handleBgColor by animateColorAsState(
        targetValue = when {
            dragProgress.value > 0.1f -> answerGreen
            dragProgress.value < -0.1f -> declineRed
            else -> if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        },
        label = "handleColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (dragNormal.value > 0.1f) Color.White 
                     else if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "iconTint"
    )
    
    val iconRotation by remember { derivedStateOf {
        dragProgress.value * 135f
    } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .padding(horizontal = 16.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), CircleShape)
    ) {
        Text(
            stringResource(R.string.action_decline),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
                .alpha((1f - (dragProgress.value * -2f).coerceIn(0f, 1f)) * hintAlpha),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = declineRed.copy(alpha = 0.8f)
        )

        Text(
            stringResource(R.string.action_answer),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
                .alpha((1f - (dragProgress.value * 2f).coerceIn(0f, 1f)) * hintAlpha),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = answerGreen.copy(alpha = 0.8f)
        )

        // drag handle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    val idleFactor = (1f - dragNormal.value * 5f).coerceIn(0f, 1f)
                    scaleX = 1f + (handlePulseScale - 1f) * idleFactor
                    scaleY = 1f + (handlePulseScale - 1f) * idleFactor
                }
                .width(handleWidth)
                .height(handleHeight)
                .clip(CircleShape)
                .background(handleBgColor)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    offsetX.value > triggerThreshold -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                        onAnswer()
                                    }
                                    offsetX.value < -triggerThreshold -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                        onDecline()
                                    }
                                    else -> offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch { 
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-maxDrag * 1.1f, maxDrag * 1.1f)
                                offsetX.snapTo(newOffset) 
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val icon = if (dragProgress.value < -0.2f) Icons.Default.CallEnd else Icons.Default.Call
            
            Crossfade(targetState = icon, animationSpec = tween(150), label = "icon") { targetIcon ->
                Icon(
                    targetIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { 
                            rotationZ = iconRotation 
                            if (targetIcon == Icons.Default.Call) scaleY = -1f
                        }
                )
            }
        }
    }
}

@Composable
fun VerticalSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val view = LocalView.current

    val handleSize = if (isLandscape) 72.dp else 80.dp
    val maxDrag = with(density) { (if (isLandscape) 80.dp else 100.dp).toPx() }
    val triggerThreshold = maxDrag * 0.7f

    val dragProgress = remember { derivedStateOf { offsetY.value / maxDrag } }
    
    // Pulse animation for the button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Arrow bounce animation
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowBounce"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 240.dp else 360.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelOffset = if (isLandscape) 80.dp else 110.dp

        // --- UP SECTION (Answer) ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -labelOffset)
                .graphicsLayer { 
                    alpha = (0.4f + (dragProgress.value * -1.8f)).coerceIn(0f, 1f)
                    translationY = -arrowOffset
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp))
            Text(
                stringResource(R.string.swipe_up_to_answer),
                style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }

        // --- DOWN SECTION (Reject) ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = labelOffset)
                .graphicsLayer { 
                    alpha = (0.4f + (dragProgress.value * 1.8f)).coerceIn(0f, 1f)
                    translationY = arrowOffset
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.swipe_down_to_reject),
                style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp))
        }

        // --- CENTER BUTTON ---
        Box(contentAlignment = Alignment.Center) {
            // Pulsing rings
            if (abs(offsetY.value) < 5f) {
                Box(
                    modifier = Modifier
                        .size(handleSize)
                        .scale(pulseScale)
                        .background(Color.White.copy(alpha = pulseAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(handleSize)
                        .scale(pulseScale * 1.4f)
                        .border(1.dp, Color.White.copy(alpha = pulseAlpha * 0.4f), CircleShape)
                )
            }

            val handleBgColor by animateColorAsState(
                targetValue = when {
                    offsetY.value < -15f -> Color(0xFF4CAF50)
                    offsetY.value > 15f -> Color(0xFFF44336)
                    else -> Color.White
                },
                label = "bgColor"
            )
            
            val iconTint by animateColorAsState(
                targetValue = if (abs(offsetY.value) > 15f) Color.White else Color(0xFF4CAF50),
                label = "iconTint"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .size(handleSize)
                    .shadow(if (abs(offsetY.value) > 5f) 12.dp else 4.dp, CircleShape)
                    .background(handleBgColor, CircleShape)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetY.value < -triggerThreshold -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                            onAnswer()
                                        }
                                        offsetY.value > triggerThreshold -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                            onDecline()
                                        }
                                        else -> offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (offsetY.value + dragAmount).coerceIn(-maxDrag, maxDrag)
                                    offsetY.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val icon = if (offsetY.value > 5f) Icons.Default.CallEnd else Icons.Default.Call
                
                Crossfade(targetState = icon, label = "icon") { targetIcon ->
                    Icon(
                        targetIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp).graphicsLayer(scaleY = if (targetIcon == Icons.Default.Call) -1f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun IPhoneSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit, onMessage: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    
    val trackWidth = 320.dp
    val trackHeight = 84.dp
    val handleSize = 68.dp
    val handlePadding = 8.dp
    
    val trackWidthPx = with(density) { trackWidth.toPx() }
    val handleSizePx = with(density) { handleSize.toPx() }
    val handlePaddingPx = with(density) { handlePadding.toPx() }
    
    val maxDrag = trackWidthPx - handleSizePx - (handlePaddingPx * 2)

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 48.dp),
        modifier = Modifier.padding(bottom = if (isLandscape) 12.dp else 60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 0.6f else 0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(if (isLandscape) 48.dp else 60.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.15f else 0.1f), 
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = stringResource(R.string.action_decline),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    stringResource(R.string.action_decline),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .size(if (isLandscape) 48.dp else 60.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.15f else 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.action_message),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    stringResource(R.string.action_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(if (isLandscape) 280.dp else trackWidth)
                .height(if (isLandscape) 72.dp else trackHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.5f))
                .border(
                    1.dp, 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), 
                    CircleShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            val baseTextColor = MaterialTheme.colorScheme.onSurface
            val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            
            val brush = Brush.linearGradient(
                colors = listOf(shimmerColor, baseTextColor, shimmerColor),
                start = Offset(trackWidthPx * shimmerOffset - 150f, 0f),
                end = Offset(trackWidthPx * shimmerOffset + 150f, 0f)
            )

            Text(
                text = stringResource(R.string.slide_to_answer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = handleSize + 16.dp)
                    .graphicsLayer { alpha = (1f - (offsetX.value / maxDrag)).coerceIn(0f, 1f) },
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = brush,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Start
            )

            Box(
                modifier = Modifier
                    .padding(start = handlePadding)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (offsetX.value > maxDrag * 0.85f) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                        onAnswer()
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, maxDrag))
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp).graphicsLayer(scaleY = -1f)
                )
            }
        }
    }
}

@Composable
fun IncomingCallButtons(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val declineColor = Color(0xFFD14249)
    val answerColor = Color(0xFF4CAF50)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp).padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = onDecline,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = declineColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = stringResource(R.string.action_decline),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.action_decline),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale * 1.2f)
                        .background(answerColor.copy(alpha = 0.2f), CircleShape)
                )

                FilledIconButton(
                    onClick = onAnswer,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = answerColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(72.dp).scale(scale)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = stringResource(R.string.action_answer),
                        modifier = Modifier.size(32.dp).graphicsLayer(scaleY = -1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.action_answer),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
