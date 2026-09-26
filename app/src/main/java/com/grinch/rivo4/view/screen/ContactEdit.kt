package com.grinch.rivo4.view.screen

import android.accounts.Account
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
                PhoneNumberEntry(initialPhone)
            } else {
                PhoneNumberEntry("")
            }
        )
    }
    val emails = remember { mutableStateListOf(EmailEntry("")) }
    val addresses = remember { mutableStateListOf<String>("") }

    val displayName = listOfNotNull(
        givenName.trim().ifBlank { null },
        middleName.trim().ifBlank { null },
        familyName.trim().ifBlank { null }
    ).joinToString(" ")

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(contactId) {
        if (!isLoaded && contactId != null && contactId != "0" && contactId != "null") {
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
                    phones.addAll(existingPhones)
                } else {
                    phones.add(PhoneNumberEntry(""))
                }
                if (!initialPhone.isNullOrBlank()) {
                    val cleanInitial = initialPhone.replace(Regex("[^0-9+]"), "")
                    val alreadyPresent = phones.any {
                        it.number.replace(Regex("[^0-9+]"), "") == cleanInitial
                    }
                    if (!alreadyPresent) {
                        if (phones.size == 1 && phones[0].number.isBlank()) {
                            phones[0] = PhoneNumberEntry(initialPhone)
                        } else {
                            phones.add(PhoneNumberEntry(initialPhone))
                        }
                    }
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
                isLoaded = true
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (contactId == null || contactId == "0") stringResource(R.string.contact_create_title) else stringResource(
                            R.string.contact_edit_title
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
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
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val savedPhones = phones.filter { it.number.isNotBlank() }
                                val savedEmails = emails.filter { it.address.isNotBlank() }
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
                        enabled = (displayName.isNotBlank() || nickname.isNotBlank() || phones.any { it.number.isNotBlank() }) && !isSaving,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
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
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
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
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_phone_numbers_header),
                    icon = Icons.Outlined.Phone
                ) {
                    phones.forEachIndexed { index, phone ->
                        item(key = "phone_$index") {
                            RivoSegmentedTypedField(
                                value = phone.number,
                                onValueChange = { phones[index] = phone.copy(number = it) },
                                label = stringResource(R.string.contact_edit_phone_field_label),
                                icon = Icons.Outlined.Phone,
                                typeValue = phone.type,
                                typeOptions = ContactTypeLabels.phoneTypeOptions,
                                typeLabel = { ContactTypeLabels.phoneTypeLabel(context, it, null) },
                                onTypeChange = { phones[index] = phone.copy(type = it) },
                                onDelete = if (phones.size > 1 || phone.number.isNotBlank()) {
                                    {
                                        if (phones.size > 1) {
                                            phones.removeAt(index)
                                        } else {
                                            phones[0] = PhoneNumberEntry("")
                                        }
                                    }
                                } else null,
                                keyboardType = KeyboardType.Phone
                            )
                        }
                    }
                    item(key = "add_phone") {
                        RivoListItem(
                            headline = stringResource(R.string.contact_edit_add_phone),
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            headlineColor = MaterialTheme.colorScheme.primary,
                            isCompact = true,
                            onClick = { phones.add(PhoneNumberEntry("")) }
                        )
                    }
                }
            }

            // Email Addresses Group (Segmented)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_emails_header),
                    icon = Icons.Outlined.Email
                ) {
                    emails.forEachIndexed { index, email ->
                        item(key = "email_$index") {
                            RivoSegmentedTypedField(
                                value = email.address,
                                onValueChange = { emails[index] = email.copy(address = it) },
                                label = stringResource(R.string.label_email),
                                icon = Icons.Outlined.Email,
                                typeValue = email.type,
                                typeOptions = ContactTypeLabels.emailTypeOptions,
                                typeLabel = { ContactTypeLabels.emailTypeLabel(context, it, null) },
                                onTypeChange = { emails[index] = email.copy(type = it) },
                                onDelete = if (emails.size > 1 || email.address.isNotBlank()) {
                                    {
                                        if (emails.size > 1) {
                                            emails.removeAt(index)
                                        } else {
                                            emails[0] = EmailEntry("")
                                        }
                                    }
                                } else null,
                                keyboardType = KeyboardType.Email
                            )
                        }
                    }
                    item(key = "add_email") {
                        RivoListItem(
                            headline = stringResource(R.string.contact_edit_add_email),
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            headlineColor = MaterialTheme.colorScheme.primary,
                            isCompact = true,
                            onClick = { emails.add(EmailEntry("")) }
                        )
                    }
                }
            }

            // Addresses Group (Segmented)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contact_edit_address_header),
                    icon = Icons.Outlined.LocationOn
                ) {
                    addresses.forEachIndexed { index, address ->
                        item(key = "address_$index") {
                            RivoSegmentedRemovableField(
                                value = address,
                                onValueChange = { addresses[index] = it },
                                label = stringResource(R.string.label_address),
                                icon = Icons.Outlined.LocationOn,
                                onDelete = if (addresses.size > 1 || address.isNotBlank()) {
                                    {
                                        if (addresses.size > 1) {
                                            addresses.removeAt(index)
                                        } else {
                                            addresses[0] = ""
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    item(key = "add_address") {
                        RivoListItem(
                            headline = stringResource(R.string.contact_edit_add_address),
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            headlineColor = MaterialTheme.colorScheme.primary,
                            isCompact = true,
                            onClick = { addresses.add("") }
                        )
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
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
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
    onDelete: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var showTypeMenu by remember { mutableStateOf(false) }

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
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
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
