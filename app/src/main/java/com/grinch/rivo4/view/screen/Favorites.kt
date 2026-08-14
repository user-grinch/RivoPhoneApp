package com.grinch.rivo4.view.screen

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.grinch.rivo4.view.components.rememberGridDragDropState
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesGridContent(navigator: DestinationsNavigator) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val allContacts by contactsVM.allContacts.collectAsState()
    val callLauncher = rememberCallLauncher()
    val gridState = rememberLazyGridState()
    val view = LocalView.current

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

    val items = remember { mutableStateListOf<Contact>() }
    LaunchedEffect(favorites) {
        if (items.map { it.id }.toSet() != favorites.map { it.id }.toSet()) {
            items.clear()
            items.addAll(favorites)
        } else {
            val byId = favorites.associateBy { it.id }
            for (i in items.indices) byId[items[i].id]?.let { items[i] = it }
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) isEditing = false
    }

    val dragDropState = rememberGridDragDropState(gridState) { from, to ->
        items.add(to, items.removeAt(from))
    }
    LaunchedEffect(dragDropState) {
        while (true) {
            val diff = dragDropState.scrollChannel.receive()
            gridState.scrollBy(diff)
        }
    }

    if (items.isEmpty()) {
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
                text = if (isEditing) {
                    stringResource(R.string.favorites_drag_to_reorder)
                } else {
                    stringResource(R.string.recents_favorites)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {
                if (isEditing) prefs.setFavoritesOrder(items.map { it.id })
                isEditing = !isEditing
            }) {
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
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEditing) {
                        Modifier.pointerInput(dragDropState) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragDropState.onDrag(offset)
                                },
                                onDragEnd = {
                                    dragDropState.onDragInterrupted()
                                    prefs.setFavoritesOrder(items.map { it.id })
                                },
                                onDragCancel = { dragDropState.onDragInterrupted() }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items, key = { _, contact -> contact.id }) { index, contact ->
                val dragging = index == dragDropState.draggingItemIndex
                val itemModifier = if (dragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationX = dragDropState.draggingItemOffset.x
                            translationY = dragDropState.draggingItemOffset.y
                            scaleX = 1.05f
                            scaleY = 1.05f
                        }
                } else {
                    Modifier.animateItem()
                }
                FavoriteGridItem(
                    modifier = itemModifier,
                    contact = contact,
                    displayOrder = displayOrder,
                    isEditing = isEditing,
                    isDragging = dragging,
                    onUnfavorite = { contactsVM.toggleFavorite(contact) },
                    onOpen = {
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                    },
                    onCall = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        callLauncher.dial(contact.phoneNumbers.firstOrNull() ?: "", contact)
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
    isDragging: Boolean,
    onUnfavorite: () -> Unit,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth(),
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
                    .graphicsLayer { if (isEditing && !isDragging) rotationZ = wiggle }
                    .combinedClickable(
                        enabled = !isEditing,
                        onClick = onOpen,
                        onLongClick = onCall
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
