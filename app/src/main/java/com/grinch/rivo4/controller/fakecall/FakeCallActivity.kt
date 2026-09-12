package com.grinch.rivo4.controller.fakecall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallRecorder
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.screen.ActiveCallControls
import com.grinch.rivo4.view.screen.CallActionButton
import com.grinch.rivo4.view.screen.ExpressiveBackground
import com.grinch.rivo4.view.screen.HeroAvatar
import com.grinch.rivo4.view.screen.HorizontalSwipeToAnswer
import com.grinch.rivo4.view.screen.IPhoneSwipeToAnswer
import com.grinch.rivo4.view.screen.InCallKeypad
import com.grinch.rivo4.view.screen.IncomingCallButtons
import com.grinch.rivo4.view.screen.PulsingAvatar
import com.grinch.rivo4.view.screen.VerticalSwipeToAnswer
import com.grinch.rivo4.view.screen.formatDuration
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.seconds

class FakeCallActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FakeCallActivity"
        private var activeActivity: java.lang.ref.WeakReference<FakeCallActivity>? = null

        fun dismissCurrentCall() {
            activeActivity?.get()?.dismissFakeCall()
        }

        fun toggleMuteCurrentCall() {
            activeActivity?.get()?.toggleMute()
        }

        fun toggleSpeakerCurrentCall() {
            activeActivity?.get()?.toggleSpeaker()
        }
    }

    private val preferenceManager: PreferenceManager by inject()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var isFinishingCall = false

    private var isMuted by mutableStateOf(false)
    private var isSpeakerOn by mutableStateOf(false)
    private var currentConnectTime = 0L

    fun toggleMute() {
        isMuted = !isMuted
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isMicrophoneMute = isMuted
        updateOngoingNotification()
    }

    fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = isSpeakerOn
        updateOngoingNotification()
    }

    private fun updateOngoingNotification() {
        val callerName = intent.getStringExtra(FakeCallManager.EXTRA_NAME) ?: "Mom"
        val phoneNumber = intent.getStringExtra(FakeCallManager.EXTRA_NUMBER) ?: "+1 (555) 019-2834"
        val photoUri = intent.getStringExtra(FakeCallManager.EXTRA_PHOTO_URI)
        val time = if (currentConnectTime > 0) currentConnectTime else System.currentTimeMillis()
        FakeCallNotificationManager.showOngoingCallNotification(
            context = this,
            callerName = callerName,
            phoneNumber = phoneNumber,
            photoUri = photoUri,
            connectTimeMillis = time,
            isMuted = isMuted,
            isSpeakerOn = isSpeakerOn
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeActivity = java.lang.ref.WeakReference(this)
        enableEdgeToEdge()

        turnScreenOnAndShowWhileLocked()

        if (preferenceManager.getBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, true)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupProximitySensor()
        initToneGenerator()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val callerName = intent.getStringExtra(FakeCallManager.EXTRA_NAME) ?: "Mom"
        val phoneNumber = intent.getStringExtra(FakeCallManager.EXTRA_NUMBER) ?: "+1 (555) 019-2834"
        val photoUri = intent.getStringExtra(FakeCallManager.EXTRA_PHOTO_URI)
        val shouldVibrate = intent.getBooleanExtra(FakeCallManager.EXTRA_VIBRATE, true)
        val startAnswered = intent.getBooleanExtra(FakeCallManager.EXTRA_START_ANSWERED, false)

        val initialCallState = if (startAnswered) Call.STATE_ACTIVE else Call.STATE_RINGING
        val initialConnectTime = if (startAnswered) System.currentTimeMillis() else 0L
        currentConnectTime = initialConnectTime

        if (startAnswered) {
            acquireProximityLock()
            updateOngoingNotification()
        } else {
            startRingtoneAndVibration(shouldVibrate)
            FakeCallNotificationManager.showIncomingCallNotification(
                context = this,
                scheduleId = intent.getStringExtra(FakeCallManager.EXTRA_SCHEDULE_ID) ?: "",
                callerName = callerName,
                phoneNumber = phoneNumber,
                photoUri = photoUri,
                vibrate = shouldVibrate
            )
        }

        setContent {
            Rivo4Theme {
                FakeCallScreenContent(
                    callerName = callerName,
                    phoneNumber = phoneNumber,
                    photoUri = photoUri,
                    preferenceManager = preferenceManager,
                    initialCallState = initialCallState,
                    initialConnectTime = initialConnectTime,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    onToggleMute = { toggleMute() },
                    onToggleSpeaker = { toggleSpeaker() },
                    onAnswerCall = {
                        stopRingtoneAndVibration()
                        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        acquireProximityLock()
                        currentConnectTime = System.currentTimeMillis()
                        updateOngoingNotification()
                    },
                    onDeclineOrEndCall = {
                        stopRingtoneAndVibration()
                        window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        releaseProximityLock()
                        FakeCallNotificationManager.cancelNotification(this@FakeCallActivity)
                        dismissFakeCall()
                    },
                    onPlayDtmf = { digit ->
                        playDtmfTone(digit)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(FakeCallManager.EXTRA_START_ANSWERED, false)) {
            stopRingtoneAndVibration()
            acquireProximityLock()
        }
    }

    private fun turnScreenOnAndShowWhileLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun setupProximitySensor() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "rivo:fake_call_proximity"
            )
        }
    }

    private fun acquireProximityLock() {
        if (preferenceManager.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) {
            if (proximityWakeLock?.isHeld == false) {
                proximityWakeLock?.acquire(10 * 60 * 1000L)
            }
        }
    }

    private fun releaseProximityLock() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock?.release()
        }
    }

    private fun initToneGenerator() {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize ToneGenerator: ${e.message}")
            null
        }
    }

    private fun playDtmfTone(digit: Char) {
        val tone = when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> -1
        }
        if (tone != -1) {
            try {
                toneGenerator?.startTone(tone, 150)
            } catch (e: Exception) {
                Log.w(TAG, "DTMF play tone error: ${e.message}")
            }
        }
    }

    private fun startRingtoneAndVibration(vibrateFromConfig: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                    )
                    setDataSource(this@FakeCallActivity, ringtoneUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ringtone: ${e.message}", e)
            }
        }

        val shouldVibrate = when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> false
            AudioManager.RINGER_MODE_VIBRATE -> true
            AudioManager.RINGER_MODE_NORMAL -> {
                if (!vibrateFromConfig) false
                else {
                    try {
                        android.provider.Settings.System.getInt(contentResolver, "vibrate_when_ringing", 1) != 0
                    } catch (e: Exception) {
                        true
                    }
                }
            }
            else -> vibrateFromConfig
        }

        if (shouldVibrate) {
            try {
                val pattern = longArrayOf(0, 1000, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start vibration: ${e.message}", e)
            }
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player: ${e.message}")
        }
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping vibrator: ${e.message}")
        }
    }

    fun dismissFakeCall() {
        if (isFinishingCall) return
        isFinishingCall = true
        if (CallRecorder.isRecording.value) {
            CallRecorder.stop()
        }
        stopRingtoneAndVibration()
        releaseProximityLock()
        FakeCallNotificationManager.cancelNotification(this)
        lifecycleScope.launch {
            delay(1200)
            finishAndRemoveTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (CallRecorder.isRecording.value) {
            CallRecorder.stop()
        }
        if (activeActivity?.get() == this) {
            activeActivity = null
        }
        stopRingtoneAndVibration()
        releaseProximityLock()
        FakeCallNotificationManager.cancelNotification(this)
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {}

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = false
        audioManager?.isMicrophoneMute = false
    }
}

@Composable
private fun FakeCallScreenContent(
    callerName: String,
    phoneNumber: String,
    photoUri: String?,
    preferenceManager: PreferenceManager,
    initialCallState: Int = Call.STATE_RINGING,
    initialConnectTime: Long = 0L,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onAnswerCall: () -> Unit,
    onDeclineOrEndCall: () -> Unit,
    onPlayDtmf: (Char) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var callState by remember { mutableStateOf(initialCallState) }
    var connectTimeMillis by remember { mutableLongStateOf(initialConnectTime) }
    var callDuration by remember { mutableLongStateOf(0L) }
    var showKeypad by remember { mutableStateOf(false) }
    var typedDigits by remember { mutableStateOf("") }

    val settingsState by preferenceManager.settingsChanged.collectAsState()
    val showCallScreenAvatar = remember(settingsState) {
        preferenceManager.getBoolean(PreferenceManager.KEY_SHOW_CALL_SCREEN_AVATAR, true)
    }
    val recordingEnabled = remember(settingsState) {
        preferenceManager.getBoolean(PreferenceManager.KEY_CALL_RECORDING, true)
    }
    val isRecording by CallRecorder.isRecording.collectAsState()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                CallRecorder.start(context, callerName.ifBlank { phoneNumber })
            }
        }
    )

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            if (connectTimeMillis == 0L) {
                connectTimeMillis = System.currentTimeMillis()
            }
            while (true) {
                callDuration = (System.currentTimeMillis() - connectTimeMillis) / 1000
                delay(1.seconds)
            }
        }
    }

    BackHandler {
        if (showKeypad) {
            showKeypad = false
        } else {
            callState = Call.STATE_DISCONNECTED
            onDeclineOrEndCall()
        }
    }

    val statusText = when (callState) {
        Call.STATE_DISCONNECTED -> stringResource(R.string.fake_call_screen_ended)
        Call.STATE_HOLDING -> stringResource(R.string.call_status_on_hold)
        Call.STATE_ACTIVE -> formatDuration(callDuration)
        else -> stringResource(R.string.fake_call_screen_incoming)
    }

    val fakeAudioState = remember(isMuted, isSpeakerOn) {
        CallAudioState(
            isMuted,
            if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
        )
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

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
                        if (callState == Call.STATE_RINGING) {
                            PulsingAvatar(photoUri, isLandscape)
                        } else {
                            HeroAvatar(photoUri, isLandscape)
                        }
                        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 20.dp))
                    }
                }
            }

            Text(
                text = callerName,
                style = if (isLandscape) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (phoneNumber.isNotEmpty() && phoneNumber != callerName) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = when (callState) {
                    Call.STATE_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    Call.STATE_HOLDING -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                },
                shape = CircleShape
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = when (callState) {
                        Call.STATE_ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                        Call.STATE_HOLDING -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
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
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.call_recording_in_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
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
                onPlayDtmf(digit)
            },
            onDigitUp = {},
            onBackspace = { typedDigits = typedDigits.dropLast(1) }
        )
    }

    val activeControls: @Composable (Boolean) -> Unit = { compact ->
        ActiveCallControls(
            callState = callState,
            isMuted = isMuted,
            audioState = fakeAudioState,
            showKeypad = showKeypad,
            recordingEnabled = recordingEnabled,
            isRecording = isRecording,
            compact = compact,
            onToggleMute = onToggleMute,
            onToggleKeypad = { showKeypad = !showKeypad },
            onAudioClick = onToggleSpeaker,
            onAddCall = {},
            onToggleHold = {
                callState = if (callState == Call.STATE_HOLDING) Call.STATE_ACTIVE else Call.STATE_HOLDING
            },
            onMessage = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                }
                try { context.startActivity(intent) } catch (e: Exception) {}
            },
            onToggleRecording = {
                if (isRecording) {
                    CallRecorder.stop()
                } else {
                    if (CallRecorder.hasAudioPermission(context)) {
                        CallRecorder.start(context, callerName.ifBlank { phoneNumber })
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onEndCall = {
                callState = Call.STATE_DISCONNECTED
                onDeclineOrEndCall()
            }
        )
    }

    val incomingControls: @Composable (Boolean) -> Unit = { compact ->
        val useCustomUI = preferenceManager.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)
        val onAnswerAction = {
            callState = Call.STATE_ACTIVE
            onAnswerCall()
        }
        val onDeclineAction = {
            callState = Call.STATE_DISCONNECTED
            onDeclineOrEndCall()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 24.dp)
        ) {
            if (useCustomUI != 2 && useCustomUI != 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallActionButton(
                        icon = Icons.AutoMirrored.Filled.Message,
                        isActive = false,
                        label = stringResource(R.string.action_message),
                        compact = compact,
                        modifier = Modifier.width(if (compact) 108.dp else 132.dp)
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeclineAction()
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$phoneNumber")
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                }
            }

            when (useCustomUI) {
                1 -> IncomingCallButtons(
                    onAnswer = onAnswerAction,
                    onDecline = onDeclineAction
                )
                2 -> IPhoneSwipeToAnswer(
                    onAnswer = onAnswerAction,
                    onDecline = onDeclineAction,
                    onMessage = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeclineAction()
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$phoneNumber")
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                )
                3 -> VerticalSwipeToAnswer(
                    onAnswer = onAnswerAction,
                    onDecline = onDeclineAction
                )
                else -> HorizontalSwipeToAnswer(
                    onAnswer = onAnswerAction,
                    onDecline = onDeclineAction
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ExpressiveBackground(photoUri, null)

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
