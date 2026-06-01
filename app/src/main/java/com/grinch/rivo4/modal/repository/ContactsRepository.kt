package com.grinch.rivo4.modal.repository
import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.`interface`.IContactsRepository

class ContactsRepository(private val context: Context) : IContactsRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun getContacts(): List<Contact> {
        val contactsMap = LinkedHashMap<String, Contact>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )

        // Query Phone table directly - it's much faster than querying Data table with all mimetypes
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val accountNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val accountTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val number = cursor.getString(numberIdx) ?: continue
                    
                    val existingContact = contactsMap[id]
                    if (existingContact != null) {
                        val numbers = existingContact.phoneNumbers as MutableList<String>
                        if (!numbers.contains(number) && numbers.size < 5) {
                            numbers.add(number)
                        }
                    } else {
                        contactsMap[id] = Contact(
                            id = id,
                            name = cursor.getString(nameIdx) ?: "Unknown",
                            photoUri = cursor.getString(photoIdx),
                            isFavorite = cursor.getInt(starredIdx) == 1,
                            accountName = cursor.getString(accountNameIdx),
                            accountType = cursor.getString(accountTypeIdx),
                            phoneNumbers = mutableListOf(number)
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return contactsMap.values.toList()
    }

    override fun getContactById(contactId: String): Contact? {
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.CUSTOM_RINGTONE,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )

        var contact: Contact? = null

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data3Idx = cursor.getColumnIndex(ContactsContract.Data.DATA3)
            val starredIdx = cursor.getColumnIndex(ContactsContract.Data.STARRED)
            val ringtoneIdx = cursor.getColumnIndex(ContactsContract.Data.CUSTOM_RINGTONE)

            val accountNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            val accountTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val mimeType = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx) ?: continue
                val isStarred = cursor.getInt(starredIdx) == 1
                val ringtone = cursor.getString(ringtoneIdx)
                val accountName = cursor.getString(accountNameIdx)
                val accountType = cursor.getString(accountTypeIdx)

                val currentContact = contact ?: Contact(
                    id = id,
                    name = cursor.getString(nameIdx) ?: "Unknown",
                    photoUri = cursor.getString(photoIdx),
                    isFavorite = isStarred,
                    customRingtone = ringtone,
                    accountName = accountName,
                    accountType = accountType
                )

                contact = when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(phoneNumbers = (currentContact.phoneNumbers + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(emails = (currentContact.emails + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(addresses = (currentContact.addresses + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val event = ContactEvent(type, label, data1)
                        currentContact.copy(events = (currentContact.events + event).distinct())
                    }
                    else -> currentContact
                }
            }
        }
        return contact
    }

    override fun toggleFavorite(contactId: String, isFavorite: Boolean) {
        val contentValue = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(contactId)
            .build()
        contentResolver.update(updateUri, contentValue, null, null)
    }

    override fun saveContact(contact: Contact) {
        val ops = ArrayList<ContentProviderOperation>()

        if (contact.id.isEmpty() || contact.id == "0") {
            // New Contact
            val rawContactIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountName)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build()
            )

            contact.phoneNumbers.forEach { number ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }

            contact.emails.forEach { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }

            contact.addresses.forEach { address ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
                        )
                        .build()
                )
            }
        } else {
            // Update
            val rawContactId = getRawContactId(contact.id) ?: return

            // Update Account (Move contact)
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection("${ContactsContract.RawContacts._ID}=?", arrayOf(rawContactId))
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountName)
                    .build()
            )

            // Update Name
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build()
            )

            // Update Phone Numbers (Delete and Insert)
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            contact.phoneNumbers.forEach { number ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }

            // Update Emails (Delete and Insert)
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            contact.emails.forEach { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }

            // Update Addresses (Delete and Insert)
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            contact.addresses.forEach { address ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
                        )
                        .build()
                )
            }
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteContact(contactId: String) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        contentResolver.delete(uri, null, null)
    }

    override fun deleteContacts(contactIds: List<String>) {
        val ops = ArrayList<ContentProviderOperation>()
        contactIds.forEach { id ->
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
            ops.add(ContentProviderOperation.newDelete(uri).build())
        }
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun moveContacts(contactIds: List<String>, accountName: String?, accountType: String?) {
        val ops = ArrayList<ContentProviderOperation>()
        contactIds.forEach { id ->
            val rawContactId = getRawContactId(id)
            if (rawContactId != null) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection("${ContactsContract.RawContacts._ID}=?", arrayOf(rawContactId))
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                        .build()
                )
            }
        }
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getAvailableAccounts(): List<Account> {
        return AccountManager.get(context).accounts.toList()
    }

    private fun getRawContactId(contactId: String): String? {
        val projection = arrayOf(ContactsContract.RawContacts._ID)
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    override fun getContactByNumber(number: String): Contact? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.STARRED,
            ContactsContract.PhoneLookup.CUSTOM_RINGTONE
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val starredIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                val ringtoneIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CUSTOM_RINGTONE)
                
                val id = if (idIdx != -1) cursor.getString(idIdx) else "0"
                val name = if (nameIdx != -1) cursor.getString(nameIdx) else "Unknown"
                val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) else null
                val starred = if (starredIdx != -1) cursor.getInt(starredIdx) == 1 else false
                val ringtone = if (ringtoneIdx != -1) cursor.getString(ringtoneIdx) else null

                return Contact(
                    id = id,
                    name = name,
                    photoUri = photoUri,
                    isFavorite = starred,
                    phoneNumbers = listOf(number),
                    customRingtone = ringtone
                )
            }
        }
        return null
    }

    override fun findDuplicates(): List<List<Contact>> {
        val allContacts = getContacts()
        val duplicates = mutableListOf<List<Contact>>()
        
        // Group by name (exact match, case-insensitive)
        val byName = allContacts.groupBy { it.name.lowercase().trim() }
            .filter { it.value.size > 1 }
        
        // Group by phone number (normalized)
        val byNumber = mutableMapOf<String, MutableSet<Contact>>()
        allContacts.forEach { contact ->
            contact.phoneNumbers.forEach { number ->
                val normalized = number.replace(Regex("[^0-9+]"), "")
                if (normalized.length >= 7) {
                    byNumber.getOrPut(normalized) { mutableSetOf() }.add(contact)
                }
            }
        }
        val byNumberFiltered = byNumber.filter { it.value.size > 1 }

        val processedIds = mutableSetOf<String>()
        
        byName.values.forEach { group ->
            duplicates.add(group)
            processedIds.addAll(group.map { it.id })
        }

        byNumberFiltered.values.forEach { group ->
            val uniqueGroup = group.filter { it.id !in processedIds }
            if (uniqueGroup.size > 1) {
                duplicates.add(uniqueGroup)
            }
        }

        return duplicates
    }

    override fun mergeContacts(targetContactId: String, sourceContactIds: List<String>) {
        val targetContact = getContactById(targetContactId) ?: return
        val ops = ArrayList<ContentProviderOperation>()

        sourceContactIds.forEach { sourceId ->
            if (sourceId == targetContactId) return@forEach
            val sourceContact = getContactById(sourceId) ?: return@forEach

            // Add all phone numbers, emails, addresses, events from source to target
            sourceContact.phoneNumbers.forEach { number ->
                if (!targetContact.phoneNumbers.contains(number)) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, getRawContactId(targetContactId))
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build())
                }
            }
            // ... similar for other data types if needed ...

            // Delete source contact
            ops.add(ContentProviderOperation.newDelete(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, sourceId))
                .build())
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setCustomRingtone(contactId: String, ringtoneUri: String?) {
        val contentValue = ContentValues().apply {
            put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
        }
        val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(contactId)
            .build()
        contentResolver.update(updateUri, contentValue, null, null)
    }

    override fun formatAllPhoneNumbers() {
        val allContacts = getContacts()
        val ops = ArrayList<ContentProviderOperation>()

        allContacts.forEach { contact ->
            contact.phoneNumbers.forEach { number ->
                val normalized = number.replace(" ", "")
                if (normalized != number) {
                    val rawId = getRawContactId(contact.id)
                    if (rawId != null) {
                        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER}=?",
                                arrayOf(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, number)
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, normalized)
                            .build())
                    }
                }
            }
        }

        try {
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
