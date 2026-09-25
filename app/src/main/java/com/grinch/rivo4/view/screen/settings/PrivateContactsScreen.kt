package com.grinch.rivo4.view.screen.settings

import android.accounts.Account
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PrivateContactsScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<ContactSelectionScreenDestination, String>
) {
    val context = LocalContext.current
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val viewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by viewModel.allContacts.collectAsState()
    val privateContacts = remember(allContacts) { allContacts.filter { it.isPrivate } }
    val availableAccounts by viewModel.availableAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedContactIds by remember { mutableStateOf(setOf<String>()) }
    var showMoveAccountDialog by remember { mutableStateOf(false) }
    var targetContactsToMove by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var secretCodeInput by remember {
        mutableStateOf(
            prefs.getString(
                com.grinch.rivo4.controller.util.PreferenceManager.KEY_SECRET_DIALPAD_CODE,
                com.grinch.rivo4.controller.util.PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
            ) ?: com.grinch.rivo4.controller.util.PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
        )
    }
    var hideFromSettings by remember {
        mutableStateOf(
            prefs.getBoolean(
                com.grinch.rivo4.controller.util.PreferenceManager.KEY_HIDE_PRIVATE_SETTINGS_ENTRY,
                false
            )
        )
    }

    val isSelecting = selectedContactIds.isNotEmpty()

    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Value -> {
                val csvValue = result.value
                csvValue.split(",").forEach { item ->
                    val trimmed = item.trim()
                    if (trimmed.isNotBlank()) {
                        val targetContact = allContacts.find { c ->
                            c.id == trimmed || c.phoneNumbers.any { num -> num == trimmed }
                        }
                        if (targetContact != null) {
                            viewModel.makeContactPrivate(targetContact.id)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    val filteredPrivateContacts = remember(privateContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            privateContacts
        } else {
            val q = searchQuery.trim()
            privateContacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.phoneNumbers.any { num -> num.contains(q) }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard"),
        onResult = { uri ->
            uri?.let { viewModel.exportPrivateContacts(it) }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.importPrivateContacts(it) }
        }
    )

    Scaffold(
        topBar = {
            if (isSelecting) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedContactIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedContactIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = {
                                val targets = privateContacts.filter { selectedContactIds.contains(it.id) }
                                targetContactsToMove = targets
                                showMoveAccountDialog = true
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.DriveFileMove,
                                contentDescription = stringResource(R.string.contact_move_to_public_storage)
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedContactIds.forEach { viewModel.makeContactPublic(it) }
                                selectedContactIds = emptySet()
                            }
                        ) {
                            Icon(
                                Icons.Outlined.LockOpen,
                                contentDescription = stringResource(R.string.contact_move_to_public_storage)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_private_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        if (filteredPrivateContacts.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    selectedContactIds = filteredPrivateContacts.map { it.id }.toSet()
                                }
                            ) {
                                Icon(Icons.Outlined.SelectAll, contentDescription = "Select All")
                            }
                        }
                        IconButton(onClick = { showSecurityDialog = true }) {
                            Icon(Icons.Outlined.Password, contentDescription = "Secret Dialpad Code")
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (isLoading) {
            RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Vault Overview Hero Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    modifier = Modifier.size(50.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.settings_private_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "${privateContacts.size} secured",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Stored locally in Rivo's sandboxed vault • Hidden from WhatsApp and system apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Action buttons: Primary full-width "Add Contacts to Vault" so text never breaks into two lines
                            Button(
                                onClick = {
                                    navigator.navigate(
                                        ContactSelectionScreenDestination(
                                            title = "Select Contacts for Private Storage",
                                            isMultiSelect = true,
                                            actionButtonText = "Move to Private Storage",
                                            returnContactId = true
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add Contacts to Vault", style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(Modifier.height(8.dp))

                            // Secondary Row: Import & Export VCF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { importLauncher.launch("text/vcard") },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import VCF", style = MaterialTheme.typography.labelMedium)
                                }

                                FilledTonalButton(
                                    onClick = { exportLauncher.launch("private_contacts.vcf") },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export VCF", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // 2. Vault Security Options
                item {
                    val currentSecretCode = prefs.getString(
                        com.grinch.rivo4.controller.util.PreferenceManager.KEY_SECRET_DIALPAD_CODE,
                        com.grinch.rivo4.controller.util.PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
                    ) ?: com.grinch.rivo4.controller.util.PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
                    RivoExpressiveGroup(
                        title = "Vault Access & Security",
                        icon = Icons.Outlined.Key
                    ) {
                        item {
                            RivoListItem(
                                headline = "Secret Dialpad Code",
                                supporting = "Dial $currentSecretCode on the dialpad to quickly open private vault",
                                leadingIcon = Icons.Outlined.Password,
                                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = { showSecurityDialog = true }
                            )
                        }
                        item {
                            RivoSwitchListItem(
                                headline = "Hide from Settings",
                                supporting = "Remove Private Storage from the settings menu (accessible only via secret code)",
                                leadingIcon = Icons.Outlined.VisibilityOff,
                                checked = hideFromSettings,
                                onCheckedChange = { checked ->
                                    hideFromSettings = checked
                                    prefs.setBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_HIDE_PRIVATE_SETTINGS_ENTRY, checked)
                                }
                            )
                        }
                    }
                }

                // 3. Search Bar (if contacts exist)
                if (privateContacts.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            placeholder = { Text(stringResource(R.string.search_contacts_placeholder)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                // 4. Contact List Section Header & Items
                if (filteredPrivateContacts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) stringResource(R.string.search_no_results_title) else stringResource(R.string.settings_private_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "No private contacts match \"$searchQuery\""
                                    } else {
                                        stringResource(R.string.settings_manage_private_contacts_supporting)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isBlank()) {
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            navigator.navigate(
                                                ContactSelectionScreenDestination(
                                                    title = "Select Contacts for Private Storage",
                                                    isMultiSelect = true,
                                                    actionButtonText = "Move to Private Storage",
                                                    returnContactId = true
                                                )
                                            )
                                        },
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pick Contacts for Vault")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        RivoSectionHeader(
                            title = "Secured Contacts (${filteredPrivateContacts.size})",
                            icon = Icons.Outlined.Lock,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(
                        items = filteredPrivateContacts,
                        key = { _, c -> "private_${c.id}" }
                    ) { index, contact ->
                        val isSelected = selectedContactIds.contains(contact.id)
                        val shape = rivoGroupedItemShape(index, filteredPrivateContacts.size)

                        PrivateContactListItem(
                            contact = contact,
                            shape = shape,
                            isSelected = isSelected,
                            isSelecting = isSelecting,
                            onSelect = {
                                selectedContactIds = if (isSelected) {
                                    selectedContactIds - contact.id
                                } else {
                                    selectedContactIds + contact.id
                                }
                            },
                            onClick = {
                                if (isSelecting) {
                                    selectedContactIds = if (isSelected) {
                                        selectedContactIds - contact.id
                                    } else {
                                        selectedContactIds + contact.id
                                    }
                                } else {
                                    navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                }
                            },
                            onCall = {
                                contact.phoneNumbers.firstOrNull()?.let { num -> makeCall(context, num) }
                            },
                            onToggleHidden = {
                                viewModel.setContactHidden(contact.id, !contact.isHidden)
                            },
                            onMoveToPublic = { viewModel.makeContactPublic(contact.id) },
                            onMoveToAccount = {
                                targetContactsToMove = listOf(contact)
                                showMoveAccountDialog = true
                            },
                            onEdit = { navigator.navigate(ContactEditScreenDestination(contactId = contact.id)) },
                            onDelete = { viewModel.deleteContact(contact.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDeleteConfirmDialog && selectedContactIds.isNotEmpty()) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteContacts(selectedContactIds.toList())
                selectedContactIds = emptySet()
                showDeleteConfirmDialog = false
            },
            title = stringResource(R.string.action_delete),
            message = "Permanently delete ${selectedContactIds.size} selected contact(s) from private storage?",
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Outlined.Delete
        )
    }

    if (showMoveAccountDialog && targetContactsToMove.isNotEmpty()) {
        val publicLabel = stringResource(R.string.contact_move_to_public_storage)
        val publicDesc = "Move to public device contacts database"
        val storageOptions = remember(availableAccounts, publicLabel) {
            listOf("public" to Pair(publicLabel, publicDesc)) +
            availableAccounts.map { acc -> acc.name to Pair(acc.name, acc.type) }
        }
        RivoSelectionDialog(
            onDismissRequest = {
                showMoveAccountDialog = false
                targetContactsToMove = emptyList()
            },
            title = publicLabel,
            icon = Icons.AutoMirrored.Outlined.DriveFileMove,
            items = storageOptions,
            itemLabel = { option -> option.second.first },
            itemSupporting = { option -> option.second.second },
            itemIcon = { option -> if (option.first == "public") Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud },
            onItemSelected = { selected ->
                val ids = targetContactsToMove.map { contact -> contact.id }
                if (selected.first == "public") {
                    ids.forEach { id -> viewModel.makeContactPublic(id) }
                } else {
                    val targetAcc = availableAccounts.find { acc -> acc.name == selected.first }
                    if (targetAcc != null) {
                        viewModel.moveContacts(ids, targetAcc)
                    }
                }
                if (isSelecting) selectedContactIds = emptySet()
                showMoveAccountDialog = false
                targetContactsToMove = emptyList()
            }
        )
    }

    if (showSecurityDialog) {
        RivoDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = "Secret Dialpad Code",
            icon = Icons.Outlined.Password,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.action_save),
                onClick = {
                    val trimmed = secretCodeInput.trim()
                    if (trimmed.isNotEmpty()) {
                        prefs.setString(com.grinch.rivo4.controller.util.PreferenceManager.KEY_SECRET_DIALPAD_CODE, trimmed)
                    }
                    prefs.setBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_HIDE_PRIVATE_SETTINGS_ENTRY, hideFromSettings)
                    showSecurityDialog = false
                    android.widget.Toast.makeText(context, "Secret code settings updated", android.widget.Toast.LENGTH_SHORT).show()
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_cancel),
                onClick = { showSecurityDialog = false }
            )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Type this secret code on the dialpad to reveal contacts in private storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = secretCodeInput,
                    onValueChange = { secretCodeInput = it },
                    label = { Text("Secret Code") },
                    placeholder = { Text("*#0000#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide from Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only the secret dialpad code will access private storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hideFromSettings,
                        onCheckedChange = { hideFromSettings = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivateContactListItem(
    contact: Contact,
    shape: androidx.compose.ui.graphics.Shape,
    isSelected: Boolean,
    isSelecting: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onToggleHidden: () -> Unit,
    onMoveToPublic: () -> Unit,
    onMoveToAccount: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelecting) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            RivoAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
                modifier = Modifier.size(46.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    val badgeBg = if (contact.isHidden) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    }
                    val badgeContent = if (contact.isHidden) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        shape = CircleShape,
                        color = badgeBg,
                        contentColor = badgeContent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (contact.isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (contact.isHidden) "Hidden" else "Private",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                if (contact.phoneNumbers.isNotEmpty()) {
                    Text(
                        text = formatPhoneNumber(contact.phoneNumbers.first()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (contact.phoneNumbers.isNotEmpty() && !isSelecting) {
                IconButton(onClick = onCall) {
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = stringResource(R.string.content_desc_call_named, contact.name),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                }
                RivoDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    RivoDropdownMenuItem(
                        text = { Text(if (contact.isHidden) "Unhide in Vault" else "Hide Completely") },
                        onClick = {
                            showMenu = false
                            onToggleHidden()
                        },
                        leadingIcon = {
                            Icon(
                                if (contact.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    )
                    RivoDropdownMenuItem(
                        text = { Text(stringResource(R.string.contact_move_to_public_storage)) },
                        onClick = {
                            showMenu = false
                            onMoveToPublic()
                        },
                        leadingIcon = { Icon(Icons.Outlined.LockOpen, contentDescription = null) }
                    )
                    RivoDropdownMenuItem(
                        text = { Text("Move to Account") },
                        onClick = {
                            showMenu = false
                            onMoveToAccount()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) }
                    )
                    RivoDropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    RivoDropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
