package com.grinch.rivo4.view.screen

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
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.*
import com.grinch.rivo4.view.theme.callColors
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
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val allContacts by contactsVM.allContacts.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialNumber ?: "")) }
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

    LaunchedEffect(number, allContacts, t9Enabled) {
        if (number.isEmpty()) {
            searchResults = emptyList()
        } else {
            searchResults = withContext(Dispatchers.Default) {
                val cleanQuery = number.replace(" ", "")
                allContacts.asSequence()
                    .filter { contact ->
                        val matchesNumber = contact.phoneNumbers.any { it.replace(" ", "").contains(cleanQuery) }
                        val matchesName = t9Enabled && T9Matcher.isMatch(contact.name, cleanQuery)
                        val matchesNickname = t9Enabled && contact.nickname?.let { T9Matcher.isMatch(it, cleanQuery) } ?: false
                        matchesNumber || matchesName || matchesNickname
                    }
                    .take(50)
                    .toList()
            }
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
            TopAppBar(
                title = { Text(stringResource(R.string.dialpad_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (number.isNotEmpty()) {
                        IconButton(onClick = { showSocialDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.label_social_apps))
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 420.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(36.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Dialpad,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.dialpad_start_dialing),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.dialpad_start_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(searchResults, key = { _, contact -> "dialpad_contact_${contact.id}" }) { index, contact ->
                        val contactNumber = contact.phoneNumbers.firstOrNull()
                        val shape = rivoGroupedItemShape(index, searchResults.size)

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
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        onClick = onDigitClick,
                                        onLongClick = { digit ->
                                            if (speedDialEnabled && number.isEmpty()) {
                                                val mapping = prefs.getString("speed_dial_$digit", null)
                                                val speedNumber = mapping?.split("|")?.getOrNull(1)
                                                if (speedNumber != null) {
                                                    callLauncher.dial(speedNumber, null)
                                                }
                                            } else if (digit == "0") {
                                                onDigitClick("+")
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }

                            if (dualSimButtonsEnabled) {
                                val sim1Handle = phoneAccounts.getOrNull(0)
                                val sim2Handle = phoneAccounts.getOrNull(1)
                                val sim1Account = sim1Handle?.let { runCatching { telecomManager.getPhoneAccount(it) }.getOrNull() }
                                val sim2Account = sim2Handle?.let { runCatching { telecomManager.getPhoneAccount(it) }.getOrNull() }
                                val sim1Label = sim1Account?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM 1"
                                val sim2Label = sim2Account?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM 2"

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DialerSimActionExpressive(
                                        onClick = { performCallWithSim(number, sim1Handle) },
                                        simNumber = 1,
                                        simLabel = sim1Label
                                    )
                                    DialerSimActionExpressive(
                                        onClick = { performCallWithSim(number, sim2Handle) },
                                        simNumber = 2,
                                        simLabel = sim2Label
                                    )
                                }
                            } else {
                                DialerActionExpressive(
                                    onClick = { performCall(number, null) },
                                    icon = Icons.Default.Call,
                                    contentDescription = stringResource(R.string.action_call),
                                    containerColor = MaterialTheme.callColors.answer,
                                    contentColor = MaterialTheme.callColors.onAnswer,
                                    modifier = Modifier.width(100.dp).height(72.dp),
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ButtonScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (if (isLarge) 24.dp else 20.dp) else (if (isLarge) 34.dp else 30.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ButtonShape"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(cornerRadius),
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
    simLabel: String,
    modifier: Modifier = Modifier.width(76.dp).height(68.dp),
    containerColor: Color = if (simNumber == 1) MaterialTheme.callColors.answer else MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = if (simNumber == 1) MaterialTheme.callColors.onAnswer else MaterialTheme.colorScheme.onPrimaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SimButtonScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 28.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SimButtonShape"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$simNumber",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = contentColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = simLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
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
    onClick: (String) -> Unit,
    onLongClick: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 28.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "KeyCornerRadius"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "KeyScale"
    )

    Surface(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .scale(scale)
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
                onLongClick = onLongClick?.let { { it(number) } }
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotBlank()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
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
