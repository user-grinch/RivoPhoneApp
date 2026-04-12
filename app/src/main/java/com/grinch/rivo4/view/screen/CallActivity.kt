package com.grinch.rivo4.view.screen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable 
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.math.roundToInt
import com.grinch.rivo4.controller.ContactsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CallActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModel()
    private val contactsRepo: IContactsRepository by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setupProximitySensor()
        enableEdgeToEdge()

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()

                val call = session?.call
                val callState = session?.state

                LaunchedEffect(callState) {
                    when (callState) {
                        Call.STATE_ACTIVE, Call.STATE_DIALING -> acquireProximityLock()
                        else -> releaseProximityLock()
                    }

                    if (session == null || callState == Call.STATE_DISCONNECTED) {
                        delay(800)
                        finish()
                    }
                }

                if (call != null && session != null) {
                    val details = call.details
                    val number = details?.handle?.schemeSpecificPart ?: ""
                    
                    var contactName by remember { mutableStateOf(number.ifEmpty { "Unknown" }) }
                    var photoUri by remember { mutableStateOf<String?>(null) }
                    var isUnknown by remember { mutableStateOf(true) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            val contact = try {
                                contactsRepo.getContactByNumber(number)
                            } catch (e: Exception) { null }
                            
                            if (contact != null) {
                                contactName = contact.name
                                photoUri = contact.photoUri
                                isUnknown = false
                            }
                        }
                    }

                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
                        phoneNumber = number,
                        isUnknown = isUnknown,
                        photoUri = photoUri,
                        audioState = audioState
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }

    private fun setupProximitySensor() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "RivoPhoneApp::ProximityWakeLock"
            )
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

    override fun onDestroy() {
        super.onDestroy()
        releaseProximityLock()
    }

    private fun acquireProximityLock() {
        proximityWakeLock?.let { if (!it.isHeld) it.acquire() }
    }

    private fun releaseProximityLock() {
        proximityWakeLock?.let { if (it.isHeld) it.release() }
    }
}

@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    phoneNumber: String,
    isUnknown: Boolean,
    photoUri: String?,
    audioState: CallAudioState?
) {
    val context = LocalView.current.context
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    
    var isOnHold by remember { mutableStateOf(false) }
    var callDuration by remember { mutableLongStateOf(0L) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Motion Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg_motion")
    val driftX by infiniteTransition.animateFloat(
        initialValue = -35f, targetValue = 35f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "driftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "driftY"
    )

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val startTime = System.currentTimeMillis()
            while (true) {
                callDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    val endCallCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "Radius"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // --- ANIMATED BLUR BACKGROUND ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    translationX = driftX
                    translationY = driftY
                    scaleX = 1.35f
                    scaleY = 1.35f
                }
        ) {
            if (!photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(90.dp).alpha(0.4f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(0.4f), Color.Transparent))
                ))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // --- CONTACT CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // FIXED IMAGE CONTAINER
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (!photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop // Prevents corruption/stretching
                            )
                        } else {
                            Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(40.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(text = contactName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (isOnHold) "On Hold" else if (callState == Call.STATE_ACTIVE) formatDuration(callDuration) else "Calling...",
                            color = if (isOnHold) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- UI CONTROLS ---
            if (callState != Call.STATE_RINGING) {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        
                        // Mute Button
                        CallActionButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, isActive = isMuted) {
                            // CallService.toggleMute()
                        }

                        // Hold Button (Functional)
                        CallActionButton(
                            icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            isActive = isOnHold,
                            label = "Hold"
                        ) {
                            isOnHold = !isOnHold
                            if (isOnHold) call.hold() else call.unhold()
                        }

                        // Note Button (Functional)
                        CallActionButton(icon = Icons.Default.EditNote, isActive = false, label = "Note") {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Notes for call with $contactName: ")
                            }
                            context.startActivity(Intent.createChooser(intent, "Save Note"))
                        }

                        // Speaker Button
                        CallActionButton(icon = Icons.AutoMirrored.Filled.VolumeUp, isActive = isSpeakerOn) {
                            // CallService.toggleSpeaker()
                        }
                    }

                    // End Call Button
                    Surface(
                        onClick = { try { call.disconnect() } catch (e: Exception) {} },
                        modifier = Modifier.fillMaxWidth().height(84.dp).scale(if (isPressed) 0.96f else 1f),
                        shape = RoundedCornerShape(endCallCornerRadius),
                        color = Color(0xFFD32F2F),
                        contentColor = Color.White,
                        interactionSource = interactionSource,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("End Call", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                HorizontalSwipeToAnswer(
                    onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                    onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                )
            }
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    isActive: Boolean,
    label: String? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp).background(
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(0.6f),
                CircleShape
            )
        ) {
            Icon(icon, null, tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun formatDuration(seconds: Long): String {
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s"
    )

    val maxDrag = with(density) { 130.dp.toPx() }
    val triggerThreshold = maxDrag * 0.7f

    val containerColor by animateColorAsState(
        targetValue = when {
            offsetX.value > 50f -> Color(0xFF2E7D32)
            offsetX.value < -50f -> Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        }, label = "c"
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 24.dp).clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SwipeLabel(text = "Decline", icon = Icons.Default.CallEnd, isVisible = offsetX.value < -10f)
            SwipeLabel(text = "Answer", icon = Icons.Default.Call, isVisible = offsetX.value > 10f)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                .size(76.dp).clip(CircleShape).background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    offsetX.value > triggerThreshold -> { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); onAnswer() }
                                    offsetX.value < -triggerThreshold -> { view.performHapticFeedback(HapticFeedbackConstants.REJECT); onDecline() }
                                    else -> offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Call, null, tint = containerColor, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun SwipeLabel(text: String, icon: ImageVector, isVisible: Boolean) {
    val alpha by animateFloatAsState(if (isVisible) 1f else 0.3f, label = "a")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha)) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.White)
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}