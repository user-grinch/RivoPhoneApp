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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.grinch.rivo4.R
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

    val unknownLabel = stringResource(R.string.label_unknown)
    val displayPhone = phoneNumber ?: fullContact?.phoneNumbers?.firstOrNull() ?: unknownLabel
    val displayName = fullContact?.name ?: phoneNumber ?: unknownLabel
    val shareContactLabel = stringResource(R.string.contact_share)

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

    val shareContactText = stringResource(R.string.contact_share_text, displayName, displayPhone)
    val shareContact = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareContactText)
        }
        context.startActivity(Intent.createChooser(intent, shareContactLabel))
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
            title = stringResource(R.string.contact_delete_dialog_title),
            message = stringResource(R.string.contact_delete_dialog_message),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }

    if (showNumberSelectionDialog && fullContact != null) {
        val mobileLabel = stringResource(R.string.label_mobile)
        RivoSelectionDialog(
            onDismissRequest = { showNumberSelectionDialog = false },
            title = selectionTitle,
            items = fullContact!!.phoneNumbers,
            itemLabel = { formatPhoneNumber(it) },
            onItemSelected = { pendingSocialAction?.invoke(it) },
            itemSupporting = { mobileLabel },
            icon = Icons.Default.Phone,
            itemIcon = { if (areNumbersEqual(favoriteNumber, it)) Icons.Default.Star else Icons.Default.Phone },
            isSelected = { areNumbersEqual(favoriteNumber, it) }
        )
    }

    if (showQrDialog) {
        RivoDialog(
            onDismissRequest = { showQrDialog = false },
            title = stringResource(R.string.contact_details_qr_title),
            icon = Icons.Default.QrCode,
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_close))
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
                        contentDescription = stringResource(R.string.contact_details_qr_content_desc),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                                contentDescription = stringResource(R.string.content_desc_favorite),
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            fullContact?.let {
                                navigator.navigate(ContactEditScreenDestination(contactId = it.id))
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    } else if (phoneNumber != null && phoneNumber != unknownLabel) {
                        IconButton(onClick = {
                            navigator.navigate(ContactEditScreenDestination(initialPhone = phoneNumber))
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.action_add_contact))
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
                                label = stringResource(R.string.contact_details_call),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    callLauncher.dial(if (fullContact == null) displayPhone else "", fullContact)
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                label = stringResource(R.string.action_message),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {
                                    messageLauncher.sendMessage(if (fullContact == null) displayPhone else "", fullContact)
                                }
                            )
                            RivoExpressiveButton(
                                icon = Icons.Default.VideoCall,
                                label = stringResource(R.string.contact_details_video),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {
                                    videoLauncher.startVideoCall(displayPhone, fullContact)
                                }
                            )
                            val hasEmails = fullContact?.emails?.isNotEmpty() == true
                            RivoExpressiveButton(
                                icon = Icons.Default.Email,
                                label = stringResource(R.string.label_email),
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
                        val mobileLabel = stringResource(R.string.label_mobile)
                        val bulletFavorite = stringResource(R.string.contact_bullet_favorite)
                        val bulletRecent = stringResource(R.string.contact_bullet_recent)
                        RivoExpressiveCard(title = stringResource(R.string.contact_details_info_title), icon = Icons.Default.Info) {
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
                                                append(mobileLabel)
                                                if (isFav) append(bulletFavorite)
                                                if (isRecent) append(bulletRecent)
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
                                                text = { Text(if (isFav) stringResource(R.string.contact_clear_favorite) else stringResource(R.string.contact_set_as_favorite)) },
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
                                                text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
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
                                            supporting = if (isFav) stringResource(R.string.label_email) + stringResource(R.string.contact_bullet_favorite) else stringResource(R.string.label_email),
                                            leadingIcon = Icons.Default.Email,
                                            onClick = { emailLauncher.sendEmail(email, fullContact) },
                                            onLongClick = { showMenu = true }
                                        )

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isFav) stringResource(R.string.contact_clear_default) else stringResource(R.string.contact_set_as_default)) },
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
                                                text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
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
                            } else if (phoneNumber != null && phoneNumber != unknownLabel) {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    RivoListItem(
                                        headline = formatPhoneNumber(phoneNumber),
                                        supporting = stringResource(R.string.label_unknown_number),
                                        leadingIcon = Icons.Default.Phone,
                                        onClick = { callLauncher.dial(phoneNumber, null) },
                                        onLongClick = { showMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.contact_add_to_contacts)) },
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
                                            text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
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
                            RivoExpressiveCard(title = stringResource(R.string.contact_events_title), icon = Icons.Default.Event) {
                                fullContact!!.events.forEachIndexed { index, event ->
                                    val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                    RivoListItem(
                                        headline = event.date,
                                        supporting = event.label ?: if (isBirthday) stringResource(R.string.contact_event_birthday) else stringResource(R.string.contact_event_generic),
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
                                        supporting = stringResource(R.string.label_address),
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
                            RivoExpressiveCard(title = stringResource(R.string.label_notes), icon = Icons.AutoMirrored.Filled.Notes) {
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
                                            text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
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
                            RivoExpressiveCard(title = stringResource(R.string.contact_recent_activity_title), icon = Icons.Default.History) {
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
                                            Text(stringResource(R.string.contact_show_full_history))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val whatsAppLabel = stringResource(R.string.brand_whatsapp)
                        val telegramLabel = stringResource(R.string.brand_telegram)
                        val signalLabel = stringResource(R.string.brand_signal)
                        RivoExpressiveCard(title = stringResource(R.string.label_social_apps), icon = Icons.AutoMirrored.Filled.Chat) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/whatsapp.png"),
                                    label = whatsAppLabel,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openWhatsApp, whatsAppLabel) }
                                )
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/telegram.png"),
                                    label = telegramLabel,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openTelegram, telegramLabel) }
                                )
                                RivoExpressiveButton(
                                    painter = rememberAsyncImagePainter("file:///android_asset/icons/signal.png"),
                                    label = signalLabel,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    size = 52.dp,
                                    iconSize = 32.dp,
                                    onClick = { onNumberActionClick(openSignal, signalLabel) }
                                )
                            }
                        }
                    }

                    if (fullContact != null) {
                        item {
                            val defaultRingtoneLabel = stringResource(R.string.ringtone_default)
                            val customRingtoneLabel = stringResource(R.string.ringtone_custom)
                            val selectRingtoneLabel = stringResource(R.string.contact_select_ringtone)
                            RivoExpressiveCard(title = stringResource(R.string.contact_settings_title), icon = Icons.Default.Settings) {
                                val currentRingtone = fullContact!!.customRingtone?.let {
                                    RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) ?: customRingtoneLabel
                                } ?: defaultRingtoneLabel

                                RivoListItem(
                                    headline = stringResource(R.string.contact_custom_ringtone),
                                    supporting = currentRingtone,
                                    leadingIcon = Icons.Default.MusicNote,
                                    onClick = {
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, selectRingtoneLabel)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, fullContact!!.customRingtone?.let { Uri.parse(it) })
                                        }
                                        ringtonePickerLauncher.launch(intent)
                                    }
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = shareContactLabel,
                                    supporting = stringResource(R.string.contact_share_description),
                                    leadingIcon = Icons.Default.Share,
                                    onClick = shareContact
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = stringResource(R.string.contact_qr_code),
                                    supporting = stringResource(R.string.contact_qr_code_description),
                                    leadingIcon = Icons.Outlined.QrCode2,
                                    onClick = { showQrDialog = true }
                                )
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                                RivoListItem(
                                    headline = if (fullContact!!.isPrivate) stringResource(R.string.contact_move_to_public_storage) else stringResource(R.string.contact_move_to_private_storage),
                                    supporting = if (fullContact!!.isPrivate) stringResource(R.string.contact_visible_to_other_apps) else stringResource(R.string.contact_hidden_from_other_apps),
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
                                    headline = stringResource(R.string.action_delete),
                                    supporting = stringResource(R.string.contact_remove_from_device),
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
