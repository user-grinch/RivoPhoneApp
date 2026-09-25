package com.grinch.rivo4.controller.fakecall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.grinch.rivo4.R

object FakeCallNotificationManager {

    private const val TAG = "FakeCallNotificationMgr"
    const val INCOMING_CHANNEL_ID = "rivo_fake_call_active_channel"
    const val SILENT_CHANNEL_ID = "rivo_fake_call_silent_channel"
    const val NOTIFICATION_ID = 8882

    fun ensureChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val channel = NotificationChannel(
            INCOMING_CHANNEL_ID,
            context.getString(R.string.fake_call_incoming_channel_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.fake_call_incoming_channel_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
            setSound(
                ringtoneUri,
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
            )
        }
        notificationManager.createNotificationChannel(channel)

        val silentChannel = NotificationChannel(
            SILENT_CHANNEL_ID,
            context.getString(R.string.fake_call_incoming_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.fake_call_incoming_channel_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(silentChannel)
    }

    fun showIncomingCallNotification(
        context: Context,
        scheduleId: String,
        callerName: String,
        phoneNumber: String,
        photoUri: String?,
        vibrate: Boolean,
        silentBackground: Boolean = false
    ) {
        ensureChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tapping the notification body opens the ringing call screen
        val contentIntent = Intent(context, FakeCallActivity::class.java).apply {
            putExtra(FakeCallManager.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(FakeCallManager.EXTRA_NAME, callerName)
            putExtra(FakeCallManager.EXTRA_NUMBER, phoneNumber)
            putExtra(FakeCallManager.EXTRA_PHOTO_URI, photoUri)
            putExtra(FakeCallManager.EXTRA_VIBRATE, vibrate)
            putExtra(FakeCallManager.EXTRA_START_ANSWERED, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tapping Answer in the notification launches FakeCallActivity directly answered
        val answerIntent = Intent(context, FakeCallActivity::class.java).apply {
            putExtra(FakeCallManager.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(FakeCallManager.EXTRA_NAME, callerName)
            putExtra(FakeCallManager.EXTRA_NUMBER, phoneNumber)
            putExtra(FakeCallManager.EXTRA_PHOTO_URI, photoUri)
            putExtra(FakeCallManager.EXTRA_VIBRATE, vibrate)
            putExtra(FakeCallManager.EXTRA_START_ANSWERED, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            102,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tapping Decline dismisses notification and terminates ringing
        val declineIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = FakeCallManager.ACTION_DECLINE_FAKE_CALL
            putExtra(FakeCallManager.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val photoBitmap = loadPhotoBitmap(context, photoUri)
        val avatarBitmap = com.grinch.rivo4.controller.util.CallNotificationHelper.getAvatarBitmap(context, callerName, photoBitmap)
        val notifColor = com.grinch.rivo4.controller.util.CallNotificationHelper.getNotificationColor(context)

        val personBuilder = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .setBot(false)
            .setIcon(IconCompat.createWithBitmap(avatarBitmap))

        if (phoneNumber.isNotBlank()) {
            personBuilder.setUri("tel:")
            personBuilder.setKey(phoneNumber)
        }
        val person = personBuilder.build()

        val declineAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            context.getString(R.string.action_decline),
            declinePendingIntent
        ).build()

        val answerAction = NotificationCompat.Action.Builder(
            android.R.drawable.sym_action_call,
            context.getString(R.string.action_answer),
            answerPendingIntent
        ).build()

        val targetChannel = if (silentBackground) SILENT_CHANNEL_ID else INCOMING_CHANNEL_ID

        // 1. Try CallStyle with explicit action buttons
        try {
            val builder = NotificationCompat.Builder(context, targetChannel)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(callerName)
                .setContentText(context.getString(R.string.call_status_incoming))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setTimeoutAfter(45_000L)
                .setFullScreenIntent(contentPendingIntent, true)
                .setColorized(true)
                .setColor(notifColor)
                .setLargeIcon(avatarBitmap)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent))

            if (silentBackground) {
                // When full-screen call UI is displayed, keep notification silent in background
                builder.setPriority(NotificationCompat.PRIORITY_LOW)
                builder.setSilent(true)
                builder.setOnlyAlertOnce(true)
            } else {
                // Heads-Up Mode: show prominent floating notification banner
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setSilent(false)
            }

            if (photoBitmap != null) {
                builder.setLargeIcon(photoBitmap)
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
            return
        } catch (e: Throwable) {
            Log.w(TAG, "CallStyle incoming notification failed: ${e.message}, falling back to standard notification", e)
        }

        // Fallback: standard notification with action buttons
        try {
            val fallbackBuilder = NotificationCompat.Builder(context, targetChannel)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(callerName)
                .setContentText(context.getString(R.string.call_status_incoming))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setTimeoutAfter(45_000L)
                .addAction(declineAction)
                .addAction(answerAction)

            if (silentBackground) {
                fallbackBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
                fallbackBuilder.setSilent(true)
                fallbackBuilder.setOnlyAlertOnce(true)
            } else {
                fallbackBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                fallbackBuilder.setSilent(false)
            }

            if (photoBitmap != null) {
                fallbackBuilder.setLargeIcon(photoBitmap)
            }

            notificationManager.notify(NOTIFICATION_ID, fallbackBuilder.build())
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback incoming notification failed: ${e.message}", e)
        }
    }

    fun showOngoingCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String,
        photoUri: String?,
        connectTimeMillis: Long,
        isMuted: Boolean = false,
        isSpeakerOn: Boolean = false
    ) {
        ensureChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Returning to active call if user leaves
        val returnIntent = Intent(context, FakeCallActivity::class.java).apply {
            putExtra(FakeCallManager.EXTRA_NAME, callerName)
            putExtra(FakeCallManager.EXTRA_NUMBER, phoneNumber)
            putExtra(FakeCallManager.EXTRA_PHOTO_URI, photoUri)
            putExtra(FakeCallManager.EXTRA_START_ANSWERED, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val returnPendingIntent = PendingIntent.getActivity(
            context,
            104,
            returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End Call action
        val endIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = FakeCallManager.ACTION_DECLINE_FAKE_CALL
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            105,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute action
        val muteIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = FakeCallManager.ACTION_MUTE_FAKE_CALL
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            106,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Speaker action
        val speakerIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = FakeCallManager.ACTION_SPEAKER_FAKE_CALL
        }
        val speakerPendingIntent = PendingIntent.getBroadcast(
            context,
            107,
            speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val photoBitmap = loadPhotoBitmap(context, photoUri)
        val avatarBitmap = com.grinch.rivo4.controller.util.CallNotificationHelper.getAvatarBitmap(context, callerName, photoBitmap)
        val notifColor = com.grinch.rivo4.controller.util.CallNotificationHelper.getNotificationColor(context)

        val personBuilder = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .setBot(false)
            .setIcon(IconCompat.createWithBitmap(avatarBitmap))

        if (phoneNumber.isNotBlank()) {
            personBuilder.setUri("tel:")
            personBuilder.setKey(phoneNumber)
        }
        val person = personBuilder.build()

        val hangupAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            context.getString(R.string.action_end_call),
            endPendingIntent
        ).build()

        val muteAction = NotificationCompat.Action.Builder(
            android.R.drawable.stat_notify_call_mute,
            if (isMuted) context.getString(R.string.action_unmute) else context.getString(R.string.action_mute),
            mutePendingIntent
        ).build()

        val speakerLabel = if (isSpeakerOn) context.getString(R.string.audio_route_speaker) else context.getString(R.string.audio_route_handset)
        val speakerAction = NotificationCompat.Action.Builder(
            android.R.drawable.stat_sys_speakerphone,
            speakerLabel,
            speakerPendingIntent
        ).build()

        val effectiveTime = if (connectTimeMillis > 0) connectTimeMillis else System.currentTimeMillis()

        // 1. Try CallStyle with explicit action buttons (Hangup, Mute, Speaker)
        try {
            val builder = NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_ongoing)
                .setContentTitle(callerName)
                .setContentText(context.getString(R.string.notif_active_call))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(returnPendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setWhen(effectiveTime)
                .setUsesChronometer(true)
                .setFullScreenIntent(returnPendingIntent, false)
                .setColorized(true)
                .setColor(notifColor)
                .setLargeIcon(avatarBitmap)
                .addAction(muteAction)
                .addAction(speakerAction)
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, endPendingIntent))

            if (photoBitmap != null) {
                builder.setLargeIcon(photoBitmap)
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
            return
        } catch (e: Throwable) {
            Log.w(TAG, "CallStyle ongoing notification failed: ${e.message}, falling back to standard notification", e)
        }

        // Fallback: standard notification with ongoing timer and action buttons
        try {
            val fallbackBuilder = NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_ongoing)
                .setContentTitle(callerName)
                .setContentText(context.getString(R.string.notif_active_call))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(returnPendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setWhen(effectiveTime)
                .setUsesChronometer(true)
                .addAction(hangupAction)
                .addAction(muteAction)
                .addAction(speakerAction)

            if (photoBitmap != null) {
                fallbackBuilder.setLargeIcon(photoBitmap)
            }

            notificationManager.notify(NOTIFICATION_ID, fallbackBuilder.build())
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback ongoing notification failed: ${e.message}", e)
        }
    }

    fun cancelNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification: ${e.message}")
        }
    }

    private fun loadPhotoBitmap(context: Context, photoUriStr: String?): Bitmap? {
        if (photoUriStr.isNullOrEmpty()) return null
        return try {
            val uri = Uri.parse(photoUriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
