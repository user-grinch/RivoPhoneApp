package com.grinch.rivo4.view.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.BackupViewModel
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val viewModel: BackupViewModel = koinActivityViewModel()
    val status by viewModel.status.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_backup_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.settings_backup_contacts_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                RivoExpressiveCard {
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_export_contacts),
                        supporting = stringResource(R.string.settings_backup_export_contacts_supporting),
                        leadingIcon = Icons.Outlined.FileUpload,
                        onClick = { exportContactsLauncher.launch("contacts_backup.vcf") }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_import_contacts),
                        supporting = stringResource(R.string.settings_backup_import_contacts_supporting),
                        leadingIcon = Icons.Outlined.FileDownload,
                        onClick = { importContactsLauncher.launch(arrayOf("text/vcard", "text/x-vcard")) }
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_backup_call_logs_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                RivoExpressiveCard {
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_export_logs),
                        supporting = stringResource(R.string.settings_backup_export_logs_supporting),
                        leadingIcon = Icons.Outlined.History,
                        onClick = { exportLogsLauncher.launch("call_logs_backup.json") }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_import_logs),
                        supporting = stringResource(R.string.settings_backup_import_logs_supporting),
                        leadingIcon = Icons.Outlined.Restore,
                        onClick = { importLogsLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }
        }
    }
}
