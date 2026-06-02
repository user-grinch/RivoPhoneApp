package com.grinch.rivo4.view.screen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.*
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
            title = "Contacts permission required",
            description = "To search your contacts and identify incoming calls, Rivo needs access to your contacts.",
            onGrantClick = onRequestPermission
        )
        return
    }

    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val contacts by contactsVM.allContacts.collectAsState()
    val prefs = koinInject<PreferenceManager>()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsState by prefs.settingsChanged.collectAsState()
    val searchMatchMode by remember(settingsState) {
        mutableStateOf(prefs.getInt(PreferenceManager.KEY_SEARCH_MATCH_MODE, 0))
    }

    var query by remember { mutableStateOf("") }
    
    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val filteredContacts = remember(query, contacts, searchMatchMode) {
        if (query.isBlank()) emptyList()
        else contacts.filter {
            val cleanQuery = query.replace(" ", "")
            val matchesName = when (searchMatchMode) {
                1 -> it.name.startsWith(query, ignoreCase = true)
                2 -> it.name.equals(query, ignoreCase = true)
                else -> it.name.contains(query, ignoreCase = true)
            }
            val matchesNumber = when (searchMatchMode) {
                1 -> it.phoneNumbers.any { number -> number.replace(" ", "").startsWith(cleanQuery) }
                2 -> it.phoneNumbers.any { number -> number.replace(" ", "") == cleanQuery }
                else -> it.phoneNumbers.any { number -> number.replace(" ", "").contains(cleanQuery) }
            }
            matchesName || matchesNumber
        }.take(50)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 0.dp
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Search contacts") },
                leadingIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
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
                    contacts.isEmpty() -> 0
                    query.isBlank() -> 1
                    filteredContacts.isEmpty() -> 2
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
                                shape = RoundedCornerShape(32.dp),
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
                                "Search Contacts",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Type a name or number to start searching",
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
                                    "No results found",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Try a different name or number",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    3 -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                RivoSectionHeader(title = "Search Results", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                            
                            itemsIndexed(filteredContacts) { index, contact ->
                                val isFirst = index == 0
                                val isLast = index == filteredContacts.size - 1
                                
                                Surface(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    shape = when {
                                        isFirst && isLast -> RoundedCornerShape(28.dp)
                                        isFirst -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                        isLast -> RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                                        else -> androidx.compose.ui.graphics.RectangleShape
                                    },
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            top = if (isFirst) 8.dp else 0.dp,
                                            bottom = if (isLast) 8.dp else 0.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                RivoListItem(
                                                    headline = contact.name,
                                                    supporting = contact.phoneNumbers.firstOrNull()?.let { formatPhoneNumber(it) },
                                                    avatarName = contact.name,
                                                    photoUri = contact.photoUri,
                                                    onClick = {
                                                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                                    }
                                                )
                                            }
                                            contact.phoneNumbers.firstOrNull()?.let { num ->
                                                IconButton(
                                                    onClick = { makeCall(context, num, contactId = contact.id) },
                                                    modifier = Modifier.padding(end = 8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Call,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        if (!isLast) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
}
