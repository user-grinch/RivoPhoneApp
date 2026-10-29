package com.grinch.rivo4.view.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.BackupViewModel
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: BackupViewModel = koinActivityViewModel()
    val prefs = koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var encryptionEnabled by remember(settingsState) { mutableStateOf(prefs.isContactBackupEncryptionEnabled()) }
    var autoBackupEncryptionEnabled by remember(settingsState) { mutableStateOf(prefs.isAutoBackupEncryptionEnabled()) }
    var autoExportEnabled by remember(settingsState) { mutableStateOf(prefs.isContactAutoExportEnabled()) }
    var backupPassword by remember(settingsState) { mutableStateOf(prefs.getContactBackupPassword()) }
    var backupFolderUri by remember(settingsState) {
        mutableStateOf(prefs.getContactBackupDirectoryUri())
    }
    var retentionCount by remember(settingsState) {
        mutableIntStateOf(prefs.getContactBackupRetentionCount().coerceIn(1, 5))
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordDraft by remember { mutableStateOf("") }
    var confirmPasswordDraft by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordDialogForImport by remember { mutableStateOf(false) }
    var passwordDialogEnablesEncryption by remember { mutableStateOf(false) }
    var passwordDialogEnablesAutoBackupEncryption by remember { mutableStateOf(false) }
    var passwordDialogTriggersExport by remember { mutableStateOf(false) }
    var showRemovePasswordConfirm by remember { mutableStateOf(false) }

    val status by viewModel.status.collectAsState()
    val contactImportProgress by viewModel.contactImportProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(status) {
        status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val exportContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        uri?.let { viewModel.exportContacts(context, it, encrypted = false) }
    }

    val exportEncryptedContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportContacts(context, it, encrypted = true) }
    }

    val importContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importContacts(context, it) }
    }

    val importEncryptedContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importContacts(context, it) }
    }

    fun requestPassword(
        forEncryptedImport: Boolean = false,
        enablesEncryption: Boolean = false,
        triggersExport: Boolean = false,
        enablesAutoBackupEncryption: Boolean = false
    ) {
        passwordDraft = backupPassword
        confirmPasswordDraft = backupPassword
        passwordVisible = false
        confirmPasswordVisible = false
        passwordError = null
        passwordDialogForImport = forEncryptedImport
        passwordDialogEnablesEncryption = enablesEncryption
        passwordDialogTriggersExport = triggersExport
        passwordDialogEnablesAutoBackupEncryption = enablesAutoBackupEncryption
        showPasswordDialog = true
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

            target.contains("Encrypt", ignoreCase = true) ||
            target.contains("Password", ignoreCase = true) -> 1

            target.contains("Auto", ignoreCase = true) ||
            target.contains("Retention", ignoreCase = true) ||
            target.contains("Version", ignoreCase = true) -> 2

            target.contains("Log", ignoreCase = true) ||
            target.contains("History", ignoreCase = true) -> 3

            target.contains("Setting", ignoreCase = true) ||
            target.contains("Preference", ignoreCase = true) ||
            target.contains("Configuration", ignoreCase = true) -> 4

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
            // 1. Standard (Unencrypted) Import & Export
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_standard_contacts_header),
                    icon = Icons.Outlined.ContactPhone
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_contacts),
                            supporting = stringResource(R.string.settings_backup_export_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileUpload,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportContactsLauncher.launch("contacts_backup_$timestamp.vcf")
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_contacts),
                            supporting = stringResource(R.string.settings_backup_import_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileDownload,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                importContactsLauncher.launch(arrayOf("text/vcard", "text/x-vcard", "*/*"))
                            }
                        )
                    }
                }
            }

            // 2. Encrypted Backups Section (Separated)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_encrypted_header),
                    icon = Icons.Outlined.EnhancedEncryption
                ) {
                    // Encrypt Toggle
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_backup_encryption),
                            supporting = stringResource(R.string.settings_backup_encryption_supporting),
                            leadingIcon = if (encryptionEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            checked = encryptionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && backupPassword.isBlank()) {
                                    requestPassword(enablesEncryption = true)
                                } else {
                                    encryptionEnabled = enabled
                                    prefs.setContactBackupEncryptionEnabled(enabled)
                                }
                            }
                        )
                    }

                    // Improved Password Status & Management Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_backup_password),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (backupPassword.isNotBlank()) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                        },
                                        contentColor = if (backupPassword.isNotBlank()) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (backupPassword.isNotBlank()) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(
                                                    if (backupPassword.isNotBlank()) R.string.settings_backup_password_status_configured
                                                    else R.string.settings_backup_password_status_not_set
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = stringResource(
                                        if (backupPassword.isNotBlank()) R.string.settings_backup_password_desc_configured
                                        else R.string.settings_backup_password_desc_not_set
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (backupPassword.isNotBlank()) {
                                        TextButton(
                                            onClick = { showRemovePasswordConfirm = true }
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.action_remove))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        FilledTonalButton(
                                            onClick = { requestPassword() },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.action_change))
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { requestPassword() },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.settings_backup_set_password))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Manual Export Encrypted Backup
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_encrypted_contacts),
                            supporting = stringResource(R.string.settings_backup_export_encrypted_contacts_supporting),
                            leadingIcon = Icons.Outlined.EnhancedEncryption,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                if (backupPassword.isBlank()) {
                                    requestPassword(triggersExport = true)
                                } else {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    exportEncryptedContactsLauncher.launch("contacts_backup_$timestamp.vcf.enc")
                                }
                            }
                        )
                    }

                    // Manual Import Encrypted Backup
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_encrypted_contacts),
                            supporting = stringResource(R.string.settings_backup_import_encrypted_contacts_supporting),
                            leadingIcon = Icons.Outlined.LockOpen,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                if (backupPassword.isBlank()) {
                                    requestPassword(forEncryptedImport = true)
                                } else {
                                    importEncryptedContactsLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                }
                            }
                        )
                    }
                }
            }

            // 3. Automatic Backups Section (with 1 to 5 slider)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_automatic_header),
                    icon = Icons.Outlined.AutoMode
                ) {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_backup_auto_export),
                            supporting = if (backupFolderUri == null) {
                                stringResource(R.string.settings_backup_folder_not_set)
                            } else {
                                stringResource(R.string.settings_backup_auto_export_supporting)
                            },
                            leadingIcon = Icons.Outlined.AutoMode,
                            checked = autoExportEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && backupFolderUri == null) {
                                    backupFolderLauncher.launch(null)
                                } else {
                                    autoExportEnabled = enabled
                                    prefs.setContactAutoExportEnabled(enabled)
                                }
                            }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_backup_auto_encrypt),
                            supporting = stringResource(R.string.settings_backup_auto_encrypt_supporting),
                            leadingIcon = if (autoBackupEncryptionEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            checked = autoBackupEncryptionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && backupPassword.isBlank()) {
                                    requestPassword(enablesAutoBackupEncryption = true)
                                } else {
                                    autoBackupEncryptionEnabled = enabled
                                    prefs.setAutoBackupEncryptionEnabled(enabled)
                                }
                            },
                            enabled = autoExportEnabled
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
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = { backupFolderLauncher.launch(null) }
                        )
                    }
                    item {
                        RivoSliderListItem(
                            headline = stringResource(R.string.settings_backup_retention),
                            supporting = stringResource(R.string.settings_backup_retention_supporting),
                            leadingIcon = Icons.Outlined.History,
                            value = retentionCount.toFloat().coerceIn(1f, 5f),
                            valueRange = 1f..5f,
                            steps = 3,
                            valueLabel = retentionCount.toString(),
                            onValueChange = { newValue ->
                                retentionCount = newValue.roundToInt().coerceIn(1, 5)
                            },
                            onValueChangeFinished = {
                                prefs.setContactBackupRetentionCount(retentionCount)
                            },
                            enabled = autoExportEnabled
                        )
                    }
                }
            }

            // 4. Call Logs
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_backup_call_logs_header),
                    icon = Icons.Outlined.History
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_export_logs),
                            supporting = stringResource(R.string.settings_backup_export_logs_supporting),
                            leadingIcon = Icons.Outlined.FileUpload,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = { exportLogsLauncher.launch("call_logs_backup.json") }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_logs),
                            supporting = stringResource(R.string.settings_backup_import_logs_supporting),
                            leadingIcon = Icons.Outlined.FileDownload,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = { importLogsLauncher.launch(arrayOf("application/json")) }
                        )
                    }
                }
            }

            // 5. App Settings
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
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                importSettingsLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    }
                }
            }
        }
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

    if (showRemovePasswordConfirm) {
        RivoConfirmationDialog(
            onDismissRequest = { showRemovePasswordConfirm = false },
            onConfirm = {
                showRemovePasswordConfirm = false
                backupPassword = ""
                prefs.setContactBackupPassword("")
                encryptionEnabled = false
                prefs.setContactBackupEncryptionEnabled(false)
                autoBackupEncryptionEnabled = false
                prefs.setAutoBackupEncryptionEnabled(false)
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_backup_password_cleared))
                }
            },
            title = stringResource(R.string.settings_backup_remove_password),
            message = stringResource(R.string.settings_backup_remove_password_confirm),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Outlined.Delete
        )
    }

    if (showPasswordDialog) {
        val isForImport = passwordDialogForImport
        val isSettingNew = !isForImport
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = if (isForImport) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = when {
                        isForImport -> stringResource(R.string.settings_backup_password_dialog_title)
                        backupPassword.isBlank() -> stringResource(R.string.settings_backup_set_password)
                        else -> stringResource(R.string.settings_backup_change_password)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isForImport) {
                            stringResource(R.string.settings_backup_import_encrypted_contacts_supporting)
                        } else {
                            stringResource(R.string.settings_backup_password_helper)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordDraft,
                        onValueChange = {
                            passwordDraft = it
                            passwordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_backup_password)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (isSettingNew) {
                        OutlinedTextField(
                            value = confirmPasswordDraft,
                            onValueChange = {
                                confirmPasswordDraft = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.settings_backup_confirm_password)) },
                            isError = passwordError != null || (confirmPasswordDraft.isNotBlank() && confirmPasswordDraft != passwordDraft),
                            supportingText = if (passwordError != null || (confirmPasswordDraft.isNotBlank() && confirmPasswordDraft != passwordDraft)) {
                                { Text(passwordError ?: stringResource(R.string.settings_backup_password_mismatch), color = MaterialTheme.colorScheme.error) }
                            } else null,
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                val canConfirm = if (isSettingNew) {
                    passwordDraft.isNotBlank() && passwordDraft == confirmPasswordDraft
                } else {
                    passwordDraft.isNotBlank()
                }
                Button(
                    enabled = canConfirm,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        backupPassword = passwordDraft
                        prefs.setContactBackupPassword(passwordDraft)
                        if (passwordDialogEnablesEncryption) {
                            encryptionEnabled = true
                            prefs.setContactBackupEncryptionEnabled(true)
                        }
                        if (passwordDialogEnablesAutoBackupEncryption) {
                            autoBackupEncryptionEnabled = true
                            prefs.setAutoBackupEncryptionEnabled(true)
                        }
                        showPasswordDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_backup_password_saved))
                        }
                        if (passwordDialogForImport) {
                            importEncryptedContactsLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        } else if (passwordDialogTriggersExport) {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            exportEncryptedContactsLauncher.launch("contacts_backup_$timestamp.vcf.enc")
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (isForImport) R.string.action_confirm
                            else R.string.action_save
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
