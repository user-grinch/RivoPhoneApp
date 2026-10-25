package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.grinch.rivo4.MainActivity
import com.grinch.rivo4.R

object AppShortcutsHelper {

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        try {
            val launcherComponent = ProBrandingManager.getActiveLauncherComponent(context)

            val dialpad = ShortcutInfoCompat.Builder(context, "shortcut_dialpad")
                .setActivity(launcherComponent)
                .setShortLabel(context.getString(R.string.action_keypad))
                .setLongLabel(context.getString(R.string.action_keypad))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_dialpad))
                .setIntent(
                    Intent(Intent.ACTION_DIAL).apply {
                        setClass(context, MainActivity::class.java)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(0)
                .build()

            val newContact = ShortcutInfoCompat.Builder(context, "new_contact")
                .setActivity(launcherComponent)
                .setShortLabel(context.getString(R.string.action_add_contact))
                .setLongLabel(context.getString(R.string.contact_create_title))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_add_contact))
                .setIntent(
                    Intent(Intent.ACTION_INSERT).apply {
                        setClass(context, MainActivity::class.java)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(1)
                .build()

            val contacts = ShortcutInfoCompat.Builder(context, "shortcut_contacts")
                .setActivity(launcherComponent)
                .setShortLabel(context.getString(R.string.app_name_contacts))
                .setLongLabel(context.getString(R.string.app_name_contacts))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_contacts))
                .setIntent(
                    Intent("com.grinch.rivo4.ACTION_VIEW_CONTACTS").apply {
                        setClass(context, MainActivity::class.java)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(2)
                .build()

            val recents = ShortcutInfoCompat.Builder(context, "shortcut_recents")
                .setActivity(launcherComponent)
                .setShortLabel(context.getString(R.string.shortcut_recents))
                .setLongLabel(context.getString(R.string.shortcut_recents))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_recents))
                .setIntent(
                    Intent("com.grinch.rivo4.ACTION_VIEW_RECENTS").apply {
                        setClass(context, MainActivity::class.java)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(3)
                .build()

            val settings = ShortcutInfoCompat.Builder(context, "shortcut_settings")
                .setActivity(launcherComponent)
                .setShortLabel(context.getString(R.string.settings_title))
                .setLongLabel(context.getString(R.string.settings_title))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_settings))
                .setIntent(
                    Intent("com.grinch.rivo4.ACTION_OPEN_SETTINGS").apply {
                        setClass(context, MainActivity::class.java)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(4)
                .build()

            val list = listOf(dialpad, newContact, contacts, recents, settings)
            val maxCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            ShortcutManagerCompat.setDynamicShortcuts(context, list.take(maxCount))
        } catch (e: Throwable) {
            Log.e("AppShortcutsHelper", "Failed to update dynamic shortcuts: ${e.message}", e)
        }
    }
}
