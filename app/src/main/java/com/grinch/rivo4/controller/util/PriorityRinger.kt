package com.grinch.rivo4.controller.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

object PriorityRinger {
    private const val TAG = "PriorityRinger"
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalAlarmVolume: Int? = null
    private var wasAlarmMuted: Boolean = false
    private var originalInterruptionFilter: Int? = null
    private var appContext: Context? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { _ ->
        synchronized(this) {
            try {
                mediaPlayer?.let { mp ->
                    mp.setVolume(1.0f, 1.0f)
                    if (!mp.isPlaying) {
                        mp.start()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error maintaining priority ringer playback on focus change: ${e.message}")
            }
        }
    }

    @Synchronized
    fun startRinging(context: Context, customRingtoneUri: String? = null) {
        if (ringtone != null || mediaPlayer != null) return // Already ringing
        appContext = context.applicationContext

        // 1. Start vibration independently so vibration always occurs even if audio fails
        startVibration(context)

        // 2. Start audio playback on alarm stream
        startAudio(context, customRingtoneUri)
    }

    private fun startVibration(context: Context) {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vib.hasVibrator()) return
            vibrator = vib

            val pattern = longArrayOf(0, 1000, 1000)
            val effect = VibrationEffect.createWaveform(pattern, 0)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            vib.vibrate(effect, audioAttributes)
            Log.d(TAG, "Priority ringer vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting priority ringer vibration", e)
        }
    }

    private fun startAudio(context: Context, customRingtoneUri: String?) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                notificationManager != null &&
                notificationManager.isNotificationPolicyAccessGranted
            ) {
                try {
                    val currentFilter = notificationManager.currentInterruptionFilter
                    if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                        originalInterruptionFilter = currentFilter
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        Log.d(TAG, "Temporarily lifted DND filter from $currentFilter for priority call")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not adjust DND interruption filter: ${e.message}")
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (audioManager != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager.isStreamMute(AudioManager.STREAM_ALARM)) {
                        wasAlarmMuted = true
                        audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                    }
                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    if (currentVol <= 0 && maxVol > 0) {
                        originalAlarmVolume = currentVol
                        val targetVol = (maxVol * 0.8f).toInt().coerceAtLeast(1)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                        Log.d(TAG, "Temporarily boosted alarm stream volume to $targetVol")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not adjust alarm volume: ${e.message}")
                }

                try {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build()
                    audioFocusRequest = focusRequest
                    audioManager.requestAudioFocus(focusRequest)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not request audio focus: ${e.message}")
                }
            }

            val candidateUris = linkedSetOf<Uri>()
            if (!customRingtoneUri.isNullOrBlank()) {
                try { candidateUris.add(Uri.parse(customRingtoneUri)) } catch (_: Exception) {}
            }
            candidateUris.add(Settings.System.DEFAULT_RINGTONE_URI)
            try {
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)?.let {
                    candidateUris.add(it)
                }
            } catch (_: Exception) {}
            try {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let {
                    candidateUris.add(it)
                }
            } catch (_: Exception) {}
            candidateUris.add(Settings.System.DEFAULT_ALARM_ALERT_URI)
            try {
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)?.let {
                    candidateUris.add(it)
                }
            } catch (_: Exception) {}
            try {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let {
                    candidateUris.add(it)
                }
            } catch (_: Exception) {}
            candidateUris.add(Settings.System.DEFAULT_NOTIFICATION_URI)

            var started = false
            for (uri in candidateUris) {
                var mp: MediaPlayer? = null
                try {
                    mp = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(context, uri)
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                    started = true
                    Log.d(TAG, "Priority ringer playing via MediaPlayer with uri: $uri")
                    break
                } catch (e: Exception) {
                    try { mp?.release() } catch (_: Exception) {}
                    Log.w(TAG, "Failed MediaPlayer with uri $uri: ${e.message}")
                }
            }

            if (!started) {
                for (uri in candidateUris) {
                    try {
                        val r = RingtoneManager.getRingtone(context, uri)
                        if (r != null) {
                            r.audioAttributes = audioAttributes
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                r.isLooping = true
                                r.volume = 1.0f
                            }
                            r.play()
                            ringtone = r
                            Log.d(TAG, "Priority ringer playing via Ringtone fallback with uri: $uri")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed Ringtone fallback with uri $uri: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting priority ringer audio", e)
        }
    }

    @Synchronized
    fun stopRinging() {
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone", e)
        } finally {
            ringtone = null
        }

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        } finally {
            vibrator = null
        }

        val ctx = appContext
        if (ctx != null) {
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                audioFocusRequest?.let { req ->
                    try {
                        audioManager.abandonAudioFocusRequest(req)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error abandoning audio focus", e)
                    }
                }

                originalAlarmVolume?.let { origVol ->
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, origVol, 0)
                        Log.d(TAG, "Restored original alarm volume to $origVol")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring alarm volume", e)
                    }
                }

                if (wasAlarmMuted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring alarm mute state", e)
                    }
                }
            }

            originalInterruptionFilter?.let { origFilter ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                            notificationManager.setInterruptionFilter(origFilter)
                            Log.d(TAG, "Restored original DND filter to $origFilter")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring DND interruption filter", e)
                    }
                }
            }
        }

        audioFocusRequest = null
        originalAlarmVolume = null
        wasAlarmMuted = false
        originalInterruptionFilter = null
        appContext = null
        Log.d(TAG, "Priority ringer stopped")
    }
}
