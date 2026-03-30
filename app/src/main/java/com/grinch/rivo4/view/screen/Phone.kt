package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.util.formatDate
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import com.grinch.rivo4.modal.data.CallLogFilter
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>
@Composable
fun RecentScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(navController, navigator)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigator.navigate(DialPadScreenDestination()) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Dialpad, "Dialpad")
            }
        },
        bottomBar = {
            BottomBar(navController, navigator)
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
                listState = listState
            )
            
            ScrollToTopButton(
                visible = showButton,
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
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (isGranted) {
        val viewModel: CallLogViewModel = koinActivityViewModel()
        val logs by viewModel.allCallLogs.collectAsState()
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

        if (logs.isEmpty()) {
            RivoLoadingIndicatorView()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CallLogFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

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
                                        val isMissed = lg.type == CallLog.Calls.MISSED_TYPE
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                RivoListItem(
                                                    headline = (lg.name ?: lg.number) + if (lg.count > 1) " (${lg.count})" else "",
                                                    supporting = "${if (lg.name != null && lg.name != lg.number) lg.number + " • " else ""}${formatDate(lg.date)}",
                                                    avatarName = lg.name ?: lg.number,
                                                    photoUri = lg.photoUri,
                                                    trailingIcon = when(lg.type) {
                                                        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                                                        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                                                        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                                                        else -> Icons.Default.Call
                                                    },
                                                    onClick = {
                                                        navigator.navigate(
                                                            ContactDetailsScreenDestination(
                                                                contactId = lg.contactId ?: "null",
                                                                phoneNumber = lg.number
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                                        val accounts = telecomManager.callCapablePhoneAccounts
                                                        if (accounts.size > 1) {
                                                            pendingNumber = lg.number
                                                            showSimPicker = true
                                                        } else {
                                                            makeCall(context, lg.number)
                                                        }
                                                    } else {
                                                        makeCall(context, lg.number)
                                                    }
                                                },
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Call, 
                                                    null, 
                                                    tint = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
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
