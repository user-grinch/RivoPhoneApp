package com.grinch.rivo4.controller.fakecall

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.grinch.rivo4.controller.util.CallUiHelper
import com.grinch.rivo4.controller.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FakeCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "FakeCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received action: $action")

        when (action) {
            FakeCallManager.ACTION_TRIGGER_FAKE_CALL -> {
                val scheduleId = intent.getStringExtra(FakeCallManager.EXTRA_SCHEDULE_ID) ?: ""
                val callerName = intent.getStringExtra(FakeCallManager.EXTRA_NAME) ?: "Mom"
                val phoneNumber = intent.getStringExtra(FakeCallManager.EXTRA_NUMBER) ?: "+1 (555) 019-2834"
                val photoUri = intent.getStringExtra(FakeCallManager.EXTRA_PHOTO_URI)
                val vibrate = intent.getBooleanExtra(FakeCallManager.EXTRA_VIBRATE, true)

                if (scheduleId.isNotEmpty()) {
                    FakeCallManager.onScheduleTriggered(context, scheduleId)
                }

                val prefs = PreferenceManager(context)
                val showFullScreen = CallUiHelper.shouldShowFullScreen(context, prefs)

                if (showFullScreen) {
                    Log.i(TAG, "Full-screen mode active: launching FakeCallActivity and displaying silent background notification.")
                    FakeCallNotificationManager.showIncomingCallNotification(
                        context = context,
                        scheduleId = scheduleId,
                        callerName = callerName,
                        phoneNumber = phoneNumber,
                        photoUri = photoUri,
                        vibrate = vibrate,
                        silentBackground = true
                    )

                    val callIntent = Intent(context, FakeCallActivity::class.java).apply {
                        putExtra(FakeCallManager.EXTRA_SCHEDULE_ID, scheduleId)
                        putExtra(FakeCallManager.EXTRA_NAME, callerName)
                        putExtra(FakeCallManager.EXTRA_NUMBER, phoneNumber)
                        putExtra(FakeCallManager.EXTRA_PHOTO_URI, photoUri)
                        putExtra(FakeCallManager.EXTRA_VIBRATE, vibrate)
                        putExtra(FakeCallManager.EXTRA_START_ANSWERED, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }

                    try {
                        context.startActivity(callIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start FakeCallActivity from receiver: ${e.message}", e)
                    }
                } else {
                    Log.i(TAG, "User in another app: showing heads-up incoming fake call notification banner only.")
                    FakeCallNotificationManager.showIncomingCallNotification(
                        context = context,
                        scheduleId = scheduleId,
                        callerName = callerName,
                        phoneNumber = phoneNumber,
                        photoUri = photoUri,
                        vibrate = vibrate,
                        silentBackground = false
                    )
                }
            }

            FakeCallManager.ACTION_DECLINE_FAKE_CALL -> {
                Log.i(TAG, "Declining/ending fake call from notification action")
                FakeCallNotificationManager.cancelNotification(context)
                FakeCallActivity.dismissCurrentCall()
            }

            FakeCallManager.ACTION_MUTE_FAKE_CALL -> {
                Log.i(TAG, "Toggling mute from notification action")
                FakeCallActivity.toggleMuteCurrentCall()
            }

            FakeCallManager.ACTION_SPEAKER_FAKE_CALL -> {
                Log.i(TAG, "Toggling speaker from notification action")
                FakeCallActivity.toggleSpeakerCurrentCall()
            }

            FakeCallManager.ACTION_CANCEL_FAKE_CALL -> {
                val scheduleId = intent.getStringExtra(FakeCallManager.EXTRA_SCHEDULE_ID)
                if (!scheduleId.isNullOrEmpty()) {
                    FakeCallManager.removeSchedule(context, scheduleId)
                } else {
                    FakeCallManager.clearAllSchedules(context)
                }
            }
        }
    }
}
