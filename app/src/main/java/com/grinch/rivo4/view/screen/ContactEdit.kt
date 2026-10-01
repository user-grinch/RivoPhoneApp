package com.grinch.rivo4.view.screen
import com.grinch.rivo4.view.components.RivoTopBarIconButton

import android.accounts.Account
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactTypeLabels
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import com.grinch.rivo4.view.components.RivoDropdownMenu
import com.grinch.rivo4.view.components.RivoDropdownMenuItem
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectionDialog
import com.grinch.rivo4.view.components.rivoGroupedItemShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

private data class EditablePhone(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val number: String,
    val type: Int = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    val label: String? = null
)

private data class EditableEmail(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val address: String,
    val type: Int = ContactsContract.CommonDataKinds.Email.TYPE_HOME,
    val label: String? = null
)

private data class EditableAddress(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactEditScreen(
    contactId: String? = null,
    initialName: String? = null,
    initialPhone: String? = null,
    navigator: DestinationsNavigator
) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val availableAccounts by contactsVM.availableAccounts.collectAsState()
    val context = LocalContext.current

    val initialSplit = remember { splitDisplayName(initialName ?: "") }
    var givenName by remember { mutableStateOf(initialSplit.givenName) }
    var middleName by remember { mutableStateOf(initialSplit.middleName) }
    var familyName by remember { mutableStateOf(initialSplit.familyName) }
    var nickname by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isPrivate by remember { mutableStateOf(false) }

    val phones = remember {
        mutableStateListOf(
            if (!initialPhone.isNullOrBlank() && (contactId == null || contactId == "0" || contactId == "null")) {
                EditablePhone(number = initialPhone)
            } else {
                EditablePhone(number = "")
            }
        )
    }
    val emails = remember { mutableStateListOf(EditableEmail(address = "")) }
    val addresses = remember { mutableStateListOf(EditableAddress(address = "")) }

    val displayName = listOfNotNull(
        givenName.trim().ifBlank { null },
        middleName.trim().ifBlank { null },
        familyName.trim().ifBlank { null }
    ).joinToString(" ")

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var loadedContactId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(contactId) {
        if (loadedContactId != contactId && contactId != null && contactId != "0" && contactId != "null") {
            val existing = contactsVM.getFullContactById(contactId)
            if (existing != null) {
                if (!existing.givenName.isNullOrBlank() || !existing.middleName.isNullOrBlank() || !existing.familyName.isNullOrBlank()) {
                    givenName = existing.givenName ?: ""
                    middleName = existing.middleName ?: ""
                    familyName = existing.familyName ?: ""
                } else {
                    val split = splitDisplayName(existing.name)
                    givenName = split.givenName
                    middleName = split.middleName
                    familyName = split.familyName
                }
                nickname = existing.nickname ?: ""
                notes = existing.notes ?: ""
                photoUri = existing.photoUri
                isPrivate = existing.isPrivate

                phones.clear()
                val existingPhones = existing.phones.ifEmpty {
                    existing.phoneNumbers.map { PhoneNumberEntry(it) }
                }
                if (existingPhones.isNotEmpty()) {
                    phones.addAll(existingPhones.map {
                        EditablePhone(number = it.number, type = it.type, label = it.label)
                    })
                } else {
                    phones.add(EditablePhone(number = ""))
                }
                if (!initialPhone.isNullOrBlank()) {
                    val cleanInitial = initialPhone.replace(Regex("[^0-9+]"), "")
                    val alreadyPresent = phones.any {
                        it.number.replace(Regex("[^0-9+]"), "") == cleanInitial
                    }
                    if (!alreadyPresent) {
                        if (phones.size == 1 && phones[0].number.isBlank()) {
                            phones[0] = EditablePhone(number = initialPhone)
                        } else {
                            phones.add(EditablePhone(number = initialPhone))
                        }
                    }
                }

                emails.clear()
                val existingEmails = existing.emailEntries.ifEmpty {
                    existing.emails.map { EmailEntry(it) }
                }
                if (existingEmails.isNotEmpty()) {
                    emails.addAll(existingEmails.map {
                        EditableEmail(address = it.address, type = it.type, label = it.label)
                    })
                } else {
                    emails.add(EditableEmail(address = ""))
                }

                addresses.clear()
                if (existing.addresses.isNotEmpty()) {
                    addresses.addAll(existing.addresses.map { EditableAddress(address = it) })
                } else {
                    addresses.add(EditableAddress(address = ""))
                }
                loadedContactId = contactId
            }
        }
    }

    LaunchedEffect(availableAccounts) {
        if (contactId != null && contactId != "0" && contactId != "null") {
            if (selectedAccount == null && !isPrivate) {
                val existing = contactsVM.getFullContactById(contactId)
                if (existing != null) {
                    selectedAccount = availableAccounts.find {
                        it.name == existing.accountName && it.type == existing.accountType
                    }
                }
            }
        } else {
            if (selectedAccount == null) {
                val lastUsed = contactsVM.getLastUsedAccount()
                if (lastUsed != null) {
                    selectedAccount = availableAccounts.find {
                        it.name == lastUsed.name && it.type == lastUsed.type
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (contactId == null || contactId == "0") stringResource(R.string.contact_create_title) else stringResource(
                            R.string.contact_edit_title
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                },
                navigationIcon = {
                    Surface(
                        onClick = { navigator.navigateUp() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (contactId != null && contactId != "0") {
                        RivoTopBarIconButton(
                            onClick = { showDeleteDialog = true },
                            icon = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val savedPhones = phones.filter { it.number.isNotBlank() }.map {
                                    PhoneNumberEntry(number = it.number.trim(), type = it.type, label = it.label)
                                }
                                val savedEmails = emails.filter { it.address.isNotBlank() }.map {
                                    EmailEntry(address = it.address.trim(), type = it.type, label = it.label)
                                }
                                val savedAddresses = addresses.map { it.address.trim() }.filter { it.isNotBlank() }
                                val contactToSave = Contact(
                                    id = if (contactId == "null" || contactId == "0" || contactId == null) "0" else contactId,
                                    name = displayName.ifBlank {
                                        nickname.trim().ifBlank { savedPhones.firstOrNull()?.number ?: "Unnamed" }
                                    },
                                    givenName = givenName.trim().ifBlank { null },
                                    middleName = middleName.trim().ifBlank { null },
                                    familyName = familyName.trim().ifBlank { null },
                                    nickname = nickname.ifBlank { null },
                                    phoneNumbers = savedPhones.map { it.number },
                                    emails = savedEmails.map { it.address },
                                    phones = savedPhones,
                                    emailEntries = savedEmails,
                                    addresses = savedAddresses,
                                    photoUri = photoUri,
                                    accountName = if (isPrivate) null else selectedAccount?.name,
                                    accountType = if (isPrivate) null else selectedAccount?.type,
                                    isPrivate = isPrivate,
                                    notes = notes.ifBlank { null }
                                )
                                val success = contactsVM.saveContact(contactToSave)
                                if (success) {
                                    navigator.navigateUp()
                                } else {
                                    isSaving = false
                                    Toast.makeText(context, "Failed to save contact", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = (displayName.isNotBlank() || nickname.isNotBlank() || phones.any { it.number.isNotBlank() }) && !isSaving,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (isSaving) {
                            LoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Check, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showDeleteDialog) {
            RivoConfirmationDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    if (contactId != null) {
                        contactsVM.deleteContact(contactId)
                        navigator.navigateUp()
                    }
                },
                title = stringResource(R.string.contact_delete_dialog_title),
                message = stringResource(R.string.contact_delete_dialog_message),
                confirmLabel = stringResource(R.string.action_delete),
                icon = Icons.Default.Delete,
                isDestructive = true
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expressive Avatar & Photo Picker
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        RivoAvatar(
                            name = displayName.ifBlank { nickname },
                            photoUri = photoUri,
                            modifier = Modifier.size(112.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.offset(x = 8.dp, y = 8.dp)
                        ) {
                            if (photoUri != null) {
                                FilledTonalIconButton(
                                    onClick = { photoUri = null },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save to Account Group
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_account_header),
                    icon = Icons.Outlined.Cloud
                ) {
                    item {
                        var showPicker by remember { mutableStateOf(false) }

                        RivoListItem(
                            headline = stringResource(R.string.contact_edit_save_to_account),
                            supporting = when {
                                isPrivate -> stringResource(R.string.contact_edit_private_storage)
                                selectedAccount != null -> ContactUtils.getFriendlyAccountName(
                                    context,
                                    selectedAccount!!
                                )

                                else -> stringResource(R.string.label_local_memory)
                            },
                            leadingIcon = when {
                                isPrivate -> Icons.Default.Lock
                                selectedAccount != null -> ContactUtils.getAccountIcon(selectedAccount!!)
                                else -> Icons.Default.CloudOff
                            },
                            trailingIcon = Icons.Default.ArrowDropDown,
                            onClick = { showPicker = true }
                        )

                        if (showPicker) {
                            val privateTitle = stringResource(R.string.contact_edit_private_storage)
                            val privateDesc = stringResource(R.string.contact_edit_private_storage_description)
                            val localTitle = stringResource(R.string.label_local_memory)

                            val accountOptions = remember(availableAccounts, privateTitle, localTitle) {
                                listOf(
                                    "private" to Triple(privateTitle, privateDesc, Icons.Default.Lock),
                                    "local" to Triple(localTitle, "", Icons.Default.CloudOff)
                                ) + availableAccounts.map { acc ->
                                    acc.name to Triple(
                                        ContactUtils.getFriendlyAccountName(context, acc),
                                        acc.name,
                                        ContactUtils.getAccountIcon(acc)
                                    )
                                }
                            }

                            RivoSelectionDialog(
                                onDismissRequest = { showPicker = false },
                                title = stringResource(R.string.contact_edit_select_account_title),
                                icon = Icons.Default.AccountBalance,
                                items = accountOptions,
                                itemLabel = { option -> option.second.first },
                                itemSupporting = { option -> option.second.second },
                                itemIcon = { option -> option.second.third },
                                isSelected = { option ->
                                    when (option.first) {
                                        "private" -> isPrivate
                                        "local" -> !isPrivate && selectedAccount == null
                                        else -> !isPrivate && selectedAccount?.name == option.first
                                    }
                                },
                                onItemSelected = { selectedOption ->
                                    when (selectedOption.first) {
                                        "private" -> {
                                            selectedAccount = null
                                            isPrivate = true
                                        }

                                        "local" -> {
                                            selectedAccount = null
                                            isPrivate = false
                                        }

                                        else -> {
                                            selectedAccount = availableAccounts.find { it.name == selectedOption.first }
                                            isPrivate = false
                                        }
                                    }
                                    showPicker = false
                                }
                            )
                        }
                    }
                }
            }

            // Name / Identity Group (Segmented)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_identity_header),
                    icon = Icons.Outlined.Person
                ) {
                    item {
                        RivoSegmentedTextField(
                            value = givenName,
                            onValueChange = { givenName = it },
                            label = stringResource(R.string.contact_edit_first_name),
                            icon = Icons.Outlined.Person
                        )
                    }
                    item {
                        RivoSegmentedTextField(
                            value = middleName,
                            onValueChange = { middleName = it },
                            label = stringResource(R.string.contact_edit_middle_name),
                            icon = Icons.Outlined.PersonOutline
                        )
                    }
                    item {
                        RivoSegmentedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = stringResource(R.string.contact_edit_last_name),
                            icon = Icons.Default.Badge
                        )
                    }
                    item {
                        RivoSegmentedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = stringResource(R.string.contact_edit_nickname),
                            icon = Icons.AutoMirrored.Outlined.Label
                        )
                    }
                }
            }

            // Phone Numbers Group (Segmented)
            item {
                val total = phones.size + 1
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    RivoSectionHeader(
                        title = stringResource(R.string.contact_edit_phone_numbers_header),
                        icon = Icons.Outlined.Phone,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    phones.forEachIndexed { index, phone ->
                        key(phone.id) {
                            val shape = rivoGroupedItemShape(index, total)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                RivoSegmentedTypedField(
                                    value = phone.number,
                                    onValueChange = { phones[index] = phones[index].copy(number = it) },
                                    label = "${stringResource(R.string.contact_edit_phone_field_label)} ${index + 1}",
                                    icon = Icons.Outlined.Phone,
                                    indexBadge = index + 1,
                                    typeValue = phone.type,
                                    typeOptions = ContactTypeLabels.phoneTypeOptions,
                                    typeLabel = { ContactTypeLabels.phoneTypeLabel(context, it, null) },
                                    onTypeChange = { phones[index] = phones[index].copy(type = it) },
                                    onDelete = if (phones.size > 1 || phone.number.isNotBlank()) {
                                        {
                                            if (phones.size > 1) {
                                                phones.removeAt(index)
                                            } else {
                                                phones[0] = EditablePhone(number = "")
                                            }
                                        }
                                    } else null,
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                        }
                    }
                    key("add_phone") {
                        val shape = rivoGroupedItemShape(phones.size, total)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { phones.add(EditablePhone(number = "")) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.contact_edit_add_phone),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Email Addresses Group (Segmented)
            item {
                val total = emails.size + 1
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    RivoSectionHeader(
                        title = stringResource(R.string.contact_edit_emails_header),
                        icon = Icons.Outlined.Email,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    emails.forEachIndexed { index, email ->
                        key(email.id) {
                            val shape = rivoGroupedItemShape(index, total)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                RivoSegmentedTypedField(
                                    value = email.address,
                                    onValueChange = { emails[index] = emails[index].copy(address = it) },
                                    label = "${stringResource(R.string.label_email)} ${index + 1}",
                                    icon = Icons.Outlined.Email,
                                    indexBadge = index + 1,
                                    typeValue = email.type,
                                    typeOptions = ContactTypeLabels.emailTypeOptions,
                                    typeLabel = { ContactTypeLabels.emailTypeLabel(context, it, null) },
                                    onTypeChange = { emails[index] = emails[index].copy(type = it) },
                                    onDelete = if (emails.size > 1 || email.address.isNotBlank()) {
                                        {
                                            if (emails.size > 1) {
                                                emails.removeAt(index)
                                            } else {
                                                emails[0] = EditableEmail(address = "")
                                            }
                                        }
                                    } else null,
                                    keyboardType = KeyboardType.Email
                                )
                            }
                        }
                    }
                    key("add_email") {
                        val shape = rivoGroupedItemShape(emails.size, total)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { emails.add(EditableEmail(address = "")) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.contact_edit_add_email),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Addresses Group (Segmented)
            item {
                val total = addresses.size + 1
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    RivoSectionHeader(
                        title = stringResource(R.string.contact_edit_address_header),
                        icon = Icons.Outlined.LocationOn,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    addresses.forEachIndexed { index, addr ->
                        key(addr.id) {
                            val shape = rivoGroupedItemShape(index, total)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                RivoSegmentedRemovableField(
                                    value = addr.address,
                                    onValueChange = { addresses[index] = addresses[index].copy(address = it) },
                                    label = "${stringResource(R.string.label_address)} ${index + 1}",
                                    icon = Icons.Outlined.LocationOn,
                                    indexBadge = index + 1,
                                    onDelete = if (addresses.size > 1 || addr.address.isNotBlank()) {
                                        {
                                            if (addresses.size > 1) {
                                                addresses.removeAt(index)
                                            } else {
                                                addresses[0] = EditableAddress(address = "")
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                    key("add_address") {
                        val shape = rivoGroupedItemShape(addresses.size, total)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { addresses.add(EditableAddress(address = "")) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.contact_edit_add_address),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Notes Group (Segmented)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_notes_header),
                    icon = Icons.AutoMirrored.Outlined.Notes
                ) {
                    item {
                        RivoSegmentedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = stringResource(R.string.label_notes),
                            icon = Icons.AutoMirrored.Outlined.Notes,
                            singleLine = false,
                            minLines = 3
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

data class NameComponents(
    val givenName: String = "",
    val middleName: String = "",
    val familyName: String = ""
)

fun splitDisplayName(full: String): NameComponents {
    val trimmed = full.trim()
    if (trimmed.isEmpty()) return NameComponents()
    val parts = trimmed.split(Regex("\\s+"))
    return when (parts.size) {
        1 -> NameComponents(givenName = parts[0])
        2 -> NameComponents(givenName = parts[0], familyName = parts[1])
        else -> NameComponents(
            givenName = parts.first(),
            middleName = parts.subList(1, parts.size - 1).joinToString(" "),
            familyName = parts.last()
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RivoSegmentedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = singleLine,
        minLines = minLines
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RivoSegmentedTypedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    typeValue: Int,
    typeOptions: List<Int>,
    typeLabel: (Int) -> String,
    onTypeChange: (Int) -> Unit,
    indexBadge: Int? = null,
    onDelete: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var showTypeMenu by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                leadingIcon = {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (indexBadge != null) {
                                    Text(
                                        text = "$indexBadge",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                trailingIcon = if (value.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(start = 52.dp, top = 2.dp)) {
            Surface(
                onClick = { showTypeMenu = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = typeLabel(typeValue),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RivoDropdownMenu(
                expanded = showTypeMenu,
                onDismissRequest = { showTypeMenu = false }
            ) {
                typeOptions.forEach { option ->
                    RivoDropdownMenuItem(
                        text = { Text(typeLabel(option)) },
                        onClick = {
                            onTypeChange(option)
                            showTypeMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RivoSegmentedRemovableField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    indexBadge: Int? = null,
    onDelete: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (indexBadge != null) {
                                Text(
                                    text = "$indexBadge",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            trailingIcon = if (value.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = false,
            maxLines = 3
        )

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
