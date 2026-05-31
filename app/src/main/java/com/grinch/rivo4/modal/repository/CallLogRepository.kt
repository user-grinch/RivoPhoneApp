package com.grinch.rivo4.modal.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.data.CallLogEntry

class CallLogRepository(
    private val contentResolver: ContentResolver
) : ICallLogRepository {

    override fun getCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.CACHED_LOOKUP_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->

            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val cachedPhotoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
            val cachedLookupIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_LOOKUP_URI)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val callId = cursor.getLong(idIdx)
                val number = cursor.getString(numberIdx) ?: "Unknown"
                val type = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val duration = cursor.getLong(durationIdx)

                val displayName = cursor.getString(cachedNameIdx)
                val photoUri = cursor.getString(cachedPhotoIdx)
                val lookupUri = cursor.getString(cachedLookupIdx)
                
                // Extract contact ID from lookup URI if possible
                val contactId = lookupUri?.let {
                    try {
                        Uri.parse(it).lastPathSegment
                    } catch (e: Exception) { null }
                }

                val lastEntry = callLogs.lastOrNull()
                if (lastEntry != null && lastEntry.number == number) {
                    val updatedEntry = lastEntry.copy(
                        types = lastEntry.types + type,
                        ids = lastEntry.ids + callId
                    )
                    callLogs[callLogs.size - 1] = updatedEntry
                } else {
                    callLogs.add(
                        CallLogEntry(
                            id = callId,
                            number = number,
                            name = displayName ?: number,
                            type = type,
                            date = date,
                            duration = duration,
                            photoUri = photoUri,
                            contactId = contactId,
                            types = listOf(type),
                            ids = listOf(callId)
                        )
                    )
                }
            }
        }

        return callLogs
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
