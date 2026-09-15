package com.grinch.rivo4.controller.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object OemPermissionHelper {

    private const val TAG = "OemPermissionHelper"

    fun isXiaomi(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("xiaomi") || b.contains("xiaomi") || b.contains("redmi") || b.contains("poco")
    }

    fun isHuawei(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("huawei") || b.contains("huawei")
    }

    fun isHonor(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("honor") || b.contains("honor")
    }

    fun isOppo(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("oppo") || b.contains("oppo")
    }

    fun isRealme(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("realme") || b.contains("realme")
    }

    fun isOnePlus(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("oneplus") || b.contains("oneplus")
    }

    fun isVivo(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("vivo") || b.contains("vivo") || b.contains("iqoo")
    }

    fun isSamsung(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("samsung") || b.contains("samsung")
    }

    fun isTranssion(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("transsion") || b.contains("infinix") || b.contains("tecno") || b.contains("itel")
    }

    fun isAsus(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("asus") || b.contains("asus")
    }

    fun isMeizu(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("meizu") || b.contains("meizu")
    }

    fun isMotorola(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("motorola") || b.contains("lenovo")
    }

    fun isOemDevice(): Boolean {
        return isXiaomi() || isHuawei() || isHonor() || isOppo() || isRealme() ||
                isOnePlus() || isVivo() || isSamsung() || isTranssion() || isAsus() || isMeizu()
    }

    fun getOemBrandDisplayName(): String {
        return when {
            isXiaomi() -> "Xiaomi (MIUI / HyperOS)"
            isSamsung() -> "Samsung (One UI)"
            isOppo() -> "Oppo (ColorOS)"
            isRealme() -> "Realme (Realme UI)"
            isOnePlus() -> "OnePlus (OxygenOS)"
            isVivo() -> "Vivo (Funtouch OS / OriginOS)"
            isHuawei() -> "Huawei (EMUI / HarmonyOS)"
            isHonor() -> "Honor (MagicOS)"
            isTranssion() -> "Tecno / Infinix (HiOS / XOS)"
            isAsus() -> "Asus (ZenUI / ROG UI)"
            isMeizu() -> "Meizu (Flyme)"
            isMotorola() -> "Motorola"
            else -> Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Attempts to open the OEM-specific permissions screen (e.g. background audio, floating window).
     */
    fun openOemPermissions(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        if (isXiaomi()) {
            // MIUI "Other permissions" editor
            intents.add(
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                    putExtra("extra_pkgname", context.packageName)
                }
            )
            intents.add(
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setPackage("com.miui.securitycenter")
                    putExtra("extra_pkgname", context.packageName)
                }
            )
            intents.add(
                Intent().apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                    putExtra("extra_pkgname", context.packageName)
                }
            )
        }

        if (isOppo() || isRealme() || isOnePlus()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionManagerActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionTopActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.PermissionManagerActivity"))
                }
            )
        }

        if (isVivo()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"))
                    putExtra("packagename", context.packageName)
                    putExtra("packageName", context.packageName)
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.vivo.permissioncontroller", "com.vivo.permissioncontroller.activity.SoftPermissionDetailActivity"))
                    putExtra("packagename", context.packageName)
                    putExtra("packageName", context.packageName)
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"))
                    putExtra("packagename", context.packageName)
                    putExtra("packageName", context.packageName)
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.FloatWindowManager"))
                    putExtra("packagename", context.packageName)
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                intents.add(
                    Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
            intents.add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }

        if (isMeizu()) {
            intents.add(
                Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
                    putExtra("packageName", context.packageName)
                }
            )
        }

        // Standard App Info fallback
        intents.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        return launchFirstWorkingIntent(context, intents)
    }

    /**
     * Alias for opening OEM background pop-up / lockscreen permission screen (e.g. Vivo/iQOO).
     */
    fun openBackgroundPopupPermission(context: Context): Boolean = openOemPermissions(context)

    /**
     * Attempts to launch the Autostart settings across all OEMs.
     */
    fun openAutostartSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        if (isXiaomi()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                }
            )
        }

        if (isOppo() || isRealme()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"))
                }
            )
        }

        if (isVivo()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"))
                }
            )
        }

        if (isHuawei()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
                }
            )
        }

        if (isHonor()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.optimize.bootstart.BootStartActivity"))
                }
            )
        }

        if (isSamsung()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.AppSleepListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity"))
                }
            )
        }

        if (isTranssion()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.AutoStartActivity"))
                }
            )
        }

        if (isAsus()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"))
                }
            )
        }

        if (isOnePlus()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
                }
            )
        }

        // Fallback to app details
        intents.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        return launchFirstWorkingIntent(context, intents)
    }

    /**
     * Opens battery saver settings across all OEMs.
     */
    fun openBatterySaverSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        if (isXiaomi()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                    putExtra("package_name", context.packageName)
                    putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
                }
            )
        }

        if (isSamsung()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
                }
            )
        }

        if (isHuawei()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"))
                }
            )
        }

        if (isHonor()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.power.ui.HwPowerManagerActivity"))
                }
            )
        }

        if (isOppo() || isRealme()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionTopActivity"))
                }
            )
        }

        if (isVivo()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
                }
            )
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.vivo.abe", "com.vivo.abe.feature.screen.service.BatteryPerfDetailsActivity"))
                }
            )
        }

        if (isAsus()) {
            intents.add(
                Intent().apply {
                    setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings"))
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                intents.add(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
            intents.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        intents.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        return launchFirstWorkingIntent(context, intents)
    }

    private fun launchFirstWorkingIntent(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.v(TAG, "Intent failed: ${intent.component?.className ?: intent.action}")
            }
        }
        return false
    }
}
