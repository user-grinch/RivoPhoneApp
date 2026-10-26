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
import com.grinch.rivo4.controller.util.findActivity
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.TipJarDialog
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
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
    var reminderMode by remember(settingsState) { mutableIntStateOf(prefs.getVideoAdReminderMode()) }

    var showTipJarDialog by remember { mutableStateOf(false) }
    var showDisableAdsDialog by remember { mutableStateOf(false) }
    var showFossDialog by remember { mutableStateOf(false) }
    var showVideoAdLoading by remember { mutableStateOf(false) }
    var videoAdError by remember { mutableStateOf<String?>(null) }

    val isPlayStoreBuild = com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED
    val isBillingSupported = com.grinch.rivo4.controller.billing.BillingManager.IS_BILLING_SUPPORTED
    val canUseProFeatures = isPlayStoreBuild && isBillingSupported

    val scope = rememberCoroutineScope()
    // Observe rewarded ad events
    LaunchedEffect(Unit) {
        // Collect ad watched events
        scope.launch {
            rewardedAdManager.adWatched.collect {
                showVideoAdLoading = false
                android.widget.Toast.makeText(
                    context,
                    "Thank you for your support!",
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
                            stringResource(R.string.settings_support_rivo_description),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                                val activity = context.findActivity()
                                if (activity != null) {
                                    rewardedAdManager.showAd(activity)
                                } else {
                                    showVideoAdLoading = false
                                }
                            } else {
                                showFossDialog = true
                            }
                        }
                    )
                }

                item {
                    RivoSwitchListItem(
                        headline = stringResource(R.string.support_video_reminders),
                        supporting = stringResource(R.string.support_video_reminders_supporting),
                        leadingIcon = Icons.Outlined.Notifications,
                        enabled = isPlayStoreBuild,
                        checked = reminderMode != PreferenceManager.VIDEO_AD_REMINDER_OFF,
                        onCheckedChange = { checked ->
                            val newMode = if (checked) PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS else PreferenceManager.VIDEO_AD_REMINDER_OFF
                            reminderMode = newMode
                            prefs.setVideoAdReminderMode(newMode)
                        }
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
