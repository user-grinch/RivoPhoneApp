package com.grinch.rivo4.controller.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.db.CallbackReminderDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CallbackReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val callbackReminderDao: CallbackReminderDao by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: phoneNumber
        val note = intent.getStringExtra(EXTRA_NOTE)

        if (action == ACTION_DISMISS) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(reminderId.toInt())
            if (reminderId != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    callbackReminderDao.markCompleted(reminderId)
                }
            }
            return
        }

        // Show notification
        showReminderNotification(context, reminderId, phoneNumber, contactName, note)
    }

    private fun showReminderNotification(
        context: Context,
        reminderId: Long,
        phoneNumber: String,
        contactName: String,
        note: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Back Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications reminding you to call back contacts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Call Action
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt() * 10 + 1,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, CallbackReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() * 10 + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (!note.isNullOrBlank()) {
            "Time to call back $contactName: \"$note\""
        } else {
            "Time to call back $contactName ($phoneNumber)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Call Back Reminder")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, "Call Now", callPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        val notifId = if (reminderId > 0) reminderId.toInt() else phoneNumber.hashCode()
        notificationManager.notify(notifId, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "callback_reminders_channel"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_NOTE = "note"
        const val ACTION_DISMISS = "com.grinch.rivo4.ACTION_DISMISS_REMINDER"
    }
}
