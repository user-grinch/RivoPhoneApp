package com.grinch.rivo4.controller.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast

class VolumeSqueezeHelper(private val context: Context, private val preferenceManager: PreferenceManager) {

    private var lastVolumeUpTime = 0L
    private var lastVolumeDownTime = 0L
    private var lastTriggerTime = 0L

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!preferenceManager.getBoolean(PreferenceManager.KEY_VOLUME_SQUEEZE_DND, false)) {
            return false
        }

        if (event.action != KeyEvent.ACTION_DOWN) return false

        val now = System.currentTimeMillis()

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                lastVolumeUpTime = now
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                lastVolumeDownTime = now
            }
            else -> return false
        }

        // Check if both keys were pressed within 350ms of each other and haven't triggered in the last 1.5s
        if (Math.abs(lastVolumeUpTime - lastVolumeDownTime) < 350 && (now - lastTriggerTime > 1500)) {
            lastTriggerTime = now
            lastVolumeUpTime = 0L
            lastVolumeDownTime = 0L
            return toggleDnd()
        }

        return false
    }

    private fun toggleDnd(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Grant Do Not Disturb permission to use Volume Squeeze", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open Do Not Disturb settings", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            val current = notificationManager.currentInterruptionFilter
            val isCurrentlyDnd = current != NotificationManager.INTERRUPTION_FILTER_ALL

            try {
                if (isCurrentlyDnd) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    Toast.makeText(context, "Do Not Disturb: OFF", Toast.LENGTH_SHORT).show()
                } else {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    Toast.makeText(context, "Do Not Disturb: ON", Toast.LENGTH_SHORT).show()
                }
                vibrateFeedback()
                return true
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to toggle Do Not Disturb", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    private fun vibrateFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70L)
            }
        } catch (e: Exception) {
        }
    }
}
