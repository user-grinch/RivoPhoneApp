package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.awaitCancellation
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
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialNumber ?: "")) }
    val number = textFieldValue.text

    var showSocialDialog by remember { mutableStateOf(false) }

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
    val dialpadStyle by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_STYLE, 0))
    }
    val dialpadLayout by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_DIALPAD_LAYOUT, 0))
    }
    val displayOrder by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0))
    }

    val callLauncher = rememberCallLauncher()

    val searchResults by remember(number, allContacts, t9Enabled) {
        derivedStateOf {
            if (number.isEmpty()) emptyList()
            else {
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
        val contact = allContacts.find { it.id == contactId }
        callLauncher.dial(targetNumber, contact)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Dialpad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (number.isNotEmpty()) {
                        IconButton(onClick = { showSocialDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Social Apps")
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
                        "Start Dialing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enter a name or number to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 420.dp)
                ) {
                    itemsIndexed(searchResults) { index, contact ->
                        val contactNumber = contact.phoneNumbers.firstOrNull()
                        val isFirst = index == 0
                        val isLast = index == searchResults.size - 1

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
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        RivoListItem(
                                            headline = com.grinch.rivo4.controller.util.ContactUtils.formatContactName(contact.name, displayOrder),
                                            supporting = buildString {
                                                contact.nickname?.let { append("$it • ") }
                                                contactNumber?.let { append(formatPhoneNumber(it)) }
                                            }.ifEmpty { null },
                                            avatarName = contact.name,
                                            photoUri = contact.photoUri,
                                            onClick = {
                                                navigator.navigate(
                                                    ContactDetailsScreenDestination(
                                                        contactId = contact.id
                                                    )
                                                )
                                            }
                                        )
                                    }
                                    contactNumber?.let { num ->
                                        IconButton(
                                            onClick = { performCall(num, contact.id) },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Call,
                                                contentDescription = "Call ${contact.name}",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                if (!isLast) {
                                    RivoDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(
                        when (dialpadLayout) {
                            1 -> Alignment.BottomStart
                            2 -> Alignment.BottomEnd
                            else -> Alignment.BottomCenter
                        }
                    )
                    .fillMaxWidth(if (dialpadLayout != 0) 0.85f else 1f),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(
                    topStart = 36.dp,
                    topEnd = 36.dp,
                    bottomEnd = if (dialpadLayout == 1) 36.dp else 0.dp,
                    bottomStart = if (dialpadLayout == 2) 36.dp else 0.dp
                )
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
                                        style = dialpadStyle,
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
                            // Left side actions
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DialerActionExpressive(
                                    onClick = {
                                        navigator.navigate(
                                            ContactEditScreenDestination(
                                                initialPhone = number
                                            )
                                        )
                                    },
                                    icon = Icons.Default.PersonAdd,
                                    contentDescription = "Add Contact",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }

                            // Centered Dial Button
                            DialerActionExpressive(
                                onClick = { performCall(number, null) },
                                icon = Icons.Default.Call,
                                contentDescription = "Call",
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White,
                                modifier = Modifier.width(100.dp).height(72.dp),
                                isLarge = true
                            )

                            // Right side actions
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
                                    contentDescription = "Backspace",
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
            title = "Connect via Social",
            icon = Icons.AutoMirrored.Filled.Chat,
            dismissButton = {
                TextButton(
                    onClick = { showSocialDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Close")
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivoExpressiveButton(
                    painter = rememberAsyncImagePainter("file:///android_asset/icons/whatsapp.png"),
                    label = "WhatsApp",
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
                    label = "Telegram",
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
                    label = "Signal",
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

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (if (isLarge) 20.dp else 16.dp) else (if (isLarge) 28.dp else 24.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonShape"
    )

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 36.dp else 24.dp))
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
    style: Int = 0,
    onClick: (String) -> Unit,
    onLongClick: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current

    val cornerRadius by animateDpAsState(
        targetValue = when (style) {
            1 -> 50.dp
            2 -> 0.dp
            else -> if (isPressed) 16.dp else 32.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonShapeAnimation"
    )

    val containerColor = when (style) {
        2 -> if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val keyModifier = if (style == 1) {
        Modifier.size(72.dp)
    } else {
        Modifier.size(width = 100.dp, height = 64.dp)
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
                onLongClick = onLongClick?.let { { it(number) } }
            ),
        shape = if (style == 1) CircleShape else RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = if (style == 2 && !isPressed) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = number,
                style = if (style == 1) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (letters.isNotBlank() && style != 2) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
