package com.grinch.rivo4.view.screen.settings

import android.content.Context
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BlockedNumbersScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    
    var blockMethod by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_METHOD, 0)) }
    var logVisibility by remember(settingsState) { mutableStateOf(prefs.getInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, 0)) }
    var blockNotification by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Numbers", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RivoExpressiveCard {
                    RivoSelectListItem(
                        headline = "Block method",
                        supporting = "How blocked calls are handled",
                        leadingIcon = Icons.Outlined.Gavel,
                        options = listOf(
                            "Decline automatically" to 0,
                            "Ring silently" to 1
                        ),
                        selectedValue = blockMethod,
                        onValueChange = {
                            blockMethod = it
                            prefs.setInt(PreferenceManager.KEY_BLOCK_METHOD, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    RivoSelectListItem(
                        headline = "Log visibility",
                        supporting = "Show or hide blocked calls in recents",
                        leadingIcon = Icons.Outlined.Visibility,
                        options = listOf(
                            "Hide from logs" to 0,
                            "Show in logs" to 1
                        ),
                        selectedValue = logVisibility,
                        onValueChange = {
                            logVisibility = it
                            prefs.setInt(PreferenceManager.KEY_BLOCK_LOG_VISIBILITY, it)
                        }
                    )
                }
            }

            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Block notifications",
                        supporting = "Show a low-priority alert for blocked calls",
                        leadingIcon = Icons.Outlined.NotificationsPaused,
                        checked = blockNotification,
                        onCheckedChange = {
                            blockNotification = it
                            prefs.setBoolean(PreferenceManager.KEY_BLOCK_NOTIFICATION, it)
                        }
                    )
                }
            }
            
            item {
                Button(
                    onClick = { 
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        try {
                            val intent = telecomManager.createManageBlockedNumbersIntent()
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if the intent is not supported on some older/custom versions
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, null)
                    Spacer(Modifier.width(8.dp))
                    Text("System Blocked Numbers")
                }
            }
        }
    }
}
