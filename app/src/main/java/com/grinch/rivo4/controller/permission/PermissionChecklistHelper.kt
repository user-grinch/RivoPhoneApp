package com.grinch.rivo4.controller.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.grinch.rivo4.controller.util.getDefaultDialerIntent
import com.grinch.rivo4.controller.util.isAlreadyDefaultDialer

enum class PermissionActionType {
    RUNTIME,
    ROLE_DIALER,
    OVERLAY,
    BATTERY_OPTIMIZATION,
    SETTINGS
}

data class PermissionCheckItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isEssential: Boolean,
    val actionType: PermissionActionType,
    val permissions: List<String> = emptyList()
)

object PermissionChecklistHelper {

    val ESSENTIAL_RUNTIME_PERMISSIONS = mutableListOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }.toTypedArray()

    fun isDefaultDialer(context: Context): Boolean {
        return isAlreadyDefaultDialer(context)
    }

    fun hasPhonePermission(context: Context): Boolean {
        val callPhone = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val readPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val answerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
        } else true
        return callPhone && readPhoneState && answerCalls
    }

    fun hasContactsPermission(context: Context): Boolean {
        val readContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val writeContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return readContacts && writeContacts
    }

    fun hasCallLogPermission(context: Context): Boolean {
        val readLogs = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val writeLogs = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        return readLogs && writeLogs
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasAudioRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun areAllEssentialGranted(context: Context): Boolean {
        return isDefaultDialer(context) &&
                hasPhonePermission(context) &&
                hasContactsPermission(context) &&
                hasCallLogPermission(context)
    }

    fun getEssentialItems(context: Context): List<PermissionCheckItem> {
        val phonePermissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }

        return listOf(
            PermissionCheckItem(
                id = "default_dialer",
                title = "Default Phone App",
                description = "Required to answer calls, handle incoming phone calls, and manage call blocking.",
                icon = Icons.Outlined.VerifiedUser,
                isGranted = isDefaultDialer(context),
                isEssential = true,
                actionType = PermissionActionType.ROLE_DIALER
            ),
            PermissionCheckItem(
                id = "phone_state",
                title = "Phone & SIM Access",
                description = "Required to place calls directly, manage dual SIM cards, and detect call status.",
                icon = Icons.Outlined.Call,
                isGranted = hasPhonePermission(context),
                isEssential = true,
                actionType = PermissionActionType.RUNTIME,
                permissions = phonePermissions
            ),
            PermissionCheckItem(
                id = "contacts",
                title = "Contacts",
                description = "Required to display contact names, show caller info, and search contacts via T9 dialpad.",
                icon = Icons.Outlined.Contacts,
                isGranted = hasContactsPermission(context),
                isEssential = true,
                actionType = PermissionActionType.RUNTIME,
                permissions = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
            ),
            PermissionCheckItem(
                id = "call_log",
                title = "Call History",
                description = "Required to display your incoming, outgoing, and missed call history in Recents.",
                icon = Icons.Outlined.History,
                isGranted = hasCallLogPermission(context),
                isEssential = true,
                actionType = PermissionActionType.RUNTIME,
                permissions = listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG)
            )
        )
    }

    fun getRecommendedItems(context: Context): List<PermissionCheckItem> {
        val items = mutableListOf<PermissionCheckItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items.add(
                PermissionCheckItem(
                    id = "notifications",
                    title = "Call Notifications",
                    description = "Show incoming call banners, active call status, and missed call reminders.",
                    icon = Icons.Outlined.Notifications,
                    isGranted = hasNotificationPermission(context),
                    isEssential = false,
                    actionType = PermissionActionType.RUNTIME,
                    permissions = listOf(Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        }

        items.add(
            PermissionCheckItem(
                id = "overlay",
                title = "Floating Call Bubble",
                description = "Multitask while on a call with a floating pill overlay with mute, speaker, and end call controls.",
                icon = Icons.Outlined.PictureInPicture,
                isGranted = hasOverlayPermission(context),
                isEssential = false,
                actionType = PermissionActionType.OVERLAY
            )
        )

        items.add(
            PermissionCheckItem(
                id = "battery",
                title = "Reliable Background Calls",
                description = "Prevents system battery saver from suppressing incoming calls when your screen is locked.",
                icon = Icons.Outlined.BatteryChargingFull,
                isGranted = isBatteryOptimizationIgnored(context),
                isEssential = false,
                actionType = PermissionActionType.BATTERY_OPTIMIZATION
            )
        )

        items.add(
            PermissionCheckItem(
                id = "audio_recording",
                title = "Call Recording",
                description = "Record incoming and outgoing phone calls and voice notes directly within the app.",
                icon = Icons.Outlined.Mic,
                isGranted = hasAudioRecordPermission(context),
                isEssential = false,
                actionType = PermissionActionType.RUNTIME,
                permissions = listOf(Manifest.permission.RECORD_AUDIO)
            )
        )

        return items
    }

    fun getOverlayIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}
