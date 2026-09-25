package com.grinch.rivo4.view.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AZListScroll(
    contacts: List<Contact>,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    grouped: Map<Char, List<Contact>>? = null,
    header: (@Composable () -> Unit)? = null
) {
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticScrollEnabled = prefs.getBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)
    val displayOrder = remember(settingsState) { prefs.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0) }

    val context = LocalContext.current
    val callLauncher = rememberCallLauncher()
    val messageLauncher = rememberMessageLauncher()
    val videoLauncher = rememberVideoLauncher()
    val clipboardManager = LocalClipboardManager.current
    val swipeEnabled = remember(settingsState) { prefs.isSwipeActionsEnabled() } && selectedIds.isEmpty()
    val swipeRightAction = remember(settingsState) { SwipeActionType.fromId(prefs.getSwipeRightAction()) }
    val swipeLeftAction = remember(settingsState) { SwipeActionType.fromId(prefs.getSwipeLeftAction()) }

    if (hapticScrollEnabled) {
        LaunchedEffect(listState.firstVisibleItemIndex) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    val finalGrouped = remember(contacts, grouped) {
        if (grouped != null) return@remember grouped
        
        val mainGroups = contacts.groupBy {
            val firstChar = it.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toMutableMap()

        val finalMap = linkedMapOf<Char, List<Contact>>()

        mainGroups.keys.filter { it.isLetter() }.sorted().forEach { char ->
            finalMap[char] = mainGroups[char]!!
        }

        val hashGroup = mainGroups['#']
        if (hashGroup != null) finalMap['#'] = hashGroup

        finalMap
    }

    val alphabetIndices = remember(finalGrouped, header != null) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = if (header != null) 1 else 0
        finalGrouped.entries.forEachIndexed { groupIndex, (char, contactsForChar) ->
            map[char] = currentIndex
            // 1 for section header
            currentIndex += 1
            // 1 for each contact
            currentIndex += contactsForChar.size
            // 1 for banner ad if groupIndex % 3 == 1
            if (groupIndex % 3 == 1) {
                currentIndex += 1
            }
        }
        map
    }

    val scope = rememberCoroutineScope()
    var draggingChar by remember { mutableStateOf<Char?>(null) }

    val scrollingChar by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            alphabetIndices.entries
                .filter { it.value <= firstVisible }
                .maxByOrNull { it.value }
                ?.key ?: alphabetIndices.keys.firstOrNull()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (header != null) {
                item(key = "header_top") {
                    header()
                }
            }

            finalGrouped.entries.forEachIndexed { groupIndex, (initial, contactsForChar) ->
                item(key = "header_$initial", contentType = "header") {
                    RivoSectionHeader(
                        title = initial.toString(),
                        modifier = Modifier.padding(
                            top = if (groupIndex == 0 && header == null) 4.dp else 16.dp,
                            bottom = 4.dp
                        )
                    )
                }

                itemsIndexed(
                    items = contactsForChar,
                    key = { _, contact -> contact.id },
                    contentType = { _, _ -> "contact" }
                ) { index, contact ->
                    val isFirst = index == 0
                    val isLast = index == contactsForChar.size - 1
                    val isSingle = contactsForChar.size == 1

                    val shape = when {
                        isSingle -> RoundedCornerShape(20.dp)
                        isFirst -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(shape)
                            .background(
                                if (selectedIds.contains(contact.id))
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerLow
                            )
                    ) {
                        val displayName = if (contact.name.isNotBlank()) {
                            ContactUtils.formatContactName(contact, displayOrder)
                        } else {
                            contact.phoneNumbers.firstOrNull()?.let { formatPhoneNumber(it) } ?: stringResource(R.string.label_unknown)
                        }

                        RivoSwipeToActionBox(
                            enabled = swipeEnabled,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            onTriggerAction = { action ->
                                val phone = contact.phoneNumbers.firstOrNull().orEmpty()
                                when (action) {
                                    SwipeActionType.CALL -> callLauncher.dial(phone, contact)
                                    SwipeActionType.MESSAGE -> messageLauncher.sendMessage(phone, contact)
                                    SwipeActionType.VIDEO_CALL -> videoLauncher.startVideoCall(phone, contact)
                                    SwipeActionType.WHATSAPP -> SocialUtils.openWhatsApp(context, phone)
                                    SwipeActionType.COPY_NUMBER -> {
                                        if (phone.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(phone))
                                            Toast.makeText(context, context.getString(R.string.number_copied_toast), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    SwipeActionType.DELETE -> {}
                                    SwipeActionType.NONE -> {}
                                }
                            }
                        ) {
                            RivoListItem(
                                headline = displayName,
                                supporting = null,
                                avatarName = contact.name,
                                photoUri = contact.photoUri,
                                trailingContent = {
                                    if (contact.isHidden) {
                                        Icon(
                                            imageVector = Icons.Outlined.VisibilityOff,
                                            contentDescription = "Private Storage (Hidden)",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (contact.isPrivate) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = "Private Storage",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        onToggleSelection(contact.id)
                                    } else {
                                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                    }
                                },
                                onLongClick = {
                                    onToggleSelection(contact.id)
                                },
                                selected = selectedIds.contains(contact.id),
                                isCompact = false
                            )
                        }
                    }
                }

                if (groupIndex % 3 == 1) {
                    item(key = "ad_$groupIndex") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            com.grinch.rivo4.view.components.ad.BannerAd()
                        }
                    }
                }
            }
        }

        AlphabetSideBar(
            alphabet = alphabetIndices.keys.toList(),
            selectedChar = draggingChar ?: scrollingChar,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            onLetterSelected = { char ->
                draggingChar = char
                val index = alphabetIndices[char] ?: return@AlphabetSideBar
                scope.launch { listState.scrollToItem(index) }
            },
            onDragEnd = { draggingChar = null }
        )

        if (draggingChar != null) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = draggingChar.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun AlphabetSideBar(
    alphabet: List<Char>,
    selectedChar: Char?,
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit,
    onDragEnd: () -> Unit
) {
    var columnHeight by remember { mutableStateOf(0) }
    
    Surface(
        modifier = modifier
            .width(24.dp)
            .wrapContentHeight()
            .onGloballyPositioned { columnHeight = it.size.height }
            .pointerInput(alphabet) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (columnHeight > 0) {
                            val itemHeight = columnHeight.toFloat() / alphabet.size
                            val index = (offset.y / itemHeight).toInt()
                            val char = alphabet.getOrNull(index.coerceIn(0, alphabet.lastIndex))
                            if (char != null) onLetterSelected(char)
                        }
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, _ ->
                    if (columnHeight > 0) {
                        val itemHeight = columnHeight.toFloat() / alphabet.size
                        val index = (change.position.y / itemHeight).toInt()
                        val char = alphabet.getOrNull(index.coerceIn(0, alphabet.lastIndex))
                        if (char != null) onLetterSelected(char)
                    }
                }
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                val isSelected = char == selectedChar

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
