package com.grinch.rivo4.view.screen.settings

import android.content.Context
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
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
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.OpenResultRecipient
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BlockedNumbersScreen(
    navigator: DestinationsNavigator,
    resultRecipient: OpenResultRecipient<String>
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsState by prefs.settingsChanged.collectAsState()

    var blockMethod by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0)) }
    var logVisibility by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0)) }
    var blockNotification by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) }

    var blockedNumbers by remember { mutableStateOf<List<BlockedNumber>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var numberToUnblock by remember { mutableStateOf<BlockedNumber?>(null) }

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
            blockedNumbers.filter {
                it.originalNumber.contains(searchQuery) ||
                formatPhoneNumber(it.originalNumber).contains(searchQuery)
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_blocked_numbers_headline),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${blockedNumbers.size} numbers blocked • " + stringResource(R.string.settings_blocked_numbers_supporting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Outlined.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Select Contacts or Enter Number")
                        }
                    }
                }
            }

            // In-Page Search Field
            if (blockedNumbers.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search blocked list...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Blocked List
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.blocked_list_title),
                    icon = Icons.Outlined.Block
                ) {
                    if (filteredBlockedNumbers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 16.dp),
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
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isBlank()) stringResource(R.string.blocked_list_empty) else "No matching blocked numbers",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filteredBlockedNumbers.forEachIndexed { index, entry ->
                            val matchedContact = remember(allContacts, entry.originalNumber) {
                                allContacts.find { c ->
                                    c.phoneNumbers.any { num -> com.grinch.rivo4.controller.util.areNumbersEqual(num, entry.originalNumber) }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (matchedContact != null) {
                                        RivoListItem(
                                            headline = matchedContact.name,
                                            supporting = formatPhoneNumber(entry.originalNumber),
                                            avatarName = matchedContact.name,
                                            photoUri = matchedContact.photoUri,
                                            onClick = { }
                                        )
                                    } else {
                                        RivoListItem(
                                            headline = formatPhoneNumber(entry.originalNumber),
                                            supporting = stringResource(R.string.blocked_list_item_supporting),
                                            leadingIcon = Icons.Outlined.Block,
                                            onClick = { }
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { numberToUnblock = entry },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_unblock),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            if (index < filteredBlockedNumbers.size - 1) {
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // Blocking Settings Section
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

            // Notifications Section
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

            // System Blocked Numbers Button
            item {
                OutlinedButton(
                    onClick = {
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        try {
                            val intent = telecomManager.createManageBlockedNumbersIntent()
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_blocked_system_button))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (numberToUnblock != null) {
        val target = numberToUnblock!!
        RivoDialog(
            onDismissRequest = { numberToUnblock = null },
            title = stringResource(R.string.action_unblock),
            icon = Icons.Outlined.LockOpen,
            confirmButton = {
                TextButton(
                    onClick = {
                        BlockedNumbersManager.unblockById(context, target.id)
                        numberToUnblock = null
                        refreshKey++
                    }
                ) {
                    Text(stringResource(R.string.action_unblock))
                }
            },
            dismissButton = {
                TextButton(onClick = { numberToUnblock = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Text(
                text = "Unblock ${formatPhoneNumber(target.originalNumber)}?",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
