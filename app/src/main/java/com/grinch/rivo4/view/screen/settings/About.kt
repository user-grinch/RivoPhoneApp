package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.grinch.rivo4.DISCORD_URL
import com.grinch.rivo4.GITHUB_URL
import com.grinch.rivo4.PLAY_STORE_URL
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.rivoPolygonShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContributorsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SupportDevelopmentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThirdPartyLicensesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val appName = stringResource(R.string.about_app_display_name)
    val appInfo = getAppVersion(context)
    val logoShape = rivoPolygonShape(RivoMaterialShapes.Cookie7Sided)

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_about_rivo),
                navigator = navigator
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RivoExpressiveCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = logoShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(R.drawable.logo),
                                contentDescription = stringResource(R.string.about_logo_content_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.25f
                                        scaleY = 1.25f
                                    }
                            )
                        }
                    }

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "v${appInfo.first} (${appInfo.second})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            RivoExpressiveCard {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.about_app_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.about_app_card_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { openLink(context, PLAY_STORE_URL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about_rate_on_google_play),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { navigator.navigate(SupportDevelopmentScreenDestination) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Support",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            RivoExpressiveGroup {
                item {
                    RivoListItem(
                        headline = stringResource(R.string.contributors_title),
                        supporting = stringResource(R.string.about_contributors_supporting),
                        leadingIcon = Icons.Outlined.Groups,
                        onClick = { navigator.navigate(ContributorsScreenDestination) }
                    )
                }
            }

            RivoExpressiveGroup {
                item {
                    RivoListItem(
                        headline = stringResource(R.string.about_discord),
                        supporting = stringResource(R.string.about_discord_supporting),
                        leadingIcon = Icons.AutoMirrored.Filled.Chat,
                        onClick = { openLink(context, DISCORD_URL) }
                    )
                }
                item {
                    RivoListItem(
                        headline = stringResource(R.string.about_source_code),
                        supporting = stringResource(R.string.about_source_code_supporting),
                        leadingIcon = Icons.Outlined.Code,
                        onClick = { openLink(context, GITHUB_URL) }
                    )
                }
                item {
                    RivoListItem(
                        headline = stringResource(R.string.about_check_updates),
                        supporting = stringResource(R.string.about_current_version, appInfo.first),
                        leadingIcon = Icons.Outlined.SystemUpdate,
                        onClick = { openLink(context, "$GITHUB_URL/releases") }
                    )
                }
                item {
                    RivoListItem(
                        headline = stringResource(R.string.about_third_party_licenses),
                        supporting = stringResource(R.string.about_third_party_licenses_supporting),
                        leadingIcon = Icons.Outlined.Gavel,
                        onClick = { navigator.navigate(ThirdPartyLicensesScreenDestination) }
                    )
                }
            }

            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

    }
}
