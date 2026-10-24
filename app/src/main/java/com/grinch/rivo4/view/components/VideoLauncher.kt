package com.grinch.rivo4.view.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import com.grinch.rivo4.controller.CallActivity
import com.grinch.rivo4.controller.CallService
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermPhoneMsg
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.modal.data.Contact
import org.koin.compose.koinInject

data class VideoAppOption(
    val id: String,
    val name: String,
    val packageName: String?,
    val iconDrawable: Drawable? = null,
    val iconVector: ImageVector? = null,
    val description: String
)

object VideoCallHelper {

    fun getAvailableVideoApps(context: Context): List<VideoAppOption> {
        val pm = context.packageManager
        val list = mutableListOf<VideoAppOption>()

        // 1. Carrier / System Video Call (Telecom / ViLTE)
        list.add(
            VideoAppOption(
                id = "system",
                name = context.getString(R.string.video_call_carrier),
                packageName = null,
                iconVector = Icons.Default.PermPhoneMsg,
                description = context.getString(R.string.video_call_carrier_desc)
            )
        )

        // 2. WhatsApp or WhatsApp Business
        val waPkg = when {
            SocialUtils.isPackageInstalled(context, "com.whatsapp") -> "com.whatsapp"
            SocialUtils.isPackageInstalled(context, "com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> null
        }
        if (waPkg != null) {
            val appInfo = runCatching { pm.getApplicationInfo(waPkg, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "WhatsApp"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "whatsapp",
                    name = label,
                    packageName = waPkg,
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_whatsapp_desc)
                )
            )
        }

        // 3. Google Meet
        val meetPkg = when {
            SocialUtils.isPackageInstalled(context, "com.google.android.apps.tachyon") -> "com.google.android.apps.tachyon"
            SocialUtils.isPackageInstalled(context, "com.google.android.apps.meetings") -> "com.google.android.apps.meetings"
            else -> null
        }
        if (meetPkg != null) {
            val appInfo = runCatching { pm.getApplicationInfo(meetPkg, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Google Meet"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "meet",
                    name = label,
                    packageName = meetPkg,
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_meet_desc)
                )
            )
        }

        // 4. Telegram
        val tgPkg = when {
            SocialUtils.isPackageInstalled(context, "org.telegram.messenger") -> "org.telegram.messenger"
            SocialUtils.isPackageInstalled(context, "org.telegram.messenger.web") -> "org.telegram.messenger.web"
            SocialUtils.isPackageInstalled(context, "org.thunderdog.challegram") -> "org.thunderdog.challegram"
            SocialUtils.isPackageInstalled(context, "org.telegram.plus") -> "org.telegram.plus"
            else -> null
        }
        if (tgPkg != null) {
            val appInfo = runCatching { pm.getApplicationInfo(tgPkg, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Telegram"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "telegram",
                    name = label,
                    packageName = tgPkg,
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_telegram_desc)
                )
            )
        }

        // 5. Signal
        if (SocialUtils.isPackageInstalled(context, "org.thoughtcrime.securesms")) {
            val appInfo = runCatching { pm.getApplicationInfo("org.thoughtcrime.securesms", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Signal"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "signal",
                    name = label,
                    packageName = "org.thoughtcrime.securesms",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_signal_desc)
                )
            )
        }

        // 6. WeChat
        if (SocialUtils.isPackageInstalled(context, "com.tencent.mm")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.tencent.mm", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "WeChat"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "wechat",
                    name = label,
                    packageName = "com.tencent.mm",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_wechat_desc)
                )
            )
        }

        // 7. LINE
        if (SocialUtils.isPackageInstalled(context, "jp.naver.line.android")) {
            val appInfo = runCatching { pm.getApplicationInfo("jp.naver.line.android", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "LINE"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "line",
                    name = label,
                    packageName = "jp.naver.line.android",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_line_desc)
                )
            )
        }

        // 8. Viber
        if (SocialUtils.isPackageInstalled(context, "com.viber.voip")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.viber.voip", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Viber"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "viber",
                    name = label,
                    packageName = "com.viber.voip",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_viber_desc)
                )
            )
        }

        // 9. Skype
        if (SocialUtils.isPackageInstalled(context, "com.skype.raider")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.skype.raider", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Skype"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "skype",
                    name = label,
                    packageName = "com.skype.raider",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_skype_desc)
                )
            )
        }

        // 10. imo
        if (SocialUtils.isPackageInstalled(context, "com.imo.android.imoim")) {
            val appInfo = runCatching { pm.getApplicationInfo("com.imo.android.imoim", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "imo"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "imo",
                    name = label,
                    packageName = "com.imo.android.imoim",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_imo_desc)
                )
            )
        }

        // 11. Zoom
        if (SocialUtils.isPackageInstalled(context, "us.zoom.videomeetings")) {
            val appInfo = runCatching { pm.getApplicationInfo("us.zoom.videomeetings", 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Zoom"
            val icon = appInfo?.let { pm.getApplicationIcon(it) }
            list.add(
                VideoAppOption(
                    id = "zoom",
                    name = label,
                    packageName = "us.zoom.videomeetings",
                    iconDrawable = icon,
                    description = context.getString(R.string.video_call_zoom_desc)
                )
            )
        }

        return list
    }

    fun launchVideoCall(
        context: Context,
        optionId: String,
        number: String,
        contact: Contact?
    ) {
        val resolvedContactId = contact?.id?.takeIf { it.isNotBlank() && it != "null" }
            ?: resolveContactIdForNumber(context, number)

        when (optionId) {
            "system" -> launchCarrierVideoCall(context, number, resolvedContactId)
            "whatsapp" -> launchWhatsAppDataVideoCall(context, number, resolvedContactId)
            "meet" -> launchMeetVideoCall(context, number, resolvedContactId)
            "telegram" -> launchTelegramVideoCall(context, number, resolvedContactId)
            "signal" -> launchSignalVideoCall(context, number, resolvedContactId)
            "wechat" -> launchWeChatVideoCall(context, number, resolvedContactId)
            "line" -> launchLineVideoCall(context, number, resolvedContactId)
            "viber" -> launchViberVideoCall(context, number, resolvedContactId)
            "skype" -> launchSkypeVideoCall(context, number)
            "imo" -> launchImoVideoCall(context, number, resolvedContactId)
            "zoom" -> launchZoomVideoCall(context)
            else -> launchCarrierVideoCall(context, number, resolvedContactId)
        }
    }

    fun resolveContactIdForNumber(context: Context, number: String): String? {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(trimmed)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.CONTACT_ID, ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)
                        .takeIf { it >= 0 } ?: cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (colIdx >= 0) cursor.getLong(colIdx).toString() else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun findContactDataRow(
        context: Context,
        number: String,
        contactId: String?,
        mimeTypes: List<String>
    ): Pair<Uri, String>? {
        if (mimeTypes.isEmpty()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val resolver = context.contentResolver
        val resolvedId = contactId?.takeIf { it.isNotBlank() && it != "null" }
            ?: resolveContactIdForNumber(context, number)

        val mimePlaceholders = mimeTypes.joinToString(" OR ") { "${ContactsContract.Data.MIMETYPE} = ?" }

        if (!resolvedId.isNullOrBlank()) {
            try {
                val args = arrayOf(resolvedId) + mimeTypes.toTypedArray()
                val cursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ($mimePlaceholders)",
                    args,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Data._ID))
                        val mime = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE))
                        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id) to mime
                    }
                }
            } catch (_: Exception) {}
        }

        val cleanDigits = SocialUtils.cleanDigitsOnly(context, number)
        if (cleanDigits.length >= 7) {
            val last7 = cleanDigits.takeLast(7)
            try {
                val args = mimeTypes.toTypedArray() + arrayOf("%$last7%", "%$last7%")
                val cursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
                    "($mimePlaceholders) AND (${ContactsContract.Data.DATA1} LIKE ? OR ${ContactsContract.Data.DATA3} LIKE ?)",
                    args,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Data._ID))
                        val mime = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE))
                        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id) to mime
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun launchCarrierVideoCall(context: Context, number: String, contactId: String? = null) {
        val trimmed = number.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, context.getString(R.string.video_call_no_number), Toast.LENGTH_SHORT).show()
            return
        }

        // If there is already an active call with this number, upgrade it to bidirectional video
        val existingCall = try {
            CallService.allCalls.value.firstOrNull { call ->
                val handleNum = call.details?.handle?.schemeSpecificPart.orEmpty()
                handleNum.isNotBlank() && PhoneNumberUtils.compare(handleNum, trimmed)
            }
        } catch (_: Exception) { null }

        if (existingCall?.videoCall != null) {
            try {
                existingCall.videoCall.sendSessionModifyRequest(VideoProfile(VideoProfile.STATE_BIDIRECTIONAL))
                val callIntent = Intent(context, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(callIntent)
                return
            } catch (_: Exception) {}
        }

        val uri = if (trimmed.contains("#")) {
            Uri.parse("tel:" + Uri.encode(trimmed))
        } else {
            Uri.fromParts("tel", trimmed, null)
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val extras = Bundle().apply {
            putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
            putBoolean("videocall", true)
        }

        if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val accounts = telecomManager.callCapablePhoneAccounts
                val prefs = PreferenceManager(context)
                val favSim = contactId?.let { prefs.getFavoriteSim(it) }
                val userPreferred = if (favSim != null) {
                    accounts.find { it.id == favSim }
                } else {
                    val defaultSim = prefs.getInt("default_sim", 0)
                    if (defaultSim > 0 && accounts.size >= defaultSim) accounts[defaultSim - 1] else null
                }

                // Prefer user's chosen SIM if it supports video calling, otherwise fall back to any SIM with CAPABILITY_VIDEO_CALLING, or user's chosen SIM
                val videoCapableAccount = accounts.firstOrNull { handle ->
                    val account = runCatching { telecomManager.getPhoneAccount(handle) }.getOrNull()
                    account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                }
                val selectedHandle = if (userPreferred != null) {
                    val acct = runCatching { telecomManager.getPhoneAccount(userPreferred) }.getOrNull()
                    if (acct?.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING) == true || videoCapableAccount == null) {
                        userPreferred
                    } else {
                        videoCapableAccount
                    }
                } else {
                    videoCapableAccount
                }

                if (selectedHandle != null) {
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedHandle)
                }
            } catch (_: Exception) {}
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission && telecomManager != null) {
            try {
                telecomManager.placeCall(uri, extras)
                return
            } catch (_: Exception) {}

            try {
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    putExtras(extras)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                return
            } catch (_: Exception) {}
        }

        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            putExtras(extras)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(dialIntent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.video_call_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun launchWhatsAppDataVideoCall(context: Context, number: String, contactId: String?) {
        val waPkg = when {
            SocialUtils.isPackageInstalled(context, "com.whatsapp") -> "com.whatsapp"
            SocialUtils.isPackageInstalled(context, "com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> null
        } ?: run {
            SocialUtils.openWhatsApp(context, number)
            return
        }

        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.com.whatsapp.video.call",
                "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"
            )
        )
        if (row != null) {
            val (directUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(directUri, mimeType)
                setPackage(waPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        // Fallback: Open WhatsApp conversation with caller
        Toast.makeText(context, context.getString(R.string.video_call_opening_whatsapp), Toast.LENGTH_SHORT).show()
        SocialUtils.openWhatsApp(context, number)
    }

    fun findWhatsAppVideoDataUri(context: Context, number: String, contactId: String?): Uri? {
        return findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.com.whatsapp.video.call",
                "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"
            )
        )?.first
    }

    fun launchMeetVideoCall(context: Context, number: String, contactId: String?) {
        val meetPkg = when {
            SocialUtils.isPackageInstalled(context, "com.google.android.apps.tachyon") -> "com.google.android.apps.tachyon"
            SocialUtils.isPackageInstalled(context, "com.google.android.apps.meetings") -> "com.google.android.apps.meetings"
            else -> null
        }

        val e164 = SocialUtils.formatInternationalNumber(context, number)

        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.google.android.apps.tachyon.phone",
                "vnd.android.cursor.item/vnd.google.android.apps.tachyon.video"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                if (meetPkg != null) setPackage(meetPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        if (meetPkg != null && e164.isNotBlank()) {
            val duoCallIntent = Intent("com.google.android.apps.tachyon.action.CALL", Uri.parse("tel:$e164")).apply {
                setPackage(meetPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(duoCallIntent)
                return
            } catch (_: Exception) {}

            val meetUri = Uri.parse("https://meet.google.com/call?number=$e164")
            val meetIntent = Intent(Intent.ACTION_VIEW, meetUri).apply {
                setPackage(meetPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(meetIntent)
                return
            } catch (_: Exception) {}

            val telIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:$e164")).apply {
                setPackage(meetPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(telIntent)
                return
            } catch (_: Exception) {}
        }

        if (meetPkg != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(meetPkg)
            launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                launchIntent?.let { context.startActivity(it) }
                return
            } catch (_: Exception) {}
        }

        Toast.makeText(context, context.getString(R.string.video_call_failed), Toast.LENGTH_SHORT).show()
    }

    fun launchTelegramVideoCall(context: Context, number: String, contactId: String? = null) {
        val tgPkg = when {
            SocialUtils.isPackageInstalled(context, "org.telegram.messenger") -> "org.telegram.messenger"
            SocialUtils.isPackageInstalled(context, "org.telegram.messenger.web") -> "org.telegram.messenger.web"
            SocialUtils.isPackageInstalled(context, "org.thunderdog.challegram") -> "org.thunderdog.challegram"
            SocialUtils.isPackageInstalled(context, "org.telegram.plus") -> "org.telegram.plus"
            else -> null
        }

        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call.video",
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call",
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                if (tgPkg != null) setPackage(tgPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val digits = SocialUtils.cleanDigitsOnly(context, number)
        if (digits.isBlank()) return

        val tgSchemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$digits")).apply {
            if (tgPkg != null) setPackage(tgPkg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(tgSchemeIntent)
            return
        } catch (_: Exception) {}

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$digits")).apply {
            if (tgPkg != null) setPackage(tgPkg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            SocialUtils.openTelegram(context, number)
        }
    }

    fun launchSignalVideoCall(context: Context, number: String, contactId: String? = null) {
        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call",
                "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                setPackage("org.thoughtcrime.securesms")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        SocialUtils.openSignal(context, number)
    }

    fun launchWeChatVideoCall(context: Context, number: String, contactId: String? = null) {
        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.voip.video",
                "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.voip",
                "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.profile"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                setPackage("com.tencent.mm")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val weixinIntent = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://")).apply {
            setPackage("com.tencent.mm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(weixinIntent)
            return
        } catch (_: Exception) {}

        SocialUtils.openWeChat(context, number)
    }

    fun launchLineVideoCall(context: Context, number: String, contactId: String? = null) {
        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.jp.naver.line.android.call",
                "vnd.android.cursor.item/vnd.jp.naver.line.android.profile"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                setPackage("jp.naver.line.android")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val lineIntent = Intent(Intent.ACTION_VIEW, Uri.parse("line://nv/call")).apply {
            setPackage("jp.naver.line.android")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(lineIntent)
            return
        } catch (_: Exception) {}

        SocialUtils.openLine(context, number)
    }

    fun launchViberVideoCall(context: Context, number: String, contactId: String? = null) {
        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.com.viber.voip.viber_number_call",
                "vnd.android.cursor.item/vnd.com.viber.voip.google_voice_message"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                setPackage("com.viber.voip")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val digits = SocialUtils.cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val callIntent = Intent(Intent.ACTION_VIEW, Uri.parse("viber://call?number=%2B$digits")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (SocialUtils.isPackageInstalled(context, "com.viber.voip")) {
                setPackage("com.viber.voip")
            }
        }
        try {
            context.startActivity(callIntent)
        } catch (_: Exception) {
            SocialUtils.openViber(context, number)
        }
    }

    fun launchSkypeVideoCall(context: Context, number: String) {
        val digits = SocialUtils.cleanDigitsOnly(context, number)
        if (digits.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("skype:+$digits?call&video=true")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (SocialUtils.isPackageInstalled(context, "com.skype.raider")) {
                setPackage("com.skype.raider")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            SocialUtils.openSkype(context, number)
        }
    }

    fun launchImoVideoCall(context: Context, number: String, contactId: String? = null) {
        val row = findContactDataRow(
            context = context,
            number = number,
            contactId = contactId,
            mimeTypes = listOf(
                "vnd.android.cursor.item/vnd.com.imo.android.imoim.video.call",
                "vnd.android.cursor.item/vnd.com.imo.android.imoim.call",
                "vnd.android.cursor.item/vnd.com.imo.android.imoim.profile"
            )
        )
        if (row != null) {
            val (dataUri, mimeType) = row
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, mimeType)
                setPackage("com.imo.android.imoim")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.imo.android.imoim")
        launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            launchIntent?.let { context.startActivity(it) }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.video_call_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun launchZoomVideoCall(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("us.zoom.videomeetings")
        launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            launchIntent?.let { context.startActivity(it) }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.video_call_failed), Toast.LENGTH_SHORT).show()
        }
    }
}

class VideoLauncher(
    private val onInitiate: (String, Contact?) -> Unit
) {
    fun startVideoCall(number: String, contact: Contact? = null) {
        onInitiate(number, contact)
    }
}

@Composable
fun rememberVideoLauncher(): VideoLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    var showAppPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }
    var availableApps by remember { mutableStateOf<List<VideoAppOption>>(emptyList()) }

    val videoLauncher = remember {
        VideoLauncher { number, contact ->
            val cleanNum = number.trim().ifBlank {
                contact?.phoneNumbers?.firstOrNull()?.trim().orEmpty()
            }
            if (cleanNum.isBlank()) {
                Toast.makeText(context, context.getString(R.string.video_call_no_number), Toast.LENGTH_SHORT).show()
                return@VideoLauncher
            }

            val apps = VideoCallHelper.getAvailableVideoApps(context)
            val defaultApp = prefs.getDefaultVideoApp()

            // If default app is set and valid, launch directly
            if (defaultApp.isNotBlank() && defaultApp != "always_ask" && apps.any { it.id == defaultApp }) {
                VideoCallHelper.launchVideoCall(context, defaultApp, cleanNum, contact)
                return@VideoLauncher
            }

            // If only carrier video call is available, launch directly
            if (apps.size == 1) {
                VideoCallHelper.launchVideoCall(context, apps.first().id, cleanNum, contact)
                return@VideoLauncher
            }

            // Multiple apps available: show picker
            pendingNumber = cleanNum
            pendingContact = contact
            availableApps = apps
            showAppPicker = true
        }
    }

    if (showAppPicker && availableApps.isNotEmpty()) {
        VideoCallPickerDialog(
            options = availableApps,
            recipientName = pendingContact?.name ?: pendingContact?.formattedDisplayName,
            recipientNumber = pendingNumber,
            onDismissRequest = { showAppPicker = false },
            onSelectOption = { option, rememberChoice ->
                if (rememberChoice) {
                    prefs.setDefaultVideoApp(option.id)
                }
                showAppPicker = false
                VideoCallHelper.launchVideoCall(context, option.id, pendingNumber, pendingContact)
            }
        )
    }

    return videoLauncher
}

@Composable
fun VideoCallPickerDialog(
    options: List<VideoAppOption>,
    recipientName: String?,
    recipientNumber: String,
    onDismissRequest: () -> Unit,
    onSelectOption: (VideoAppOption, Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.video_call_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val subtitle = if (!recipientName.isNullOrBlank() && recipientName != recipientNumber) {
                            "$recipientName ($recipientNumber)"
                        } else {
                            recipientNumber
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(options, key = { it.id }) { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectOption(option, rememberChoice)
                                },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (option.iconDrawable != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(option.iconDrawable),
                                            contentDescription = option.name,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = option.iconVector ?: Icons.Default.VideoCall,
                                            contentDescription = option.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { rememberChoice = !rememberChoice }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.video_call_remember_choice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
