package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.grinch.rivo4.MainActivity
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.data.Contact

object DynamicShortcutManager {

    fun updateDynamicShortcuts(context: Context, favoriteContacts: List<Contact>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        try {
            val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java) ?: return

            val top2Favorites = favoriteContacts
                .filter { it.phoneNumbers.any { num -> num.isNotBlank() } }
                .take(2)

            val shortcuts = mutableListOf<ShortcutInfo>()

            top2Favorites.forEachIndexed { index, contact ->
                val primaryPhone = contact.phoneNumbers.firstOrNull { it.isNotBlank() } ?: return@forEachIndexed
                val displayName = contact.formattedDisplayName.ifBlank { contact.name.ifBlank { primaryPhone } }
                val shortcutId = "dial_favorite_${contact.id}"

                val dialIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(primaryPhone)}")).apply {
                    setClass(context, MainActivity::class.java)
                    putExtra("from_shortcut", true)
                    putExtra("contact_id", contact.id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                val icon = createShortcutIcon(context, contact, displayName)

                val shortcut = ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(displayName.take(15))
                    .setLongLabel(context.getString(R.string.shortcut_dial_contact, displayName))
                    .setIcon(icon)
                    .setIntent(dialIntent)
                    .setRank(index)
                    .build()

                shortcuts.add(shortcut)
            }

            shortcutManager.dynamicShortcuts = shortcuts
        } catch (e: Exception) {
            android.util.Log.e("DynamicShortcutManager", "Error updating dynamic shortcuts", e)
        }
    }

    private fun createShortcutIcon(context: Context, contact: Contact, displayName: String): Icon {
        if (!contact.photoUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(contact.photoUri)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    return Icon.createWithAdaptiveBitmap(bitmap)
                }
            } catch (e: Exception) {
            }
        }

        val size = 108
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = intArrayOf(
            0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFD32F2F.toInt(),
            0xFF7B1FA2.toInt(), 0xFF00796B.toInt(), 0xFFF57C00.toInt(),
            0xFF303F9F.toInt(), 0xFF5D4037.toInt(), 0xFF0288D1.toInt()
        )
        val colorIndex = Math.abs(displayName.hashCode()) % colors.size
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors[colorIndex]
            style = Paint.Style.FILL
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        val initial = ContactUtils.stripTitlePrefix(displayName).firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val bounds = Rect()
        textPaint.getTextBounds(initial, 0, initial.length, bounds)
        val yOffset = bounds.height() / 2f - bounds.bottom
        canvas.drawText(initial, radius, radius + yOffset, textPaint)

        return Icon.createWithAdaptiveBitmap(bitmap)
    }
}
