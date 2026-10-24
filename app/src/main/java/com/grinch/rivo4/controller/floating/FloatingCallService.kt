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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
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
import android.view.ViewOutlineProvider
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.VelocityTracker
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallActivity
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

class FloatingCallService : Service(), KoinComponent {

    private val contactsRepository: IContactsRepository by inject()
    private val preferenceManager: PreferenceManager by inject()

    private var windowManager: WindowManager? = null
    private var rootLayout: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var bubbleContainer: FrameLayout? = null
    private var avatarCircle: FrameLayout? = null
    private var avatarImageView: ImageView? = null
    private var badgeContainer: FrameLayout? = null

    private var dismissTargetView: FrameLayout? = null
    private var dismissTargetParams: WindowManager.LayoutParams? = null
    private var dismissTargetCircle: FrameLayout? = null
    private var dismissIconView: ImageView? = null
    private var isHoveringDismiss = false
    private var isDismissTargetVisible = false
    private var isDismissingInProgress = false

    private var dropdownLayout: LinearLayout? = null
    private var topNotchView: NotchView? = null
    private var bottomNotchView: NotchView? = null
    private var cardLayout: LinearLayout? = null

    private var backToCallRow: LinearLayout? = null
    private var backToCallIcon: ImageView? = null
    private var backToCallText: TextView? = null

    private var muteRow: LinearLayout? = null
    private var muteIconView: ImageView? = null
    private var muteTextView: TextView? = null

    private var speakerRow: LinearLayout? = null
    private var speakerIconView: ImageView? = null
    private var speakerTextView: TextView? = null

    private var endCallRow: LinearLayout? = null

    private var bubbleX = 0
    private var bubbleY = 0
    private var isDropdownOpen = false

    private val handler = Handler(Looper.getMainLooper())
    private var updateTimerRunnable: Runnable? = null

    private var cachedNumber: String? = null
    private var cachedDisplayName: String? = null
    private var cachedPhotoUri: String? = null
    private var cachedPhotoBitmap: Bitmap? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        createFloatingBubble()
        startCallStateObserver()
    }

    private fun isDarkTheme(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun isAmoled(): Boolean {
        return preferenceManager.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
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
                description = getString(R.string.floating_call_channel_description)
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
            .setContentTitle(getString(R.string.floating_call_ongoing))
            .setContentText(getString(R.string.floating_call_return))
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification: ${e.message}", e)
        }
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

        val bubbleSize = (60 * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        bubbleX = (screenWidth - bubbleSize - (16 * density).toInt()).coerceAtLeast(20)
        bubbleY = (screenHeight * 0.22f).toInt()

        layoutParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }

        rootLayout = FrameLayout(this)

        // Dismiss dropdown if tapped outside window
        rootLayout?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                if (isDropdownOpen) {
                    collapseDropdown()
                }
                return@setOnTouchListener true
            }
            false
        }

        buildBubbleView(density, bubbleSize)
        buildDropdownView(density)

        rootLayout?.addView(dropdownLayout)
        rootLayout?.addView(bubbleContainer)

        dropdownLayout?.visibility = View.GONE

        val activeCall = CallService.allCalls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
        if (activeCall != null) {
            updateCallerAvatar(activeCall)
        }

        try {
            windowManager?.addView(rootLayout, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating bubble to WindowManager: ${e.message}", e)
            stopSelf()
        }

        buildDismissTargetView(density)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildBubbleView(density: Float, bubbleSize: Int) {
        bubbleContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        }

        // Circular avatar container: entire bubble is the profile picture
        val isDark = isDarkTheme()
        avatarCircle = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDark) Color.parseColor("#374151") else Color.parseColor("#9AA0A6"))
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 8 * density
        }

        avatarImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_person)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        avatarCircle?.addView(avatarImageView)
        bubbleContainer?.addView(avatarCircle)

        // Rivo App Icon Badge in bottom-right corner
        val badgeSize = (23 * density).toInt()
        val badgeBorderColor = if (isDark) Color.parseColor("#374151") else Color.WHITE

        badgeContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize, Gravity.BOTTOM or Gravity.END)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke((2 * density).toInt(), badgeBorderColor)
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 10 * density
        }

        val badgeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_rivo_badge)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        badgeContainer?.addView(badgeIcon)

        bubbleContainer?.addView(badgeContainer)

        // Drag & Tap Controller on Bubble with Velocity, Throw & Bouncy Physics (Messenger style)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var velocityTracker: VelocityTracker? = null

        bubbleContainer?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    flingAnimator?.cancel()
                    flingAnimator = null

                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)

                    initialX = bubbleX
                    initialY = bubbleY
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    bubbleContainer?.animate()?.scaleX(0.92f)?.scaleY(0.92f)?.setDuration(100)?.start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)

                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        if (!isDragging) {
                            if (isDropdownOpen) {
                                collapseDropdown()
                            }
                            isDragging = true
                            showDismissTarget()
                        }
                        val screenHeight = resources.displayMetrics.heightPixels
                        val screenWidth = resources.displayMetrics.widthPixels
                        val dismissSize = (64 * density).toInt()
                        val dismissWindowSize = (96 * density).toInt()
                        val dismissBottomOffset = (36 * density).toInt()
                        val targetCenterX = screenWidth / 2f
                        val targetCenterY = screenHeight - dismissBottomOffset - (dismissWindowSize / 2f)

                        val rawFingerX = event.rawX
                        val rawFingerY = event.rawY
                        val distance = kotlin.math.hypot(
                            (rawFingerX - targetCenterX).toDouble(),
                            (rawFingerY - targetCenterY).toDouble()
                        ).toFloat()

                        val magneticRadius = 110 * density
                        if (distance < magneticRadius) {
                            if (!isHoveringDismiss) {
                                isHoveringDismiss = true
                                setDismissTargetHovered(true)
                                try {
                                    dismissTargetView?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                } catch (_: Exception) {}
                            }

                            // Magnetic snap interpolation: pulls smoothly into target center
                            val ratio = (distance / magneticRadius).coerceIn(0f, 1f)
                            val pullFactor = (1f - ratio) * 0.88f
                            val targetBubbleX = (targetCenterX - bubbleSize / 2f)
                            val targetBubbleY = (targetCenterY - bubbleSize / 2f)
                            val freeBubbleX = (initialX + dx).toFloat()
                            val freeBubbleY = (initialY + dy).toFloat()

                            bubbleX = (freeBubbleX + (targetBubbleX - freeBubbleX) * pullFactor).toInt()
                                .coerceIn(-bubbleSize / 4, screenWidth - (bubbleSize * 3 / 4))
                            bubbleY = (freeBubbleY + (targetBubbleY - freeBubbleY) * pullFactor).toInt()
                                .coerceIn(20, screenHeight - bubbleSize - 20)
                        } else {
                            if (isHoveringDismiss) {
                                isHoveringDismiss = false
                                setDismissTargetHovered(false)
                            }
                            bubbleX = (initialX + dx).coerceIn(-bubbleSize / 4, screenWidth - (bubbleSize * 3 / 4))
                            bubbleY = (initialY + dy).coerceIn(20, screenHeight - bubbleSize - 20)
                        }

                        val params = layoutParams ?: return@setOnTouchListener true
                        params.x = bubbleX
                        params.y = bubbleY
                        try {
                            windowManager?.updateViewLayout(rootLayout, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    if (isHoveringDismiss) {
                        isHoveringDismiss = false
                        val screenHeight = resources.displayMetrics.heightPixels
                        val screenWidth = resources.displayMetrics.widthPixels
                        val dismissSize = (64 * density).toInt()
                        val dismissWindowSize = (96 * density).toInt()
                        val dismissBottomOffset = (36 * density).toInt()
                        val targetCenterX = screenWidth / 2f
                        val targetCenterY = screenHeight - dismissBottomOffset - (dismissWindowSize / 2f)
                        animateDismissAndStop(targetCenterX, targetCenterY, bubbleSize, density)
                    } else {
                        hideDismissTarget()
                        bubbleContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(120)?.start()
                        if (!isDragging) {
                            if (isDropdownOpen) {
                                collapseDropdown()
                            } else {
                                openDropdown()
                            }
                        } else {
                            animateFlingToEdge(vx, vy, bubbleSize, density)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    hideDismissTarget()
                    bubbleContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(120)?.start()
                    false
                }
                else -> false
            }
        }
    }

    private fun buildDismissTargetView(density: Float) {
        val dismissSize = (64 * density).toInt()
        val dismissWindowSize = (96 * density).toInt()
        val dismissBottomOffset = (36 * density).toInt()
        val dismissType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dismissTargetParams = WindowManager.LayoutParams(
            dismissWindowSize,
            dismissWindowSize,
            dismissType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dismissBottomOffset
        }

        dismissTargetView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dismissWindowSize,
                dismissWindowSize
            )
            visibility = View.GONE
            alpha = 0f
        }

        // Circle target centered inside window
        dismissTargetCircle = FrameLayout(this).apply {
            val circleParams = FrameLayout.LayoutParams(
                dismissSize,
                dismissSize,
                Gravity.CENTER
            )
            layoutParams = circleParams
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D9222222"))
                setStroke((2 * density).toInt(), Color.parseColor("#4DFFFFFF"))
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            alpha = 0f
            translationY = 30 * density
        }

        dismissIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_dismiss)
            layoutParams = FrameLayout.LayoutParams(
                (26 * density).toInt(),
                (26 * density).toInt(),
                Gravity.CENTER
            )
        }

        dismissTargetCircle?.addView(dismissIconView)
        dismissTargetView?.addView(dismissTargetCircle)

        try {
            windowManager?.addView(dismissTargetView, dismissTargetParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add dismissTargetView to WindowManager: ${e.message}")
        }
    }

    private fun showDismissTarget() {
        if (isDismissTargetVisible || isDismissingInProgress) return
        isDismissTargetVisible = true
        val density = resources.displayMetrics.density

        dismissTargetView?.visibility = View.VISIBLE
        dismissTargetView?.animate()
            ?.alpha(1f)
            ?.setDuration(180)
            ?.start()

        dismissTargetCircle?.let { circle ->
            circle.translationY = 30 * density
            circle.scaleX = 0.6f
            circle.scaleY = 0.6f
            circle.alpha = 0f
            circle.animate()
                ?.translationY(0f)
                ?.scaleX(1.0f)
                ?.scaleY(1.0f)
                ?.alpha(1.0f)
                ?.setDuration(240)
                ?.setInterpolator(OvershootInterpolator(1.3f))
                ?.start()
        }
    }

    private fun hideDismissTarget() {
        if (!isDismissTargetVisible || isDismissingInProgress) return
        isDismissTargetVisible = false
        setDismissTargetHovered(false)
        val density = resources.displayMetrics.density

        dismissTargetCircle?.animate()
            ?.translationY(30 * density)
            ?.scaleX(0.6f)
            ?.scaleY(0.6f)
            ?.alpha(0f)
            ?.setDuration(180)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()

        dismissTargetView?.animate()
            ?.alpha(0f)
            ?.setDuration(180)
            ?.withEndAction {
                if (!isDismissTargetVisible) {
                    dismissTargetView?.visibility = View.GONE
                }
            }
            ?.start()
    }

    private fun setDismissTargetHovered(hovered: Boolean) {
        val density = resources.displayMetrics.density
        val drawable = dismissTargetCircle?.background as? GradientDrawable ?: return
        if (hovered) {
            drawable.setColor(Color.parseColor("#E53935"))
            drawable.setStroke((2.5f * density).toInt(), Color.WHITE)
            dismissTargetCircle?.animate()
                ?.scaleX(1.30f)
                ?.scaleY(1.30f)
                ?.setDuration(160)
                ?.setInterpolator(OvershootInterpolator(1.4f))
                ?.start()

            // Scale 'X' slightly for emphasis, but keep rotation strictly at 0f (NEVER rotate into '+')
            dismissIconView?.animate()
                ?.scaleX(1.18f)
                ?.scaleY(1.18f)
                ?.rotation(0f)
                ?.setDuration(160)
                ?.start()

            bubbleContainer?.animate()
                ?.scaleX(0.80f)
                ?.scaleY(0.80f)
                ?.setDuration(120)
                ?.start()
        } else {
            drawable.setColor(Color.parseColor("#D9222222"))
            drawable.setStroke((2 * density).toInt(), Color.parseColor("#4DFFFFFF"))
            dismissTargetCircle?.animate()
                ?.scaleX(1.0f)
                ?.scaleY(1.0f)
                ?.setDuration(160)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()

            dismissIconView?.animate()
                ?.scaleX(1.0f)
                ?.scaleY(1.0f)
                ?.rotation(0f)
                ?.setDuration(160)
                ?.start()

            bubbleContainer?.animate()
                ?.scaleX(0.92f)
                ?.scaleY(0.92f)
                ?.setDuration(120)
                ?.start()
        }
    }

    private fun animateDismissAndStop(targetCenterX: Float, targetCenterY: Float, bubbleSize: Int, density: Float) {
        if (isDismissingInProgress) return
        isDismissingInProgress = true

        try {
            dismissTargetView?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } catch (_: Exception) {}

        val startX = bubbleX.toFloat()
        val startY = bubbleY.toFloat()
        val destX = targetCenterX - (bubbleSize / 2f)
        val destY = targetCenterY - (bubbleSize / 2f)

        // Ensure icon is firmly an 'X' and not rotated
        dismissIconView?.rotation = 0f

        // 1. Swallow animation: Bubble pulls into center of the 'X', shrinks to 0
        val swallowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            interpolator = AccelerateInterpolator(1.8f)
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                val curX = (startX + (destX - startX) * fraction).toInt()
                val curY = (startY + (destY - startY) * fraction).toInt()

                layoutParams?.let { params ->
                    params.x = curX
                    params.y = curY
                    try {
                        windowManager?.updateViewLayout(rootLayout, params)
                    } catch (_: Exception) {}
                }

                val scale = (0.80f * (1f - fraction)).coerceAtLeast(0f)
                val alpha = (1f - fraction).coerceIn(0f, 1f)
                bubbleContainer?.scaleX = scale
                bubbleContainer?.scaleY = scale
                bubbleContainer?.alpha = alpha
            }
        }

        // 2. The 'X' target circle: absorbs the bubble, then immediately sinks down smoothly into the bottom of the screen
        dismissTargetCircle?.animate()
            ?.scaleX(1.36f)
            ?.scaleY(1.36f)
            ?.setDuration(90)
            ?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction {
                dismissTargetCircle?.animate()
                    ?.scaleX(0.1f)
                    ?.scaleY(0.1f)
                    ?.translationY(40 * density)
                    ?.alpha(0f)
                    ?.setDuration(160)
                    ?.setInterpolator(AccelerateInterpolator(1.6f))
                    ?.start()

                dismissTargetView?.animate()
                    ?.alpha(0f)
                    ?.setDuration(160)
                    ?.withEndAction {
                        stopSelf()
                    }
                    ?.start()
            }
            ?.start()

        swallowAnimator.start()
    }

    private var flingAnimator: ValueAnimator? = null

    private fun animateFlingToEdge(
        vx: Float,
        vy: Float,
        bubbleSize: Int,
        density: Float
    ) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val edgeMargin = (12 * density).toInt()
        val minX = edgeMargin
        val maxX = (screenWidth - bubbleSize - edgeMargin).coerceAtLeast(0)

        val minY = (40 * density).toInt()
        val maxY = (screenHeight - bubbleSize - (60 * density).toInt()).coerceAtLeast(minY)

        // 1. Determine target X based on velocity (throw direction)
        val flingThresholdX = 400 * density
        val currentMidX = bubbleX + (bubbleSize / 2)
        val targetX = when {
            vx > flingThresholdX -> maxX // Thrown to the right
            vx < -flingThresholdX -> minX // Thrown to the left
            else -> if (currentMidX < screenWidth / 2) minX else maxX // Snap to closer edge
        }

        // 2. Determine target Y with momentum projection
        val momentumFactor = 0.18f
        val projectedY = bubbleY + (vy * momentumFactor).toInt()
        val targetY = projectedY.coerceIn(minY, maxY)

        // 3. Dynamic timing calculation
        val startX = bubbleX
        val startY = bubbleY
        val dxDist = abs(targetX - startX)
        val dyDist = abs(targetY - startY)
        val totalDist = kotlin.math.hypot(dxDist.toDouble(), dyDist.toDouble()).toFloat()
        val totalSpeed = kotlin.math.hypot(vx.toDouble(), vy.toDouble()).toFloat()

        val isFastThrow = totalSpeed > flingThresholdX
        val duration = if (isFastThrow) {
            val calculated = (totalDist / (totalSpeed + 900f) * 1000).toLong()
            calculated.coerceIn(200L, 360L)
        } else {
            val calculated = (totalDist / (screenWidth * 0.75f) * 320).toLong()
            calculated.coerceIn(180L, 300L)
        }

        flingAnimator?.cancel()
        flingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val newX = (startX + (targetX - startX) * fraction).toInt()
                val newY = (startY + (targetY - startY) * fraction).toInt()

                bubbleX = newX.coerceIn(minX, maxX)
                bubbleY = newY.coerceIn(minY, maxY)

                val params = layoutParams ?: return@addUpdateListener
                params.x = bubbleX
                params.y = bubbleY
                try {
                    windowManager?.updateViewLayout(rootLayout, params)
                } catch (_: Exception) {}
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bubbleX = targetX
                    bubbleY = targetY
                    val params = layoutParams
                    if (params != null) {
                        params.x = bubbleX
                        params.y = bubbleY
                        try {
                            windowManager?.updateViewLayout(rootLayout, params)
                        } catch (_: Exception) {}
                    }
                }
            })
        }
        flingAnimator?.start()
    }

    private fun buildDropdownView(density: Float) {
        val dropdownWidth = (195 * density).toInt()
        val notchHeight = (8 * density).toInt()
        val notchWidth = (16 * density).toInt()

        dropdownLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(dropdownWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        topNotchView = NotchView(this, pointingUp = true).apply {
            layoutParams = LinearLayout.LayoutParams(notchWidth, notchHeight)
        }
        dropdownLayout?.addView(topNotchView)

        cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dropdownWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 14 * density
        }

        // 1. Back to call
        val (backRow, backIcon, backText) = createRow(
            iconRes = R.drawable.ic_floating_back_to_call,
            label = getString(R.string.floating_call_back),
            density = density,
            onClick = {
                returnToCallActivity()
            }
        )
        backToCallRow = backRow
        backToCallIcon = backIcon
        backToCallText = backText
        cardLayout?.addView(backRow)

        // 2. Mute
        val (mRow, mIcon, mText) = createRow(
            iconRes = R.drawable.ic_floating_mic_off,
            label = getString(R.string.action_mute),
            density = density,
            onClick = {
                val currentMute = CallService.audioState.value?.isMuted == true
                CallService.mute(!currentMute)
                updateDropdownAudioState()
            }
        )
        muteRow = mRow
        muteIconView = mIcon
        muteTextView = mText
        cardLayout?.addView(mRow)

        // 3. Speaker
        val (sRow, sIcon, sText) = createRow(
            iconRes = R.drawable.ic_floating_speaker,
            label = getString(R.string.audio_route_speaker),
            density = density,
            onClick = {
                CallService.cycleAudioRoute()
                updateDropdownAudioState()
            }
        )
        speakerRow = sRow
        speakerIconView = sIcon
        speakerTextView = sText
        cardLayout?.addView(sRow)

        // 4. End call
        endCallRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * density).toInt()
            )
            setPadding((18 * density).toInt(), 0, (18 * density).toInt(), 0)
            setOnClickListener {
                try {
                    CallService.allCalls.value.firstOrNull { c -> c.state != Call.STATE_DISCONNECTED }?.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disconnect call: ${e.message}")
                }
                stopSelf()
            }
        }

        val endCallIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_end_call)
            setColorFilter(Color.WHITE)
            val iconSize = (22 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                rightMargin = (16 * density).toInt()
            }
        }
        endCallRow?.addView(endCallIcon)

        val endCallText = TextView(this).apply {
            text = "End call"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        endCallRow?.addView(endCallText)

        cardLayout?.addView(endCallRow)
        dropdownLayout?.addView(cardLayout)

        bottomNotchView = NotchView(this, pointingUp = false).apply {
            layoutParams = LinearLayout.LayoutParams(notchWidth, notchHeight)
        }
        dropdownLayout?.addView(bottomNotchView)
    }

    private fun createRow(
        iconRes: Int,
        label: String,
        density: Float,
        onClick: (View) -> Unit
    ): Triple<LinearLayout, ImageView, TextView> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * density).toInt()
            )
            setPadding((18 * density).toInt(), 0, (18 * density).toInt(), 0)
            setOnClickListener(onClick)
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            val iconSize = (22 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                rightMargin = (16 * density).toInt()
            }
        }
        row.addView(icon)

        val text = TextView(this).apply {
            this.text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        row.addView(text)

        return Triple(row, icon, text)
    }

    private fun createRowRipple(normalColor: Int, rippleColor: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            ColorDrawable(normalColor),
            ColorDrawable(Color.WHITE)
        )
    }

    private fun createEndCallRipple(normalColor: Int, rippleColor: Int, cornerRadius: Float): RippleDrawable {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            setColor(normalColor)
        }
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            setColor(Color.WHITE)
        }
        return RippleDrawable(ColorStateList.valueOf(rippleColor), bg, mask)
    }

    private fun applyPopupTheme() {
        val density = resources.displayMetrics.density
        val isDark = isDarkTheme()
        val isAmoled = isDark && isAmoled()

        val cardBgColor = when {
            isAmoled -> Color.parseColor("#000000")
            isDark -> Color.parseColor("#1E1F22")
            else -> Color.WHITE
        }
        val cardBorderColor = when {
            isAmoled -> Color.parseColor("#262626")
            isDark -> Color.parseColor("#374151")
            else -> Color.parseColor("#E5E7EB")
        }
        val primaryTextColor = if (isDark) Color.parseColor("#F3F4F6") else Color.parseColor("#202124")
        val secondaryColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#5F6368")
        val rowRippleColor = if (isDark) Color.parseColor("#26FFFFFF") else Color.parseColor("#1F000000")
        val endCallBgColor = if (isDark) Color.parseColor("#DC2626") else Color.parseColor("#C5221F")

        cardLayout?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18 * density
            setColor(cardBgColor)
            setStroke((1 * density).toInt(), cardBorderColor)
        }

        topNotchView?.setColor(cardBgColor)
        bottomNotchView?.setColor(cardBgColor)

        // Back to call
        backToCallRow?.background = createRowRipple(cardBgColor, rowRippleColor)
        backToCallIcon?.setColorFilter(secondaryColor)
        backToCallText?.setTextColor(primaryTextColor)

        // Mute & Speaker backgrounds
        muteRow?.background = createRowRipple(cardBgColor, rowRippleColor)
        speakerRow?.background = createRowRipple(cardBgColor, rowRippleColor)

        // End call
        endCallRow?.background = createEndCallRipple(endCallBgColor, Color.parseColor("#40FFFFFF"), 18 * density)

        // Also refresh badge container colors
        val badgeBorderColor = if (isDark) Color.parseColor("#374151") else Color.WHITE
        badgeContainer?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke((2 * density).toInt(), badgeBorderColor)
        }
    }

    private fun openDropdown() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val bubbleSize = (60 * density).toInt()
        val dropdownWidth = (195 * density).toInt()
        val cardHeight = (194 * density).toInt()
        val notchHeight = (8 * density).toInt()
        val notchWidth = (16 * density).toInt()
        val totalDropdownHeight = cardHeight + notchHeight
        val margin = (4 * density).toInt()

        val isBelow = (bubbleY + bubbleSize + totalDropdownHeight + (40 * density).toInt()) < screenHeight

        topNotchView?.visibility = if (isBelow) View.VISIBLE else View.GONE
        bottomNotchView?.visibility = if (isBelow) View.GONE else View.VISIBLE

        applyPopupTheme()

        val bubbleCenterX = bubbleX + (bubbleSize / 2)
        val minMargin = (12 * density).toInt()
        val maxDropdownX = screenWidth - dropdownWidth - minMargin
        val targetDropdownX = (bubbleCenterX - (dropdownWidth / 2)).coerceIn(minMargin, maxDropdownX)

        val windowLeft = minOf(bubbleX, targetDropdownX)
        val windowRight = maxOf(bubbleX + bubbleSize, targetDropdownX + dropdownWidth)
        val windowWidth = windowRight - windowLeft

        val windowTop: Int
        val windowHeight: Int
        val bubbleRelY: Float
        val dropdownRelY: Float

        if (isBelow) {
            windowTop = bubbleY
            windowHeight = bubbleSize + margin + totalDropdownHeight
            bubbleRelY = 0f
            dropdownRelY = (bubbleSize + margin).toFloat()
        } else {
            windowTop = (bubbleY - margin - totalDropdownHeight).coerceAtLeast(0)
            windowHeight = bubbleY + bubbleSize - windowTop
            dropdownRelY = 0f
            bubbleRelY = (totalDropdownHeight + margin).toFloat()
        }

        bubbleContainer?.translationX = (bubbleX - windowLeft).toFloat()
        bubbleContainer?.translationY = bubbleRelY

        dropdownLayout?.translationX = (targetDropdownX - windowLeft).toFloat()
        dropdownLayout?.translationY = dropdownRelY

        val notchTargetX = (bubbleCenterX - targetDropdownX - (notchWidth / 2))
            .coerceIn((18 * density).toInt(), dropdownWidth - notchWidth - (18 * density).toInt())

        topNotchView?.translationX = notchTargetX.toFloat()
        bottomNotchView?.translationX = notchTargetX.toFloat()

        val params = layoutParams ?: return
        params.x = windowLeft
        params.y = windowTop
        params.width = windowWidth
        params.height = windowHeight
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        windowManager?.updateViewLayout(rootLayout, params)

        updateDropdownAudioState()

        dropdownLayout?.alpha = 0f
        dropdownLayout?.scaleX = 0.88f
        dropdownLayout?.scaleY = 0.88f
        dropdownLayout?.visibility = View.VISIBLE
        dropdownLayout?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(160)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()

        isDropdownOpen = true
    }

    private fun collapseDropdown() {
        if (!isDropdownOpen) return
        isDropdownOpen = false

        val density = resources.displayMetrics.density
        val bubbleSize = (60 * density).toInt()

        dropdownLayout?.visibility = View.GONE
        bubbleContainer?.translationX = 0f
        bubbleContainer?.translationY = 0f

        val params = layoutParams ?: return
        params.x = bubbleX
        params.y = bubbleY
        params.width = bubbleSize
        params.height = bubbleSize
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        windowManager?.updateViewLayout(rootLayout, params)
    }

    private fun returnToCallActivity() {
        collapseDropdown()
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun updateDropdownAudioState() {
        val isDark = isDarkTheme()
        val primaryTextColor = if (isDark) Color.parseColor("#F3F4F6") else Color.parseColor("#202124")
        val secondaryColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#5F6368")
        val activeHighlightColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#1A73E8")

        val audioState = CallService.audioState.value
        val isMuted = audioState?.isMuted == true
        val isSpeaker = audioState?.route == CallAudioState.ROUTE_SPEAKER

        muteIconView?.setImageResource(if (isMuted) R.drawable.ic_floating_mic_off else R.drawable.ic_floating_mic)
        if (isMuted) {
            muteIconView?.setColorFilter(activeHighlightColor)
            muteTextView?.setTextColor(activeHighlightColor)
            muteTextView?.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        } else {
            muteIconView?.setColorFilter(secondaryColor)
            muteTextView?.setTextColor(primaryTextColor)
            muteTextView?.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }

        speakerIconView?.setImageResource(R.drawable.ic_floating_speaker)
        if (isSpeaker) {
            speakerIconView?.setColorFilter(activeHighlightColor)
            speakerTextView?.setTextColor(activeHighlightColor)
            speakerTextView?.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        } else {
            speakerIconView?.setColorFilter(secondaryColor)
            speakerTextView?.setTextColor(primaryTextColor)
            speakerTextView?.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
    }

    private fun updateCallerAvatar(activeCall: Call) {
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

            cachedDisplayName = contact?.name ?: if (number.isNotEmpty()) formatPhoneNumber(number) else "Active Call"
            cachedPhotoUri = contact?.photoUri

            cachedPhotoBitmap = try {
                if (!cachedPhotoUri.isNullOrEmpty()) {
                    val uri = Uri.parse(cachedPhotoUri)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    val reqSize = (60 * resources.displayMetrics.density).toInt().coerceAtLeast(180)
                    var sampleSize = 1
                    if (bounds.outHeight > reqSize || bounds.outWidth > reqSize) {
                        val halfHeight = bounds.outHeight / 2
                        val halfWidth = bounds.outWidth / 2
                        while (halfHeight / sampleSize >= reqSize && halfWidth / sampleSize >= reqSize) {
                            sampleSize *= 2
                        }
                    }
                    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }

        val photo = cachedPhotoBitmap
        if (photo != null) {
            avatarImageView?.setImageBitmap(photo)
            avatarImageView?.colorFilter = null
            avatarImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            avatarImageView?.setPadding(0, 0, 0, 0)
        } else {
            val isDark = isDarkTheme()
            avatarCircle?.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDark) Color.parseColor("#374151") else Color.parseColor("#9AA0A6"))
            }
            avatarImageView?.setImageResource(R.drawable.ic_floating_person)
            avatarImageView?.setColorFilter(Color.WHITE)
            avatarImageView?.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val density = resources.displayMetrics.density
            val pad = (10 * density).toInt()
            avatarImageView?.setPadding(pad, pad, pad, pad)
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

                updateCallerAvatar(activeCall)
                if (isDropdownOpen) {
                    updateDropdownAudioState()
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimerRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        flingAnimator?.cancel()
        flingAnimator = null
        updateTimerRunnable?.let { handler.removeCallbacks(it) }
        rootLayout?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating bubble view: ${e.message}")
            }
        }
        rootLayout = null

        dismissTargetView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove dismissTargetView: ${e.message}")
            }
        }
        dismissTargetView = null
        dismissTargetCircle = null
        dismissIconView = null
    }

    class NotchView(context: Context, var pointingUp: Boolean = true) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val path = Path()

        fun setColor(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            path.reset()
            val w = width.toFloat()
            val h = height.toFloat()
            if (pointingUp) {
                path.moveTo(w / 2f, 0f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
            } else {
                path.moveTo(0f, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w / 2f, h)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
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
