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
import com.grinch.rivo4.modal.db.PrivateContactDao
import com.grinch.rivo4.modal.db.PrivateContactEntity
import com.grinch.rivo4.controller.util.deduplicateNumbers
import com.grinch.rivo4.controller.util.areNumbersEqual
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ContactsRepository(
    private val context: Context,
    private val privateContactDao: PrivateContactDao
) : IContactsRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferenceManager = com.grinch.rivo4.controller.util.PreferenceManager(context)

    private fun formatName(rawName: String): String {
        return rawName
    }

    override fun getContacts(includePrivate: Boolean): List<Contact> {
        val contactsMap = LinkedHashMap<String, Contact>()
        
        if (includePrivate) {
            privateContactDao.getAll().forEach {
                val contact = it.toContact()
                contactsMap[contact.id] = contact
            }
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )

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
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
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
                        if (numbers.none { areNumbersEqual(it, number) } && numbers.size < 5) {
                            numbers.add(number)
                        }
                    } else {
                        contactsMap[id] = Contact(
                            id = id,
                            name = formatName(cursor.getString(nameIdx) ?: "Unknown"),
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
        val list = contactsMap.values.toList()
        
        // Fetch nicknames
        val nicknameMap = mutableMapOf<String, String>()
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.CONTACT_ID)
                val nickIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx)
                    val nickname = cursor.getString(nickIdx)
                    if (id != null && nickname != null) {
                        nicknameMap[id] = nickname
                    }
                }
            }
        } catch (e: Exception) {}

        val finalList = list.map { contact ->
            if (nicknameMap.containsKey(contact.id)) {
                contact.copy(nickname = nicknameMap[contact.id])
            } else {
                contact
            }
        }

        return finalList.sortedBy { it.name.lowercase() }
    }

    override fun getContactById(contactId: String): Contact? {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return null
            return privateContactDao.getById(id)?.toContact()
        }
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

        try {
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
                        name = formatName(cursor.getString(nameIdx) ?: "Unknown"),
                        photoUri = cursor.getString(photoIdx),
                        isFavorite = isStarred,
                        customRingtone = ringtone,
                        accountName = accountName,
                        accountType = accountType
                    )

                    contact = when (mimeType) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            currentContact.copy(phoneNumbers = deduplicateNumbers(currentContact.phoneNumbers + data1))
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            currentContact.copy(emails = (currentContact.emails + data1).distinct())
                        }
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            currentContact.copy(addresses = (currentContact.addresses + data1).distinct())
                        }
                        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                            currentContact.copy(nickname = data1)
                        }
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                            val type = cursor.getInt(data2Idx)
                            val label = cursor.getString(data3Idx)
                            val event = ContactEvent(type, label, data1)
                            currentContact.copy(events = (currentContact.events + event).distinct())
                        }
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            currentContact.copy(notes = data1)
                        }
                        else -> currentContact
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return contact
    }

    override fun toggleFavorite(contactId: String, isFavorite: Boolean) {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.getById(id)?.let {
                privateContactDao.update(it.copy(isFavorite = isFavorite))
            }
            return
        }
        val contentValue = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(contactId)
            .build()
        contentResolver.update(updateUri, contentValue, null, null)
    }

    private fun getPhotoBytes(uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // Downscale to a reasonable size for Contacts provider (960x960 is usually max)
            val maxSize = 720
            val width = bitmap.width
            val height = bitmap.height

            val finalBitmap = if (width > maxSize || height > maxSize) {
                val scale = maxSize.toFloat() / Math.max(width, height)
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
            val bytes = outputStream.toByteArray()

            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
            bitmap.recycle()

            bytes
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRawContactIds(contactId: String): List<String> {
        val ids = mutableListOf<String>()
        try {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getString(idIdx))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return ids
    }

    private fun getRawContactId(contactId: String): String? {
        return getRawContactIds(contactId).firstOrNull()
    }

    override fun saveContact(contact: Contact) {
        if (contact.isPrivate) {
            val entity = PrivateContactEntity.fromContact(contact)
            if (entity.localId == 0L) {
                privateContactDao.insert(entity)
            } else {
                privateContactDao.update(entity)
            }
            return
        }
        val ops = ArrayList<ContentProviderOperation>()
        val photoBytes = contact.photoUri?.let { getPhotoBytes(it) }

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

            if (photoBytes != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                )
            }

            if (contact.nickname != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                        .build()
                )
            }

            if (contact.notes != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.notes)
                        .build()
                )
            }

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
            val rawContactIds = getRawContactIds(contact.id)
            if (rawContactIds.isEmpty()) return

            rawContactIds.forEach { rawContactId ->
                // Update Account (Move contact)
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection("${ContactsContract.RawContacts._ID}=?", arrayOf(rawContactId))
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountType)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountName)
                        .build()
                )

                // Update Name (Delete and Insert for reliability)
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                        .build()
                )

                // Update Photo
                if (contact.photoUri == null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                arrayOf(rawContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            )
                            .build()
                    )
                } else if (photoBytes != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                arrayOf(rawContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            )
                            .build()
                    )
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                }

                // Update Nickname
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(rawContactId, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
                if (contact.nickname != null) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname)
                            .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                            .build()
                    )
                }

                // Update Notes
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(rawContactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
                if (contact.notes != null) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.notes)
                            .build()
                    )
                }

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
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteContact(contactId: String) {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.deleteById(id)
            return
        }
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        contentResolver.delete(uri, null, null)
    }

    override fun deleteContacts(contactIds: List<String>) {
        val ops = ArrayList<ContentProviderOperation>()
        contactIds.forEach { id ->
            if (id.startsWith("p")) {
                val lid = id.substring(1).toLongOrNull()
                if (lid != null) privateContactDao.deleteById(lid)
                return@forEach
            }
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
        return try {
            AccountManager.get(context).accounts.toList()
        } catch (e: SecurityException) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getContactByNumber(number: String): Contact? {
        // Check private contacts first
        privateContactDao.getAll().forEach {
            val contact = it.toContact()
            if (contact.phoneNumbers.any { num -> areNumbersEqual(num, number) }) {
                return contact
            }
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.STARRED,
            ContactsContract.PhoneLookup.CUSTOM_RINGTONE
        )

        try {
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
                        name = formatName(name),
                        photoUri = photoUri,
                        isFavorite = starred,
                        phoneNumbers = listOf(number),
                        customRingtone = ringtone
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
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
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.getById(id)?.let {
                privateContactDao.update(it.copy(customRingtone = ringtoneUri))
            }
            return
        }
        val contentValue = ContentValues().apply {
            put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
        }
        val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(contactId)
            .build()
        contentResolver.update(updateUri, contentValue, null, null)
    }

    override fun formatAllPhoneNumbers(onProgress: ((current: Int, total: Int) -> Unit)?) {
        val allContacts = getContacts(true)
        val ops = ArrayList<ContentProviderOperation>()
        val total = allContacts.size

        allContacts.forEachIndexed { index, contact ->
            onProgress?.invoke(index + 1, total)
            val updatedNumbers = contact.phoneNumbers.map { it.replace(" ", "") }
            if (updatedNumbers != contact.phoneNumbers) {
                if (contact.isPrivate) {
                    val id = contact.id.substring(1).toLongOrNull()
                    if (id != null) {
                        privateContactDao.getById(id)?.let {
                            privateContactDao.update(it.copy(phoneNumbersJson = Json.encodeToString(updatedNumbers)))
                        }
                    }
                } else {
                    contact.phoneNumbers.forEachIndexed { i, oldNum ->
                        val newNum = updatedNumbers[i]
                        if (newNum != oldNum) {
                            val rawId = getRawContactId(contact.id)
                            if (rawId != null) {
                                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(
                                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER}=?",
                                        arrayOf(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, oldNum)
                                    )
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNum)
                                    .build())
                            }
                        }
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

    override fun makeContactPrivate(contactId: String) {
        val contact = getContactById(contactId) ?: return
        if (contact.isPrivate) return

        // 1. Save to local DB
        val privateContact = contact.copy(isPrivate = true)
        privateContactDao.insert(PrivateContactEntity.fromContact(privateContact))

        // 2. Delete from system contacts
        deleteContact(contactId)
    }

    override fun makeContactPublic(contactId: String) {
        val contact = getContactById(contactId) ?: return
        if (!contact.isPrivate) return

        // 1. Save to system contacts
        saveContact(contact.copy(id = "", isPrivate = false))

        // 2. Delete from local DB
        deleteContact(contactId)
    }

    override fun exportPrivateContacts(uri: Uri) {
        val privateContacts = privateContactDao.getAll().map { it.toContact() }
        val vcfContent = buildString {
            privateContacts.forEach { contact ->
                append("BEGIN:VCARD\n")
                append("VERSION:3.0\n")
                append("FN:${contact.name}\n")
                contact.phoneNumbers.forEach { append("TEL;TYPE=CELL:$it\n") }
                contact.emails.forEach { append("EMAIL;TYPE=HOME:$it\n") }
                append("END:VCARD\n")
            }
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(vcfContent.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun importPrivateContacts(uri: Uri) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
            val vCards = content.split("BEGIN:VCARD")
            vCards.forEach { vCard ->
                if (vCard.isBlank()) return@forEach
                var name = ""
                val numbers = mutableListOf<String>()
                val emails = mutableListOf<String>()
                
                vCard.lines().forEach { line ->
                    when {
                        line.startsWith("FN:") -> name = line.substring(3)
                        line.startsWith("TEL") -> {
                            val parts = line.split(":")
                            if (parts.size > 1) numbers.add(parts[1])
                        }
                        line.startsWith("EMAIL") -> {
                            val parts = line.split(":")
                            if (parts.size > 1) emails.add(parts[1])
                        }
                    }
                }
                
                if (name.isNotBlank() || numbers.isNotEmpty()) {
                    saveContact(Contact(
                        id = "0",
                        name = if (name.isBlank()) numbers.firstOrNull() ?: "Unknown" else name,
                        phoneNumbers = numbers,
                        emails = emails,
                        isPrivate = true
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}