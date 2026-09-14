package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.PLAY_STORE_URL
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.TipJarDialog
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val listState = rememberLazyListState()
    val appInfo = getAppVersion(context)
    val logoMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.2f }

    var enableAds by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true)) }
    var showDisableAdsDialog by remember { mutableStateOf(false) }
    var showTipJarDialog by remember { mutableStateOf(false) }
    val isSupporter = remember(settingsState) { prefs.isSupporter() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Info Banner
            item {
                RivoExpressiveCard(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { navigator.navigate(AboutScreenDestination) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = logoMorph,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(11.dp)) {
                                Image(
                                    painter = painterResource(R.drawable.logo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.about_app_display_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "v${appInfo.first}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_top_card_subtext),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 1. Personalization & Display
            item {
                RivoExpressiveCard(
                    title = "Personalization & Display",
                    icon = Icons.Outlined.Palette
                ) {
                    RivoListItem(
                        headline = "Theme & Appearance",
                        supporting = "Material You, color palette, AMOLED dark mode & animations",
                        leadingIcon = Icons.Outlined.Palette,
                        onClick = { navigator.navigate(InterfaceScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "Navigation Bar",
                        supporting = "Floating bar style, blur effect, roundness & tab layout",
                        leadingIcon = Icons.Outlined.Dock,
                        onClick = { navigator.navigate(BottomNavScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "Avatars & Contact Cards",
                        supporting = "11 avatar shapes, contact photos, initials & cards",
                        leadingIcon = Icons.Outlined.AccountCircle,
                        onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.settings_sound_vibration_headline),
                        supporting = stringResource(R.string.settings_sound_vibration_supporting),
                        leadingIcon = Icons.Outlined.VolumeUp,
                        onClick = { navigator.navigate(SoundVibrationScreenDestination) }
                    )
                }
            }

            // 2. Calling & Behavior
            item {
                RivoExpressiveCard(
                    title = "Calling & Behavior",
                    icon = Icons.Outlined.Phone
                ) {
                    RivoListItem(
                        headline = stringResource(R.string.settings_call_settings_headline),
                        supporting = stringResource(R.string.settings_call_settings_supporting),
                        leadingIcon = Icons.Outlined.SimCard,
                        onClick = { navigator.navigate(CallAccountsScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.settings_swipe_actions_title),
                        supporting = stringResource(R.string.settings_swipe_actions_supporting),
                        leadingIcon = Icons.Outlined.Swipe,
                        onClick = { navigator.navigate(SwipeActionsScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.call_recordings_title),
                        supporting = "Auto-recording, Shizuku internal audio & saved recordings",
                        leadingIcon = Icons.Outlined.FiberManualRecord,
                        onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "Call Analytics & Insights",
                        supporting = "Talk time leaderboard, peak hours & distribution",
                        leadingIcon = Icons.Outlined.Analytics,
                        onClick = { navigator.navigate(CallAnalyticsScreenDestination()) }
                    )
                }
            }

            // 3. Call Protection & Security
            item {
                RivoExpressiveCard(
                    title = "Call Protection & Security",
                    icon = Icons.Outlined.Security
                ) {
                    val appLockEnabled = remember(settingsState) { prefs.isAppLockEnabled() }
                    RivoListItem(
                        headline = "App Lock",
                        supporting = if (appLockEnabled) "Enabled (Face, Fingerprint, PIN)" else "Protect app with biometrics or PIN",
                        leadingIcon = Icons.Outlined.Lock,
                        onClick = { navigator.navigate(AppLockScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    val secretCode = remember(settingsState) {
                        prefs.getString(PreferenceManager.KEY_SECRET_DIALPAD_CODE, PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE) ?: PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
                    }
                    RivoListItem(
                        headline = "Private Storage",
                        supporting = "Secret dialpad vault ($secretCode) • Stored only in app memory",
                        leadingIcon = Icons.Outlined.FolderShared,
                        onClick = { navigator.navigate(PrivateContactsScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.settings_blocked_numbers_headline),
                        supporting = stringResource(R.string.settings_blocked_numbers_supporting),
                        leadingIcon = Icons.Outlined.Block,
                        onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.fake_call_title),
                        supporting = stringResource(R.string.fake_call_subtitle),
                        leadingIcon = Icons.Outlined.PhoneCallback,
                        onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "Permissions & App Setup",
                        supporting = "Review granted permissions and system capabilities",
                        leadingIcon = Icons.Outlined.VerifiedUser,
                        onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
                    )
                }
            }

            // 4. Contacts & Data
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.settings_contacts_management_title),
                    icon = Icons.Outlined.ManageAccounts
                ) {
                    RivoListItem(
                        headline = stringResource(R.string.settings_contact_management_headline),
                        supporting = stringResource(R.string.settings_contact_management_supporting),
                        leadingIcon = Icons.Outlined.ManageAccounts,
                        onClick = { navigator.navigate(ContactManagementScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.settings_manage_visibility),
                        supporting = stringResource(R.string.settings_manage_visibility_supporting),
                        leadingIcon = Icons.Outlined.Visibility,
                        onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_restore_headline),
                        supporting = stringResource(R.string.settings_backup_restore_supporting),
                        leadingIcon = Icons.Outlined.Backup,
                        onClick = { navigator.navigate(BackupRestoreScreenDestination) }
                    )
                }
            }

            // 5. Support & About
            item {
                RivoExpressiveCard(
                    title = "Support & About",
                    icon = Icons.Outlined.HelpOutline
                ) {
                    if (IS_ADS_SUPPORTED) {
                        RivoSwitchListItem(
                            headline = "Display Banner Ads",
                            supporting = "Show non-intrusive banner ads inside lists to support development",
                            leadingIcon = Icons.Outlined.AdUnits,
                            checked = enableAds,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    showDisableAdsDialog = true
                                } else {
                                    enableAds = true
                                    prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, true)
                                }
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    RivoListItem(
                        headline = if (isSupporter) "Rivo Supporter ⭐" else "Support Us",
                        supporting = if (isSupporter) "Thank you for supporting Rivo!" else "Support development via Tip Jar or Patreon",
                        leadingIcon = if (isSupporter) Icons.Outlined.Star else Icons.Outlined.Favorite,
                        onClick = { showTipJarDialog = true }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "Rate on Google Play",
                        supporting = "Support Rivo on Google Play Store",
                        leadingIcon = Icons.Default.Star,
                        onClick = { openLink(context, PLAY_STORE_URL) }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoListItem(
                        headline = "About Rivo",
                        supporting = "Version, open source licenses & contributors",
                        leadingIcon = Icons.Outlined.Info,
                        onClick = { navigator.navigate(AboutScreenDestination) }
                    )
                }
            }

            item {
                com.grinch.rivo4.view.components.ad.BannerAd()
            }

            item {
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }

        if (showDisableAdsDialog) {
            RivoDialog(
                onDismissRequest = { showDisableAdsDialog = false },
                title = stringResource(R.string.ads_disable_dialog_title),
                icon = Icons.Outlined.Favorite,
                confirmAction = RivoDialogAction(
                    label = stringResource(R.string.ads_disable_dialog_confirm),
                    onClick = {
                        enableAds = false
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
                            openLink(context, PATREON_URL)
                            showDisableAdsDialog = false
                        }
                    ) {
                        Icon(Icons.Outlined.VolunteerActivism, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.patreon_prompt_confirm))
                    }
                }
            }
        }

        if (showTipJarDialog) {
            TipJarDialog(onDismissRequest = { showTipJarDialog = false })
        }
    }
}
