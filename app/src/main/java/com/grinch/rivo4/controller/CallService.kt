package com.grinch.rivo4.controller

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
import android.provider.BlockedNumberContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.screen.CallActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

data class CallSession(
    val call: Call,
    val state: Int,
    val updateTime: Long = System.currentTimeMillis()
)

class CallService : InCallService() {

    private val contactsRepository: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var redialCount = 0

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
        private const val NOTIFICATION_ID = 101

        private val _currentCallSession = MutableStateFlow<CallSession?>(null)
        val currentCallSession = _currentCallSession.asStateFlow()

        private val _allCalls = MutableStateFlow<List<Call>>(emptyList())
        val allCalls = _allCalls.asStateFlow()

        private val _audioState = MutableStateFlow<CallAudioState?>(null)
        val audioState = _audioState.asStateFlow()

        val isActivityVisible = MutableStateFlow(false)

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
            
            if (state == Call.STATE_ACTIVE) {
                redialCount = 0
            }

            if (state == Call.STATE_DISCONNECTED) {
                val cause = call.details.disconnectCause
                handleDisconnect(call, cause)

                if ((instance?.getCalls()?.size ?: 0) == 0) {
                    removeForeground()
                    cancelNotification()
                }
            } else {
                updateNotification(call)
            }
        }
    }

    private fun handleDisconnect(call: Call, cause: DisconnectCause?) {
        val number = call.details.handle?.schemeSpecificPart ?: ""
        
        // Auto Redial on Busy
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

        // Missed Call Notification
        if (call.state == Call.STATE_RINGING || (cause != null && cause.code == DisconnectCause.MISSED)) {
            if (!isNumberBlocked(number) || preferenceManager.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0) == 1) {
                showMissedCallNotification(call)
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
        val method = preferenceManager.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0) // 0: Decline, 1: Silent
        
        if (method == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                call.reject(Call.REJECT_REASON_DECLINED)
            } else {
                call.disconnect()
            }
        }
        
        if (preferenceManager.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) {
            showBlockedNotification(number)
        }
    }

    private fun showBlockedNotification(number: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle("Blocked Call")
            .setContentText("Blocked call from $number")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        
        notificationManager.notify(number.hashCode(), builder.build())
    }

    private fun showMissedCallNotification(call: Call) {
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
        
        val contactName = contact?.name ?: number.ifEmpty { "Unknown Number" }
        val contactPhoto = getContactBitmap(contact?.photoUri)

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val simLabel = call.details.accountHandle?.let {
            try { telecomManager.getPhoneAccount(it)?.label?.toString() } catch (e: SecurityException) { null }
        }

        val intent = Intent(this, com.grinch.rivo4.MainActivity::class.java).apply {
            action = "com.grinch.rivo4.ACTION_VIEW_RECENTS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed Call")
            .setContentText("Missed call from $contactName at $timeString${if (simLabel != null) " via $simLabel" else ""}")
            .setLargeIcon(contactPhoto)
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

        val number = call.details.handle?.schemeSpecificPart ?: ""
        if (isNumberBlocked(number)) {
            handleBlockedCall(call, number)
            return
        }

        updateCallState()
        updateNotification(call)

        serviceScope.launch {
            isActivityVisible.collect {
                _currentCallSession.value?.call?.let { currentCall ->
                    updateNotification(currentCall)
                }
            }
        }

        // If it's an incoming call, we rely on setFullScreenIntent in the notification.
        // Android will automatically decide:
        // - If the device is locked: launch the full-screen activity.
        // - If the user is actively using the device: show a heads-up notification.
        // This satisfies the requirement: "If the user is actively using device only then the full screen intent wont come but notification will"
        if (call.state != Call.STATE_RINGING) {
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
        
        val contactPhoto = getContactBitmap(contact?.photoUri)

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

        val speakerIntent = Intent(this, CallService::class.java).apply { action = "TOGGLE_SPEAKER" }
        val speakerPendingIntent = PendingIntent.getService(this, 4, speakerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val personBuilder = androidx.core.app.Person.Builder()
            .setName(contactName)
            .setImportant(true)
        
        if (contactPhoto != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(contactPhoto))
        }
        val person = personBuilder.build()

        val isSpeaker = _audioState.value?.route == CallAudioState.ROUTE_SPEAKER

        val contentText = buildString {
            if (call.state == Call.STATE_RINGING) append("Incoming call") else append("Active call")
            if (!simLabel.isNullOrEmpty()) append(" via $simLabel")
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contactName)
            .setContentText(contentText)
            .setPriority(if (isActivityVisible.value) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, !isActivityVisible.value)
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

        if (call.state != Call.STATE_RINGING) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.stat_sys_speakerphone,
                    if (isSpeaker) "Handset" else "Speaker",
                    speakerPendingIntent
                ).build()
            )
        }

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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
