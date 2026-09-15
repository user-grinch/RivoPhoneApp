package com.grinch.rivo4.view.screen

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.view.components.PermissionDeniedView
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.components.rememberCallLauncher
import com.grinch.rivo4.view.components.rememberGridDragDropState
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.grinch.rivo4.view.theme.callColors
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
fun FavoritesScreenContent(
    navController: NavController,
    navigator: DestinationsNavigator,
    showTopBar: Boolean = true
) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val isGranted = permState.status == PermissionStatus.Granted

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopBar(navController, navigator)
            }
        },
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
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }

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
        EmptyFavoritesState(onAddClick = { showAddSheet = true })
        if (showAddSheet) {
            AddFavoriteBottomSheet(
                contacts = allContacts.filter { !it.isFavorite },
                displayOrder = displayOrder,
                onDismiss = { showAddSheet = false },
                onAddFavorite = { contact ->
                    contactsVM.toggleFavorite(contact)
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) {
                        stringResource(R.string.favorites_drag_to_reorder)
                    } else {
                        stringResource(R.string.recents_favorites)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (items.size == 1) {
                            stringResource(R.string.favorites_one_pill)
                        } else {
                            stringResource(R.string.favorites_count_pill, items.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isEditing) {
                    FilledTonalIconButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.favorites_add_action),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        if (isEditing) prefs.setFavoritesOrder(items.map { it.id })
                        isEditing = !isEditing
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(50),
                    colors = if (isEditing) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                ) {
                    if (isEditing) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isEditing) stringResource(R.string.action_done) else stringResource(R.string.action_edit),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isEditing && items.size >= 3) {
                val heroContact = items.first()
                item(span = { GridItemSpan(2) }, key = "hero_${heroContact.id}") {
                    HeroFavoriteCard(
                        contact = heroContact,
                        displayOrder = displayOrder,
                        onOpen = {
                            navigator.navigate(ContactDetailsScreenDestination(contactId = heroContact.id))
                        },
                        onCall = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            callLauncher.dial(heroContact.phoneNumbers.firstOrNull() ?: "", heroContact)
                        },
                        onMessage = {
                            val phone = heroContact.phoneNumbers.firstOrNull() ?: ""
                            if (phone.isNotBlank()) SocialUtils.openSms(context, phone)
                        }
                    )
                }
            }

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

                ExpressiveFavoriteCard(
                    modifier = itemModifier,
                    contact = contact,
                    displayOrder = displayOrder,
                    isEditing = isEditing,
                    isDragging = dragging,
                    onUnfavorite = {
                        contactsVM.toggleFavorite(contact)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    },
                    onOpen = {
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                    },
                    onCall = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        callLauncher.dial(contact.phoneNumbers.firstOrNull() ?: "", contact)
                    },
                    onMessage = {
                        val phone = contact.phoneNumbers.firstOrNull() ?: ""
                        if (phone.isNotBlank()) SocialUtils.openSms(context, phone)
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddFavoriteBottomSheet(
            contacts = allContacts.filter { !it.isFavorite },
            displayOrder = displayOrder,
            onDismiss = { showAddSheet = false },
            onAddFavorite = { contact ->
                contactsVM.toggleFavorite(contact)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        )
    }
}

@Composable
private fun HeroFavoriteCard(
    contact: Contact,
    displayOrder: Int,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                RivoAvatar(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.size(58.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.favorites_hero_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = ContactUtils.formatContactName(contact.name, displayOrder),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val primaryPhone = contact.phoneNumbers.firstOrNull() ?: ""
                if (primaryPhone.isNotBlank()) {
                    Text(
                        text = formatPhoneNumber(primaryPhone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onMessage,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.favorites_action_message),
                        modifier = Modifier.size(18.dp)
                    )
                }
                FilledIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = stringResource(R.string.favorites_action_call),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFavoriteCard(
    contact: Contact,
    displayOrder: Int,
    isEditing: Boolean,
    isDragging: Boolean,
    onUnfavorite: () -> Unit,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "favoriteWiggle")
    val wiggle by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    Card(
        onClick = { if (!isEditing) onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isEditing && !isDragging) rotationZ = wiggle
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                RivoAvatar(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.size(54.dp)
                )

                if (isEditing) {
                    Surface(
                        onClick = onUnfavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(26.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = stringResource(R.string.content_desc_remove_favorite),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = ContactUtils.formatContactName(contact.name, displayOrder),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            val primaryPhone = contact.phoneNumbers.firstOrNull() ?: ""
            Text(
                text = if (primaryPhone.isNotBlank()) formatPhoneNumber(primaryPhone) else stringResource(R.string.favorites_no_number),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCall,
                    enabled = !isEditing && primaryPhone.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = stringResource(R.string.favorites_action_call),
                        modifier = Modifier.size(16.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onMessage,
                    enabled = !isEditing && primaryPhone.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.favorites_action_message),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(56.dp),
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.favorites_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.favorites_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.favorites_add_empty_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFavoriteBottomSheet(
    contacts: List<Contact>,
    displayOrder: Int,
    onDismiss: () -> Unit,
    onAddFavorite: (Contact) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts.sortedBy { it.name }
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.contains(searchQuery) }
            }.sortedBy { it.name }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.favorites_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${filteredContacts.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.search_contacts),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.content_desc_clear_search)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(14.dp))

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.favorites_no_contacts_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        Surface(
                            onClick = {
                                onAddFavorite(contact)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RivoAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    textStyle = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.size(42.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ContactUtils.formatContactName(contact.name, displayOrder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val phone = contact.phoneNumbers.firstOrNull() ?: ""
                                    if (phone.isNotBlank()) {
                                        Text(
                                            text = formatPhoneNumber(phone),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        onAddFavorite(contact)
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.action_add_to_favorites),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
