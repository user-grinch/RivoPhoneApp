package com.grinch.rivo4.modal.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.content.Context
import android.content.ComponentName
import android.content.ContentValues
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.controller.util.normalizePhoneNumber

class CallLogRepository(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val contactsRepo: IContactsRepository
) : ICallLogRepository {

    private val preferenceManager = com.grinch.rivo4.controller.util.PreferenceManager(context)
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    override fun getCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()
        
        // Optimization: Fetch all contacts once for quick lookup
        val allContacts = try { contactsRepo.getContacts() } catch (e: Exception) { emptyList() }
        val contactMap = mutableMapOf<String, Contact>()
        allContacts.forEach { contact ->
            contact.phoneNumbers.forEach { number ->
                val normalized = normalizePhoneNumber(number)
                // Use last 10 digits as key for flexible matching (local vs international)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            baseProjection.add("phone_account_label")
        }
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_ID)
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)

        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                baseProjection.toTypedArray(),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use { parseCursor(it, callLogs, contactMap) }
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
                cursor?.use { parseCursor(it, callLogs, contactMap) }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        return callLogs
    }

    private fun parseCursor(cursor: Cursor, callLogs: MutableList<CallLogEntry>, contactMap: Map<String, Contact>) {
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

        val tempLogs = mutableListOf<CallLogEntry>()
        val simCache = mutableMapOf<String, String>()

        while (cursor.moveToNext()) {
            val callId = cursor.getLong(idIdx)
            val number = cursor.getString(numberIdx) ?: "Unknown"
            val type = cursor.getInt(typeIdx)
            val date = cursor.getLong(dateIdx)
            val duration = cursor.getLong(durationIdx)
            
            var simLabel = if (labelIdx != -1) cursor.getString(labelIdx) else null
            
            val isBlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                type == CallLog.Calls.BLOCKED_TYPE || type == CallLog.Calls.REJECTED_TYPE
            } else {
                type == 6 
            }
            
            if (simLabel.isNullOrEmpty() && accountIdIdx != -1 && componentNameIdx != -1) {
                val accountId = cursor.getString(accountIdIdx)
                val componentStr = cursor.getString(componentNameIdx)
                
                if (!accountId.isNullOrEmpty() && !componentStr.isNullOrEmpty()) {
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
            }

            if (simLabel?.isEmpty() == true) simLabel = null

            // Enrich with contact data
            val normalizedNum = normalizePhoneNumber(number)
            val lookupKey = if (normalizedNum.length >= 10) normalizedNum.takeLast(10) else normalizedNum
            val matchedContact = contactMap[lookupKey]
            
            val displayName = matchedContact?.name ?: cursor.getString(cachedNameIdx)
            val photoUri = matchedContact?.photoUri ?: cursor.getString(cachedPhotoIdx)
            val contactId = matchedContact?.id ?: cursor.getString(cachedLookupIdx)?.let {
                try {
                    Uri.parse(it).lastPathSegment
                } catch (e: Exception) { null }
            }

            val lastEntry = tempLogs.lastOrNull()
            if (lastEntry != null && lastEntry.number == number && lastEntry.type == type) {
                // Grouping consecutive same number and type
                tempLogs[tempLogs.size - 1] = lastEntry.copy(
                    types = lastEntry.types + type,
                    ids = lastEntry.ids + callId
                )
            } else {
                tempLogs.add(
                    CallLogEntry(
                        id = callId,
                        number = number,
                        name = displayName ?: number,
                        type = type,
                        date = date,
                        duration = duration,
                        photoUri = photoUri,
                        contactId = contactId,
                        simLabel = simLabel,
                        isBlocked = isBlocked,
                        types = listOf(type),
                        ids = listOf(callId)
                    )
                )
            }
        }
        callLogs.addAll(tempLogs)
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
