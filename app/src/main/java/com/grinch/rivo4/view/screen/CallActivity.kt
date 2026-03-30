package com.grinch.rivo4.view.screen

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import kotlin.math.roundToInt

class CallActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        enableEdgeToEdge()

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()
                val contacts by contactsViewModel.allContacts.collectAsState()

                val call = session?.call

                LaunchedEffect(session) {
                    if (session == null) {
                        delay(500)
                        finish()
                    }
                }

                if (call != null) {
                    val details = call.details
                    val number = details?.handle?.schemeSpecificPart ?: "Unknown"
                    val contact = remember(number, contacts) {
                        contacts.find { c -> c.phoneNumbers.any { it.replace(" ", "").contains(number.replace(" ", "")) } }
                    }
                    val contactName = contact?.name ?: number
                    val photoUri = contact?.photoUri

                    ModernCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_DISCONNECTED,
                        contactName = contactName,
                        phoneNumber = number,
                        photoUri = photoUri,
                        audioState = audioState
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
    }
}

@Composable
fun ModernCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    phoneNumber: String,
    photoUri: String?,
    audioState: CallAudioState?
) {
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    val isHolding = callState == Call.STATE_HOLDING

    var callDuration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val startTime = System.currentTimeMillis()
            while (true) {
                callDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    val durationText = remember(callDuration) {
        val minutes = callDuration / 60
        val seconds = callDuration % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val isDarkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dynamic Ambient Glow
        val ambientColor by animateColorAsState(
            targetValue = when (callState) {
                Call.STATE_RINGING -> MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.15f else 0.08f)
                Call.STATE_ACTIVE -> Color(0xFF146C2E).copy(alpha = if (isDarkTheme) 0.1f else 0.05f)
                Call.STATE_HOLDING -> MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDarkTheme) 0.15f else 0.08f)
                else -> Color.Transparent
            },
            animationSpec = tween(1000),
            label = "AmbientColor"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ambientColor, Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Contact Info Section
            Text(
                text = contactName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (callState) {
                Call.STATE_RINGING -> "Incoming call • $phoneNumber"
                Call.STATE_DIALING -> "Calling..."
                Call.STATE_ACTIVE -> durationText
                Call.STATE_HOLDING -> "On hold"
                else -> "Connecting..."
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (callState == Call.STATE_ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Middle Content: Large Avatar
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                RivoAvatar(
                    name = contactName,
                    photoUri = photoUri,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Card-based Action UI at the Bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = callState == Call.STATE_RINGING,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    SwipeToAnswerSlider(
                        onAnswer = { call.answer(VideoProfile.STATE_AUDIO_ONLY) },
                        onDecline = { call.disconnect() }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = callState != Call.STATE_RINGING,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    // Samsung-inspired MD3 Expressive Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(48.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            // High-Expressive Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ExpressiveActionButton(
                                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    label = "Mute",
                                    isActive = isMuted,
                                    onClick = { CallService.mute(!isMuted) }
                                )
                                ExpressiveActionButton(
                                    icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeUp,
                                    label = "Speaker",
                                    isActive = isSpeakerOn,
                                    onClick = { CallService.setSpeaker(!isSpeakerOn) }
                                )
                                ExpressiveActionButton(
                                    icon = if (isHolding) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    label = if (isHolding) "Resume" else "Hold",
                                    isActive = isHolding,
                                    onClick = { if (isHolding) call.unhold() else call.hold() }
                                )
                            }

                            // Large Expressive End Call Button
                            Surface(
                                onClick = { call.disconnect() },
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CallEnd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            "End call",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToAnswerSlider(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val maxDrag = with(LocalDensity.current) { 100.dp.toPx() }
    val triggerThreshold = maxDrag * 0.7f

    Box(
        modifier = Modifier
            .width(88.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    when {
                        offsetY.value < -20f -> Color(0xFF146C2E)
                        offsetY.value > 20f -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetY.value < -triggerThreshold) {
                                    onAnswer()
                                } else if (offsetY.value > triggerThreshold) {
                                    onDecline()
                                } else {
                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetY.snapTo((offsetY.value + dragAmount).coerceIn(-maxDrag, maxDrag))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                null,
                tint = if (offsetY.value == 0f) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ExpressiveActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // MD3 Expressive: Shape morphing and scaling
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 44.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "CornerRadius"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ButtonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp)
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(cornerRadius),
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
