package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.grinch.rivo4.R

object CallNotificationHelper {

    /**
     * Returns the Material Design 3 Expressive theme accent color for notifications.
     * Uses Android 12+ Material You dynamic color system if available, else Rivo's brand seed.
     */
    fun getNotificationColor(context: Context): Int {
        val isNight =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getColor(
                context,
                if (isNight) android.R.color.system_accent1_300 else android.R.color.system_accent1_600
            )
        } else {
            if (isNight) 0xFF9BCAFF.toInt() else 0xFF0461A3.toInt()
        }
    }

    /**
     * Generates a circular avatar bitmap matching Material Design 3 Expressive / Google Dialer.
     * If a photo bitmap is provided, it is clipped into a smooth circle.
     * If no photo is provided, a dynamic M3 tonal colored circle is rendered with the contact's initial
     * or a fallback call icon.
     */
    fun getAvatarBitmap(context: Context, name: String, photo: Bitmap?): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (64 * density).toInt().coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (photo != null) {
            paint.isFilterBitmap = true
            val srcRect = Rect(0, 0, photo.width, photo.height)
            val dstRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
            val path = Path().apply { addOval(dstRect, Path.Direction.CW) }
            canvas.clipPath(path)
            canvas.drawBitmap(photo, srcRect, dstRect, paint)
        } else {
            val isNight =
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val bgColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.getColor(
                    context,
                    if (isNight) android.R.color.system_accent1_700 else android.R.color.system_accent1_100
                )
            } else {
                if (isNight) 0xFF004880.toInt() else 0xFFCFE4FF.toInt()
            }
            val textColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.getColor(
                    context,
                    if (isNight) android.R.color.system_accent1_100 else android.R.color.system_accent1_900
                )
            } else {
                if (isNight) 0xFFCFE4FF.toInt() else 0xFF001C38.toInt()
            }

            paint.color = bgColor
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            val cleanName = name.trim()
            val isGenericUnknown = cleanName.isBlank() || cleanName == context.getString(R.string.label_unknown_number)
            val initial = if (!isGenericUnknown) cleanName.firstOrNull { it.isLetter() }?.uppercaseChar() else null

            if (initial != null) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = size * 0.44f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText(initial.toString(), size / 2f, yPos, textPaint)
            } else {
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_call_ongoing)
                    ?: ContextCompat.getDrawable(context, android.R.drawable.sym_call_incoming)
                if (drawable != null) {
                    val inset = (size * 0.25f).toInt()
                    drawable.setBounds(inset, inset, size - inset, size - inset)
                    drawable.setTint(textColor)
                    drawable.draw(canvas)
                }
            }
        }
        return bitmap
    }

    fun getAvatarIcon(context: Context, name: String, photo: Bitmap?): IconCompat {
        return IconCompat.createWithBitmap(getAvatarBitmap(context, name, photo))
    }
}
