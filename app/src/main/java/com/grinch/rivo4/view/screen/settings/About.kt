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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.APP_VERSION
import com.grinch.rivo4.BUILD_NUMBER
import com.grinch.rivo4.DISCORD_URL
import com.grinch.rivo4.GITHUB_URL
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.R
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.rivo_logo),
                        contentDescription = "Rivo Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rivo",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            RivoExpressiveCard(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            ) {
                Text(
                    "About the App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Rivo is a modern dialer app that brings simplicity and elegance to calling. Designed with Material You, it adapts seamlessly to your theme.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = "Version",
                    supporting = APP_VERSION,
                    leadingIcon = Icons.Outlined.Info,
                    onClick = { }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = "Build number",
                    supporting = BUILD_NUMBER,
                    leadingIcon = Icons.Default.Numbers,
                    onClick = { }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = "Contributors",
                    supporting = "Meet the team behind the project",
                    leadingIcon = Icons.Outlined.Groups,
                    onClick = { navigator.navigate(ContributorsScreenDestination) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = "Support Us on Patreon",
                    supporting = "Help keep the project alive",
                    leadingIcon = Icons.Default.Favorite,
                    onClick = { openLink(context, PATREON_URL) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = "Discord Server",
                    supporting = "Join the community",
                    leadingIcon = Icons.Default.Chat,
                    onClick = { openLink(context, DISCORD_URL) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = "Source Code",
                    supporting = "View the source code on GitHub",
                    leadingIcon = Icons.Outlined.Code,
                    onClick = { openLink(context, GITHUB_URL) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                RivoListItem(
                    headline = "Check for Updates",
                    supporting = "Current version: $APP_VERSION",
                    leadingIcon = Icons.Outlined.SystemUpdate,
                    onClick = { openLink(context, "$GITHUB_URL/releases") }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "© 2025-2026 RivoPhone Project",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
