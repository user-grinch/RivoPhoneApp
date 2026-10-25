package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

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
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import androidx.compose.material.icons.outlined.People
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

data class Contributor(
    val name: String,
    @StringRes val role: Int,
    val githubUrl: String? = null,
    val imageAsset: String? = null
)

val appContributors = listOf(
    Contributor(
        name = "Grinch_",
        role = R.string.contributor_role_author,
        githubUrl = "https://github.com/user-grinch",
        imageAsset = "grinch.jpeg"
    ),
    Contributor(
        name = "Hamma",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/MoHamed-B-M",
        imageAsset = "hamma.jpeg"
    ),
    Contributor(
        name = "azeemsher788",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/azeemsher788",
        imageAsset = "azeemsher788.png"
    ),
    Contributor(
        name = "Victor-root",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/Victor-root",
        imageAsset = "victor.png"
    ),
    Contributor(
        name = "tmpjx555",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/tmpjx555",
        imageAsset = "tmpjx555.png"
    ),
    Contributor(
        name = "master-bob",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/master-bob",
        imageAsset = "master_bob.png"
    ),
    Contributor(
        name = "Federico Iosue",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/federicoiosue",
        imageAsset = "federicoiosue.png"
    ),
    Contributor(
        name = "Maxime Monroux",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/maxime-monroux",
        imageAsset = "maxime_monroux.png"
    ),
    Contributor(
        name = "SaeedDev94",
        role = R.string.contributor_role_contributor,
        githubUrl = "https://github.com/SaeedDev94",
        imageAsset = "saeeddev94.png"
    ),
    Contributor(
        name = "kitsumed (Med)",
        role = R.string.contributor_role_upstream_call_recording,
        githubUrl = "https://github.com/kitsumed/ShizuCallRecorder"
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.contributors_title),
                navigator = navigator
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.contributors_title),
                    icon = Icons.Outlined.People
                ) {
                    appContributors.forEach { contributor ->
                        item {
                            RivoListItem(
                                headline = contributor.name,
                                supporting = stringResource(contributor.role),
                                avatarName = contributor.name,
                                photoUri = contributor.imageAsset?.let { "file:///android_asset/contributors/$it" },
                                trailingIcon = if (contributor.githubUrl != null) Icons.AutoMirrored.Outlined.Launch else null,
                                onClick = {
                                    contributor.githubUrl?.let { openLink(context, it) }
                                }
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
