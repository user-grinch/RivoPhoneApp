package com.grinch.rivo4.view.screen

import android.content.Context
import android.provider.CallLog
import android.telecom.TelecomManager
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.BlockedNumbersManager
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.areNumbersEqual
import com.grinch.rivo4.controller.util.formatDateHeader
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.formatTime
import com.grinch.rivo4.controller.util.isYesterday
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.controller.util.normalizePhoneNumber
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.modal.data.displayLabel
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatCallDurationHuman(seconds: Long): String {
    if (seconds <= 0) return ""
    val minutes = seconds / 60
    val remSec = seconds % 60
    return if (minutes > 0) {
        if (remSec > 0) "${minutes}m ${remSec}s" else "${minutes}m"
    } else {
        "${remSec}s"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val settingsState by prefs.settingsChanged.collectAsState(initial = 0)

    var selectedEntries by remember { mutableStateOf(setOf<CallLogEntry>()) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showAddContactSheet by remember { mutableStateOf(false) }
    
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
    val contactsById by contactsVM.contactsById.collectAsState()
    val contactsByNumber by contactsVM.contactsByNumber.collectAsState()

    val callLauncher = rememberCallLauncher()
    val messageLauncher = rememberMessageLauncher()
    val videoLauncher = rememberVideoLauncher()

    val matchedContact = remember(contactId, phoneNumber, contactsById, contactsByNumber) {
        if (contactId != null && contactId != "null") {
            contactsById[contactId]
        } else if (phoneNumber != null) {
            var match: Contact? = null
            for (k in ContactUtils.getLookupKeys(phoneNumber)) {
                val c = contactsByNumber[k]
                if (c != null) {
                    match = c
                    break
                }
            }
            match
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

    val targetPhoneNumber = remember(phoneNumber, filteredLogsByContact) {
        phoneNumber ?: filteredLogsByContact.firstOrNull()?.number ?: ""
    }

    val contactName = remember(matchedContact, filteredLogsByContact, displayOrder) {
        matchedContact?.let { ContactUtils.formatContactName(it, displayOrder) }
            ?: filteredLogsByContact.firstOrNull { it.name != null && it.name != it.number }?.let {
                ContactUtils.formatContactName(it.name!!, displayOrder)
            }
            ?: (if (targetPhoneNumber.isNotEmpty()) formatPhoneNumber(targetPhoneNumber) else null)
    }

    val phoneLabel = remember(matchedContact, targetPhoneNumber) {
        if (matchedContact != null) {
            val entry = matchedContact.phones.firstOrNull { areNumbersEqual(it.number, targetPhoneNumber) }
                ?: matchedContact.phones.firstOrNull()
            if (entry != null) {
                android.provider.ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources,
                    entry.type,
                    entry.label
                ).toString()
            } else "Mobile"
        } else null
    }

    val headerSubtitle = remember(phoneLabel, targetPhoneNumber) {
        val formatted = formatPhoneNumber(targetPhoneNumber)
        if (phoneLabel != null) "$phoneLabel • $formatted" else formatted
    }

    val handleCallNumber: (String, String?) -> Unit = { numberToCall, cid ->
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val accounts = telecomManager.callCapablePhoneAccounts
            val favSim = cid?.let { prefs.getDefaultSimForContact(it) }
            val preferredHandle = if (favSim != null) accounts.find { it.id == favSim } else null
            if (preferredHandle != null) {
                makeCall(context, numberToCall, preferredHandle, contactId = cid)
            } else if (accounts.size > 1 && prefs.getInt("default_sim", 0) == 0) {
                pendingNumber = numberToCall
                pendingContactId = cid
                showSimPicker = true
            } else {
                makeCall(context, numberToCall, contactId = cid)
            }
        } else {
            makeCall(context, numberToCall, contactId = cid)
        }
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
                        if (isHistoryWithContact) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { navigator.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    ) {
                                        RivoAvatar(
                                            name = contactName ?: targetPhoneNumber,
                                            photoUri = matchedContact?.photoUri ?: filteredLogsByContact.firstOrNull()?.photoUri,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = matchedContact != null) {
                                                if (matchedContact != null) {
                                                    navigator.navigate(ContactDetailsScreenDestination(contactId = matchedContact.id))
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = contactName ?: formatPhoneNumber(targetPhoneNumber),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = headerSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box {
                                        var showMenu by remember { mutableStateOf(false) }
                                        RivoTopBarIconButton(
                                            onClick = { showMenu = true },
                                            icon = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.content_desc_options)
                                        )
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            if (matchedContact != null) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.swipe_action_view_contact)) },
                                                    leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                                    onClick = {
                                                        showMenu = false
                                                        navigator.navigate(ContactDetailsScreenDestination(contactId = matchedContact.id))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.action_edit)) },
                                                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                                    onClick = {
                                                        showMenu = false
                                                        navigator.navigate(ContactEditScreenDestination(contactId = matchedContact.id))
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.contact_add_to_contacts)) },
                                                    leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) },
                                                    onClick = {
                                                        showMenu = false
                                                        showAddContactSheet = true
                                                    }
                                                )
                                            }
                                            if (filteredLogsByContact.isNotEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.content_desc_clear_history)) },
                                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                                    onClick = {
                                                        showMenu = false
                                                        showDeleteAllDialog = true
                                                    }
                                                )
                                            }
                                            val isNumberBlocked = remember(targetPhoneNumber) {
                                                BlockedNumbersManager.isBlocked(context, targetPhoneNumber)
                                            }
                                            DropdownMenuItem(
                                                text = { Text(stringResource(if (isNumberBlocked) R.string.action_unblock_number else R.string.action_block_number)) },
                                                leadingIcon = { Icon(Icons.Outlined.Block, null) },
                                                onClick = {
                                                    showMenu = false
                                                    if (isNumberBlocked) {
                                                        BlockedNumbersManager.unblock(context, targetPhoneNumber)
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_unblocked_number, targetPhoneNumber), android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        BlockedNumbersManager.block(context, targetPhoneNumber)
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_blocked_number, targetPhoneNumber), android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.action_copy_number)) },
                                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                                onClick = {
                                                    showMenu = false
                                                    clipboardManager.setText(AnnotatedString(targetPhoneNumber))
                                                    android.widget.Toast.makeText(context, context.getString(R.string.number_copied_toast), android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            MenuTopAppBar(
                                text = stringResource(R.string.call_history_title),
                                navigator = navigator,
                                actions = {
                                    if (filteredLogsByContact.isNotEmpty()) {
                                        IconButton(onClick = { showDeleteAllDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteSweep,
                                                contentDescription = stringResource(R.string.content_desc_clear_history),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        val allSelected = filteredLogsByContact.isNotEmpty() && selectedEntries.size >= filteredLogsByContact.size
                        BatchCallLogActionBar(
                            selectedCount = selectedEntries.size,
                            onClearSelection = { selectedEntries = emptySet() },
                            onDelete = {
                                val allIdsToDelete = selectedEntries.flatMap { it.ids }
                                viewModel.deleteCallLogsByIds(allIdsToDelete)
                                selectedEntries = emptySet()
                            },
                            onSelectAll = if (filteredLogsByContact.isNotEmpty()) {
                                {
                                    selectedEntries = if (allSelected) emptySet() else filteredLogsByContact.toSet()
                                }
                            } else null,
                            allSelected = allSelected
                        )
                    }
                }
            },
            bottomBar = {
                if (isHistoryWithContact && selectedEntries.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Large green Call pill button
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .height(52.dp)
                                    .weight(1f)
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { handleCallNumber(targetPhoneNumber, matchedContact?.id ?: contactId) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = stringResource(R.string.action_call),
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_call),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Message action
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { messageLauncher.sendMessage(targetPhoneNumber, matchedContact) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Message,
                                            contentDescription = stringResource(R.string.swipe_action_message),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.swipe_action_message),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            if (showDeleteAllDialog) {
                RivoConfirmationDialog(
                    onDismissRequest = { showDeleteAllDialog = false },
                    onConfirm = {
                        val allIdsToDelete = filteredLogsByContact.flatMap { it.ids }
                        viewModel.deleteCallLogsByIds(allIdsToDelete)
                        showDeleteAllDialog = false
                    },
                    title = stringResource(R.string.call_log_delete_title),
                    message = stringResource(R.string.call_log_clear_contact_confirm),
                    confirmLabel = stringResource(R.string.action_delete),
                    dismissLabel = stringResource(R.string.action_cancel),
                    icon = Icons.Outlined.DeleteSweep,
                    isDestructive = true
                )
            }

            if (showAddContactSheet && targetPhoneNumber.isNotEmpty()) {
                AddToContactBottomSheet(
                    phoneNumber = targetPhoneNumber,
                    onDismissRequest = { showAddContactSheet = false },
                    onCreateNewContact = {
                        showAddContactSheet = false
                        navigator.navigate(ContactEditScreenDestination(initialPhone = targetPhoneNumber))
                    },
                    onAddToExistingContact = {
                        showAddContactSheet = false
                        navigator.navigate(
                            ContactSelectionScreenDestination(
                                title = context.getString(R.string.contact_add_to_existing),
                                initialPhoneToAssign = targetPhoneNumber
                            )
                        )
                    }
                )
            }

            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isHistoryWithContact) {
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
                        val finalLogs = remember(filteredLogsByContact, selectedFilter, isHistoryWithContact) {
                            if (isHistoryWithContact) {
                                filteredLogsByContact
                            } else {
                                when (selectedFilter) {
                                    CallLogFilter.All -> filteredLogsByContact
                                    CallLogFilter.Missed -> filteredLogsByContact.filter { it.type == CallLog.Calls.MISSED_TYPE }
                                    CallLogFilter.Incoming -> filteredLogsByContact.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                                    CallLogFilter.Outgoing -> filteredLogsByContact.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                                    CallLogFilter.Contacts -> filteredLogsByContact.filter { it.name != null && it.name != it.number }
                                }
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
                                if (!isHistoryWithContact) {
                                    item(key = "ad_header", contentType = "ad") {
                                        com.grinch.rivo4.view.components.ad.BannerAd()
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
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
                                                                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                                            } else Modifier
                                                        )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            RivoListItem(
                                                                headline = callTypeLabel,
                                                                headlineColor = if (isBlocked || isMissedOrRejected) MaterialTheme.colorScheme.error else Color.Unspecified,
                                                                supporting = if ((!callLogConfig.showSim || lgSimNumber == null) && !lg.isHd) supportingText else null,
                                                                supportingContent = if ((callLogConfig.showSim && lgSimNumber != null) || lg.isHd) {
                                                                    {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            modifier = Modifier.padding(top = 1.dp)
                                                                        ) {
                                                                            if (callLogConfig.showSim && lgSimNumber != null) {
                                                                                SimBadge(simNumber = lgSimNumber)
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                            }
                                                                            if (lg.isHd) {
                                                                                HdBadge()
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                            }
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
                                                                onClick = { handleCallNumber(lg.number, lg.contactId ?: contactId) },
                                                                modifier = Modifier.padding(end = 10.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Call,
                                                                    contentDescription = stringResource(R.string.action_call),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(22.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (!showCards && index < logsInGroup.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                    )
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
                                                        shape = shape,
                                                        onClick = {
                                                            if (selectedEntries.isNotEmpty()) {
                                                                selectedEntries = if (selectedEntries.any { it.id == lg.id }) {
                                                                    selectedEntries.filter { it.id != lg.id }.toSet()
                                                                } else {
                                                                    selectedEntries + lg
                                                                }
                                                            } else {
                                                                handleCallNumber(lg.number, lg.contactId)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (selectedEntries.none { it.id == lg.id }) {
                                                                selectedEntries = selectedEntries + lg
                                                            }
                                                        },
                                                        onCallClick = { handleCallNumber(lg.number, lg.contactId) },
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
