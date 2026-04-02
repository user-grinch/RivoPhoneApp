package com.grinch.rivo4.view.screen

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.QrCodeUtils
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CallLogFullScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactDetailsScreen(
    contactId: String? = null,
    phoneNumber: String? = null,
    navigator: DestinationsNavigator
) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    
    val contacts by contactsViewModel.allContacts.collectAsState()
    val allLogs by callLogViewModel.allCallLogs.collectAsState()
    
    val contact = remember(contactId, phoneNumber, contacts) {
        if (contactId != null && contactId != "null") {
            contacts.find { it.id == contactId }
        } else if (phoneNumber != null) {
            contacts.find { it.phoneNumbers.any { num -> num.replace(" ", "").contains(phoneNumber.replace(" ", "")) } }
        } else null
    }
    
    val displayPhone = phoneNumber ?: contact?.phoneNumbers?.firstOrNull() ?: "Unknown"
    val displayName = contact?.name ?: phoneNumber ?: "Unknown"

    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    
    var showSimPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    val contactLogs = remember(contact, phoneNumber, allLogs) {
        allLogs.filter { log ->
            (contact != null && (log.contactId == contact.id || contact.phoneNumbers.any { num -> log.number.replace(" ", "").contains(num.replace(" ", "")) })) ||
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }
    
    val isFavorite = contact?.isFavorite ?: false
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
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

    if (showQrDialog) {
        QrCodeDialog(
            name = displayName,
            phone = displayPhone,
            email = contact?.emails?.firstOrNull(),
            onDismiss = { showQrDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = "QR Code")
                    }
                    if (contact != null) {
                        IconButton(onClick = { contactsViewModel.toggleFavorite(contact) }) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id.toLong())
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else if (phoneNumber != null && phoneNumber != "Unknown") {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RivoAvatar(
                            name = displayName,
                            photoUri = contact?.photoUri,
                            modifier = Modifier.size(140.dp),
                            shape = CircleShape
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RivoExpressiveButton(
                            icon = Icons.Default.Call,
                            label = "Call",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                if (displayPhone != "Unknown") {
                                    val accounts = telecomManager.callCapablePhoneAccounts
                                    if (accounts.size > 1) {
                                        pendingNumber = displayPhone
                                        showSimPicker = true
                                    } else {
                                        makeCall(context, displayPhone)
                                    }
                                }
                            }
                        )
                        RivoExpressiveButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "Text",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = {
                                if (displayPhone != "Unknown") {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$displayPhone"))
                                    context.startActivity(intent)
                                }
                            }
                        )
                        RivoExpressiveButton(
                            icon = Icons.Default.VideoCall,
                            label = "Video",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { }
                        )
                    }
                }

                item {
                    RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                        if (contact != null) {
                            contact.phoneNumbers.forEachIndexed { index, number ->
                                RivoListItem(
                                    headline = number,
                                    supporting = "Mobile",
                                    leadingIcon = Icons.Default.Phone,
                                    onClick = {
                                        val accounts = telecomManager.callCapablePhoneAccounts
                                        if (accounts.size > 1) {
                                            pendingNumber = number
                                            showSimPicker = true
                                        } else {
                                            makeCall(context, number)
                                        }
                                    }
                                )
                                if (index < contact.phoneNumbers.size - 1 || contact.emails.isNotEmpty()) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            contact.emails.forEachIndexed { index, email ->
                                RivoListItem(
                                    headline = email,
                                    supporting = "Email",
                                    leadingIcon = Icons.Default.Email,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                        context.startActivity(intent)
                                    }
                                )
                                if (index < contact.emails.size - 1) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        } else if (phoneNumber != null && phoneNumber != "Unknown") {
                            RivoListItem(
                                headline = phoneNumber,
                                supporting = "Unknown Number",
                                leadingIcon = Icons.Default.Phone,
                                onClick = @androidx.annotation.RequiresPermission(android.Manifest.permission.READ_PHONE_STATE) {
                                    val accounts = telecomManager.callCapablePhoneAccounts
                                    if (accounts.size > 1) {
                                        pendingNumber = phoneNumber
                                        showSimPicker = true
                                    } else {
                                        makeCall(context, phoneNumber)
                                    }
                                }
                            )
                        }
                    }
                }

                if (contact != null && (contact.events.isNotEmpty() || contact.addresses.isNotEmpty())) {
                    item {
                        RivoExpressiveCard(title = "Events & More", icon = Icons.Default.Event) {
                            contact.events.forEachIndexed { index, event ->
                                val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                RivoListItem(
                                    headline = event.date,
                                    supporting = event.label ?: if (isBirthday) "Birthday" else "Event",
                                    leadingIcon = if (isBirthday) Icons.Outlined.Cake else Icons.Outlined.Event,
                                    onClick = { }
                                )
                                if (index < contact.events.size - 1 || contact.addresses.isNotEmpty()) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            contact.addresses.forEachIndexed { index, address ->
                                RivoListItem(
                                    headline = address,
                                    supporting = "Address",
                                    leadingIcon = Icons.Default.LocationOn,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address"))
                                        context.startActivity(intent)
                                    }
                                )
                                if (index < contact.addresses.size - 1) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                if (contactLogs.isNotEmpty()) {
                    item {
                        RivoExpressiveCard(title = "Recent Activity", icon = Icons.Default.History) {
                            Column(modifier = Modifier.animateContentSize()) {
                                contactLogs.take(3).forEachIndexed { index, log ->
                                    CallLogTileSimple(log)
                                    if (index < 2 && index < contactLogs.size - 1) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                
                                if (contactLogs.size > 3) {
                                    TextButton(
                                        onClick = { 
                                            navigator.navigate(CallLogFullScreenDestination(
                                                contactId = contactId,
                                                phoneNumber = phoneNumber
                                            ))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Show full history")
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
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

@Composable
fun QrCodeDialog(
    name: String,
    phone: String?,
    email: String?,
    onDismiss: () -> Unit
) {
    val vCard = remember(name, phone, email) { QrCodeUtils.generateVCard(name, phone, email) }
    val qrBitmap = remember(vCard) { QrCodeUtils.generateQrCode(vCard, 600) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Contact QR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    phone ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
