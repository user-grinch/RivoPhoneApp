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
            // Prefer the number with a '+' or the longer one (usually more complete)
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
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    
    // For MMI/USSD codes, we need to use Uri.parse with encoded # to tel:%23
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
    val intent = Intent(Intent.ACTION_VIEW,
        link.toUri())
    context.startActivity(intent)
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
        // PackageInfoCompat handles retrieving long version codes safely across old/new API levels
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
