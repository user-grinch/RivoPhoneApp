package com.grinch.rivo4.view.screen
import com.grinch.rivo4.view.components.MenuTopAppBar

import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.formatTime
import com.grinch.rivo4.modal.data.Contact
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.rounded.Call
import org.koin.compose.viewmodel.koinActivityViewModel

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
import com.grinch.rivo4.view.components.SimBadge
import com.grinch.rivo4.view.components.resolveSimNumber
import com.grinch.rivo4.view.components.RivoListItemDefaults
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

    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val displayOrder by contactsVM.displayOrder.collectAsState()

    val contactsById = remember(allContacts) { allContacts.associateBy { it.id } }
    val contactsByNumber = remember(allContacts) {
        val map = mutableMapOf<String, Contact>()
        for (c in allContacts) {
            for (p in c.phoneNumbers) {
                val norm = normalizePhoneNumber(p)
                val key = if (norm.length >= 10) norm.takeLast(10) else norm
                if (key.isNotEmpty()) map[key] = c
            }
        }
        map
    }

    val matchedContact = remember(contactId, phoneNumber, allContacts, contactsById, contactsByNumber) {
        if (contactId != null && contactId != "null") {
            contactsById[contactId]
        } else if (phoneNumber != null) {
            val norm = normalizePhoneNumber(phoneNumber)
            val key = if (norm.length >= 10) norm.takeLast(10) else norm
            if (key.isNotEmpty()) contactsByNumber[key] else null
        } else null
    }

    val isHistoryWithContact = (contactId != null && contactId != "null") || phoneNumber != null

    val filteredLogsByContact = remember(allLogs, contactId, phoneNumber) {
        if (contactId == null && phoneNumber == null) allLogs
        else allLogs.filter { log ->
            (contactId != null && contactId != "null" && log.contactId == contactId) || 
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }

    val contactName = remember(matchedContact, filteredLogsByContact, displayOrder) {
        matchedContact?.let { ContactUtils.formatContactName(it, displayOrder) }
            ?: filteredLogsByContact.firstOrNull { it.name != null && it.name != it.number }?.let {
                ContactUtils.formatContactName(it.name!!, displayOrder)
            }
            ?: (if (phoneNumber != null) formatPhoneNumber(phoneNumber) else null)
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
            displayOrder = displayOrder
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
                    MenuTopAppBar(
                        text = if (contactName != null) stringResource(R.string.call_history_with_contact, contactName) else stringResource(R.string.call_history_title),
                        navigator = navigator
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
                                    val shape = rivoGroupedItemShape(index, logsInGroup.size)

                                    val handleCall: () -> Unit = {
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
                                    }

                                    if (isHistoryWithContact) {
                                        val isSelected = selectedEntries.any { it.id == lg.id }
                                        val isBlocked = lg.isBlocked || lg.type == CallLog.Calls.BLOCKED_TYPE
                                        val isMissedOrRejected = lg.type == CallLog.Calls.MISSED_TYPE || lg.type == CallLog.Calls.REJECTED_TYPE

                                        val callTypeLabel = when {
                                            isBlocked -> stringResource(R.string.call_type_blocked)
                                            lg.type == CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
                                            lg.type == CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
                                            lg.type == CallLog.Calls.MISSED_TYPE -> stringResource(R.string.call_type_missed)
                                            lg.type == CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.call_type_rejected)
                                            lg.type == CallLog.Calls.VOICEMAIL_TYPE -> stringResource(R.string.call_type_voicemail)
                                            else -> stringResource(R.string.call_type_call)
                                        }

                                        val leadingIcon = when {
                                            isBlocked -> Icons.Default.Block
                                            lg.type == CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                                            lg.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                                            lg.type == CallLog.Calls.MISSED_TYPE || lg.type == CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        }

                                        val iconTint = when {
                                            isBlocked || isMissedOrRejected -> MaterialTheme.colorScheme.error
                                            lg.type == CallLog.Calls.OUTGOING_TYPE -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        val supportingText = buildString {
                                            append(formatTime(context, lg.date))
                                            if (lg.duration > 0) {
                                                append(" • ${android.text.format.DateUtils.formatElapsedTime(lg.duration)}")
                                            }
                                            if (lg.count > 1) {
                                                append(" (${lg.count})")
                                            }
                                        }

                                        val lgSimNumber = lg.simNumber ?: resolveSimNumber(lg)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    RivoListItem(
                                                        headline = callTypeLabel,
                                                        headlineColor = if (isBlocked || isMissedOrRejected) MaterialTheme.colorScheme.error else Color.Unspecified,
                                                        supporting = if (!callLogConfig.showSim || lgSimNumber == null) supportingText else null,
                                                        supportingContent = if (callLogConfig.showSim && lgSimNumber != null) {
                                                            {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(top = 1.dp)
                                                                ) {
                                                                    SimBadge(simNumber = lgSimNumber)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = supportingText,
                                                                        style = RivoListItemDefaults.supportingStyle(),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        } else null,
                                                        leadingContent = {
                                                            Icon(
                                                                imageVector = leadingIcon,
                                                                contentDescription = null,
                                                                tint = iconTint,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        },
                                                        selected = isSelected,
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
                                                        }
                                                    )
                                                }

                                                if (!isSelected) {
                                                    IconButton(
                                                        onClick = handleCall,
                                                        modifier = Modifier.padding(end = 10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Call,
                                                            contentDescription = stringResource(R.string.action_call),
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val itemContact = lg.contactId?.let { contactsById[it] } ?: run {
                                            val norm = normalizePhoneNumber(lg.number)
                                            val key = if (norm.length >= 10) norm.takeLast(10) else norm
                                            if (key.isNotEmpty()) contactsByNumber[key] else null
                                        }

                                        val showCards = surfaceStyle.showCards
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (showCards) {
                                                            Modifier
                                                                .clip(shape)
                                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                        } else Modifier
                                                    )
                                            ) {
                                                CallLogTileSimple(
                                                log = lg,
                                                contact = itemContact,
                                                displayOrder = displayOrder,
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
                                                onCallClick = handleCall,
                                                selected = selectedEntries.any { it.id == lg.id },
                                                onSwipeAction = { action, log ->
                                                    if (action == SwipeActionType.DELETE) {
                                                        viewModel.deleteCallLogsByIds(log.ids)
                                                    }
                                                }
                                            )
                                            }
                                            if (!showCards && index < logsInGroup.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                )
                                            }
                                        }
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
