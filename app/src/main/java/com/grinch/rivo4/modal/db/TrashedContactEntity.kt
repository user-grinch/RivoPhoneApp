package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grinch.rivo4.modal.data.Contact
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "trashed_contacts")
data class TrashedContactEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val originalId: String,
    val name: String,
    val phoneNumbersJson: String,
    val contactJson: String,
    val trashedAt: Long = System.currentTimeMillis()
) {
    fun toContact(): Contact? {
        return runCatching { Json.decodeFromString<Contact>(contactJson) }.getOrNull()
    }

    companion object {
        fun fromContact(contact: Contact): TrashedContactEntity {
            return TrashedContactEntity(
                originalId = contact.id,
                name = contact.formattedDisplayName.ifBlank { contact.name },
                phoneNumbersJson = Json.encodeToString(contact.phoneNumbers),
                contactJson = Json.encodeToString(contact),
                trashedAt = System.currentTimeMillis()
            )
        }
    }
}
