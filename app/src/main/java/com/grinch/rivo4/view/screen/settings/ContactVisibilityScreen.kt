package com.grinch.rivo4.view.screen.settings

import android.accounts.Account
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
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
    val sortOrderState by viewModel.sortOrder.collectAsState()
    val displayOrderState by viewModel.displayOrder.collectAsState()

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
                title = { Text(stringResource(R.string.settings_visibility_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                    text = stringResource(R.string.settings_visibility_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.settings_manage_display_sorting)
                ) {
                    RivoVisualOptionSelectorRow(
                        headline = stringResource(R.string.settings_manage_sort_by),
                        supporting = stringResource(R.string.settings_manage_sort_by_supporting),
                        leadingIcon = Icons.Outlined.SortByAlpha,
                        options = listOf(
                            stringResource(R.string.settings_manage_sort_first_name) to 0,
                            stringResource(R.string.settings_manage_sort_last_name) to 1
                        ),
                        selectedValue = sortOrderState,
                        onValueChange = {
                            viewModel.setSortOrder(it)
                        }
                    ) { _, selected ->
                        Icon(
                            imageVector = Icons.Outlined.SortByAlpha,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoVisualOptionSelectorRow(
                        headline = stringResource(R.string.settings_manage_name_format),
                        supporting = stringResource(R.string.settings_manage_name_format_supporting),
                        leadingIcon = Icons.Outlined.Badge,
                        options = listOf(
                            stringResource(R.string.settings_manage_name_format_first_first) to 0,
                            stringResource(R.string.settings_manage_name_format_last_first) to 1
                        ),
                        selectedValue = displayOrderState,
                        onValueChange = {
                            viewModel.setDisplayOrder(it)
                        }
                    ) { _, selected ->
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.settings_visibility_accounts_header),
                    icon = Icons.Default.Visibility
                ) {
                    RivoSwitchListItem(
                        headline = stringResource(R.string.label_local_memory),
                        supporting = stringResource(R.string.settings_visibility_local_memory_supporting),
                        leadingIcon = Icons.Default.CloudOff,
                        checked = currentVisible.contains("local|local"),
                        onCheckedChange = { isChecked: Boolean -> toggleAccount("local|local", isChecked) }
                    )

                    accounts.forEach { account ->
                        val key = "${account.type}|${account.name}"
                        RivoSwitchListItem(
                            headline = ContactUtils.getFriendlyAccountName(LocalContext.current, account),
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
                    Text(stringResource(R.string.settings_visibility_reset_all))
                }
            }
        }
    }
}
