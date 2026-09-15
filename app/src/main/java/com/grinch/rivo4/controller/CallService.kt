package com.grinch.rivo4.controller

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.BlockedNumberContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.sensor.FlipToSilenceManager
import com.grinch.rivo4.controller.util.CallUiHelper
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.Contact
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

data class CallSession(
    val call: Call,
    val state: Int,
    val updateTime: Long = System.currentTimeMillis(),
    val connectTimeMillis: Long = 0L
)

class CallService : InCallService() {

    private val contactsRepository: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var redialCount = 0
    private val callStartTimes = mutableMapOf<Call, Long>()
    private val callRingStartTimes = mutableMapOf<Call, Long>()
    private val cachedContactNames = java.util.concurrent.ConcurrentHashMap<String, String>()
    private var flipToSilenceManager: FlipToSilenceManager? = null
    private var screenWakeLock: PowerManager.WakeLock? = null

    private fun acquireScreenWakeLock() {
        try {
            if (screenWakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                screenWakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "Rivo:IncomingCallWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            screenWakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(15000L) // 15 sec safety timeout
                }
            }
        } catch (e: Exception) {
            Log.w("CallService", "Failed to acquire screen wake lock: ${e.message}")
        }
    }

    private fun releaseScreenWakeLock() {
        try {
            screenWakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.w("CallService", "Failed to release screen wake lock: ${e.message}")
        }
    }

    private fun getContactBitmap(photoUri: String?): Bitmap? {
        if (photoUri == null) return null
        return try {
            val uri = Uri.parse(photoUri)
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val SILENT_CHANNEL_ID = "call_silent_channel"
        private const val MISSED_CHANNEL_ID = "missed_call_channel"
        private const val NOTIFICATION_ID = 101

        private val _currentCallSession = MutableStateFlow<CallSession?>(null)
        val currentCallSession = _currentCallSession.asStateFlow()

        private val _allCalls = MutableStateFlow<List<Call>>(emptyList())
        val allCalls = _allCalls.asStateFlow()

        private val _preferredCall = MutableStateFlow<Call?>(null)

        private val _audioState = MutableStateFlow<CallAudioState?>(null)
        val audioState = _audioState.asStateFlow()

        val isActivityVisible = MutableStateFlow(false)

        var instance: CallService? = null

        fun setPreferredCall(call: Call) {
            _preferredCall.value = call
            instance?.updateCallState()
        }

        fun mute(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun toggleMute() {
            val currentMute = _audioState.value?.isMuted ?: false
            mute(!currentMute)
        }

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun cycleAudioRoute() {
            val state = _audioState.value ?: return
            val supported = state.supportedRouteMask
            val current = state.route

            val nextRoute = when (current) {
                CallAudioState.ROUTE_EARPIECE -> {
                    if ((supported and CallAudioState.ROUTE_BLUETOOTH) != 0) CallAudioState.ROUTE_BLUETOOTH
                    else if ((supported and CallAudioState.ROUTE_SPEAKER) != 0) CallAudioState.ROUTE_SPEAKER
                    else current
                }
                CallAudioState.ROUTE_WIRED_HEADSET -> {
                    if ((supported and CallAudioState.ROUTE_SPEAKER) != 0) CallAudioState.ROUTE_SPEAKER
                    else if ((supported and CallAudioState.ROUTE_BLUETOOTH) != 0) CallAudioState.ROUTE_BLUETOOTH
                    else current
                }
                CallAudioState.ROUTE_BLUETOOTH -> {
                    if ((supported and CallAudioState.ROUTE_SPEAKER) != 0) CallAudioState.ROUTE_SPEAKER
                    else if ((supported and CallAudioState.ROUTE_EARPIECE) != 0) CallAudioState.ROUTE_EARPIECE
                    else current
                }
                CallAudioState.ROUTE_SPEAKER -> {
                    if ((supported and CallAudioState.ROUTE_EARPIECE) != 0) CallAudioState.ROUTE_EARPIECE
                    else if ((supported and CallAudioState.ROUTE_WIRED_HEADSET) != 0) CallAudioState.ROUTE_WIRED_HEADSET
                    else if ((supported and CallAudioState.ROUTE_BLUETOOTH) != 0) CallAudioState.ROUTE_BLUETOOTH
                    else current
                }
                else -> if ((supported and CallAudioState.ROUTE_SPEAKER) != 0) CallAudioState.ROUTE_SPEAKER else current
            }

            if (nextRoute != current) {
                instance?.setAudioRoute(nextRoute)
            }
        }

        fun mergeCalls() {
            val calls = instance?.getCalls() ?: return
            if (calls.size >= 2) {
                val activeCall = calls.find { it.state == Call.STATE_ACTIVE }
                val heldCall = calls.find { it.state == Call.STATE_HOLDING }
                if (activeCall != null && heldCall != null) {
                    activeCall.conference(heldCall)
                } else if (calls.size >= 2) {
                    calls[0].conference(calls[1])
                }
            }
        }

        fun answerCall() {
            instance?.let { CallRecorder.prepare(it) }
            _currentCallSession.value?.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun answerRingingCall(endActive: Boolean) {
            val inst = instance ?: return
            CallRecorder.prepare(inst)
            val calls = inst.getCalls() ?: return
            val ringing = calls.find { it.state == Call.STATE_RINGING } ?: return
            val others = calls.filter { it != ringing && it.state != Call.STATE_DISCONNECTED }

            others.forEach { other ->
                try {
                    if (endActive) other.disconnect() else if (other.state == Call.STATE_ACTIVE) other.hold()
                } catch (e: Exception) {
                }
            }

            try {
                ringing.answer(VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Exception) {
            }
        }

        fun declineCall() {
            val call = _currentCallSession.value?.call ?: return
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
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        flipToSilenceManager = FlipToSilenceManager(this)
        serviceScope.launch {
            isActivityVisible.collect {
                _currentCallSession.value?.call?.let { currentCall ->
                    updateNotification(currentCall)
                }
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            updateCallState()
            
            if (state != Call.STATE_RINGING) {
                val hasRinging = getCalls()?.any { it.state == Call.STATE_RINGING } == true
                if (!hasRinging) {
                    flipToSilenceManager?.stopListening()
                    releaseScreenWakeLock()
                }
            } else if (!callRingStartTimes.containsKey(call)) {
                callRingStartTimes[call] = System.currentTimeMillis()
                acquireScreenWakeLock()
            }

            if (state == Call.STATE_ACTIVE) {
                redialCount = 0
                startAutoRecordingIfEnabled(call)
            }

            if (state == Call.STATE_DISCONNECTED) {
                val cause = call.details.disconnectCause
                handleDisconnect(call, cause)

                val remaining = getCalls()?.filter { it.state != Call.STATE_DISCONNECTED } ?: emptyList()
                if (remaining.isEmpty()) {
                    removeForeground()
                    cancelNotification()
                }
            } else {
                updateNotification(call)
            }
        }
    }

    private fun startAutoRecordingIfEnabled(call: Call) {
        if (!preferenceManager.getBoolean(PreferenceManager.KEY_CALL_RECORDING, true)) return
        if (!preferenceManager.getBoolean(PreferenceManager.KEY_CALL_RECORDING_AUTO, false)) return
        if (CallRecorder.isRecording.value) return

        val filter = preferenceManager.getInt(PreferenceManager.KEY_CALL_RECORDING_FILTER, PreferenceManager.RECORD_FILTER_ALL)
        val isIncoming = call.details.callDirection == Call.Details.DIRECTION_INCOMING
        val isOutgoing = call.details.callDirection == Call.Details.DIRECTION_OUTGOING

        if (filter == PreferenceManager.RECORD_FILTER_INCOMING_ONLY && !isIncoming) return
        if (filter == PreferenceManager.RECORD_FILTER_OUTGOING_ONLY && !isOutgoing) return

        val number = call.details.handle?.schemeSpecificPart ?: ""
        val immediateName = call.details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: cachedContactNames[number]
            ?: number.ifEmpty { getString(R.string.label_unknown_number) }

        if (filter == PreferenceManager.RECORD_FILTER_ALL) {
            // Start recording IMMEDIATELY with 0ms delay
            CallRecorder.start(this@CallService, immediateName)
        } else {
            val cached = cachedContactNames[number]
            if (cached != null) {
                if (filter == PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY) return
                CallRecorder.start(this@CallService, cached)
            } else {
                serviceScope.launch(Dispatchers.IO) {
                    val contact = if (number.isNotEmpty()) {
                        try { contactsRepository.getContactByNumber(number) } catch (e: Exception) { null }
                    } else null
                    val isKnownContact = contact != null

                    if (filter == PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY && isKnownContact) return@launch
                    if (filter == PreferenceManager.RECORD_FILTER_CONTACTS_ONLY && !isKnownContact) return@launch

                    val name = contact?.name ?: immediateName
                    if (contact?.name != null) {
                        cachedContactNames[number] = contact.name
                    }
                    CallRecorder.start(this@CallService, name)
                }
            }
        }
    }

    private fun handleDisconnect(call: Call, cause: DisconnectCause?) {
        val number = call.details.handle?.schemeSpecificPart ?: ""

        if (CallRecorder.isRecording.value &&
            (getCalls()?.none { it != call && it.state == Call.STATE_ACTIVE } != false)) {
            CallRecorder.stop()
        }
        if (getCalls()?.none { it != call && it.state != Call.STATE_DISCONNECTED } != false) {
            CallRecorder.releasePrewarm()
        }

        if (cause?.code == DisconnectCause.BUSY &&
            preferenceManager.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_BUSY, false)) {
            
            val maxAttempts = preferenceManager.getInt(PreferenceManager.KEY_REDIAL_ATTEMPTS, 3)
            val delayMs = preferenceManager.getInt(PreferenceManager.KEY_REDIAL_DELAY, 3000).toLong()
            
            if (redialCount < maxAttempts) {
                redialCount++
                serviceScope.launch {
                    delay(delayMs)
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }
        }

        val wasNeverConnected = call.details.connectTimeMillis == 0L
        val isIncoming = call.details.callDirection == Call.Details.DIRECTION_INCOMING
        val isOutgoing = call.details.callDirection == Call.Details.DIRECTION_OUTGOING

        if (isOutgoing && wasNeverConnected) {
            val isAirplane = com.grinch.rivo4.controller.util.isAirplaneModeOn(this)
            val isWifi = com.grinch.rivo4.controller.util.isWifiConnected(this)
            val failMessage = when {
                cause?.code == DisconnectCause.RESTRICTED -> {
                    if (isAirplane && !isWifi) getString(R.string.call_failed_airplane_mode)
                    else getString(R.string.call_failed_restricted)
                }
                cause?.code == DisconnectCause.ERROR -> {
                    if (isAirplane && !isWifi) getString(R.string.call_failed_airplane_mode)
                    else cause.description?.toString()?.takeIf { it.isNotBlank() } ?: getString(R.string.call_failed_generic)
                }
                isAirplane && !isWifi && cause?.code != DisconnectCause.LOCAL && cause?.code != DisconnectCause.CANCELED -> {
                    getString(R.string.call_failed_airplane_mode)
                }
                else -> null
            }
            if (failMessage != null) {
                serviceScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(applicationContext, failMessage, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        
        val ringStartTime = callRingStartTimes.remove(call)
        val ringDurationSeconds = if (ringStartTime != null && ringStartTime > 0L) {
            ((System.currentTimeMillis() - ringStartTime) / 1000L).coerceAtLeast(1L).toInt()
        } else 0
        
        if (isIncoming && wasNeverConnected && (cause?.code == DisconnectCause.MISSED || cause?.code == DisconnectCause.REMOTE || cause?.code == DisconnectCause.REJECTED)) {
            val contact = if (number.isNotEmpty()) {
                try {
                    contactsRepository.getContactByNumber(number)
                } catch (e: Exception) { null }
            } else null
            val contactName = contact?.name ?: cachedContactNames[number] ?: number.ifEmpty { getString(R.string.label_unknown_number) }

            if (!isNumberBlocked(number) || preferenceManager.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0) == 1) {
                showMissedCallNotification(call, contact, contactName, ringDurationSeconds)
            }

            if (preferenceManager.isMissedCallCardEnabled()) {
                MissedCallActivity.start(
                    context = this,
                    contactName = contactName,
                    phoneNumber = number,
                    photoUri = contact?.photoUri,
                    ringSeconds = ringDurationSeconds
                )
            }
        }
    }

    private fun isNumberBlocked(number: String): Boolean {
        if (number.isEmpty()) return false
        return try {
            BlockedNumberContract.isBlocked(this, number)
        } catch (e: Exception) {
            false
        }
    }

    private fun handleBlockedCall(call: Call, number: String) {
        val method = preferenceManager.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0)

        if (method == 0) {
            call.reject(Call.REJECT_REASON_DECLINED)
        }

        if (preferenceManager.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) {
            showBlockedNotification(number)
        }
    }

    private fun showBlockedNotification(number: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle(getString(R.string.notif_blocked_call_title))
            .setContentText(getString(R.string.notif_blocked_call_text, number))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        notificationManager.notify(number.hashCode(), builder.build())
    }

    private fun showMissedCallNotification(call: Call, contact: Contact?, contactName: String, ringDurationSeconds: Int) {
        if (!preferenceManager.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATIONS, true)) {
            return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            MISSED_CHANNEL_ID,
            getString(R.string.notif_channel_missed_calls),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)

        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: ""
        val contactPhoto = getContactBitmap(contact?.photoUri)

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val simLabel = call.details.accountHandle?.let {
            try { telecomManager.getPhoneAccount(it)?.label?.toString() } catch (e: SecurityException) { null }
        }

        val intent = if (preferenceManager.isMissedCallCardEnabled()) {
            Intent(this, MissedCallActivity::class.java).apply {
                putExtra(MissedCallActivity.EXTRA_CONTACT_NAME, contactName)
                putExtra(MissedCallActivity.EXTRA_PHONE_NUMBER, number)
                putExtra(MissedCallActivity.EXTRA_PHOTO_URI, contact?.photoUri)
                putExtra(MissedCallActivity.EXTRA_RING_SECONDS, ringDurationSeconds)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(this, com.grinch.rivo4.MainActivity::class.java).apply {
                action = "com.grinch.rivo4.ACTION_VIEW_RECENTS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val timeString = android.text.format.DateFormat.getTimeFormat(this).format(java.util.Date())

        val missedCallText = buildString {
            append(getString(R.string.notif_missed_call_text, contactName, timeString))
            if (simLabel != null) {
                append(" ")
                append(getString(R.string.notif_via_sim, simLabel))
            }
        }

        val builder = NotificationCompat.Builder(this, MISSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(getString(R.string.notif_missed_call_title))
            .setContentText(missedCallText)
            .setLargeIcon(contactPhoto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.RED)

        notificationManager.notify(number.hashCode(), builder.build())
    }

    private fun updateCallState() {
        val calls = getCalls() ?: emptyList()
        _allCalls.value = ArrayList(calls)

        calls.forEach { c ->
            if (c.state == Call.STATE_ACTIVE) {
                val detailsTime = c.details.connectTimeMillis
                if (detailsTime > 0) {
                    callStartTimes[c] = detailsTime
                } else if (!callStartTimes.containsKey(c)) {
                    callStartTimes[c] = System.currentTimeMillis()
                }
            }
        }
        callStartTimes.keys.retainAll(calls.toSet())

        val preferred = _preferredCall.value
        if (preferred != null && (preferred !in calls || preferred.state == Call.STATE_DISCONNECTED)) {
            _preferredCall.value = null
        }

        val activePreferred = if (preferred != null && preferred.state != Call.STATE_DISCONNECTED && preferred.state != Call.STATE_HOLDING) preferred else null

        val priorityCall = calls.find { it.state == Call.STATE_RINGING }
            ?: activePreferred
            ?: calls.find { it.state == Call.STATE_DIALING || it.state == Call.STATE_CONNECTING }
            ?: calls.find { it.state == Call.STATE_ACTIVE }
            ?: calls.find { it == preferred }
            ?: calls.find { it.state == Call.STATE_HOLDING }
            ?: calls.firstOrNull { it.state != Call.STATE_DISCONNECTED }

        if (priorityCall != null) {
            val connectTime = callStartTimes[priorityCall] ?: 0L
            _currentCallSession.value = CallSession(priorityCall, priorityCall.state, connectTimeMillis = connectTime)
        } else {
            _currentCallSession.value = null
        }
    }

    private fun removeForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this
        redialCount = 0
        call.registerCallback(callCallback)

        val number = call.details.handle?.schemeSpecificPart ?: ""
        if (isNumberBlocked(number)) {
            handleBlockedCall(call, number)
            return
        }

        val isUnknownNumber = number.isBlank() || number == "null" || number == "-1" || number == "-2" ||
                call.details.handlePresentation == TelecomManager.PRESENTATION_RESTRICTED ||
                call.details.handlePresentation == TelecomManager.PRESENTATION_UNKNOWN

        if (isUnknownNumber && preferenceManager.isAutoDeclineUnknownEnabled()) {
            handleBlockedCall(call, number.ifBlank { getString(R.string.label_unknown_number) })
            return
        }

        if (preferenceManager.isAutoDeclineNonContactsEnabled()) {
            val contact = if (number.isNotBlank()) {
                try { contactsRepository.getContactByNumber(number) } catch (e: Exception) { null }
            } else null
            if (contact == null) {
                handleBlockedCall(call, number.ifBlank { getString(R.string.label_unknown_number) })
                return
            }
        }

        if (call.state == Call.STATE_RINGING) {
            callRingStartTimes[call] = System.currentTimeMillis()
        }

        // Prime CallRecorder asynchronously so recording starts with zero delay when call answers
        CallRecorder.prepare(this)
        if (number.isNotEmpty()) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val contact = contactsRepository.getContactByNumber(number)
                    if (contact != null) {
                        cachedContactNames[number] = contact.name
                    }
                } catch (e: Exception) {
                    Log.e("CallService", "Failed to pre-cache contact name: ${e.message}")
                }
            }
        }

        updateCallState()

        val isIncoming = call.state == Call.STATE_RINGING
        if (isIncoming) {
            acquireScreenWakeLock()
            if (preferenceManager.getBoolean(PreferenceManager.KEY_FLIP_TO_SILENCE, false)) {
                flipToSilenceManager?.startListening()
            }
        }

        // 1. Post notification FIRST so foreground service & fullScreenIntent are fully armed
        updateNotification(call)

        // 2. Direct full-screen activity start as an active trigger
        val showFullScreen = !isIncoming || CallUiHelper.shouldShowFullScreen(this, preferenceManager)

        if (showFullScreen) {
            val intent = Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("CallService", "Failed to start CallActivity: ${e.message}", e)
            }
        } else {
            Log.i("CallService", "User is actively in another app; presenting heads-up incoming call notification only.")
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        updateCallState()
        val calls = getCalls() ?: emptyList()
        val hasRinging = calls.any { it.state == Call.STATE_RINGING }
        if (!hasRinging) {
            flipToSilenceManager?.stopListening()
            releaseScreenWakeLock()
        }
        if (calls.isEmpty()) {
            if (CallRecorder.isRecording.value) CallRecorder.stop()
            com.grinch.rivo4.controller.floating.FloatingCallService.stop(this)
            removeForeground()
            cancelNotification()
        } else {
            _currentCallSession.value?.call?.let { updateNotification(it) }
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        _audioState.value = audioState
        _currentCallSession.value?.call?.let { updateNotification(it) }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ANSWER_CALL" -> {
                answerCall()
                val activityIntent = Intent(this, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                startActivity(activityIntent)
            }
            "DECLINE_CALL" -> declineCall()
            "TOGGLE_MUTE" -> toggleMute()
            "TOGGLE_SPEAKER" -> cycleAudioRoute()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(call: Call) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_calls),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_calls_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)

        val silentChannel = NotificationChannel(
            SILENT_CHANNEL_ID,
            getString(R.string.notif_channel_calls),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_calls_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(silentChannel)

        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: ""

        val contact = if (number.isNotEmpty()) {
            try {
                contactsRepository.getContactByNumber(number)
            } catch (e: Exception) { null }
        } else null

        val contactName = when {
            contact != null -> contact.name
            number.isNotEmpty() -> number
            else -> getString(R.string.label_unknown_number)
        }
        
        val contactPhoto = getContactBitmap(contact?.photoUri)

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val accountHandle = call.details.accountHandle
        val simLabel = accountHandle?.let {
            try {
                telecomManager.getPhoneAccount(it)?.label?.toString()
            } catch (e: SecurityException) { null }
        }
        
        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            if (call.state == Call.STATE_RINGING) call.hashCode() else 0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(this, CallService::class.java).apply { action = "ANSWER_CALL" }
        val answerPendingIntent = PendingIntent.getService(this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, CallService::class.java).apply { action = "DECLINE_CALL" }
        val declinePendingIntent = PendingIntent.getService(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val muteIntent = Intent(this, CallService::class.java).apply { action = "TOGGLE_MUTE" }
        val mutePendingIntent = PendingIntent.getService(this, 3, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val speakerIntent = Intent(this, CallService::class.java).apply { action = "TOGGLE_SPEAKER" }
        val speakerPendingIntent = PendingIntent.getService(this, 4, speakerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val personBuilder = androidx.core.app.Person.Builder()
            .setName(contactName)
            .setImportant(true)
        
        if (contactPhoto != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(contactPhoto))
        }
        val person = personBuilder.build()

        val audioState = _audioState.value
        val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        val audioLabel = when (audioRoute) {
            CallAudioState.ROUTE_SPEAKER -> getString(R.string.audio_route_speaker)
            CallAudioState.ROUTE_BLUETOOTH -> {
                try {
                    audioState?.activeBluetoothDevice?.name ?: getString(R.string.audio_route_bluetooth)
                } catch (e: SecurityException) {
                    getString(R.string.audio_route_bluetooth)
                }
            }
            CallAudioState.ROUTE_WIRED_HEADSET -> getString(R.string.audio_route_headset)
            else -> getString(R.string.audio_route_handset)
        }

        val contentText = buildString {
            if (call.state == Call.STATE_RINGING) append(getString(R.string.call_status_incoming)) else append(getString(R.string.notif_active_call))
            if (!simLabel.isNullOrEmpty()) {
                append(" ")
                append(getString(R.string.notif_via_sim, simLabel))
            }
        }

        val connectTime = callStartTimes[call]
            ?: call.details.connectTimeMillis.takeIf { it > 0 }
            ?: System.currentTimeMillis()

        val isRinging = call.state == Call.STATE_RINGING

        // CRITICAL FOR VIVO & OEM COMPATIBILITY:
        // Ringing calls must NEVER use the silent channel or low priority!
        // Ringing calls need IMPORTANCE_HIGH and PRIORITY_MAX so the system can trigger
        // heads-up banners or full-screen intents across all OEMs (including Vivo, Xiaomi, Samsung).
        val targetChannel = if (isRinging) {
            CHANNEL_ID
        } else if (isActivityVisible.value) {
            // Ongoing call with CallActivity currently visible on screen:
            // Keep notification low-priority in the status bar so it doesn't obstruct the conversation.
            SILENT_CHANNEL_ID
        } else {
            // Ongoing call while user is multitasking/in background:
            // Use CHANNEL_ID so the notification is easily reachable in the notification shade.
            CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(this, targetChannel)
            .setSmallIcon(if (isRinging) android.R.drawable.sym_call_incoming else R.drawable.ic_call_ongoing)
            .setContentTitle(contactName)
            .setContentText(contentText)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setStyle(
                if (isRinging) {
                    NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent)
                } else {
                    NotificationCompat.CallStyle.forOngoingCall(person, declinePendingIntent)
                }
            )

        if (isRinging) {
            // Incoming ringing call: Always MAX priority + full-screen intent.
            // Android and OEM notification managers (especially Vivo Funtouch/OriginOS) require this
            // to show the heads-up notification and/or launch the incoming call activity when locked or unlocked.
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
            builder.setSilent(false)
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
        } else if (targetChannel == SILENT_CHANNEL_ID) {
            // Ongoing call while inside CallActivity: quiet status bar icon
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
            builder.setSilent(true)
            builder.setOnlyAlertOnce(true)
        } else {
            // Ongoing call while in background: standard ongoing call priority
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setSilent(true)
            builder.setOnlyAlertOnce(true)
        }

        if (call.state == Call.STATE_ACTIVE) {
            builder.setWhen(connectTime)
            builder.setUsesChronometer(true)
        }

        if (!isRinging) {
            val isMuted = audioState?.isMuted ?: false
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.stat_notify_call_mute,
                    if (isMuted) getString(R.string.action_unmute) else getString(R.string.action_mute),
                    mutePendingIntent
                ).build()
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.stat_sys_speakerphone,
                    audioLabel,
                    speakerPendingIntent
                ).build()
            )
        }

        val notification = builder.build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && CallRecorder.isRecording.value && CallRecorder.hasAudioPermission(this)) {
                    fgsType = fgsType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("CallService", "Error starting foreground notification: ${e.message}", e)
        }
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        releaseScreenWakeLock()
        super.onDestroy()
        flipToSilenceManager?.stopListening()
        if (CallRecorder.isRecording.value) CallRecorder.stop()
        if (instance == this) instance = null
        serviceScope.cancel()
    }
}
