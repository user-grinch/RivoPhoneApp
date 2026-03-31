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
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
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

            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

            
            val contactInfoCache =
                mutableMapOf<String, Triple<String?, String?, Long?>>()

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx) ?: "Unknown"
                val type = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val duration = cursor.getLong(durationIdx)

                val (contactName, photoUri, contactId) =
                    contactInfoCache.getOrPut(number) {
                        getContactDataByNumber(number)
                    }

                val displayName = contactName
                    ?: cursor.getString(cachedNameIdx)
                    ?: number

                val lastEntry = callLogs.lastOrNull()
                if (lastEntry != null && lastEntry.number == number) {
                    
                    val updatedEntry = lastEntry.copy(
                        types = lastEntry.types + type
                    )
                    callLogs[callLogs.size - 1] = updatedEntry
                } else {
                    callLogs.add(
                        CallLogEntry(
                            number = number,
                            name = displayName,
                            type = type,
                            date = date,
                            duration = duration,
                            photoUri = photoUri,
                            contactId = contactId?.toString(),
                            types = listOf(type)
                        )
                    )
                }
            }
        }

        return callLogs
    }

    private fun getContactDataByNumber(
        number: String
    ): Triple<String?, String?, Long?> {

        if (number.isBlank() || number == "Unknown") {
            return Triple(null, null, null)
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        return try {
            contentResolver.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                        val nameIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        val photoIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                        val contactId = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx)
                        val photoUri = cursor.getString(photoIdx)

                        Triple(name, photoUri, contactId)
                    } else {
                        Triple(null, null, null)
                    }
                } ?: Triple(null, null, null)
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }
}