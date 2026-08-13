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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallRecorder
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.components.RivoSelectionDialog
import com.grinch.rivo4.view.theme.callColors
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
    initialConnectTime: Long = 0L,
    backgroundUri: String? = null
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

    var callDuration by remember(initialConnectTime) {
        mutableLongStateOf(
            if (initialConnectTime > 0) (System.currentTimeMillis() - initialConnectTime) / 1000 else 0L
        )
    }
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

    val recordingEnabled = remember(settingsState) {
        preferenceManager.getBoolean(PreferenceManager.KEY_CALL_RECORDING, false)
    }
    val isRecording by CallRecorder.isRecording.collectAsState()

    val statusText = when (callState) {
        Call.STATE_DISCONNECTED -> stringResource(R.string.call_status_ended)
        Call.STATE_HOLDING -> stringResource(R.string.call_status_on_hold)
        Call.STATE_ACTIVE -> formatDuration(callDuration)
        Call.STATE_DIALING -> stringResource(R.string.call_status_calling)
        Call.STATE_RINGING -> stringResource(R.string.call_status_incoming)
        Call.STATE_DISCONNECTING -> ""
        Call.STATE_CONNECTING -> stringResource(R.string.call_status_connecting)
        else -> ""
    }

    val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
    val audioIcon = when (audioRoute) {
        CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
        CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
        CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
        else -> Icons.Default.Phone
    }
    val audioLabel = audioRouteLabel(audioRoute, audioState)
    val hasBluetooth = ((audioState?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH) != 0

    val otherCallCard: @Composable () -> Unit = {
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
                            Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.action_end), tint = MaterialTheme.callColors.decline)
                        }
                    }
                }
            }
        }
    }

    val heroSection: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showKeypad) {
                AnimatedVisibility(
                    visible = showCallScreenAvatar,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (callState == Call.STATE_RINGING || callState == Call.STATE_DIALING) {
                            PulsingAvatar(photoUri, isLandscape)
                        } else {
                            HeroAvatar(photoUri, isLandscape)
                        }
                        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 20.dp))
                    }
                }
            }

            Text(
                text = contactName,
                style = if (isLandscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (phoneNumber.isNotEmpty() && phoneNumber != contactName) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = when (callState) {
                    Call.STATE_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    Call.STATE_HOLDING -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (callState) {
                        Call.STATE_ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                        Call.STATE_HOLDING -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            if (isRecording) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.callColors.decline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.call_recording_in_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.callColors.decline
                    )
                }
            }

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

    val keypadSection: @Composable (Boolean) -> Unit = { compact ->
        InCallKeypad(
            typedDigits = typedDigits,
            compact = compact,
            onDigitDown = { digit ->
                typedDigits += digit
                try { call.playDtmfTone(digit) } catch (e: Exception) {}
            },
            onDigitUp = { try { call.stopDtmfTone() } catch (e: Exception) {} },
            onBackspace = { typedDigits = typedDigits.dropLast(1) }
        )
    }

    val activeControls: @Composable (Boolean) -> Unit = { compact ->
        ActiveCallControls(
            callState = callState,
            isMuted = isMuted,
            audioState = audioState,
            showKeypad = showKeypad,
            recordingEnabled = recordingEnabled,
            isRecording = isRecording,
            compact = compact,
            onToggleMute = { CallService.mute(!isMuted) },
            onToggleKeypad = { showKeypad = !showKeypad },
            onAudioClick = {
                if (hasBluetooth) showAudioPicker = true else CallService.cycleAudioRoute()
            },
            onAddCall = {
                if (callState != Call.STATE_HOLDING) {
                    try { call.hold() } catch (e: Exception) {}
                }
                try { context.startActivity(Intent(Intent.ACTION_DIAL)) } catch (e: Exception) {}
            },
            onToggleHold = {
                try {
                    if (callState == Call.STATE_HOLDING) call.unhold() else call.hold()
                } catch (e: Exception) {}
            },
            onMessage = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:$phoneNumber") }
                    )
                } catch (e: Exception) {}
            },
            onToggleRecording = {
                if (isRecording) {
                    CallRecorder.stop()
                } else {
                    CallRecorder.start(context, contactName.ifBlank { phoneNumber })
                }
            },
            onEndCall = { try { call.disconnect() } catch (e: Exception) {} }
        )
    }

    val incomingControls: @Composable (Boolean) -> Unit = { compact ->
        val useCustomUI = preferenceManager.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)
        val onDeclineCallAction = {
            try {
                if (call.state == Call.STATE_RINGING) {
                    call.reject(Call.REJECT_REASON_DECLINED)
                } else {
                    call.disconnect()
                }
            } catch (e: Exception) {
                try { call.disconnect() } catch (e: Exception) {}
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 24.dp)
        ) {
            if (otherCall != null) {
                CallWaitingButtons(
                    onAnswerHold = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        CallService.answerRingingCall(endActive = false)
                    },
                    onEndAndAnswer = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        CallService.answerRingingCall(endActive = true)
                    },
                    onDecline = {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        onDeclineCallAction()
                    }
                )
            } else {
                if (useCustomUI != 2 && useCustomUI != 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallActionButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            isActive = false,
                            label = stringResource(R.string.action_message),
                            compact = compact
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDeclineCallAction()
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
                        onDecline = onDeclineCallAction
                    )
                    2 -> IPhoneSwipeToAnswer(
                        onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                        onDecline = onDeclineCallAction,
                        onMessage = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDeclineCallAction()
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phoneNumber")
                            }
                            context.startActivity(intent)
                        }
                    )
                    3 -> VerticalSwipeToAnswer(
                        onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                        onDecline = onDeclineCallAction
                    )
                    else -> HorizontalSwipeToAnswer(
                        onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                        onDecline = onDeclineCallAction
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ExpressiveBackground(photoUri, backgroundUri)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    otherCallCard()
                    heroSection()
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        showKeypad -> keypadSection(true)
                        callState == Call.STATE_RINGING -> incomingControls(true)
                        else -> activeControls(true)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                otherCallCard()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    heroSection()
                }

                if (showKeypad) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                        keypadSection(false)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (callState == Call.STATE_RINGING) {
                        incomingControls(false)
                    } else {
                        activeControls(false)
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingAvatar(photoUri: String?, isLandscape: Boolean = false) {
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

    val avatarSize = heroAvatarSize(isLandscape)

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(avatarSize * 0.9f)
                .scale(scale)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(avatarSize * 1.1f)
                .scale(scale * 1.2f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f), CircleShape)
        )
        HeroAvatar(photoUri, isLandscape)
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
private fun heroAvatarSize(isLandscape: Boolean): Dp {
    val configuration = LocalConfiguration.current
    return if (isLandscape) 120.dp
           else (configuration.screenHeightDp.dp * 0.22f).coerceAtMost(200.dp)
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

    val size = heroAvatarSize(isLandscape)
    val iconSize = size * 0.6f

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
    val maxHandleWidth = 110.dp
    val handleHeight = 72.dp // Increased from 64.dp
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val trackWidth = with(density) { trackWidthPx.toDp() }
    val handleWidth = if (trackWidthPx > 0f) (trackWidth * 0.32f).coerceAtMost(maxHandleWidth) else maxHandleWidth
    val handleWidthPx = with(density) { handleWidth.toPx() }

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

    val answerGreen = MaterialTheme.callColors.answer
    val declineRed = MaterialTheme.callColors.decline
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
        targetValue = if (dragNormal.value > 0.1f) MaterialTheme.callColors.onAnswer
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
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.action_decline),
                modifier = Modifier
                    .weight(1f)
                    .alpha((1f - (dragProgress.value * -2f).coerceIn(0f, 1f)) * hintAlpha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = declineRed.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(handleWidth))

            Text(
                stringResource(R.string.action_answer),
                modifier = Modifier
                    .weight(1f)
                    .alpha((1f - (dragProgress.value * 2f).coerceIn(0f, 1f)) * hintAlpha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = answerGreen.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        onAnswer()
                                    }
                                    offsetX.value < -triggerThreshold -> {
                                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
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
    val isDark = isSystemInDarkTheme()

    val trackHeight = if (isLandscape) 180.dp else 240.dp
    val trackWidth = if (isLandscape) 72.dp else 84.dp
    val handleSize = if (isLandscape) 64.dp else 72.dp
    val maxDrag = with(density) { (trackHeight / 2f - handleSize / 2f - 8.dp).toPx() }
    val triggerThreshold = maxDrag * 0.75f

    val dragProgress = remember { derivedStateOf { if (maxDrag > 0f) offsetY.value / maxDrag else 0f } }
    val dragNormal = remember { derivedStateOf { abs(dragProgress.value) } }

    val infiniteTransition = rememberInfiniteTransition(label = "vertPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "vertPulseScale"
    )

    val answerGreen = MaterialTheme.callColors.answer
    val declineRed = MaterialTheme.callColors.decline

    val handleBgColor by animateColorAsState(
        targetValue = when {
            dragProgress.value < -0.15f -> answerGreen
            dragProgress.value > 0.15f -> declineRed
            else -> if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        },
        label = "vertHandleBg"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            dragProgress.value < -0.15f -> MaterialTheme.callColors.onAnswer
            dragProgress.value > 0.15f -> MaterialTheme.callColors.onDecline
            else -> if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "vertIconTint"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight + 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vertical Capsule Track
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(42.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(42.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Top Answer Hint Target
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .alpha((0.4f + (dragProgress.value * -1.5f)).coerceIn(0.15f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = answerGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    stringResource(R.string.action_answer),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = answerGreen
                )
            }

            // Bottom Decline Hint Target
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .alpha((0.4f + (dragProgress.value * 1.5f)).coerceIn(0.15f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.action_decline),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = declineRed
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = declineRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Draggable Action Handle
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .graphicsLayer {
                        val idleFactor = (1f - dragNormal.value * 4f).coerceIn(0f, 1f)
                        scaleX = 1f + (pulseScale - 1f) * idleFactor
                        scaleY = 1f + (pulseScale - 1f) * idleFactor
                    }
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(handleBgColor)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetY.value < -triggerThreshold -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onAnswer()
                                        }
                                        offsetY.value > triggerThreshold -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                            onDecline()
                                        }
                                        else -> offsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (offsetY.value + dragAmount).coerceIn(-maxDrag * 1.1f, maxDrag * 1.1f)
                                    offsetY.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val icon = if (dragProgress.value > 0.2f) Icons.Default.CallEnd else Icons.Default.Call

                Crossfade(targetState = icon, animationSpec = tween(150), label = "vertIconCrossfade") { targetIcon ->
                    Icon(
                        targetIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                rotationZ = dragProgress.value * -90f
                            }
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
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
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
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun CallWaitingButtons(
    onAnswerHold: () -> Unit,
    onEndAndAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAnswerHold,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.callColors.answer,
                contentColor = MaterialTheme.callColors.onAnswer
            )
        ) {
            Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.call_waiting_answer_hold), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onEndAndAnswer,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.SwapCalls, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.call_waiting_end_and_answer), fontWeight = FontWeight.Bold)
        }

        TextButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = null, tint = MaterialTheme.callColors.decline, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_decline), color = MaterialTheme.callColors.decline, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncomingCallButtons(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val declineColor = MaterialTheme.callColors.decline
    val answerColor = MaterialTheme.callColors.answer
    val onDeclineColor = MaterialTheme.callColors.onDecline
    val onAnswerColor = MaterialTheme.callColors.onAnswer

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
                    contentColor = onDeclineColor
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
                        contentColor = onAnswerColor
                    ),
                    modifier = Modifier.size(72.dp).scale(scale)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = stringResource(R.string.action_answer),
                        modifier = Modifier.size(32.dp)
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
