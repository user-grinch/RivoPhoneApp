package com.grinch.rivo4.controller

import android.accounts.Account
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.modal.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(
    private val contactsRepo: IContactsRepository
) : ViewModel() {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<Account>>(emptyList())
    val availableAccounts = _availableAccounts.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount = _selectedAccount.asStateFlow()

    val filteredContacts = combine(_allContacts, _selectedAccount) { contacts, account ->
        if (account == null) {
            contacts
        } else {
            contacts.filter { it.accountName == account.name && it.accountType == account.type }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedContacts = filteredContacts.combine(MutableStateFlow(Unit)) { contacts, _ ->
        val favorites = contacts.filter { it.isFavorite }
        val nonFavs = contacts.filter { !it.isFavorite }

        val mainGroups = nonFavs.groupBy {
            val firstChar = it.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toMutableMap()

        val finalMap = linkedMapOf<Char, List<Contact>>()

        if (favorites.isNotEmpty()) finalMap['❤'] = favorites

        mainGroups.keys.filter { it.isLetter() }.sorted().forEach { char ->
            finalMap[char] = mainGroups[char]!!
        }

        val hashGroup = mainGroups['#']
        if (hashGroup != null) finalMap['#'] = hashGroup

        finalMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        fetchAccounts()
    }

    fun fetchContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = contactsRepo.getContacts()
            _allContacts.value = result
            _isLoading.value = false
        }
    }

    suspend fun getFullContactById(contactId: String): Contact? {
        return withContext(Dispatchers.IO) {
            contactsRepo.getContactById(contactId)
        }
    }
    
    suspend fun getFullContactByNumber(number: String): Contact? {
        return withContext(Dispatchers.IO) {
            contactsRepo.getContactByNumber(number)
        }
    }

    fun fetchAccounts() {
        viewModelScope.launch(Dispatchers.IO) {
            _availableAccounts.value = contactsRepo.getAvailableAccounts()
        }
    }

    fun selectAccount(account: Account?) {
        _selectedAccount.value = account
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.toggleFavorite(contact.id, !contact.isFavorite)
            fetchContacts()
        }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.saveContact(contact)
            fetchContacts()
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            fetchContacts()
        }
    }

    fun deleteContacts(contactIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContacts(contactIds)
            fetchContacts()
        }
    }

    fun moveContacts(contactIds: List<String>, account: Account?) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.moveContacts(contactIds, account?.name, account?.type)
            fetchContacts()
        }
    }

    fun findDuplicates(onResult: (List<List<Contact>>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicates = contactsRepo.findDuplicates()
            withContext(Dispatchers.Main) {
                onResult(duplicates)
            }
        }
    }

    fun mergeContacts(targetId: String, sourceIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.mergeContacts(targetId, sourceIds)
            fetchContacts()
        }
    }

    fun setCustomRingtone(contactId: String, ringtoneUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.setCustomRingtone(contactId, ringtoneUri)
        }
    }

    fun formatAllPhoneNumbers() {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.formatAllPhoneNumbers()
            fetchContacts()
        }
    }
}
