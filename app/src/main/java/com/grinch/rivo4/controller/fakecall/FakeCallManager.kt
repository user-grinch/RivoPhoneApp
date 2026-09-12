package com.grinch.rivo4.controller.fakecall

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FakeCallSchedule(
    val id: String = UUID.randomUUID().toString(),
    val callerName: String = "Mom",
    val phoneNumber: String = "+1 (555) 019-2834",
    val photoUri: String? = null,
    val triggerTimestampMillis: Long,
    val vibrate: Boolean = true
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("callerName", callerName)
        put("phoneNumber", phoneNumber)
        put("photoUri", photoUri ?: "")
        put("triggerTimestampMillis", triggerTimestampMillis)
        put("vibrate", vibrate)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FakeCallSchedule {
            val photo = json.optString("photoUri", "")
            return FakeCallSchedule(
                id = json.optString("id", UUID.randomUUID().toString()),
                callerName = json.optString("callerName", "Mom"),
                phoneNumber = json.optString("phoneNumber", "+1 (555) 019-2834"),
                photoUri = if (photo.isNotEmpty()) photo else null,
                triggerTimestampMillis = json.optLong("triggerTimestampMillis", 0L),
                vibrate = json.optBoolean("vibrate", true)
            )
        }
    }
}

object FakeCallManager {

    private const val TAG = "FakeCallManager"
    private const val PREFS_NAME = "rivo_fake_call_prefs"
    private const val KEY_SCHEDULES_JSON = "fake_schedules_json"

    const val ACTION_TRIGGER_FAKE_CALL = "com.grinch.rivo4.ACTION_TRIGGER_FAKE_CALL"
    const val ACTION_CANCEL_FAKE_CALL = "com.grinch.rivo4.ACTION_CANCEL_FAKE_CALL"
    const val ACTION_ANSWER_FAKE_CALL = "com.grinch.rivo4.ACTION_ANSWER_FAKE_CALL"
    const val ACTION_DECLINE_FAKE_CALL = "com.grinch.rivo4.ACTION_DECLINE_FAKE_CALL"
    const val ACTION_MUTE_FAKE_CALL = "com.grinch.rivo4.ACTION_MUTE_FAKE_CALL"
    const val ACTION_SPEAKER_FAKE_CALL = "com.grinch.rivo4.ACTION_SPEAKER_FAKE_CALL"

    const val EXTRA_SCHEDULE_ID = "extra_fake_schedule_id"
    const val EXTRA_NAME = "extra_fake_caller_name"
    const val EXTRA_NUMBER = "extra_fake_caller_number"
    const val EXTRA_PHOTO_URI = "extra_fake_caller_photo"
    const val EXTRA_VIBRATE = "extra_fake_caller_vibrate"
    const val EXTRA_START_ANSWERED = "extra_fake_start_answered"

    private const val CHANNEL_ID = "fake_call_schedule_channel"
    private const val NOTIFICATION_ID = 8881

    private val _schedules = MutableStateFlow<List<FakeCallSchedule>>(emptyList())
    val schedules = _schedules.asStateFlow()

    fun init(context: Context) {
        val list = loadSchedulesFromPrefs(context)
        val now = System.currentTimeMillis()
        val validList = list.filter { it.triggerTimestampMillis > now }.sortedBy { it.triggerTimestampMillis }
        _schedules.value = validList
        saveSchedulesToPrefs(context, validList)
        updateScheduleNotification(context)
    }

    fun shouldVibrateOnRing(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> false
            AudioManager.RINGER_MODE_VIBRATE -> true
            AudioManager.RINGER_MODE_NORMAL -> {
                try {
                    Settings.System.getInt(context.contentResolver, "vibrate_when_ringing", 1) != 0
                } catch (e: Exception) {
                    true
                }
            }
            else -> true
        }
    }

    fun scheduleCall(context: Context, schedule: FakeCallSchedule): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = schedule.triggerTimestampMillis

        if (triggerAtMillis <= System.currentTimeMillis()) {
            triggerCallNow(context, schedule)
            return true
        }

        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            action = ACTION_TRIGGER_FAKE_CALL
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_NAME, schedule.callerName)
            putExtra(EXTRA_NUMBER, schedule.phoneNumber)
            putExtra(EXTRA_PHOTO_URI, schedule.photoUri)
            putExtra(EXTRA_VIBRATE, schedule.vibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }

            val currentList = _schedules.value.filter { it.id != schedule.id }.toMutableList()
            currentList.add(schedule)
            val sortedList = currentList.sortedBy { it.triggerTimestampMillis }
            _schedules.value = sortedList
            saveSchedulesToPrefs(context, sortedList)
            updateScheduleNotification(context)

            Log.i(TAG, "Scheduled fake call id=${schedule.id} from ${schedule.callerName} at $triggerAtMillis")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule fake call alarm: ${e.message}", e)
            return false
        }
    }

    fun removeSchedule(context: Context, scheduleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            action = ACTION_TRIGGER_FAKE_CALL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        val updated = _schedules.value.filter { it.id != scheduleId }
        _schedules.value = updated
        saveSchedulesToPrefs(context, updated)
        updateScheduleNotification(context)
        Log.i(TAG, "Cancelled fake call schedule $scheduleId")
    }

    fun clearAllSchedules(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        for (item in _schedules.value) {
            val intent = Intent(context, FakeCallReceiver::class.java).apply {
                action = ACTION_TRIGGER_FAKE_CALL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        _schedules.value = emptyList()
        saveSchedulesToPrefs(context, emptyList())
        cancelScheduleNotification(context)
        Log.i(TAG, "Cleared all scheduled fake calls")
    }

    fun onScheduleTriggered(context: Context, scheduleId: String) {
        val updated = _schedules.value.filter { it.id != scheduleId }
        _schedules.value = updated
        saveSchedulesToPrefs(context, updated)
        updateScheduleNotification(context)
    }

    fun triggerCallNow(context: Context, schedule: FakeCallSchedule) {
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            action = ACTION_TRIGGER_FAKE_CALL
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_NAME, schedule.callerName)
            putExtra(EXTRA_NUMBER, schedule.phoneNumber)
            putExtra(EXTRA_PHOTO_URI, schedule.photoUri)
            putExtra(EXTRA_VIBRATE, schedule.vibrate)
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send fake call broadcast: ${e.message}", e)
        }
    }

    private fun loadSchedulesFromPrefs(context: Context): List<FakeCallSchedule> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SCHEDULES_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FakeCallSchedule>()
            for (i in 0 until array.length()) {
                list.add(FakeCallSchedule.fromJsonObject(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing schedules JSON: ${e.message}")
            emptyList()
        }
    }

    private fun saveSchedulesToPrefs(context: Context, list: List<FakeCallSchedule>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { array.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_SCHEDULES_JSON, array.toString()).apply()
    }

    private fun updateScheduleNotification(context: Context) {
        val next = _schedules.value.firstOrNull()
        if (next == null) {
            cancelScheduleNotification(context)
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.fake_call_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.fake_call_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)

        val cancelIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = ACTION_CANCEL_FAKE_CALL
            putExtra(EXTRA_SCHEDULE_ID, next.id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            next.id.hashCode() + 1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(next.triggerTimestampMillis))
        val count = _schedules.value.size
        val title = if (count > 1) {
            "${context.getString(R.string.fake_call_notif_title)} ($count)"
        } else {
            context.getString(R.string.fake_call_notif_title)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.fake_call_notif_content, next.callerName, timeStr))
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_cancel),
                cancelPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelScheduleNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
