package com.grinch.rivo4.controller

import android.accounts.Account
import android.net.Uri
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.controller.util.PreferenceManager
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
    private val contactsRepo: IContactsRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _standardizeProgress = MutableStateFlow<Float?>(null)
    val standardizeProgress: StateFlow<Float?> = _standardizeProgress.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<Account>>(emptyList())
    val availableAccounts = _availableAccounts.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount = _selectedAccount.asStateFlow()

    private val _showPrivateOnly = MutableStateFlow(false)
    val showPrivateOnly = _showPrivateOnly.asStateFlow()

    val filteredContacts = combine(_allContacts, _selectedAccount, _showPrivateOnly) { contacts, account, privateOnly ->
        when {
            privateOnly -> contacts.filter { it.isPrivate }
            account == null -> contacts
            else -> contacts.filter { it.accountName == account.name && it.accountType == account.type }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedContacts = filteredContacts.combine(MutableStateFlow(Unit)) { contacts, _ ->
        val mainGroups = contacts.groupBy {
            val firstChar = it.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toMutableMap()

        val finalMap = linkedMapOf<Char, List<Contact>>()

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
        if (account != null) _showPrivateOnly.value = false
    }

    fun setShowPrivateOnly(show: Boolean) {
        _showPrivateOnly.value = show
        if (show) _selectedAccount.value = null
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFavStatus = !contact.isFavorite
            contactsRepo.toggleFavorite(contact.id, newFavStatus)
            
            val currentOrder = preferenceManager.getFavoritesOrder().toMutableList()
            if (newFavStatus) {
                if (!currentOrder.contains(contact.id)) {
                    currentOrder.add(contact.id)
                    preferenceManager.setFavoritesOrder(currentOrder)
                }
            } else {
                if (currentOrder.contains(contact.id)) {
                    currentOrder.remove(contact.id)
                    preferenceManager.setFavoritesOrder(currentOrder)
                }
            }
            
            fetchContacts()
        }
    }

    suspend fun saveContact(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactsRepo.saveContact(contact)
            
            preferenceManager.setString(PreferenceManager.KEY_LAST_USED_ACCOUNT_NAME, contact.accountName)
            preferenceManager.setString(PreferenceManager.KEY_LAST_USED_ACCOUNT_TYPE, contact.accountType)
            
            fetchContacts()
        }
    }
    
    fun getLastUsedAccount(): Account? {
        val name = preferenceManager.getString(PreferenceManager.KEY_LAST_USED_ACCOUNT_NAME, null)
        val type = preferenceManager.getString(PreferenceManager.KEY_LAST_USED_ACCOUNT_TYPE, null)
        return if (name != null && type != null) {
            Account(name, type)
        } else {
            null
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            
            val currentOrder = preferenceManager.getFavoritesOrder().toMutableList()
            if (currentOrder.contains(contactId)) {
                currentOrder.remove(contactId)
                preferenceManager.setFavoritesOrder(currentOrder)
            }
            
            fetchContacts()
        }
    }

    fun deleteContacts(contactIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContacts(contactIds)
            
            val currentOrder = preferenceManager.getFavoritesOrder().toMutableList()
            var changed = false
            contactIds.forEach { id ->
                if (currentOrder.contains(id)) {
                    currentOrder.remove(id)
                    changed = true
                }
            }
            if (changed) {
                preferenceManager.setFavoritesOrder(currentOrder)
            }
            
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
            _standardizeProgress.value = 0f
            contactsRepo.formatAllPhoneNumbers { current, total ->
                _standardizeProgress.value = if (total > 0) current.toFloat() / total else 1f
            }
            fetchContacts()
            _standardizeProgress.value = null
        }
    }

    fun makeContactPrivate(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.makeContactPrivate(contactId)
            fetchContacts()
        }
    }

    fun makeContactPublic(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.makeContactPublic(contactId)
            fetchContacts()
        }
    }

    fun exportPrivateContacts(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.exportPrivateContacts(uri)
        }
    }

    fun importPrivateContacts(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.importPrivateContacts(uri)
            fetchContacts()
        }
    }
}
