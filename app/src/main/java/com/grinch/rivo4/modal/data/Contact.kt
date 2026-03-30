package com.grinch.rivo4.modal.data

data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
)