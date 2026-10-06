package com.grinch.rivo4.view.screen
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.grinch.rivo4.view.components.MenuTopAppBar

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.controller.util.normalizePhoneNumber
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.*
import androidx.compose.ui.unit.TextUnit
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoMotion
import com.grinch.rivo4.view.theme.callColors
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactSelectionScreenDestination
import com.grinch.rivo4.view.components.AddToContactBottomSheet
import com.grinch.rivo4.modal.data.Contact
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

data class DialpadDimensions(
    val keyWidth: androidx.compose.ui.unit.Dp,
    val keyHeight: androidx.compose.ui.unit.Dp,
    val circleKeySize: androidx.compose.ui.unit.Dp,
    val rowSpacing: androidx.compose.ui.unit.Dp,
    val numberFontSize: TextUnit,
    val lettersFontSize: TextUnit,
    val actionButtonSize: androidx.compose.ui.unit.Dp,
    val callButtonWidth: androidx.compose.ui.unit.Dp,
    val callButtonHeight: androidx.compose.ui.unit.Dp,
    val simButtonWidth: androidx.compose.ui.unit.Dp,
    val simButtonHeight: androidx.compose.ui.unit.Dp,
    val topPadding: androidx.compose.ui.unit.Dp,
    val bottomSheetBottomPadding: androidx.compose.ui.unit.Dp,
    val contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    companion object {
        fun forSize(size: Int): DialpadDimensions = when (size) {
            PreferenceManager.DIALPAD_SIZE_COMPACT -> DialpadDimensions(
                keyWidth = 84.dp,
                keyHeight = 52.dp,
                circleKeySize = 54.dp,
                rowSpacing = 5.dp,
                numberFontSize = 24.sp,
                lettersFontSize = 10.sp,
                actionButtonSize = 54.dp,
                callButtonWidth = 84.dp,
                callButtonHeight = 58.dp,
                simButtonWidth = 70.dp,
                simButtonHeight = 58.dp,
                topPadding = 14.dp,
                bottomSheetBottomPadding = 8.dp,
                contentBottomPadding = 350.dp
            )
            PreferenceManager.DIALPAD_SIZE_LARGE -> DialpadDimensions(
                keyWidth = 106.dp,
                keyHeight = 72.dp,
                circleKeySize = 72.dp,
                rowSpacing = 10.dp,
                numberFontSize = 36.sp,
                lettersFontSize = 12.5.sp,
                actionButtonSize = 72.dp,
                callButtonWidth = 114.dp,
                callButtonHeight = 76.dp,
                simButtonWidth = 84.dp,
                simButtonHeight = 76.dp,
                topPadding = 24.dp,
                bottomSheetBottomPadding = 16.dp,
                contentBottomPadding = 480.dp
            )
            else -> DialpadDimensions(
                keyWidth = 96.dp,
                keyHeight = 62.dp,
                circleKeySize = 64.dp,
                rowSpacing = 8.dp,
                numberFontSize = 30.sp,
                lettersFontSize = 11.sp,
                actionButtonSize = 64.dp,
                callButtonWidth = 100.dp,
                callButtonHeight = 68.dp,
                simButtonWidth = 76.dp,
                simButtonHeight = 68.dp,
                topPadding = 20.dp,
                bottomSheetBottomPadding = 12.dp,
                contentBottomPadding = 420.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Destination<RootGraph>
@Composable
fun DialPadScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialNumber: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val callLogVM: CallLogViewModel = koinActivityViewModel()
    val allCallLogs by callLogVM.allCallLogs.collectAsState()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val allContacts by contactsVM.allContacts.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialNumber ?: "")) }
    var dialpadHeightPx by remember { mutableIntStateOf(0) }
    val number = textFieldValue.text

    LaunchedEffect(Unit) {
        if (initialNumber.isNullOrEmpty() && prefs.isAutoPasteClipboardEnabled()) {
            val clipText = clipboardManager.getText()?.text?.trim()
            if (!clipText.isNullOrEmpty()) {
                val digitsCount = clipText.count { it.isDigit() }
                val validChars = clipText.all { it.isDigit() || it == '+' || it == '*' || it == '#' || it == ' ' || it == '-' || it == '(' || it == ')' }
                if (validChars && digitsCount >= 3 && clipText.length <= 30) {
                    textFieldValue = TextFieldValue(clipText, TextRange(clipText.length))
                }
            }
        }
    }

    var showSocialDialog by remember { mutableStateOf(false) }
    var showAddToContactSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = number.isNotEmpty()) {
        textFieldValue = TextFieldValue("")
    }

    val onDigitClick = { digit: String ->
        val selection = textFieldValue.selection
        val text = textFieldValue.text
        val newText = text.substring(0, selection.start) + digit + text.substring(selection.end)
        val newSelection = TextRange(selection.start + digit.length)
        textFieldValue = TextFieldValue(newText, newSelection)
    }

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_DTMF, 80) }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    val t9Enabled by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_T9_DIALING, true))
    }
    val speedDialEnabled by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SPEED_DIAL, true))
    }
    val displayOrder by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0))
    }
    val dialpadStyle by remember(settingsState) {
        mutableIntStateOf(prefs.getDialpadStyle())
    }
    val dialpadSize by remember(settingsState) {
        mutableIntStateOf(prefs.getDialpadSize())
    }
    val dimensions = remember(dialpadSize) {
        DialpadDimensions.forSize(dialpadSize)
    }
    val dialpadHeightDp = with(LocalDensity.current) {
        if (dialpadHeightPx > 0) dialpadHeightPx.toDp() else dimensions.contentBottomPadding
    }

    val isKnownSecretCode = remember {
        { input: String ->
            val knownExact = setOf(
                "*#06#", "*#07#", "*#0*#", "*#0228#", "*#9900#", "*#1234#", "*#0808#",
                "*#9090#", "*#2663#", "*#800#", "*#808#", "*#888#", "*#899#", "*#6776#"
            )
            if (input in knownExact) true
            else if (input.startsWith("*#*#") && input.endsWith("#*#*") && input.length >= 9) true
            else if (input.startsWith("##") && input.endsWith("##") && input.length >= 6) true
            else false
        }
    }

    LaunchedEffect(number) {
        val cleanNumber = number.replace(" ", "")
        val secretDialpadCode = prefs.getString(PreferenceManager.KEY_SECRET_DIALPAD_CODE, PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE) ?: PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
        if (cleanNumber.isNotEmpty() && cleanNumber == secretDialpadCode.replace(" ", "")) {
            textFieldValue = TextFieldValue("")
            val visible = contactsVM.toggleHiddenContactsVisible()
            val msg = if (visible) "Private Storage visible" else "Private Storage hidden"
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        if (isKnownSecretCode(cleanNumber)) {
            val handled = com.grinch.rivo4.controller.util.processSecretCode(context, cleanNumber)
            if (handled) {
                textFieldValue = TextFieldValue("")
            }
        }
    }

    val callLauncher = rememberCallLauncher()

    val avatarStyle = rememberRivoAvatarStyle()

    var searchResults by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var searchRecentNumbers by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }

    LaunchedEffect(number, allContacts, allCallLogs, t9Enabled) {
        if (number.isEmpty()) {
            searchResults = emptyList()
            searchRecentNumbers = emptyList()
        } else {
            val (contacts, recents) = withContext(Dispatchers.Default) {
                val cleanQuery = number.replace(" ", "")
                val cleanDigits = cleanQuery.filter { it.isDigit() }

                val matchedContacts = allContacts.asSequence()
                    .filter { contact ->
                        val matchesNumber = contact.phoneNumbers.any { it.replace(" ", "").contains(cleanQuery) }
                        val matchesName = t9Enabled && T9Matcher.isMatch(contact.name, cleanQuery)
                        val matchesNickname = t9Enabled && contact.nickname?.let { T9Matcher.isMatch(it, cleanQuery) } ?: false
                        matchesNumber || matchesName || matchesNickname
                    }
                    .take(50)
                    .toList()

                val savedNumbersSet = allContacts.flatMap { it.phoneNumbers }
                    .map { normalizePhoneNumber(it) }
                    .filter { it.isNotEmpty() }
                    .toSet()

                val matchedRecents = if (cleanDigits.isNotEmpty()) {
                    allCallLogs.asSequence()
                        .filter { entry ->
                            val entryClean = entry.number.replace(" ", "")
                            val entryDigits = entryClean.filter { it.isDigit() }
                            val norm = normalizePhoneNumber(entry.number)
                            val isUnsaved = (entry.contactId.isNullOrBlank() || entry.contactId == "0") &&
                                (entry.name.isNullOrBlank() || entry.name == entry.number) &&
                                !savedNumbersSet.contains(norm)
                            isUnsaved && (entryDigits.contains(cleanDigits) || entryClean.contains(cleanQuery))
                        }
                        .distinctBy { normalizePhoneNumber(it.number) }
                        .sortedWith(compareBy<CallLogEntry> { entry ->
                            val norm = normalizePhoneNumber(entry.number).filter { it.isDigit() }
                            if (norm.startsWith(cleanDigits)) 0 else 1
                        }.thenByDescending { it.date })
                        .take(20)
                        .toList()
                } else {
                    emptyList()
                }

                matchedContacts to matchedRecents
            }
            searchResults = contacts
            searchRecentNumbers = recents
        }
    }

    val performCall = { targetNumber: String, contactId: String? ->
        val cleanNumber = targetNumber.replace(" ", "")
        if (cleanNumber == "*#06#" ||
            (cleanNumber.startsWith("*#*#") && cleanNumber.endsWith("#*#*") && cleanNumber.length >= 9) ||
            (cleanNumber.startsWith("##") && cleanNumber.endsWith("#") && cleanNumber.length >= 4)) {
            val handled = com.grinch.rivo4.controller.util.processSecretCode(context, cleanNumber)
            if (handled) {
                textFieldValue = TextFieldValue("")
            } else {
                val contact = allContacts.find { it.id == contactId }
                callLauncher.dial(targetNumber, contact)
            }
        } else {
            val contact = allContacts.find { it.id == contactId }
            callLauncher.dial(targetNumber, contact)
            
            if (cleanNumber.startsWith("*") && cleanNumber.endsWith("#")) {
                textFieldValue = TextFieldValue("")
            }
        }
    }

    val dualSimButtonsEnabled by remember(settingsState) {
        mutableStateOf(prefs.isDualSimDialpadButtonsEnabled())
    }
    val telecomManager = remember(context) {
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }
    val phoneAccounts = remember(telecomManager, context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                telecomManager.callCapablePhoneAccounts
            } catch (e: SecurityException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val performCallWithSim = { targetNumber: String, handle: PhoneAccountHandle? ->
        val cleanNumber = targetNumber.replace(" ", "")
        if (cleanNumber.isNotEmpty()) {
            if (cleanNumber == "*#06#" ||
                (cleanNumber.startsWith("*#*#") && cleanNumber.endsWith("#*#*") && cleanNumber.length >= 9) ||
                (cleanNumber.startsWith("##") && cleanNumber.endsWith("#") && cleanNumber.length >= 4)
            ) {
                val handled = com.grinch.rivo4.controller.util.processSecretCode(context, cleanNumber)
                if (handled) {
                    textFieldValue = TextFieldValue("")
                } else {
                    com.grinch.rivo4.controller.util.makeCall(context, targetNumber, handle)
                }
            } else {
                com.grinch.rivo4.controller.util.makeCall(context, targetNumber, handle)
                if (cleanNumber.startsWith("*") && cleanNumber.endsWith("#")) {
                    textFieldValue = TextFieldValue("")
                }
            }
        }
    }

    CompositionLocalProvider(LocalRivoAvatarStyle provides avatarStyle) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                MenuTopAppBar(
                    text = stringResource(R.string.dialpad_title),
                    navigator = navigator,
                    actions = {
                        if (number.isNotEmpty()) {
                            Surface(
                                onClick = { showSocialDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = stringResource(R.string.label_social_apps),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize().padding(top =innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {

            if (number.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dialpadHeightDp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Dialpad,
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.dialpad_start_dialing),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.dialpad_start_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            val totalResults = searchResults.size + searchRecentNumbers.size
            if (totalResults > 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = dialpadHeightDp + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(searchResults, key = { _, contact -> "dialpad_contact_${contact.id}" }) { index, contact ->
                        val contactNumber = contact.phoneNumbers.firstOrNull()
                        val shape = rivoGroupedItemShape(index, totalResults)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            RivoListItem(
                                headline = com.grinch.rivo4.controller.util.ContactUtils.formatContactName(contact, displayOrder),
                                supporting = buildString {
                                    contact.nickname?.let { append("$it • ") }
                                    contactNumber?.let { append(formatPhoneNumber(it)) }
                                }.ifEmpty { null },
                                avatarName = contact.name,
                                photoUri = contact.photoUri,
                                trailingContent = {
                                    contactNumber?.let { num ->
                                        IconButton(
                                            onClick = { performCall(num, contact.id) },
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Call,
                                                contentDescription = stringResource(R.string.content_desc_call_named, contact.name),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    navigator.navigate(
                                        ContactDetailsScreenDestination(
                                            contactId = contact.id
                                        )
                                    )
                                }
                            )
                        }
                    }

                    itemsIndexed(searchRecentNumbers, key = { _, entry -> "dialpad_recent_${entry.id}_${entry.number}" }) { index, entry ->
                        val itemIndex = searchResults.size + index
                        val shape = rivoGroupedItemShape(itemIndex, totalResults)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            RivoListItem(
                                headline = formatPhoneNumber(entry.number),
                                supporting = buildString {
                                    append(stringResource(R.string.nav_recents))
                                    if (!entry.simLabel.isNullOrBlank()) {
                                        append(" • ")
                                        append(entry.simLabel)
                                    }
                                },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = { performCall(entry.number, null) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Call,
                                            contentDescription = stringResource(R.string.action_call),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    performCall(entry.number, null)
                                }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        dialpadHeightPx = coordinates.size.height
                    },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions.topPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InterceptPlatformTextInput(
                        interceptor = { _, _ ->
                            awaitCancellation()
                        }
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(
                                showKeyboardOnFocus = false,
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (number.isEmpty()) {
                                        Text(
                                            "",
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimensions.rowSpacing)
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("*", "0", "#")
                        )

                        val subKeys = mapOf(
                            "1" to "   ", "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
                            "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "0" to "+"
                        )

                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    DialPadKey(
                                        number = key,
                                        letters = subKeys[key] ?: "",
                                        toneGenerator = toneGenerator,
                                        context = context,
                                        dialpadStyle = dialpadStyle,
                                        dimensions = dimensions,
                                        onClick = onDigitClick,
                                        onLongClick = { digit ->
                                            if (digit == "0") {
                                                if (prefs.getBoolean(PreferenceManager.KEY_HOLD_ZERO_FOR_PLUS, true)) {
                                                    onDigitClick("+")
                                                }
                                            } else if (speedDialEnabled && number.isEmpty()) {
                                                val mapping = prefs.getString("speed_dial_$digit", null)
                                                val speedNumber = mapping?.split("|")?.getOrNull(1)
                                                if (speedNumber != null) {
                                                    callLauncher.dial(speedNumber, null)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(dimensions.rowSpacing))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = dimensions.bottomSheetBottomPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DialerActionExpressive(
                                    onClick = {
                                        showAddToContactSheet = true
                                    },
                                    icon = Icons.Default.PersonAdd,
                                    contentDescription = stringResource(R.string.action_add_contact),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(dimensions.actionButtonSize)
                                )
                            }

                            if (dualSimButtonsEnabled) {
                                val sim1Handle = phoneAccounts.getOrNull(0)
                                val sim2Handle = phoneAccounts.getOrNull(1)

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DialerSimActionExpressive(
                                        onClick = { performCallWithSim(number, sim1Handle) },
                                        simNumber = 1,
                                        modifier = Modifier.width(dimensions.simButtonWidth).height(dimensions.simButtonHeight)
                                    )
                                    DialerSimActionExpressive(
                                        onClick = { performCallWithSim(number, sim2Handle) },
                                        simNumber = 2,
                                        modifier = Modifier.width(dimensions.simButtonWidth).height(dimensions.simButtonHeight)
                                    )
                                }
                            } else {
                                DialerActionExpressive(
                                    onClick = { performCall(number, null) },
                                    icon = Icons.Default.Call,
                                    contentDescription = stringResource(R.string.action_call),
                                    containerColor = MaterialTheme.callColors.answer,
                                    contentColor = MaterialTheme.callColors.onAnswer,
                                    modifier = Modifier.width(dimensions.callButtonWidth).height(dimensions.callButtonHeight),
                                    isLarge = true
                                )
                            }

                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DialerActionExpressive(
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        textFieldValue = TextFieldValue("")
                                    },
                                    onClick = {
                                        if (number.isNotEmpty()) {
                                            val selection = textFieldValue.selection
                                            if (selection.collapsed) {
                                                if (selection.start > 0) {
                                                    val newText = number.substring(0, selection.start - 1) + number.substring(selection.start)
                                                    textFieldValue = TextFieldValue(newText, TextRange(selection.start - 1))
                                                }
                                            } else {
                                                val newText = number.substring(0, selection.start) + number.substring(selection.end)
                                                textFieldValue = TextFieldValue(newText, TextRange(selection.start))
                                            }
                                        }
                                    },
                                    icon = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = stringResource(R.string.content_desc_backspace),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(dimensions.actionButtonSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSocialDialog) {
        RivoDialog(
            onDismissRequest = { showSocialDialog = false },
            title = stringResource(R.string.dialpad_connect_via_social),
            icon = Icons.AutoMirrored.Filled.Chat,
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_close),
                onClick = { showSocialDialog = false }
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivoExpressiveButton(
                    painter = rememberAsyncImagePainter("file:///android_asset/icons/whatsapp.png"),
                    label = stringResource(R.string.brand_whatsapp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    size = 52.dp,
                    iconSize = 32.dp,
                    onClick = {
                        SocialUtils.openWhatsApp(context, number)
                        showSocialDialog = false
                    }
                )
                RivoExpressiveButton(
                    painter = rememberAsyncImagePainter("file:///android_asset/icons/telegram.png"),
                    label = stringResource(R.string.brand_telegram),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    size = 52.dp,
                    iconSize = 32.dp,
                    onClick = {
                        SocialUtils.openTelegram(context, number)
                        showSocialDialog = false
                    }
                )
                RivoExpressiveButton(
                    painter = rememberAsyncImagePainter("file:///android_asset/icons/signal.png"),
                    label = stringResource(R.string.brand_signal),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    size = 52.dp,
                    iconSize = 32.dp,
                    onClick = {
                        SocialUtils.openSignal(context, number)
                        showSocialDialog = false
                    }
                )
            }
        }
    }

    if (showAddToContactSheet) {
        AddToContactBottomSheet(
            phoneNumber = number,
            onDismissRequest = { showAddToContactSheet = false },
            onCreateNewContact = {
                navigator.navigate(
                    ContactEditScreenDestination(
                        initialPhone = number
                    )
                )
            },
            onAddToExistingContact = {
                navigator.navigate(
                    ContactSelectionScreenDestination(
                        title = "Add to Existing Contact",
                        initialPhoneToAssign = number
                    )
                )
            }
        )
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerActionExpressive(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    modifier: Modifier = Modifier.size(64.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isLarge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val actionCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else (if (isLarge) 32.dp else 24.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "ActionCorner"
    )

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(actionCornerRadius),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isLarge) 6.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (isLarge) 32.dp else 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerSimActionExpressive(
    onClick: () -> Unit,
    simNumber: Int,
    modifier: Modifier = Modifier.width(76.dp).height(68.dp),
    containerColor: Color = if (simNumber == 1) MaterialTheme.callColors.answer else MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = if (simNumber == 1) MaterialTheme.callColors.onAnswer else MaterialTheme.colorScheme.onPrimaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val simCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 24.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SimCorner"
    )

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(simCornerRadius),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$simNumber",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadKey(
    number: String,
    letters: String,
    toneGenerator: ToneGenerator,
    context: Context,
    dialpadStyle: Int = PreferenceManager.DIALPAD_STYLE_MODERN,
    dimensions: DialpadDimensions = DialpadDimensions.forSize(PreferenceManager.DIALPAD_SIZE_MEDIUM),
    onClick: (String) -> Unit,
    onLongClick: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current

    val modernCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 28.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "ModernKeyCorner"
    )

    val outlinedCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "OutlinedKeyCorner"
    )

    val circleCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else (dimensions.circleKeySize / 2),
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "CircleKeyCorner"
    )

    val organicMorphProgress by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 0.2f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "OrganicKeyMorph"
    )

    val keyShape: androidx.compose.ui.graphics.Shape = when (dialpadStyle) {
        PreferenceManager.DIALPAD_STYLE_CIRCLE -> {
            RoundedCornerShape(circleCornerRadius)
        }
        PreferenceManager.DIALPAD_STYLE_ORGANIC -> {
            rememberRivoMorphShape(RivoMaterialShapes.Cookie9Sided, RivoMaterialShapes.Circle) { organicMorphProgress }
        }
        PreferenceManager.DIALPAD_STYLE_OUTLINED -> {
            RoundedCornerShape(outlinedCornerRadius)
        }
        PreferenceManager.DIALPAD_STYLE_UNIFIED -> {
            RoundedCornerShape(if (isPressed) 8.dp else 14.dp)
        }
        PreferenceManager.DIALPAD_STYLE_MINIMAL -> {
            RoundedCornerShape(if (isPressed) 16.dp else 24.dp)
        }
        else -> {
            RoundedCornerShape(modernCornerRadius)
        }
    }

    val containerColor: Color = when (dialpadStyle) {
        PreferenceManager.DIALPAD_STYLE_CIRCLE -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
        }
        PreferenceManager.DIALPAD_STYLE_ORGANIC -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        }
        PreferenceManager.DIALPAD_STYLE_OUTLINED -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
        }
        PreferenceManager.DIALPAD_STYLE_UNIFIED -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
        }
        PreferenceManager.DIALPAD_STYLE_MINIMAL -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
        }
        else -> {
            if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        }
    }

    val contentColor: Color = when (dialpadStyle) {
        PreferenceManager.DIALPAD_STYLE_MINIMAL -> {
            if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        }
        else -> {
            if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        }
    }

    val border: BorderStroke? = when (dialpadStyle) {
        PreferenceManager.DIALPAD_STYLE_OUTLINED -> {
            BorderStroke(
                width = if (isPressed) 2.dp else 1.5.dp,
                color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        }
        PreferenceManager.DIALPAD_STYLE_ORGANIC -> {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        }
        PreferenceManager.DIALPAD_STYLE_UNIFIED -> {
            BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
        else -> null
    }

    val keyModifier = when (dialpadStyle) {
        PreferenceManager.DIALPAD_STYLE_CIRCLE,
        PreferenceManager.DIALPAD_STYLE_ORGANIC -> {
            Modifier.size(dimensions.circleKeySize)
        }
        else -> {
            Modifier.size(width = dimensions.keyWidth, height = dimensions.keyHeight)
        }
    }

    Surface(
        modifier = keyModifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, true)) {
                        playDtmf(number, toneGenerator)
                    }
                    if (prefs.getBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, true)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick(number)
                },
                onLongClick = onLongClick?.let {
                    {
                        if (prefs.getBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, true)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        it(number)
                    }
                }
            ),
        shape = keyShape,
        color = containerColor,
        border = border,
        tonalElevation = if (dialpadStyle == PreferenceManager.DIALPAD_STYLE_MODERN || dialpadStyle == PreferenceManager.DIALPAD_STYLE_CIRCLE) 1.dp else 0.dp
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = dimensions.numberFontSize,
                    lineHeight = dimensions.numberFontSize
                ),
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (letters.isNotBlank()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = dimensions.lettersFontSize,
                        letterSpacing = 2.sp
                    ),
                    color = if (isPressed) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

object T9Matcher {
    fun isMatch(contactName: String, query: String): Boolean {
        if (query.isEmpty()) return false

        val startIndices = mutableListOf(0)
        for (i in 0 until contactName.length - 1) {
            val c = contactName[i]
            if (c == ' ' || c == '-' || c == '.' || c == '_') {
                startIndices.add(i + 1)
            }
        }

        for (startIndex in startIndices) {
            var qIdx = 0
            var nIdx = startIndex

            while (qIdx < query.length && nIdx < contactName.length) {
                val nC = contactName[nIdx]
                if (nC == ' ' || nC == '-' || nC == '.' || nC == '_') {
                    nIdx++
                    continue
                }

                if (charToT9(nC) != query[qIdx]) {
                    break
                }
                qIdx++
                nIdx++
            }

            if (qIdx == query.length) return true
        }

        return false
    }

    private fun charToT9(c: Char): Char = when (c.uppercaseChar()) {
        'A', 'B', 'C' -> '2'
        'D', 'E', 'F' -> '3'
        'G', 'H', 'I' -> '4'
        'J', 'K', 'L' -> '5'
        'M', 'N', 'O' -> '6'
        'P', 'Q', 'R', 'S' -> '7'
        'T', 'U', 'V' -> '8'
        'W', 'X', 'Y', 'Z' -> '9'
        '0' -> '0'
        '1' -> '1'
        '2' -> '2'
        '3' -> '3'
        '4' -> '4'
        '5' -> '5'
        '6' -> '6'
        '7' -> '7'
        '8' -> '8'
        '9' -> '9'
        else -> ' '
    }
}

private fun playDtmf(key: String, toneGenerator: ToneGenerator) {
    val toneType = when (key) {
        "1" -> ToneGenerator.TONE_DTMF_1
        "2" -> ToneGenerator.TONE_DTMF_2
        "3" -> ToneGenerator.TONE_DTMF_3
        "4" -> ToneGenerator.TONE_DTMF_4
        "5" -> ToneGenerator.TONE_DTMF_5
        "6" -> ToneGenerator.TONE_DTMF_6
        "7" -> ToneGenerator.TONE_DTMF_7
        "8" -> ToneGenerator.TONE_DTMF_8
        "9" -> ToneGenerator.TONE_DTMF_9
        "0" -> ToneGenerator.TONE_DTMF_0
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> -1
    }
    if (toneType != -1) {
        toneGenerator.startTone(toneType, 120)
    }
}
