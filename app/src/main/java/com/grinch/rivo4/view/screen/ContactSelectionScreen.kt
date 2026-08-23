package com.grinch.rivo4.view.screen

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.components.NumberPickerDialog
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoFilterChip
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoShapeDefaults
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoCornerDp
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
    returnContactId: Boolean = false
) {
    val viewModel: ContactsViewModel = koinActivityViewModel()
    val allContacts by viewModel.allContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val roundness = LocalCardRoundness.current

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
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumbers.any { num -> num.contains(searchQuery) }
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
                    IconButton(onClick = { navigator.navigate(ContactEditScreenDestination()) }) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = "Create New Contact")
                    }
                    if (isMultiSelect && totalSelectedCount > 0) {
                        IconButton(onClick = {
                            selectedContactIds = emptySet()
                            selectedPhoneNumbers = emptySet()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                }
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
        }
    ) { padding ->
        if (isLoading) {
            RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(heroCornerDp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = logoMorph,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Contacts,
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
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${allContacts.size} contacts available • Tap to select or enter custom number",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { navigator.navigate(ContactEditScreenDestination()) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(itemCornerDp)
                                ) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Contact", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_contacts_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(heroCornerDp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RivoFilterChip(
                            label = "All (${allContacts.size})",
                            selected = selectedFilterTab == 0,
                            onClick = { selectedFilterTab = 0 },
                            leadingIcon = { Icon(Icons.Outlined.People, contentDescription = null) }
                        )
                        RivoFilterChip(
                            label = "Favorites",
                            selected = selectedFilterTab == 1,
                            onClick = { selectedFilterTab = 1 },
                            leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                        )
                        RivoFilterChip(
                            label = "Private",
                            selected = selectedFilterTab == 2,
                            onClick = { selectedFilterTab = 2 },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }
                        )
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
                                    modifier = Modifier.size(46.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                            }
                        }
                    }
                } else {
                    groupedContacts.forEach { (initial, contactsInGroup) ->
                        item(key = "header_$initial") {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(contactsInGroup, key = { it.id }) { contact ->
                            val primaryNumber = contact.phoneNumbers.firstOrNull() ?: ""
                            val isSelected = if (isMultiSelect) {
                                selectedContactIds.contains(contact.id) || selectedPhoneNumbers.contains(primaryNumber)
                            } else {
                                false
                            }

                            RivoExpressiveCard(
                                modifier = Modifier.clip(RoundedCornerShape(itemCornerDp)),
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
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
                                                if (returnContactId) {
                                                    resultNavigator.navigateBack(result = contact.id)
                                                } else {
                                                    if (contact.phoneNumbers.size > 1) {
                                                        pendingMultiNumberContact = contact
                                                    } else if (primaryNumber.isNotBlank()) {
                                                        resultNavigator.navigateBack(result = primaryNumber)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    RivoAvatar(
                                        name = contact.name,
                                        photoUri = contact.photoUri,
                                        modifier = Modifier.size(46.dp)
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (contact.isPrivate) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Outlined.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        if (contact.phoneNumbers.isNotEmpty()) {
                                            Text(
                                                text = if (contact.phoneNumbers.size > 1) {
                                                    "${formatPhoneNumber(primaryNumber)} (+${contact.phoneNumbers.size - 1} more)"
                                                } else {
                                                    formatPhoneNumber(primaryNumber)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!isMultiSelect && primaryNumber.isNotBlank()) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
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
