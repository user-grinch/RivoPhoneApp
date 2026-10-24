package com.grinch.rivo4.view.screen

import android.Manifest
import android.provider.CallLog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.CallLogTileSimple
import com.grinch.rivo4.view.components.LocalRivoAvatarStyle
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.PermissionDeniedView
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.grinch.rivo4.view.components.rememberCallLauncher
import com.grinch.rivo4.view.components.rememberRivoAvatarStyle
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.ramcosta.composedestinations.generated.destinations.SupportDevelopmentScreenDestination
import com.grinch.rivo4.view.components.rivoGroupedItemShape
import com.grinch.rivo4.view.components.rivoSurfaceStyle
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.view.components.RivoSwipeToActionBox
import com.grinch.rivo4.view.components.rememberMessageLauncher
import com.grinch.rivo4.view.components.rememberVideoLauncher
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>
@Composable
fun SearchScreen(
    navController: NavController,
    navigator: DestinationsNavigator
) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    val avatarStyle = rememberRivoAvatarStyle()

    CompositionLocalProvider(LocalRivoAvatarStyle provides avatarStyle) {
        ContactSearchContent(
            navigator = navigator,
            isGranted = permState.status == PermissionStatus.Granted,
            onRequestPermission = { permState.launchPermissionRequest() },
            listState = listState,
            showScrollToTop = showButton,
            onScrollToTop = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        )
    }
}

private enum class SearchTabFilter {
    ALL, CONTACTS, FAVORITES, RECENTS, MISSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSearchContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showScrollToTop: Boolean,
    onScrollToTop: () -> Unit
) {
    if (!isGranted) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                MenuTopAppBar(
                    text = stringResource(R.string.search_contacts_permission_title),
                    navigator = navigator
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                PermissionDeniedView(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.search_contacts_permission_title),
                    description = stringResource(R.string.search_contacts_permission_description),
                    onGrantClick = onRequestPermission
                )
            }
        }
        return
    }

    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val callLogVM: CallLogViewModel = koinActivityViewModel()
    val contacts by contactsVM.allContacts.collectAsState()
    val callLogs by callLogVM.allCallLogs.collectAsState()
    val isContactsLoading by contactsVM.isLoading.collectAsState()
    val isCallLogsLoading by callLogVM.isLoading.collectAsState()

    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val callLauncher = rememberCallLauncher()
    val messageLauncher = rememberMessageLauncher()
    val videoLauncher = rememberVideoLauncher()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val swipeEnabled = remember(settingsState) { prefs.isSwipeActionsEnabled() }
    val swipeRightAction = remember(settingsState) {
        SwipeActionType.fromId(prefs.getSwipeRightAction())
    }
    val swipeLeftAction = remember(settingsState) {
        SwipeActionType.fromId(prefs.getSwipeLeftAction())
    }

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchTabFilter.ALL) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        contactsVM.fetchContacts()
        callLogVM.fetchLogs()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val favoritesCount = remember(contacts) { contacts.count { it.isFavorite } }

    val filteredContacts = remember(query, contacts, selectedFilter) {
        val baseContacts = if (selectedFilter == SearchTabFilter.FAVORITES) {
            contacts.asSequence().filter { it.isFavorite }
        } else {
            contacts.asSequence()
        }

        if (query.isBlank()) {
            if (selectedFilter == SearchTabFilter.CONTACTS || selectedFilter == SearchTabFilter.FAVORITES) {
                baseContacts.take(100).toList()
            } else {
                emptyList()
            }
        } else {
            val cleanQuery = query.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            baseContacts.filter {
                val matchesName = it.name.contains(query, ignoreCase = true)
                val matchesNickname = it.nickname?.contains(query, ignoreCase = true) ?: false
                val matchesNumber = cleanQuery.isNotEmpty() && it.phoneNumbers.any { number ->
                    number.filter { ch -> ch.isDigit() || ch == '+' || ch == '*' || ch == '#' }.contains(cleanQuery)
                }
                matchesName || matchesNickname || matchesNumber
            }.take(50).toList()
        }
    }

    val filteredCallLogs = remember(query, callLogs, selectedFilter) {
        val baseLogs = if (selectedFilter == SearchTabFilter.MISSED) {
            callLogs.asSequence().filter {
                it.type == CallLog.Calls.MISSED_TYPE || it.types.contains(CallLog.Calls.MISSED_TYPE)
            }
        } else {
            callLogs.asSequence()
        }

        if (query.isBlank()) {
            if (selectedFilter == SearchTabFilter.RECENTS || selectedFilter == SearchTabFilter.MISSED) {
                baseLogs.take(50).toList()
            } else {
                emptyList()
            }
        } else {
            val cleanQuery = query.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            baseLogs.filter { log ->
                val matchesName = log.name?.contains(query, ignoreCase = true) == true
                val matchesNumber = cleanQuery.isNotEmpty() &&
                    log.number.filter { ch -> ch.isDigit() || ch == '+' || ch == '*' || ch == '#' }.contains(cleanQuery)
                matchesName || matchesNumber
            }.take(30).toList()
        }
    }

    val showContacts = selectedFilter == SearchTabFilter.ALL ||
        selectedFilter == SearchTabFilter.CONTACTS ||
        selectedFilter == SearchTabFilter.FAVORITES
    val showRecents = selectedFilter == SearchTabFilter.ALL ||
        selectedFilter == SearchTabFilter.RECENTS ||
        selectedFilter == SearchTabFilter.MISSED
    val totalResultsCount = (if (showContacts) filteredContacts.size else 0) + (if (showRecents) filteredCallLogs.size else 0)
    val isFilterOrQueryActive = query.isNotBlank() || selectedFilter != SearchTabFilter.ALL

    val placeholderText = when (selectedFilter) {
        SearchTabFilter.CONTACTS -> "Search contacts…"
        SearchTabFilter.FAVORITES -> "Search favorites…"
        SearchTabFilter.RECENTS -> "Search call history…"
        SearchTabFilter.MISSED -> "Search missed calls…"
        SearchTabFilter.ALL -> stringResource(R.string.search_contacts_placeholder)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }
                            if (query.isNotEmpty() || selectedFilter != SearchTabFilter.ALL) {
                                Surface(
                                    onClick = {
                                        if (query.isNotEmpty()) {
                                            query = ""
                                        } else {
                                            selectedFilter = SearchTabFilter.ALL
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.action_clear),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = { navigator.navigateUp() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips when query or quick filter is active
                if (isFilterOrQueryActive) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == SearchTabFilter.ALL,
                                onClick = { selectedFilter = SearchTabFilter.ALL },
                                label = {
                                    val countSuffix = if (query.isNotBlank()) " (${filteredContacts.size + filteredCallLogs.size})" else ""
                                    Text(stringResource(R.string.loc_all_countsuffix))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SearchTabFilter.CONTACTS,
                                onClick = {
                                    selectedFilter = if (selectedFilter == SearchTabFilter.CONTACTS && query.isBlank()) {
                                        SearchTabFilter.ALL
                                    } else {
                                        SearchTabFilter.CONTACTS
                                    }
                                },
                                label = {
                                    val count = if (selectedFilter == SearchTabFilter.CONTACTS || query.isNotBlank()) filteredContacts.size else contacts.size
                                    Text(stringResource(R.string.loc_contacts_count))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SearchTabFilter.FAVORITES,
                                onClick = {
                                    selectedFilter = if (selectedFilter == SearchTabFilter.FAVORITES && query.isBlank()) {
                                        SearchTabFilter.ALL
                                    } else {
                                        SearchTabFilter.FAVORITES
                                    }
                                },
                                label = {
                                    val count = if (selectedFilter == SearchTabFilter.FAVORITES) filteredContacts.size else favoritesCount
                                    Text(stringResource(R.string.loc_favorites_count))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SearchTabFilter.RECENTS,
                                onClick = {
                                    selectedFilter = if (selectedFilter == SearchTabFilter.RECENTS && query.isBlank()) {
                                        SearchTabFilter.ALL
                                    } else {
                                        SearchTabFilter.RECENTS
                                    }
                                },
                                label = {
                                    val count = if (selectedFilter == SearchTabFilter.RECENTS || query.isNotBlank()) filteredCallLogs.size else callLogs.size
                                    Text(stringResource(R.string.loc_call_logs_count))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SearchTabFilter.MISSED,
                                onClick = {
                                    selectedFilter = if (selectedFilter == SearchTabFilter.MISSED && query.isBlank()) {
                                        SearchTabFilter.ALL
                                    } else {
                                        SearchTabFilter.MISSED
                                    }
                                },
                                label = {
                                    val count = if (selectedFilter == SearchTabFilter.MISSED) filteredCallLogs.size else callLogs.count {
                                        it.type == CallLog.Calls.MISSED_TYPE || it.types.contains(CallLog.Calls.MISSED_TYPE)
                                    }
                                    Text(stringResource(R.string.loc_missed_count))
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.CallMissed, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            !isFilterOrQueryActive -> 1
                            (isContactsLoading && contacts.isEmpty()) && (isCallLogsLoading && callLogs.isEmpty()) -> 0
                            totalResultsCount == 0 -> 2
                            else -> 3
                        },
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "SearchContentState"
                    ) { state ->
                        when (state) {
                            0 -> RivoLoadingIndicatorView()
                            1 -> {
                                // MD3 Expressive Welcome Screen
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        RivoExpressiveCard {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Surface(
                                                    shape = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.25f },
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(56.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Search,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(R.string.search_contacts_title),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = stringResource(R.string.search_type_to_start),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Text(
                                            text = stringResource(R.string.search_quick_filters),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                        )
                                    }

                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    selectedFilter = SearchTabFilter.CONTACTS
                                                    focusRequester.requestFocus()
                                                    keyboardController?.show()
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Contacts,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = stringResource(R.string.search_contacts_action),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = {
                                                    selectedFilter = SearchTabFilter.RECENTS
                                                    focusRequester.requestFocus()
                                                    keyboardController?.show()
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Call,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = stringResource(R.string.search_call_history_action),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // MD3 Expressive No Results State
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RivoExpressiveCard {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                shape = rememberRivoMorphShape(RivoMaterialShapes.Cookie9Sided, RivoMaterialShapes.Circle) { 0.25f },
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.SearchOff,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.search_no_results_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = stringResource(R.string.search_no_results_hint),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            3 -> {
                                val surfaceStyle = rivoSurfaceStyle()
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    item {
                                        com.grinch.rivo4.view.components.ad.BannerAd()
                                    }

                                    if (showContacts && filteredContacts.isNotEmpty()) {
                                        item {
                                            val headerTitle = if (selectedFilter == SearchTabFilter.FAVORITES) {
                                                "Favorites (${filteredContacts.size})"
                                            } else {
                                                "${stringResource(R.string.nav_contacts)} (${filteredContacts.size})"
                                            }
                                            RivoSectionHeader(
                                                title = headerTitle,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            )
                                        }

                                        val showCards = surfaceStyle.showCards
                                        itemsIndexed(
                                            filteredContacts,
                                            key = { _, c -> "contact_${c.id}" },
                                            contentType = { _, _ -> "contact" }
                                        ) { index, contact ->
                                            val shape = rivoGroupedItemShape(index, filteredContacts.size)
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = if (showCards) shape else androidx.compose.ui.graphics.RectangleShape,
                                                    color = if (showCards) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
                                                ) {
                                                    RivoSwipeToActionBox(
                                                        enabled = swipeEnabled,
                                                        swipeRightAction = swipeRightAction,
                                                        swipeLeftAction = swipeLeftAction,
                                                        onTriggerAction = { action ->
                                                            val phone = contact.phoneNumbers.firstOrNull().orEmpty()
                                                            when (action) {
                                                                SwipeActionType.CALL -> callLauncher.dial(phone, contact)
                                                                SwipeActionType.MESSAGE -> messageLauncher.sendMessage(phone, contact)
                                                                SwipeActionType.VIDEO_CALL -> videoLauncher.startVideoCall(phone, contact)
                                                                SwipeActionType.WHATSAPP -> SocialUtils.openWhatsApp(context, phone)
                                                                SwipeActionType.COPY_NUMBER -> {
                                                                    if (phone.isNotBlank()) {
                                                                        clipboardManager.setText(AnnotatedString(phone))
                                                                        Toast.makeText(context, context.getString(R.string.number_copied_toast), Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                                SwipeActionType.VIEW_CONTACT -> {
                                                                    navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                                                }
                                                                SwipeActionType.SHARE -> {
                                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                        type = "text/plain"
                                                                        putExtra(android.content.Intent.EXTRA_TEXT, "${contact.name}: $phone")
                                                                    }
                                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                                                }
                                                                SwipeActionType.BLOCK -> {
                                                                    if (phone.isNotBlank()) {
                                                                        com.grinch.rivo4.controller.util.BlockedNumbersManager.block(context, phone)
                                                                        Toast.makeText(context, context.getString(R.string.toast_blocked_number, phone), Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                                SwipeActionType.DELETE -> {}
                                                                SwipeActionType.NONE -> {}
                                                            }
                                                        }
                                                    ) {
                                                        RivoListItem(
                                                            headline = contact.name,
                                                            supporting = buildString {
                                                                contact.nickname?.let { append("$it • ") }
                                                                contact.phoneNumbers.firstOrNull()?.let { append(formatPhoneNumber(it)) }
                                                            }.ifEmpty { null },
                                                            avatarName = contact.name,
                                                            photoUri = contact.photoUri,
                                                            onClick = {
                                                                navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                                            },
                                                            trailingContent = {
                                                                contact.phoneNumbers.firstOrNull()?.let { num ->
                                                                    IconButton(
                                                                        onClick = { callLauncher.dial(num, contact) }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Rounded.Call,
                                                                            contentDescription = stringResource(R.string.action_call),
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                                if (!showCards && index < filteredContacts.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (showRecents && filteredCallLogs.isNotEmpty()) {
                                        item {
                                            val headerTitle = if (selectedFilter == SearchTabFilter.MISSED) {
                                                "Missed Calls (${filteredCallLogs.size})"
                                            } else {
                                                "${stringResource(R.string.nav_recents)} (${filteredCallLogs.size})"
                                            }
                                            RivoSectionHeader(
                                                title = headerTitle,
                                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            )
                                        }

                                        val showCards = surfaceStyle.showCards
                                        itemsIndexed(
                                            filteredCallLogs,
                                            key = { _, lg -> "call_${lg.id}_${lg.date}" },
                                            contentType = { _, _ -> "call_log" }
                                        ) { index, lg ->
                                            val shape = rivoGroupedItemShape(index, filteredCallLogs.size)
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = if (showCards) shape else androidx.compose.ui.graphics.RectangleShape,
                                                    color = if (showCards) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
                                                ) {
                                                    RivoSwipeToActionBox(
                                                        enabled = swipeEnabled,
                                                        swipeRightAction = swipeRightAction,
                                                        swipeLeftAction = swipeLeftAction,
                                                        onTriggerAction = { action ->
                                                            val phone = lg.number
                                                            when (action) {
                                                                SwipeActionType.CALL -> callLauncher.dial(phone, null)
                                                                SwipeActionType.MESSAGE -> messageLauncher.sendMessage(phone, null)
                                                                SwipeActionType.VIDEO_CALL -> videoLauncher.startVideoCall(phone, null)
                                                                SwipeActionType.WHATSAPP -> SocialUtils.openWhatsApp(context, phone)
                                                                SwipeActionType.COPY_NUMBER -> {
                                                                    if (phone.isNotBlank()) {
                                                                        clipboardManager.setText(AnnotatedString(phone))
                                                                        Toast.makeText(context, context.getString(R.string.number_copied_toast), Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                                SwipeActionType.VIEW_CONTACT -> {
                                                                    navigator.navigate(
                                                                        ContactDetailsScreenDestination(
                                                                            contactId = lg.contactId,
                                                                            phoneNumber = lg.number
                                                                        )
                                                                    )
                                                                }
                                                                SwipeActionType.SHARE -> {
                                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                        type = "text/plain"
                                                                        putExtra(android.content.Intent.EXTRA_TEXT, "${lg.name ?: lg.number}: $phone")
                                                                    }
                                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                                                }
                                                                SwipeActionType.BLOCK -> {
                                                                    if (phone.isNotBlank()) {
                                                                        com.grinch.rivo4.controller.util.BlockedNumbersManager.block(context, phone)
                                                                        Toast.makeText(context, context.getString(R.string.toast_blocked_number, phone), Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                                SwipeActionType.DELETE -> {}
                                                                SwipeActionType.NONE -> {}
                                                            }
                                                        }
                                                    ) {
                                                        CallLogTileSimple(
                                                            log = lg,
                                                            onClick = {
                                                                navigator.navigate(
                                                                    ContactDetailsScreenDestination(
                                                                        contactId = lg.contactId,
                                                                        phoneNumber = lg.number
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                                if (!showCards && index < filteredCallLogs.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ScrollToTopButton(
                visible = showScrollToTop,
                onClick = onScrollToTop
            )
        }
    }
}
