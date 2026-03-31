package com.grinch.rivo4.modal.`interface`

import com.grinch.rivo4.modal.data.Contact

interface IContactsRepository {
    fun getContacts(): List<Contact>
    fun getContactById(contactId: String): Contact?
    fun getContactByNumber(number: String): Contact?
    fun toggleFavorite(contactId: String, isFavorite: Boolean)
    fun saveContact(contact: Contact)
    fun deleteContact(contactId: String)
}