package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    val notes: String? = null
) {
    fun toContact(): Contact {
        return Contact(
            id = "p$localId",
            name = name,
            nickname = nickname,
            phoneNumbers = Json.decodeFromString(phoneNumbersJson),
            emails = Json.decodeFromString(emailsJson),
            addresses = Json.decodeFromString(addressesJson),
            events = Json.decodeFromString(eventsJson),
            photoUri = photoUri,
            isFavorite = isFavorite,
            customRingtone = customRingtone,
            isPrivate = true,
            notes = notes
        )
    }

    companion object {
        fun fromContact(contact: Contact): PrivateContactEntity {
            return PrivateContactEntity(
                localId = if (contact.id.startsWith("p")) contact.id.substring(1).toLong() else 0,
                name = contact.name,
                nickname = contact.nickname,
                phoneNumbersJson = Json.encodeToString(contact.phoneNumbers),
                emailsJson = Json.encodeToString(contact.emails),
                addressesJson = Json.encodeToString(contact.addresses),
                eventsJson = Json.encodeToString(contact.events),
                photoUri = contact.photoUri,
                isFavorite = contact.isFavorite,
                customRingtone = contact.customRingtone,
                notes = contact.notes
            )
        }
    }
}
