package com.grinch.rivo4.view.screen.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.GraphicEq
import com.grinch.rivo4.controller.shizuku.ShizukuConnectionManager
import rikka.shizuku.Shizuku
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
    var showShizukuInstallDialog by remember { mutableStateOf(false) }

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

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            refreshTrigger++
        }
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (_: Exception) {}
        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(listener)
            } catch (_: Exception) {}
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

    val storageLauncher = rememberLauncherForActivityResult(
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
            PermissionActionType.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    storageLauncher.launch(PermissionChecklistHelper.getStorageAccessIntent(context))
                } else {
                    runtimeLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }
            PermissionActionType.SHIZUKU -> {
                when {
                    !PermissionChecklistHelper.isShizukuInstalled(context) -> {
                        showShizukuInstallDialog = true
                    }
                    !PermissionChecklistHelper.isShizukuRunning() -> {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(PermissionChecklistHelper.SHIZUKU_PACKAGE)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            showShizukuInstallDialog = true
                        }
                    }
                    else -> {
                        ShizukuConnectionManager.requestPermission()
                    }
                }
            }
        }
    }

    if (showShizukuInstallDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuInstallDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Install Shizuku",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Shizuku enables crystal-clear 2-way call audio capture directly from Android system audio without rooting your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Select an installation source:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShizukuInstallDialog = false
                        try {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${PermissionChecklistHelper.SHIZUKU_PACKAGE}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(marketIntent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${PermissionChecklistHelper.SHIZUKU_PACKAGE}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    }
                ) {
                    Text("Google Play")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            showShizukuInstallDialog = false
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    ) {
                        Text("Website / APK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showShizukuInstallDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
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
