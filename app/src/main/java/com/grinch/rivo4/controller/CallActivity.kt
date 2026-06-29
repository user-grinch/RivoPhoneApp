package com.grinch.rivo4.controller

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.screen.ExpressiveCallScreen
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

class CallActivity : ComponentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        turnScreenOnAndShowWhileLocked()

        if (preferenceManager.getBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, true)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

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
                    val keepScreenOn =
                        preferenceManager.getBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, true)
                    if (keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    when (callState) {
                        Call.STATE_ACTIVE -> {
                            if (preferenceManager.getBoolean(
                                    PreferenceManager.KEY_VIBRATE_ON_ANSWER,
                                    true
                                )
                            ) {
                                this@CallActivity.window?.decorView?.performHapticFeedback(
                                    HapticFeedbackConstants.VIRTUAL_KEY
                                )
                            }
                            if (preferenceManager.getBoolean(
                                    PreferenceManager.KEY_PROXIMITY_SENSOR,
                                    true
                                )
                            ) {
                                acquireProximityLock()
                            } else {
                                releaseProximityLock()
                            }
                        }

                        Call.STATE_DIALING -> {
                            if (preferenceManager.getBoolean(
                                    PreferenceManager.KEY_PROXIMITY_SENSOR,
                                    true
                                )
                            ) {
                                acquireProximityLock()
                            } else {
                                releaseProximityLock()
                            }
                        }

                        Call.STATE_DISCONNECTED -> {
                            if (preferenceManager.getBoolean(
                                    PreferenceManager.KEY_VIBRATE_ON_HANGUP,
                                    false
                                )
                            ) {
                                this@CallActivity.window?.decorView?.performHapticFeedback(
                                    HapticFeedbackConstants.LONG_PRESS
                                )
                            }
                            releaseProximityLock()
                            delay(1200) // Brief delay to show "Call Ended" state
                            finish()
                        }

                        else -> releaseProximityLock()
                    }

                    if (session == null) {
                        delay(1200)
                        if (CallService.allCalls.value.isEmpty()) {
                            finish()
                        }
                    }
                }

                if (call != null && session != null) {
                    val details = call.details
                    val number = details?.handle?.schemeSpecificPart ?: ""

                    var contactName by remember(number) { mutableStateOf(number.ifEmpty { "Unknown" }) }
                    var photoUri by remember(number) { mutableStateOf<String?>(null) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            val contact = try {
                                contactsRepo.getContactByNumber(number)
                            } catch (e: Exception) {
                                null
                            }

                            if (contact != null) {
                                contactName = contact.name
                                photoUri = contact.photoUri
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = call,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                                .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300)))
                        },
                        label = "CallSwitch"
                    ) { targetCall ->
                        ExpressiveCallScreen(
                            call = targetCall,
                            callState = if (targetCall == call) session?.state ?: Call.STATE_ACTIVE else targetCall.state,
                            contactName = if (targetCall == call) contactName else (targetCall.details.handle?.schemeSpecificPart ?: "Unknown"),
                            phoneNumber = targetCall.details.handle?.schemeSpecificPart ?: "",
                            photoUri = if (targetCall == call) photoUri else null,
                            audioState = audioState
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }

    private fun setupProximitySensor() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        turnScreenOnAndShowWhileLocked()
    }

    private fun turnScreenOnAndShowWhileLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        CallService.isActivityVisible.value = true
    }

    override fun onStop() {
        super.onStop()
        CallService.isActivityVisible.value = false
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
