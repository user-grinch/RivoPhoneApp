package com.grinch.rivo4.view.screen.settings

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.grinch.rivo4.controller.permission.PermissionActionType
import com.grinch.rivo4.controller.permission.PermissionCheckItem
import com.grinch.rivo4.controller.permission.PermissionChecklistHelper
import com.grinch.rivo4.controller.util.getDefaultDialerIntent
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.screen.onboarding.PermissionsChecklistCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PermissionsChecklistScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val essentialItems = remember(refreshTrigger) {
        PermissionChecklistHelper.getEssentialItems(context)
    }
    val recommendedItems = remember(refreshTrigger) {
        PermissionChecklistHelper.getRecommendedItems(context)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTrigger++
    }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTrigger++
    }

    fun handlePermissionItemClick(item: PermissionCheckItem) {
        when (item.actionType) {
            PermissionActionType.ROLE_DIALER -> {
                roleLauncher.launch(getDefaultDialerIntent(context))
            }
            PermissionActionType.OVERLAY -> {
                settingsLauncher.launch(PermissionChecklistHelper.getOverlayIntent(context))
            }
            PermissionActionType.BATTERY_OPTIMIZATION -> {
                settingsLauncher.launch(PermissionChecklistHelper.getBatteryOptimizationIntent(context))
            }
            PermissionActionType.SETTINGS -> {
                settingsLauncher.launch(PermissionChecklistHelper.getAppSettingsIntent(context))
            }
            PermissionActionType.RUNTIME -> {
                runtimeLauncher.launch(item.permissions.toTypedArray())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Permissions & App Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                RivoExpressiveCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "System Permissions Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Manage runtime permissions and system integrations required for seamless calling.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Essential Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }

            item {
                PermissionsChecklistCard(
                    items = essentialItems,
                    onItemClick = { handlePermissionItemClick(it) }
                )
            }

            item {
                Text(
                    text = "Recommended & Advanced Features",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }

            item {
                PermissionsChecklistCard(
                    items = recommendedItems,
                    onItemClick = { handlePermissionItemClick(it) }
                )
            }

            item {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Open App System Settings",
                        supporting = "View and manage permissions directly in Android device settings",
                        leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = {
                            context.startActivity(PermissionChecklistHelper.getAppSettingsIntent(context))
                        }
                    )
                }
            }
        }
    }
}
