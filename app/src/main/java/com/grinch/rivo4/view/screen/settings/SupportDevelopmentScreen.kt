package com.grinch.rivo4.view.screen.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ads.RewardedAdManager
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.ProBrandingManager
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.TipJarDialog
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoSelectionDialog
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SupportDevelopmentScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val rewardedAdManager = koinInject<RewardedAdManager>()

    val isSupporter by remember(settingsState) { mutableStateOf(prefs.isSupporter()) }
    val enableAds by remember(settingsState) {
        mutableStateOf(
            prefs.getBoolean(
                PreferenceManager.KEY_ENABLE_ADS,
                true
            )
        )
    }
    val selectedAppIcon by remember(settingsState) { mutableStateOf(prefs.getSelectedAppIcon()) }
    var reminderMode by remember(settingsState) { mutableIntStateOf(prefs.getVideoAdReminderMode()) }

    var showTipJarDialog by remember { mutableStateOf(false) }
    var showDisableAdsDialog by remember { mutableStateOf(false) }
    var showAppIconPicker by remember { mutableStateOf(false) }
    var showIconRestartDialog by remember { mutableStateOf(false) }
    var pendingAppIcon by remember { mutableStateOf(selectedAppIcon) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showFossDialog by remember { mutableStateOf(false) }
    var showVideoAdLoading by remember { mutableStateOf(false) }
    var videoAdError by remember { mutableStateOf<String?>(null) }

    val isPlayStoreBuild = com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED
    val isBillingSupported = com.grinch.rivo4.controller.billing.BillingManager.IS_BILLING_SUPPORTED
    val canUseProFeatures = isPlayStoreBuild && isBillingSupported

    LaunchedEffect(settingsState) {
        ProBrandingManager.apply(context, prefs)
    }

    // Ads-enabled access is intentional: it funds the app while keeping Pro perks small.
    val shouldBePro = isPlayStoreBuild &&
        (isSupporter || enableAds || prefs.hasAdSupportedPro())
    val proSource = when {
        !isPlayStoreBuild -> stringResource(R.string.support_pro_foss_reason)
        isSupporter -> stringResource(R.string.support_tip_jar_pro)
        prefs.hasAdSupportedPro() -> stringResource(R.string.support_rewarded_pro)
        enableAds -> stringResource(R.string.support_ads_enabled_pro)
        else -> stringResource(R.string.support_pro_not_active)
    }

    val scope = rememberCoroutineScope()
    // Observe rewarded ad events
    LaunchedEffect(Unit) {
        // Collect ad watched events
        scope.launch {
            rewardedAdManager.adWatched.collect {
                showVideoAdLoading = false
                android.widget.Toast.makeText(
                    context,
                    "Thank you for supporting Rivo!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
        // Collect ad failed to show events
        scope.launch {
            rewardedAdManager.adFailedToShow.collect { error ->
                showVideoAdLoading = false
                videoAdError = error
            }
        }
        // Collect ad dismissed events
        scope.launch {
            rewardedAdManager.adDismissed.collect { _ ->
                showVideoAdLoading = false
            }
        }
        // Collect ad load errors
        scope.launch {
            rewardedAdManager.adLoadError.collect { error ->
                showVideoAdLoading = false
                videoAdError = error
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_support_development),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { navigator.navigateUp() },
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Support summary
            RivoExpressiveCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(0.85f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text(stringResource(R.string.settings_support_rivo_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Support development and unlock Pro benefits.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            RivoExpressiveCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.support_rivo_pro_perks_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProPerkRow(
                        stringResource(R.string.support_dialer_icon_label),
                        Icons.Default.Apps,
                        shouldBePro,
                        onClick = {
                            pendingAppIcon = selectedAppIcon
                            showAppIconPicker = true
                        }
                    )
                    Button(
                        onClick = {
                            pendingAppIcon = selectedAppIcon
                            showAppIconPicker = true
                        },
                        enabled = shouldBePro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (shouldBePro) "Choose icon & label" else "Unlock Pro to choose icon & label")
                    }
                    Text(
                        if (canUseProFeatures) {
                            "Watch a video for one day of Pro, even with ads disabled. Keep non-intrusive ads enabled for ongoing access, or tip once on Google Play for permanent Pro."
                        } else {
                            "Rivo Pro perks are unavailable in FOSS builds. Use the Play Store build to unlock them."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (shouldBePro) {
                            stringResource(R.string.support_pro_active_reason, proSource)
                        } else {
                            proSource
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (shouldBePro) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Support Options
            RivoExpressiveGroup(
                title = stringResource(R.string.support_ways_title),
                icon = Icons.Default.Favorite
            ) {
                // Provide a Tip (renamed from Rivo Supporter)
                item {
                    RivoListItem(
                        headline = stringResource(R.string.settings_provide_tip),
                        supporting = stringResource(R.string.settings_provide_tip_supporting),
                        leadingIcon = Icons.Default.Favorite,
                        trailingIcon = if (isSupporter) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onClick = {
                            if (canUseProFeatures) {
                                showTipJarDialog = true
                            } else {
                                showFossDialog = true
                            }
                        }
                    )
                }

                // Patreon
                item {
                    RivoListItem(
                        headline = stringResource(R.string.settings_patreon),
                        supporting = stringResource(R.string.settings_patreon_supporting),
                        leadingIcon = Icons.Default.MonetizationOn,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        enabled = canUseProFeatures,
                        onClick = {
                            if (canUseProFeatures) {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.patreon.com/c/grinch_")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } else {
                                showFossDialog = true
                            }
                        }
                    )
                }

                // Watch Video Ad
                item {
                    RivoListItem(
                        headline = stringResource(R.string.settings_watch_video_ad),
                        supporting = stringResource(R.string.settings_watch_video_ad_supporting),
                        leadingIcon = Icons.Default.PlayCircle,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        enabled = isPlayStoreBuild,
                        onClick = {
                            if (isPlayStoreBuild) {
                                showVideoAdLoading = true
                                videoAdError = null
                                val activity = context as? Activity
                                activity?.let {
                                    rewardedAdManager.showAd(it) {}
                                }
                            } else {
                                showFossDialog = true
                            }
                        }
                    )
                }

                item {
                    RivoListItem(
                        headline = stringResource(R.string.support_video_reminders),
                        supporting = when (reminderMode) {
                            PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS -> "Show a support button every 6 hours"
                            PreferenceManager.VIDEO_AD_REMINDER_ALWAYS -> stringResource(R.string.support_always_show)
                            else -> stringResource(R.string.support_off)
                        },
                        leadingIcon = Icons.Outlined.Notifications,
                        enabled = isPlayStoreBuild,
                        onClick = { showReminderPicker = true }
                    )
                }

                // Non-intrusive Advertisement (renamed from Display Banner Ads)
                item {
                    RivoSwitchListItem(
                        headline = stringResource(R.string.settings_display_banner_ads),
                        supporting = stringResource(R.string.settings_display_banner_ads_supporting),
                        leadingIcon = Icons.Outlined.AdUnits,
                        checked = enableAds,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                showDisableAdsDialog = true
                            } else {
                                prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, true)
                            }
                        }
                    )
                }

            }

            // FOSS Build Notice
            if (!isPlayStoreBuild) {
                RivoExpressiveCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_support_development),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.support_foss_only_donate),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.patreon.com/c/grinch_")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(
                                text = "Support on Patreon",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showTipJarDialog) {
        TipJarDialog(onDismissRequest = { showTipJarDialog = false })
    }

    if (showReminderPicker) {
        RivoSelectionDialog(
            onDismissRequest = { showReminderPicker = false },
            title = stringResource(R.string.support_video_reminders),
            items = listOf(
                PreferenceManager.VIDEO_AD_REMINDER_OFF,
                PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS,
                PreferenceManager.VIDEO_AD_REMINDER_ALWAYS
            ),
            itemLabel = { mode ->
                when (mode) {
                    PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS -> "Every 6 hours"
                    PreferenceManager.VIDEO_AD_REMINDER_ALWAYS -> context.getString(R.string.support_always)
                    else -> context.getString(R.string.support_off)
                }
            },
            itemSupporting = { mode ->
                when (mode) {
                    PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS -> "Show one support button every 6 hours"
                    PreferenceManager.VIDEO_AD_REMINDER_ALWAYS -> context.getString(R.string.support_keep_button)
                    else -> context.getString(R.string.support_hide_button)
                }
            },
            itemIcon = { Icons.Outlined.Notifications },
            isSelected = { it == reminderMode },
            onItemSelected = {
                reminderMode = it
                prefs.setVideoAdReminderMode(it)
            }
        )
    }

    if (showDisableAdsDialog) {
        RivoDialog(
            onDismissRequest = { showDisableAdsDialog = false },
            title = stringResource(R.string.ads_disable_dialog_title),
            icon = Icons.Outlined.Favorite,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.ads_disable_dialog_confirm),
                onClick = {
                    prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, false)
                    showDisableAdsDialog = false
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.ads_disable_dialog_keep),
                onClick = { showDisableAdsDialog = false }
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.ads_disable_dialog_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.patreon.com/c/grinch_")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        showDisableAdsDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.support_patreon_instead))
                }
            }
        }
    }

    if (showAppIconPicker) {
        RivoDialog(
            modifier = Modifier.widthIn(max = 320.dp),
            onDismissRequest = { showAppIconPicker = false },
            title = stringResource(R.string.support_choose_app_icon_title),
            icon = Icons.Default.Apps,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.action_confirm),
                onClick = {
                    if (pendingAppIcon != selectedAppIcon) {
                        showAppIconPicker = false
                        showIconRestartDialog = true
                    } else {
                        showAppIconPicker = false
                    }
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_cancel),
                onClick = { showAppIconPicker = false }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconOptions(
                    selectedIcon = pendingAppIcon,
                    onIconSelected = { iconName ->
                        pendingAppIcon = iconName
                    },
                    isPro = shouldBePro
                )
            }
        }
    }

    if (showIconRestartDialog) {
        RivoDialog(
            onDismissRequest = {
                pendingAppIcon = selectedAppIcon
                showIconRestartDialog = false
            },
            title = stringResource(R.string.support_restart_required),
            icon = Icons.Default.RestartAlt,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.support_restart_now),
                onClick = {
                    val restartIntent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    prefs.setSelectedAppIcon(pendingAppIcon)
                    ProBrandingManager.apply(context, prefs)
                    showIconRestartDialog = false
                    (context as? Activity)?.let { activity ->
                        restartIntent?.let(activity::startActivity)
                        activity.finishAndRemoveTask()
                    }
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.support_dont_apply),
                onClick = {
                    pendingAppIcon = selectedAppIcon
                    showIconRestartDialog = false
                }
            )
        ) {
            Text(
                text = stringResource(R.string.support_restart_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showFossDialog) {
        RivoDialog(
            onDismissRequest = { showFossDialog = false },
            title = stringResource(R.string.support_feature_unavailable),
            icon = Icons.Default.Info,
            confirmAction = RivoDialogAction(
                label = stringResource(R.string.support_on_patreon),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.patreon.com/c/grinch_")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    showFossDialog = false
                }
            ),
            dismissAction = RivoDialogAction(
                label = stringResource(R.string.action_cancel),
                onClick = { showFossDialog = false }
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.support_foss_only_donate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f),
            contentColor = color,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun RowScope.StatDetail(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProPerkRow(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        enabled: Boolean,
        onClick: () -> Unit = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
    }
}

@Composable
private fun AppIconOptions(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    isPro: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Default icon
        AppIconOption(
            name = "Rivo",
            iconName = "default",
            isSelected = selectedIcon == "default",
            isEnabled = true,
            onClick = { onIconSelected("default") }
        )

        // Pro icon (only if pro)
        if (isPro) {
            AppIconOption(
                name = "Rivo Pro",
                iconName = "pro",
                isSelected = selectedIcon == "pro",
                isEnabled = true,
                onClick = { onIconSelected("pro") }
            )
        } else {
            AppIconOption(
                name = "Rivo Pro",
                iconName = "pro",
                isSelected = false,
                isEnabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun AppIconOption(
    name: String,
    iconName: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        contentColor = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.38f
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    if (iconName == "pro") R.drawable.pro_app_icon else R.drawable.logo
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = 1.2f
                        scaleY = 1.2f
                    }
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            RadioButton(
                selected = isSelected,
                enabled = isEnabled,
                onClick = onClick
            )
        }
    }
}
