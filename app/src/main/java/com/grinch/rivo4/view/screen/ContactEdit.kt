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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.deduplicateNumbers
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSectionHeader
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

    var name by remember { mutableStateOf(initialName ?: "") }
    var nickname by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isPrivate by remember { mutableStateOf(false) }

    val phoneNumbers = remember { mutableStateListOf<String>("") }
    val emails = remember { mutableStateListOf<String>("") }
    val addresses = remember { mutableStateListOf<String>("") }

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(contactId, availableAccounts) {
        if (contactId != null && contactId != "0" && contactId != "null") {
            val existing = contactsVM.getFullContactById(contactId)
            if (existing != null) {
                name = existing.name
                nickname = existing.nickname ?: ""
                notes = existing.notes ?: ""
                photoUri = existing.photoUri
                isPrivate = existing.isPrivate
                if (selectedAccount == null && !existing.isPrivate) {
                    selectedAccount = availableAccounts.find {
                        it.name == existing.accountName && it.type == existing.accountType
                    }
                }

                phoneNumbers.clear()
                if (existing.phoneNumbers.isNotEmpty()) {
                    phoneNumbers.addAll(deduplicateNumbers(existing.phoneNumbers))
                } else {
                    phoneNumbers.add("")
                }

                emails.clear()
                if (existing.emails.isNotEmpty()) {
                    emails.addAll(existing.emails)
                } else {
                    emails.add("")
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

            if (!initialPhone.isNullOrBlank() && phoneNumbers.all { it.isBlank() }) {
                phoneNumbers.clear()
                phoneNumbers.add(initialPhone)
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
                                    val contactToSave = Contact(
                                        id = if (contactId == "null" || contactId == "0" || contactId == null) "0" else contactId,
                                        name = name,
                                        nickname = nickname.ifBlank { null },
                                        phoneNumbers = phoneNumbers.filter { it.isNotBlank() },
                                        emails = emails.filter { it.isNotBlank() },
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
                            enabled = name.isNotBlank() && phoneNumbers.any { it.isNotBlank() },
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
                            name = name,
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
                                        selectedAccount != null -> ContactUtils.getFriendlyAccountName(selectedAccount!!)
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
                        RivoDialog(
                            onDismissRequest = { showPicker = false },
                            title = stringResource(R.string.contact_edit_select_account_title),
                            icon = Icons.Default.AccountBalance,
                            dismissButton = {
                                TextButton(onClick = { showPicker = false }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        ) {
                            Surface(
                                onClick = {
                                    selectedAccount = null
                                    isPrivate = true
                                    showPicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPrivate) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.contact_edit_private_storage)) },
                                    supportingContent = { Text(stringResource(R.string.contact_edit_private_storage_description)) },
                                    leadingContent = { Icon(Icons.Default.Lock, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }

                            Surface(
                                onClick = {
                                    selectedAccount = null
                                    isPrivate = false
                                    showPicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedAccount == null && !isPrivate) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.label_local_memory)) },
                                    leadingContent = { Icon(Icons.Default.CloudOff, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }

                            availableAccounts.forEach { account ->
                                val isSelected = selectedAccount == account && !isPrivate
                                Surface(
                                    onClick = {
                                        selectedAccount = account
                                        isPrivate = false
                                        showPicker = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = { Text(ContactUtils.getFriendlyAccountName(account)) },
                                        supportingContent = { Text(account.name) },
                                        leadingContent = { Icon(ContactUtils.getAccountIcon(account), null) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                RivoSectionHeader(title = stringResource(R.string.contact_edit_identity_header))
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.contact_edit_full_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null) },
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
                        phoneNumbers.forEachIndexed { index, phone ->
                            EditField(
                                value = phone,
                                onValueChange = { phoneNumbers[index] = it },
                                label = phoneFieldLabel,
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone,
                                onDelete = {
                                    if (phoneNumbers.size > 1) {
                                        phoneNumbers.removeAt(index)
                                    } else {
                                        phoneNumbers[0] = ""
                                    }
                                }
                            )
                        }
                        TextButton(
                            onClick = { phoneNumbers.add("") },
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
                            EditField(
                                value = email,
                                onValueChange = { emails[index] = it },
                                label = emailFieldLabel,
                                icon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email,
                                onDelete = {
                                    if (emails.size > 1) {
                                        emails.removeAt(index)
                                    } else {
                                        emails[0] = ""
                                    }
                                }
                            )
                        }
                        TextButton(
                            onClick = { emails.add("") },
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
