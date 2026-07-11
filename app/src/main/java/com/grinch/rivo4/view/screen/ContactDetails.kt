package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.media.RingtoneManager
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.*
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CallLogFullScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val clipboardManager = LocalClipboardManager.current

    val allLogs by callLogViewModel.allCallLogs.collectAsState()

    var fullContact by remember { mutableStateOf<Contact?>(null) }
    var isFullLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(contactId, phoneNumber) {
        isFullLoading = true
        fullContact = if (contactId != null && contactId != "null") {
            contactsViewModel.getFullContactById(contactId)
        } else if (phoneNumber != null) {
            contactsViewModel.getFullContactByNumber(phoneNumber)
        } else null
        isFullLoading = false
    }

    // Refresh contact data when returning to screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val updated = if (contactId != null && contactId != "null") {
                        contactsViewModel.getFullContactById(contactId)
                    } else if (phoneNumber != null) {
                        contactsViewModel.getFullContactByNumber(phoneNumber)
                    } else null
                    if (updated != null) fullContact = updated
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayPhone = phoneNumber ?: fullContact?.phoneNumbers?.firstOrNull() ?: "Unknown"
    val displayName = fullContact?.name ?: phoneNumber ?: "Unknown"

    val context = LocalContext.current
    val callLauncher = rememberCallLauncher()
    val messageLauncher = rememberMessageLauncher()
    val emailLauncher = rememberEmailLauncher()
    val videoLauncher = rememberVideoLauncher()

    var showQrDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNumberSelectionDialog by remember { mutableStateOf(false) }
    var pendingSocialAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var selectionTitle by remember { mutableStateOf("") }

    var favoriteNumber by remember { mutableStateOf<String?>(null) }
    var favoriteEmail by remember { mutableStateOf<String?>(null) }
    val contactsVM: ContactsViewModel = koinActivityViewModel()

    LaunchedEffect(fullContact) {
        fullContact?.id?.let {
            favoriteNumber = prefs.getFavoriteNumber(it)
            favoriteEmail = prefs.getFavoriteEmail(it)
        }
    }

    val contactLogs = remember(fullContact, phoneNumber, allLogs) {
        allLogs.filter { log ->
            (fullContact != null && (log.contactId == fullContact!!.id || fullContact!!.phoneNumbers.any { num -> areNumbersEqual(log.number, num) })) ||
                    (phoneNumber != null && areNumbersEqual(log.number, phoneNumber))
        }
    }

    val isFavorite = fullContact?.isFavorite ?: false
    val listState = rememberLazyListState()
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
                fullContact = fullContact!!.copy(customRingtone = uri?.toString())
            }
        }
    }

    val openWhatsApp = { num: String -> SocialUtils.openWhatsApp(context, num) }
    val openTelegram = { num: String -> SocialUtils.openTelegram(context, num) }
    val openSignal = { num: String -> SocialUtils.openSignal(context, num) }

    val shareContact = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Name: $displayName\nPhone: $displayPhone")
        }
        context.startActivity(Intent.createChooser(intent, "Share Contact"))
    }

    val onNumberActionClick = { action: (String) -> Unit, title: String ->
        if (fullContact != null && fullContact!!.phoneNumbers.size > 1) {
            selectionTitle = title
            pendingSocialAction = { action(it) }
            showNumberSelectionDialog = true
        } else {
            action(displayPhone)
        }
    }

    if (showDeleteDialog) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                if (contactId != null) {
                    contactsVM.deleteContact(contactId)
                    navigator.navigateUp()
                }
            },
            title = "Delete Contact?",
            message = "Are you sure you want to delete this contact? This action cannot be undone.",
            confirmLabel = "Delete",
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }

    if (showNumberSelectionDialog && fullContact != null) {
        RivoSelectionDialog(
            onDismissRequest = { showNumberSelectionDialog = false },
            title = selectionTitle,
            items = fullContact!!.phoneNumbers,
            itemLabel = { formatPhoneNumber(it) },
            onItemSelected = { pendingSocialAction?.invoke(it) },
            itemSupporting = { "Mobile" },
            icon = Icons.Default.Phone,
            itemIcon = { if (areNumbersEqual(favoriteNumber, it)) Icons.Default.Star else Icons.Default.Phone },
            isSelected = { areNumbersEqual(favoriteNumber, it) }
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
                    formatPhoneNumber(displayPhone),
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
                    if (fullContact != null) {
                        IconButton(onClick = {
                            fullContact?.let { contact ->
                                val newFavorite = !contact.isFavorite
                                fullContact = contact.copy(isFavorite = newFavorite)
                                contactsViewModel.toggleFavorite(contact)
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            fullContact?.let {
                                navigator.navigate(ContactEditScreenDestination(contactId = it.id))
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else if (phoneNumber != null && phoneNumber != "Unknown") {
                        IconButton(onClick = {
                            navigator.navigate(ContactEditScreenDestination(initialPhone = phoneNumber))
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
                                textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            fullContact?.nickname?.let { nickname ->
                                Text(
                                    text = nickname,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    callLauncher.dial(if (fullContact == null) displayPhone else "", fullContact)
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                label = "Message",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {
                                    messageLauncher.sendMessage(if (fullContact == null) displayPhone else "", fullContact)
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.Default.VideoCall,
                                label = "Video",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {
                                    videoLauncher.startVideoCall(displayPhone, fullContact)
                                }
                            )
                            val hasEmails = fullContact?.emails?.isNotEmpty() == true
                            RivoExpressiveButton(
                                icon = Icons.Default.Email,
                                label = "Email",
                                containerColor = if (hasEmails) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (hasEmails) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f),
                                onClick = {
                                    if (hasEmails) {
                                        emailLauncher.sendEmail("", fullContact)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        val lastUsed = fullContact?.id?.let { prefs.getLastUsedNumber(it) }
                        RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                            if (fullContact != null) {
                                val uniquePhoneNumbers = remember(fullContact) {
                                    deduplicateNumbers(fullContact!!.phoneNumbers)
                                }
                                uniquePhoneNumbers.forEachIndexed { index, number ->
                                    val isRecent = lastUsed != null && areNumbersEqual(lastUsed, number)
                                    val isFav = areNumbersEqual(favoriteNumber, number)
                                    
                                    var showMenu by remember { mutableStateOf(false) }
                                    
                                    Box {
                                        RivoListItem(
                                            headline = formatPhoneNumber(number),
                                            supporting = buildString {
                                                append("Mobile")
                                                if (isFav) append(" • Favorite")
                                                if (isRecent) append(" • Recent")
                                            },
                                            leadingIcon = Icons.Default.Phone,
                                            trailingIcon = if (isFav) Icons.Default.Star else if (isRecent) Icons.Default.History else null,
                                            onClick = { callLauncher.dial(number, fullContact) },
                                            onLongClick = { showMenu = true }
                                        )

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isFav) "Clear Favorite" else "Set as Favorite") },
                                                onClick = {
                                                    showMenu = false
                                                    if (isFav) {
                                                        prefs.setFavoriteNumber(fullContact!!.id, null)
                                                        prefs.setFavoriteSim(fullContact!!.id, null)
                                                        favoriteNumber = null
                                                    } else {
                                                        prefs.setFavoriteNumber(fullContact!!.id, number)
                                                        favoriteNumber = number
                                                    }
                                                },
                                                leadingIcon = { Icon(if (isFav) Icons.Default.StarOutline else Icons.Default.Star, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy to Clipboard") },
                                                onClick = {
                                                    showMenu = false
                                                    clipboardManager.setText(AnnotatedString(number))
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                            )
                                        }
                                    }
                                    if (index < fullContact!!.phoneNumbers.size - 1 || fullContact!!.emails.isNotEmpty()) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                                fullContact!!.emails.forEachIndexed { index, email ->
                                    val isFav = email == favoriteEmail
                                    var showMenu by remember { mutableStateOf(false) }

                                    Box {
                                        RivoListItem(
                                            headline = email,
                                            supporting = if (isFav) "Email • Favorite" else "Email",
                                            leadingIcon = Icons.Default.Email,
                                            onClick = { emailLauncher.sendEmail(email, fullContact) },
                                            onLongClick = { showMenu = true }
                                        )

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isFav) "Clear Default" else "Set as Default") },
                                                onClick = {
                                                    showMenu = false
                                                    if (isFav) {
                                                        prefs.setFavoriteEmail(fullContact!!.id, null)
                                                        favoriteEmail = null
                                                    } else {
                                                        prefs.setFavoriteEmail(fullContact!!.id, email)
                                                        favoriteEmail = email
                                                    }
                                                },
                                                leadingIcon = { Icon(if (isFav) Icons.Default.StarOutline else Icons.Default.Star, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy to Clipboard") },
                                                onClick = {
                                                    showMenu = false
                                                    clipboardManager.setText(AnnotatedString(email))
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                            )
                                        }
                                    }
                                    if (index < fullContact!!.emails.size - 1) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            } else if (phoneNumber != null && phoneNumber != "Unknown") {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    RivoListItem(
                                        headline = formatPhoneNumber(phoneNumber),
                                        supporting = "Unknown Number",
                                        leadingIcon = Icons.Default.Phone,
                                        onClick = { callLauncher.dial(phoneNumber, null) },
                                        onLongClick = { showMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Add to Contacts") },
                                            onClick = {
                                                showMenu = false
                                                navigator.navigate(
                                                    ContactEditScreenDestination(
                                                        initialPhone = phoneNumber
                                                    )
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Copy to Clipboard") },
                                            onClick = {
                                                showMenu = false
                                                clipboardManager.setText(AnnotatedString(phoneNumber))
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                        )
                                    }
                                }
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
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
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
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (fullContact?.notes?.isNotBlank() == true) {
                        item {
                            var showNotesMenu by remember { mutableStateOf(false) }
                            RivoExpressiveCard(title = "Notes", icon = Icons.AutoMirrored.Filled.Notes) {
                                Box {
                                    Text(
                                        text = fullContact!!.notes!!,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { },
                                                onLongClick = { showNotesMenu = true }
                                            )
                                            .padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    DropdownMenu(
                                        expanded = showNotesMenu,
                                        onDismissRequest = { showNotesMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Copy to Clipboard") },
                                            onClick = {
                                                showNotesMenu = false
                                                clipboardManager.setText(AnnotatedString(fullContact!!.notes!!))
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                        )
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
                                                callLauncher.dial(log.number, fullContact)
                                            }
                                        )
                                        if (index < 2 && index < contactLogs.size - 1) {
                                            RivoDivider(Modifier.padding(horizontal = 16.dp))
                                        }
                                    }

                                    if (contactLogs.size > 3) {
                                        val finalContactId = if (fullContact?.id != null) fullContact!!.id else if (contactId != "null") contactId else null
                                        TextButton(
                                            onClick = {
                                                navigator.navigate(CallLogFullScreenDestination(
                                                    contactId = finalContactId,
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

                    item {
                        RivoExpressiveCard(title = "Social Apps", icon = Icons.AutoMirrored.Filled.Chat) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/whatsapp.png"),
                                    label = "WhatsApp",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openWhatsApp, "WhatsApp") }
                                )
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/telegram.png"),
                                    label = "Telegram",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openTelegram, "Telegram") }
                                )
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/signal.png"),
                                    label = "Signal",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openSignal, "Signal") }
                                )
                            }
                        }
                    }

                    if (fullContact != null) {
                        item {
                            RivoExpressiveCard(title = "Contact Settings", icon = Icons.Default.Settings) {
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
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = "Share Contact",
                                    supporting = "Send contact details to others",
                                    leadingIcon = Icons.Default.Share,
                                    onClick = shareContact
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = "QR Code",
                                    supporting = "Show contact QR code",
                                    leadingIcon = Icons.Outlined.QrCode2,
                                    onClick = { showQrDialog = true }
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = if (fullContact!!.isPrivate) "Move to Public Storage" else "Move to Private Storage",
                                    supporting = if (fullContact!!.isPrivate) "Make visible to other apps" else "Hide from other apps",
                                    leadingIcon = if (fullContact!!.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                                    onClick = {
                                        if (fullContact!!.isPrivate) {
                                            contactsViewModel.makeContactPublic(fullContact!!.id)
                                        } else {
                                            contactsViewModel.makeContactPrivate(fullContact!!.id)
                                        }
                                        navigator.navigateUp()
                                    }
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = "Delete",
                                    supporting = "Remove this contact from device",
                                    leadingIcon = Icons.Default.Delete,
                                    onClick = { showDeleteDialog = true }
                                )
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
