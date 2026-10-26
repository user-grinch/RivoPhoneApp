package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val entityJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    coerceInputValues = true
}

@Entity(tableName = "private_contacts")
data class PrivateContactEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val name: String,
    val nickname: String? = null,
    val phoneNumbersJson: String,
    val emailsJson: String,
    val addressesJson: String,
    val addressEntriesJson: String? = null,
    val eventsJson: String,
    val organizationsJson: String? = null,
    val websitesJson: String? = null,
    val relationsJson: String? = null,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val customRingtone: String? = null,
    val isHidden: Boolean = false,
    val notes: String? = null
) {
    fun toContact(): Contact {
        val decodedPhones = runCatching { entityJson.decodeFromString<List<String>>(phoneNumbersJson) }.getOrDefault(emptyList())
        val decodedEmails = runCatching { entityJson.decodeFromString<List<String>>(emailsJson) }.getOrDefault(emptyList())
        val decodedAddresses = runCatching { entityJson.decodeFromString<List<String>>(addressesJson) }.getOrDefault(emptyList())
        val decodedAddressEntries = addressEntriesJson?.let {
            runCatching { entityJson.decodeFromString<List<com.grinch.rivo4.modal.data.PostalAddressEntry>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
        // Legacy rows (v5) have no typed entries: derive them from the plain list with default types
        val effectiveAddressEntries = if (decodedAddressEntries.isNotEmpty()) decodedAddressEntries
        else decodedAddresses.map { com.grinch.rivo4.modal.data.PostalAddressEntry(address = it) }
        val (splitGiven, splitMiddle, splitFamily) = com.grinch.rivo4.controller.util.ContactUtils.splitDisplayName(name)
        return Contact(
            id = "p$localId",
            name = name,
            givenName = splitGiven.ifBlank { null },
            middleName = splitMiddle.ifBlank { null },
            familyName = splitFamily.ifBlank { null },
            nickname = nickname,
            phoneNumbers = decodedPhones,
            phones = decodedPhones.map { PhoneNumberEntry(it) },
            emails = decodedEmails,
            emailEntries = decodedEmails.map { EmailEntry(it) },
            addresses = decodedAddresses,
            addressEntries = effectiveAddressEntries,
            events = runCatching { entityJson.decodeFromString<List<ContactEvent>>(eventsJson) }.getOrDefault(emptyList()),
            organizations = organizationsJson?.let {
                runCatching { entityJson.decodeFromString<List<com.grinch.rivo4.modal.data.OrganizationEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList(),
            websites = websitesJson?.let {
                runCatching { entityJson.decodeFromString<List<com.grinch.rivo4.modal.data.WebsiteEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList(),
            relations = relationsJson?.let {
                runCatching { entityJson.decodeFromString<List<com.grinch.rivo4.modal.data.RelationEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList(),
            photoUri = photoUri,
            isFavorite = isFavorite,
            customRingtone = customRingtone,
            isPrivate = true,
            isHidden = isHidden,
            notes = notes
        )
    }

    companion object {
        fun fromContact(contact: Contact): PrivateContactEntity {
            val effectivePhones = if (contact.phoneNumbers.isNotEmpty()) {
                contact.phoneNumbers
            } else {
                contact.phones.map { it.number }.filter { it.isNotBlank() }
            }
            val effectiveEmails = if (contact.emails.isNotEmpty()) {
                contact.emails
            } else {
                contact.emailEntries.map { it.address }.filter { it.isNotBlank() }
            }
            val effectiveAddressEntries = if (contact.addressEntries.isNotEmpty()) {
                contact.addressEntries
            } else {
                contact.addresses.map { com.grinch.rivo4.modal.data.PostalAddressEntry(address = it) }
            }
            return PrivateContactEntity(
                localId = if (contact.id.startsWith("p")) contact.id.substring(1).toLongOrNull() ?: 0L else 0L,
                name = contact.formattedDisplayName.ifBlank { contact.name },
                nickname = contact.nickname,
                phoneNumbersJson = entityJson.encodeToString(effectivePhones),
                emailsJson = entityJson.encodeToString(effectiveEmails),
                addressesJson = entityJson.encodeToString(contact.addresses),
                addressEntriesJson = entityJson.encodeToString(effectiveAddressEntries),
                eventsJson = entityJson.encodeToString(contact.events),
                organizationsJson = entityJson.encodeToString(contact.organizations),
                websitesJson = entityJson.encodeToString(contact.websites),
                relationsJson = entityJson.encodeToString(contact.relations),
                photoUri = contact.photoUri,
                isFavorite = contact.isFavorite,
                customRingtone = contact.customRingtone,
                isHidden = contact.isHidden,
                notes = contact.notes
            )
        }
    }
}
