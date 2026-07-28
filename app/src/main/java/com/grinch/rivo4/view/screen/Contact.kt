package com.grinch.rivo4.view.screen

import android.Manifest
import android.accounts.Account
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.view.components.AZListScroll
import com.grinch.rivo4.view.components.BottomBar
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoFilterChip
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.grinch.rivo4.view.components.RivoPullToRefreshIndicator
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = NoTransitions::class)
@Composable
fun ContactScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val contactsVM: ContactsViewModel = koinActivityViewModel()

    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                AnimatedContent(
                    targetState = selectedIds.isNotEmpty(),
                    transitionSpec = {
                        (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                    },
                    label = "TopBarTransition"
                ) { isSelecting ->
                    if (!isSelecting) {
                        Column {
                            TopBar(navController, navigator)
                            AccountFilterBar(contactsVM)
                        }
                    } else {
                        BatchActionBar(
                            selectedCount = selectedIds.size,
                            onClear = { selectedIds = emptySet() },
                            onDelete = {
                                contactsVM.deleteContacts(selectedIds.toList())
                                selectedIds = emptySet()
                            },
                            onMove = { account ->
                                contactsVM.moveContacts(selectedIds.toList(), account)
                                selectedIds = emptySet()
                            },
                            onMoveToPrivate = {
                                selectedIds.forEach { contactsVM.makeContactPrivate(it) }
                                selectedIds = emptySet()
                            },
                            availableAccounts = contactsVM.availableAccounts.collectAsState().value
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedIds.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        navigator.navigate(ContactEditScreenDestination())
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, stringResource(R.string.action_add_contact))
                }
            }
        },
        bottomBar = {
            if (selectedIds.isEmpty()) {
                BottomBar(navController, navigator)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box (
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            ContactContent(
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState,
                selectedIds = selectedIds,
                onToggleSelection = { id ->
                    selectedIds = if (selectedIds.contains(id)) {
                        selectedIds - id
                    } else {
                        selectedIds + id
                    }
                }
            )

            ScrollToTopButton(
                visible = showButton && selectedIds.isEmpty(),
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
fun AccountFilterBar(viewModel: ContactsViewModel) {
    val accounts by viewModel.availableAccounts.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val showPrivateOnly by viewModel.showPrivateOnly.collectAsState()
    val showLocalOnly by viewModel.showLocalOnly.collectAsState()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            RivoFilterChip(stringResource(R.string.filter_all), selectedAccount == null && !showPrivateOnly && !showLocalOnly, {
                viewModel.selectAccount(null)
                viewModel.setShowPrivateOnly(false)
                viewModel.setShowLocalOnly(false)
            }, isAllFilter = true)
        }
        item {
            RivoFilterChip(stringResource(R.string.label_local_memory), showLocalOnly, {
                viewModel.setShowLocalOnly(true)
            })
        }
        item {
            RivoFilterChip(stringResource(R.string.contact_filter_private), showPrivateOnly, {
                viewModel.setShowPrivateOnly(true)
            })
        }
        items(accounts) { account ->
            RivoFilterChip(ContactUtils.getFriendlyAccountName(account), selectedAccount == account, {
                viewModel.selectAccount(account)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchActionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Account?) -> Unit,
    onMoveToPrivate: () -> Unit,
    availableAccounts: List<Account>
) {
    var showMoveDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, stringResource(R.string.action_clear_selection))
            }
            Text(
                text = stringResource(R.string.selection_count_selected, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = { showMoveDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.content_desc_move))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
            }
        }
    }

    if (showMoveDialog) {
        RivoDialog(
            onDismissRequest = { showMoveDialog = false },
            title = stringResource(R.string.contact_move_to_storage),
            icon = Icons.AutoMirrored.Filled.DriveFileMove,
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Surface(
                onClick = {
                    onMoveToPrivate()
                    showMoveDialog = false
                },
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.contact_move_private_storage_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.contact_move_private_storage_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    onMove(null)
                    showMoveDialog = false
                },
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.label_local_memory),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.contact_move_local_memory_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            availableAccounts.forEachIndexed { index, account ->
                Surface(
                    onClick = {
                        onMove(account)
                        showMoveDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(ContactUtils.getAccountIcon(account), null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                ContactUtils.getFriendlyAccountName(account),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                account.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit
) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val isLoading by contactsVM.isLoading.collectAsState()
    val contacts by contactsVM.filteredContacts.collectAsState()
    val groupedContacts by contactsVM.groupedContacts.collectAsState()

    LaunchedEffect(isGranted) {
        if (isGranted) {
            contactsVM.fetchContacts()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading && contacts.isNotEmpty(),
        onRefresh = { contactsVM.fetchContacts() },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            RivoPullToRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading && contacts.isNotEmpty()
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isGranted) {
                if (isLoading && contacts.isEmpty()) {
                    RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
                } else if (contacts.isEmpty()) {
                    EmptyContactsState()
                } else {
                    AZListScroll(
                        contacts = contacts,
                        navigator = navigator,
                        listState = listState,
                        selectedIds = selectedIds,
                        onToggleSelection = onToggleSelection,
                        grouped = groupedContacts
                    )
                }
            } else {
                PermissionRequiredState(onRequestPermission)
            }
        }
    }
}

@Composable
fun EmptyContactsState() {
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
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.contacts_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.empty_state_try_clearing_filters),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PermissionRequiredState(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(120.dp),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.contacts_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(stringResource(R.string.action_grant_permission_lower))
        }
    }
}