package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.SimPickerDialog
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.components.tiles.SingleTile
import com.grinch.rivo4.view.components.tiles.TileGroup
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var number by remember { mutableStateOf(initialNumber ?: "") }

    BackHandler(enabled = number.isNotEmpty()) {
        number = ""
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

    var showSimPicker by remember { mutableStateOf(false) }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    val searchResults by remember(number, allContacts, t9Enabled) {
        derivedStateOf {
            if (number.isEmpty()) emptyList()
            else {
                allContacts.filter { contact ->
                    val matchesNumber = contact.phoneNumbers.any { it.replace(" ", "").contains(number) }
                    val matchesName = t9Enabled && T9Matcher.isMatch(contact.name, number)
                    matchesNumber || matchesName
                }
            }.take(5)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (hasPhoneState) {
                val accounts = telecomManager.callCapablePhoneAccounts
                if (accounts.size > 1) {
                    showSimPicker = true
                } else {
                    makeCall(context, number)
                }
            } else {
                makeCall(context, number)
            }
        }
    }

    if (showSimPicker) {
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, number, handle)
                showSimPicker = false
            }
        )
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (searchResults.isNotEmpty()) {
                    TileGroup(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        searchResults.forEach { contact ->
                            SingleTile(
                                title = contact.name,
                                subtitle = contact.phoneNumbers.firstOrNull(),
                                photoUri = contact.photoUri,
                                onClick = {
                                    navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 4.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                onClick = { digit -> number += digit },
                                onLongClick = { digit ->
                                    if (speedDialEnabled && number.isEmpty()) {
                                        val mapping = prefs.getString("speed_dial_$digit", null)
                                        val speedNumber = mapping?.split("|")?.getOrNull(1)
                                        if (speedNumber != null) {
                                            makeCall(context, speedNumber)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        if (number.isNotEmpty()) {
                            DialerActionExpressive(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        type = ContactsContract.RawContacts.CONTENT_TYPE
                                        putExtra(ContactsContract.Intents.Insert.PHONE, number)
                                    }
                                    context.startActivity(intent)
                                },
                                icon = Icons.Default.PersonAdd,
                                contentDescription = "Add Contact",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }

                    DialerActionExpressive(
                        onClick = {
                            if (number.isNotEmpty()) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                        val accounts = telecomManager.callCapablePhoneAccounts
                                        if (accounts.size > 1) {
                                            showSimPicker = true
                                        } else {
                                            makeCall(context, number)
                                        }
                                    } else {
                                        makeCall(context, number)
                                    }
                                } else {
                                    callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                                }
                            }
                        },
                        icon = Icons.Default.Call,
                        contentDescription = "Call",
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        modifier = Modifier.width(100.dp).height(72.dp),
                        isLarge = true
                    )

                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        if (number.isNotEmpty()) {
                            DialerActionExpressive(
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    number = ""
                                },
                                onClick = { number = number.dropLast(1) },
                                icon = Icons.Default.Backspace,
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
    onClick: (String) -> Unit,
    onLongClick: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val haptic = LocalHapticFeedback.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 32.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonShapeAnimation"
    )

    Surface(
        modifier = Modifier
            .size(width = 100.dp, height = 72.dp)
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (letters.isNotBlank()) {
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
        
        val normalizedName = contactName.uppercase(Locale.getDefault())
        val t9Name = convertNameToT9(normalizedName)
        
        // Find indices of start of words
        val startOfWordIndices = mutableListOf(0)
        for (i in 0 until normalizedName.length - 1) {
            if (normalizedName[i] == ' ' || normalizedName[i] == '-' || normalizedName[i] == '.') {
                startOfWordIndices.add(i + 1)
            }
        }
        
        for (index in startOfWordIndices) {
            if (index >= t9Name.length) continue
            val suffix = t9Name.substring(index).replace(Regex("[^0-9]"), "")
            if (suffix.startsWith(query)) return true
        }
        
        return false
    }

    private fun convertNameToT9(name: String): String {
        return name.map { char ->
            when (char) {
                'A', 'B', 'C' -> '2'
                'D', 'E', 'F' -> '3'
                'G', 'H', 'I' -> '4'
                'J', 'K', 'L' -> '5'
                'M', 'N', 'O' -> '6'
                'P', 'Q', 'R', 'S' -> '7'
                'T', 'U', 'V' -> '8'
                'W', 'X', 'Y', 'Z' -> '9'
                '0', '+' -> '0'
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
        }.joinToString("")
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
