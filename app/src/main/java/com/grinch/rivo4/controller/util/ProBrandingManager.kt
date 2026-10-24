package com.grinch.rivo4.controller.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object ProBrandingManager {
    private const val DEFAULT_LAUNCHER = "com.grinch.rivo4.DefaultLauncherAlias"
    private const val PRO_LAUNCHER = "com.grinch.rivo4.ProLauncherAlias"

    fun isActive(preferences: PreferenceManager): Boolean = false

    fun resetToDefault(context: Context) {
        val packageManager = context.packageManager
        setState(packageManager, context, DEFAULT_LAUNCHER, true)
        setState(packageManager, context, PRO_LAUNCHER, false)
    }

    fun apply(context: Context, preferences: PreferenceManager) {
        resetToDefault(context)
    }

    private fun setState(
        packageManager: PackageManager,
        context: Context,
        className: String,
        enabled: Boolean
    ) {
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(context, className),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}
    }
}

