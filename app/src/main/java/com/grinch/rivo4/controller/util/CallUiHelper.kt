package com.grinch.rivo4.controller.util

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

object CallUiHelper {

    private const val TAG = "CallUiHelper"

    /**
     * Determines whether an incoming call (real or fake) should immediately open full-screen.
     *
     * Returns true (Full-Screen) IF:
     * 1. User enabled "Always Jump to Full Screen" in Settings.
     * 2. The device is on the lockscreen or display is turned off (screen locked/sleeping).
     *
     * Returns false (Heads-Up Notification only) IF:
     * - The user is actively using the phone (screen is on and device is unlocked).
     */
    fun shouldShowFullScreen(context: Context, preferenceManager: PreferenceManager): Boolean {
        // 1. Check user preference toggle
        if (preferenceManager.getBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, false)) {
            Log.d(TAG, "Full-screen enabled via preference")
            return true
        }

        // 2. Lockscreen or screen is off/sleeping
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val isLocked = keyguardManager?.isKeyguardLocked == true ||
                keyguardManager?.inKeyguardRestrictedInputMode() == true ||
                keyguardManager?.isDeviceLocked == true
        val isInteractive = powerManager?.isInteractive == true

        if (isLocked || !isInteractive) {
            Log.d(TAG, "Full-screen: device is locked ($isLocked), screen off (${!isInteractive})")
            return true
        }

        // 3. User is actively using the phone -> show heads-up banner only
        Log.d(TAG, "Heads-up notification only: user is actively using the phone")
        return false
    }

    /**
     * Checks whether the system launcher (home screen) is the current foreground app.
     */
    fun isHomeScreenForeground(context: Context): Boolean {
        val launcherPackages = getLauncherPackages(context)
        if (launcherPackages.isEmpty()) return false

        // A. Check via UsageStatsManager if available
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm?.queryEvents(now - 10000, now)
            if (events != null) {
                var lastResumedPkg: String? = null
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastResumedPkg = event.packageName
                    }
                }
                if (lastResumedPkg != null) {
                    return launcherPackages.contains(lastResumedPkg)
                }
            }
        } catch (e: Throwable) {
            Log.v(TAG, "UsageStats check omitted: ${e.message}")
        }

        // B. Check via ActivityManager running tasks
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            @Suppress("DEPRECATION")
            val tasks = am?.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                val topTask = tasks[0]
                val topPkg = topTask.topActivity?.packageName ?: topTask.baseActivity?.packageName
                if (topPkg != null) {
                    return launcherPackages.contains(topPkg)
                }
            }
        } catch (e: Throwable) {
            Log.v(TAG, "RunningTasks check omitted: ${e.message}")
        }

        return false
    }

    /**
     * Finds installed launcher / home screen packages on the device.
     */
    fun getLauncherPackages(context: Context): Set<String> {
        val packages = mutableSetOf<String>()
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            for (info in resolveInfos) {
                info.activityInfo?.packageName?.let { packages.add(it) }
            }

            val allHomeInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(homeIntent, 0)
            }
            for (info in allHomeInfos) {
                info.activityInfo?.packageName?.let { packages.add(it) }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed resolving launcher packages: ${e.message}")
        }

        // Well-known OEM launchers fallback
        packages.add("com.google.android.apps.nexuslauncher")
        packages.add("com.android.launcher3")
        packages.add("com.miui.home")
        packages.add("com.sec.android.app.launcher")
        packages.add("com.oppo.launcher")
        packages.add("com.oneplus.launcher")
        packages.add("com.huawei.android.launcher")
        packages.add("com.transsion.hilauncher")
        packages.add("com.bbk.launcher2")
        packages.add("com.vivo.launcher")
        packages.add("com.vivo.upslide")

        return packages
    }
}
