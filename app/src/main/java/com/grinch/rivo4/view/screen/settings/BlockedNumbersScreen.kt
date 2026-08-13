package com.grinch.rivo4.view.screen.settings

import android.content.Context
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.BlockedNumber
import com.grinch.rivo4.controller.util.BlockedNumbersManager
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BlockedNumbersScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var blockMethod by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0)) }
    var logVisibility by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0)) }
    var blockNotification by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) }

    var blockedNumbers by remember { mutableStateOf<List<BlockedNumber>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newNumber by remember { mutableStateOf("") }

    LaunchedEffect(refreshKey) {
        blockedNumbers = BlockedNumbersManager.getAll(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_blocked_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newNumber = ""
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.blocked_add_number))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.blocked_list_title),
                    icon = Icons.Outlined.Block
                ) {
                    if (blockedNumbers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.blocked_list_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        blockedNumbers.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    RivoListItem(
                                        headline = formatPhoneNumber(entry.originalNumber),
                                        supporting = stringResource(R.string.blocked_list_item_supporting),
                                        leadingIcon = Icons.Outlined.Block,
                                        onClick = { }
                                    )
                                }
                                TextButton(onClick = {
                                    BlockedNumbersManager.unblockById(context, entry.id)
                                    refreshKey++
                                }) {
                                    Text(stringResource(R.string.action_unblock))
                                }
                            }
                            if (index < blockedNumbers.size - 1) {
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            item {
                RivoExpressiveCard {
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
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
            }

            item {
                RivoExpressiveCard {
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

            item {
                Button(
                    onClick = {
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        try {
                            val intent = telecomManager.createManageBlockedNumbersIntent()
                            context.startActivity(intent)
                        } catch (e: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_blocked_system_button))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        RivoDialog(
            onDismissRequest = { showAddDialog = false },
            title = stringResource(R.string.blocked_add_number),
            icon = Icons.Outlined.Block,
            confirmButton = {
                TextButton(
                    enabled = newNumber.isNotBlank(),
                    onClick = {
                        BlockedNumbersManager.block(context, newNumber.trim())
                        showAddDialog = false
                        refreshKey++
                    }
                ) {
                    Text(stringResource(R.string.action_block))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            OutlinedTextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.blocked_add_number_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }
    }
}
