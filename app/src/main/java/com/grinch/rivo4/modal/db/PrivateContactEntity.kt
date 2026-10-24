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
    val eventsJson: String,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val customRingtone: String? = null,
    val isHidden: Boolean = false,
    val notes: String? = null
) {
    fun toContact(): Contact {
        val decodedPhones = runCatching { entityJson.decodeFromString<List<String>>(phoneNumbersJson) }.getOrDefault(emptyList())
        val decodedEmails = runCatching { entityJson.decodeFromString<List<String>>(emailsJson) }.getOrDefault(emptyList())
        return Contact(
            id = "p$localId",
            name = name,
            nickname = nickname,
            phoneNumbers = decodedPhones,
            phones = decodedPhones.map { PhoneNumberEntry(it) },
            emails = decodedEmails,
            emailEntries = decodedEmails.map { EmailEntry(it) },
            addresses = runCatching { entityJson.decodeFromString<List<String>>(addressesJson) }.getOrDefault(emptyList()),
            events = runCatching { entityJson.decodeFromString<List<ContactEvent>>(eventsJson) }.getOrDefault(emptyList()),
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
            return PrivateContactEntity(
                localId = if (contact.id.startsWith("p")) contact.id.substring(1).toLongOrNull() ?: 0L else 0L,
                name = contact.formattedDisplayName.ifBlank { contact.name },
                nickname = contact.nickname,
                phoneNumbersJson = entityJson.encodeToString(effectivePhones),
                emailsJson = entityJson.encodeToString(effectiveEmails),
                addressesJson = entityJson.encodeToString(contact.addresses),
                eventsJson = entityJson.encodeToString(contact.events),
                photoUri = contact.photoUri,
                isFavorite = contact.isFavorite,
                customRingtone = contact.customRingtone,
                isHidden = contact.isHidden,
                notes = contact.notes
            )
        }
    }
}
