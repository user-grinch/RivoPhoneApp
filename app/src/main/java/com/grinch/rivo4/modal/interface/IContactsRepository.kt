package com.grinch.rivo4.modal.`interface`

import android.accounts.Account
import com.grinch.rivo4.modal.data.Contact

interface IContactsRepository {
    fun getContacts(): List<Contact>
    fun getContactById(contactId: String): Contact?
    fun getContactByNumber(number: String): Contact?
    fun toggleFavorite(contactId: String, isFavorite: Boolean)
    fun saveContact(contact: Contact)
    fun deleteContact(contactId: String)
    fun deleteContacts(contactIds: List<String>)
    fun moveContacts(contactIds: List<String>, accountName: String?, accountType: String?)
    fun getAvailableAccounts(): List<Account>
    fun findDuplicates(): List<List<Contact>>
    fun mergeContacts(targetContactId: String, sourceContactIds: List<String>)
    fun setCustomRingtone(contactId: String, ringtoneUri: String?)
    fun formatAllPhoneNumbers(onProgress: ((current: Int, total: Int) -> Unit)? = null)
}