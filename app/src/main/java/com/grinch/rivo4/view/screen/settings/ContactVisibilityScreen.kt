package com.grinch.rivo4.view.screen.settings

import android.accounts.Account
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactVisibilityScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: ContactsViewModel = koinActivityViewModel()
    val accounts by viewModel.availableAccounts.collectAsState()
    val visibleAccounts by viewModel.visibleAccountsFlow.collectAsState()

    val currentVisible = remember(visibleAccounts, accounts) {
        visibleAccounts ?: (accounts.map { "${it.type}|${it.name}" } + "local|local").toSet()
    }

    fun toggleAccount(key: String, enabled: Boolean) {
        val newSet = if (enabled) currentVisible + key else currentVisible - key
        viewModel.setVisibleAccounts(newSet)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Visibility", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Select which accounts should be visible in your contact list. Deselecting an account will hide its contacts but won't delete them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            item {
                RivoExpressiveCard(
                    title = "Accounts",
                    icon = Icons.Default.Visibility
                ) {
                    RivoSwitchListItem(
                        headline = "Local Memory",
                        supporting = "Contacts stored on device",
                        leadingIcon = Icons.Default.CloudOff,
                        checked = currentVisible.contains("local|local"),
                        onCheckedChange = { isChecked: Boolean -> toggleAccount("local|local", isChecked) }
                    )

                    accounts.forEach { account ->
                        val key = "${account.type}|${account.name}"
                        RivoSwitchListItem(
                            headline = ContactUtils.getFriendlyAccountName(account),
                            supporting = account.name,
                            leadingIcon = ContactUtils.getAccountIcon(account),
                            checked = currentVisible.contains(key),
                            onCheckedChange = { isChecked: Boolean -> toggleAccount(key, isChecked) }
                        )
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { viewModel.setVisibleAccounts(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Show All")
                }
            }
        }
    }
}
