package com.grinch.rivo4.controller

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.grinch.rivo4.view.screen.MissedCallScreen
import com.grinch.rivo4.view.theme.Rivo4Theme

class MissedCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: ""
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val photoUri = intent.getStringExtra(EXTRA_PHOTO_URI)
        val ringSeconds = intent.getIntExtra(EXTRA_RING_SECONDS, 0)
        val timestampMillis = intent.getLongExtra(EXTRA_TIMESTAMP_MILLIS, System.currentTimeMillis())

        setContent {
            Rivo4Theme {
                MissedCallScreen(
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    photoUri = photoUri,
                    ringSeconds = ringSeconds,
                    timestampMillis = timestampMillis,
                    onDismiss = {
                        finish()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_PHOTO_URI = "extra_photo_uri"
        const val EXTRA_RING_SECONDS = "extra_ring_seconds"
        const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"

        fun start(
            context: Context,
            contactName: String,
            phoneNumber: String,
            photoUri: String?,
            ringSeconds: Int,
            timestampMillis: Long = System.currentTimeMillis()
        ) {
            val intent = Intent(context, MissedCallActivity::class.java).apply {
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_PHOTO_URI, photoUri)
                putExtra(EXTRA_RING_SECONDS, ringSeconds)
                putExtra(EXTRA_TIMESTAMP_MILLIS, timestampMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
