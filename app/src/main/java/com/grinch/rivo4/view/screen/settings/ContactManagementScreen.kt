package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactManagementScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: ContactsViewModel = koinActivityViewModel()
    var duplicateGroups by remember { mutableStateOf<List<List<Contact>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.findDuplicates { groups ->
            duplicateGroups = groups
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RivoExpressiveCard(
                        title = "Quick Fixes",
                        icon = Icons.Outlined.AutoFixHigh
                    ) {
                        RivoListItem(
                            headline = "Standardize phone numbers",
                            supporting = "Remove spaces and special characters from all numbers",
                            leadingIcon = Icons.Outlined.FormatListNumbered,
                            onClick = { viewModel.formatAllPhoneNumbers() }
                        )
                    }
                }

                if (duplicateGroups.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No duplicates found.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    item {
                        Text(
                            "Found ${duplicateGroups.size} groups of duplicates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    items(duplicateGroups) { group ->
                        DuplicateGroupCard(group) { target, sources ->
                            viewModel.mergeContacts(target.id, sources.map { it.id })
                            // Refresh list locally for better UX
                            duplicateGroups = duplicateGroups.filter { it != group }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: List<Contact>,
    onMerge: (Contact, List<Contact>) -> Unit
) {
    RivoExpressiveCard {
        group.forEachIndexed { index, contact ->
            RivoListItem(
                headline = contact.name,
                supporting = contact.phoneNumbers.joinToString(", "),
                avatarName = contact.name,
                photoUri = contact.photoUri,
                onClick = { }
            )
            if (index < group.size - 1) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { onMerge(group.first(), group.drop(1)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Outlined.Merge, null)
            Spacer(Modifier.width(8.dp))
            Text("Merge duplicates")
        }
    }
}
