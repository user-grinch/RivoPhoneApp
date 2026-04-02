package com.grinch.rivo4.view.screen

import android.content.Context
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.util.formatDateHeader
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallLogFullScreen(
    navigator: DestinationsNavigator,
    contactId: String? = null,
    phoneNumber: String? = null
) {
    val viewModel: CallLogViewModel = koinActivityViewModel()
    val allLogs by viewModel.allCallLogs.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    var showSimPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }

    val filteredLogsByContact = remember(allLogs, contactId, phoneNumber) {
        if (contactId == null && phoneNumber == null) allLogs
        else allLogs.filter { log ->
            (contactId != null && contactId != "null" && log.contactId == contactId) || 
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }

    val contactName = remember(filteredLogsByContact) {
        filteredLogsByContact.firstOrNull { it.name != null && it.name != it.number }?.name ?: phoneNumber
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (contactName != null) "History with $contactName" else "Call History", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
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

                if (filteredLogsByContact.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No call history found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val finalLogs = when (selectedFilter) {
                        CallLogFilter.All -> filteredLogsByContact
                        CallLogFilter.Missed -> filteredLogsByContact.filter { it.type == CallLog.Calls.MISSED_TYPE }
                        CallLogFilter.Incoming -> filteredLogsByContact.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                        CallLogFilter.Outgoing -> filteredLogsByContact.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                        CallLogFilter.Contacts -> filteredLogsByContact.filter { it.name != null && it.name != it.number }
                    }

                    val groupedLogs = finalLogs.groupBy { formatDateHeader(it.date) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        groupedLogs.forEach { (header, logsInGroup) ->
                            item {
                                RivoSectionHeader(title = header)
                                Spacer(modifier = Modifier.height(8.dp))
                                RivoExpressiveCard {
                                    logsInGroup.forEachIndexed { index, lg ->
                                        CallLogTileSimple(lg)
                                        
                                        if (index < logsInGroup.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }

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
