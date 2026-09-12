package com.grinch.rivo4.controller.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.TelecomManager
import android.util.Log

class FlipToSilenceManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private var isListening = false
    private var hasBeenNonFaceDown = false
    private var isNear = false
    private var onSilenceCallback: (() -> Unit)? = null

    fun startListening(onSilenced: (() -> Unit)? = null) {
        if (isListening || sensorManager == null || accelerometer == null) return
        this.onSilenceCallback = onSilenced
        hasBeenNonFaceDown = false
        isNear = false
        isListening = true

        try {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            Log.d(TAG, "FlipToSilenceManager: Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register sensor listeners: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        hasBeenNonFaceDown = false
        onSilenceCallback = null
        try {
            sensorManager?.unregisterListener(this)
            Log.d(TAG, "FlipToSilenceManager: Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering sensor listeners: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening || event == null) return

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values.firstOrNull() ?: return
            val maxRange = event.sensor.maximumRange
            isNear = distance < 4.0f && distance < maxRange
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // If phone is not face-down (e.g. face-up or upright), record that it was in a neutral/upright state
            // Earth gravity is ~9.8 m/s^2. Face down means z is negative (~ -9.8 m/s^2).
            if (z > -5.0f) {
                hasBeenNonFaceDown = true
            }

            // Detect flip face-down:
            // z should be strongly negative (< -7.5f) and screen relatively parallel to table (|x| < 4.5f, |y| < 4.5f)
            val isFaceDown = z < -7.5f && kotlin.math.abs(x) < 4.5f && kotlin.math.abs(y) < 4.5f

            if (hasBeenNonFaceDown && isFaceDown) {
                Log.i(TAG, "Flip-to-silence triggered! z=$z, x=$x, y=$y")
                triggerSilence()
            }
        }
    }

    private fun triggerSilence() {
        stopListening()

        // Silence the incoming call ringer
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telecomManager?.silenceRinger()
            }
        } catch (e: Exception) {
            Log.e(TAG, "telecomManager.silenceRinger() failed: ${e.message}")
        }

        // Haptic feedback tick to confirm gesture
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50L)
            }
        } catch (e: Exception) {
            // Ignore vibration exception
        }

        onSilenceCallback?.invoke()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "FlipToSilenceManager"
    }
}
