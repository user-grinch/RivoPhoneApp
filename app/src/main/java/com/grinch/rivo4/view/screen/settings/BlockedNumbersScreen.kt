package com.grinch.rivo4.view.screen.settings

import android.content.Context
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.BlockedNumber
import com.grinch.rivo4.controller.util.BlockedNumbersManager
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BlockedNumbersScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<ContactSelectionScreenDestination, String>
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsState by prefs.settingsChanged.collectAsState()

    var blockMethod by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0)) }
    var logVisibility by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0)) }
    var blockNotification by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) }
    var autoDeclineUnknown by remember(settingsState) { mutableStateOf(prefs.isAutoDeclineUnknownEnabled()) }
    var autoDeclineNonContacts by remember(settingsState) { mutableStateOf(prefs.isAutoDeclineNonContactsEnabled()) }

    var blockedNumbers by remember { mutableStateOf<List<BlockedNumber>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var numberToUnblock by remember { mutableStateOf<BlockedNumber?>(null) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var manualNumberInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProcessingFile by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        val result = BlockedNumbersManager.exportToCsv(context, os, allContacts)
                        result.onSuccess { count ->
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_export_success, count))
                            }
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_file_error))
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_file_error))
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessingFile = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val result = BlockedNumbersManager.importFromCsv(context, inputStream)
                        result.onSuccess { res ->
                            withContext(Dispatchers.Main) {
                                isProcessingFile = false
                                refreshKey++
                                if (res.totalParsed == 0) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_csv_empty))
                                } else {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.settings_blocked_import_success,
                                            res.newlyBlocked,
                                            res.alreadyBlocked
                                        )
                                    )
                                }
                            }
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                isProcessingFile = false
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_file_error))
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessingFile = false
                        snackbarHostState.showSnackbar(context.getString(R.string.settings_blocked_file_error))
                    }
                }
            }
        }
    }

    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Value -> {
                val csvValue = result.value
                csvValue.split(",").forEach { num ->
                    val trimmed = num.trim()
                    if (trimmed.isNotBlank()) {
                        BlockedNumbersManager.block(context, trimmed)
                    }
                }
                refreshKey++
            }
            else -> {}
        }
    }

    LaunchedEffect(refreshKey) {
        blockedNumbers = BlockedNumbersManager.getAll(context)
        contactsVM.fetchContacts()
    }

    val filteredBlockedNumbers = remember(blockedNumbers, searchQuery) {
        if (searchQuery.isBlank()) {
            blockedNumbers
        } else {
            val q = searchQuery.trim()
            blockedNumbers.filter {
                it.originalNumber.contains(q) ||
                formatPhoneNumber(it.originalNumber).contains(q, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_blocked_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddManualDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.blocked_add_number))
                    }
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                        }
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = stringResource(R.string.settings_blocked_import_csv))
                    }
                    IconButton(
                        onClick = {
                            exportLauncher.launch("rivo_blocked_numbers_${System.currentTimeMillis()}.csv")
                        },
                        enabled = blockedNumbers.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.settings_blocked_export_csv))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Protection Hero Card
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
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                contentColor = MaterialTheme.colorScheme.error
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.settings_blocked_numbers_headline),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Text(
                                            text = "${blockedNumbers.size} blocked",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = stringResource(R.string.settings_blocked_numbers_supporting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Primary Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    navigator.navigate(
                                        ContactSelectionScreenDestination(
                                            title = "Block Numbers or Contacts",
                                            isMultiSelect = true,
                                            actionButtonText = "Block"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Outlined.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Block Contact", style = MaterialTheme.typography.labelLarge)
                            }

                            FilledTonalButton(
                                onClick = {
                                    manualNumberInput = ""
                                    showAddManualDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Number", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // CSV Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import CSV", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    exportLauncher.launch("rivo_blocked_numbers_${System.currentTimeMillis()}.csv")
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                enabled = blockedNumbers.isNotEmpty(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export CSV", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 2. Search Field (when blocked numbers exist)
            if (blockedNumbers.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        placeholder = { Text("Search blocked numbers...") },
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

            // 3. Blocked Numbers List Section
            if (filteredBlockedNumbers.isEmpty()) {
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
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isBlank()) stringResource(R.string.blocked_list_empty) else "No matching blocked numbers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Your blocklist is empty. Tap above to block contacts or enter unknown spam numbers."
                                } else {
                                    "No blocked numbers match \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    RivoSectionHeader(
                        title = "${stringResource(R.string.blocked_list_title)} (${filteredBlockedNumbers.size})",
                        icon = Icons.Outlined.Block,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(
                    items = filteredBlockedNumbers,
                    key = { _, b -> "blocked_${b.id}_${b.originalNumber}" }
                ) { index, entry ->
                    val matchedContact = remember(allContacts, entry.originalNumber) {
                        allContacts.find { c ->
                            c.phoneNumbers.any { num -> com.grinch.rivo4.controller.util.areNumbersEqual(num, entry.originalNumber) }
                        }
                    }
                    val shape = rivoGroupedItemShape(index, filteredBlockedNumbers.size)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (matchedContact != null) {
                                RivoAvatar(
                                    name = matchedContact.name,
                                    photoUri = matchedContact.photoUri,
                                    modifier = Modifier.size(44.dp)
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Block,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = matchedContact?.name ?: formatPhoneNumber(entry.originalNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (matchedContact != null) formatPhoneNumber(entry.originalNumber) else stringResource(R.string.blocked_list_item_supporting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            FilledTonalButton(
                                onClick = { numberToUnblock = entry },
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.action_unblock),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Call Screening Section
            item {
                RivoExpressiveGroup(
                    title = "Call Screening",
                    icon = Icons.Outlined.PhoneDisabled
                ) {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_auto_decline_unknown_title),
                            supporting = stringResource(R.string.settings_auto_decline_unknown_supporting),
                            leadingIcon = Icons.Outlined.PhoneDisabled,
                            checked = autoDeclineUnknown,
                            onCheckedChange = {
                                autoDeclineUnknown = it
                                prefs.setAutoDeclineUnknownEnabled(it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_auto_decline_non_contacts_title),
                            supporting = stringResource(R.string.settings_auto_decline_non_contacts_supporting),
                            leadingIcon = Icons.Outlined.PersonOff,
                            checked = autoDeclineNonContacts,
                            onCheckedChange = {
                                autoDeclineNonContacts = it
                                prefs.setAutoDeclineNonContactsEnabled(it)
                            }
                        )
                    }
                }
            }

            // 5. Blocking Behavior Section
            item {
                RivoExpressiveGroup(
                    title = "Blocking Behavior",
                    icon = Icons.Outlined.Gavel
                ) {
                    item {
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_blocked_method),
                            supporting = stringResource(R.string.settings_blocked_method_supporting),
                            leadingIcon = Icons.Outlined.Gavel,
                            options = listOf(
                                stringResource(R.string.settings_blocked_method_decline) to 0,
                                stringResource(R.string.settings_blocked_method_silent) to 1
                            ),
                            selectedValue = blockMethod,
                            onValueChange = {
                                blockMethod = it
                                prefs.setInt(PreferenceManager.KEY_BLOCK_METHOD, it)
                            }
                        )
                    }
                    item {
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_blocked_log_visibility),
                            supporting = stringResource(R.string.settings_blocked_log_visibility_supporting),
                            leadingIcon = Icons.Outlined.Visibility,
                            options = listOf(
                                stringResource(R.string.settings_blocked_log_hide) to 0,
                                stringResource(R.string.settings_blocked_log_show) to 1
                            ),
                            selectedValue = logVisibility,
                            onValueChange = {
                                logVisibility = it
                                prefs.setInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, it)
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_blocked_notifications),
                            supporting = stringResource(R.string.settings_blocked_notifications_supporting),
                            leadingIcon = Icons.Outlined.NotificationsPaused,
                            checked = blockNotification,
                            onCheckedChange = {
                                blockNotification = it
                                prefs.setBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, it)
                            }
                        )
                    }
                }
            }

            // 6. System Integration Section
            item {
                RivoExpressiveGroup(
                    title = "System Integration",
                    icon = Icons.Outlined.SettingsSuggest
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_blocked_system_button),
                            supporting = "Open the Android system blocked numbers manager",
                            leadingIcon = Icons.AutoMirrored.Outlined.List,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                                try {
                                    val intent = telecomManager.createManageBlockedNumbersIntent()
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Manual Add Phone Number Dialog
    if (showAddManualDialog) {
        RivoDialog(
            onDismissRequest = { showAddManualDialog = false },
            title = stringResource(R.string.blocked_add_number),
            icon = Icons.Outlined.Block,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.action_block),
                onClick = {
                    val trimmed = manualNumberInput.trim()
                    if (trimmed.isNotEmpty()) {
                        BlockedNumbersManager.block(context, trimmed)
                        refreshKey++
                        showAddManualDialog = false
                        manualNumberInput = ""
                    }
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_cancel),
                onClick = {
                    showAddManualDialog = false
                    manualNumberInput = ""
                }
            )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Calls and text messages from this number will be blocked immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = manualNumberInput,
                    onValueChange = { manualNumberInput = it },
                    label = { Text(stringResource(R.string.blocked_add_number_hint)) },
                    placeholder = { Text("+1 (555) 000-0000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }

    // Unblock Confirmation Dialog
    if (numberToUnblock != null) {
        val target = numberToUnblock!!
        val matchedName = remember(allContacts, target.originalNumber) {
            allContacts.find { c ->
                c.phoneNumbers.any { num -> com.grinch.rivo4.controller.util.areNumbersEqual(num, target.originalNumber) }
            }?.name
        }
        val targetDisplayName = matchedName ?: formatPhoneNumber(target.originalNumber)

        RivoConfirmationDialog(
            onDismissRequest = { numberToUnblock = null },
            onConfirm = {
                BlockedNumbersManager.unblockById(context, target.id)
                numberToUnblock = null
                refreshKey++
            },
            title = stringResource(R.string.action_unblock),
            message = "Unblock $targetDisplayName? They will be able to call and message you again.",
            confirmLabel = stringResource(R.string.action_unblock),
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Outlined.LockOpen
        )
    }

    // Progress Dialog during CSV Import/Export
    if (isProcessingFile) {
        RivoDialog(
            onDismissRequest = { },
            title = "Processing Blocklist",
            icon = Icons.Outlined.HourglassEmpty
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Importing blocked numbers...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
