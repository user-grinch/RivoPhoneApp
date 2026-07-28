package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SystemUpdate
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
import com.grinch.rivo4.DISCORD_URL
import com.grinch.rivo4.GITHUB_URL
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContributorsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val appInfo = getAppVersion(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = stringResource(R.string.about_logo_content_desc),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.about_app_display_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            RivoExpressiveCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    stringResource(R.string.about_app_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.about_app_card_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = stringResource(R.string.about_version),
                    supporting = appInfo.first,
                    leadingIcon = Icons.Outlined.Info,
                    onClick = { }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = stringResource(R.string.about_build_number),
                    supporting = appInfo.second.toString(),
                    leadingIcon = Icons.Default.Numbers,
                    onClick = { }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = stringResource(R.string.contributors_title),
                    supporting = stringResource(R.string.about_contributors_supporting),
                    leadingIcon = Icons.Outlined.Groups,
                    onClick = { navigator.navigate(ContributorsScreenDestination) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = stringResource(R.string.about_patreon),
                    supporting = stringResource(R.string.about_patreon_supporting),
                    leadingIcon = Icons.Default.Favorite,
                    onClick = { openLink(context, PATREON_URL) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = stringResource(R.string.about_discord),
                    supporting = stringResource(R.string.about_discord_supporting),
                    leadingIcon = Icons.Default.Chat,
                    onClick = { openLink(context, DISCORD_URL) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = stringResource(R.string.about_source_code),
                    supporting = stringResource(R.string.about_source_code_supporting),
                    leadingIcon = Icons.Outlined.Code,
                    onClick = { openLink(context, GITHUB_URL) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = stringResource(R.string.about_check_updates),
                    supporting = stringResource(R.string.about_current_version, appInfo.first),
                    leadingIcon = Icons.Outlined.SystemUpdate,
                    onClick = { openLink(context, "$GITHUB_URL/releases") }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
