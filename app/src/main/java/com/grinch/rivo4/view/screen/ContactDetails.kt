package com.grinch.rivo4.view.screen
import android.widget.Toast
import com.grinch.rivo4.view.components.RivoTopBarIconButton
import androidx.compose.material.icons.automirrored.filled.CallMissed

import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.rivoCornerDp
import android.Manifest
import android.util.Log
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
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.view.screen.settings.CallRecordEntryCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.filled.NotificationImportant
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
    val allCallNotes by callNoteDao.getAllNotes().collectAsState(initial = emptyList())
    var showCallNotesSheet by remember { mutableStateOf(false) }
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
            Log.e("ContactDetails", "Error: ${e.message}", e)
            null
        }
    }

    val contactsById by contactsViewModel.contactsById.collectAsState()

    LaunchedEffect(contactId, phoneNumber) {
        isFullLoading = true
        fullContact = loadContact()
        isFullLoading = false
    }

    LaunchedEffect(contactsById) {
        val targetId = fullContact?.id?.takeIf { it.isNotBlank() } ?: contactId
        if (targetId != null && targetId != "null") {
            val matching = contactsById[targetId]
            if (matching != null) {
                val current = fullContact
                if (current != null) {
                    fullContact = current.copy(
                        name = matching.name,
                        givenName = matching.givenName,
                        middleName = matching.middleName,
                        familyName = matching.familyName,
                        nickname = matching.nickname,
                        photoUri = matching.photoUri,
                        isFavorite = matching.isFavorite,
                        phoneNumbers = matching.phoneNumbers,
                        phones = if (matching.phones.isNotEmpty()) matching.phones else current.phones,
                        emails = if (matching.emails.isNotEmpty()) matching.emails else current.emails,
                        emailEntries = if (matching.emailEntries.isNotEmpty()) matching.emailEntries else current.emailEntries,
                        accountName = matching.accountName,
                        accountType = matching.accountType
                    )
                } else {
                    fullContact = matching
                }
            } else if (contactsById.isNotEmpty() && contactId != null && contactId != "null") {
                // Contact was deleted, return up
                navigator.navigateUp()
            }
        }
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
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }
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
    val roundness = LocalCardRoundness.current

    var defaultSimId by remember { mutableStateOf<String?>(null) }
    var showSimSelectDialog by remember { mutableStateOf(false) }

    val telecomMgr = remember(context) { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    val phoneAccounts = remember(telecomMgr, context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                telecomMgr.callCapablePhoneAccounts
            } catch (e: SecurityException) {
                emptyList()
            }
        } else emptyList()
    }

    var isPriorityContact by remember(fullContact?.id, phoneNumber) {
        mutableStateOf(
            fullContact?.let {
                prefs.isPriorityContact(it.id, phoneNumber ?: it.phoneNumbers.firstOrNull())
            } ?: false
        )
    }

    LaunchedEffect(fullContact) {
        fullContact?.id?.let {
            favoriteNumber = prefs.getFavoriteNumber(it)
            favoriteEmail = prefs.getFavoriteEmail(it)
            defaultSimId = prefs.getDefaultSimForContact(it)
            isPriorityContact = prefs.isPriorityContact(it, phoneNumber ?: fullContact?.phoneNumbers?.firstOrNull())
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
    val contactNumbers = remember(fullContact, phoneNumber) {
        fullContact?.phoneNumbers?.ifEmpty { null } ?: listOfNotNull(phoneNumber)
    }
    val contactBlocked = contactNumbers.isNotEmpty() && contactNumbers.all { isNumberBlocked(it) }

    val matchedCallNotes = remember(allCallNotes, contactNumbers, fullContact?.name) {
        allCallNotes.filter { note ->
            contactNumbers.any { num -> areNumbersEqual(note.phoneNumber, num) } ||
            (fullContact?.name != null && !note.contactName.isNullOrBlank() && note.contactName.equals(fullContact?.name, ignoreCase = true))
        }
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
                } catch (_: Exception) {
                }
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

    var contactRecordings by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(fullContact, displayName, phoneNumber, recordingsRefreshKey) {
        contactRecordings = withContext(Dispatchers.IO) {
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
        val targetId = fullContact?.id
        val targetKeys = buildSet {
            fullContact?.phoneNumbers?.forEach { num ->
                addAll(ContactUtils.getLookupKeys(num))
            }
            if (phoneNumber != null) {
                addAll(ContactUtils.getLookupKeys(phoneNumber))
            }
        }
        allLogs.filter { log ->
            (targetId != null && log.contactId == targetId) ||
                    ContactUtils.getLookupKeys(log.number).any { it in targetKeys }
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

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        showQrDialog = false
                        shareContact()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = shareContactLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
                CallRecorder.delete(context, fileToDelete)
                recordingsRefreshKey++
                pendingDeleteRecording = null
            },
            title = stringResource(R.string.contact_delete_recording),
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
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (fullContact != null) {
                        val fc = fullContact!!
                        RivoTopBarIconButton(
                            onClick = { showQrDialog = true },
                            icon = Icons.Default.Share,
                            contentDescription = shareContactLabel
                        )
                        Box {
                            RivoTopBarIconButton(
                                onClick = { showOverflowMenu = true },
                                icon = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        navigator.navigate(ContactEditScreenDestination(contactId = fc.id))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (fc.isPrivate) stringResource(R.string.contact_move_to_public_storage)
                                            else stringResource(R.string.contact_move_to_private_storage)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (fc.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        if (fc.isPrivate) {
                                            contactsViewModel.makeContactPublic(fc.id)
                                        } else {
                                            contactsViewModel.makeContactPrivate(fc.id)
                                        }
                                        navigator.navigateUp()
                                    }
                                )
                                if (fc.isPrivate) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(if (fc.isHidden) R.string.contact_unhide else R.string.contact_hide_completely)) },
                                        leadingIcon = {
                                            Icon(
                                                if (fc.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            contactsViewModel.setContactHidden(fc.id, !fc.isHidden)
                                            navigator.navigateUp()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.call_notes_title)) },
                                    leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showCallNotesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (contactBlocked) stringResource(R.string.contact_unblock)
                                            else stringResource(R.string.contact_block)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (contactBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
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
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.action_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    } else if (phoneNumber != null && phoneNumber != unknownLabel) {
                        RivoTopBarIconButton(
                            onClick = { showQrDialog = true },
                            icon = Icons.Default.Share,
                            contentDescription = shareContactLabel
                        )
                        Box {
                            RivoTopBarIconButton(
                                onClick = { showOverflowMenu = true },
                                icon = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_contact)) },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        navigator.navigate(ContactEditScreenDestination(initialPhone = phoneNumber))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.call_notes_title)) },
                                    leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showCallNotesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (contactBlocked) stringResource(R.string.contact_unblock)
                                            else stringResource(R.string.contact_block)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (contactBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .clickable { showFullScreenPhoto = true },
                                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
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
                                labelMaxLines = if (fullContact == null) 2 else 1,
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
                                labelMaxLines = if (fullContact == null) 2 else 1,
                                onClick = {
                                    messageLauncher.sendMessage(
                                        if (fullContact == null) displayPhone else "",
                                        fullContact
                                    )
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
                                    labelMaxLines = 2,
                                    onClick = {
                                        addToContactNumber = displayPhone
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                val numberBlocked = isNumberBlocked(displayPhone)
                                RivoExpressiveButton(
                                    icon = if (numberBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                    label = if (numberBlocked) stringResource(R.string.action_unblock_number) else stringResource(
                                        R.string.action_block_number
                                    ),
                                    containerColor = if (numberBlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    labelMaxLines = 2,
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
                                RivoExpressiveButton(
                                    icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    label = stringResource(R.string.content_desc_favorite),
                                    containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    size = 52.dp,
                                    iconSize = 22.dp,
                                    onClick = {
                                        fullContact?.let { contact ->
                                            val newFavorite = !contact.isFavorite
                                            fullContact = contact.copy(isFavorite = newFavorite)
                                            contactsViewModel.toggleFavorite(contact)
                                        }
                                    },
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
                        RivoExpressiveGroup(
                            title = stringResource(R.string.contact_details_info_title),
                            icon = Icons.Default.Info
                        ) {
                            if (fullContact != null) {
                                val phoneEntries = remember(fullContact) {
                                    val fc = fullContact ?: return@remember emptyList()
                                    if (fc.phones.isNotEmpty()) fc.phones
                                    else deduplicateNumbers(fc.phoneNumbers).map { PhoneNumberEntry(it) }
                                }
                                phoneEntries.forEach { phoneEntry ->
                                    val number = phoneEntry.number
                                    val typeText =
                                        ContactTypeLabels.phoneTypeLabel(context, phoneEntry.type, phoneEntry.label)
                                    val isRecent = lastUsed != null && areNumbersEqual(lastUsed, number)
                                    val isFav = areNumbersEqual(favoriteNumber, number)

                                    item {
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
                                                    text = {
                                                        Text(
                                                            if (isFav) stringResource(R.string.contact_clear_favorite) else stringResource(
                                                                R.string.contact_set_as_favorite
                                                            )
                                                        )
                                                    },
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
                                                    leadingIcon = {
                                                        Icon(
                                                            if (isFav) Icons.Default.StarOutline else Icons.Default.Star,
                                                            null
                                                        )
                                                    }
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
                                                    text = {
                                                        Text(
                                                            if (numberBlocked) stringResource(R.string.action_unblock_number) else stringResource(
                                                                R.string.action_block_number
                                                            )
                                                        )
                                                    },
                                                    onClick = {
                                                        showMenu = false
                                                        if (numberBlocked) {
                                                            BlockedNumbersManager.unblock(context, number)
                                                        } else {
                                                            BlockedNumbersManager.block(context, number)
                                                        }
                                                        blockedVersion++
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            if (numberBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                                            null
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                val emailEntries = remember(fullContact) {
                                    val fc = fullContact ?: return@remember emptyList()
                                    if (fc.emailEntries.isNotEmpty()) fc.emailEntries
                                    else fc.emails.map { EmailEntry(it) }
                                }
                                emailEntries.forEach { emailEntry ->
                                    val email = emailEntry.address
                                    val emailTypeText =
                                        ContactTypeLabels.emailTypeLabel(context, emailEntry.type, emailEntry.label)
                                    val isFav = email == favoriteEmail

                                    item {
                                        var showMenu by remember { mutableStateOf(false) }

                                        Box {
                                            RivoListItem(
                                                headline = email,
                                                supporting = emailTypeText.ifBlank { stringResource(R.string.label_email) } + if (isFav) stringResource(
                                                    R.string.contact_bullet_favorite
                                                ) else "",
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
                                                    text = {
                                                        Text(
                                                            if (isFav) stringResource(R.string.contact_clear_default) else stringResource(
                                                                R.string.contact_set_as_default
                                                            )
                                                        )
                                                    },
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
                                                    leadingIcon = {
                                                        Icon(
                                                            if (isFav) Icons.Default.StarOutline else Icons.Default.Star,
                                                            null
                                                        )
                                                    }
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
                                    }
                                }

                                fullContact?.addresses?.filter { it.isNotBlank() }?.forEach { address ->
                                    item {
                                        RivoListItem(
                                            headline = address,
                                            supporting = stringResource(R.string.label_address),
                                            leadingIcon = Icons.Default.LocationOn,
                                            isCompact = true,
                                            onClick = {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            } else if (phoneNumber != null) {
                                item {
                                    RivoListItem(
                                        headline = formatPhoneNumber(phoneNumber),
                                        supporting = stringResource(R.string.label_mobile),
                                        leadingIcon = Icons.Default.Phone,
                                        isCompact = true,
                                        onClick = { callLauncher.dial(phoneNumber, null) }
                                    )
                                }
                            }
                        }
                    }

                    val hasContactNotes = fullContact != null && !fullContact!!.notes.isNullOrBlank()
                    if (hasContactNotes || matchedCallNotes.isNotEmpty()) {
                        item {
                            RivoExpressiveGroup(
                                title = stringResource(R.string.label_notes),
                                icon = Icons.Outlined.EditNote
                            ) {
                                if (hasContactNotes) {
                                    item {
                                        val notesText = fullContact!!.notes!!
                                        RivoListItem(
                                            headline = notesText,
                                            supporting = stringResource(R.string.label_notes),
                                            leadingIcon = Icons.AutoMirrored.Filled.Notes,
                                            isCompact = true,
                                            headlineMaxLines = Int.MAX_VALUE,
                                            onClick = {
                                                fullContact?.let {
                                                    navigator.navigate(ContactEditScreenDestination(contactId = it.id))
                                                }
                                            },
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(notesText))
                                                Toast.makeText(context, context.getString(R.string.contact_copy_to_clipboard), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                                matchedCallNotes.forEach { callNote ->
                                    item {
                                        val noteDate = remember(callNote.timestamp) {
                                            SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(callNote.timestamp))
                                        }
                                        RivoListItem(
                                            headline = callNote.note,
                                            supporting = noteDate,
                                            leadingIcon = Icons.AutoMirrored.Filled.Notes,
                                            isCompact = true,
                                            headlineMaxLines = Int.MAX_VALUE,
                                            onClick = {
                                                showCallNotesSheet = true
                                            },
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(callNote.note))
                                                Toast.makeText(context, context.getString(R.string.contact_copy_to_clipboard), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { showCallNotesSheet = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_add_note),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (contactLogs.isNotEmpty()) {
                        item {
                            val finalContactId =
                                if (fullContact?.id != null) fullContact!!.id else if (contactId != "null") contactId else null
                            RivoExpressiveGroup(
                                title = stringResource(R.string.contact_recent_activity_title),
                                icon = Icons.Default.History
                            ) {
                                contactLogs.take(3).forEach { log ->
                                    item {
                                        val callTypeLabel = when (log.type) {
                                            android.provider.CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
                                            android.provider.CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
                                            android.provider.CallLog.Calls.MISSED_TYPE -> stringResource(R.string.call_type_missed)
                                            android.provider.CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.call_type_rejected)
                                            android.provider.CallLog.Calls.BLOCKED_TYPE -> stringResource(R.string.call_type_blocked)
                                            android.provider.CallLog.Calls.VOICEMAIL_TYPE -> stringResource(R.string.call_type_voicemail)
                                            else -> stringResource(R.string.call_type_call)
                                        }
                                        RivoListItem(
                                            headline = callTypeLabel,
                                            supporting = formatRelativeCallDateTime(context, log.date),
                                            leadingIcon = when (log.type) {
                                                android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                                                android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                                                android.provider.CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                                                else -> Icons.Default.Call
                                            },
                                            isCompact = true
                                        )
                                    }
                                }

                                if (contactLogs.size > 3) {
                                    item {
                                        TextButton(
                                            onClick = {
                                                navigator.navigate(
                                                    CallLogFullScreenDestination(
                                                        contactId = finalContactId,
                                                        phoneNumber = phoneNumber
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = stringResource(R.string.contact_show_full_history),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
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
                            RivoExpressiveGroup(
                                title = stringResource(R.string.contact_call_recordings_count, contactRecordings.size),
                                icon = Icons.Outlined.Mic
                            ) {
                                val recordingsToShow = contactRecordings.take(3)
                                recordingsToShow.forEachIndexed { index, file ->
                                    item {
                                        val isCurrentActive = activePlayingFile?.absolutePath == file.absolutePath
                                        CallRecordEntryCard(
                                            file = file,
                                            isCurrentActive = isCurrentActive,
                                            isPlaying = isCurrentActive && isRecordingPlaying,
                                            currentPositionMs = if (isCurrentActive) currentPositionMs else 0,
                                            durationMs = if (isCurrentActive) recordingDurationMs else 0,
                                            playbackSpeed = playbackSpeed,
                                            shape = RoundedCornerShape(0.dp),
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
                                                    val newPos =
                                                        (currentPositionMs + 10000).coerceAtMost(recordingDurationMs)
                                                    currentPositionMs = newPos
                                                    mediaPlayer?.seekTo(newPos)
                                                }
                                            },
                                            onSpeedChange = { newSpeed ->
                                                playbackSpeed = newSpeed
                                                mediaPlayer?.let { mp ->
                                                    runCatching {
                                                        mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                                                    }
                                                }
                                            },
                                            onShareClick = { CallRecorder.share(context, file, "Share Recording") },
                                            onDeleteClick = { pendingDeleteRecording = file }
                                        )
                                    }
                                }

                                if (contactRecordings.size > 3) {
                                    item {
                                        TextButton(
                                            onClick = {
                                                navigator.navigate(CallRecordingsScreenDestination(initialShowList = true))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "View All Recordings (${contactRecordings.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (installedSocialApps.isNotEmpty()) {
                        item {
                            RivoExpressiveGroup(
                                title = stringResource(R.string.label_social_apps),
                                icon = Icons.AutoMirrored.Filled.Chat
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    }

                    if (fullContact == null) {
                        item {
                            RivoExpressiveGroup(
                                title = stringResource(R.string.contact_personalization_title),
                                icon = Icons.Default.Tune
                            ) {
                                item {
                                    RivoListItem(
                                        headline = stringResource(R.string.contact_callback_reminder),
                                        supporting = stringResource(R.string.contact_callback_reminder_supporting),
                                        leadingIcon = Icons.Outlined.Alarm,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = { showReminderDialog = true }
                                    )
                                }
                                if (backgroundAvailable) {
                                    item {
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
                                runCatching {
                                    RingtoneManager.getRingtone(context, Uri.parse(uriStr))?.getTitle(context)
                                }.getOrNull() ?: customRingtoneLabel
                            } ?: defaultRingtoneLabel

                            RivoExpressiveGroup(
                                title = stringResource(R.string.contact_personalization_title),
                                icon = Icons.Default.Tune
                            ) {
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

                                    item {
                                        RivoListItem(
                                            headline = stringResource(R.string.contact_default_sim),
                                            supporting = simLabel,
                                            leadingIcon = Icons.Outlined.SimCard,
                                            isCompact = true,
                                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            onClick = { showSimSelectDialog = true }
                                        )
                                    }
                                }

                                // 1. Custom Ringtone
                                item {
                                    RivoListItem(
                                        headline = stringResource(R.string.contact_custom_ringtone),
                                        supporting = currentRingtone,
                                        leadingIcon = Icons.Default.MusicNote,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(
                                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                                    RingtoneManager.TYPE_RINGTONE
                                                )
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, selectRingtoneLabel)
                                                putExtra(
                                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                    fc.customRingtone?.let { Uri.parse(it) })
                                            }
                                            ringtonePickerLauncher.launch(intent)
                                        }
                                    )
                                }

                                // 2. Call Background (if available)
                                if (backgroundAvailable) {
                                    item {
                                        CallBackgroundRow(
                                            background = callBackground,
                                            saving = backgroundSaving,
                                            onClick = onBackgroundClick
                                        )
                                    }
                                }

                                // 3. Callback Reminder
                                item {
                                    RivoListItem(
                                        headline = stringResource(R.string.contact_callback_reminder),
                                        supporting = stringResource(R.string.contact_callback_reminder_supporting),
                                        leadingIcon = Icons.Outlined.Alarm,
                                        isCompact = true,
                                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = { showReminderDialog = true }
                                    )
                                }

                                // 4. Priority Contact
                                item {
                                    RivoListItem(
                                        headline = stringResource(R.string.priority_contact_title),
                                        supporting = stringResource(R.string.priority_contact_summary),
                                        leadingIcon = if (isPriorityContact) Icons.Filled.NotificationImportant else Icons.Outlined.NotificationImportant,
                                        isCompact = true,
                                        trailingContent = {
                                            RivoSwitch(
                                                checked = isPriorityContact,
                                                onCheckedChange = { checked ->
                                                    isPriorityContact = checked
                                                    prefs.setPriorityContact(fc.id, fc.phoneNumbers.ifEmpty { listOfNotNull(phoneNumber) }, checked)
                                                }
                                            )
                                        },
                                        onClick = {
                                            val newChecked = !isPriorityContact
                                            isPriorityContact = newChecked
                                            prefs.setPriorityContact(fc.id, fc.phoneNumbers.ifEmpty { listOfNotNull(phoneNumber) }, newChecked)
                                        }
                                    )
                                }
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
            val targetPhone = addToContactNumber!!
            AddToContactBottomSheet(
                phoneNumber = targetPhone,
                onDismissRequest = { addToContactNumber = null },
                onCreateNewContact = {
                    addToContactNumber = null
                    navigator.navigate(
                        ContactEditScreenDestination(
                            initialPhone = targetPhone
                        )
                    )
                },
                onAddToExistingContact = {
                    addToContactNumber = null
                    navigator.navigate(
                        ContactSelectionScreenDestination(
                            title = context.getString(R.string.action_add_existing_contact),
                            initialPhoneToAssign = targetPhone
                        )
                    )
                }
            )
        }

        if (showFullScreenPhoto) {
            Dialog(
                onDismissRequest = { showFullScreenPhoto = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (fullContact?.photoUri != null) {
                        AsyncImage(
                            model = fullContact?.photoUri,
                            contentDescription = displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreenPhoto = false },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreenPhoto = false },
                            contentAlignment = Alignment.Center
                        ) {
                            RivoAvatar(
                                name = displayName,
                                photoUri = null,
                                modifier = Modifier.size(200.dp),
                                textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFullScreenPhoto = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (showCallNotesSheet) {
            CallNotesSheet(
                phoneNumber = if (displayPhone != unknownLabel) displayPhone else (fullContact?.phoneNumbers?.firstOrNull() ?: ""),
                contactName = displayName,
                onDismiss = { showCallNotesSheet = false }
            )
        }
    }
}
