package com.grinch.rivo4.view.screen

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalContext
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
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import java.util.*
import kotlin.math.roundToInt

class CallActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModel()
    private val contactsRepo: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Comprehensive lock screen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        setupProximitySensor()
        enableEdgeToEdge()

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()
                val settingsState by preferenceManager.settingsChanged.collectAsState()

                val call = session?.call
                val callState = session?.state

                LaunchedEffect(callState, settingsState) {
                    when (callState) {
                        Call.STATE_ACTIVE -> {
                            if (preferenceManager.getBoolean(PreferenceManager.KEY_VIBRATE_ON_ANSWER, true)) {
                                this@CallActivity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            if (preferenceManager.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) {
                                acquireProximityLock()
                            } else {
                                releaseProximityLock()
                            }
                        }
                        Call.STATE_DIALING -> {
                            if (preferenceManager.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) {
                                acquireProximityLock()
                            } else {
                                releaseProximityLock()
                            }
                        }
                        Call.STATE_DISCONNECTED -> {
                            if (preferenceManager.getBoolean(PreferenceManager.KEY_VIBRATE_ON_HANGUP, false)) {
                                this@CallActivity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            releaseProximityLock()
                            delay(1200) // Brief delay to show "Call Ended" state
                            finish()
                        }
                        else -> releaseProximityLock()
                    }

                    if (session == null) {
                        delay(1200)
                        finish()
                    }
                }

                if (call != null && session != null) {
                    val details = call.details
                    val number = details?.handle?.schemeSpecificPart ?: ""

                    var contactName by remember { mutableStateOf(number.ifEmpty { "Unknown" }) }
                    var photoUri by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            val contact = try {
                                contactsRepo.getContactByNumber(number)
                            } catch (e: Exception) { null }

                            if (contact != null) {
                                contactName = contact.name
                                photoUri = contact.photoUri
                            }
                        }
                    }

                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
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

    override fun onDestroy() {
        super.onDestroy()
        releaseProximityLock()
    }

    private fun acquireProximityLock() {
        if (preferenceManager.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) {
            proximityWakeLock?.let { if (!it.isHeld) it.acquire() }
        }
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
    photoUri: String?,
    audioState: CallAudioState?
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER

    var isOnHold by remember { mutableStateOf(false) }
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

        // --- EXPRESSIVE BACKGROUND ---
        ExpressiveBackground(photoUri)
        
        // Add animated particles for dynamic feel
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HERO SECTION ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.weight(if (showKeypad) 0.7f else 1f)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Animate content appearance
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

                        val statusText = when {
                            callState == Call.STATE_DISCONNECTED -> "Call Ended"
                            isOnHold -> "On Hold"
                            callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                            callState == Call.STATE_DIALING -> "Calling..."
                            callState == Call.STATE_RINGING -> "Incoming call"
                            else -> "Connecting..."
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isOnHold) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!showKeypad) {
                    Spacer(modifier = Modifier.height(64.dp))

                    if (callState == Call.STATE_RINGING) {
                        PulsingAvatar(photoUri)
                    } else {
                        HeroAvatar(photoUri)
                    }
                }
            }

            if (showKeypad) {
                Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
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
                    // Row 1 (2 centered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
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
                        
                        Spacer(modifier = Modifier.width(32.dp))

                        CallActionButton(
                            icon = Icons.Default.Dialpad,
                            isActive = showKeypad,
                            label = "Keypad"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showKeypad = !showKeypad
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
                            icon = Icons.Default.PersonAdd,
                            isActive = false,
                            label = "Add"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                val handle = call.details.handle
                                if (handle != null) {
                                    putExtra(ContactsContract.Intents.Insert.PHONE, handle.schemeSpecificPart)
                                }
                            }
                            context.startActivity(intent)
                        }

                        CallActionButton(
                            icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            isActive = isOnHold,
                            label = if (isOnHold) "Resume" else "Hold"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isOnHold = !isOnHold
                            if (isOnHold) call.hold() else call.unhold()
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
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
                        
                        Spacer(modifier = Modifier.width(32.dp))

                        CallActionButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            isActive = isSpeakerOn,
                            label = "Speaker"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            CallService.setSpeaker(!isSpeakerOn)
                        }
                    }

                    HorizontalSwipeToAnswer(
                        onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                        onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                    )
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
        // Display typed digits with expressive typography
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
fun VerticalSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s"
    )

    val maxDrag = with(density) { 150.dp.toPx() }
    val triggerThreshold = maxDrag * 0.6f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        // Track
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha( (offsetY.value / -maxDrag).coerceIn(0f, 1f) )) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                Text("Answer", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelLarge)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha( (offsetY.value / maxDrag).coerceIn(0f, 1f) )) {
                Text("Decline", color = Color(0xFFF44336), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
            }
        }

        // Draggable Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
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
                            coroutineScope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceIn(-maxDrag, maxDrag)) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        
        if (offsetY.value == 0f) {
            Text(
                "Swipe up to answer, down to decline",
                modifier = Modifier.offset(y = 60.dp).alpha(0.6f),
                style = MaterialTheme.typography.labelMedium
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
        
        // Dark overlay for better readability in both light and dark modes
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
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
        )
        // Second pulsing ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale * 1.1f)
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
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
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

    val maxDrag = with(density) { 120.dp.toPx() }
    val triggerThreshold = maxDrag * 0.7f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        // Background Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Decline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336), // Solid red
                modifier = Modifier.alpha( (offsetX.value / -maxDrag).coerceIn(0.4f, 1f) )
            )
            Text(
                "Answer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50), // Solid green
                modifier = Modifier.alpha( (offsetX.value / maxDrag).coerceIn(0.4f, 1f) )
            )
        }

        // Draggable Handle (Google Dialer Style: White circle with icon)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(90.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White)
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
                            coroutineScope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = if (offsetX.value > 10f) Color(0xFF4CAF50) 
                       else if (offsetX.value < -10f) Color(0xFFF44336) 
                       else Color(0xFF4CAF50), // Default green as in screenshot handle
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun SwipeLabel(text: String, icon: ImageVector, isVisible: Boolean) {
    val alpha by animateFloatAsState(if (isVisible) 1f else 0.5f, label = "a")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
