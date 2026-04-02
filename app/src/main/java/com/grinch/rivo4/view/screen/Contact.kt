package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.view.components.AZListScroll
import com.grinch.rivo4.view.components.BottomBar
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.grinch.rivo4.view.components.TopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(
    start = true
)
@Composable
fun ContactScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(navController, navigator)
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                        context.startActivity(intent)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, "Add Contact")
                }
            }
        },
        bottomBar = {
            BottomBar(navController, navigator)
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
fun ContactContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isGranted) {
            val contactsVM: ContactsViewModel = koinActivityViewModel()
            val contacts = contactsVM.allContacts.collectAsState().value

            if (contacts.isEmpty()) {
                RivoLoadingIndicatorView()
            } else {
                AZListScroll(contacts, navigator, listState = listState)
            }

        } else {
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
                    text = "To show your contact list and identify incoming calls, Rivo needs permission to access your contacts.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Grant permission")
                }
            }
        }
    }
}
