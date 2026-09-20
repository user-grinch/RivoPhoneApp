package com.grinch.rivo4.view.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.*
import com.grinch.rivo4.view.theme.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactSelectionScreen(
    navigator: DestinationsNavigator,
    resultNavigator: ResultBackNavigator<String>,
    title: String = "Select Contact",
    isMultiSelect: Boolean = false,
    actionButtonText: String = "Select",
    returnContactId: Boolean = false,
    initialPhoneToAssign: String? = null
) {
    val view = LocalView.current
    val viewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by viewModel.allContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val roundness = LocalCardRoundness.current
    val avatarStyle = rememberRivoAvatarStyle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) }
    var selectedContactIds by remember { mutableStateOf(setOf<String>()) }
    var selectedPhoneNumbers by remember { mutableStateOf(setOf<String>()) }
    var pendingMultiNumberContact by remember { mutableStateOf<Contact?>(null) }

    val heroCornerDp = rivoCornerDp(RivoShapeDefaults.BaseExtraLarge, roundness)
    val itemCornerDp = rivoCornerDp(RivoShapeDefaults.BaseLarge, roundness)
    val logoMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.25f }

    LaunchedEffect(Unit) {
        viewModel.fetchContacts()
    }

    val filteredContacts = remember(allContacts, searchQuery, selectedFilterTab) {
        var list = when (selectedFilterTab) {
            1 -> allContacts.filter { it.isFavorite }
            2 -> allContacts.filter { it.isPrivate }
            else -> allContacts
        }
        if (searchQuery.isNotBlank()) {
            val cleanSearch = searchQuery.trim()
            list = list.filter {
                it.name.contains(cleanSearch, ignoreCase = true) ||
                it.phoneNumbers.any { num -> num.contains(cleanSearch) }
            }
        }
        list.sortedBy { it.name.lowercase() }
    }

    val groupedContacts = remember(filteredContacts) {
        filteredContacts.groupBy { contact ->
            val first = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (first in 'A'..'Z') first.toString() else "#"
        }
    }

    val totalSelectedCount = maxOf(selectedContactIds.size, selectedPhoneNumbers.size)

    CompositionLocalProvider(LocalRivoAvatarStyle provides avatarStyle) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isMultiSelect && totalSelectedCount > 0) {
                                "$totalSelectedCount selected"
                            } else {
                                title
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.navigate(ContactEditScreenDestination(initialPhone = initialPhoneToAssign)) }) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = stringResource(R.string.contact_create_new))
                        }
                        if (isMultiSelect && totalSelectedCount > 0) {
                            IconButton(onClick = {
                                selectedContactIds = emptySet()
                                selectedPhoneNumbers = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (isMultiSelect && totalSelectedCount > 0) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val csvResult = if (returnContactId) {
                                selectedContactIds.joinToString(",")
                            } else {
                                selectedPhoneNumbers.joinToString(",")
                            }
                            resultNavigator.navigateBack(result = csvResult)
                        },
                        icon = { Icon(Icons.Default.Check, contentDescription = null) },
                        text = { Text("$actionButtonText ($totalSelectedCount)", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            if (isLoading && allContacts.isEmpty()) {
                RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hero Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(heroCornerDp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        modifier = Modifier.size(50.dp),
                                        shape = logoMorph,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (initialPhoneToAssign != null) Icons.Outlined.PersonSearch else Icons.Outlined.Contacts,
                                                contentDescription = null,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (initialPhoneToAssign != null) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Phone,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = formatPhoneNumber(initialPhoneToAssign),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${allContacts.size} contacts available",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (initialPhoneToAssign != null) {
                                    Spacer(Modifier.height(14.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            navigator.navigate(ContactEditScreenDestination(initialPhone = initialPhoneToAssign))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(itemCornerDp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.contact_create_new),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search Bar
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(heroCornerDp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_contacts_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.action_clear),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RivoFilterChip(
                                label = "All (${allContacts.size})",
                                selected = selectedFilterTab == 0,
                                onClick = { selectedFilterTab = 0 },
                                leadingIcon = { Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            val favCount = remember(allContacts) { allContacts.count { it.isFavorite } }
                            RivoFilterChip(
                                label = "Favorites ($favCount)",
                                selected = selectedFilterTab == 1,
                                onClick = { selectedFilterTab = 1 },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            val privCount = remember(allContacts) { allContacts.count { it.isPrivate } }
                            if (privCount > 0) {
                                RivoFilterChip(
                                    label = "Private ($privCount)",
                                    selected = selectedFilterTab == 2,
                                    onClick = { selectedFilterTab = 2 },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    val cleanQuery = searchQuery.trim()
                    val isPhoneQuery = cleanQuery.any { it.isDigit() } || cleanQuery.startsWith("+")

                    if (isPhoneQuery) {
                        item {
                            val isCustomSelected = selectedPhoneNumbers.contains(cleanQuery)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(itemCornerDp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        if (isMultiSelect) {
                                            selectedPhoneNumbers = if (isCustomSelected) {
                                                selectedPhoneNumbers - cleanQuery
                                            } else {
                                                selectedPhoneNumbers + cleanQuery
                                            }
                                        } else {
                                            resultNavigator.navigateBack(result = cleanQuery)
                                        }
                                    },
                                shape = RoundedCornerShape(itemCornerDp),
                                color = if (isCustomSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = if (isCustomSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isMultiSelect) {
                                        Checkbox(
                                            checked = isCustomSelected,
                                            onCheckedChange = { checked ->
                                                selectedPhoneNumbers = if (checked) {
                                                    selectedPhoneNumbers + cleanQuery
                                                } else {
                                                    selectedPhoneNumbers - cleanQuery
                                                }
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Dialpad, contentDescription = null, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Use number: $cleanQuery",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Tap to select this custom number",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filteredContacts.isEmpty() && !isPhoneQuery) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(heroCornerDp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.PersonSearch,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.search_no_results_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.search_no_results_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (initialPhoneToAssign != null) {
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                navigator.navigate(ContactEditScreenDestination(initialPhone = initialPhoneToAssign))
                                            },
                                            shape = RoundedCornerShape(itemCornerDp)
                                        ) {
                                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.contact_create_new))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        groupedContacts.forEach { (initial, contactsInGroup) ->
                            item(key = "header_$initial") {
                                RivoSectionHeader(
                                    title = initial,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }

                            item(key = "group_$initial") {
                                RivoExpressiveCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    contactsInGroup.forEachIndexed { index, contact ->
                                        val primaryNumber = contact.phoneNumbers.firstOrNull() ?: ""
                                        val isSelected = if (isMultiSelect) {
                                            selectedContactIds.contains(contact.id) || selectedPhoneNumbers.contains(primaryNumber)
                                        } else false

                                        RivoListItem(
                                            headline = contact.name,
                                            supporting = if (contact.phoneNumbers.isNotEmpty()) {
                                                if (contact.phoneNumbers.size > 1) {
                                                    "${formatPhoneNumber(primaryNumber)} (+${contact.phoneNumbers.size - 1} more)"
                                                } else {
                                                    formatPhoneNumber(primaryNumber)
                                                }
                                            } else null,
                                            avatarName = contact.name,
                                            photoUri = contact.photoUri,
                                            badgeIcon = if (contact.isFavorite) Icons.Outlined.Star else if (contact.isPrivate) Icons.Outlined.Lock else null,
                                            badgeColor = if (contact.isFavorite) MaterialTheme.colorScheme.primary else null,
                                            selected = isSelected,
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                if (isMultiSelect) {
                                                    if (isSelected) {
                                                        selectedContactIds = selectedContactIds - contact.id
                                                        selectedPhoneNumbers = selectedPhoneNumbers - contact.phoneNumbers.toSet()
                                                    } else {
                                                        selectedContactIds = selectedContactIds + contact.id
                                                        if (primaryNumber.isNotBlank()) {
                                                            selectedPhoneNumbers = selectedPhoneNumbers + primaryNumber
                                                        }
                                                    }
                                                } else {
                                                    if (initialPhoneToAssign != null) {
                                                        navigator.navigate(
                                                            ContactEditScreenDestination(
                                                                contactId = contact.id,
                                                                initialPhone = initialPhoneToAssign
                                                            )
                                                        )
                                                    } else if (returnContactId) {
                                                        resultNavigator.navigateBack(result = contact.id)
                                                    } else {
                                                        if (contact.phoneNumbers.size > 1) {
                                                            pendingMultiNumberContact = contact
                                                        } else if (primaryNumber.isNotBlank()) {
                                                            resultNavigator.navigateBack(result = primaryNumber)
                                                        }
                                                    }
                                                }
                                            },
                                            trailingContent = {
                                                if (isMultiSelect) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                selectedContactIds = selectedContactIds + contact.id
                                                                if (primaryNumber.isNotBlank()) {
                                                                    selectedPhoneNumbers = selectedPhoneNumbers + primaryNumber
                                                                }
                                                            } else {
                                                                selectedContactIds = selectedContactIds - contact.id
                                                                selectedPhoneNumbers = selectedPhoneNumbers - contact.phoneNumbers.toSet()
                                                            }
                                                        }
                                                    )
                                                } else if (initialPhoneToAssign != null) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.PersonAdd,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                text = "Add",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        )

                                        if (index < contactsInGroup.size - 1) {
                                            RivoDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        pendingMultiNumberContact?.let { contact ->
            NumberPickerDialog(
                numbers = contact.phoneNumbers,
                onDismissRequest = { pendingMultiNumberContact = null },
                onNumberSelected = { selectedNumber ->
                    pendingMultiNumberContact = null
                    resultNavigator.navigateBack(result = selectedNumber)
                }
            )
        }
    }
}
