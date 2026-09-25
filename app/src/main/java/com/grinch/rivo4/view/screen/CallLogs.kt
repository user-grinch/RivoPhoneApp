package com.grinch.rivo4.view.screen

import android.content.Context
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.grinch.rivo4.view.components.CallLogTileConfig
import com.grinch.rivo4.view.components.LocalCallLogTileConfig
import com.grinch.rivo4.view.components.LocalRivoSurfaceStyle
import com.grinch.rivo4.view.components.rememberRivoSurfaceStyle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.util.formatDateHeader
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.normalizePhoneNumber
import com.grinch.rivo4.controller.util.areNumbersEqual
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.modal.data.displayLabel
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallLogFullScreen(
    navigator: DestinationsNavigator,
    contactId: String? = null,
    phoneNumber: String? = null
) {
    val viewModel: CallLogViewModel = koinActivityViewModel()
    val prefs: PreferenceManager = koinInject()

    LaunchedEffect(Unit) {
        viewModel.fetchLogs()
    }

    val allLogs by viewModel.allCallLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val settingsState by prefs.settingsChanged.collectAsState(initial = 0)

    var selectedEntries by remember { mutableStateOf(setOf<CallLogEntry>()) }
    
    BackHandler(enabled = selectedEntries.isNotEmpty()) {
        selectedEntries = emptySet()
    }
    
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    var showSimPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var pendingContactId by remember { mutableStateOf<String?>(null) }

    val filteredLogsByContact = remember(allLogs, contactId, phoneNumber) {
        if (contactId == null && phoneNumber == null) allLogs
        else allLogs.filter { log ->
            (contactId != null && contactId != "null" && log.contactId == contactId) || 
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }

    val contactName = remember(filteredLogsByContact) {
        filteredLogsByContact.firstOrNull { it.name != null && it.name != it.number }?.name ?: (if (phoneNumber != null) formatPhoneNumber(phoneNumber) else null)
    }

    if (showSimPicker && pendingNumber != null) {
        val cid = pendingContactId
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, pendingNumber!!, handle, contactId = cid)
                showSimPicker = false
            },
            showRememberOption = cid != null,
            onSimSelectedWithRemember = { handle, rememberForContact ->
                if (rememberForContact && cid != null) {
                    prefs.setDefaultSimForContact(cid, handle.id)
                }
                makeCall(context, pendingNumber!!, handle, contactId = cid)
                showSimPicker = false
            }
        )
    }
    val avatarStyle = rememberRivoAvatarStyle()
    val surfaceStyle = rememberRivoSurfaceStyle(prefs)
    val callLogConfig = remember(settingsState, selectedEntries.isNotEmpty()) {
        CallLogTileConfig(
            showSim = prefs.getBoolean(PreferenceManager.KEY_SHOW_SIM_ICON_HISTORY, true),
            swipeEnabled = prefs.isSwipeActionsEnabled() && selectedEntries.isEmpty(),
            swipeRightAction = SwipeActionType.fromId(prefs.getSwipeRightAction()),
            swipeLeftAction = SwipeActionType.fromId(prefs.getSwipeLeftAction()),
            displayOrder = 0
        )
    }

    CompositionLocalProvider(
        LocalRivoAvatarStyle provides avatarStyle,
        LocalRivoSurfaceStyle provides surfaceStyle,
        LocalCallLogTileConfig provides callLogConfig
    ) {
        Scaffold(
            topBar = {
            AnimatedContent(
                targetState = selectedEntries.isNotEmpty(),
                transitionSpec = {
                    (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                },
                label = "TopBarTransition"
            ) { isSelecting ->
                if (!isSelecting) {
                    TopAppBar(
                        title = {
                            Text(
                                if (contactName != null) stringResource(R.string.call_history_with_contact, contactName) else stringResource(R.string.call_history_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        }
                    )
                } else {
                    BatchCallLogActionBar(
                        selectedCount = selectedEntries.size,
                        onClearSelection = { selectedEntries = emptySet() },
                        onDelete = {
                            val allIdsToDelete = selectedEntries.flatMap { it.ids }
                            viewModel.deleteCallLogsByIds(allIdsToDelete)
                            selectedEntries = emptySet()
                        },
                        onBlock = {
                            selectedEntries.forEach { entry ->
                                com.grinch.rivo4.controller.util.BlockedNumbersManager.block(context, entry.number)
                            }
                            selectedEntries = emptySet()
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CallLogFilter.entries) { filter ->
                        RivoFilterChip(filter.displayLabel(), selectedFilter == filter, {
                            _ ->
                            viewModel.setFilter(filter)
                        }, isAllFilter = filter == CallLogFilter.All)
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        RivoLoadingIndicatorView()
                    }
                } else if (filteredLogsByContact.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.call_log_no_history_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val finalLogs = remember(filteredLogsByContact, selectedFilter) {
                        when (selectedFilter) {
                            CallLogFilter.All -> filteredLogsByContact
                            CallLogFilter.Missed -> filteredLogsByContact.filter { it.type == CallLog.Calls.MISSED_TYPE }
                            CallLogFilter.Incoming -> filteredLogsByContact.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                            CallLogFilter.Outgoing -> filteredLogsByContact.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                            CallLogFilter.Contacts -> filteredLogsByContact.filter { it.name != null && it.name != it.number }
                        }
                    }

                    if (finalLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.call_log_no_filter_match), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val groupedLogs = remember(finalLogs) {
                            finalLogs.groupBy { formatDateHeader(context, it.date) }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            item(key = "ad_header", contentType = "ad") {
                                com.grinch.rivo4.view.components.ad.BannerAd()
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            groupedLogs.entries.forEachIndexed { groupIndex, (header, logsInGroup) ->
                                item(key = "header_${header}_$groupIndex", contentType = "header") {
                                    RivoSectionHeader(
                                        title = header,
                                        modifier = Modifier.padding(top = if (groupIndex == 0) 4.dp else 16.dp, bottom = 4.dp)
                                    )
                                }

                                itemsIndexed(
                                    items = logsInGroup,
                                    key = { _, lg -> lg.id },
                                    contentType = { _, _ -> "call_log" }
                                ) { index, lg ->
                                    val isFirst = index == 0
                                    val isLast = index == logsInGroup.size - 1
                                    val isSingle = logsInGroup.size == 1

                                    val shape = when {
                                        isSingle -> RoundedCornerShape(20.dp)
                                        isFirst -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                        isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                        else -> RoundedCornerShape(4.dp)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    ) {
                                        CallLogTileSimple(
                                            log = lg,
                                            showSim = callLogConfig.showSim,
                                            swipeEnabled = callLogConfig.swipeEnabled,
                                            swipeRightAction = callLogConfig.swipeRightAction,
                                            swipeLeftAction = callLogConfig.swipeLeftAction,
                                            onClick = {
                                                if (selectedEntries.isNotEmpty()) {
                                                    selectedEntries = if (selectedEntries.any { it.id == lg.id }) {
                                                        selectedEntries.filter { it.id != lg.id }.toSet()
                                                    } else {
                                                        selectedEntries + lg
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (selectedEntries.none { it.id == lg.id }) {
                                                    selectedEntries = selectedEntries + lg
                                                }
                                            },
                                            onCallClick = {
                                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.READ_PHONE_STATE
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                                val targetContactId = lg.contactId ?: contactId

                                                if (hasPermission) {
                                                    val accounts = telecomManager.callCapablePhoneAccounts
                                                    val favSim = targetContactId?.let { prefs.getDefaultSimForContact(it) }
                                                    val preferredHandle = if (favSim != null) accounts.find { it.id == favSim } else null
                                                    if (preferredHandle != null) {
                                                        makeCall(context, lg.number, preferredHandle, contactId = targetContactId)
                                                    } else if (accounts.size > 1 && prefs.getInt("default_sim", 0) == 0) {
                                                        pendingNumber = lg.number
                                                        pendingContactId = targetContactId
                                                        showSimPicker = true
                                                    } else {
                                                        makeCall(context, lg.number, contactId = targetContactId)
                                                    }
                                                } else {
                                                    makeCall(context, lg.number, contactId = targetContactId)
                                                }
                                            },
                                            selected = selectedEntries.any { it.id == lg.id },
                                            onSwipeAction = { action, log ->
                                                if (action == SwipeActionType.DELETE) {
                                                    viewModel.deleteCallLogsByIds(log.ids)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            }

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
}
