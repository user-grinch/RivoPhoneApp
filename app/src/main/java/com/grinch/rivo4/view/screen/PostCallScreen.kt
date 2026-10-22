package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.formatPhoneNumber
import android.provider.ContactsContract
import com.grinch.rivo4.MainActivity
import com.grinch.rivo4.controller.CallRecorder
import android.media.MediaPlayer
import androidx.compose.material.icons.filled.Delete
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import java.io.File
import com.grinch.rivo4.view.components.AddToContactBottomSheet
import com.grinch.rivo4.view.components.CallNotesSheet
import com.grinch.rivo4.view.screen.settings.toCallRecordingItem
import com.grinch.rivo4.view.components.ad.PostCallNativeAd
import kotlinx.coroutines.delay

@Composable
fun PostCallScreen(
    contactName: String,
    phoneNumber: String,
    photoUri: String?,
    durationSeconds: Long,
    recordingPath: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showNoteSheet by remember { mutableStateOf(false) }
    var showAddToContactSheet by remember { mutableStateOf(false) }
    var showDeleteRecordingDialog by remember { mutableStateOf(false) }
    var activeRecordingFile by remember(recordingPath) {
        mutableStateOf(recordingPath?.let { File(it) }?.takeIf { it.exists() })
    }
    var isVisible by remember { mutableStateOf(false) }

    // In-app recording playback state
    var playerSetUp by remember(recordingPath) { mutableStateOf(false) }
    val playerFile by remember { derivedStateOf { if (playerSetUp) activeRecordingFile else null } }
    var isRecordingPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var recordingDurationMs by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val recItem = remember(activeRecordingFile) {
        activeRecordingFile?.let { it.toCallRecordingItem(context) }
    }

    val toggleRecordingPlayback: () -> Unit = {
        if (!playerSetUp) {
            playerSetUp = true
        } else {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isRecordingPlaying = false
                } else {
                    mp.start()
                    isRecordingPlaying = true
                }
            }
        }
    }

    val isUnsaved = remember(contactName, phoneNumber) {
        contactName.isEmpty() || contactName == phoneNumber
    }

    val quickReplies = remember {
        listOf(
            "Call you right back!",
            "Sorry, I'm busy right now.",
            "Can't talk now. What's up?",
            "Type custom message..."
        )
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    BackHandler {
        onDismiss()
    }

    // Managed MediaPlayer lifecycle (lazy: only created once playback is requested)
    DisposableEffect(playerFile) {
        val file = playerFile
        var mp: MediaPlayer? = null
        if (file != null) {
            mp = MediaPlayer().apply {
                try {
                    setDataSource(context, Uri.fromFile(file))
                    prepare()
                    start()
                    this@apply.playbackParams = this@apply.playbackParams.setSpeed(playbackSpeed)
                } catch (_: Exception) {
                }
            }
            mediaPlayer = mp
            isRecordingPlaying = runCatching { mp.isPlaying }.getOrDefault(false)
            recordingDurationMs = runCatching { mp.duration }.getOrDefault(0)
            currentPositionMs = 0
        }
        onDispose {
            runCatching {
                mp?.stop()
                mp?.release()
            }
            mediaPlayer = null
            isRecordingPlaying = false
        }
    }

    // Playback progress poll loop
    LaunchedEffect(isRecordingPlaying, playerFile) {
        while (isRecordingPlaying && playerFile != null) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPositionMs = mp.currentPosition
                    recordingDurationMs = mp.duration
                } else {
                    isRecordingPlaying = false
                }
            }
            delay(250)
        }
    }

    // Soft translucent scrim (no deep black): background app / launcher remains clearly visible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.24f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
        ) {
            // Truecaller-style floating bottom popup card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks inside card
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top drag indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Top Status Bar: Logo + Rivo text + Call Summary + Close ✕ button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.logo),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "RIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Call Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Prominent ✕ Close Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(34.dp),
                            onClick = onDismiss
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caller Card (Truecaller-style elevated container)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!photoUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = photoUri,
                                                contentDescription = contactName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val displayName = contactName.ifEmpty { phoneNumber.ifEmpty { stringResource(R.string.label_unknown) } }
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val durationText = if (durationSeconds > 0) {
                                        val mins = durationSeconds / 60
                                        val secs = durationSeconds % 60
                                        "Call ended • %02d:%02d".format(mins, secs)
                                    } else {
                                        "Call ended"
                                    }

                                    val formattedPhone = remember(phoneNumber) { formatPhoneNumber(phoneNumber) }
                                    Text(
                                        text = if (displayName != phoneNumber && phoneNumber.isNotEmpty()) {
                                            "$formattedPhone • $durationText"
                                        } else {
                                            durationText
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick SMS reply chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickReplies.forEach { text ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        onClick = {
                                            val body = if (text.startsWith("Type custom")) "" else text
                                            sendSms(context, phoneNumber, body)
                                            onDismiss()
                                        }
                                    ) {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons row (Call, Message, Note, Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call
                                FilledTonalButton(
                                    onClick = {
                                        initiateCall(context, phoneNumber)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Call", style = MaterialTheme.typography.labelMedium)
                                }

                                // Message
                                FilledTonalButton(
                                    onClick = {
                                        sendSms(context, phoneNumber)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("SMS", style = MaterialTheme.typography.labelMedium)
                                }

                                // Note
                                FilledTonalButton(
                                    onClick = { showNoteSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Note", style = MaterialTheme.typography.labelMedium)
                                }

                                // If unsaved number, show "Save" button
                                if (isUnsaved && phoneNumber.isNotEmpty()) {
                                    FilledTonalButton(
                                        onClick = {
                                            showAddToContactSheet = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Save", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Dedicated Call Recording Card with inline player (no external apps needed)
                    AnimatedVisibility(
                        visible = activeRecordingFile?.exists() == true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val file = activeRecordingFile
                        val item = recItem
                        if (file != null && file.exists() && item != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = toggleRecordingPlayback,
                                        modifier = Modifier.size(38.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isRecordingPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isRecordingPlaying) "Pause" else "Play",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.settings_call_recording),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val durMs = if (recordingDurationMs > 0) recordingDurationMs.toLong() else item.durationMs
                                        val durationLabel = if (durMs > 0) {
                                            android.text.format.DateUtils.formatElapsedTime(durMs / 1000)
                                        } else {
                                            item.sizeFormatted
                                        }
                                        Text(
                                            text = durationLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            CallRecorder.share(
                                                context,
                                                file,
                                                context.getString(R.string.call_recordings_share)
                                            )
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = stringResource(R.string.call_recordings_share),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showDeleteRecordingDialog = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Ad Card (Native Advanced with MediaView in Play variant)
                    PostCallNativeAd(
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (showNoteSheet) {
            CallNotesSheet(
                phoneNumber = phoneNumber,
                contactName = contactName,
                onDismiss = { showNoteSheet = false }
            )
        }

        if (showAddToContactSheet) {
            AddToContactBottomSheet(
                phoneNumber = phoneNumber,
                onDismissRequest = { showAddToContactSheet = false },
                onCreateNewContact = {
                    showAddToContactSheet = false
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_INSERT
                        putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                onAddToExistingContact = {
                    showAddToContactSheet = false
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = "com.grinch.rivo4.ACTION_ADD_TO_EXISTING_CONTACT"
                        putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )
        }

        if (showDeleteRecordingDialog) {
            RivoConfirmationDialog(
                onDismissRequest = { showDeleteRecordingDialog = false },
                onConfirm = {
                    val fileToDelete = activeRecordingFile
                    if (fileToDelete != null) {
                        CallRecorder.delete(context, fileToDelete)
                        CallRecorder.clearLastRecordedFile()
                        activeRecordingFile = null
                        playerSetUp = false
                        isRecordingPlaying = false
                        currentPositionMs = 0
                        recordingDurationMs = 0
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.call_recording_deleted),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showDeleteRecordingDialog = false
                },
                title = stringResource(R.string.call_recordings_delete_title),
                message = stringResource(R.string.call_recordings_delete_message),
                confirmLabel = stringResource(R.string.action_delete),
                icon = Icons.Default.Delete,
                isDestructive = true
            )
        }
    }
}

private fun initiateCall(context: Context, number: String) {
    if (number.isEmpty()) return
    com.grinch.rivo4.controller.util.makeCall(context, number)
}

private fun sendSms(context: Context, number: String, body: String = "") {
    if (number.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
            if (body.isNotEmpty()) {
                putExtra("sms_body", body)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun addContact(context: Context, number: String) {
    if (number.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = "vnd.android.cursor.item/contact"
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
