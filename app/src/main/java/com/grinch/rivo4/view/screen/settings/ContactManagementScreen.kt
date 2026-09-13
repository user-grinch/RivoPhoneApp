package com.grinch.rivo4.view.screen.settings

import android.accounts.Account
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactVisibilityScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateContactsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

sealed class StorageTarget(val id: String, val displayName: String) {
    object LocalMemory : StorageTarget("local", "Local Memory (Device)")
    object PrivateStorage : StorageTarget("private", "Private Storage (App Vault)")
    data class SimCard(val account: Account) : StorageTarget("sim_${account.name}", "SIM Card (${account.name})")
    data class CloudAccount(val account: Account) : StorageTarget("account_${account.name}_${account.type}", account.name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactManagementScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = koinInject<PreferenceManager>()

    val allContacts by contactsVM.allContacts.collectAsState()
    val duplicateGroups by contactsVM.duplicateGroups.collectAsState()
    val availableAccounts by contactsVM.availableAccounts.collectAsState()
    val isLoading by contactsVM.isLoading.collectAsState()
    val isMerging by contactsVM.isMerging.collectAsState()
    val standardizeProgress by contactsVM.standardizeProgress.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showMergeAllDialog by remember { mutableStateOf(false) }
    var showStandardizeConfirm by remember { mutableStateOf(false) }

    // Storage Mover State
    val storageTargets = remember(availableAccounts) {
        val list = mutableListOf<StorageTarget>(
            StorageTarget.LocalMemory,
            StorageTarget.PrivateStorage
        )
        availableAccounts.forEach { acc ->
            if (acc.type.contains("sim", ignoreCase = true)) {
                list.add(StorageTarget.SimCard(acc))
            } else {
                list.add(StorageTarget.CloudAccount(acc))
            }
        }
        list
    }

    var selectedSourceStorage by remember { mutableStateOf<StorageTarget>(StorageTarget.LocalMemory) }
    var selectedDestStorage by remember {
        mutableStateOf<StorageTarget>(
            storageTargets.firstOrNull { it is StorageTarget.CloudAccount } ?: StorageTarget.PrivateStorage
        )
    }
    var showMoveSelectionDialog by remember { mutableStateOf(false) }
    var showMoveAllConfirmDialog by remember { mutableStateOf(false) }

    // Filter contacts belonging to selected source storage
    val sourceContacts = remember(allContacts, selectedSourceStorage) {
        when (val src = selectedSourceStorage) {
            is StorageTarget.LocalMemory -> allContacts.filter { !it.isPrivate && it.accountName == null && it.accountType == null }
            is StorageTarget.PrivateStorage -> allContacts.filter { it.isPrivate }
            is StorageTarget.SimCard -> allContacts.filter { !it.isPrivate && it.accountName == src.account.name && it.accountType == src.account.type }
            is StorageTarget.CloudAccount -> allContacts.filter { !it.isPrivate && it.accountName == src.account.name && it.accountType == src.account.type }
        }
    }

    // Counts by storage
    val localCount = remember(allContacts) { allContacts.count { !it.isPrivate && it.accountName == null && it.accountType == null } }
    val privateCount = remember(allContacts) { allContacts.count { it.isPrivate } }
    val simCount = remember(allContacts) { allContacts.count { !it.isPrivate && it.accountType?.contains("sim", ignoreCase = true) == true } }
    val cloudCount = remember(allContacts) { allContacts.count { !it.isPrivate && it.accountType != null && it.accountType?.contains("sim", ignoreCase = true) != true } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_management_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { contactsVM.fetchContacts() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Storage Breakdown Card
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.contact_management_storage_overview),
                    icon = Icons.Outlined.PieChart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${allContacts.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StorageCountChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.PhoneAndroid,
                            label = "Local",
                            count = localCount,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                        StorageCountChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Cloud,
                            label = "Cloud",
                            count = cloudCount,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                        StorageCountChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.SimCard,
                            label = "SIM",
                            count = simCount,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                        StorageCountChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Lock,
                            label = "Private",
                            count = privateCount,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }

            // 2. Duplicate Contacts & Merge Engine
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.contact_management_duplicates_title),
                    icon = Icons.Outlined.CallMerge
                ) {
                    if (duplicateGroups.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_management_no_duplicates),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "All contact names, phone numbers, and emails are unique.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val totalDups = duplicateGroups.sumOf { it.size }
                        val totalSets = duplicateGroups.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.contact_management_duplicates_found, totalDups, totalSets),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Combine redundant details and keep clean contacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showMergeAllDialog = true },
                                enabled = !isMerging,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isMerging) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.contact_management_merge_all))
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        duplicateGroups.forEachIndexed { index, group ->
                            DuplicateGroupItem(
                                group = group,
                                onMerge = {
                                    val primary = group.first()
                                    val sources = group.drop(1).map { it.id }
                                    contactsVM.mergeDuplicateGroup(primary.id, sources)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Merged contacts for ${primary.name}")
                                    }
                                },
                                onDismiss = {
                                    contactsVM.dismissDuplicateGroup(group)
                                }
                            )
                            if (index < duplicateGroups.lastIndex) {
                                RivoDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // 3. Move / Transfer Contacts Between Storage Locations
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.contact_management_move_title),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove
                ) {
                    Text(
                        text = stringResource(R.string.contact_management_move_supporting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    // Source & Destination Selector Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source
                        StorageDropdownSelector(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.contact_management_move_from),
                            selected = selectedSourceStorage,
                            options = storageTargets,
                            onSelect = { selectedSourceStorage = it }
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        // Destination
                        StorageDropdownSelector(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.contact_management_move_to),
                            selected = selectedDestStorage,
                            options = storageTargets,
                            onSelect = { selectedDestStorage = it }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedSourceStorage == selectedDestStorage) {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.contact_management_same_source_dest)) }
                                } else if (sourceContacts.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("No contacts found in ${selectedSourceStorage.displayName}") }
                                } else {
                                    showMoveSelectionDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.contact_management_select_to_move))
                        }

                        Button(
                            onClick = {
                                if (selectedSourceStorage == selectedDestStorage) {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.contact_management_same_source_dest)) }
                                } else if (sourceContacts.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("No contacts found in ${selectedSourceStorage.displayName}") }
                                } else {
                                    showMoveAllConfirmDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            enabled = sourceContacts.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.contact_management_move_all, sourceContacts.size))
                        }
                    }
                }
            }

            // 4. Address Book Tools
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.contact_management_tools_title),
                    icon = Icons.Outlined.Build
                ) {
                    RivoListItem(
                        headline = stringResource(R.string.contact_management_standardize_numbers),
                        supporting = stringResource(R.string.contact_management_standardize_supporting),
                        leadingIcon = Icons.Outlined.FormatColorText,
                        onClick = { showStandardizeConfirm = true }
                    )
                    if (standardizeProgress != null) {
                        LinearProgressIndicator(
                            progress = { standardizeProgress ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    RivoListItem(
                        headline = stringResource(R.string.settings_manage_visibility),
                        supporting = stringResource(R.string.settings_manage_visibility_supporting),
                        leadingIcon = Icons.Outlined.Visibility,
                        onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
                    )

                    RivoListItem(
                        headline = "Private Storage Vault",
                        supporting = "Manage secret local contacts stored strictly in app database",
                        leadingIcon = Icons.Outlined.Lock,
                        onClick = { navigator.navigate(PrivateContactsScreenDestination) }
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // Dialog: Merge All Confirmation
    if (showMergeAllDialog) {
        RivoDialog(
            onDismissRequest = { showMergeAllDialog = false },
            title = stringResource(R.string.contact_management_merge_all_confirm_title),
            icon = Icons.Outlined.CallMerge,
            confirmButton = {
                Button(
                    onClick = {
                        showMergeAllDialog = false
                        contactsVM.mergeAllDuplicates()
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.contact_management_merge_success, duplicateGroups.size))
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.contact_management_merge_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.contact_management_merge_all_confirm_msg, duplicateGroups.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Dialog: Move All Confirmation
    if (showMoveAllConfirmDialog) {
        RivoDialog(
            onDismissRequest = { showMoveAllConfirmDialog = false },
            title = stringResource(R.string.contact_management_move_confirm_title),
            icon = Icons.AutoMirrored.Filled.DriveFileMove,
            confirmButton = {
                Button(
                    onClick = {
                        showMoveAllConfirmDialog = false
                        val targetAccount = when (val dest = selectedDestStorage) {
                            is StorageTarget.LocalMemory -> Pair(null, null)
                            is StorageTarget.PrivateStorage -> Pair("private", "com.grinch.rivo4.private")
                            is StorageTarget.SimCard -> Pair(dest.account.name, dest.account.type)
                            is StorageTarget.CloudAccount -> Pair(dest.account.name, dest.account.type)
                        }
                        contactsVM.moveContactsToStorage(
                            contactIds = sourceContacts.map { it.id },
                            accountName = targetAccount.first,
                            accountType = targetAccount.second
                        ) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        R.string.contact_management_move_success,
                                        sourceContacts.size,
                                        selectedDestStorage.displayName
                                    )
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Move All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveAllConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Text(
                text = stringResource(
                    R.string.contact_management_move_confirm_msg,
                    sourceContacts.size,
                    selectedSourceStorage.displayName,
                    selectedDestStorage.displayName
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Dialog: Standardize Phone Numbers Confirm
    if (showStandardizeConfirm) {
        RivoDialog(
            onDismissRequest = { showStandardizeConfirm = false },
            title = stringResource(R.string.contact_management_standardize_numbers),
            icon = Icons.Outlined.FormatColorText,
            confirmButton = {
                Button(
                    onClick = {
                        showStandardizeConfirm = false
                        contactsVM.formatAllPhoneNumbers()
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_manage_standardize_completed))
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStandardizeConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.settings_manage_standardize_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Sheet / Dialog: Select specific contacts to move
    if (showMoveSelectionDialog) {
        MoveContactsSelectionDialog(
            contacts = sourceContacts,
            sourceStorageName = selectedSourceStorage.displayName,
            destStorageName = selectedDestStorage.displayName,
            onDismiss = { showMoveSelectionDialog = false },
            onConfirmMove = { selectedIds ->
                showMoveSelectionDialog = false
                val targetAccount = when (val dest = selectedDestStorage) {
                    is StorageTarget.LocalMemory -> Pair(null, null)
                    is StorageTarget.PrivateStorage -> Pair("private", "com.grinch.rivo4.private")
                    is StorageTarget.SimCard -> Pair(dest.account.name, dest.account.type)
                    is StorageTarget.CloudAccount -> Pair(dest.account.name, dest.account.type)
                }
                contactsVM.moveContactsToStorage(
                    contactIds = selectedIds,
                    accountName = targetAccount.first,
                    accountType = targetAccount.second
                ) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.contact_management_move_success,
                                selectedIds.size,
                                selectedDestStorage.displayName
                            )
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun StorageCountChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DuplicateGroupItem(
    group: List<Contact>,
    onMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = group.firstOrNull() ?: return
    val others = group.drop(1)
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivoAvatar(
                    name = primary.name,
                    photoUri = primary.photoUri,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${group.size} matching contacts • ${primary.phoneNumbers.firstOrNull() ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                    }
                    FilledTonalButton(
                        onClick = onMerge,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(stringResource(R.string.contact_management_merge_group_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Contacts to be combined:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    group.forEachIndexed { idx, c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (idx == 0) "Primary" else "Duplicate #$idx",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )
                            Column {
                                Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    text = c.phoneNumbers.joinToString(", ").ifEmpty { "No numbers" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDropdownSelector(
    label: String,
    selected: StorageTarget,
    options: List<StorageTarget>,
    onSelect: (StorageTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { target ->
                DropdownMenuItem(
                    text = { Text(target.displayName, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onSelect(target)
                        expanded = false
                    },
                    leadingIcon = {
                        val icon = when (target) {
                            is StorageTarget.LocalMemory -> Icons.Outlined.PhoneAndroid
                            is StorageTarget.PrivateStorage -> Icons.Outlined.Lock
                            is StorageTarget.SimCard -> Icons.Outlined.SimCard
                            is StorageTarget.CloudAccount -> Icons.Outlined.Cloud
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }
    }
}

@Composable
fun MoveContactsSelectionDialog(
    contacts: List<Contact>,
    sourceStorageName: String,
    destStorageName: String,
    onDismiss: () -> Unit,
    onConfirmMove: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(contacts.map { it.id }.toSet()) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumbers.any { num -> num.contains(searchQuery) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Contacts to Move",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From $sourceStorageName to $destStorageName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search & Select All
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} of ${contacts.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == contacts.size) emptySet() else contacts.map { it.id }.toSet()
                    }) {
                        Text(if (selectedIds.size == contacts.size) "Deselect All" else "Select All")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Contacts list with checkboxes
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filtered, key = { it.id }) { contact ->
                        val isChecked = selectedIds.contains(contact.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - contact.id else selectedIds + contact.id
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + contact.id else selectedIds - contact.id
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            RivoAvatar(name = contact.name, photoUri = contact.photoUri, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    contact.phoneNumbers.firstOrNull()?.let { formatPhoneNumber(it) } ?: "No number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmMove(selectedIds.toList()) },
                        enabled = selectedIds.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Move (${selectedIds.size})")
                    }
                }
            }
        }
    }
}
