package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.RingtoneManager
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.QrCodeUtils
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.modal.data.Contact
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
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    
    val allLogs by callLogViewModel.allCallLogs.collectAsState()
    
    var fullContact by remember { mutableStateOf<Contact?>(null) }
    var isFullLoading by remember { mutableStateOf(true) }

    LaunchedEffect(contactId, phoneNumber) {
        isFullLoading = true
        fullContact = if (contactId != null && contactId != "null") {
            contactsViewModel.getFullContactById(contactId)
        } else if (phoneNumber != null) {
            contactsViewModel.getFullContactByNumber(phoneNumber)
        } else null
        isFullLoading = false
    }
    
    val displayPhone = phoneNumber ?: fullContact?.phoneNumbers?.firstOrNull() ?: "Unknown"
    val displayName = fullContact?.name ?: phoneNumber ?: "Unknown"

    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    
    var showSimPicker by remember { mutableStateOf(false) }
    var showNumberPicker by remember { mutableStateOf(false) }
    var isSmsAction by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    val contactLogs = remember(fullContact, phoneNumber, allLogs) {
        allLogs.filter { log ->
            (fullContact != null && (log.contactId == fullContact!!.id || fullContact!!.phoneNumbers.any { num -> log.number.replace(" ", "").contains(num.replace(" ", "")) })) ||
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }
    
    val isFavorite = fullContact?.isFavorite ?: false
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (fullContact != null) {
                contactsViewModel.setCustomRingtone(fullContact!!.id, uri?.toString())
                // Optimistically update UI
                fullContact = fullContact!!.copy(customRingtone = uri?.toString())
            }
        }
    }

    val initiateCall = { number: String ->
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val accounts = try { telecomManager.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }
            if (accounts.size > 1 && prefs.getInt("default_sim", 0) == 0) {
                pendingNumber = number
                isSmsAction = false
                showSimPicker = true
            } else {
                makeCall(context, number)
            }
        } else {
            makeCall(context, number)
        }
    }

    val initiateSms = { number: String ->
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val accounts = try { telecomManager.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }
            if (accounts.size > 1) {
                pendingNumber = number
                isSmsAction = true
                showSimPicker = true
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))
                context.startActivity(intent)
            }
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))
            context.startActivity(intent)
        }
    }

    val shareContact = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Name: $displayName\nPhone: $displayPhone")
        }
        context.startActivity(Intent.createChooser(intent, "Share Contact"))
    }

    if (showNumberPicker && fullContact != null) {
        RivoDialog(
            onDismissRequest = { showNumberPicker = false },
            title = "Select Number",
            icon = Icons.Default.Phone,
            dismissButton = {
                TextButton(onClick = { showNumberPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            fullContact!!.phoneNumbers.forEach { selectedNumber ->
                Surface(
                    onClick = {
                        showNumberPicker = false
                        if (isSmsAction) initiateSms(selectedNumber) else initiateCall(selectedNumber)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(selectedNumber, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showSimPicker && pendingNumber != null) {
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                if (isSmsAction) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${pendingNumber!!}"))
                    intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", handle)
                    context.startActivity(intent)
                } else {
                    makeCall(context, pendingNumber!!, handle)
                }
                showSimPicker = false
            }
        )
    }

    if (showQrDialog) {
        RivoDialog(
            onDismissRequest = { showQrDialog = false },
            title = "Contact QR",
            icon = Icons.Default.QrCode,
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        ) {
            val vCard = remember(displayName, displayPhone, fullContact?.emails?.firstOrNull()) { 
                QrCodeUtils.generateVCard(displayName, displayPhone, fullContact?.emails?.firstOrNull()) 
            }
            val qrBitmap = remember(vCard) { QrCodeUtils.generateQrCode(vCard, 600) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    displayPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                    IconButton(onClick = shareContact) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    if (fullContact != null) {
                        IconButton(onClick = { 
                            fullContact?.let { contact ->
                                val newFavorite = !contact.isFavorite
                                fullContact = contact.copy(isFavorite = newFavorite)
                                contactsViewModel.toggleFavorite(contact)
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, fullContact!!.id.toLong())
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
            if (isFullLoading) {
                RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
            } else {
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
                                photoUri = fullContact?.photoUri,
                                modifier = Modifier.size(140.dp),
                                shape = CircleShape,
                                textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
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
                                    if (fullContact != null && fullContact!!.phoneNumbers.size > 1) {
                                        showNumberPicker = true
                                    } else if (displayPhone != "Unknown") {
                                        initiateCall(displayPhone)
                                    }
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                label = "Text",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {
                                    if (fullContact != null && fullContact!!.phoneNumbers.size > 1) {
                                        isSmsAction = true
                                        showNumberPicker = true
                                    } else if (displayPhone != "Unknown") {
                                        initiateSms(displayPhone)
                                    }
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.Default.VideoCall,
                                label = "Video",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = {
                                    if (displayPhone != "Unknown") {
                                        val uri = Uri.parse("tel:$displayPhone")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            // Attempt to use common video calling schemes
                                            setDataAndType(uri, "vnd.android.cursor.item/video-chat-address")
                                        }
                                        
                                        // Fallback to a chooser for common apps like Google Meet/Duo, WhatsApp, etc.
                                        val chooser = Intent.createChooser(intent, "Video Call with")
                                        context.startActivity(chooser)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                            if (fullContact != null) {
                                fullContact!!.phoneNumbers.forEachIndexed { index, number ->
                                    RivoListItem(
                                        headline = number,
                                        supporting = "Mobile",
                                        leadingIcon = Icons.Default.Phone,
                                        onClick = { initiateCall(number) }
                                    )
                                    if (index < fullContact!!.phoneNumbers.size - 1 || fullContact!!.emails.isNotEmpty()) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                fullContact!!.emails.forEachIndexed { index, email ->
                                    RivoListItem(
                                        headline = email,
                                        supporting = "Email",
                                        leadingIcon = Icons.Default.Email,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                            context.startActivity(intent)
                                        }
                                    )
                                    if (index < fullContact!!.emails.size - 1) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                
                                if (fullContact != null) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    val currentRingtone = fullContact!!.customRingtone?.let { 
                                        RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) ?: "Custom"
                                    } ?: "Default"
                                    
                                    RivoListItem(
                                        headline = "Custom Ringtone",
                                        supporting = currentRingtone,
                                        leadingIcon = Icons.Default.MusicNote,
                                        onClick = {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, fullContact!!.customRingtone?.let { Uri.parse(it) })
                                            }
                                            ringtonePickerLauncher.launch(intent)
                                        }
                                    )
                                }
                            } else if (phoneNumber != null && phoneNumber != "Unknown") {
                                RivoListItem(
                                    headline = phoneNumber,
                                    supporting = "Unknown Number",
                                    leadingIcon = Icons.Default.Phone,
                                    onClick = { initiateCall(phoneNumber) }
                                )
                            }
                        }
                    }

                    if (fullContact != null && (fullContact!!.events.isNotEmpty() || fullContact!!.addresses.isNotEmpty())) {
                        item {
                            RivoExpressiveCard(title = "Events & More", icon = Icons.Default.Event) {
                                fullContact!!.events.forEachIndexed { index, event ->
                                    val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                    RivoListItem(
                                        headline = event.date,
                                        supporting = event.label ?: if (isBirthday) "Birthday" else "Event",
                                        leadingIcon = if (isBirthday) Icons.Outlined.Cake else Icons.Outlined.Event,
                                        onClick = { }
                                    )
                                    if (index < fullContact!!.events.size - 1 || fullContact!!.addresses.isNotEmpty()) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                fullContact!!.addresses.forEachIndexed { index, address ->
                                    RivoListItem(
                                        headline = address,
                                        supporting = "Address",
                                        leadingIcon = Icons.Default.LocationOn,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address"))
                                            context.startActivity(intent)
                                        }
                                    )
                                    if (index < fullContact!!.addresses.size - 1) {
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
                                        CallLogTileSimple(
                                            log = log,
                                            onCallClick = {
                                                initiateCall(log.number)
                                            }
                                        )
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
