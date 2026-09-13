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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.grinch.rivo4.PLAY_STORE_URL
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
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
    val hidePrivateContacts = remember(settingsState) {
        prefs.getBoolean(PreferenceManager.KEY_HIDE_PRIVATE_SETTINGS_ENTRY, false)
    }
    val listState = rememberLazyListState()
    val appInfo = getAppVersion(context)
    val logoMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.2f }

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            item {
                RivoExpressiveCard(
                    modifier = Modifier.clickable { navigator.navigate(AboutScreenDestination) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = logoMorph,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                Image(
                                    painter = painterResource(R.drawable.logo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.about_app_display_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "v${appInfo.first}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
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
                        headline = stringResource(R.string.settings_interface_headline),
                        supporting = stringResource(R.string.settings_interface_supporting),
                        leadingIcon = Icons.Outlined.Palette,
                        onClick = { navigator.navigate(InterfaceScreenDestination) }
                    )
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
                    RivoListItem(
                        headline = stringResource(R.string.call_recordings_title),
                        supporting = "Auto-recording, Shizuku internal audio & saved recordings",
                        leadingIcon = Icons.Outlined.FiberManualRecord,
                        onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
                    )
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
                    RivoListItem(
                        headline = stringResource(R.string.settings_blocked_numbers_headline),
                        supporting = stringResource(R.string.settings_blocked_numbers_supporting),
                        leadingIcon = Icons.Outlined.Block,
                        onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
                    )
                    RivoListItem(
                        headline = stringResource(R.string.fake_call_title),
                        supporting = stringResource(R.string.fake_call_subtitle),
                        leadingIcon = Icons.Outlined.PhoneCallback,
                        onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
                    )
                    val appLockEnabled = remember(settingsState) { prefs.isAppLockEnabled() }
                    RivoListItem(
                        headline = "App Lock",
                        supporting = if (appLockEnabled) "Enabled (Face, Fingerprint, PIN)" else "Protect app with biometrics or PIN",
                        leadingIcon = Icons.Outlined.Lock,
                        onClick = { navigator.navigate(AppLockScreenDestination) }
                    )
                }
            }

            // 4. Contacts & Storage
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.settings_contacts_management_title),
                    icon = Icons.Outlined.ManageAccounts
                ) {
                    RivoListItem(
                        headline = stringResource(R.string.settings_contact_management_headline),
                        supporting = stringResource(R.string.settings_contact_management_supporting),
                        leadingIcon = Icons.Outlined.CallMerge,
                        onClick = { navigator.navigate(ContactManagementScreenDestination) }
                    )
                    RivoListItem(
                        headline = stringResource(R.string.settings_manage_visibility),
                        supporting = stringResource(R.string.settings_manage_visibility_supporting),
                        leadingIcon = Icons.Outlined.Visibility,
                        onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
                    )
                }
            }

            // 5. Privacy & Private Storage
            item {
                val secretCode = remember(settingsState) {
                    prefs.getString(PreferenceManager.KEY_SECRET_DIALPAD_CODE, PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE) ?: PreferenceManager.DEFAULT_SECRET_DIALPAD_CODE
                }
                RivoExpressiveCard(
                    title = "Privacy & Private Storage",
                    icon = Icons.Outlined.Lock
                ) {
                    RivoListItem(
                        headline = "Private Storage",
                        supporting = "Secret dialpad vault ($secretCode) • Stored only in app memory",
                        leadingIcon = Icons.Outlined.Lock,
                        onClick = { navigator.navigate(PrivateContactsScreenDestination) }
                    )
                    RivoListItem(
                        headline = "Permissions & App Setup",
                        supporting = "Review granted permissions and system capabilities",
                        leadingIcon = Icons.Outlined.VerifiedUser,
                        onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
                    )
                }
            }

            // 5. Data & Support
            item {
                RivoExpressiveCard(
                    title = "Data & Support",
                    icon = Icons.Outlined.HelpOutline
                ) {
                    RivoListItem(
                        headline = stringResource(R.string.settings_backup_restore_headline),
                        supporting = stringResource(R.string.settings_backup_restore_supporting),
                        leadingIcon = Icons.Outlined.Backup,
                        onClick = { navigator.navigate(BackupRestoreScreenDestination) }
                    )
                    RivoListItem(
                        headline = "Rate on Google Play",
                        supporting = "Support Rivo on Google Play Store",
                        leadingIcon = Icons.Default.Star,
                        onClick = { openLink(context, PLAY_STORE_URL) }
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
    }
}
