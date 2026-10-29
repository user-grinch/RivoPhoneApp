package com.grinch.rivo4.modal.data

import android.provider.ContactsContract
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

@Immutable
@Serializable
data class PhoneNumberEntry(
    val number: String,
    val type: Int = 2,
    val label: String? = null
)

@Immutable
@Serializable
data class EmailEntry(
    val address: String,
    val type: Int = 1,
    val label: String? = null
)

@Immutable
@Serializable
data class AccountEntry(
    val name: String?,
    val type: String?
)

@Immutable
@Serializable
data class OrganizationEntry(
    val company: String,
    val title: String? = null,
    val department: String? = null
)

@Immutable
@Serializable
data class WebsiteEntry(
    val url: String,
    val type: Int = ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE,
    val label: String? = null
)

@Immutable
@Serializable
data class RelationEntry(
    val name: String,
    val type: Int = ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND,
    val label: String? = null
)

@Immutable
@Serializable
data class PostalAddressEntry(
    val address: String,
    val type: Int = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
    val label: String? = null
)

@Immutable
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
    val addressEntries: List<PostalAddressEntry> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val organizations: List<OrganizationEntry> = emptyList(),
    val websites: List<WebsiteEntry> = emptyList(),
    val relations: List<RelationEntry> = emptyList(),
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
            val sanitized = com.grinch.rivo4.controller.util.ContactUtils.sanitizeStructuredName(
                prefix = prefix,
                givenName = givenName,
                middleName = middleName,
                familyName = familyName,
                suffix = suffix,
                fallbackFullName = name.takeIf { it.isNotBlank() && it != "Unnamed" && it != "Unknown" }
            )
            return if (sanitized.fullName.isNotBlank() && sanitized.fullName != "Unnamed" && sanitized.fullName != "Unknown") {
                sanitized.fullName
            } else if (name.isNotBlank() && name != "Unnamed" && name != "Unknown") {
                com.grinch.rivo4.controller.util.ContactUtils.collapseAdjacentDuplicateTokens(name)
            } else if (!nickname.isNullOrBlank()) {
                nickname
            } else if (emails.isNotEmpty()) {
                emails.first()
            } else if (emailEntries.isNotEmpty()) {
                emailEntries.first().address
            } else if (phoneNumbers.isNotEmpty()) {
                phoneNumbers.first()
            } else {
                name.ifBlank { "Unnamed" }
            }
        }
}
