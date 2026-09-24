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
data class AccountEntry(
    val name: String?,
    val type: String?
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val prefix: String? = null,
    val givenName: String? = null,
    val middleName: String? = null,
    val familyName: String? = null,
    val suffix: String? = null,
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
    val isHidden: Boolean = false,
    val notes: String? = null,
    val linkedAccounts: List<AccountEntry> = emptyList()
) {
    val formattedDisplayName: String
        get() {
            val parts = listOfNotNull(
                prefix?.trim()?.ifBlank { null },
                givenName?.trim()?.ifBlank { null },
                middleName?.trim()?.ifBlank { null },
                familyName?.trim()?.ifBlank { null },
                suffix?.trim()?.ifBlank { null }
            )
            return if (parts.isNotEmpty()) {
                parts.joinToString(" ")
            } else {
                name
            }
        }
}
