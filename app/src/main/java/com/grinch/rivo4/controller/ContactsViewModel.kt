package com.grinch.rivo4.controller

import android.accounts.Account
import android.net.Uri
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.db.TrashedContactDao
import com.grinch.rivo4.modal.db.TrashedContactEntity
import com.grinch.rivo4.controller.util.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Collections

class ContactsViewModel(
    private val contactsRepo: IContactsRepository,
    private val preferenceManager: PreferenceManager,
    private val trashedContactDao: TrashedContactDao,
    private val context: android.content.Context
) : ViewModel() {

    private val recentlyDeletedIds = Collections.synchronizedSet(mutableSetOf<String>())
    private var debounceJob: Job? = null
    private var isObserverRegistered = false

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scheduleRefresh(delayMs = 400)
        }
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scheduleRefresh(delayMs = 400)
        }
    }

    fun registerObserver() {
        if (isObserverRegistered) return
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contentObserver
            )
            isObserverRegistered = true
        } catch (e: Exception) {
            // READ_CONTACTS permission might not be granted yet
        }
    }

    fun unregisterObserver() {
        if (!isObserverRegistered) return
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
            isObserverRegistered = false
        } catch (e: Exception) {
        }
    }

    fun scheduleRefresh(delayMs: Long = 400) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch(Dispatchers.IO) {
            delay(delayMs)
            fetchContactsInternal()
        }
    }

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<List<Contact>>>(emptyList())
    val duplicateGroups: StateFlow<List<List<Contact>>> = _duplicateGroups.asStateFlow()

    private val _trashedContacts = MutableStateFlow<List<TrashedContactEntity>>(emptyList())
    val trashedContacts: StateFlow<List<TrashedContactEntity>> = _trashedContacts.asStateFlow()

    private val _isMerging = MutableStateFlow(false)
    val isMerging: StateFlow<Boolean> = _isMerging.asStateFlow()

    private val _hiddenContactsVisible = MutableStateFlow(preferenceManager.isHiddenContactsVisible())
    val hiddenContactsVisible: StateFlow<Boolean> = _hiddenContactsVisible.asStateFlow()

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

    private val _showLocalOnly = MutableStateFlow(false)
    val showLocalOnly = _showLocalOnly.asStateFlow()

    private val _visibleAccounts = MutableStateFlow<Set<String>?>(preferenceManager.getVisibleAccounts())
    val visibleAccountsFlow = _visibleAccounts.asStateFlow()

    private val _sortOrder = MutableStateFlow(preferenceManager.getInt(PreferenceManager.KEY_CONTACT_SORT_ORDER, 0))
    val sortOrder = _sortOrder.asStateFlow()

    private val _displayOrder = MutableStateFlow(preferenceManager.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0))
    val displayOrder = _displayOrder.asStateFlow()

    val filteredContacts = combine(
        _allContacts,
        _selectedAccount,
        _showPrivateOnly,
        _showLocalOnly,
        _visibleAccounts,
        _sortOrder,
        _availableAccounts
    ) { args ->
        val contacts = args[0] as List<Contact>
        val account = args[1] as Account?
        val privateOnly = args[2] as Boolean
        val localOnly = args[3] as Boolean
        val visibleAccounts = args[4] as Set<String>?
        val sortOrder = args[5] as Int
        val availableAccountsList = args[6] as List<Account>

        val baseFiltered = when {
            privateOnly -> contacts.filter { it.isPrivate }
            localOnly -> contacts.filter { !it.isPrivate && ContactUtils.isContactLocal(it, availableAccountsList) }
            account == null -> {
                if (visibleAccounts == null) contacts
                else contacts.filter { contact ->
                    if (contact.isPrivate) {
                        true
                    } else {
                        val isLocal = ContactUtils.isContactLocal(contact, availableAccountsList)
                        val isLocalVisible = isLocal && visibleAccounts.contains("local|local")
                        val hasVisibleAccount = contact.linkedAccounts.any { acc ->
                            if (ContactUtils.isLocalAccount(acc.type, acc.name, availableAccountsList)) {
                                visibleAccounts.contains("local|local")
                            } else {
                                visibleAccounts.contains("${acc.type}|${acc.name}")
                            }
                        }
                        val isPrimaryVisible = if (contact.accountType == null || ContactUtils.isLocalAccount(contact.accountType, contact.accountName, availableAccountsList)) {
                            visibleAccounts.contains("local|local")
                        } else {
                            visibleAccounts.contains("${contact.accountType}|${contact.accountName}")
                        }
                        isLocalVisible || hasVisibleAccount || isPrimaryVisible
                    }
                }
            }
            else -> contacts.filter { contact ->
                (contact.accountName == account.name && contact.accountType == account.type) ||
                        contact.linkedAccounts.any { it.name == account.name && it.type == account.type }
            }
        }
        
        if (sortOrder == 1) {
            baseFiltered.sortedBy { contact ->
                val sortKey = when {
                    !contact.familyName.isNullOrBlank() -> contact.familyName
                    !contact.givenName.isNullOrBlank() -> contact.givenName
                    else -> contact.name.split(" ").lastOrNull() ?: contact.name
                }
                ContactUtils.stripTitlePrefix(sortKey).lowercase()
            }
        } else {
            baseFiltered.sortedBy { contact ->
                val sortKey = when {
                    !contact.givenName.isNullOrBlank() -> contact.givenName
                    else -> contact.name
                }
                ContactUtils.stripTitlePrefix(sortKey).lowercase()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedContacts = combine(filteredContacts, _sortOrder) { contacts, sortOrder ->
        val mainGroups = contacts.groupBy { contact ->
            val nameToUse = if (sortOrder == 1) {
                when {
                    !contact.familyName.isNullOrBlank() -> contact.familyName
                    !contact.givenName.isNullOrBlank() -> contact.givenName
                    else -> contact.name.split(" ").lastOrNull() ?: contact.name
                }
            } else {
                when {
                    !contact.givenName.isNullOrBlank() -> contact.givenName
                    else -> contact.name
                }
            }
            val firstChar = ContactUtils.stripTitlePrefix(nameToUse).firstOrNull()?.uppercaseChar() ?: '#'
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
        registerObserver()
    }

    fun syncFavoriteShortcuts() {
        val favContacts = _allContacts.value.filter { it.isFavorite }
        val order = preferenceManager.getFavoritesOrder()
        val sortedFavs = favContacts.sortedWith(compareBy<Contact> { contact ->
            val index = order.indexOf(contact.id)
            if (index != -1) index else Int.MAX_VALUE
        }.thenBy { it.name })
        com.grinch.rivo4.controller.util.DynamicShortcutManager.updateDynamicShortcuts(context, sortedFavs)
    }

    fun fetchContacts() {
        registerObserver()
        if (_allContacts.value.isEmpty()) {
            _isLoading.value = true
        }
        fetchContactsInternal()
    }

    private fun fetchContactsInternal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = contactsRepo.getContacts(
                    includePrivate = true,
                    includeHidden = preferenceManager.isHiddenContactsVisible()
                )
                val filteredResult = if (recentlyDeletedIds.isEmpty()) {
                    result
                } else {
                    result.filter { !recentlyDeletedIds.contains(it.id) }
                }
                _allContacts.value = filteredResult
                _isLoading.value = false
                refreshDuplicates()
                syncFavoriteShortcuts()
            } catch (e: Exception) {
                _isLoading.value = false
                android.util.Log.e("ContactsVM", "Error in fetchContactsInternal", e)
            }
        }
    }

    fun refreshDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            val dupes = contactsRepo.findDuplicates()
            _duplicateGroups.value = dupes
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
        if (account != null) {
            _showPrivateOnly.value = false
            _showLocalOnly.value = false
        }
    }

    fun setShowPrivateOnly(show: Boolean) {
        _showPrivateOnly.value = show
        if (show) {
            _selectedAccount.value = null
            _showLocalOnly.value = false
        }
    }

    fun setShowLocalOnly(show: Boolean) {
        _showLocalOnly.value = show
        if (show) {
            _selectedAccount.value = null
            _showPrivateOnly.value = false
        }
    }

    fun setVisibleAccounts(accounts: Set<String>?) {
        if (accounts == null) {
            preferenceManager.setString(PreferenceManager.KEY_VISIBLE_ACCOUNTS, null)
        } else {
            preferenceManager.setVisibleAccounts(accounts)
        }
        _visibleAccounts.value = accounts
    }

    fun setSortOrder(order: Int) {
        preferenceManager.setInt(PreferenceManager.KEY_CONTACT_SORT_ORDER, order)
        _sortOrder.value = order
    }

    fun setDisplayOrder(order: Int) {
        preferenceManager.setInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, order)
        _displayOrder.value = order
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } catch (e: Exception) {
                android.util.Log.e("ContactsVM", "Error toggling favorite", e)
            }
        }
    }

    suspend fun saveContact(contact: Contact): Boolean {
        return withContext(Dispatchers.IO) {
            val isNew = contact.id.isEmpty() || contact.id == "0" || contact.id == "null"
            val (success, savedId) = contactsRepo.saveContactWithResult(contact)
            if (success) {
                preferenceManager.setString(PreferenceManager.KEY_LAST_USED_ACCOUNT_NAME, contact.accountName)
                preferenceManager.setString(PreferenceManager.KEY_LAST_USED_ACCOUNT_TYPE, contact.accountType)

                val finalId = savedId ?: contact.id
                val updatedContact = contact.copy(id = finalId)

                withContext(Dispatchers.Main) {
                    val currentList = _allContacts.value
                    if (isNew) {
                        _allContacts.value = listOf(updatedContact) + currentList.filter { it.id != finalId }
                    } else {
                        _allContacts.value = currentList.map {
                            if (it.id == contact.id || it.id == finalId) updatedContact else it
                        }
                    }
                }
                scheduleRefresh(delayMs = 500)
            }
            success
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

    fun fetchTrashedContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _trashedContacts.value = contactsRepo.getTrashedContacts()
        }
    }

    fun restoreTrashedContact(localId: Long, onDone: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.restoreTrashedContact(localId)
            _trashedContacts.value = contactsRepo.getTrashedContacts()
            fetchContacts()
            withContext(Dispatchers.Main) {
                onDone?.invoke()
            }
        }
    }

    fun permanentlyDeleteTrashedContact(localId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.permanentlyDeleteTrashedContact(localId)
            _trashedContacts.value = contactsRepo.getTrashedContacts()
        }
    }

    fun emptyTrash(onDone: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.emptyTrash()
            _trashedContacts.value = emptyList()
            withContext(Dispatchers.Main) {
                onDone?.invoke()
            }
        }
    }

    fun deleteContact(contactId: String) {
        recentlyDeletedIds.add(contactId)
        _allContacts.value = _allContacts.value.filter { it.id != contactId && !recentlyDeletedIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (preferenceManager.isContactsTrashEnabled()) {
                    val fullContact = contactsRepo.getContactById(contactId)
                    if (fullContact != null) {
                        trashedContactDao.insert(TrashedContactEntity.fromContact(fullContact))
                    }
                }
                val currentOrder = preferenceManager.getFavoritesOrder().toMutableList()
                if (currentOrder.contains(contactId)) {
                    currentOrder.remove(contactId)
                    preferenceManager.setFavoritesOrder(currentOrder)
                }

                contactsRepo.deleteContact(contactId)

                viewModelScope.launch {
                    delay(5000)
                    recentlyDeletedIds.remove(contactId)
                }

                fetchTrashedContacts()
                scheduleRefresh(delayMs = 600)
            } catch (e: Exception) {
                android.util.Log.e("ContactsVM", "Error deleting contact", e)
            }
        }
    }

    fun deleteContacts(contactIds: List<String>) {
        val idSet = contactIds.toSet()
        recentlyDeletedIds.addAll(idSet)
        _allContacts.value = _allContacts.value.filter { it.id !in idSet && !recentlyDeletedIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (preferenceManager.isContactsTrashEnabled()) {
                    val toTrash = contactIds.mapNotNull { contactsRepo.getContactById(it) }
                    if (toTrash.isNotEmpty()) {
                        trashedContactDao.insertAll(toTrash.map { TrashedContactEntity.fromContact(it) })
                    }
                }
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

                viewModelScope.launch {
                    delay(5000)
                    recentlyDeletedIds.removeAll(idSet)
                }

                fetchTrashedContacts()
                scheduleRefresh(delayMs = 600)
            } catch (e: Exception) {
                android.util.Log.e("ContactsVM", "Error deleting contacts", e)
            }
        }
    }

    fun moveContacts(contactIds: List<String>, account: Account?) {
        val idSet = contactIds.toSet()
        val isPrivateMove = account?.type == "com.grinch.rivo4.private" || account?.name == "private"
        _allContacts.value = _allContacts.value.map { contact ->
            if (contact.id in idSet) {
                if (isPrivateMove) {
                    contact.copy(isPrivate = true, accountName = null, accountType = null)
                } else {
                    contact.copy(accountName = account?.name, accountType = account?.type, isPrivate = false)
                }
            } else contact
        }
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.moveContacts(contactIds, account?.name, account?.type)
            scheduleRefresh(delayMs = 600)
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
            _isMerging.value = true
            try {
                contactsRepo.mergeContacts(targetId, sourceIds)
                fetchContacts()
            } catch (e: Exception) {
                android.util.Log.e("ContactsVM", "Error merging contacts", e)
            } finally {
                _isMerging.value = false
            }
        }
    }

    fun mergeDuplicateGroup(primaryId: String, sourceIds: List<String>) {
        mergeContacts(primaryId, sourceIds)
    }

    fun mergeAllDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            _isMerging.value = true
            try {
                val currentDupes = contactsRepo.findDuplicates()
                for (group in currentDupes) {
                    if (!isActive) break
                    if (group.size > 1) {
                        try {
                            val primary = group.first()
                            val sources = group.drop(1).map { it.id }
                            contactsRepo.mergeContacts(primary.id, sources)
                        } catch (e: Exception) {
                            android.util.Log.e("ContactsVM", "Error merging group", e)
                        }
                        kotlinx.coroutines.delay(40)
                    }
                }
                _duplicateGroups.value = emptyList()
                fetchContacts()
            } catch (e: Exception) {
                android.util.Log.e("ContactsVM", "Error merging all duplicates", e)
            } finally {
                _isMerging.value = false
            }
        }
    }

    fun dismissDuplicateGroup(group: List<Contact>) {
        val groupIds = group.map { it.id }.toSet()
        _duplicateGroups.value = _duplicateGroups.value.filter { g ->
            g.map { it.id }.toSet() != groupIds
        }
    }

    fun moveContactsToStorage(contactIds: List<String>, accountName: String?, accountType: String?, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            contactsRepo.moveContacts(contactIds, accountName, accountType)
            fetchContacts()
            _isLoading.value = false
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
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
        _allContacts.value = _allContacts.value.map {
            if (it.id == contactId) it.copy(isPrivate = true) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.makeContactPrivate(contactId)
            scheduleRefresh(delayMs = 500)
        }
    }

    fun makeContactPublic(contactId: String) {
        _allContacts.value = _allContacts.value.map {
            if (it.id == contactId) it.copy(isPrivate = false) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.makeContactPublic(contactId)
            scheduleRefresh(delayMs = 500)
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

    fun toggleHiddenContactsVisible(): Boolean {
        val newState = !preferenceManager.isHiddenContactsVisible()
        preferenceManager.setHiddenContactsVisible(newState)
        _hiddenContactsVisible.value = newState
        fetchContacts()
        return newState
    }

    fun isNumberHidden(number: String): Boolean {
        return contactsRepo.isNumberHidden(number)
    }

    fun setContactHidden(contactId: String, isHidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.setContactHidden(contactId, isHidden)
            fetchContacts()
        }
    }

    override fun onCleared() {
        super.onCleared()
        debounceJob?.cancel()
        unregisterObserver()
    }
}