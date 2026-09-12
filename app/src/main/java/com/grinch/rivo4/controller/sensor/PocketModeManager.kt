package com.grinch.rivo4.controller.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class PocketModeManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isListening = false
    private var onProximityStateChanged: ((isNear: Boolean) -> Unit)? = null

    fun startListening(onStateChanged: (isNear: Boolean) -> Unit) {
        if (isListening || sensorManager == null || proximitySensor == null) return
        this.onProximityStateChanged = onStateChanged
        isListening = true

        try {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "PocketModeManager: Started listening to proximity sensor")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register proximity listener: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        onProximityStateChanged = null
        try {
            sensorManager?.unregisterListener(this)
            Log.d(TAG, "PocketModeManager: Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering proximity listener: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening || event == null) return

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values.firstOrNull() ?: return
            val maxRange = event.sensor.maximumRange
            // Some sensors report 0 for near and maxRange for far, others report distance in cm
            val isNear = distance < 4.0f && distance < maxRange
            onProximityStateChanged?.invoke(isNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "PocketModeManager"
    }
}
