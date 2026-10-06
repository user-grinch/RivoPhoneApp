package com.grinch.rivo4.modal.repository

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.CallLog
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.content.Context
import android.content.ComponentName
import android.content.ContentValues
import android.telephony.SubscriptionManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.controller.util.normalizePhoneNumber
import com.grinch.rivo4.controller.util.areNumbersEqual

class CallLogRepository(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val contactsRepo: IContactsRepository
) : ICallLogRepository {

    private val preferenceManager = com.grinch.rivo4.controller.util.PreferenceManager(context)

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    override fun getCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

        val allContacts = try { contactsRepo.getContacts() } catch (e: Exception) { emptyList() }
        val contactMap = mutableMapOf<String, Contact>()
        allContacts.forEach { contact ->
            contact.phoneNumbers.forEach { number ->
                val normalized = normalizePhoneNumber(number)
                val key = if (normalized.length >= 10) normalized.takeLast(10) else normalized
                contactMap[key] = contact
            }
        }

        val baseProjection = mutableListOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.CACHED_LOOKUP_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        baseProjection.add("phone_account_label")
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_ID)
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)

        val limit = preferenceManager.getCallLogLimit()

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && limit > 0) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(CallLog.Calls.DATE)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }
                try {
                    contentResolver.query(CallLog.Calls.CONTENT_URI, baseProjection.toTypedArray(), queryArgs, null)
                } catch (e: Exception) {
                    contentResolver.query(CallLog.Calls.CONTENT_URI, baseProjection.toTypedArray(), null, null, "${CallLog.Calls.DATE} DESC")
                }
            } else {
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    baseProjection.toTypedArray(),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
            }

            cursor?.use { parseCursor(it, callLogs, contactMap, limit) }
        } catch (e: Exception) {
            try {
                val safeProjection = arrayOf(
                    CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.CACHED_PHOTO_URI, CallLog.Calls.CACHED_LOOKUP_URI,
                    CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
                    CallLog.Calls.PHONE_ACCOUNT_ID, CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
                )
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    safeProjection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use { parseCursor(it, callLogs, contactMap, limit) }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        return callLogs
    }

    override fun saveCallLog(entry: CallLogEntry) {
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, entry.number)
            put(CallLog.Calls.TYPE, entry.type)
            put(CallLog.Calls.DATE, entry.date)
            put(CallLog.Calls.DURATION, entry.duration)
            put(CallLog.Calls.NEW, 1)
        }
        try {
            contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun contactIdFromLookupUri(lookupUri: String): String? {
        return try {
            val segments = Uri.parse(lookupUri).pathSegments
            segments.lastOrNull { it.toLongOrNull() != null } ?: segments.lastOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveSimSlotNumber(
        accountId: String?,
        componentStr: String?,
        rawLabel: String?,
        accountSimMap: Map<String, Int>,
        labelSimMap: Map<String, Int>
    ): Int? {
        if (!accountId.isNullOrEmpty()) {
            accountSimMap[accountId]?.let { return it }
            if (!componentStr.isNullOrEmpty()) {
                accountSimMap["$componentStr/$accountId"]?.let { return it }
            }
        }
        if (!rawLabel.isNullOrBlank()) {
            labelSimMap[rawLabel.trim().lowercase()]?.let { return it }
        }
        if (!accountId.isNullOrEmpty()) {
            val lowerId = accountId.lowercase()
            when {
                lowerId == "0" || lowerId.endsWith("_0") || lowerId.contains("sim1") || lowerId.contains("slot0") || lowerId.contains("sub0") -> return 1
                lowerId == "1" && accountSimMap.containsKey("0") -> return 2
                lowerId == "1" -> return 1
                lowerId == "2" || lowerId.endsWith("_1") || lowerId.contains("sim2") || lowerId.contains("slot1") || lowerId.contains("sub1") -> return 2
            }
        }
        if (!rawLabel.isNullOrBlank()) {
            val lower = rawLabel.lowercase()
            when {
                lower.contains("sim 1") || lower.contains("sim1") || lower.contains("slot 1") || lower.contains("[1]") -> return 1
                lower.contains("sim 2") || lower.contains("sim2") || lower.contains("slot 2") || lower.contains("[2]") -> return 2
                lower.contains("1") && !lower.contains("2") -> return 1
                lower.contains("2") && !lower.contains("1") -> return 2
            }
        }
        if ((!accountId.isNullOrEmpty() || !rawLabel.isNullOrBlank()) && (accountSimMap.size == 1 || labelSimMap.size == 1)) {
            return 1
        }
        return if (!accountId.isNullOrEmpty() || !rawLabel.isNullOrBlank()) 1 else null
    }

    private fun parseCursor(cursor: Cursor, callLogs: MutableList<CallLogEntry>, contactMap: Map<String, Contact>, limit: Int = 0) {
        val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
        val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
        val cachedNameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val cachedPhotoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
        val cachedLookupIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_LOOKUP_URI)
        val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
        val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
        val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
        
        val labelIdx = cursor.getColumnIndex("phone_account_label")
        val accountIdIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
        val componentNameIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)

        val accountSimMap = mutableMapOf<String, Int>()
        val labelSimMap = mutableMapOf<String, Int>()

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                subscriptionManager?.activeSubscriptionInfoList?.forEach { sub ->
                    val slotNum = sub.simSlotIndex + 1
                    accountSimMap[sub.subscriptionId.toString()] = slotNum
                    sub.iccId?.takeIf { it.isNotEmpty() }?.let { accountSimMap[it] = slotNum }
                    sub.displayName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        labelSimMap[it.lowercase()] = slotNum
                    }
                    sub.carrierName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        labelSimMap[it.lowercase()] = slotNum
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val phoneAccounts = if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telecomManager?.callCapablePhoneAccounts ?: emptyList()
            } else emptyList()

            phoneAccounts.forEachIndexed { index, handle ->
                val simNum = index + 1
                accountSimMap.putIfAbsent(handle.id, simNum)
                handle.componentName?.flattenToString()?.let { compStr ->
                    accountSimMap.putIfAbsent("$compStr/${handle.id}", simNum)
                }
                telecomManager?.getPhoneAccount(handle)?.label?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { label ->
                    labelSimMap.putIfAbsent(label.lowercase(), simNum)
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val tempLogs = mutableListOf<CallLogEntry>()
        val simCache = mutableMapOf<String, String>()
        val unknownLabel = context.getString(R.string.label_unknown)
        val hiddenNumbers = if (!preferenceManager.isHiddenContactsVisible()) {
            try { contactsRepo.getHiddenNumbers() } catch (e: Exception) { emptyList() }
        } else {
            emptyList()
        }
        val blockedNumbers = try {
            com.grinch.rivo4.controller.util.BlockedNumbersManager.getAll(context).map { normalizePhoneNumber(it.originalNumber) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        while (cursor.moveToNext()) {
            if (limit > 0 && tempLogs.size >= limit) {
                break
            }
            val callId = cursor.getLong(idIdx)
            val number = cursor.getString(numberIdx) ?: unknownLabel
            if (hiddenNumbers.isNotEmpty() && hiddenNumbers.any { areNumbersEqual(it, number) }) {
                continue
            }
            val type = cursor.getInt(typeIdx)
            val date = cursor.getLong(dateIdx)
            val duration = cursor.getLong(durationIdx)
            
            val accountId = if (accountIdIdx != -1) cursor.getString(accountIdIdx) else null
            val componentStr = if (componentNameIdx != -1) cursor.getString(componentNameIdx) else null

            var simLabel = if (labelIdx != -1) cursor.getString(labelIdx) else null
            
            val isBlocked = type == CallLog.Calls.BLOCKED_TYPE || (number != unknownLabel && blockedNumbers.contains(normalizePhoneNumber(number)))
            
            if (simLabel.isNullOrEmpty() && !accountId.isNullOrEmpty() && !componentStr.isNullOrEmpty()) {
                simLabel = simCache.getOrPut("$componentStr/$accountId") {
                    try {
                        val componentName = ComponentName.unflattenFromString(componentStr)
                        if (componentName != null) {
                            val handle = PhoneAccountHandle(componentName, accountId)
                            telecomManager?.getPhoneAccount(handle)?.label?.toString() ?: ""
                        } else ""
                    } catch (e: Exception) { "" }
                }
            }

            if (simLabel?.isEmpty() == true) simLabel = null

            val simNumber = resolveSimSlotNumber(
                accountId = accountId,
                componentStr = componentStr,
                rawLabel = simLabel,
                accountSimMap = accountSimMap,
                labelSimMap = labelSimMap
            )

            val normalizedNum = normalizePhoneNumber(number)
            val lookupKey = if (normalizedNum.length >= 10) normalizedNum.takeLast(10) else normalizedNum
            val matchedContact = contactMap[lookupKey]
            
            val displayName = matchedContact?.name ?: cursor.getString(cachedNameIdx)
            val photoUri = matchedContact?.photoUri ?: cursor.getString(cachedPhotoIdx)
            val contactId = matchedContact?.id ?: cursor.getString(cachedLookupIdx)?.let {
                contactIdFromLookupUri(it)
            }

            val singleEntry = CallLogEntry(
                id = callId,
                number = number,
                name = displayName ?: number,
                type = type,
                date = date,
                duration = duration,
                photoUri = photoUri,
                contactId = contactId,
                simLabel = simLabel,
                simNumber = simNumber,
                isBlocked = isBlocked,
                types = listOf(type),
                ids = listOf(callId)
            )

            val lastEntry = tempLogs.lastOrNull()
            val sameContact = if (lastEntry != null) {
                (contactId != null && lastEntry.contactId == contactId) ||
                areNumbersEqual(lastEntry.number, number)
            } else false

            if (lastEntry != null && sameContact) {
                val currentSubLogs = if (lastEntry.subLogs.isEmpty()) listOf(lastEntry) else lastEntry.subLogs
                tempLogs[tempLogs.size - 1] = lastEntry.copy(
                    types = lastEntry.types + type,
                    ids = lastEntry.ids + callId,
                    isBlocked = lastEntry.isBlocked || isBlocked,
                    simNumber = lastEntry.simNumber ?: simNumber,
                    subLogs = currentSubLogs + singleEntry
                )
            } else {
                tempLogs.add(singleEntry)
            }
        }
        callLogs.addAll(tempLogs)
    }

    override fun deleteCallLog(number: String) {
        try {
            contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls.NUMBER} = ?",
                arrayOf(number)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteCallLogsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        try {
            val selection = "${CallLog.Calls._ID} IN (${ids.joinToString(",")})"
            contentResolver.delete(CallLog.Calls.CONTENT_URI, selection, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun clearCallLogs() {
        try {
            contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
