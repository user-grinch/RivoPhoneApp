package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

data class SocialAppInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val iconDrawable: Drawable? = null,
    val assetIcon: String? = null,
    val action: (Context, String) -> Unit
)

object SocialUtils {

    /**
     * Formats a raw phone number to international E.164 format (+15551234567).
     * If formatting fails, cleans formatting symbols and preserves leading '+'.
     */
    fun formatInternationalNumber(context: Context, rawNumber: String): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return ""

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val countryIso = tm?.simCountryIso?.uppercase()?.ifBlank { null }
            ?: tm?.networkCountryIso?.uppercase()?.ifBlank { null }
            ?: Locale.getDefault().country.uppercase().ifBlank { "US" }

        val e164 = try {
            PhoneNumberUtils.formatNumberToE164(trimmed, countryIso)
        } catch (_: Exception) {
            null
        }

        if (!e164.isNullOrBlank()) {
            return e164
        }

        // If it starts with '+', keep '+' and strip any spaces/dashes/parentheses
        return if (trimmed.startsWith("+")) {
            "+" + trimmed.drop(1).replace(Regex("[^0-9]"), "")
        } else {
            trimmed.replace(Regex("[^0-9]"), "")
        }
    }

    /**
     * Extracts only digits without '+' or punctuation for APIs like WhatsApp / Viber.
     */
    fun cleanDigitsOnly(context: Context, rawNumber: String): String {
        val intl = formatInternationalNumber(context, rawNumber)
        return intl.replace(Regex("[^0-9]"), "")
    }

    fun openWhatsApp(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val url = "https://api.whatsapp.com/send?phone=$digits"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            when {
                isPackageInstalled(context, "com.whatsapp") -> setPackage("com.whatsapp")
                isPackageInstalled(context, "com.whatsapp.w4b") -> setPackage("com.whatsapp.w4b")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                intent.setPackage(null)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openTelegram(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val url = "https://t.me/+$digits"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            when {
                isPackageInstalled(context, "org.telegram.messenger") -> setPackage("org.telegram.messenger")
                isPackageInstalled(context, "org.thunderdog.challegram") -> setPackage("org.thunderdog.challegram")
                isPackageInstalled(context, "org.telegram.plus") -> setPackage("org.telegram.plus")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                intent.setPackage(null)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openSignal(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val url = "sgnl://signal.me/#p/+$digits"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (isPackageInstalled(context, "org.thoughtcrime.securesms")) {
                setPackage("org.thoughtcrime.securesms")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://signal.me/#p/+$digits")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun openViber(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val url = "viber://chat?number=%2B$digits"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (isPackageInstalled(context, "com.viber.voip")) {
                setPackage("com.viber.voip")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                intent.setPackage(null)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openSkype(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val url = "skype:+$digits?chat"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (isPackageInstalled(context, "com.skype.raider")) {
                setPackage("com.skype.raider")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                intent.setPackage(null)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openWeChat(context: Context, number: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {}
        }
    }

    fun openLine(context: Context, number: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("jp.naver.line.android")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {}
        }
    }

    fun openMeet(context: Context, number: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.tachyon")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {}
        }
    }

    fun openTruecaller(context: Context, number: String) {
        val digits = cleanDigitsOnly(context, number)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("truecaller://search?q=$digits")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (isPackageInstalled(context, "com.truecaller")) {
                setPackage("com.truecaller")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.truecaller")
            launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent?.let { runCatching { context.startActivity(it) } }
        }
    }

    fun openSms(context: Context, number: String) {
        val intl = formatInternationalNumber(context, number)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$intl")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Queries and returns ONLY the supported social/messaging apps that are
     * actually installed on the user's device.
     */
    fun getInstalledSocialApps(context: Context): List<SocialAppInfo> {
        val pm = context.packageManager
        val list = mutableListOf<SocialAppInfo>()

        // 1. WhatsApp or WhatsApp Business
        val waPkg = when {
            isPackageInstalled(context, "com.whatsapp") -> "com.whatsapp"
            isPackageInstalled(context, "com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> null
        }
        if (waPkg != null) {
            val appInfo = runCatching { pm.getApplicationInfo(waPkg, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "WhatsApp"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "whatsapp",
                    name = label,
                    packageName = waPkg,
                    iconDrawable = icon,
                    assetIcon = "file:///android_asset/icons/whatsapp.png",
                    action = { ctx, num -> openWhatsApp(ctx, num) }
                )
            )
        }

        // 2. Telegram or Telegram X or Plus Messenger
        val tgPkg = when {
            isPackageInstalled(context, "org.telegram.messenger") -> "org.telegram.messenger"
            isPackageInstalled(context, "org.thunderdog.challegram") -> "org.thunderdog.challegram"
            isPackageInstalled(context, "org.telegram.plus") -> "org.telegram.plus"
            else -> null
        }
        if (tgPkg != null) {
            val appInfo = runCatching { pm.getApplicationInfo(tgPkg, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Telegram"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "telegram",
                    name = label,
                    packageName = tgPkg,
                    iconDrawable = icon,
                    assetIcon = "file:///android_asset/icons/telegram.png",
                    action = { ctx, num -> openTelegram(ctx, num) }
                )
            )
        }

        // 3. Signal
        if (isPackageInstalled(context, "org.thoughtcrime.securesms")) {
            val appInfo = runCatching { pm.getApplicationInfo("org.thoughtcrime.securesms", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Signal"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "signal",
                    name = label,
                    packageName = "org.thoughtcrime.securesms",
                    iconDrawable = icon,
                    assetIcon = "file:///android_asset/icons/signal.png",
                    action = { ctx, num -> openSignal(ctx, num) }
                )
            )
        }

        // 4. Viber
        if (isPackageInstalled(context, "com.viber.voip")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.viber.voip", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Viber"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "viber",
                    name = label,
                    packageName = "com.viber.voip",
                    iconDrawable = icon,
                    action = { ctx, num -> openViber(ctx, num) }
                )
            )
        }

        // 5. Skype
        if (isPackageInstalled(context, "com.skype.raider")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.skype.raider", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Skype"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "skype",
                    name = label,
                    packageName = "com.skype.raider",
                    iconDrawable = icon,
                    action = { ctx, num -> openSkype(ctx, num) }
                )
            )
        }

        // 6. WeChat
        if (isPackageInstalled(context, "com.tencent.mm")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.tencent.mm", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "WeChat"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "wechat",
                    name = label,
                    packageName = "com.tencent.mm",
                    iconDrawable = icon,
                    action = { ctx, num -> openWeChat(ctx, num) }
                )
            )
        }

        // 7. LINE
        if (isPackageInstalled(context, "jp.naver.line.android")) {
            val appInfo = runCatching { pm.getApplicationInfo("jp.naver.line.android", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "LINE"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "line",
                    name = label,
                    packageName = "jp.naver.line.android",
                    iconDrawable = icon,
                    action = { ctx, num -> openLine(ctx, num) }
                )
            )
        }

        // 8. Meet
        if (isPackageInstalled(context, "com.google.android.apps.tachyon")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.google.android.apps.tachyon", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Meet"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "meet",
                    name = label,
                    packageName = "com.google.android.apps.tachyon",
                    iconDrawable = icon,
                    action = { ctx, num -> openMeet(ctx, num) }
                )
            )
        }

        // 9. Truecaller
        if (isPackageInstalled(context, "com.truecaller")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.truecaller", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Truecaller"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                SocialAppInfo(
                    id = "truecaller",
                    name = label,
                    packageName = "com.truecaller",
                    iconDrawable = icon,
                    action = { ctx, num -> openTruecaller(ctx, num) }
                )
            )
        }

        return list
    }
}

