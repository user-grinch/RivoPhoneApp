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
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallActivity
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

class FloatingCallService : Service(), KoinComponent {

    private val contactsRepository: IContactsRepository by inject()

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var updateTimerRunnable: Runnable? = null

    private var avatarInitialView: TextView? = null
    private var avatarIconView: ImageView? = null
    private var callerNameView: TextView? = null
    private var statusDot: View? = null
    private var timerView: TextView? = null
    private var muteButton: ImageView? = null
    private var speakerButton: ImageView? = null

    private var cachedNumber: String? = null
    private var cachedDisplayName: String? = null
    private var cachedInitial: String? = null

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
            x = (resources.displayMetrics.widthPixels - (270 * density).toInt()).coerceAtLeast(20)
            y = (resources.displayMetrics.heightPixels * 0.22f).toInt()
        }

        val rootLayout = FrameLayout(this)

        // Pill shape background with deep obsidian frosted container & luminous highlight border
        val pillBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 27 * density
            setColor(Color.parseColor("#F412151B")) // Deep dark obsidian surface, 96% opacity
            setStroke((1.2f * density).toInt(), Color.parseColor("#334155")) // Sleek slate border
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBg
            setPadding(
                (9 * density).toInt(),
                (5 * density).toInt(),
                (8 * density).toInt(),
                (5 * density).toInt()
            )
            elevation = 16 * density
        }

        // Left: Avatar / Active Beacon Container
        val avatarSize = (34 * density).toInt()
        val avatarContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                rightMargin = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#143525"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#22C55E"))
            }
        }

        avatarInitialView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#4ADE80"))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        avatarContainer.addView(avatarInitialView)

        avatarIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_call_ongoing)
            setColorFilter(Color.parseColor("#4ADE80"))
            val iconSize = (17 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            visibility = View.VISIBLE
        }
        avatarContainer.addView(avatarIconView)
        container.addView(avatarContainer)

        // Middle: Caller Name and Status / Timer Column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = (10 * density).toInt()
            }
        }

        callerNameView = TextView(this).apply {
            text = "Active Call"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            maxLines = 1
            maxWidth = (85 * density).toInt()
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        textCol.addView(callerNameView)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (2 * density).toInt()
            }
        }

        statusDot = View(this).apply {
            val dotSize = (5 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                rightMargin = (4 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4ADE80"))
            }
        }
        statusRow.addView(statusDot)

        timerView = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#86EFAC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        statusRow.addView(timerView)
        textCol.addView(statusRow)
        container.addView(textCol)

        // Right: Action Buttons Group
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val actionBtnSize = (32 * density).toInt()
        val endCallBtnSize = (35 * density).toInt()

        // Mute Button
        muteButton = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(actionBtnSize, actionBtnSize).apply {
                rightMargin = (6 * density).toInt()
            }
            val pad = (7 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val currentMute = CallService.audioState.value?.isMuted == true
                CallService.mute(!currentMute)
                updateAudioIcons()
            }
        }
        actionsRow.addView(muteButton)

        // Speaker Button
        speakerButton = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(actionBtnSize, actionBtnSize).apply {
                rightMargin = (6 * density).toInt()
            }
            val pad = (7 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                CallService.cycleAudioRoute()
                updateAudioIcons()
            }
        }
        actionsRow.addView(speakerButton)

        // End Call Button
        val endCallBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#DC2626"))
            setStroke((1.2f * density).toInt(), Color.parseColor("#EF4444"))
        }
        val endCallButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_end_call)
            setColorFilter(Color.WHITE)
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), endCallBg, null)
            layoutParams = LinearLayout.LayoutParams(endCallBtnSize, endCallBtnSize)
            val pad = (7.5f * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                try {
                    CallService.allCalls.value.firstOrNull { c -> c.state != Call.STATE_DISCONNECTED }?.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disconnect call: ${e.message}")
                }
                stopSelf()
            }
        }
        actionsRow.addView(endCallButton)
        container.addView(actionsRow)

        rootLayout.addView(container)

        // Initial appearance
        updateAudioIcons()
        val activeCall = CallService.allCalls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
        if (activeCall != null) {
            updateCallerInfo(activeCall)
        }

        // Dragging & Edge-snapping gesture controller
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
                    rootLayout.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.92f).setDuration(120).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        val screenHeight = resources.displayMetrics.heightPixels
                        params.x = initialX + dx
                        params.y = (initialY + dy).coerceIn(40, (screenHeight - (120 * density).toInt()))
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    rootLayout.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start()
                    if (!isDragging) {
                        returnToCallActivity()
                    } else {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val currentMidX = params.x + (rootLayout.width / 2)
                        val targetX = if (currentMidX < screenWidth / 2) {
                            (12 * density).toInt()
                        } else {
                            (screenWidth - rootLayout.width - (12 * density).toInt()).coerceAtLeast(0)
                        }

                        val animator = android.animation.ValueAnimator.ofInt(params.x, targetX).apply {
                            duration = 220
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { animation ->
                                params.x = animation.animatedValue as Int
                                try {
                                    windowManager?.updateViewLayout(rootLayout, params)
                                } catch (_: Exception) {}
                            }
                        }
                        animator.start()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    rootLayout.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start()
                    false
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
        val density = resources.displayMetrics.density

        muteButton?.apply {
            setImageResource(if (isMuted) R.drawable.ic_floating_mic_off else R.drawable.ic_floating_mic)
            val iconColor = if (isMuted) Color.parseColor("#FCA5A5") else Color.parseColor("#E2E8F0")
            setColorFilter(iconColor)
            val baseBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isMuted) {
                    setColor(Color.parseColor("#451A20"))
                    setStroke((1f * density).toInt(), Color.parseColor("#991B1B"))
                } else {
                    setColor(Color.parseColor("#222834"))
                    setStroke((1f * density).toInt(), Color.parseColor("#374151"))
                }
            }
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), baseBg, null)
        }

        speakerButton?.apply {
            setImageResource(R.drawable.ic_floating_speaker)
            val iconColor = if (isSpeaker) Color.parseColor("#4ADE80") else Color.parseColor("#E2E8F0")
            setColorFilter(iconColor)
            val baseBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isSpeaker) {
                    setColor(Color.parseColor("#123826"))
                    setStroke((1f * density).toInt(), Color.parseColor("#16A34A"))
                } else {
                    setColor(Color.parseColor("#222834"))
                    setStroke((1f * density).toInt(), Color.parseColor("#374151"))
                }
            }
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), baseBg, null)
        }
    }

    private fun updateCallerInfo(activeCall: Call) {
        val number = activeCall.details.handle?.schemeSpecificPart ?: ""
        if (number != cachedNumber) {
            cachedNumber = number
            val contact = if (number.isNotEmpty()) {
                try {
                    contactsRepository.getContactByNumber(number)
                } catch (_: Exception) {
                    null
                }
            } else null

            val name = contact?.name ?: if (number.isNotEmpty()) formatPhoneNumber(number) else "Active Call"
            cachedDisplayName = name
            cachedInitial = if (contact != null && contact.name.isNotBlank()) {
                contact.name.trim().take(1).uppercase()
            } else null
        }

        callerNameView?.text = cachedDisplayName ?: "Active Call"
        if (cachedInitial != null) {
            avatarInitialView?.text = cachedInitial
            avatarInitialView?.visibility = View.VISIBLE
            avatarIconView?.visibility = View.GONE
        } else {
            avatarInitialView?.visibility = View.GONE
            avatarIconView?.visibility = View.VISIBLE
        }
    }

    private fun updateStatusDot(callState: Int) {
        val isMuted = CallService.audioState.value?.isMuted == true
        val dotColor = when {
            isMuted -> Color.parseColor("#F87171")
            callState == Call.STATE_HOLDING -> Color.parseColor("#FBBF24")
            else -> Color.parseColor("#4ADE80")
        }
        statusDot?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
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

                updateCallerInfo(activeCall)

                val session = CallService.currentCallSession.value
                val connectTime = session?.connectTimeMillis ?: activeCall.details.connectTimeMillis
                if (connectTime > 0) {
                    val durationSec = ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0)
                    val minutes = durationSec / 60
                    val seconds = durationSec % 60
                    timerView?.text = String.format("%02d:%02d", minutes, seconds)
                    timerView?.setTextColor(Color.parseColor("#86EFAC"))
                } else {
                    val isHolding = activeCall.state == Call.STATE_HOLDING
                    timerView?.text = if (isHolding) "On Hold" else "Connecting..."
                    timerView?.setTextColor(if (isHolding) Color.parseColor("#FBBF24") else Color.parseColor("#94A3B8"))
                }

                updateAudioIcons()
                updateStatusDot(activeCall.state)
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
