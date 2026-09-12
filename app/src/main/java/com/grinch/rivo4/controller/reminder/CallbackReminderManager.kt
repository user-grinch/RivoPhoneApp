package com.grinch.rivo4.controller.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.grinch.rivo4.modal.db.CallbackReminderDao
import com.grinch.rivo4.modal.db.CallbackReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallbackReminderManager(
    private val context: Context,
    private val callbackReminderDao: CallbackReminderDao
) {

    suspend fun scheduleReminder(
        phoneNumber: String,
        contactName: String?,
        delayMinutes: Long,
        note: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val remindAt = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        val entity = CallbackReminderEntity(
            phoneNumber = phoneNumber,
            contactName = contactName,
            remindAtMillis = remindAt,
            note = note
        )
        val reminderId = callbackReminderDao.insertReminder(entity)
        scheduleAlarm(reminderId, phoneNumber, contactName, remindAt, note)
        reminderId
    }

    private fun scheduleAlarm(
        reminderId: Long,
        phoneNumber: String,
        contactName: String?,
        remindAtMillis: Long,
        note: String?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, CallbackReminderReceiver::class.java).apply {
            putExtra(CallbackReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(CallbackReminderReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(CallbackReminderReceiver.EXTRA_CONTACT_NAME, contactName)
            putExtra(CallbackReminderReceiver.EXTRA_NOTE, note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder alarm for ID $reminderId at $remindAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing, falling back to inexact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
        }
    }

    suspend fun cancelReminder(reminderId: Long) = withContext(Dispatchers.IO) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, CallbackReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        callbackReminderDao.deleteReminderById(reminderId)
    }

    companion object {
        private const val TAG = "CallbackReminderManager"
    }
}
