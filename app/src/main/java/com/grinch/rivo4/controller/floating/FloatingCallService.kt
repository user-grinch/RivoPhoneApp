package com.grinch.rivo4.controller.floating

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallActivity
import com.grinch.rivo4.controller.CallService
import kotlin.math.abs

class FloatingCallService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var updateTimerRunnable: Runnable? = null

    private var callerNameView: TextView? = null
    private var timerView: TextView? = null
    private var muteButton: ImageView? = null
    private var speakerButton: ImageView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        createFloatingBubble()
        startCallStateObserver()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_call_bubble_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Call Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating controls for active call"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ongoing Call")
            .setContentText("Tap to return to call")
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - (260 * density).toInt()).coerceAtLeast(30)
            y = (resources.displayMetrics.heightPixels * 0.25f).toInt()
        }

        val rootLayout = FrameLayout(this)

        // Pill shape background
        val pillBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28 * density
            setColor(Color.parseColor("#EE1E2124"))
            setStroke((1.5f * density).toInt(), Color.parseColor("#44FFFFFF"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBg
            setPadding(
                (10 * density).toInt(),
                (8 * density).toInt(),
                (12 * density).toInt(),
                (8 * density).toInt()
            )
            elevation = 16 * density
        }

        // Caller Avatar circle
        val avatarBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#386A20"))
        }
        val avatarIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_call_ongoing)
            setColorFilter(Color.WHITE)
            background = avatarBg
            val size = (36 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = (10 * density).toInt()
            }
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
        }
        container.addView(avatarIcon)

        // Caller Name and Timer Column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = (14 * density).toInt()
            }
        }

        val activeCall = CallService.allCalls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
        val displayName = activeCall?.details?.handle?.schemeSpecificPart ?: "In Call"

        callerNameView = TextView(this).apply {
            text = displayName
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 1
            maxWidth = (110 * density).toInt()
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        textCol.addView(callerNameView)

        timerView = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#80D651"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        textCol.addView(timerView)
        container.addView(textCol)

        // Action Buttons Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Mute Button
        val isMuted = CallService.audioState.value?.isMuted == true
        val buttonBg = {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
            }
        }

        muteButton = ImageView(this).apply {
            setImageResource(if (isMuted) R.drawable.ic_floating_mic_off else R.drawable.ic_floating_mic)
            setColorFilter(Color.WHITE)
            background = buttonBg()
            val size = (34 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = (8 * density).toInt()
            }
            setPadding((7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt())
            setOnClickListener {
                val currentMute = CallService.audioState.value?.isMuted == true
                CallService.mute(!currentMute)
                updateAudioIcons()
            }
        }
        actionsRow.addView(muteButton)

        // Speaker Button
        speakerButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_speaker)
            setColorFilter(Color.WHITE)
            background = buttonBg()
            val size = (34 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = (8 * density).toInt()
            }
            setPadding((7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt())
            setOnClickListener {
                CallService.cycleAudioRoute()
                updateAudioIcons()
            }
        }
        actionsRow.addView(speakerButton)

        // End Call Button
        val endCallBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#BA1A1A"))
        }
        val endCallButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_end_call)
            setColorFilter(Color.WHITE)
            background = endCallBg
            val size = (34 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding((7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt())
            setOnClickListener {
                try {
                    CallService.allCalls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }?.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disconnect call: ${e.message}")
                }
                stopSelf()
            }
        }
        actionsRow.addView(endCallButton)
        container.addView(actionsRow)

        rootLayout.addView(container)

        // Touch listener for dragging and click detection
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        rootLayout.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Clicked -> return to full-screen CallActivity
                        returnToCallActivity()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = rootLayout

        try {
            windowManager?.addView(bubbleView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating bubble to WindowManager: ${e.message}", e)
            stopSelf()
        }
    }

    private fun returnToCallActivity() {
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun updateAudioIcons() {
        val audioState = CallService.audioState.value
        val isMuted = audioState?.isMuted == true
        val isSpeaker = audioState?.route == CallAudioState.ROUTE_SPEAKER

        muteButton?.apply {
            setImageResource(if (isMuted) R.drawable.ic_floating_mic_off else R.drawable.ic_floating_mic)
            setColorFilter(if (isMuted) Color.parseColor("#FFB4AB") else Color.WHITE)
        }

        speakerButton?.apply {
            setColorFilter(if (isSpeaker) Color.parseColor("#80D651") else Color.WHITE)
        }
    }

    private fun startCallStateObserver() {
        updateTimerRunnable = object : Runnable {
            override fun run() {
                val activeCall = CallService.allCalls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
                if (activeCall == null) {
                    stopSelf()
                    return
                }

                val session = CallService.currentCallSession.value
                val connectTime = session?.connectTimeMillis ?: activeCall.details.connectTimeMillis
                if (connectTime > 0) {
                    val durationSec = ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0)
                    val minutes = durationSec / 60
                    val seconds = durationSec % 60
                    timerView?.text = String.format("%02d:%02d", minutes, seconds)
                } else {
                    timerView?.text = if (activeCall.state == Call.STATE_HOLDING) "On Hold" else "Connecting..."
                }

                updateAudioIcons()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimerRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimerRunnable?.let { handler.removeCallbacks(it) }
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating bubble view: ${e.message}")
            }
        }
        bubbleView = null
    }

    companion object {
        private const val TAG = "FloatingCallService"
        private const val NOTIFICATION_ID = 9988

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                try {
                    val intent = Intent(context, FloatingCallService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting FloatingCallService: ${e.message}")
                }
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, FloatingCallService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping FloatingCallService: ${e.message}")
            }
        }
    }
}
