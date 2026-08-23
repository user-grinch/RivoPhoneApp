package com.grinch.rivo4.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

@Serializable
data class PhoneNumberEntry(
    val number: String,
    val type: Int = 2,
    val label: String? = null
)

@Serializable
data class EmailEntry(
    val address: String,
    val type: Int = 1,
    val label: String? = null
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val nickname: String? = null,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val phones: List<PhoneNumberEntry> = emptyList(),
    val emailEntries: List<EmailEntry> = emptyList(),
    val addresses: List<String> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val customRingtone: String? = null,
    val accountName: String? = null,
    val accountType: String? = null,
    val isPrivate: Boolean = false,
    val notes: String? = null
)
