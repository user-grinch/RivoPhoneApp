package com.grinch.rivo4.view.screen

import android.accounts.Account
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.grinch.rivo4.controller.util.deduplicateNumbers
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDropdownMenu
import com.grinch.rivo4.view.components.RivoDropdownMenuItem
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectionDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

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
    var givenName by remember { mutableStateOf(initialSplit.first) }
    var familyName by remember { mutableStateOf(initialSplit.second) }
    var nickname by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isPrivate by remember { mutableStateOf(false) }

    val phones = remember { mutableStateListOf(PhoneNumberEntry("")) }
    val emails = remember { mutableStateListOf(EmailEntry("")) }
    val addresses = remember { mutableStateListOf<String>("") }

    val displayName = "${givenName.trim()} ${familyName.trim()}".trim()

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(contactId, availableAccounts) {
        if (contactId != null && contactId != "0" && contactId != "null") {
            val existing = contactsVM.getFullContactById(contactId)
            if (existing != null) {
                val split = if (!existing.givenName.isNullOrBlank() || !existing.familyName.isNullOrBlank()) {
                    (existing.givenName ?: "") to (existing.familyName ?: "")
                } else {
                    splitDisplayName(existing.name)
                }
                givenName = split.first
                familyName = split.second
                nickname = existing.nickname ?: ""
                notes = existing.notes ?: ""
                photoUri = existing.photoUri
                isPrivate = existing.isPrivate
                if (selectedAccount == null && !existing.isPrivate) {
                    selectedAccount = availableAccounts.find {
                        it.name == existing.accountName && it.type == existing.accountType
                    }
                }

                phones.clear()
                val existingPhones = existing.phones.ifEmpty {
                    existing.phoneNumbers.map { PhoneNumberEntry(it) }
                }
                if (existingPhones.isNotEmpty()) {
                    phones.addAll(existingPhones)
                } else {
                    phones.add(PhoneNumberEntry(""))
                }

                emails.clear()
                val existingEmails = existing.emailEntries.ifEmpty {
                    existing.emails.map { EmailEntry(it) }
                }
                if (existingEmails.isNotEmpty()) {
                    emails.addAll(existingEmails)
                } else {
                    emails.add(EmailEntry(""))
                }

                addresses.clear()
                if (existing.addresses.isNotEmpty()) {
                    addresses.addAll(existing.addresses)
                } else {
                    addresses.add("")
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

            if (!initialPhone.isNullOrBlank() && phones.all { it.number.isBlank() }) {
                phones.clear()
                phones.add(PhoneNumberEntry(initialPhone))
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (contactId == null || contactId == "0") stringResource(R.string.contact_create_title) else stringResource(R.string.contact_edit_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (contactId != null && contactId != "0") {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    val savedPhones = phones.filter { it.number.isNotBlank() }
                                    val savedEmails = emails.filter { it.address.isNotBlank() }
                                    val contactToSave = Contact(
                                        id = if (contactId == "null" || contactId == "0" || contactId == null) "0" else contactId,
                                        name = displayName,
                                        givenName = givenName.trim().ifBlank { null },
                                        familyName = familyName.trim().ifBlank { null },
                                        nickname = nickname.ifBlank { null },
                                        phoneNumbers = savedPhones.map { it.number },
                                        emails = savedEmails.map { it.address },
                                        phones = savedPhones,
                                        emailEntries = savedEmails,
                                        addresses = addresses.filter { it.isNotBlank() },
                                        photoUri = photoUri,
                                        accountName = if (isPrivate) null else selectedAccount?.name,
                                        accountType = if (isPrivate) null else selectedAccount?.type,
                                        isPrivate = isPrivate,
                                        notes = notes.ifBlank { null }
                                    )
                                    contactsVM.saveContact(contactToSave)
                                    navigator.navigateUp()
                                }
                            },
                            enabled = displayName.isNotBlank() && phones.any { it.number.isNotBlank() },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        RivoAvatar(
                            name = displayName,
                            photoUri = photoUri,
                            modifier = Modifier.size(120.dp)
                        )

                        Row {
                            if (photoUri != null) {
                                SmallFloatingActionButton(
                                    onClick = { photoUri = null },
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_account_header))
                RivoExpressiveCard {
                    var showPicker by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { showPicker = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isPrivate -> Icons.Default.Lock
                                    selectedAccount != null -> ContactUtils.getAccountIcon(selectedAccount!!)
                                    else -> Icons.Default.CloudOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.contact_edit_save_to_account),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when {
                                        isPrivate -> stringResource(R.string.contact_edit_private_storage)
                                        selectedAccount != null -> ContactUtils.getFriendlyAccountName(context, selectedAccount!!)
                                        else -> stringResource(R.string.label_local_memory)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    if (showPicker) {
                        val privateTitle = stringResource(R.string.contact_edit_private_storage)
                        val privateDesc = stringResource(R.string.contact_edit_private_storage_description)
                        val localTitle = stringResource(R.string.label_local_memory)

                        val accountOptions = remember(availableAccounts, privateTitle, localTitle) {
                            listOf(
                                "private" to Triple(privateTitle, privateDesc, Icons.Default.Lock),
                                "local" to Triple(localTitle, "", Icons.Default.CloudOff)
                            ) + availableAccounts.map { acc ->
                                acc.name to Triple(ContactUtils.getFriendlyAccountName(context, acc), acc.name, ContactUtils.getAccountIcon(acc))
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

            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_identity_header))
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = givenName,
                            onValueChange = { givenName = it },
                            label = { Text(stringResource(R.string.contact_edit_first_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = { Text(stringResource(R.string.contact_edit_last_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Badge, null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text(stringResource(R.string.contact_edit_nickname)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }


            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_phone_numbers_header))
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val phoneFieldLabel = stringResource(R.string.contact_edit_phone_field_label)
                        phones.forEachIndexed { index, phone ->
                            TypedEditField(
                                value = phone.number,
                                onValueChange = { phones[index] = phone.copy(number = it) },
                                label = phoneFieldLabel,
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone,
                                typeValue = phone.type,
                                typeOptions = ContactTypeLabels.phoneTypeOptions,
                                typeLabel = { ContactTypeLabels.phoneTypeLabel(context, it, null) },
                                onTypeChange = { phones[index] = phone.copy(type = it) },
                                onDelete = {
                                    if (phones.size > 1) {
                                        phones.removeAt(index)
                                    } else {
                                        phones[0] = PhoneNumberEntry("")
                                    }
                                }
                            )
                        }
                        TextButton(
                            onClick = { phones.add(PhoneNumberEntry("")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.contact_edit_add_phone))
                        }
                    }
                }
            }


            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_emails_header))
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val emailFieldLabel = stringResource(R.string.label_email)
                        emails.forEachIndexed { index, email ->
                            TypedEditField(
                                value = email.address,
                                onValueChange = { emails[index] = email.copy(address = it) },
                                label = emailFieldLabel,
                                icon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email,
                                typeValue = email.type,
                                typeOptions = ContactTypeLabels.emailTypeOptions,
                                typeLabel = { ContactTypeLabels.emailTypeLabel(context, it, null) },
                                onTypeChange = { emails[index] = email.copy(type = it) },
                                onDelete = {
                                    if (emails.size > 1) {
                                        emails.removeAt(index)
                                    } else {
                                        emails[0] = EmailEntry("")
                                    }
                                }
                            )
                        }
                        TextButton(
                            onClick = { emails.add(EmailEntry("")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.contact_edit_add_email))
                        }
                    }
                }
            }


            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_address_header))
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val addressFieldLabel = stringResource(R.string.label_address)
                        addresses.forEachIndexed { index, address ->
                            EditField(
                                value = address,
                                onValueChange = { addresses[index] = it },
                                label = addressFieldLabel,
                                icon = Icons.Default.LocationOn,
                                onDelete = {
                                    if (addresses.size > 1) {
                                        addresses.removeAt(index)
                                    } else {
                                        addresses[0] = ""
                                    }
                                }
                            )
                        }
                        TextButton(
                            onClick = { addresses.add("") },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.contact_edit_add_address))
                        }
                    }
                }
            }

            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_notes_header))
                RivoExpressiveCard {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.label_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

fun splitDisplayName(full: String): Pair<String, String> {
    val trimmed = full.trim()
    if (trimmed.isEmpty()) return "" to ""
    val parts = trimmed.split(Regex("\\s+"))
    return if (parts.size == 1) {
        parts[0] to ""
    } else {
        parts.first() to parts.drop(1).joinToString(" ")
    }
}

@Composable
fun TypedEditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    typeValue: Int,
    typeOptions: List<Int>,
    typeLabel: (Int) -> String,
    onTypeChange: (Int) -> Unit,
    onDelete: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            icon = icon,
            keyboardType = keyboardType,
            onDelete = onDelete
        )
        var showTypeMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.padding(start = 8.dp)) {
            TextButton(onClick = { showTypeMenu = true }) {
                Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(typeLabel(typeValue), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
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
fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    onDelete: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
