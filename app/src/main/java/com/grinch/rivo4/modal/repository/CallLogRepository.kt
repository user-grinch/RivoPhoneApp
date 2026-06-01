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
import com.grinch.rivo4.modal.data.CallLogEntry

class CallLogRepository(
    private val contentResolver: ContentResolver,
    private val context: Context
) : ICallLogRepository {

    private val preferenceManager = com.grinch.rivo4.controller.util.PreferenceManager(context)

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    override fun getCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

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

        // Try adding all possible SIM info columns
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            baseProjection.add("phone_account_label")
        }
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_ID)
        baseProjection.add(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)

        val limit = preferenceManager.getInt(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_LOG_LIMIT, 500)
        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                baseProjection.toTypedArray(),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )

            cursor?.use { parseCursor(it, callLogs) }
        } catch (e: Exception) {
            // If the above fails due to "phone_account_label" or other columns, try a safer subset
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
                    "${CallLog.Calls.DATE} DESC LIMIT $limit"
                )
                cursor?.use { parseCursor(it, callLogs) }
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

    private fun parseCursor(cursor: Cursor, callLogs: MutableList<CallLogEntry>) {
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
                type == 6 // REJECTED_TYPE fallback
            }
            
            // If label is missing, try to resolve it from account ID
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

            val displayName = cursor.getString(cachedNameIdx)
            val photoUri = cursor.getString(cachedPhotoIdx)
            val lookupUri = cursor.getString(cachedLookupIdx)
            
            val contactId = lookupUri?.let {
                try {
                    Uri.parse(it).lastPathSegment
                } catch (e: Exception) { null }
            }

            val lastEntry = tempLogs.lastOrNull()
            if (lastEntry != null && lastEntry.number == number) {
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
