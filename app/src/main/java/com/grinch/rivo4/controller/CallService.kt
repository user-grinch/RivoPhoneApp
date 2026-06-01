package com.grinch.rivo4.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.screen.CallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.grinch.rivo4.controller.util.PreferenceManager

data class CallSession(
    val call: Call,
    val state: Int,
    val updateTime: Long = System.currentTimeMillis()
)

class CallService : InCallService() {

    private val contactsRepository: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val NOTIFICATION_ID = 101

        private val _currentCallSession = MutableStateFlow<CallSession?>(null)
        val currentCallSession = _currentCallSession.asStateFlow()

        private val _allCalls = MutableStateFlow<List<Call>>(emptyList())
        val allCalls = _allCalls.asStateFlow()

        private val _audioState = MutableStateFlow<CallAudioState?>(null)
        val audioState = _audioState.asStateFlow()

        private var instance: CallService? = null

        fun mute(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun setSpeaker(on: Boolean) {
            val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            instance?.setAudioRoute(route)
        }

        fun toggleMute() {
            val currentMute = _audioState.value?.isMuted ?: false
            mute(!currentMute)
        }

        fun toggleSpeaker() {
            val isSpeaker = _audioState.value?.route == CallAudioState.ROUTE_SPEAKER
            setSpeaker(!isSpeaker)
        }

        fun mergeCalls() {
            val calls = instance?.getCalls() ?: return
            if (calls.size >= 2) {
                val activeCall = calls.find { it.state == Call.STATE_ACTIVE }
                val heldCall = calls.find { it.state == Call.STATE_HOLDING }
                if (activeCall != null && heldCall != null) {
                    activeCall.conference(heldCall)
                } else if (calls.size >= 2) {
                    // Fallback: try conferencing the first two calls
                    calls[0].conference(calls[1])
                }
            }
        }

        fun answerCall() {
            _currentCallSession.value?.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun declineCall() {
            _currentCallSession.value?.call?.disconnect()
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            updateCallState()
            
            if (state == Call.STATE_DISCONNECTED) {
                val disconnectCause = call.details.disconnectCause
                if (call.state == Call.STATE_RINGING || (disconnectCause != null && disconnectCause.code == android.telecom.DisconnectCause.MISSED)) {
                    showMissedCallNotification(call)
                }

                if ((instance?.getCalls()?.size ?: 0) == 0) {
                    removeForeground()
                    cancelNotification()
                }
            } else {
                updateNotification(call)
            }
        }
    }

    private fun showMissedCallNotification(call: Call) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: ""
        
        val contact = if (number.isNotEmpty()) {
            try {
                contactsRepository.getContactByNumber(number)
            } catch (e: Exception) { null }
        } else null
        
        val contactName = contact?.name ?: number.ifEmpty { "Unknown Number" }

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val simLabel = call.details.accountHandle?.let {
            try { telecomManager.getPhoneAccount(it)?.label?.toString() } catch (e: SecurityException) { null }
        }

        val intent = Intent(this, CallActivity::class.java) // Or Recents
        val pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed Call")
            .setContentText("Missed call from $contactName at $timeString${if (simLabel != null) " via $simLabel" else ""}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.RED)

        notificationManager.notify(number.hashCode(), builder.build())
    }

    private fun updateCallState() {
        val calls = getCalls()
        _allCalls.value = calls
        
        val activeCall = calls.find { it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING || it.state == Call.STATE_RINGING }
            ?: calls.firstOrNull()
            
        if (activeCall != null) {
            _currentCallSession.value = CallSession(activeCall, activeCall.state)
        } else {
            _currentCallSession.value = null
        }
    }

    private fun removeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this
        call.registerCallback(callCallback)
        updateCallState()
        
        updateNotification(call)

        val usePopup = preferenceManager.getBoolean(PreferenceManager.KEY_INCOMING_CALL_POPUP, false)
        
        if (!usePopup || call.state != Call.STATE_RINGING) {
            val intent = Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        updateCallState()
        if ((getCalls()?.size ?: 0) == 0) {
            removeForeground()
            cancelNotification()
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        _audioState.value = audioState
        _currentCallSession.value?.call?.let { updateNotification(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ANSWER_CALL" -> answerCall()
            "DECLINE_CALL" -> declineCall()
            "TOGGLE_MUTE" -> toggleMute()
            "TOGGLE_SPEAKER" -> toggleSpeaker()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(call: Call) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

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
            else -> "Unknown Number"
        }

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val accountHandle = call.details.accountHandle
        val simLabel = accountHandle?.let {
            try {
                telecomManager.getPhoneAccount(it)?.label?.toString()
            } catch (e: SecurityException) { null }
        }
        
        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val answerIntent = Intent(this, CallService::class.java).apply { action = "ANSWER_CALL" }
        val answerPendingIntent = PendingIntent.getService(this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, CallService::class.java).apply { action = "DECLINE_CALL" }
        val declinePendingIntent = PendingIntent.getService(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val muteIntent = Intent(this, CallService::class.java).apply { action = "TOGGLE_MUTE" }
        val mutePendingIntent = PendingIntent.getService(this, 3, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val speakerIntent = Intent(this, CallService::class.java).apply { action = "TOGGLE_SPEAKER" }
        val speakerPendingIntent = PendingIntent.getService(this, 4, speakerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val person = androidx.core.app.Person.Builder()
            .setName(contactName)
            .setImportant(true)
            .build()

        val isMuted = _audioState.value?.isMuted ?: false
        val isSpeaker = _audioState.value?.route == CallAudioState.ROUTE_SPEAKER

        val contentText = buildString {
            if (call.state == Call.STATE_RINGING) append("Incoming call") else append("Active call")
            if (!simLabel.isNullOrEmpty()) append(" via $simLabel")
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contactName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSilent(call.state != Call.STATE_RINGING)
            .setOnlyAlertOnce(true)
            .setDefaults(if (call.state == Call.STATE_RINGING) NotificationCompat.DEFAULT_ALL else 0)
            .setStyle(
                if (call.state == Call.STATE_RINGING) {
                    NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent)
                } else {
                    NotificationCompat.CallStyle.forOngoingCall(person, declinePendingIntent)
                }
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.stat_sys_speakerphone,
                    if (isSpeaker) "Handset" else "Speaker",
                    speakerPendingIntent
                ).build()
            )
            .setColor(Color.parseColor("#4CAF50")) // Changed to Green for better visibility
            .setColorized(true)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
