package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.modal.data.CallLogEntry
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>()
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
        val logs by viewModel.allCallLogs.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val context = LocalContext.current
        val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

        var showSimPicker by remember { mutableStateOf(false) }
        var pendingNumber by remember { mutableStateOf<String?>(null) }

        val filteredLogs = remember(logs, selectedFilter) {
            when (selectedFilter) {
                CallLogFilter.All -> logs
                CallLogFilter.Missed -> logs.filter { it.type == CallLog.Calls.MISSED_TYPE }
                CallLogFilter.Incoming -> logs.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                CallLogFilter.Outgoing -> logs.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                CallLogFilter.Contacts -> logs.filter { it.name != null && it.name != it.number }
            }
        }

        val groupedLogs = remember(filteredLogs) {
            filteredLogs.groupBy { formatDateHeader(it.date) }
        }

        if (showSimPicker && pendingNumber != null) {
            SimPickerDialog(
                onDismissRequest = { showSimPicker = false },
                onSimSelected = { handle ->
                    makeCall(context, pendingNumber!!, handle)
                    showSimPicker = false
                }
            )
        }

        if (isLoading && logs.isEmpty()) {
            RivoLoadingIndicatorView()
        } else if (logs.isEmpty()) {
            EmptyCallLogsState()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                                                val hasPermission =
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.READ_PHONE_STATE
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                if (hasPermission) {
                                                    val accounts = telecomManager.callCapablePhoneAccounts
                                                    if (accounts.size > 1) {
                                                        pendingNumber = log.number
                                                        showSimPicker = true
                                                    } else {
                                                        makeCall(context, log.number)
                                                    }
                                                } else {
                                                    makeCall(context, log.number)
                                                }
                                            },
                                            onLongClick = { log ->
                                                onToggleSelection(log)
                                            },
                                            selected = selectedEntries.any { it.id == lg.id }
                                        )
                                        if (index < logsInGroup.size - 1) {
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
