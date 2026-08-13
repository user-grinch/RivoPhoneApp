package com.grinch.rivo4.controller

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.CallBackgroundStore
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.screen.ExpressiveCallScreen
import com.grinch.rivo4.view.theme.Rivo4Theme
import com.grinch.rivo4.view.theme.RivoStaticMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private data class CallIdentity(
    val number: String,
    val name: String,
    val photoUri: String?,
    val backgroundUri: String?
)

private data class CachedCallIdentity(
    val identity: CallIdentity,
    val version: Int
)

class CallActivity : ComponentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var isFinishingCall = false
    private var keyguardDismissRequested = false
    private val identityCache = mutableMapOf<String, CachedCallIdentity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CallBackgroundStore.attach(preferenceManager)

        if (CallService.allCalls.value.none { it.state != Call.STATE_DISCONNECTED } &&
            CallService.currentCallSession.value == null
        ) {
            finish()
            return
        }

        turnScreenOnAndShowWhileLocked()

        if (preferenceManager.getBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, true)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupProximitySensor()
        applySystemBarStyle(isNightMode())

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()
                val settingsState by preferenceManager.settingsChanged.collectAsState()

                var retainedSession by remember { mutableStateOf<CallSession?>(null) }
                LaunchedEffect(session) {
                    session?.let { retainedSession = it }
                }

                val displaySession = session ?: retainedSession
                val displayCall = displaySession?.call
                val callState = session?.state

                val identity = if (displayCall != null) {
                    rememberCallIdentity(displayCall, settingsState)
                } else {
                    null
                }

                val darkTheme = isSystemInDarkTheme()
                val lightSystemBarIcons = darkTheme ||
                        identity?.backgroundUri != null ||
                        identity?.photoUri != null

                DisposableEffect(lightSystemBarIcons) {
                    applySystemBarStyle(lightSystemBarIcons)
                    onDispose { }
                }

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
                                    HapticFeedbackConstants.CONFIRM
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
                                    HapticFeedbackConstants.REJECT
                                )
                            }
                            releaseProximityLock()
                            delay(1200)
                            dismissCallScreen()
                        }

                        else -> releaseProximityLock()
                    }

                    if (session == null) {
                        delay(1200)
                        if (CallService.allCalls.value.none { it.state != Call.STATE_DISCONNECTED }) {
                            dismissCallScreen()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AnimatedContent(
                        targetState = displayCall,
                        transitionSpec = {
                            fadeIn(animationSpec = RivoStaticMotion.screenEnterEffects())
                                .togetherWith(
                                    fadeOut(animationSpec = RivoStaticMotion.screenExitEffects())
                                )
                        },
                        label = "CallSwitch"
                    ) { targetCall ->
                        if (targetCall == null) {
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            val isDisplayed = targetCall === displayCall
                            val targetIdentity =
                                if (isDisplayed && identity != null) {
                                    identity
                                } else {
                                    rememberCallIdentity(targetCall, settingsState)
                                }
                            val targetState = if (isDisplayed) {
                                session?.state ?: targetCall.state
                            } else {
                                targetCall.state
                            }
                            val connectTime = if (isDisplayed) {
                                displaySession?.connectTimeMillis ?: 0L
                            } else {
                                targetCall.details?.connectTimeMillis ?: 0L
                            }

                            ExpressiveCallScreen(
                                call = targetCall,
                                callState = targetState,
                                contactName = targetIdentity.name,
                                phoneNumber = targetIdentity.number,
                                photoUri = targetIdentity.photoUri,
                                audioState = audioState,
                                initialConnectTime = connectTime,
                                backgroundUri = targetIdentity.backgroundUri
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun rememberCallIdentity(call: Call, settingsState: Int): CallIdentity {
        val context = LocalContext.current
        val unknownLabel = stringResource(R.string.label_unknown)
        val number = remember(call) { call.details?.handle?.schemeSpecificPart.orEmpty() }

        var identity by remember(number, unknownLabel) {
            val base = cachedIdentity(number, settingsState)
                ?: CallIdentity(number, number.ifEmpty { unknownLabel }, null, null)
            mutableStateOf(
                if (base.backgroundUri == null) {
                    base.copy(backgroundUri = CallBackgroundStore.defaultModel(context))
                } else {
                    base
                }
            )
        }

        LaunchedEffect(number, settingsState) {
            val handle = number.ifEmpty { null }

            val handleResult = CallBackgroundStore.resolveResult(context, handle, null)
            val handleBackground = handleResult.uri
            if (handleBackground != null && handleBackground != identity.backgroundUri) {
                identity = identity.copy(backgroundUri = handleBackground)
            }

            val lookup = if (handle == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { contactsRepo.getContactByNumber(number) }
                }
            }
            val contact = lookup?.getOrNull()
            val contactFailed = lookup?.isFailure == true

            val contactId = contact?.id?.takeIf { it.isNotBlank() }
            val idResult = if (handleBackground == null && contactId != null) {
                CallBackgroundStore.resolveResult(context, handle, contactId)
            } else {
                null
            }

            val resolvedBackground = handleBackground ?: idResult?.uri
            val resolveFailed =
                handleResult.failed || idResult?.failed == true || contactFailed
            val defaultBackground = CallBackgroundStore.defaultModelAsync(context)
            val background = resolvedBackground
                ?: identity.backgroundUri.takeIf { resolveFailed }
                ?: defaultBackground

            val resolved = CallIdentity(
                number = number,
                name = contact?.name?.takeIf { it.isNotBlank() }
                    ?: identity.name.takeIf { contactFailed && it.isNotBlank() }
                    ?: number.ifEmpty { unknownLabel },
                photoUri = contact?.photoUri ?: identity.photoUri.takeIf { contactFailed },
                backgroundUri = background
            )
            cacheIdentity(number, resolved, settingsState)
            identity = resolved
        }

        return identity
    }

    private fun cachedIdentity(number: String, version: Int): CallIdentity? {
        if (number.isEmpty()) return null
        val cached = identityCache[number] ?: return null
        if (cached.version != version) return null
        return cached.identity
    }

    private fun cacheIdentity(number: String, identity: CallIdentity, version: Int) {
        identityCache.entries.removeAll { it.value.version != version }
        if (number.isEmpty()) return
        identityCache[number] = CachedCallIdentity(identity, version)
    }

    private fun isNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun applySystemBarStyle(lightIcons: Boolean) {
        val style = if (lightIcons) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
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
        if (!isFinishingCall && CallService.allCalls.value.any { it.state != Call.STATE_DISCONNECTED }) {
            turnScreenOnAndShowWhileLocked()
        }
    }

    private fun dismissCallScreen() {
        if (isFinishingCall) return
        isFinishingCall = true

        if (CallService.allCalls.value.any { it.state != Call.STATE_DISCONNECTED }) {
            isFinishingCall = false
            return
        }

        setShowWhenLocked(false)
        setTurnScreenOn(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        finishAndRemoveTask()
    }

    private fun turnScreenOnAndShowWhileLocked() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        if (!keyguardDismissRequested) {
            keyguardDismissRequested = true
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
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
