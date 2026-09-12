package com.grinch.rivo4.view.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

data class Contributor(
    val name: String,
    @StringRes val role: Int,
    val githubUrl: String? = null,
    val imageAsset: String? = null,
    val commits: Int? = null
)

val appContributors = listOf(
    Contributor(
        name = "Grinch_",
        role = R.string.contributor_role_lead_developer,
        githubUrl = "https://github.com/user-grinch",
        imageAsset = "grinch.jpeg",
        commits = 177
    ),
    Contributor(
        name = "Hamma",
        role = R.string.contributor_role_developer,
        githubUrl = "https://github.com/MoHamed-B-M",
        imageAsset = "hamma.jpeg",
        commits = 52
    ),
    Contributor(
        name = "Crowdin Bot",
        role = R.string.contributor_role_localization,
        githubUrl = "https://github.com/crowdin-bot",
        imageAsset = "crowdin.png",
        commits = 9
    ),
    Contributor(
        name = "Victor-root",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/Victor-root",
        imageAsset = "victor.png",
        commits = 7
    ),
    Contributor(
        name = "master-bob",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/master-bob",
        imageAsset = "master_bob.png",
        commits = 2
    ),
    Contributor(
        name = "tmpjx555",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/tmpjx555",
        imageAsset = "tmpjx555.png",
        commits = 1
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContributorsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contributors_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RivoExpressiveCard {
                    appContributors.forEachIndexed { index, contributor ->
                        RivoListItem(
                            headline = contributor.name,
                            supporting = stringResource(contributor.role),
                            supporting2 = contributor.commits?.let { stringResource(R.string.contributor_commits, it) },
                            avatarName = contributor.name,
                            photoUri = contributor.imageAsset?.let { "file:///android_asset/contributors/$it" },
                            trailingIcon = if (contributor.githubUrl != null) Icons.AutoMirrored.Outlined.Launch else null,
                            onClick = {
                                contributor.githubUrl?.let { openLink(context, it) }
                            }
                        )
                        if (index < appContributors.size - 1) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
