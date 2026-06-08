package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    phoneNumber: String,
    photoUri: String?,
    audioState: CallAudioState?
) {
    val view = LocalView.current
    val context = LocalContext.current
    val preferenceManager = koinInject<PreferenceManager>()
    val contactsRepo = koinInject<IContactsRepository>()
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    
    val allCalls by CallService.allCalls.collectAsState()
    val otherCall = remember(allCalls, call) {
        allCalls.find { it != call && it.state != Call.STATE_DISCONNECTED }
    }

    val simLabel = remember(call.details.accountHandle) {
        val handle = call.details.accountHandle
        if (handle != null) {
            val account = try {
                telecomManager.getPhoneAccount(handle)
            } catch (e: Exception) {
                null
            }

            val label = account?.label?.toString()
            if (!label.isNullOrEmpty()) {
                label
            } else {
                "SIM ${handle.id}"
            }
        } else {
            null
        }
    }
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER

    var callDuration by remember { mutableLongStateOf(0L) }
    var showKeypad by remember { mutableStateOf(false) }
    var typedDigits by remember { mutableStateOf("") }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val startTime = System.currentTimeMillis() - (callDuration * 1000)
            while (true) {
                callDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    BackHandler(showKeypad) {
        showKeypad = false
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ExpressiveBackground(photoUri)
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Other Call Card
            AnimatedVisibility(
                visible = otherCall != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                otherCall?.let { oc ->
                    var ocName by remember(oc) { mutableStateOf(oc.details.handle?.schemeSpecificPart ?: "Unknown") }
                    LaunchedEffect(oc) {
                        val number = oc.details.handle?.schemeSpecificPart ?: ""
                        if (number.isNotEmpty()) {
                            val contact = try { contactsRepo.getContactByNumber(number) } catch (e: Exception) { null }
                            if (contact != null) ocName = contact.name
                        }
                    }
                    
                    Surface(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            // Swap calls reliably
                            try {
                                CallService.setPreferredCall(oc)
                                if (call.state != Call.STATE_HOLDING) {
                                    call.hold()
                                }
                                oc.unhold()
                            } catch (e: Exception) {
                                // Fallback: just try to unhold the other one
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
                                        text = "On Hold",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { oc.disconnect() }) {
                                Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // --- HERO SECTION ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.weight(if (showKeypad) 0.7f else 1f)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(1000)) + expandVertically(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        val statusText = when (callState) {
                            Call.STATE_DISCONNECTED -> "Call Ended"
                            Call.STATE_HOLDING -> "On Hold"
                            Call.STATE_ACTIVE -> formatDuration(callDuration)
                            Call.STATE_DIALING -> "Calling..."
                            Call.STATE_RINGING -> "Incoming call"
                            else -> "Connecting..."
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
                    Spacer(modifier = Modifier.height(48.dp))

                    if (callState == Call.STATE_RINGING) {
                        PulsingAvatar(photoUri)
                    } else {
                        HeroAvatar(photoUri)
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Row 1 (3 centered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallActionButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            isActive = isMuted,
                            label = "Mute"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            CallService.mute(!isMuted)
                        }

                        CallActionButton(
                            icon = Icons.Default.Dialpad,
                            isActive = showKeypad,
                            label = "Keypad"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showKeypad = !showKeypad
                        }

                        CallActionButton(
                            icon = Icons.Default.Message,
                            isActive = false,
                            label = "Message"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phoneNumber")
                            }
                            context.startActivity(intent)
                        }
                    }

                    // Row 2 (3 centered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallActionButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            isActive = isSpeakerOn,
                            label = "Speaker"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            CallService.setSpeaker(!isSpeakerOn)
                        }

                        CallActionButton(
                            icon = Icons.Default.Add,
                            isActive = false,
                            label = "Add call"
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
                            label = if (callState == Call.STATE_HOLDING) "Resume" else "Hold"
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
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                val useCustomUI = preferenceManager.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallActionButton(
                            icon = Icons.Default.Message,
                            isActive = false,
                            label = "Message"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            try { call.disconnect() } catch (e: Exception) {}
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phoneNumber")
                            }
                            context.startActivity(intent)
                        }
                    }

                    if (useCustomUI == 1) {
                        IncomingCallButtons(
                            onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                            onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                        )
                    } else {
                        HorizontalSwipeToAnswer(
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
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_DTMF, 80) }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = typedDigits,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .animateContentSize()
        )

        val keys = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf('*', '0', '#')
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            key = key,
                            onClick = {
                                if (prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, true)) {
                                    val toneType = when (key) {
                                        '1' -> ToneGenerator.TONE_DTMF_1
                                        '2' -> ToneGenerator.TONE_DTMF_2
                                        '3' -> ToneGenerator.TONE_DTMF_3
                                        '4' -> ToneGenerator.TONE_DTMF_4
                                        '5' -> ToneGenerator.TONE_DTMF_5
                                        '6' -> ToneGenerator.TONE_DTMF_6
                                        '7' -> ToneGenerator.TONE_DTMF_7
                                        '8' -> ToneGenerator.TONE_DTMF_8
                                        '9' -> ToneGenerator.TONE_DTMF_9
                                        '0' -> ToneGenerator.TONE_DTMF_0
                                        '*' -> ToneGenerator.TONE_DTMF_S
                                        '#' -> ToneGenerator.TONE_DTMF_P
                                        else -> -1
                                    }
                                    if (toneType != -1) {
                                        toneGenerator.startTone(toneType, 120)
                                    }
                                }
                                call.playDtmfTone(key)
                                call.stopDtmfTone()
                                onDigitClick(key)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: Char, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 32.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonShape"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExpressiveBackground(photoUri: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val driftX by infiniteTransition.animateFloat(
        initialValue = -30f, targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "x"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = driftX
                        translationY = driftY
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .blur(80.dp)
                    .alpha(0.35f),
                contentScale = ContentScale.Crop
            )
        } else {
            val color1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            val color2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            val color3 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(color1, color2, color3)))
                    .blur(40.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
        )
    }
}

@Composable
fun PulsingAvatar(photoUri: String?) {
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), avatarShape)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale * 1.1f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f), avatarShape)
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
                    animation = tween(durationMillis = 5000 + index * 500, easing = LinearEasing),
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
fun HeroAvatar(photoUri: String?) {
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

    Box(
        modifier = Modifier
            .size(200.dp)
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
                modifier = Modifier.size(120.dp),
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
            "Decline",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
                .alpha((1f - (dragProgress.value * -2f).coerceIn(0f, 1f)) * hintAlpha),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = declineRed.copy(alpha = 0.8f)
        )

        Text(
            "Answer",
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
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
        }
    }
}

@Composable
fun VerticalSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val trackHeight = 320.dp
    val trackWidth = 80.dp
    val handleSize = 64.dp
    val handleSizePx = with(density) { handleSize.toPx() }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    val maxDrag by remember(trackHeightPx, handleSizePx) {
        derivedStateOf {
            if (trackHeightPx > 0f) (trackHeightPx / 2f) - (handleSizePx / 2f) - with(density) { 16.dp.toPx() }
            else 0f
        }
    }
    val triggerThreshold = maxDrag * 0.8f

    val dragProgress = remember { derivedStateOf { if (maxDrag > 0f) offsetY.value / maxDrag else 0f } }
    val dragNormal = remember { derivedStateOf { abs(dragProgress.value) } }

    val cream = if (isDark) Color(0xFF322F33) else Color(0xFFF7F2FA)
    val answerGreen = Color(0xFF4CAF50)
    val declineRed = Color(0xFFF44336)

    val handleBgColor by remember { derivedStateOf {
        val t = dragNormal.value
        when {
            offsetY.value < 0f -> lerp(cream, answerGreen, t)
            offsetY.value > 0f -> lerp(cream, declineRed, t)
            else -> cream
        }
    } }

    val iconTint by remember { derivedStateOf {
        lerp(if (isDark) Color.White else answerGreen, Color.White, dragNormal.value.coerceIn(0f, 1f))
    } }

    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(trackHeight)
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .clip(RoundedCornerShape(40.dp))
            .background(if (isDark) Color(0xFF211F24) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = answerGreen.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
            Icon(
                Icons.Default.CallEnd,
                contentDescription = null,
                tint = declineRed.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
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
                                    else -> offsetY.animateTo(0f, spring(dampingRatio = 0.8f))
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
            val icon = if (offsetY.value > 10f) Icons.Default.CallEnd else Icons.Default.Call
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
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
                    contentDescription = "Decline",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Decline",
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
                        contentDescription = "Answer",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Answer",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

