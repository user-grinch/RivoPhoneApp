package com.grinch.rivo4.modal.repository

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.db.PrivateContactDao
import com.grinch.rivo4.modal.db.TrashedContactDao
import com.grinch.rivo4.modal.db.TrashedContactEntity
import com.grinch.rivo4.modal.db.PrivateContactEntity
import com.grinch.rivo4.modal.data.AccountEntry
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.deduplicateNumbers
import com.grinch.rivo4.controller.util.areNumbersEqual
import com.grinch.rivo4.controller.util.CallBackgroundStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private data class StructuredNameData(
    val prefix: String? = null,
    val givenName: String? = null,
    val middleName: String? = null,
    val familyName: String? = null,
    val suffix: String? = null
)

class ContactsRepository(
    private val context: Context,
    private val privateContactDao: PrivateContactDao,
    private val trashedContactDao: TrashedContactDao
) : IContactsRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferenceManager = com.grinch.rivo4.controller.util.PreferenceManager(context)
    private val unknownLabel: String get() = context.getString(R.string.label_unknown)

    private fun formatName(rawName: String): String {
        return rawName
    }

    private fun isSyncAdapterAccount(type: String?): Boolean {
        if (type == null) return false
        val t = type.lowercase()
        return t.contains("whatsapp") || t.contains("telegram") || t.contains("viber") || t.contains("skype")
    }

    private fun pickPrimaryAccount(
        rawList: List<AccountEntry>,
        availableAccounts: List<Account>
    ): Pair<String?, String?> {
        if (rawList.isEmpty()) return Pair(null, null)
        val local = rawList.find { ContactUtils.isLocalAccount(it.type, it.name, availableAccounts) }
        if (local != null) return Pair(local.name, local.type)

        val cloud = rawList.find { !isSyncAdapterAccount(it.type) }
        if (cloud != null) return Pair(cloud.name, cloud.type)

        return Pair(null, null)
    }

    override fun getContacts(includePrivate: Boolean, includeHidden: Boolean): List<Contact> {
        val contactsMap = LinkedHashMap<String, Contact>()
        
        if (includePrivate) {
            privateContactDao.getAll().forEach {
                val contact = it.toContact()
                if (!contact.isHidden || includeHidden) {
                    contactsMap[contact.id] = contact
                }
            }
        }

        val availableAccountsList = getAvailableAccounts()
        val rawAccountsMap = mutableMapOf<String, MutableList<AccountEntry>>()
        try {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val contactId = if (idCol != -1) cursor.getString(idCol) else null
                    val name = if (nameCol != -1) cursor.getString(nameCol) else null
                    val type = if (typeCol != -1) cursor.getString(typeCol) else null
                    if (!contactId.isNullOrBlank()) {
                        val entry = AccountEntry(name, type)
                        val list = rawAccountsMap.getOrPut(contactId) { mutableListOf() }
                        if (list.none { it.name == name && it.type == type }) {
                            list.add(entry)
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }

        try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    if (contactsMap.containsKey(id)) continue
                    val rawName = cursor.getString(nameIdx)
                    val linked = rawAccountsMap[id] ?: emptyList()
                    val (primaryName, primaryType) = pickPrimaryAccount(linked, availableAccountsList)
                    contactsMap[id] = Contact(
                        id = id,
                        name = formatName(rawName?.ifBlank { null } ?: unknownLabel),
                        photoUri = cursor.getString(photoIdx),
                        isFavorite = cursor.getInt(starredIdx) == 1,
                        phoneNumbers = mutableListOf(),
                        phones = emptyList(),
                        emails = emptyList(),
                        emailEntries = emptyList(),
                        accountName = primaryName,
                        accountType = primaryType,
                        linkedAccounts = linked
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying Contacts.CONTENT_URI", e)
        }

        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val number = cursor.getString(numberIdx) ?: continue
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else 2
                    val label = if (labelIdx != -1) cursor.getString(labelIdx) else null

                    val existingContact = contactsMap[id]
                    if (existingContact != null) {
                        val numbers = existingContact.phoneNumbers.toMutableList()
                        val phones = existingContact.phones.toMutableList()
                        if (numbers.none { areNumbersEqual(it, number) }) {
                            numbers.add(number)
                            phones.add(PhoneNumberEntry(number = number, type = type, label = label))
                            contactsMap[id] = existingContact.copy(
                                phoneNumbers = numbers,
                                phones = phones
                            )
                        }
                    } else {
                        val linked = rawAccountsMap[id] ?: emptyList()
                        val (primaryName, primaryType) = pickPrimaryAccount(linked, availableAccountsList)
                        contactsMap[id] = Contact(
                            id = id,
                            name = formatName(cursor.getString(nameIdx) ?: unknownLabel),
                            photoUri = cursor.getString(photoIdx),
                            isFavorite = cursor.getInt(starredIdx) == 1,
                            phoneNumbers = mutableListOf(number),
                            phones = listOf(PhoneNumberEntry(number = number, type = type, label = label)),
                            accountName = primaryName,
                            accountType = primaryType,
                            linkedAccounts = linked
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying Phone.CONTENT_URI", e)
        }

        val emailProjection = arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL
        )

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                emailProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val address = cursor.getString(addrIdx) ?: continue
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else 1
                    val label = if (labelIdx != -1) cursor.getString(labelIdx) else null

                    val existingContact = contactsMap[id]
                    if (existingContact != null) {
                        val emails = existingContact.emails.toMutableList()
                        val emailEntries = existingContact.emailEntries.toMutableList()
                        if (!emails.contains(address)) {
                            emails.add(address)
                            emailEntries.add(EmailEntry(address = address, type = type, label = label))
                            val currentName = existingContact.name
                            val updatedName = if (currentName.isBlank() || currentName == unknownLabel || currentName == "Unnamed") {
                                address
                            } else {
                                currentName
                            }
                            contactsMap[id] = existingContact.copy(
                                name = updatedName,
                                emails = emails,
                                emailEntries = emailEntries
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying Email.CONTENT_URI", e)
        }

        val list = contactsMap.values.toList()
        
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

        val structuredNameMap = mutableMapOf<String, StructuredNameData>()
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.SUFFIX
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val prefixIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
                val givenIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                val middleIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
                val familyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                val suffixIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx)
                    if (id != null && !structuredNameMap.containsKey(id)) {
                        structuredNameMap[id] = StructuredNameData(
                            prefix = if (prefixIdx != -1) cursor.getString(prefixIdx) else null,
                            givenName = if (givenIdx != -1) cursor.getString(givenIdx) else null,
                            middleName = if (middleIdx != -1) cursor.getString(middleIdx) else null,
                            familyName = if (familyIdx != -1) cursor.getString(familyIdx) else null,
                            suffix = if (suffixIdx != -1) cursor.getString(suffixIdx) else null
                        )
                    }
                }
            }
        } catch (e: Exception) {}

        val finalList = list.map { contact ->
            val nameData = structuredNameMap[contact.id]
            val nickname = nicknameMap[contact.id]

            val constructedName = listOfNotNull(
                nameData?.prefix?.trim()?.ifBlank { null },
                nameData?.givenName?.trim()?.ifBlank { null },
                nameData?.middleName?.trim()?.ifBlank { null },
                nameData?.familyName?.trim()?.ifBlank { null },
                nameData?.suffix?.trim()?.ifBlank { null }
            ).joinToString(" ").ifBlank { null }

            val resolvedName = when {
                constructedName != null -> constructedName
                contact.name.isNotBlank() && contact.name != unknownLabel && contact.name != "Unnamed" -> contact.name
                nickname?.isNotBlank() == true -> nickname
                contact.phoneNumbers.isNotEmpty() -> contact.phoneNumbers.first()
                contact.emails.isNotEmpty() -> contact.emails.first()
                contact.emailEntries.isNotEmpty() -> contact.emailEntries.first().address
                else -> contact.name
            }

            contact.copy(
                name = resolvedName,
                prefix = nameData?.prefix ?: contact.prefix,
                givenName = nameData?.givenName ?: contact.givenName,
                middleName = nameData?.middleName ?: contact.middleName,
                familyName = nameData?.familyName ?: contact.familyName,
                suffix = nameData?.suffix ?: contact.suffix,
                nickname = nickname ?: contact.nickname
            )
        }

        return finalList.sortedBy { it.name.lowercase() }
    }

    private fun resolveLookupKey(lookupKey: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                Uri.encode(lookupKey)
            )
            var resolved: String? = null
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) resolved = cursor.getString(0)
            }
            resolved
        } catch (e: Exception) {
            null
        }
    }

    private fun getNumbersForContactId(contactId: String): List<String> {
        val numbers = mutableListOf<String>()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { numbers.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return numbers
    }

    override fun getContactById(contactId: String): Contact? {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return null
            return privateContactDao.getById(id)?.toContact()
        }
        val resolvedId = if (contactId.toLongOrNull() != null) {
            contactId
        } else {
            resolveLookupKey(contactId) ?: return null
        }
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5,
            ContactsContract.Data.DATA6,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.CUSTOM_RINGTONE
        )

        var contact: Contact? = null

        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(resolvedId),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI)
                val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
                val data3Idx = cursor.getColumnIndex(ContactsContract.Data.DATA3)
                val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)
                val data5Idx = cursor.getColumnIndex(ContactsContract.Data.DATA5)
                val data6Idx = cursor.getColumnIndex(ContactsContract.Data.DATA6)
                val starredIdx = cursor.getColumnIndex(ContactsContract.Data.STARRED)
                val ringtoneIdx = cursor.getColumnIndex(ContactsContract.Data.CUSTOM_RINGTONE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val mimeType = cursor.getString(mimeIdx)
                    val data1 = cursor.getString(data1Idx) ?: continue
                    val isStarred = cursor.getInt(starredIdx) == 1
                    val ringtone = cursor.getString(ringtoneIdx)

                    val currentContact = contact ?: run {
                        val (accInfo, linked) = getAccountInfoWithLinked(resolvedId)
                        Contact(
                            id = id,
                            name = formatName(cursor.getString(nameIdx) ?: unknownLabel),
                            photoUri = cursor.getString(photoIdx),
                            isFavorite = isStarred,
                            customRingtone = ringtone,
                            accountName = accInfo.first,
                            accountType = accInfo.second,
                            linkedAccounts = linked
                        )
                    }

                    contact = when (mimeType) {
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                            val prefix = if (data4Idx != -1) cursor.getString(data4Idx) else null
                            val given = cursor.getString(data2Idx)
                            val family = cursor.getString(data3Idx)
                            val middle = if (data5Idx != -1) cursor.getString(data5Idx) else null
                            val suffix = if (data6Idx != -1) cursor.getString(data6Idx) else null
                            val constructed = listOfNotNull(
                                prefix?.trim()?.ifBlank { null },
                                given?.trim()?.ifBlank { null },
                                middle?.trim()?.ifBlank { null },
                                family?.trim()?.ifBlank { null },
                                suffix?.trim()?.ifBlank { null }
                            ).joinToString(" ")
                            currentContact.copy(
                                name = if (constructed.isNotBlank()) constructed else currentContact.name,
                                prefix = prefix,
                                givenName = given,
                                middleName = middle,
                                familyName = family,
                                suffix = suffix
                            )
                        }
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val entry = PhoneNumberEntry(
                                number = data1,
                                type = cursor.getInt(data2Idx),
                                label = cursor.getString(data3Idx)
                            )
                            val alreadyPresent = currentContact.phones.any { areNumbersEqual(it.number, data1) }
                            currentContact.copy(
                                phoneNumbers = deduplicateNumbers(currentContact.phoneNumbers + data1),
                                phones = if (alreadyPresent) currentContact.phones else currentContact.phones + entry
                            )
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val entry = EmailEntry(
                                address = data1,
                                type = cursor.getInt(data2Idx),
                                label = cursor.getString(data3Idx)
                            )
                            val alreadyPresent = currentContact.emailEntries.any { it.address == data1 }
                            currentContact.copy(
                                emails = (currentContact.emails + data1).distinct(),
                                emailEntries = if (alreadyPresent) currentContact.emailEntries else currentContact.emailEntries + entry
                            )
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
        val finalContact = contact?.let { c ->
            val resolvedName = if (c.name.isBlank() || c.name == unknownLabel || c.name == "Unnamed") {
                c.emails.firstOrNull() ?: c.phoneNumbers.firstOrNull() ?: c.name
            } else {
                c.name
            }
            c.copy(name = resolvedName)
        }
        return finalContact
    }

    override fun toggleFavorite(contactId: String, isFavorite: Boolean) {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.getById(id)?.let {
                privateContactDao.update(it.copy(isFavorite = isFavorite))
            }
            return
        }
        val resolvedId = if (contactId.toLongOrNull() != null) {
            contactId
        } else {
            resolveLookupKey(contactId) ?: contactId
        }
        try {
            val contentValue = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
            }
            val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendPath(resolvedId)
                .build()
            contentResolver.update(updateUri, contentValue, null, null)
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error toggling favorite for contactId ", e)
        }
    }

    private fun getPhotoBytes(uriString: String): ByteArray? {
        return try {
            val uri = if (uriString.startsWith("/")) Uri.fromFile(java.io.File(uriString)) else Uri.parse(uriString)
            var inputStream: java.io.InputStream? = null
            if (uri.scheme == "file" && uri.path != null) {
                val f = java.io.File(uri.path!!)
                if (f.exists()) inputStream = f.inputStream()
            }
            if (inputStream == null && uriString.contains("contacts")) {
                try {
                    inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)
                } catch (_: Exception) {}
                if (inputStream == null) {
                    try {
                        val baseUriStr = if (uriString.endsWith("/photo") || uriString.endsWith("/display_photo")) {
                            uriString.substringBeforeLast("/")
                        } else uriString
                        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, Uri.parse(baseUriStr), true)
                    } catch (_: Exception) {}
                }
            }
            if (inputStream == null) {
                try {
                    inputStream = context.contentResolver.openInputStream(uri)
                } catch (_: Exception) {}
            }
            if (inputStream == null) return null

            val bitmap = inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
            if (bitmap == null) return null

            val maxSize = 480
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
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
            bitmap.recycle()

            bytes
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error decoding photo bytes", t)
            null
        }
    }

    private fun getRawContactIds(contactId: String): List<String> {
        val ids = mutableListOf<String>()
        val resolvedId = if (contactId.toLongOrNull() != null) {
            contactId
        } else {
            resolveLookupKey(contactId) ?: contactId
        }
        try {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                arrayOf(resolvedId),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                val allRaw = mutableListOf<Pair<String, String?>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val type = if (typeIdx != -1) cursor.getString(typeIdx) else null
                    allRaw.add(id to type)
                }
                val writable = allRaw.filter { !isSyncAdapterAccount(it.second) }
                val targetList = if (writable.isNotEmpty()) writable else allRaw
                ids.addAll(targetList.map { it.first })
            }

            // Fallback 1: Query RawContacts directly by _ID if resolvedId is a raw contact ID
            if (ids.isEmpty() && resolvedId.toLongOrNull() != null) {
                contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE),
                    "${ContactsContract.RawContacts._ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                    arrayOf(resolvedId),
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                    if (cursor.moveToNext()) {
                        cursor.getString(idIdx)?.let { ids.add(it) }
                    }
                }
            }

            // Fallback 2: Look up RAW_CONTACT_ID via ContactsContract.Data
            if (ids.isEmpty()) {
                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                    "${ContactsContract.Data.CONTACT_ID} = ?",
                    arrayOf(resolvedId),
                    null
                )?.use { cursor ->
                    val rawCol = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)
                    while (cursor.moveToNext()) {
                        val rawId = cursor.getString(rawCol)
                        if (!rawId.isNullOrBlank() && !ids.contains(rawId)) {
                            ids.add(rawId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying raw contact ids for contactId: $contactId", e)
        }
        return ids
    }

    private fun getRawContactId(contactId: String): String? {
        return getRawContactIds(contactId).firstOrNull()
    }

    private fun getAccountInfoWithLinked(contactId: String): Pair<Pair<String?, String?>, List<AccountEntry>> {
        val entries = mutableListOf<AccountEntry>()
        try {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val type = if (typeIdx != -1) cursor.getString(typeIdx) else null
                    val entry = AccountEntry(name, type)
                    if (entries.none { it.name == name && it.type == type }) {
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val primary = pickPrimaryAccount(entries, getAvailableAccounts())
        return Pair(primary, entries)
    }

    private fun getAccountInfo(contactId: String): Pair<String?, String?> {
        return getAccountInfoWithLinked(contactId).first
    }

    override fun saveContact(contact: Contact): Boolean {
        return saveContactWithResult(contact).first
    }

    override fun saveContactWithResult(contact: Contact): Pair<Boolean, String?> {
        if (contact.isPrivate) {
            return try {
                val entity = PrivateContactEntity.fromContact(contact)
                val id = if (entity.localId == 0L) {
                    privateContactDao.insert(entity)
                } else {
                    privateContactDao.update(entity)
                    entity.localId
                }
                Pair(true, "p$id")
            } catch (e: Exception) {
                android.util.Log.e("ContactsRepo", "Error saving private contact", e)
                Pair(false, null)
            }
        }
        val isSim = contact.accountType?.contains("sim", ignoreCase = true) == true
        val ops = ArrayList<ContentProviderOperation>()
        val photoBytes = if (isSim) null else contact.photoUri?.let { getPhotoBytes(it) }

        val effectivePhones = if (contact.phones.isNotEmpty()) {
            contact.phones
        } else {
            contact.phoneNumbers.map { PhoneNumberEntry(it) }
        }
        val effectiveEmails = if (isSim) emptyList() else {
            if (contact.emailEntries.isNotEmpty()) {
                contact.emailEntries
            } else {
                contact.emails.map { EmailEntry(it) }
            }
        }
        val effectiveAddresses = if (isSim) emptyList() else contact.addresses

        val effectiveName = if (contact.name.isNotBlank() && contact.name != "Unnamed" && contact.name != unknownLabel) {
            contact.name
        } else {
            effectiveEmails.firstOrNull()?.address ?: effectivePhones.firstOrNull()?.number ?: contact.name
        }
        val effectiveGivenName = if (!contact.givenName.isNullOrBlank()) {
            contact.givenName
        } else if (contact.familyName.isNullOrBlank() && contact.middleName.isNullOrBlank()) {
            effectiveName
        } else {
            null
        }

        val resolvedContactId = if (contact.id.isBlank() || contact.id == "0" || contact.id == "null") {
            null
        } else if (contact.id.toLongOrNull() != null) {
            contact.id
        } else {
            resolveLookupKey(contact.id)
        }

        val existingRawContactIds = resolvedContactId?.let { getRawContactIds(it) } ?: emptyList()

        if (resolvedContactId == null || existingRawContactIds.isEmpty()) {
            // New contact or fallback creation of raw contact
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
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, effectiveName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, contact.prefix)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, effectiveGivenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.familyName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
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

            if (!isSim && contact.nickname != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                        .build()
                )
            }

            if (!isSim && contact.notes != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.notes)
                        .build()
                )
            }

            effectivePhones.forEach { entry ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, entry.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, entry.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, entry.label)
                        .build()
                )
            }

            effectiveEmails.forEach { entry ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, entry.address)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, entry.type)
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, entry.label)
                        .build()
                )
            }

            effectiveAddresses.forEach { address ->
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
            // Update existing contact targeting primary raw contact
            val primaryRawContactId = existingRawContactIds.firstOrNull()?.takeIf { it.isNotBlank() && it != "0" && it != "null" }
            if (primaryRawContactId == null) {
                android.util.Log.e("ContactsRepo", "Cannot update contact: primaryRawContactId is invalid for contact ID: " + contact.id)
                return Pair(false, null)
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, effectiveName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, contact.prefix)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, effectiveGivenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.familyName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
                    .build()
            )

            if (contact.photoUri == null) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
            } else if (photoBytes != null) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        )
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                )
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (contact.nickname != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, contact.nickname)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                        .build()
                )
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            if (contact.notes != null) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.notes)
                        .build()
                )
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            effectivePhones.forEach { entry ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, entry.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, entry.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, entry.label)
                        .build()
                )
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            effectiveEmails.forEach { entry ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, entry.address)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, entry.type)
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, entry.label)
                        .build()
                )
            }

            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(primaryRawContactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )
            effectiveAddresses.forEach { address ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
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

        var createdContactId: String? = null
        val success = try {
            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            if (resolvedContactId == null || existingRawContactIds.isEmpty()) {
                val newRawUri = results.getOrNull(0)?.uri
                if (newRawUri != null) {
                    val rawId = try { ContentUris.parseId(newRawUri) } catch (e: Exception) { null }
                    if (rawId != null) {
                        for (i in 0..5) {
                            try {
                                contentResolver.query(
                                    ContactsContract.RawContacts.CONTENT_URI,
                                    arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                                    "${ContactsContract.RawContacts._ID} = ?",
                                    arrayOf(rawId.toString()),
                                    null
                                )?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val idx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                                        if (idx != -1) {
                                            val cid = cursor.getString(idx)
                                            if (!cid.isNullOrBlank()) {
                                                createdContactId = cid
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                            if (createdContactId != null) break
                            Thread.sleep(40)
                        }
                        if (createdContactId == null) {
                            createdContactId = rawId.toString()
                        }
                    }
                }
            } else {
                createdContactId = resolvedContactId
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error applying batch operations for contact save", e)
            false
        }

        if (success) {
            val targetId = createdContactId ?: contact.id
            val currentFavNum = preferenceManager.getFavoriteNumber(targetId)
            if (currentFavNum != null) {
                val stillExists = effectivePhones.any { areNumbersEqual(it.number, currentFavNum) }
                if (!stillExists) {
                    val newPrimary = effectivePhones.firstOrNull()?.number
                    if (newPrimary != null) {
                        preferenceManager.setFavoriteNumber(targetId, newPrimary)
                    } else {
                        preferenceManager.setFavoriteNumber(targetId, null)
                    }
                }
            }
        }
        return Pair(success, createdContactId)
    }
    private fun clearCallBackground(contactId: String) {
        val numbers: List<String> = try {
            getContactById(contactId)?.phoneNumbers ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        CallBackgroundStore.clearBlocking(context, contactId, numbers)
    }

    private fun deleteContactInternal(contactId: String, clearBackground: Boolean) {
        if (contactId.isBlank() || contactId == "0" || contactId == "null") {
            android.util.Log.w("ContactsRepo", "Aborted deleteContactInternal: invalid contactId '$contactId'")
            return
        }
        if (clearBackground) clearCallBackground(contactId)
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.deleteById(id)
            return
        }
        val resolvedId = if (contactId.toLongOrNull() != null) {
            contactId
        } else {
            resolveLookupKey(contactId)
        }

        if (resolvedId.isNullOrBlank() || resolvedId == "0" || resolvedId == "null") {
            android.util.Log.w("ContactsRepo", "Aborted deleteContactInternal: cannot safely resolve ID for '$contactId'")
            return
        }

        // 1. Delete all raw contacts associated with this contact by CONTACT_ID
        try {
            contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(resolvedId)
            )
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error deleting raw contacts by CONTACT_ID $resolvedId", t)
        }

        // 2. Query any explicit raw contact IDs and delete each
        try {
            val rawIds = getRawContactIds(contactId).toMutableSet()
            if (resolvedId != contactId) {
                rawIds.addAll(getRawContactIds(resolvedId))
            }
            for (rawId in rawIds) {
                if (rawId.isNotBlank() && rawId != "0" && rawId != "null") {
                    try {
                        val rawUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId.toLong())
                        contentResolver.delete(rawUri, null, null)
                    } catch (e: Exception) {
                        android.util.Log.e("ContactsRepo", "Error deleting raw contact uri $rawId", e)
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error querying and deleting raw contacts for: $contactId", t)
        }

        // 3. In case resolvedId is directly a raw contact _ID
        try {
            contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(resolvedId)
            )
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error deleting raw contact by _ID: $resolvedId", t)
        }

        // 4. Delete Data rows linked to this contact
        try {
            contentResolver.delete(
                ContactsContract.Data.CONTENT_URI,
                "${ContactsContract.Data.CONTACT_ID} = ? OR ${ContactsContract.Data.RAW_CONTACT_ID} = ?",
                arrayOf(resolvedId, resolvedId)
            )
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error deleting data rows for: $resolvedId", t)
        }

        // 5. Delete aggregate contact URI
        try {
            if (resolvedId.toLongOrNull() != null) {
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, resolvedId.toLong())
                contentResolver.delete(uri, null, null)
            }
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error deleting contact uri $resolvedId", t)
        }

        // 6. Delete via lookup URI (only if contactId is a valid non-numeric string and not empty/blank)
        try {
            if (contactId.isNotBlank() && contactId != "0" && contactId != "null" && contactId.toLongOrNull() == null) {
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                    Uri.encode(contactId)
                )
                contentResolver.delete(lookupUri, null, null)
            }
        } catch (t: Throwable) {
            // Ignore
        }
    }

    override fun deleteContact(contactId: String) {
        deleteContactInternal(contactId, true)
    }

    override fun deleteContacts(contactIds: List<String>) {
        val validIds = contactIds.filter { it.isNotBlank() && it != "0" && it != "null" }
        if (validIds.isEmpty()) return
        validIds.chunked(50).forEach { chunk ->
            val ops = ArrayList<ContentProviderOperation>()
            chunk.forEach { id ->
                clearCallBackground(id)
                if (id.startsWith("p")) {
                    val lid = id.substring(1).toLongOrNull()
                    if (lid != null) privateContactDao.deleteById(lid)
                    return@forEach
                }
                val resolvedId = if (id.toLongOrNull() != null) id else resolveLookupKey(id)
                if (resolvedId.isNullOrBlank() || resolvedId == "0" || resolvedId == "null") {
                    return@forEach
                }

                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.RawContacts.CONTACT_ID} = ? OR ${ContactsContract.RawContacts._ID} = ?",
                            arrayOf(resolvedId, resolvedId)
                        )
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? OR ${ContactsContract.Data.RAW_CONTACT_ID} = ?",
                            arrayOf(resolvedId, resolvedId)
                        )
                        .build()
                )
                if (resolvedId.toLongOrNull() != null) {
                    val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, resolvedId.toLong())
                    ops.add(ContentProviderOperation.newDelete(uri).build())
                }
            }
            if (ops.isNotEmpty()) {
                try {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                } catch (t: Throwable) {
                    android.util.Log.e("ContactsRepo", "Error batch deleting contacts with applyBatch, fallback to single delete", t)
                    chunk.forEach { id ->
                        deleteContactInternal(id, clearBackground = false)
                    }
                }
            }
        }
    }

    override fun moveContacts(contactIds: List<String>, accountName: String?, accountType: String?) {
        val isTargetPrivate = accountType == "com.grinch.rivo4.private" || accountName == "private"

        contactIds.forEach { id ->
            try {
                if (isTargetPrivate) {
                    if (!id.startsWith("p")) {
                        makeContactPrivate(id)
                    }
                } else {
                    val contact = getContactById(id) ?: return@forEach
                    val targetContact = contact.copy(
                        id = "",
                        accountName = accountName,
                        accountType = accountType,
                        isPrivate = false
                    )
                    val saved = saveContact(targetContact)
                    if (saved) {
                        if (contact.isPrivate) {
                            val lid = id.substring(1).toLongOrNull()
                            if (lid != null) privateContactDao.deleteById(lid)
                        } else {
                            deleteContactInternal(id, clearBackground = false)
                        }
                    } else {
                        android.util.Log.e("ContactsRepo", "Failed to save moved contact: ${contact.name} (id: $id) to $accountName/$accountType")
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("ContactsRepo", "Error moving contact $id", t)
            }
        }
    }

    override fun getAvailableAccounts(): List<Account> {
        return try {
            val allAccounts = AccountManager.get(context).accounts.toList()
            val syncAdapters = ContentResolver.getSyncAdapterTypes()
            val contactAccountTypes = syncAdapters
                .filter { it.authority == ContactsContract.AUTHORITY }
                .map { it.accountType }
                .toSet()

            val existingAccountKeys = mutableSetOf<String>()
            try {
                contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                    val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    while (cursor.moveToNext()) {
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null
                        val type = if (typeCol != -1) cursor.getString(typeCol) else null
                        if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                            existingAccountKeys.add("$name|$type")
                        }
                    }
                }
            } catch (e: Exception) {
            }

            val filtered = allAccounts.filter { account ->
                val isSyncable = try { ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY) > 0 } catch (e: Exception) { false }
                val matchesType = account.type in contactAccountTypes
                val hasRawContacts = existingAccountKeys.contains("${account.name}|${account.type}")
                (matchesType && isSyncable) || hasRawContacts
            }

            if (filtered.isNotEmpty()) filtered else allAccounts
        } catch (e: SecurityException) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getContactByNumber(number: String): Contact? {
        if (com.grinch.rivo4.controller.util.isVoicemailNumber(context, number)) {
            return Contact(
                id = "voicemail",
                name = context.getString(R.string.settings_voicemail_title),
                photoUri = "voicemail://icon",
                phoneNumbers = listOf(number)
            )
        }

        privateContactDao.getAll().forEach {
            val contact = it.toContact()
            if (contact.phoneNumbers.any { num -> areNumbersEqual(num, number) }) {
                return contact
            }
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.CONTACT_ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.STARRED,
            ContactsContract.PhoneLookup.CUSTOM_RINGTONE
        )

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val starredIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                    val ringtoneIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CUSTOM_RINGTONE)

                    val id = if (idIdx != -1) cursor.getString(idIdx).orEmpty() else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else unknownLabel
                    val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) else null
                    val starred = if (starredIdx != -1) cursor.getInt(starredIdx) == 1 else false
                    val ringtone = if (ringtoneIdx != -1) cursor.getString(ringtoneIdx) else null

                    val numbers = if (id.isNotBlank()) {
                        deduplicateNumbers(listOf(number) + getNumbersForContactId(id))
                    } else {
                        listOf(number)
                    }

                    val (accInfo, linked) = if (id.isNotBlank()) getAccountInfoWithLinked(id) else Pair(Pair(null, null), emptyList())
                    return Contact(
                        id = id,
                        name = formatName(name ?: unknownLabel),
                        photoUri = photoUri,
                        isFavorite = starred,
                        phoneNumbers = numbers,
                        customRingtone = ringtone,
                        accountName = accInfo.first,
                        accountType = accInfo.second,
                        linkedAccounts = linked
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return null
    }

    override fun findDuplicates(): List<List<Contact>> {
        val allContacts = getContacts(includePrivate = true, includeHidden = preferenceManager.isHiddenContactsVisible())
        if (allContacts.size < 2) return emptyList()

        val parent = IntArray(allContacts.size) { it }
        fun find(i: Int): Int {
            var root = i
            while (root != parent[root]) root = parent[root]
            var curr = i
            while (curr != root) {
                val nxt = parent[curr]
                parent[curr] = root
                curr = nxt
            }
            return root
        }
        fun union(i: Int, j: Int) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootI] = rootJ
            }
        }

        val nameMap = mutableMapOf<String, Int>()
        val phoneBuckets = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        val emailMap = mutableMapOf<String, Int>()

        val invalidNames = setOf("", "unnamed", "(no name)", "no name", unknownLabel.lowercase(), "unknown", "null")
        allContacts.forEachIndexed { index, contact ->
            val cleanName = contact.name.trim().lowercase()
            val validNumbers = contact.phoneNumbers.filter { it.filter { c -> c.isDigit() }.length >= 5 }
            val validEmails = contact.emails.map { it.trim().lowercase() }.filter { it.length >= 5 && it.contains('@') }

            if (cleanName.isNotBlank() && !invalidNames.contains(cleanName)) {
                val existing = nameMap[cleanName]
                if (existing != null) {
                    val other = allContacts[existing]
                    val otherNumbers = other.phoneNumbers.filter { it.filter { c -> c.isDigit() }.length >= 5 }
                    if (validNumbers.isEmpty() || otherNumbers.isEmpty()) {
                        union(index, existing)
                    }
                } else {
                    nameMap[cleanName] = index
                }
            }

            validNumbers.forEach { cleanNum ->
                val digits = cleanNum.filter { it.isDigit() }
                val bucketKey = if (digits.length >= 7) digits.takeLast(7) else digits
                val bucket = phoneBuckets.getOrPut(bucketKey) { mutableListOf() }
                val match = bucket.find { areNumbersEqual(it.first, cleanNum) }
                if (match != null) {
                    union(index, match.second)
                } else {
                    bucket.add(Pair(cleanNum, index))
                }
            }

            validEmails.forEach { cleanEmail ->
                val existing = emailMap[cleanEmail]
                if (existing != null) {
                    union(index, existing)
                } else {
                    emailMap[cleanEmail] = index
                }
            }
        }

        val groups = mutableMapOf<Int, MutableList<Contact>>()
        allContacts.forEachIndexed { index, contact ->
            val root = find(index)
            groups.getOrPut(root) { mutableListOf() }.add(contact)
        }

        return groups.values
            .filter { it.size > 1 }
            .map { group ->
                group.sortedWith(
                    compareByDescending<Contact> { it.photoUri != null }
                        .thenByDescending { it.phoneNumbers.size }
                        .thenByDescending { it.emails.size }
                        .thenByDescending { !it.isPrivate }
                        .thenBy { it.id }
                )
            }
    }

    override fun mergeContacts(targetContactId: String, sourceContactIds: List<String>) {
        val targetContact = getContactById(targetContactId) ?: return
        val sources = sourceContactIds.filter { it != targetContactId }.mapNotNull { getContactById(it) }
        if (sources.isEmpty()) return

        val mergedNumbers = targetContact.phoneNumbers.toMutableList()
        val mergedPhones = targetContact.phones.toMutableList()
        sources.forEach { source ->
            source.phoneNumbers.forEach { num ->
                if (mergedNumbers.none { areNumbersEqual(it, num) }) {
                    mergedNumbers.add(num)
                }
            }
            source.phones.forEach { entry ->
                if (mergedPhones.none { areNumbersEqual(it.number, entry.number) }) {
                    mergedPhones.add(entry)
                }
            }
        }

        val mergedEmails = targetContact.emails.toMutableList()
        val mergedEmailEntries = targetContact.emailEntries.toMutableList()
        sources.forEach { source ->
            source.emails.forEach { email ->
                if (mergedEmails.none { it.equals(email, ignoreCase = true) }) {
                    mergedEmails.add(email)
                }
            }
            source.emailEntries.forEach { entry ->
                if (mergedEmailEntries.none { it.address.equals(entry.address, ignoreCase = true) }) {
                    mergedEmailEntries.add(entry)
                }
            }
        }

        val mergedAddresses = targetContact.addresses.toMutableList()
        sources.forEach { source ->
            source.addresses.forEach { addr ->
                if (mergedAddresses.none { it.equals(addr, ignoreCase = true) }) {
                    mergedAddresses.add(addr)
                }
            }
        }

        val notesList = mutableListOf<String>()
        if (!targetContact.notes.isNullOrBlank()) notesList.add(targetContact.notes)
        sources.forEach { source ->
            if (!source.notes.isNullOrBlank() && !notesList.contains(source.notes)) {
                notesList.add(source.notes)
            }
        }
        val mergedNotes = if (notesList.isNotEmpty()) notesList.joinToString("\n---\n") else null

        val mergedPhoto = targetContact.photoUri ?: sources.firstNotNullOfOrNull { it.photoUri }
        val isFav = targetContact.isFavorite || sources.any { it.isFavorite }

        val updatedTarget = targetContact.copy(
            phoneNumbers = mergedNumbers,
            phones = mergedPhones,
            emails = mergedEmails,
            emailEntries = mergedEmailEntries,
            addresses = mergedAddresses,
            notes = mergedNotes,
            photoUri = mergedPhoto,
            isFavorite = isFav
        )

        saveContact(updatedTarget)

        sources.forEach { source ->
            if (targetContact.photoUri == null && source.photoUri != null) {
                CallBackgroundStore.carryBlocking(context, source.id, targetContactId, source.phoneNumbers)
            } else {
                CallBackgroundStore.clearBlocking(context, source.id, source.phoneNumbers)
            }
        }
        deleteContacts(sources.map { it.id })
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
        try {
            val contact = getContactById(contactId) ?: return
            if (contact.isPrivate) return

            val privateContact = contact.copy(isPrivate = true)
            val localId = privateContactDao.insert(PrivateContactEntity.fromContact(privateContact))

            if (localId > 0) {
                deleteContactInternal(contactId, false)
                CallBackgroundStore.carryBlocking(context, contactId, "p$localId", contact.phoneNumbers)
            }
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error making contact private: $contactId", t)
        }
    }

    override fun makeContactPublic(contactId: String) {
        try {
            val contact = getContactById(contactId) ?: return
            if (!contact.isPrivate) return

            val saved = saveContact(contact.copy(id = "", isPrivate = false))
            if (saved) {
                deleteContactInternal(contactId, false)
                val newId = contact.phoneNumbers.firstNotNullOfOrNull { number ->
                    getContactByNumber(number)?.id?.takeIf { it.isNotBlank() && !it.startsWith("p") }
                }
                CallBackgroundStore.carryBlocking(context, contactId, newId, contact.phoneNumbers)
            }
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepo", "Error making contact public: $contactId", t)
        }
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
                        name = if (name.isBlank()) numbers.firstOrNull() ?: unknownLabel else name,
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

    override fun setContactHidden(contactId: String, isHidden: Boolean) {
        if (contactId.startsWith("p")) {
            val id = contactId.substring(1).toLongOrNull() ?: return
            privateContactDao.setHidden(id, isHidden)
        }
    }

    override fun getHiddenNumbers(): List<String> {
        return try {
            privateContactDao.getAll()
                .filter { it.isHidden }
                .flatMap { entity ->
                    runCatching { Json.decodeFromString<List<String>>(entity.phoneNumbersJson) }
                        .getOrDefault(emptyList())
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun isNumberHidden(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = number.replace(" ", "")
        return getHiddenNumbers().any { areNumbersEqual(it, clean) }
    }

    override fun getTrashedContacts(): List<TrashedContactEntity> {
        pruneOldTrash()
        return try {
            trashedContactDao.getAll()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun restoreTrashedContact(localId: Long): Boolean {
        val entry = trashedContactDao.getById(localId) ?: return false
        val contact = entry.toContact() ?: return false
        saveContact(contact.copy(id = "0"))
        trashedContactDao.deleteById(localId)
        return true
    }

    override fun permanentlyDeleteTrashedContact(localId: Long) {
        trashedContactDao.deleteById(localId)
    }

    override fun emptyTrash() {
        trashedContactDao.deleteAll()
    }

    override fun pruneOldTrash() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        try {
            trashedContactDao.pruneOlderThan(thirtyDaysAgo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}