package com.grinch.rivo4.controller.util

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
    private var appContext: Context? = null

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
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
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
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            if (audioManager != null) {
                try {
                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    if (currentVol == 0 && maxVol > 0) {
                        originalAlarmVolume = 0
                        val targetVol = (maxVol * 0.75f).toInt().coerceAtLeast(1)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                        Log.d(TAG, "Temporarily boosted alarm stream volume to $targetVol")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not adjust alarm volume: ${e.message}")
                }

                try {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(audioAttributes)
                        .build()
                    audioFocusRequest = focusRequest
                    audioManager.requestAudioFocus(focusRequest)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not request audio focus: ${e.message}")
                }
            }

            // Build candidate URIs in priority order
            val candidateUris = mutableListOf<Uri>()
            if (!customRingtoneUri.isNullOrBlank()) {
                try { candidateUris.add(Uri.parse(customRingtoneUri)) } catch (_: Exception) {}
            }
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
            try {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let {
                    candidateUris.add(it)
                }
            } catch (_: Exception) {}
            candidateUris.add(Settings.System.DEFAULT_RINGTONE_URI)
            candidateUris.add(Settings.System.DEFAULT_ALARM_ALERT_URI)

            // 1. Try RingtoneManager first: uses IRingtonePlayer IPC in system_server,
            // which has permissions to play ringtone content URIs that MediaPlayer cannot access.
            var started = false
            for (uri in candidateUris) {
                try {
                    val r = RingtoneManager.getRingtone(context, uri)
                    if (r != null) {
                        r.audioAttributes = audioAttributes
                        r.isLooping = true
                        r.play()
                        ringtone = r
                        started = true
                        Log.d(TAG, "Priority ringer playing via Ringtone with uri: $uri")
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed Ringtone with uri $uri: ${e.message}")
                }
            }

            // 2. Fallback to MediaPlayer if RingtoneManager did not start
            if (!started) {
                for (uri in candidateUris) {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(context, uri)
                            setAudioAttributes(audioAttributes)
                            isLooping = true
                            prepare()
                            start()
                        }
                        mediaPlayer = mp
                        Log.d(TAG, "Priority ringer playing via MediaPlayer with uri: $uri")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed MediaPlayer with uri $uri: ${e.message}")
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
            }
        }

        audioFocusRequest = null
        originalAlarmVolume = null
        appContext = null
        Log.d(TAG, "Priority ringer stopped")
    }
}
