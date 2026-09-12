package com.grinch.rivo4.view.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuickResponsesScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    var responses by remember { mutableStateOf(prefs.getQuickResponses().toMutableList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    fun save(newResponses: List<String>) {
        responses = newResponses.toMutableList()
        prefs.setQuickResponses(newResponses)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Responses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset to Defaults")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingText = ""
                    isAddingNew = true
                },
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Response")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Quickreply, contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "In-Call Quick Decline Messages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tap the message button during an incoming call to decline and send one of these canned replies via SMS.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                RivoExpressiveCard(
                    title = "Canned Responses (${responses.size})",
                    icon = Icons.Outlined.Message
                ) {
                    if (responses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No quick responses configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        responses.forEachIndexed { index, responseText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = responseText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        editingText = responseText
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val updated = responses.toMutableList()
                                        updated.removeAt(index)
                                        save(updated)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < responses.size - 1) {
                                RivoDivider(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingNew) {
        RivoDialog(
            onDismissRequest = { isAddingNew = false },
            title = "New Quick Response",
            icon = Icons.Outlined.AddComment,
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editingText.trim()
                        if (trimmed.isNotEmpty()) {
                            val updated = responses.toMutableList()
                            updated.add(trimmed)
                            save(updated)
                        }
                        isAddingNew = false
                    },
                    enabled = editingText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNew = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            OutlinedTextField(
                value = editingText,
                onValueChange = { editingText = it },
                label = { Text("Message text") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
        }
    }

    if (editingIndex != null) {
        val index = editingIndex!!
        RivoDialog(
            onDismissRequest = { editingIndex = null },
            title = "Edit Quick Response",
            icon = Icons.Outlined.Edit,
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editingText.trim()
                        if (trimmed.isNotEmpty()) {
                            val updated = responses.toMutableList()
                            updated[index] = trimmed
                            save(updated)
                        }
                        editingIndex = null
                    },
                    enabled = editingText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            OutlinedTextField(
                value = editingText,
                onValueChange = { editingText = it },
                label = { Text("Message text") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
        }
    }

    if (showResetConfirm) {
        RivoDialog(
            onDismissRequest = { showResetConfirm = false },
            title = "Reset to Defaults",
            icon = Icons.Outlined.RestartAlt,
            confirmButton = {
                TextButton(
                    onClick = {
                        save(PreferenceManager.DEFAULT_QUICK_RESPONSES)
                        showResetConfirm = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Text(
                text = "Restore original preset quick responses?",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
