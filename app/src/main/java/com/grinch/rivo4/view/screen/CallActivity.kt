package com.grinch.rivo4.view.screen

import android.app.KeyguardManager
import android.content.Context
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoExpressiveButton
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
                        Call.STATE_ACTIVE, Call.STATE_DIALING -> {
                            acquireProximityLock()
                        }
                        else -> {
                            releaseProximityLock()
                        }
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
        proximityWakeLock?.let {
            if (!it.isHeld) {
                it.acquire()
            }
        }
    }

    private fun releaseProximityLock() {
        proximityWakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
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
    
    val avatarColors = listOf(
        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
        Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
        Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
        Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
    )
    
    val baseColor = remember(contactName) {
        avatarColors[abs(contactName.hashCode()) % avatarColors.size]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (!photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(100.dp).alpha(0.25f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    baseColor.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            
            RivoExpressiveCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shape = RoundedCornerShape(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    RivoAvatar(
                        name = contactName,
                        photoUri = photoUri,
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Column {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val statusText = when (callState) {
                            Call.STATE_RINGING -> "Incoming"
                            Call.STATE_DIALING -> "Calling..."
                            Call.STATE_ACTIVE -> durationText
                            Call.STATE_HOLDING -> "On hold"
                            else -> "In call"
                        }
                        
                        Text(
                            text = if (phoneNumber.isNotEmpty() && !isUnknown) "$statusText • $phoneNumber" else statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (callState == Call.STATE_ACTIVE) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            
            AnimatedVisibility(
                visible = callState == Call.STATE_RINGING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RivoExpressiveCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = RoundedCornerShape(48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Text(
                            "Incoming Call",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        HorizontalSwipeToAnswer(
                            onAnswer = { 
                                try {
                                    call.answer(VideoProfile.STATE_AUDIO_ONLY) 
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onDecline = { 
                                try {
                                    call.disconnect()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = callState != Call.STATE_RINGING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RivoExpressiveCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RivoExpressiveButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = "Mute",
                                size = 72.dp,
                                containerColor = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isMuted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                onClick = { CallService.mute(!isMuted) }
                            )
                            RivoExpressiveButton(
                                icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeUp,
                                label = "Speaker",
                                size = 72.dp,
                                containerColor = if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isSpeakerOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                onClick = { CallService.setSpeaker(!isSpeakerOn) }
                            )
                            RivoExpressiveButton(
                                icon = if (isHolding) Icons.Default.PlayArrow else Icons.Default.Pause,
                                label = if (isHolding) "Resume" else "Hold",
                                size = 72.dp,
                                containerColor = if (isHolding) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isHolding) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                onClick = { if (isHolding) call.unhold() else call.hold() }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        
                        Surface(
                            onClick = { 
                                try {
                                    call.disconnect()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(88.dp),
                            shape = CircleShape,
                            color = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("End Call", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
fun HorizontalSwipeToAnswer(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val view = LocalView.current
    val density = LocalDensity.current
    
    val maxDrag = with(density) { 140.dp.toPx() }
    val triggerThreshold = maxDrag * 0.7f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.6f)) {
                Icon(Icons.Default.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(28.dp))
                Text("Decline", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.6f)) {
                Icon(Icons.Default.Check, null, tint = Color(0xFF388E3C), modifier = Modifier.size(28.dp))
                Text("Answer", style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
            }
        }

        
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(width = 90.dp, height = 86.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    when {
                        offsetX.value > 20f -> Color(0xFF388E3C)
                        offsetX.value < -20f -> Color(0xFFD32F2F)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(40.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value > triggerThreshold) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onAnswer()
                                } else if (offsetX.value < -triggerThreshold) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onDecline()
                                } else {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    offsetX.value > 20f -> Icons.Default.Call
                    offsetX.value < -20f -> Icons.Default.CallEnd
                    else -> Icons.Default.DragHandle
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
