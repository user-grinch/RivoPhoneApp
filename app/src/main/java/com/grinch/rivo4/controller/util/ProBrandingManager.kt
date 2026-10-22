package com.grinch.rivo4.controller.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED

object ProBrandingManager {
    private const val DEFAULT_LAUNCHER = "com.grinch.rivo4.DefaultLauncherAlias"
    private const val PRO_LAUNCHER = "com.grinch.rivo4.ProLauncherAlias"

    fun isActive(preferences: PreferenceManager): Boolean =
        IS_ADS_SUPPORTED &&
            (preferences.isSupporter() ||
                preferences.hasAdSupportedPro() ||
                preferences.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true)) &&
            preferences.getSelectedAppIcon() == "pro"

    fun apply(context: Context, preferences: PreferenceManager) {
        val proEnabled = IS_ADS_SUPPORTED &&
            (preferences.isSupporter() ||
                preferences.hasAdSupportedPro() ||
                preferences.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true)) &&
            preferences.getSelectedAppIcon() == "pro"

        val packageManager = context.packageManager
        setState(packageManager, context, DEFAULT_LAUNCHER, !proEnabled)
        setState(packageManager, context, PRO_LAUNCHER, proEnabled)
    }

    private fun setState(
        packageManager: PackageManager,
        context: Context,
        className: String,
        enabled: Boolean
    ) {
        packageManager.setComponentEnabledSetting(
            ComponentName(context, className),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
