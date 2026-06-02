package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.util.formatDateHeader
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.controller.util.areNumbersEqual
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>(start = true, style = NoTransitions::class)
@Composable
fun RecentScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val viewModel: CallLogViewModel = koinActivityViewModel()
    
    var selectedEntries by remember { mutableStateOf(setOf<CallLogEntry>()) }
    
    BackHandler(enabled = selectedEntries.isNotEmpty()) {
        selectedEntries = emptySet()
    }
    
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AnimatedContent(
                targetState = selectedEntries.isNotEmpty(),
                transitionSpec = {
                    (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                },
                label = "TopBarTransition"
            ) { isSelecting ->
                if (!isSelecting) {
                    Column {
                        TopBar(navController, navigator)
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CallLogFilter.entries) { filter ->
                                RivoFilterChip(filter.name, selectedFilter == filter, {
                                        _ ->
                                    viewModel.setFilter(filter)
                                })
                            }
                        }
                    }
                } else {
                    BatchCallLogActionBar(
                        selectedCount = selectedEntries.size,
                        onClearSelection = { selectedEntries = emptySet() },
                        onDelete = {
                            val allIdsToDelete = selectedEntries.flatMap { it.ids }
                            viewModel.deleteCallLogsByIds(allIdsToDelete)
                            selectedEntries = emptySet()
                        },
                        onClearAll = {
                            viewModel.clearCallLogs()
                            selectedEntries = emptySet()
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedEntries.isEmpty()) {
                FloatingActionButton(
                    onClick = { navigator.navigate(DialPadScreenDestination()) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.Dialpad, "Dialpad")
                }
            }
        },
        bottomBar = {
            if (selectedEntries.isEmpty()) {
                BottomBar(navController, navigator)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            CallLogFullContent(
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState,
                selectedEntries = selectedEntries,
                onToggleSelection = { entry ->
                    selectedEntries = if (selectedEntries.any { it.id == entry.id }) {
                        selectedEntries.filter { it.id != entry.id }.toSet()
                    } else {
                        selectedEntries + entry
                    }
                }
            )
            
            ScrollToTopButton(
                visible = showButton && selectedEntries.isEmpty(),
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogFullContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedEntries: Set<CallLogEntry>,
    onToggleSelection: (CallLogEntry) -> Unit
) {
    if (isGranted) {
        val viewModel: CallLogViewModel = koinActivityViewModel()
        val contactsVM: ContactsViewModel = koinActivityViewModel()
        val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
        val settingsState by prefs.settingsChanged.collectAsState()
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val hapticScrollEnabled = prefs.getBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)
        if (hapticScrollEnabled) {
            LaunchedEffect(listState.firstVisibleItemIndex) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        }
        
        LaunchedEffect(Unit) {
            viewModel.fetchLogs()
            contactsVM.fetchContacts()
        }

        val logs by viewModel.allCallLogs.collectAsState()
        val allContacts by contactsVM.allContacts.collectAsState()
        val favorites = remember(allContacts) { allContacts.filter { it.isFavorite } }

        val isLoading by viewModel.isLoading.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val context = LocalContext.current
        val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

        var showSimPicker by remember { mutableStateOf(false) }
        var showNumberPicker by remember { mutableStateOf(false) }
        var pendingNumber by remember { mutableStateOf<String?>(null) }
        var pendingContactId by remember { mutableStateOf<String?>(null) }
        var activeFavorite by remember { mutableStateOf<Contact?>(null) }
        val blockLogVisibility = prefs.getInt(com.grinch.rivo4.controller.util.PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0)

        val performCall = { targetNumber: String, contactId: String? ->
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                val defaultSim = prefs.getInt("default_sim", 0)
                val accounts = telecomManager.callCapablePhoneAccounts
                if (accounts.size > 1 && defaultSim == 0) {
                    pendingNumber = targetNumber
                    pendingContactId = contactId
                    showSimPicker = true
                } else {
                    makeCall(context, targetNumber, contactId = contactId)
                }
            } else {
                makeCall(context, targetNumber, contactId = contactId)
            }
        }

        if (showNumberPicker && activeFavorite != null) {
            val lastUsed = activeFavorite?.id?.let { prefs.getLastUsedNumber(it) }
            RivoDialog(
                onDismissRequest = { showNumberPicker = false },
                title = "Select Number",
                icon = Icons.Default.Phone,
                dismissButton = {
                    TextButton(onClick = { showNumberPicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                activeFavorite!!.phoneNumbers.forEach { selectedNumber ->
                    val isRecent = lastUsed != null && areNumbersEqual(lastUsed, selectedNumber)
                    Surface(
                        onClick = {
                            showNumberPicker = false
                            performCall(selectedNumber, activeFavorite?.id)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isRecent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, null, tint = if (isRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedNumber, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isRecent) FontWeight.Bold else FontWeight.Normal)
                                if (isRecent) {
                                    Text("Recent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        val filteredLogs = remember(logs, selectedFilter, blockLogVisibility) {
            val baseLogs = if (blockLogVisibility == 0) logs.filter { !it.isBlocked } else logs

            when (selectedFilter) {
                CallLogFilter.All -> baseLogs
                CallLogFilter.Missed -> baseLogs.filter { it.type == CallLog.Calls.MISSED_TYPE }
                CallLogFilter.Incoming -> baseLogs.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                CallLogFilter.Outgoing -> baseLogs.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                CallLogFilter.Contacts -> baseLogs.filter { it.name != null && it.name != it.number }
            }
        }

        val groupedLogs = remember(filteredLogs) {
            filteredLogs.groupBy { formatDateHeader(it.date) }
        }

        if (showSimPicker && pendingNumber != null) {
            SimPickerDialog(
                onDismissRequest = { showSimPicker = false },
                onSimSelected = { handle ->
                    makeCall(context, pendingNumber!!, handle, contactId = pendingContactId)
                    showSimPicker = false
                }
            )
        }

        val pullToRefreshState = rememberPullToRefreshState()

        val showDividers = prefs.getBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_SHOW_DIVIDERS, true)

        PullToRefreshBox(
            isRefreshing = isLoading && logs.isNotEmpty(),
            onRefresh = { 
                viewModel.fetchLogs()
                contactsVM.fetchContacts()
            },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                RivoPullToRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isLoading && logs.isNotEmpty()
                )
            }
        ) {
            if (isLoading && logs.isEmpty()) {
                RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
            } else if (logs.isEmpty() && (favorites.isEmpty() || selectedFilter != CallLogFilter.All)) {
                EmptyCallLogsState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (favorites.isNotEmpty() && selectedFilter == CallLogFilter.All) {
                            item {
                                RivoSectionHeader(title = "Favorites")
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(favorites) { contact ->
                                        FavoriteCircleItem(
                                            contact = contact,
                                            onClick = {
                                                if (contact.phoneNumbers.size > 1) {
                                                    activeFavorite = contact
                                                    showNumberPicker = true
                                                } else {
                                                    contact.phoneNumbers.firstOrNull()?.let { 
                                                        performCall(it, contact.id)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        groupedLogs.forEach { (header, logsInGroup) ->
                            item {
                                RivoSectionHeader(title = header)
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RivoExpressiveCard {
                                        logsInGroup.forEachIndexed { index, lg ->
                                            CallLogTile(
                                                log = lg,
                                                onTileClick = { log ->
                                                    if (selectedEntries.isNotEmpty()) {
                                                        onToggleSelection(log)
                                                    } else {
                                                        navigator.navigate(
                                                            ContactDetailsScreenDestination(
                                                                contactId = log.contactId ?: "null",
                                                                phoneNumber = log.number
                                                            )
                                                        )
                                                    }
                                                },
                                                onButtonClick = { log ->
                                                    performCall(log.number, log.contactId)
                                                },
                                                onLongClick = { log ->
                                                    onToggleSelection(log)
                                                },
                                                selected = selectedEntries.any { it.id == lg.id }
                                            )
                                            if (showDividers && index < logsInGroup.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    } else {
        PermissionDeniedView(
            icon = Icons.Default.Call,
            title = "Call History",
            description = "Rivo needs access to your call logs to show your recent activity and missed calls.",
            onGrantClick = onRequestPermission
        )
    }
}

@Composable
fun FavoriteCircleItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RivoAvatar(
            name = contact.name,
            photoUri = contact.photoUri,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            badgeIcon = Icons.Default.Call
        )
        Text(
            text = contact.name.split(" ").firstOrNull() ?: "",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun EmptyCallLogsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    imageVector = Icons.Default.PhoneMissed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your call log is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Try clearing your filters or add a new contact.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}
