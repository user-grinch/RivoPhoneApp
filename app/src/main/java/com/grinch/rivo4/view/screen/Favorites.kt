package com.grinch.rivo4.view.screen

import android.Manifest
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.PermissionDeniedView
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.components.rememberCallLauncher
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>(style = NoTransitions::class)
@Composable
fun FavoritesScreen(navController: NavController, navigator: DestinationsNavigator) {
    FavoritesScreenContent(navController, navigator)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FavoritesScreenContent(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val isGranted = permState.status == PermissionStatus.Granted

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (!isGranted) {
                PermissionDeniedView(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.favorites_permission_title),
                    description = stringResource(R.string.favorites_permission_description),
                    onGrantClick = { permState.launchPermissionRequest() }
                )
            } else {
                FavoritesGridContent(navigator)
            }
        }
    }
}

@Composable
private fun FavoritesGridContent(navigator: DestinationsNavigator) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val allContacts by contactsVM.allContacts.collectAsState()
    val callLauncher = rememberCallLauncher()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        contactsVM.fetchContacts()
    }

    val displayOrder = remember(settingsState) {
        prefs.getInt(PreferenceManager.KEY_CONTACT_DISPLAY_ORDER, 0)
    }

    val favorites = remember(allContacts, settingsState) {
        val favContacts = allContacts.filter { it.isFavorite }
        val order = prefs.getFavoritesOrder()
        favContacts.sortedWith(compareBy<Contact> { contact ->
            val index = order.indexOf(contact.id)
            if (index != -1) index else Int.MAX_VALUE
        }.thenBy { it.name })
    }

    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(favorites.isEmpty()) {
        if (favorites.isEmpty()) isEditing = false
    }

    if (favorites.isEmpty()) {
        EmptyFavoritesState()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recents_favorites),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { isEditing = !isEditing }) {
                Text(
                    text = if (isEditing) stringResource(R.string.action_done) else stringResource(R.string.action_edit),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(favorites, key = { it.id }) { contact ->
                FavoriteGridItem(
                    contact = contact,
                    displayOrder = displayOrder,
                    isEditing = isEditing,
                    onUnfavorite = { contactsVM.toggleFavorite(contact) },
                    onClick = {
                        callLauncher.dial(contact.phoneNumbers.firstOrNull() ?: "", contact)
                    },
                    onLongClick = {
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteGridItem(
    contact: Contact,
    displayOrder: Int,
    isEditing: Boolean,
    onUnfavorite: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "favoriteWiggle")
    val wiggle by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            RivoAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { if (isEditing) rotationZ = wiggle }
                    .combinedClickable(
                        enabled = !isEditing,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
            )

            if (isEditing) {
                Surface(
                    onClick = onUnfavorite,
                    modifier = Modifier.size(22.dp).offset(x = 6.dp, y = (-6).dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.content_desc_remove_favorite),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = ContactUtils.formatContactName(contact.name, displayOrder),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyFavoritesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.favorites_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.favorites_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}
