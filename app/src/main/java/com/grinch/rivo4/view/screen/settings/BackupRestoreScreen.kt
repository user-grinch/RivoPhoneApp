package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

import android.net.Uri
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
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.BackupViewModel
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
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
    val status by viewModel.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStandardizeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val exportContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        uri?.let { viewModel.exportContacts(context, it) }
    }

    val importContactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importContacts(context, it) }
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
                text = stringResource(R.string.settings_backup_title),
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
                            headline = stringResource(R.string.settings_backup_export_contacts),
                            supporting = stringResource(R.string.settings_backup_export_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileUpload,
                            onClick = { exportContactsLauncher.launch("contacts_backup.vcf") }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_import_contacts),
                            supporting = stringResource(R.string.settings_backup_import_contacts_supporting),
                            leadingIcon = Icons.Outlined.FileDownload,
                            onClick = { importContactsLauncher.launch(arrayOf("text/vcard", "text/x-vcard")) }
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
}
