package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.media.RingtoneManager
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.MediaPlayer
import com.grinch.rivo4.view.screen.settings.CallRecordEntryCard
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import com.grinch.rivo4.modal.db.CallNoteDao
import com.grinch.rivo4.view.components.AddCallNoteDialog
import com.grinch.rivo4.view.components.CallbackReminderDialog
import com.grinch.rivo4.controller.CallRecorder
import com.ramcosta.composedestinations.generated.destinations.CallRecordingsScreenDestination
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CallLogFullScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun CallBackgroundRow(
    background: String?,
    saving: Boolean,
    onClick: () -> Unit
) {
    val previewDescription = stringResource(R.string.contact_call_background_preview)
    val headline = stringResource(R.string.contact_call_background)
    val supporting = when {
        saving -> stringResource(R.string.contact_call_background_saving)
        background != null -> stringResource(R.string.contact_call_background_set)
        else -> stringResource(R.string.contact_call_background_none)
    }

    if (background == null) {
        RivoListItem(
            headline = headline,
            supporting = supporting,
            leadingIcon = Icons.Default.Wallpaper,
            isCompact = true,
            onClick = onClick
        )
    } else {
        RivoListItem(
            headline = headline,
            supporting = supporting,
            leadingIcon = Icons.Default.Wallpaper,
            isCompact = true,
            onClick = onClick,
            trailingContent = {
                AsyncImage(
                    model = background,
                    contentDescription = previewDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 44.dp, height = 32.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }
        )
    }
}

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
    val callNoteDao = org.koin.compose.koinInject<CallNoteDao>()
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun loadContact(): Contact? {
        return try {
            val byId = if (contactId != null && contactId != "null") {
                contactsViewModel.getFullContactById(contactId)
            } else null
            if (byId != null) return byId
            if (phoneNumber != null) contactsViewModel.getFullContactByNumber(phoneNumber) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    LaunchedEffect(contactId, phoneNumber) {
        isFullLoading = true
        fullContact = loadContact()
        isFullLoading = false
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val updated = loadContact()
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
    val displayName = fullContact?.formattedDisplayName ?: fullContact?.name ?: phoneNumber ?: unknownLabel
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
    var callBackground by remember { mutableStateOf<String?>(null) }
    var backgroundSaving by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var addToContactNumber by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val contactsVM: ContactsViewModel = koinActivityViewModel()

    var defaultSimId by remember { mutableStateOf<String?>(null) }
    var showSimSelectDialog by remember { mutableStateOf(false) }

    val telecomMgr = remember(context) { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    val phoneAccounts = remember(telecomMgr, context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try { telecomMgr.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }
        } else emptyList()
    }

    LaunchedEffect(fullContact) {
        fullContact?.id?.let {
            favoriteNumber = prefs.getFavoriteNumber(it)
            favoriteEmail = prefs.getFavoriteEmail(it)
            defaultSimId = prefs.getDefaultSimForContact(it)
        }
    }

    var blockedVersion by remember { mutableIntStateOf(0) }
    val knownNumbers = remember(fullContact, phoneNumber) {
        ((fullContact?.phoneNumbers ?: emptyList()) + listOfNotNull(phoneNumber)).distinct()
    }
    val blockedNumbers = remember(knownNumbers, blockedVersion) {
        knownNumbers.filter { BlockedNumbersManager.isBlocked(context, it) }.toSet()
    }
    val isNumberBlocked: (String) -> Boolean = { number ->
        blockedNumbers.any { areNumbersEqual(it, number) }
    }

    val backgroundContactId = fullContact?.id?.takeIf { it.isNotBlank() }
    val backgroundNumbers = knownNumbers
    val backgroundKeys = remember(backgroundNumbers) {
        CallBackgroundStore.numberKeys(backgroundNumbers)
    }
    val backgroundAvailable = backgroundContactId != null || backgroundKeys.isNotEmpty()
    val backgroundErrorMessage = stringResource(R.string.contact_call_background_error)
    val backgroundNoTargetMessage = stringResource(R.string.contact_call_background_no_target)

    LaunchedEffect(backgroundContactId, backgroundNumbers) {
        callBackground = CallBackgroundStore.peek(context, backgroundContactId, backgroundNumbers)
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                backgroundSaving = true
                val saved = CallBackgroundStore.save(
                    context,
                    backgroundContactId,
                    backgroundNumbers,
                    uri
                )
                backgroundSaving = false
                if (saved) {
                    callBackground = CallBackgroundStore.peek(context, backgroundContactId, backgroundNumbers)
                } else {
                    snackbarHostState.showSnackbar(backgroundErrorMessage)
                }
            }
        }
    }

    var activePlayingFile by remember { mutableStateOf<File?>(null) }
    var isRecordingPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var recordingDurationMs by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingDeleteRecording by remember { mutableStateOf<File?>(null) }
    var recordingsRefreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(activePlayingFile) {
        if (activePlayingFile != null) {
            val mp = MediaPlayer().apply {
                try {
                    setDataSource(context, Uri.fromFile(activePlayingFile))
                    prepare()
                    start()
                    this@apply.playbackParams = this@apply.playbackParams.setSpeed(playbackSpeed)
                } catch (_: Exception) {}
            }
            mediaPlayer = mp
            isRecordingPlaying = mp.isPlaying
            recordingDurationMs = runCatching { mp.duration }.getOrDefault(0)

            onDispose {
                runCatching {
                    mp.stop()
                    mp.release()
                }
                mediaPlayer = null
                isRecordingPlaying = false
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(isRecordingPlaying, activePlayingFile) {
        while (isRecordingPlaying && activePlayingFile != null) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPositionMs = mp.currentPosition
                    recordingDurationMs = mp.duration
                } else {
                    isRecordingPlaying = false
                }
            }
            delay(250)
        }
    }

    val contactRecordings = remember(fullContact, displayName, phoneNumber, recordingsRefreshKey) {
        val all = CallRecorder.listRecordings(context)
        all.filter { file ->
            val name = file.name
            val cleanDisplay = displayName.replace(Regex("[^\\p{L}\\p{N}]"), "_").trim('_')
            val matchesName = cleanDisplay.length >= 3 && name.contains(cleanDisplay, ignoreCase = true)
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9]"), "")
            val matchesPhone = cleanPhone != null && cleanPhone.length >= 6 && name.contains(cleanPhone)
            val matchesContactPhones = fullContact?.phoneNumbers?.any { num ->
                val digits = num.replace(Regex("[^0-9]"), "")
                digits.length >= 6 && name.contains(digits)
            } == true
            matchesName || matchesPhone || matchesContactPhones
        }
    }

    val onBackgroundClick: () -> Unit = {
        when {
            !backgroundAvailable -> {
                scope.launch { snackbarHostState.showSnackbar(backgroundNoTargetMessage) }
            }
            callBackground != null -> showBackgroundDialog = true
            else -> backgroundPickerLauncher.launch(arrayOf("image/*"))
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

    val installedSocialApps = remember(context) { SocialUtils.getInstalledSocialApps(context) }
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

    if (showSimSelectDialog && phoneAccounts.size > 1) {
        val unknownSimLabel = stringResource(R.string.sim_picker_unknown_sim)
        val askEveryTimeLabel = stringResource(R.string.sim_ask_every_time)
        val options = listOf<android.telecom.PhoneAccountHandle?>(null) + phoneAccounts

        RivoSelectionDialog(
            onDismissRequest = { showSimSelectDialog = false },
            title = stringResource(R.string.contact_default_sim),
            items = options,
            itemLabel = { handle ->
                if (handle == null) {
                    askEveryTimeLabel
                } else {
                    val account = telecomMgr.getPhoneAccount(handle)
                    account?.label?.toString()?.takeIf { it.isNotBlank() }
                        ?: ("SIM " + (phoneAccounts.indexOf(handle) + 1) + " (" + unknownSimLabel + ")")
                }
            },
            onItemSelected = { handle ->
                fullContact?.id?.let { cid ->
                    prefs.setDefaultSimForContact(cid, handle?.id)
                    defaultSimId = handle?.id
                }
            },
            itemSupporting = { handle ->
                if (handle == null) {
                    askEveryTimeLabel
                } else {
                    val account = telecomMgr.getPhoneAccount(handle)
                    val address = account?.address?.schemeSpecificPart
                    val desc = account?.shortDescription?.toString()
                    if (!address.isNullOrBlank()) address
                    else if (!desc.isNullOrBlank()) desc
                    else "Slot " + (phoneAccounts.indexOf(handle) + 1)
                }
            },
            icon = Icons.Outlined.SimCard,
            itemIcon = { Icons.Outlined.SimCard },
            isSelected = { handle -> handle?.id == defaultSimId }
        )
    }

    if (showReminderDialog) {
        CallbackReminderDialog(
            phoneNumber = displayPhone,
            contactName = displayName,
            onDismissRequest = { showReminderDialog = false }
        )
    }

    if (showAddNoteDialog) {
        AddCallNoteDialog(
            phoneNumber = displayPhone,
            contactName = displayName,
            onDismissRequest = { showAddNoteDialog = false }
        )
    }

    if (showDeleteDialog) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                val targetId = fullContact?.id?.takeIf { it.isNotBlank() } ?: contactId
                if (targetId != null) {
                    contactsVM.deleteContact(targetId)
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

    if (showBackgroundDialog) {
        RivoDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = stringResource(R.string.contact_call_background),
            icon = Icons.Default.Wallpaper
        ) {
            callBackground?.let { current ->
                AsyncImage(
                    model = current,
                    contentDescription = stringResource(R.string.contact_call_background_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.large)
                )
                Spacer(Modifier.height(12.dp))
            }
            RivoListItem(
                headline = if (callBackground != null) {
                    stringResource(R.string.contact_call_background_change)
                } else {
                    stringResource(R.string.contact_call_background_choose)
                },
                leadingIcon = Icons.Default.Image,
                onClick = {
                    showBackgroundDialog = false
                    backgroundPickerLauncher.launch(arrayOf("image/*"))
                }
            )
            if (callBackground != null) {
                RivoListItem(
                    headline = stringResource(R.string.contact_call_background_remove),
                    leadingIcon = Icons.Default.Delete,
                    onClick = {
                        showBackgroundDialog = false
                        scope.launch {
                            CallBackgroundStore.clear(context, backgroundContactId, backgroundNumbers)
                            callBackground = null
                        }
                    }
                )
            }
        }
    }

    if (showQrDialog) {
        RivoDialog(
            onDismissRequest = { showQrDialog = false },
            title = stringResource(R.string.contact_details_qr_title),
            icon = Icons.Default.QrCode
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

    if (pendingDeleteRecording != null) {
        val fileToDelete = pendingDeleteRecording!!
        RivoConfirmationDialog(
            onDismissRequest = { pendingDeleteRecording = null },
            onConfirm = {
                if (activePlayingFile?.absolutePath == fileToDelete.absolutePath) {
                    activePlayingFile = null
                }
                fileToDelete.delete()
                recordingsRefreshKey++
                pendingDeleteRecording = null
            },
            title = "Delete Recording",
            message = "Delete ${fileToDelete.nameWithoutExtension}?",
            confirmLabel = stringResource(R.string.action_delete),
            isDestructive = true,
            icon = Icons.Default.Delete
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        IconButton(onClick = { showQrDialog = true }) {
                            Icon(
                                Icons.Outlined.QrCode2,
                                contentDescription = stringResource(R.string.contact_qr_code)
                            )
                        }
                        IconButton(onClick = shareContact) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = shareContactLabel
                            )
                        }
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
                        IconButton(onClick = shareContact) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = shareContactLabel
                            )
                        }
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RivoAvatar(
                                name = displayName,
                                photoUri = fullContact?.photoUri,
                                modifier = Modifier.size(92.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .basicMarquee()
                            )
                            fullContact?.nickname?.let { nickname ->
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nickname,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RivoExpressiveButton(
                                icon = Icons.Default.Call,
                                label = stringResource(R.string.contact_details_call),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                size = 52.dp,
                                iconSize = 22.dp,
                                onClick = {
                                    callLauncher.dial(if (fullContact == null) displayPhone else "", fullContact)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RivoExpressiveButton(
                                icon = Icons.AutoMirrored.Filled.Message,
                                label = stringResource(R.string.action_message),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                size = 52.dp,
                                iconSize = 22.dp,
                                onClick = {
                                    messageLauncher.sendMessage(if (fullContact == null) displayPhone else "", fullContact)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (fullContact == null) {
                                RivoExpressiveButton(
                                    icon = Icons.Default.PersonAdd,
                                    label = stringResource(R.string.contact_add_to_contacts),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    onClick = {
                                        addToContactNumber = displayPhone
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                val numberBlocked = isNumberBlocked(displayPhone)
                                RivoExpressiveButton(
                                    icon = if (numberBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                    label = if (numberBlocked) stringResource(R.string.action_unblock_number) else stringResource(R.string.action_block_number),
                                    containerColor = if (numberBlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    onClick = {
                                        if (numberBlocked) {
                                            BlockedNumbersManager.unblock(context, displayPhone)
                                        } else {
                                            BlockedNumbersManager.block(context, displayPhone)
                                        }
                                        blockedVersion++
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                RivoExpressiveButton(
                                    icon = Icons.Default.VideoCall,
                                    label = stringResource(R.string.contact_details_video),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    onClick = {
                                        videoLauncher.startVideoCall(displayPhone, fullContact)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                val hasEmails = fullContact?.emails?.isNotEmpty() == true
                                RivoExpressiveButton(
                                    icon = Icons.Default.Email,
                                    label = stringResource(R.string.label_email),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    enabled = hasEmails,
                                    onClick = { emailLauncher.sendEmail("", fullContact) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        val lastUsed = fullContact?.id?.let { prefs.getLastUsedNumber(it) }
                        val mobileLabel = stringResource(R.string.label_mobile)
                        val bulletFavorite = stringResource(R.string.contact_bullet_favorite)
                        val bulletRecent = stringResource(R.string.contact_bullet_recent)
                        RivoExpressiveCard(title = stringResource(R.string.contact_details_info_title), icon = Icons.Default.Info, isCompact = true) {
                            if (fullContact != null) {
                                val phoneEntries = remember(fullContact) {
                                    val fc = fullContact ?: return@remember emptyList()
                                    if (fc.phones.isNotEmpty()) fc.phones
                                    else deduplicateNumbers(fc.phoneNumbers).map { PhoneNumberEntry(it) }
                                }
                                phoneEntries.forEachIndexed { index, phoneEntry ->
                                    val number = phoneEntry.number
                                    val typeText = ContactTypeLabels.phoneTypeLabel(context, phoneEntry.type, phoneEntry.label)
                                    val isRecent = lastUsed != null && areNumbersEqual(lastUsed, number)
                                    val isFav = areNumbersEqual(favoriteNumber, number)

                                    var showMenu by remember { mutableStateOf(false) }

                                    Box {
                                        RivoListItem(
                                            headline = formatPhoneNumber(number),
                                            supporting = buildString {
                                                append(typeText.ifBlank { mobileLabel })
                                                if (isFav) append(bulletFavorite)
                                                if (isRecent) append(bulletRecent)
                                            },
                                            leadingIcon = Icons.Default.Phone,
                                            trailingIcon = if (isFav) Icons.Default.Star else if (isRecent) Icons.Default.History else null,
                                            isCompact = true,
                                            onClick = { callLauncher.dial(number, fullContact) },
                                            onLongClick = { showMenu = true }
                                        )

                                        RivoDropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            RivoDropdownMenuItem(
                                                text = { Text(if (isFav) stringResource(R.string.contact_clear_favorite) else stringResource(R.string.contact_set_as_favorite)) },
                                                onClick = {
                                                    showMenu = false
                                                    fullContact?.id?.let { cid ->
                                                        if (isFav) {
                                                            prefs.setFavoriteNumber(cid, null)
                                                            favoriteNumber = null
                                                        } else {
                                                            prefs.setFavoriteNumber(cid, number)
                                                            favoriteNumber = number
                                                        }
                                                    }
                                                },
                                                leadingIcon = { Icon(if (isFav) Icons.Default.StarOutline else Icons.Default.Star, null) }
                                            )
                                            RivoDropdownMenuItem(
                                                text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
                                                onClick = {
                                                    showMenu = false
                                                    clipboardManager.setText(AnnotatedString(number))
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                            )
                                            val numberBlocked = isNumberBlocked(number)
                                            RivoDropdownMenuItem(
                                                text = { Text(if (numberBlocked) stringResource(R.string.action_unblock_number) else stringResource(R.string.action_block_number)) },
                                                onClick = {
                                                    showMenu = false
                                                    if (numberBlocked) {
                                                        BlockedNumbersManager.unblock(context, number)
                                                    } else {
                                                        BlockedNumbersManager.block(context, number)
                                                    }
                                                    blockedVersion++
                                                },
                                                leadingIcon = { Icon(if (numberBlocked) Icons.Default.LockOpen else Icons.Default.Block, null) }
                                            )
                                        }
                                    }
                                    if (index < phoneEntries.size - 1 || fullContact?.emails?.isNotEmpty() == true) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                                val emailEntries = remember(fullContact) {
                                    val fc = fullContact ?: return@remember emptyList()
                                    if (fc.emailEntries.isNotEmpty()) fc.emailEntries
                                    else fc.emails.map { EmailEntry(it) }
                                }
                                emailEntries.forEachIndexed { index, emailEntry ->
                                    val email = emailEntry.address
                                    val emailTypeText = ContactTypeLabels.emailTypeLabel(context, emailEntry.type, emailEntry.label)
                                    val isFav = email == favoriteEmail
                                    var showMenu by remember { mutableStateOf(false) }

                                    Box {
                                        RivoListItem(
                                            headline = email,
                                            supporting = emailTypeText.ifBlank { stringResource(R.string.label_email) } + if (isFav) stringResource(R.string.contact_bullet_favorite) else "",
                                            leadingIcon = Icons.Default.Email,
                                            isCompact = true,
                                            onClick = { emailLauncher.sendEmail(email, fullContact) },
                                            onLongClick = { showMenu = true }
                                        )

                                        RivoDropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            RivoDropdownMenuItem(
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
                                            RivoDropdownMenuItem(
                                                text = { Text(stringResource(R.string.contact_copy_to_clipboard)) },
                                                onClick = {
                                                    showMenu = false
                                                    clipboardManager.setText(AnnotatedString(email))
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                            )
                                        }
                                    }
                                    if (index < emailEntries.size - 1) {
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
                                        isCompact = true,
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
                                                addToContactNumber = phoneNumber
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
                                        val numberBlocked = isNumberBlocked(phoneNumber)
                                        DropdownMenuItem(
                                            text = { Text(if (numberBlocked) stringResource(R.string.action_unblock_number) else stringResource(R.string.action_block_number)) },
                                            onClick = {
                                                showMenu = false
                                                if (numberBlocked) {
                                                    BlockedNumbersManager.unblock(context, phoneNumber)
                                                } else {
                                                    BlockedNumbersManager.block(context, phoneNumber)
                                                }
                                                blockedVersion++
                                            },
                                            leadingIcon = { Icon(if (numberBlocked) Icons.Default.LockOpen else Icons.Default.Block, null) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (fullContact != null && (fullContact!!.events.isNotEmpty() || fullContact!!.addresses.isNotEmpty())) {
                        item {
                            RivoExpressiveCard(title = stringResource(R.string.contact_events_title), icon = Icons.Default.Event, isCompact = true) {
                                fullContact!!.events.forEachIndexed { index, event ->
                                    val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                    RivoListItem(
                                        headline = event.date,
                                        supporting = event.label ?: if (isBirthday) stringResource(R.string.contact_event_birthday) else stringResource(R.string.contact_event_generic),
                                        leadingIcon = if (isBirthday) Icons.Outlined.Cake else Icons.Outlined.Event,
                                        isCompact = true,
                                        onClick = { clipboardManager.setText(AnnotatedString(event.date)) }
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
                                        isCompact = true,
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
                            RivoExpressiveCard(title = stringResource(R.string.label_notes), icon = Icons.AutoMirrored.Filled.Notes, isCompact = true) {
                                Box {
                                    Text(
                                        text = fullContact!!.notes!!,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { showNotesMenu = true },
                                                onLongClick = { showNotesMenu = true }
                                            )
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    DropdownMenu(
                                        expanded = showNotesMenu,
                                        onDismissRequest = { showNotesMenu = false }
                                    ) {
                                        RivoDropdownMenuItem(
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

                    item {
                        val callNotes by callNoteDao.getNotesForNumber(displayPhone).collectAsState(initial = emptyList())
                        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
                        RivoExpressiveCard(
                            title = "Call Notes (${callNotes.size})",
                            icon = Icons.Outlined.EditNote,
                            isCompact = true
                        ) {
                            Column(modifier = Modifier.animateContentSize()) {
                                if (callNotes.isEmpty()) {
                                    Text(
                                        text = "No call notes for this contact",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                } else {
                                    callNotes.forEachIndexed { index, noteItem ->
                                        RivoListItem(
                                            headline = noteItem.note,
                                            supporting = dateFormat.format(Date(noteItem.timestamp)),
                                            leadingIcon = Icons.AutoMirrored.Filled.Notes,
                                            isCompact = true,
                                            onClick = {},
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch { callNoteDao.deleteNote(noteItem) }
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        )
                                        if (index < callNotes.size - 1) {
                                            RivoDivider(Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showAddNoteDialog = true },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Call Note")
                                }
                            }
                        }
                    }

                    if (contactLogs.isNotEmpty()) {
                        item {
                            RivoExpressiveCard(title = stringResource(R.string.contact_recent_activity_title), icon = Icons.Default.History, isCompact = true) {
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
                        com.grinch.rivo4.view.components.ad.BannerAd()
                    }

                    if (contactRecordings.isNotEmpty()) {
                        item {
                            RivoExpressiveCard(
                                title = "Call Recordings (${contactRecordings.size})",
                                icon = Icons.Outlined.Mic,
                                isCompact = true
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    contactRecordings.take(3).forEach { file ->
                                        val isCurrentActive = activePlayingFile?.absolutePath == file.absolutePath
                                        CallRecordEntryCard(
                                            file = file,
                                            isCurrentActive = isCurrentActive,
                                            isPlaying = isCurrentActive && isRecordingPlaying,
                                            currentPositionMs = if (isCurrentActive) currentPositionMs else 0,
                                            durationMs = if (isCurrentActive) recordingDurationMs else 0,
                                            playbackSpeed = playbackSpeed,
                                            onCardClick = {
                                                if (isCurrentActive) {
                                                    mediaPlayer?.let { mp ->
                                                        if (mp.isPlaying) {
                                                            mp.pause()
                                                            isRecordingPlaying = false
                                                        } else {
                                                            mp.start()
                                                            isRecordingPlaying = true
                                                        }
                                                    }
                                                } else {
                                                    activePlayingFile = file
                                                }
                                            },
                                            onPlayPauseClick = {
                                                if (isCurrentActive) {
                                                    mediaPlayer?.let { mp ->
                                                        if (mp.isPlaying) {
                                                            mp.pause()
                                                            isRecordingPlaying = false
                                                        } else {
                                                            mp.start()
                                                            isRecordingPlaying = true
                                                        }
                                                    }
                                                } else {
                                                    activePlayingFile = file
                                                }
                                            },
                                            onSeekTo = { posMs ->
                                                if (isCurrentActive) {
                                                    currentPositionMs = posMs
                                                    mediaPlayer?.seekTo(posMs)
                                                }
                                            },
                                            onRewind10 = {
                                                if (isCurrentActive) {
                                                    val newPos = (currentPositionMs - 10000).coerceAtLeast(0)
                                                    currentPositionMs = newPos
                                                    mediaPlayer?.seekTo(newPos)
                                                }
                                            },
                                            onForward10 = {
                                                if (isCurrentActive) {
                                                    val newPos = (currentPositionMs + 10000).coerceAtMost(recordingDurationMs)
                                                    currentPositionMs = newPos
                                                    mediaPlayer?.seekTo(newPos)
                                                }
                                            },
                                            onSpeedChange = { newSpeed ->
                                                playbackSpeed = newSpeed
                                                mediaPlayer?.let { mp ->
                                                    runCatching { mp.playbackParams = mp.playbackParams.setSpeed(newSpeed) }
                                                }
                                            },
                                            onShareClick = { CallRecorder.share(context, file, "Share Recording") },
                                            onDeleteClick = { pendingDeleteRecording = file }
                                        )
                                    }
                                    if (contactRecordings.size > 3) {
                                        TextButton(
                                            onClick = { navigator.navigate(CallRecordingsScreenDestination(initialShowList = true)) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("View All Recordings (${contactRecordings.size})")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (installedSocialApps.isNotEmpty()) {
                        item {
                            RivoExpressiveCard(
                                title = stringResource(R.string.label_social_apps),
                                icon = Icons.AutoMirrored.Filled.Chat,
                                isCompact = true
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    installedSocialApps.forEach { app ->
                                        val painter = rememberAsyncImagePainter(app.assetIcon ?: app.iconDrawable)
                                        RivoExpressiveButton(
                                            painter = painter,
                                            label = app.name,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            size = 52.dp,
                                            iconSize = 32.dp,
                                            onClick = {
                                                onNumberActionClick({ num -> app.action(context, num) }, app.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (fullContact == null) {
                        item {
                            RivoExpressiveCard(
                                title = stringResource(R.string.contact_personalization_title),
                                icon = Icons.Default.Tune,
                                isCompact = true
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    RivoListItem(
                                        headline = "Callback Reminder",
                                        supporting = "Schedule a reminder to call back",
                                        leadingIcon = Icons.Outlined.Alarm,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = { showReminderDialog = true }
                                    )
                                    if (backgroundAvailable) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                        CallBackgroundRow(
                                            background = callBackground,
                                            saving = backgroundSaving,
                                            onClick = onBackgroundClick
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val fc = fullContact
                    if (fc != null) {
                        item {
                            val defaultRingtoneLabel = stringResource(R.string.ringtone_default)
                            val customRingtoneLabel = stringResource(R.string.ringtone_custom)
                            val selectRingtoneLabel = stringResource(R.string.contact_select_ringtone)

                            val currentRingtone = fc.customRingtone?.let { uriStr ->
                                runCatching { RingtoneManager.getRingtone(context, Uri.parse(uriStr))?.getTitle(context) }.getOrNull() ?: customRingtoneLabel
                            } ?: defaultRingtoneLabel

                            RivoExpressiveCard(
                                title = stringResource(R.string.contact_personalization_title),
                                icon = Icons.Default.Tune,
                                isCompact = true
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 0. Default SIM card (when Dual SIM)
                                    if (phoneAccounts.size > 1) {
                                        val selectedHandle = phoneAccounts.find { it.id == defaultSimId }
                                        val unknownSimLabel = stringResource(R.string.sim_picker_unknown_sim)
                                        val promptLabel = stringResource(R.string.sim_ask_every_time)
                                        val simLabel = if (selectedHandle != null) {
                                            val account = telecomMgr.getPhoneAccount(selectedHandle)
                                            account?.label?.toString()?.takeIf { it.isNotBlank() }
                                                ?: ("SIM " + (phoneAccounts.indexOf(selectedHandle) + 1) + " (" + unknownSimLabel + ")")
                                        } else promptLabel

                                        RivoListItem(
                                            headline = stringResource(R.string.contact_default_sim),
                                            supporting = simLabel,
                                            leadingIcon = Icons.Outlined.SimCard,
                                            isCompact = true,
                                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            onClick = { showSimSelectDialog = true }
                                        )
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                    }

                                    // 1. Custom Ringtone
                                    RivoListItem(
                                        headline = stringResource(R.string.contact_custom_ringtone),
                                        supporting = currentRingtone,
                                        leadingIcon = Icons.Default.MusicNote,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, selectRingtoneLabel)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, fc.customRingtone?.let { Uri.parse(it) })
                                            }
                                            ringtonePickerLauncher.launch(intent)
                                        }
                                    )

                                    // 2. Call Background (if available)
                                    if (backgroundAvailable) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                        CallBackgroundRow(
                                            background = callBackground,
                                            saving = backgroundSaving,
                                            onClick = onBackgroundClick
                                        )
                                    }

                                    RivoDivider(Modifier.padding(horizontal = 16.dp))

                                    // 3. Callback Reminder
                                    RivoListItem(
                                        headline = "Callback Reminder",
                                        supporting = "Schedule a reminder to call back",
                                        leadingIcon = Icons.Outlined.Alarm,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = { showReminderDialog = true }
                                    )
                                }
                            }
                        }

                        item {
                            val contactNumbers = fc.phoneNumbers
                            val contactBlocked = contactNumbers.isNotEmpty() && contactNumbers.all { isNumberBlocked(it) }

                            RivoExpressiveCard(
                                title = stringResource(R.string.contact_privacy_title),
                                icon = Icons.Default.Security,
                                isCompact = true
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 1. Storage Location
                                    RivoListItem(
                                        headline = if (fc.isPrivate) stringResource(R.string.contact_move_to_public_storage) else stringResource(R.string.contact_move_to_private_storage),
                                        supporting = if (fc.isPrivate) stringResource(R.string.contact_visible_to_other_apps) else stringResource(R.string.contact_hidden_from_other_apps),
                                        leadingIcon = if (fc.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = {
                                            if (fc.isPrivate) {
                                                contactsViewModel.makeContactPublic(fc.id)
                                            } else {
                                                contactsViewModel.makeContactPrivate(fc.id)
                                            }
                                            navigator.navigateUp()
                                        }
                                    )

                                    // 2. Hide Completely (if private)
                                    if (fc.isPrivate) {
                                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                                        RivoListItem(
                                            headline = if (fc.isHidden) "Unhide Contact" else "Hide Contact Completely",
                                            supporting = if (fc.isHidden) "Visible in lists" else "Hidden from lists (dial secret code to unlock)",
                                            leadingIcon = if (fc.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            isCompact = true,
                                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            onClick = {
                                                contactsViewModel.setContactHidden(fc.id, !fc.isHidden)
                                                navigator.navigateUp()
                                            }
                                        )
                                    }

                                    RivoDivider(Modifier.padding(horizontal = 16.dp))

                                    // 3. Block / Unblock Number
                                    RivoListItem(
                                        headline = if (contactBlocked) stringResource(R.string.contact_unblock) else stringResource(R.string.contact_block),
                                        supporting = if (contactBlocked) stringResource(R.string.contact_unblock_supporting) else stringResource(R.string.contact_block_supporting),
                                        leadingIcon = if (contactBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = {
                                            contactNumbers.forEach { number ->
                                                if (contactBlocked) {
                                                    BlockedNumbersManager.unblock(context, number)
                                                } else {
                                                    BlockedNumbersManager.block(context, number)
                                                }
                                            }
                                            blockedVersion++
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(48.dp)) }
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

        if (addToContactNumber != null) {
            AddToContactBottomSheet(
                phoneNumber = addToContactNumber!!,
                onDismissRequest = { addToContactNumber = null },
                onCreateNewContact = {
                    val num = addToContactNumber
                    addToContactNumber = null
                    navigator.navigate(
                        ContactEditScreenDestination(
                            initialPhone = num
                        )
                    )
                },
                onAddToExistingContact = {
                    val num = addToContactNumber
                    addToContactNumber = null
                    navigator.navigate(
                        ContactSelectionScreenDestination(
                            title = "Add to Existing Contact",
                            initialPhoneToAssign = num
                        )
                    )
                }
            )
        }
    }
}

