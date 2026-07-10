package com.grinch.rivo4.view.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.modal.data.Contact
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
    grouped: Map<Char, List<Contact>>? = null
) {
    val prefs = org.koin.compose.koinInject<com.grinch.rivo4.controller.util.PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticScrollEnabled = prefs.getBoolean(PreferenceManager.KEY_HAPTIC_LIST_SCROLL, false)
    val displayOrder = remember(settingsState) { prefs.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0) }

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

    val alphabetIndices = remember(finalGrouped) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = 0
        finalGrouped.forEach { (char, _) ->
            map[char] = currentIndex
            currentIndex += 2 
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
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            finalGrouped.forEach { (initial, contactsForChar) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RivoExpressiveCard(isCompact = true) {
                            contactsForChar.forEachIndexed { index, contact ->
                                val displayName = ContactUtils.formatContactName(
                                    contact.name.ifEmpty {
                                        contact.phoneNumbers.firstOrNull()?.let { formatPhoneNumber(it) } ?: "Unknown"
                                    },
                                    displayOrder
                                )
                                RivoListItem(
                                    headline = displayName,
                                    supporting = null,
                                    avatarName = contact.name,
                                    photoUri = contact.photoUri,
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
                                    isCompact = true
                                )
                                if (index < contactsForChar.size - 1) {
                                    RivoDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
