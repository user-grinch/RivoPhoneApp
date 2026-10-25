package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.BackupViewModel
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoLinearProgressIndicator
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: BackupViewModel = koinActivityViewModel()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    var encryptionEnabled by remember(settingsState) { mutableStateOf(prefs.isContactBackupEncryptionEnabled()) }
    var autoExportEnabled by remember(settingsState) { mutableStateOf(prefs.isContactAutoExportEnabled()) }
    var backupPassword by remember(settingsState) { mutableStateOf(prefs.getContactBackupPassword()) }
    var backupFolderUri by remember(settingsState) {
        mutableStateOf(prefs.getContactBackupDirectoryUri())
    }
    var retentionCount by remember(settingsState) {
        mutableIntStateOf(prefs.getContactBackupRetentionCount())
    }
    var retentionMenuExpanded by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordDraft by remember { mutableStateOf("") }
    var passwordDialogForImport by remember { mutableStateOf(false) }
    var passwordDialogEnablesEncryption by remember { mutableStateOf(false) }
    val status by viewModel.status.collectAsState()
    val contactImportProgress by viewModel.contactImportProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStandardizeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val exportContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { viewModel.exportContacts(context, it) }
    }

    val importContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importContacts(context, it) }
    }

    fun requestPassword(
        forEncryptedImport: Boolean = false,
        enablesEncryption: Boolean = false
    ) {
        passwordDraft = backupPassword
        passwordDialogForImport = forEncryptedImport
        passwordDialogEnablesEncryption = enablesEncryption
        showPasswordDialog = true
    }

    fun launchEncryptedImport() {
        if (backupPassword.isBlank()) {
            requestPassword(forEncryptedImport = true)
        } else {
            importContactsLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val selectedUri = it
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onSuccess {
                backupFolderUri = selectedUri.toString()
                prefs.setContactBackupDirectoryUri(selectedUri.toString())
            }
        }
    }

    val exportLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportCallLogs(context, it) }
    }

    val importLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCallLogs(context, it) }
    }

    var pendingSettingsRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportAppSettings(context, it) }
    }

    val importSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { pendingSettingsRestoreUri = it }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(SettingsHighlightManager.highlightEpoch) {
        if (SettingsHighlightManager.highlightEpoch == 0L) return@LaunchedEffect
        val target = SettingsHighlightManager.highlightedSettingTitle ?: return@LaunchedEffect
        val targetIndex = when {
            target.contains("Contact", ignoreCase = true) ||
            target.contains("VCF", ignoreCase = true) -> 0

            target.contains("Log", ignoreCase = true) ||
            target.contains("History", ignoreCase = true) -> 1

            target.contains("Setting", ignoreCase = true) ||
            target.contains("Preference", ignoreCase = true) ||
            target.contains("Configuration", ignoreCase = true) -> 2

            target.contains("Standardize", ignoreCase = true) ||
            target.contains("E.164", ignoreCase = true) -> 3

            else -> -1
        }
        if (targetIndex >= 0) {
            delay(150L)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_backup_restore_headline),
                navigator = navigator
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_contacts_header),
                    icon = Icons.Outlined.ContactPhone
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_encryption),
                            supporting = stringResource(R.string.settings_backup_encryption_supporting),
                            leadingIcon = Icons.Outlined.Lock,
                            onClick = {
                                if (!encryptionEnabled && backupPassword.isBlank()) {
                                    requestPassword(enablesEncryption = true)
                                } else {
                                    encryptionEnabled = !encryptionEnabled
                                    prefs.setContactBackupEncryptionEnabled(encryptionEnabled)
                                }
                            },
                            toggled = encryptionEnabled,
                            trailingContent = {
                                Switch(
                                    checked = encryptionEnabled,
                                    onCheckedChange = {
                                        if (it && backupPassword.isBlank()) {
                                            requestPassword(enablesEncryption = true)
                                        } else {
                                            encryptionEnabled = it
                                            prefs.setContactBackupEncryptionEnabled(it)
                                        }
                                    }
                                )
                            }
                        )
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = if (backupPassword.isBlank()) "" else "********",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.settings_backup_password)) },
                                supportingText = {
                                    Text(
                                        if (backupPassword.isBlank()) {
                                            stringResource(R.string.settings_backup_password_not_set)
                                        } else {
                                            stringResource(R.string.settings_backup_password_set)
                                        }
                                    )
                                },
                                singleLine = true,
                                enabled = encryptionEnabled,
                                readOnly = true
                            )
                            TextButton(onClick = { requestPassword() }) {
                                Text(
                                    stringResource(
                                        if (backupPassword.isBlank()) {
                                            R.string.settings_backup_set_password
                                        } else {
                                            R.string.settings_backup_change_password
                                        }
                                    )
                                )
                            }
                        }
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_auto_export),
                            supporting = if (backupFolderUri == null) {
                                stringResource(R.string.settings_backup_folder_not_set)
                            } else {
                                stringResource(R.string.settings_backup_auto_export_supporting)
                            },
                            leadingIcon = Icons.Outlined.AutoMode,
                            onClick = {
                                if (!autoExportEnabled && backupFolderUri == null) {
                                    backupFolderLauncher.launch(null)
                                } else {
                                    autoExportEnabled = !autoExportEnabled
                                    prefs.setContactAutoExportEnabled(autoExportEnabled)
                                }
                            },
                            toggled = autoExportEnabled,
                            trailingContent = {
                                Switch(
                                    checked = autoExportEnabled,
                                    onCheckedChange = {
                                        if (it && backupFolderUri == null) {
                                            backupFolderLauncher.launch(null)
                                        } else {
                                            autoExportEnabled = it
                                            prefs.setContactAutoExportEnabled(it)
                                        }
                                    }
                                )
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_folder),
                            supporting = if (backupFolderUri == null) {
                                stringResource(R.string.settings_backup_folder_not_set)
                            } else {
                                stringResource(R.string.settings_backup_folder_supporting)
                            },
                            leadingIcon = Icons.Outlined.Folder,
                            onClick = { backupFolderLauncher.launch(null) }
                        )
                    }
                    item {
                        Box {
                            RivoListItem(
                                headline = stringResource(R.string.settings_backup_retention),
                                supporting = stringResource(
                                    R.string.settings_backup_retention_supporting
                                ),
                                leadingIcon = Icons.Outlined.History,
                                onClick = { retentionMenuExpanded = true },
                                trailingContent = {
                                    TextButton(onClick = { retentionMenuExpanded = true }) {
                                        Text(stringResource(R.string.settings_backup_retention_count, retentionCount))
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = retentionMenuExpanded,
                                onDismissRequest = { retentionMenuExpanded = false }
                            ) {
                                (1..20).forEach { count ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.settings_backup_retention_count, count))
                                        },
                                        onClick = {
                                            retentionCount = count
                                            prefs.setContactBackupRetentionCount(count)
                                            retentionMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_contacts),
                            supporting = stringResource(R.string.settings_backup_export_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileUpload,
                            onClick = {
                                if (encryptionEnabled && backupPassword.isBlank()) {
                                    requestPassword()
                                } else {
                                    exportContactsLauncher.launch(
                                        if (encryptionEnabled) "contacts_backup.vcf.enc" else "contacts_backup.vcf"
                                    )
                                }
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_contacts),
                            supporting = stringResource(R.string.settings_backup_import_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileDownload,
                            onClick = { importContactsLauncher.launch(arrayOf("text/vcard", "text/x-vcard", "application/octet-stream", "*/*")) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_encrypted_contacts),
                            supporting = stringResource(R.string.settings_backup_import_encrypted_contacts_supporting),
                            leadingIcon = Icons.Outlined.LockOpen,
                            onClick = { launchEncryptedImport() }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_call_logs_header),
                    icon = Icons.Outlined.History
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_logs),
                            supporting = stringResource(R.string.settings_backup_export_logs_supporting),
                            leadingIcon = Icons.Outlined.History,
                            onClick = { exportLogsLauncher.launch("call_logs_backup.json") }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_logs),
                            supporting = stringResource(R.string.settings_backup_import_logs_supporting),
                            leadingIcon = Icons.Outlined.Restore,
                            onClick = { importLogsLauncher.launch(arrayOf("application/json")) }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_app_settings_header),
                    icon = Icons.Outlined.SettingsSuggest
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_settings),
                            supporting = stringResource(R.string.settings_backup_export_settings_supporting),
                            leadingIcon = Icons.Outlined.FileUpload,
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportSettingsLauncher.launch("rivo_settings_backup_$timestamp.json")
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_settings),
                            supporting = stringResource(R.string.settings_backup_import_settings_supporting),
                            leadingIcon = Icons.Outlined.FileDownload,
                            onClick = {
                                importSettingsLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_manage_contacts_headline),
                    icon = Icons.Outlined.Numbers
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_manage_standardize_numbers),
                            supporting = stringResource(R.string.settings_manage_standardize_numbers_supporting),
                            leadingIcon = Icons.Outlined.Numbers,
                            onClick = { showStandardizeConfirm = true }
                        )
                    }
                }
            }

            item {
            }
        }
    }

    if (showStandardizeConfirm) {
        val completedMessage = stringResource(R.string.settings_manage_standardize_completed)
        RivoConfirmationDialog(
            onDismissRequest = { showStandardizeConfirm = false },
            onConfirm = {
                showStandardizeConfirm = false
                contactsVM.formatAllPhoneNumbers()
                scope.launch {
                    snackbarHostState.showSnackbar(completedMessage)
                }
            },
            title = stringResource(R.string.settings_manage_standardize_numbers),
            message = stringResource(R.string.settings_manage_standardize_confirm_message),
            confirmLabel = stringResource(R.string.action_confirm),
            icon = Icons.Outlined.Numbers
        )
    }

    if (pendingSettingsRestoreUri != null) {
        RivoConfirmationDialog(
            onDismissRequest = { pendingSettingsRestoreUri = null },
            onConfirm = {
                val uri = pendingSettingsRestoreUri
                pendingSettingsRestoreUri = null
                if (uri != null) {
                    viewModel.importAppSettings(context, uri)
                }
            },
            title = stringResource(R.string.settings_backup_restore_confirm_title),
            message = stringResource(R.string.settings_backup_restore_confirm_message),
            confirmLabel = stringResource(R.string.settings_backup_restore_confirm_action),
            icon = Icons.Outlined.Restore
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(stringResource(R.string.settings_backup_password_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = passwordDraft,
                    onValueChange = { passwordDraft = it },
                    label = { Text(stringResource(R.string.settings_backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = passwordDraft.isNotBlank(),
                    onClick = {
                        backupPassword = passwordDraft
                        prefs.setContactBackupPassword(passwordDraft)
                        if (passwordDialogEnablesEncryption) {
                            encryptionEnabled = true
                            prefs.setContactBackupEncryptionEnabled(true)
                        }
                        showPasswordDialog = false
                        if (passwordDialogForImport) {
                            importContactsLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    contactImportProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_backup_import_progress_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RivoLinearProgressIndicator(
                        progress = {
                            if (progress.total == 0) 0f
                            else progress.imported.toFloat() / progress.total
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(
                            R.string.settings_backup_import_progress_count,
                            progress.imported,
                            progress.total
                        )
                    )
                }
            },
            confirmButton = {}
        )
    }
}
