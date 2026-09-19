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
import com.grinch.rivo4.view.components.ad.AdPreloader
import com.grinch.rivo4.view.screen.PostCallScreen
import com.grinch.rivo4.view.theme.Rivo4Theme

class PostCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdPreloader.preloadPostCallAd(this@PostCallActivity)

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
        val durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)

        if (durationSeconds <= 0L) {
            finish()
            return
        }

        setContent {
            Rivo4Theme {
                PostCallScreen(
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    photoUri = photoUri,
                    durationSeconds = durationSeconds,
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
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        fun start(
            context: Context,
            contactName: String,
            phoneNumber: String,
            photoUri: String?,
            durationSeconds: Long
        ) {
            if (durationSeconds <= 0L) {
                // Never show post-call summary dialog for calls that were not connected/completed
                return
            }
            val intent = Intent(context, PostCallActivity::class.java).apply {
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_PHOTO_URI, photoUri)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
