package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.SHIZU_CALL_RECORDER_URL
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

data class ThirdPartyLicenseItem(
    val name: String,
    val license: String,
    val description: String? = null,
    val url: String? = null
)

private val specialAttributions = listOf(
    ThirdPartyLicenseItem(
        name = "ShizuCallRecorder",
        license = "GPLv3",
        description = "Call recording engine by kitsumed",
        url = SHIZU_CALL_RECORDER_URL
    )
)

private val openSourceLibraries = listOf(
    ThirdPartyLicenseItem(
        name = "AndroidX & Jetpack Compose",
        license = "Apache 2.0",
        description = "Modern UI toolkit & Android architecture components",
        url = "https://developer.android.com/jetpack/compose"
    ),
    ThirdPartyLicenseItem(
        name = "Kotlin & Coroutines",
        license = "Apache 2.0",
        description = "Language runtime, serialization & async coroutines",
        url = "https://kotlinlang.org"
    ),
    ThirdPartyLicenseItem(
        name = "Material 3",
        license = "Apache 2.0",
        description = "Material Design expressive UI components",
        url = "https://m3.material.io"
    ),
    ThirdPartyLicenseItem(
        name = "Koin",
        license = "Apache 2.0",
        description = "Lightweight dependency injection framework",
        url = "https://insert-koin.io"
    ),
    ThirdPartyLicenseItem(
        name = "Room Database",
        license = "Apache 2.0",
        description = "SQLite object mapping & persistence library",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    ThirdPartyLicenseItem(
        name = "Coil",
        license = "Apache 2.0",
        description = "Fast image loading library for Android",
        url = "https://coil-kt.github.io/coil"
    ),
    ThirdPartyLicenseItem(
        name = "Compose Destinations",
        license = "Apache 2.0",
        description = "Type-safe navigation framework for Compose",
        url = "https://github.com/raamcosta/compose-destinations"
    ),
    ThirdPartyLicenseItem(
        name = "ZXing",
        license = "Apache 2.0",
        description = "Barcode & QR code image processing",
        url = "https://github.com/zxing/zxing"
    ),
    ThirdPartyLicenseItem(
        name = "Accompanist Permissions",
        license = "Apache 2.0",
        description = "Android runtime permissions for Jetpack Compose",
        url = "https://github.com/google/accompanist"
    ),
    ThirdPartyLicenseItem(
        name = "Shizuku",
        license = "Apache 2.0",
        description = "Privileged system API access framework",
        url = "https://shizuku.rikka.app"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ThirdPartyLicensesScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.about_third_party_licenses_title),
                navigator = navigator
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RivoExpressiveGroup(
                title = stringResource(R.string.about_attributions_title),
                icon = Icons.Outlined.Code
            ) {
                specialAttributions.forEach { item ->
                    item {
                        RivoListItem(
                            headline = item.name,
                            supporting = listOfNotNull(item.description, item.license).joinToString(" • "),
                            trailingIcon = if (item.url != null) Icons.AutoMirrored.Outlined.Launch else null,
                            onClick = item.url?.let { url -> { openLink(context, url) } }
                        )
                    }
                }
            }

            RivoExpressiveGroup(
                title = stringResource(R.string.about_third_party_libraries_title),
                icon = Icons.Outlined.Gavel
            ) {
                openSourceLibraries.forEach { lib ->
                    item {
                        RivoListItem(
                            headline = lib.name,
                            supporting = listOfNotNull(lib.description, lib.license).joinToString(" • "),
                            trailingIcon = if (lib.url != null) Icons.AutoMirrored.Outlined.Launch else null,
                            onClick = lib.url?.let { url -> { openLink(context, url) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
