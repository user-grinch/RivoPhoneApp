package com.grinch.rivo4.view.screen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import com.grinch.rivo4.view.components.rivoGroupedItemShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.grinch.rivo4.view.components.PermissionDeniedView
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.grinch.rivo4.view.components.rememberCallLauncher
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.grinch.rivo4.view.components.LocalRivoAvatarStyle
import com.grinch.rivo4.view.components.rememberRivoAvatarStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ContactSearchContent(
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSearchContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (!isGranted) {
        PermissionDeniedView(
            icon = Icons.Default.Person,
            title = stringResource(R.string.search_contacts_permission_title),
            description = stringResource(R.string.search_contacts_permission_description),
            onGrantClick = onRequestPermission
        )
        return
    }

    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val callLogVM: CallLogViewModel = koinActivityViewModel()
    val contacts by contactsVM.allContacts.collectAsState()
    val callLogs by callLogVM.allCallLogs.collectAsState()
    val isContactsLoading by contactsVM.isLoading.collectAsState()
    val isCallLogsLoading by callLogVM.isLoading.collectAsState()

    val prefs = koinInject<PreferenceManager>()
    val callLauncher = rememberCallLauncher()
    val settingsState by prefs.settingsChanged.collectAsState()
    val roundness = remember(settingsState) { prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28).coerceAtLeast(1) }

    var query by remember { mutableStateOf("") }

    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        contactsVM.fetchContacts()
        callLogVM.fetchLogs()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val filteredContacts = remember(query, contacts) {
        if (query.isBlank()) emptyList()
        else {
            val cleanQuery = query.replace(" ", "")
            contacts.asSequence().filter {
                val matchesName = it.name.contains(query, ignoreCase = true)
                val matchesNickname = it.nickname?.contains(query, ignoreCase = true) ?: false
                val matchesNumber = it.phoneNumbers.any { number -> number.replace(" ", "").contains(cleanQuery) }
                matchesName || matchesNickname || matchesNumber
            }.take(50).toList()
        }
    }

    val filteredCallLogs = remember(query, callLogs) {
        if (query.isBlank()) emptyList()
        else {
            val cleanQuery = query.replace(" ", "")
            callLogs.asSequence().filter { log ->
                val matchesName = log.name?.contains(query, ignoreCase = true) == true
                val matchesNumber = log.number.replace(" ", "").contains(cleanQuery)
                matchesName || matchesNumber
            }.take(30).toList()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(roundness.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 0.dp
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_contacts_placeholder)) },
                leadingIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = when {
                    query.isBlank() -> 1
                    (isContactsLoading && contacts.isEmpty()) && (isCallLogsLoading && callLogs.isEmpty()) -> 0
                    filteredContacts.isEmpty() && filteredCallLogs.isEmpty() -> 2
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(roundness.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.size(120.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                stringResource(R.string.search_contacts_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.search_type_to_start),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    2 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(R.string.search_no_results_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.search_no_results_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    3 -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                RivoSectionHeader(
                                    title = stringResource(R.string.search_results_header),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }

                            item {
                                com.grinch.rivo4.view.components.ad.BannerAd()
                            }

                            if (filteredContacts.isNotEmpty()) {
                                item {
                                    RivoSectionHeader(
                                        title = stringResource(R.string.nav_contacts),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    )
                                }

                                itemsIndexed(filteredContacts, key = { _, c -> "contact_${c.id}" }) { index, contact ->
                                    val shape = rivoGroupedItemShape(index, filteredContacts.size)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = shape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                            }

                            if (filteredCallLogs.isNotEmpty()) {
                                item {
                                    RivoSectionHeader(
                                        title = stringResource(R.string.nav_recents),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    )
                                }

                                itemsIndexed(filteredCallLogs, key = { _, lg -> "call_${lg.id}_${lg.date}" }) { index, lg ->
                                    val shape = rivoGroupedItemShape(index, filteredCallLogs.size)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = shape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                                            },
                                            onCallClick = {
                                                callLauncher.dial(lg.number, null)
                                            }
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
