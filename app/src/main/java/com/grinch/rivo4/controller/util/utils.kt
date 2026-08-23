package com.grinch.rivo4.controller.util
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.grinch.rivo4.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isYesterday(timestamp: Long): Boolean {
    return DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)
}

private fun isSameYear(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

private fun getRelativeDay(context: Context, timestamp: Long): String? {
    return when {
        DateUtils.isToday(timestamp) -> context.getString(R.string.date_today)
        isYesterday(timestamp) -> context.getString(R.string.date_yesterday)
        else -> null
    }
}

fun formatDateHeader(context: Context, timestamp: Long): String {
    val relative = getRelativeDay(context, timestamp)
    if (relative != null) return relative

    val pattern = if (isSameYear(timestamp, System.currentTimeMillis())) "MMMM d" else "MMMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

fun formatDate(context: Context, timestamp: Long): String {
    val relative = getRelativeDay(context, timestamp)
    val time = android.text.format.DateFormat.getTimeFormat(context).format(Date(timestamp))

    return if (relative != null) "$relative, $time" else "${formatDateHeader(context, timestamp)}, $time"
}

fun formatTime(context: Context, timestamp: Long): String {
    val time = android.text.format.DateFormat.getTimeFormat(context).format(Date(timestamp))
    return "$time"
}

fun formatDuration(durationSeconds: Long): String {
    return DateUtils.formatElapsedTime(durationSeconds)
}

fun formatPhoneNumber(number: String): String {
    return PhoneNumberUtils.formatNumber(number, Locale.getDefault().country) ?: number
}

fun normalizePhoneNumber(number: String): String {
    return PhoneNumberUtils.normalizeNumber(number)
}

fun areNumbersEqual(num1: String?, num2: String?): Boolean {
    if (num1 == null || num2 == null) return false
    return PhoneNumberUtils.compare(num1, num2)
}

fun deduplicateNumbers(numbers: List<String>): List<String> {
    val unique = mutableListOf<String>()
    numbers.forEach { number ->
        val existingIndex = unique.indexOfFirst { areNumbersEqual(it, number) }
        if (existingIndex == -1) {
            unique.add(number)
        } else {
            val existing = unique[existingIndex]
            if (number.contains("+") && !existing.contains("+")) {
                unique[existingIndex] = number
            } else if (number.length > existing.length && (number.contains("+") == existing.contains("+"))) {
                unique[existingIndex] = number
            }
        }
    }
    return unique
}

fun isAirplaneModeOn(context: Context): Boolean {
    return try {
        android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    } catch (e: Exception) {
        false
    }
}

fun isVoicemailNumber(context: Context, number: String?): Boolean {
    if (number.isNullOrBlank()) return false
    val clean = number.trim()
    if (clean.equals("voicemail", ignoreCase = true) || clean.startsWith("voicemail:", ignoreCase = true)) {
        return true
    }
    val prefs = PreferenceManager(context)
    val configuredVm = prefs.getString(PreferenceManager.KEY_VOICEMAIL_NUMBER, null)
    if (!configuredVm.isNullOrBlank() && areNumbersEqual(clean, configuredVm)) {
        return true
    }
    val sysVm = getSystemVoicemailNumber(context)
    if (!sysVm.isNullOrBlank() && areNumbersEqual(clean, sysVm)) {
        return true
    }
    return try {
        @Suppress("DEPRECATION")
        PhoneNumberUtils.isVoiceMailNumber(clean)
    } catch (e: Exception) {
        false
    }
}

fun getSystemVoicemailNumber(context: Context): String? {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        try {
            val accounts = telecomManager.callCapablePhoneAccounts
            val defaultHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telecomManager.getDefaultOutgoingPhoneAccount(Uri.fromParts("tel", "123", null).scheme)
            } else null
            
            val handle = defaultHandle ?: accounts.firstOrNull()
            if (handle != null) {
                val num = telecomManager.getVoiceMailNumber(handle)
                if (!num.isNullOrEmpty()) return num
            }
        } catch (e: SecurityException) {
        } catch (e: Exception) {}
        
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val num = tm.voiceMailNumber
            if (!num.isNullOrEmpty()) return num
        } catch (e: SecurityException) {
        } catch (e: Exception) {}
    }
    return null
}

fun makeCall(context: Context, number: String, accountHandle: PhoneAccountHandle? = null, contactId: String? = null) {
    if (isAirplaneModeOn(context)) {
        android.widget.Toast.makeText(context, context.getString(R.string.call_failed_airplane_mode), android.widget.Toast.LENGTH_LONG).show()
        return
    }

    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    
    val uri = if (number.startsWith("voicemail:")) {
        Uri.parse(number)
    } else if (number.contains("#")) {
        Uri.parse("tel:" + Uri.encode(number))
    } else {
        Uri.fromParts("tel", number, null)
    }
    val extras = Bundle()
    
    val prefs = PreferenceManager(context)
    if (contactId != null) {
        prefs.setLastUsedNumber(contactId, number)
    }

    var preferredHandle = accountHandle
    if (preferredHandle == null) {
        val accounts = if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try { telecomManager.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }
        } else emptyList()

        val favSim = contactId?.let { prefs.getFavoriteSim(it) }
        val favNum = contactId?.let { prefs.getFavoriteNumber(it) }
        
        preferredHandle = if (favSim != null && areNumbersEqual(number, favNum)) {
            accounts.find { it.id == favSim }
        } else null

        if (preferredHandle == null) {
            val defaultSim = prefs.getInt("default_sim", 0)
            if (defaultSim > 0 && accounts.size >= defaultSim) {
                preferredHandle = accounts[defaultSim - 1]
            }
        }
    }

    if (preferredHandle != null) {
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, preferredHandle)
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
        telecomManager.placeCall(uri, extras)
    } else {
        
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

fun openInContacts(context: Context, contactId: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
    }
    context.startActivity(intent)
}

fun openLink(context: Context, link: String) {
    try {
        val uri = link.toUri()
        val intent = if (uri.scheme == "tel") {
            Intent(Intent.ACTION_DIAL, uri)
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getAppVersion(context: Context): Pair<String, Long> {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        val versionName = packageInfo.versionName ?: context.getString(R.string.label_unknown)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

        Pair(versionName, versionCode)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        Pair(context.getString(R.string.label_unknown), -1L)
    }
}

fun isAlreadyDefaultDialer(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val defaultDialerPackage = telecomManager.defaultDialerPackage
        defaultDialerPackage == context.packageName
    }
}

fun getDefaultDialerIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    } else {
        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }
}

data class DeviceImeiInfo(
    val imei1: String? = null,
    val imei2: String? = null,
    val meid: String? = null,
    val serial: String? = null
)

fun getDeviceImeiInfo(context: Context): DeviceImeiInfo {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    var imei1: String? = null
    var imei2: String? = null
    var meid: String? = null
    var serial: String? = null

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei1 = try { tm.getImei(0) } catch (e: Exception) { null }
                imei2 = try { tm.getImei(1) } catch (e: Exception) { null }
                meid = try { tm.getMeid() } catch (e: Exception) { null }
            }
            if (imei1.isNullOrEmpty()) {
                @Suppress("DEPRECATION")
                imei1 = try { tm.deviceId } catch (e: Exception) { null }
            }
            @Suppress("DEPRECATION")
            serial = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Build.getSerial()
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    Build.SERIAL
                } else null
            } catch (e: Exception) { null }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return DeviceImeiInfo(imei1 = imei1, imei2 = imei2, meid = meid, serial = serial)
}

fun processSecretCode(context: Context, fullCode: String): Boolean {
    val cleanNumber = fullCode.replace(" ", "")
    var code: String? = null

    if (cleanNumber.startsWith("*#*#") && cleanNumber.endsWith("#*#*") && cleanNumber.length > 8) {
        code = cleanNumber.substring(4, cleanNumber.length - 4)
    } else if (cleanNumber.startsWith("##") && cleanNumber.endsWith("#") && cleanNumber.length >= 4) {
        code = cleanNumber.replace("#", "")
    } else if (cleanNumber.startsWith("*#") && cleanNumber.endsWith("#") && cleanNumber.length >= 3) {
        code = cleanNumber.substring(2, cleanNumber.length - 1)
    }

    if (code.isNullOrEmpty()) return false

    var handled = false

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.sendDialerSpecialCode(code)
            handled = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val actionSecretCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TelephonyManager.ACTION_SECRET_CODE
        } else {
            "android.telephony.action.SECRET_CODE"
        }
        val intent1 = Intent(actionSecretCode).apply {
            data = Uri.parse("android_secret_code://$code")
        }
        context.sendBroadcast(intent1)
        handled = true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val intent2 = Intent("android.provider.Telephony.SECRET_CODE").apply {
            data = Uri.parse("android_secret_code://$code")
        }
        context.sendBroadcast(intent2)
        handled = true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    when (code) {
        "4636" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.android.settings", "com.android.settings.Settings\$TestingSettingsActivity"),
                Intent(Intent.ACTION_MAIN).setClassName("com.android.settings", "com.android.settings.RadioInfo"),
                Intent("android.intent.action.MAIN").setClassName("com.android.settings", "com.android.settings.TestingSettings")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "07" -> {
            val targets = listOf(
                Intent("android.settings.REGULATORY_INFO"),
                Intent(Intent.ACTION_MAIN).setClassName("com.android.settings", "com.android.settings.Settings\$RegulatoryInfoActivity")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "0", "0*" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.hwmoduletest", "com.sec.android.app.hwmoduletest.HwModuleTest"),
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.factory", "com.sec.factory.main"),
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.ServiceModeApp")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "0228" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.BatteryStatus"),
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.ServiceModeApp")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "9900" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.SysDump", "com.sec.android.SysDump.SysDump")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "1234" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.VersionInfo")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "0808" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.parser", "com.sec.android.app.parser.UsbSettings"),
                Intent(Intent.ACTION_MAIN).setClassName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.UsbSettings")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "800", "808", "888", "899", "6776" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.oplus.logkit", "com.oplus.logkit.LogKitMainActivity"),
                Intent(Intent.ACTION_MAIN).setClassName("com.oplus.engineermode", "com.oplus.engineermode.Engineermode"),
                Intent(Intent.ACTION_MAIN).setClassName("com.oneplus.factorymode", "com.oneplus.factorymode.FactoryModeMain")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "2846579", "0000" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.huawei.projectmenu", "com.huawei.projectmenu.ProjectMenu"),
                Intent(Intent.ACTION_MAIN).setClassName("com.huawei.settings.projectmenu", "com.huawei.settings.projectmenu.ProjectMenu")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "2486" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.motorola.cqatest", "com.motorola.cqatest.CQATest")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "7378423" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.sonyericsson.android.servicemenu", "com.sonyericsson.android.servicemenu.ServiceMenu")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "3646633" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "6484", "64663" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.miui.cit", "com.miui.cit.CitLauncherActivity"),
                Intent(Intent.ACTION_MAIN).setClassName("com.miui.cit", "com.miui.cit.CitTestActivity")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "225" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.android.providers.calendar", "com.android.providers.calendar.CalendarDebugActivity")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "426" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.google.android.gms", "com.google.android.gms.gcm.GcmDiagnostics"),
                Intent(Intent.ACTION_MAIN).setClassName("com.google.android.gms", "com.google.android.gms.cloudmessaging.CloudMessagingDiagnostics")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
        "759" -> {
            val targets = listOf(
                Intent(Intent.ACTION_MAIN).setClassName("com.google.android.apps.rlz", "com.google.android.apps.rlz.DebugActivity"),
                Intent(Intent.ACTION_MAIN).setClassName("com.google.android.partnersetup", "com.google.android.partnersetup.RlzDebugActivity")
            )
            for (target in targets) {
                try {
                    target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(target)
                    handled = true
                    break
                } catch (e: Exception) {}
            }
        }
    }

    return handled
}

fun isCustomPermissionDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return manufacturer.contains("xiaomi") ||
            manufacturer.contains("oppo") ||
            manufacturer.contains("vivo") ||
            manufacturer.contains("realme") ||
            manufacturer.contains("huawei") ||
            manufacturer.contains("meizu") ||
            manufacturer.contains("oneplus")
}
