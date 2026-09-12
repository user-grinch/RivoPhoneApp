package com.grinch.rivo4.view.screen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.CallRecorder
import com.grinch.rivo4.controller.shizuku.ShizukuConnectionManager
import com.grinch.rivo4.controller.util.OemPermissionHelper
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.formatDateHeader
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.LocalRivoSurfaceStyle
import com.grinch.rivo4.view.components.RivoConfirmationDialog
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoLeadingIconTile
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSurfaceStyle
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@Destination<RootGraph>
@Composable
fun CallRecordingsScreen(
    navigator: DestinationsNavigator,
    initialShowList: Boolean = false
) {
    CallRecordingsContent(
        showTopBar = true,
        initialShowList = initialShowList,
        onBack = { navigator.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRecordingsContent(
    showTopBar: Boolean = false,
    initialShowList: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var showingRecordingsList by remember { mutableStateOf(initialShowList) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var recordings by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var selectedFilterNumber by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showingRecordingsList && !initialShowList) {
        showingRecordingsList = false
    }

    // Shizuku & Recording Preference States
    val shizukuAvailable = remember(settingsState, refreshKey) { ShizukuConnectionManager.isAvailable() }
    val shizukuPermissionGranted = remember(settingsState, refreshKey) { ShizukuConnectionManager.hasPermission(context) }

    // Auto-refresh recordings when returning to the screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var callRecordingEnabled by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_RECORDING, true))
    }
    var autoRecordEnabled by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_RECORDING_AUTO, false))
    }
    var minDurationFilter by remember(settingsState) { mutableIntStateOf(prefs.getInt("call_recording_min_duration", 0)) }
    var bitrate by remember(settingsState) { mutableIntStateOf(prefs.getInt("call_recording_bitrate", 128000)) }

    val shareTitle = stringResource(R.string.call_recordings_share)

    // Inline Media Player State
    var activePlayingFile by remember { mutableStateOf<File?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(activePlayingFile) {
        if (activePlayingFile != null) {
            val mp = MediaPlayer().apply {
                try {
                    setDataSource(context, Uri.fromFile(activePlayingFile))
                    prepare()
                    start()
                    this@apply.playbackParams = this@apply.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {
                }
            }
            mediaPlayer = mp
            isPlaying = mp.isPlaying
            durationMs = runCatching { mp.duration }.getOrDefault(0)

            onDispose {
                runCatching {
                    mp.stop()
                    mp.release()
                }
                mediaPlayer = null
                isPlaying = false
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(isPlaying, activePlayingFile) {
        while (isPlaying && activePlayingFile != null) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPositionMs = mp.currentPosition
                    durationMs = mp.duration
                } else {
                    isPlaying = false
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(refreshKey) {
        recordings = CallRecorder.listRecordings(context)
    }

    val filteredRecordings = remember(recordings, searchQuery, selectedFilterNumber) {
        recordings.filter { file ->
            val matchesSearch = searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)
            val callerLabel = file.nameWithoutExtension
                .substringBeforeLast('_')
                .substringBeforeLast('_')
            val matchesFilter = selectedFilterNumber == null || callerLabel.equals(selectedFilterNumber, ignoreCase = true)
            matchesSearch && matchesFilter
        }
    }

    val uniqueCallerLabels = remember(recordings) {
        recordings.map { file ->
            file.nameWithoutExtension
                .substringBeforeLast('_')
                .substringBeforeLast('_')
        }.distinct().sorted()
    }

    val groupedRecordings = remember(filteredRecordings) {
        filteredRecordings
            .sortedByDescending { it.lastModified() }
            .groupBy { formatDateHeader(context, it.lastModified()) }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showingRecordingsList) "Saved Call Recordings" else "Call Recording Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (onBack != null || (showingRecordingsList && !initialShowList)) {
                            IconButton(onClick = {
                                if (showingRecordingsList && !initialShowList) {
                                    showingRecordingsList = false
                                } else {
                                    onBack?.invoke()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        }
                    },
                    actions = {
                        if (showingRecordingsList && recordings.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllConfirm = true }) {
                                Icon(
                                    Icons.Outlined.DeleteSweep,
                                    contentDescription = "Delete all recordings",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { refreshKey++ }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh recordings")
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // View Mode 1: Saved Call Recordings List
            if (showingRecordingsList) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search recordings...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Filter Chips Row
                if (uniqueCallerLabels.size > 1) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFilterNumber == null,
                                    onClick = { selectedFilterNumber = null },
                                    label = { Text("All") },
                                    leadingIcon = if (selectedFilterNumber == null) {
                                        { Icon(Icons.Outlined.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    shape = CircleShape
                                )
                            }
                            items(uniqueCallerLabels) { label ->
                                FilterChip(
                                    selected = selectedFilterNumber == label,
                                    onClick = {
                                        selectedFilterNumber = if (selectedFilterNumber == label) null else label
                                    },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = if (selectedFilterNumber == label) {
                                        { Icon(Icons.Outlined.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    shape = CircleShape
                                )
                            }
                        }
                    }
                }

                if (filteredRecordings.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.MicNone,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.call_recordings_empty),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.call_recordings_empty_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    groupedRecordings.forEach { (dateHeader, filesInGroup) ->
                        item {
                            RivoSectionHeader(
                                title = dateHeader,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                        items(filesInGroup, key = { it.absolutePath }) { file ->
                            val isCurrentActive = activePlayingFile?.absolutePath == file.absolutePath

                            CallRecordEntryCard(
                                file = file,
                                isCurrentActive = isCurrentActive,
                                isPlaying = isCurrentActive && isPlaying,
                                currentPositionMs = if (isCurrentActive) currentPositionMs else 0,
                                durationMs = if (isCurrentActive) durationMs else 0,
                                playbackSpeed = playbackSpeed,
                                onCardClick = {
                                    if (isCurrentActive) {
                                        mediaPlayer?.let { mp ->
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        }
                                    } else {
                                        activePlayingFile = file
                                    }
                                },
                                onPlayPauseClick = {
                                    if (isCurrentActive) {
                                        mediaPlayer?.let { mp ->
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        }
                                    } else {
                                        activePlayingFile = file
                                    }
                                },
                                onSeekTo = { posMs ->
                                    if (isCurrentActive) {
                                        currentPositionMs = posMs
                                        mediaPlayer?.seekTo(posMs)
                                    }
                                },
                                onRewind10 = {
                                    if (isCurrentActive) {
                                        val newPos = (currentPositionMs - 10000).coerceAtLeast(0)
                                        currentPositionMs = newPos
                                        mediaPlayer?.seekTo(newPos)
                                    }
                                },
                                onForward10 = {
                                    if (isCurrentActive) {
                                        val newPos = (currentPositionMs + 10000).coerceAtMost(durationMs)
                                        currentPositionMs = newPos
                                        mediaPlayer?.seekTo(newPos)
                                    }
                                },
                                onSpeedChange = { newSpeed ->
                                    playbackSpeed = newSpeed
                                    mediaPlayer?.let { mp ->
                                        runCatching { mp.playbackParams = mp.playbackParams.setSpeed(newSpeed) }
                                    }
                                },
                                onShareClick = { CallRecorder.share(context, file, shareTitle) },
                                onDeleteClick = { pendingDelete = file }
                            )
                        }
                    }
                }
            } else {
                // View Mode 2: Main Settings & Quality Page

                // 1. Shizuku ADB Service Banner (at top, regular tile color, not red!)
                item {
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RivoLeadingIconTile(
                                    icon = if (shizukuAvailable && shizukuPermissionGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (shizukuAvailable && shizukuPermissionGranted) "Shizuku ADB Service Active"
                                        else if (!shizukuAvailable) "Shizuku Service Required"
                                        else "Shizuku Permission Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (shizukuAvailable && shizukuPermissionGranted) "Elevated internal call audio recording is enabled."
                                        else "Android restricts call audio. Shizuku is required to capture crystal-clear internal call audio.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!shizukuAvailable || !shizukuPermissionGranted) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!shizukuAvailable) {
                                        Button(
                                            onClick = { openLink(context, "https://shizuku.rikka.app/") },
                                            modifier = Modifier.weight(1f),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Install Shizuku")
                                        }
                                    }
                                    if (shizukuAvailable) {
                                        FilledTonalButton(
                                            onClick = {
                                                ShizukuConnectionManager.requestPermission()
                                                refreshKey++
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Grant Permission")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Direct Internal Storage Access Card (only shown when permission not granted)
                item {
                    val hasStoragePermission = remember(refreshKey) { CallRecorder.hasStoragePermission(context) }
                    val manageStorageLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) {
                        refreshKey++
                    }
                    val requestPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) {
                        refreshKey++
                    }

                    if (!hasStoragePermission) {
                        RivoExpressiveCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RivoLeadingIconTile(
                                        icon = Icons.Outlined.FolderSpecial,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Direct Storage Access Recommended",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Grant All Files Access to store call recordings directly into Internal Storage / ${CallRecorder.DIRECTORY_NAME} without Scoped Storage restrictions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            try {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                                manageStorageLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                manageStorageLauncher.launch(intent)
                                            }
                                        } else {
                                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Grant Storage Access")
                                }
                            }
                        }
                    }
                }

                // 2.5 OEM / Xiaomi Optimization Card
                if (OemPermissionHelper.isOemDevice()) {
                    item {
                        val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
                        val isIgnoringBattery = remember(refreshKey) {
                            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                        }
                        var oemDismissed by remember(refreshKey) {
                            mutableStateOf(prefs.getBoolean("oem_opt_dismissed", false))
                        }

                        if (!oemDismissed && !isIgnoringBattery) {
                            RivoExpressiveCard(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RivoLeadingIconTile(
                                            icon = Icons.Outlined.PhoneAndroid,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${OemPermissionHelper.getOemBrandDisplayName()} Optimization",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(R.string.oem_recording_guide_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                oemDismissed = true
                                                prefs.setBoolean("oem_opt_dismissed", true)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Dismiss",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Vertical action rows instead of squeezed horizontal buttons
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (OemPermissionHelper.isXiaomi() || OemPermissionHelper.isOppo() || OemPermissionHelper.isRealme() || OemPermissionHelper.isVivo() || OemPermissionHelper.isMeizu()) {
                                            OemActionRow(
                                                icon = Icons.Outlined.Mic,
                                                title = stringResource(R.string.oem_perm_bg_recording),
                                                subtitle = stringResource(R.string.oem_perm_bg_recording_desc),
                                                onClick = { OemPermissionHelper.openOemPermissions(context) }
                                            )
                                        }
                                        OemActionRow(
                                            icon = Icons.Outlined.RocketLaunch,
                                            title = stringResource(R.string.oem_perm_autostart),
                                            subtitle = stringResource(R.string.oem_perm_autostart_desc),
                                            onClick = { OemPermissionHelper.openAutostartSettings(context) }
                                        )
                                        OemActionRow(
                                            icon = Icons.Outlined.BatteryChargingFull,
                                            title = stringResource(R.string.oem_perm_battery),
                                            subtitle = stringResource(R.string.oem_perm_battery_desc),
                                            onClick = { OemPermissionHelper.openBatterySaverSettings(context) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Saved Call Recordings Navigation Tile (directly under Storage Banner)
                item {
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showingRecordingsList = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RivoLeadingIconTile(
                                icon = Icons.Outlined.LibraryMusic,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved Call Recordings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${recordings.size} recordings available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Recordings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 4. Recording Controls Section
                item {
                    RivoSectionHeader(
                        title = "Recording Controls",
                        icon = Icons.Outlined.SettingsVoice
                    )
                    Spacer(Modifier.height(4.dp))
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        RivoSwitchListItem(
                            headline = "Enable Call Recording",
                            supporting = "Allow recording calls and show Record button during active calls",
                            leadingIcon = Icons.Outlined.Mic,
                            checked = callRecordingEnabled,
                            onCheckedChange = {
                                callRecordingEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_CALL_RECORDING, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = "Auto-Record Calls",
                            supporting = "Automatically record every call as soon as it connects",
                            leadingIcon = Icons.Outlined.PlayCircleOutline,
                            checked = autoRecordEnabled,
                            onCheckedChange = {
                                autoRecordEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_CALL_RECORDING_AUTO, it)
                            }
                        )
                    }
                }

                // 5. Audio Quality & Filters Section
                item {
                    RivoSectionHeader(
                        title = "Audio Quality & Filters",
                        icon = Icons.Outlined.GraphicEq
                    )
                    Spacer(Modifier.height(4.dp))
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        RivoSelectListItem(
                            headline = "Audio Bitrate",
                            supporting = "Higher bitrate yields clearer audio output",
                            leadingIcon = Icons.Outlined.HighQuality,
                            options = listOf(
                                "64 kbps (Compact)" to 64000,
                                "96 kbps (Balanced)" to 96000,
                                "128 kbps (High Quality)" to 128000,
                                "192 kbps (Ultra)" to 192000,
                                "256 kbps (Maximum)" to 256000
                            ),
                            selectedValue = bitrate,
                            onValueChange = {
                                bitrate = it
                                prefs.setInt("call_recording_bitrate", it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSelectListItem(
                            headline = "Minimum Duration Filter",
                            supporting = "Discard ultra-short calls below threshold",
                            leadingIcon = Icons.Outlined.Timer,
                            options = listOf(
                                "Record All Calls" to 0,
                                "Ignore calls < 3s" to 3,
                                "Ignore calls < 5s" to 5,
                                "Ignore calls < 10s" to 10
                            ),
                            selectedValue = minDurationFilter,
                            onValueChange = {
                                minDurationFilter = it
                                prefs.setInt("call_recording_min_duration", it)
                            }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { file ->
        RivoConfirmationDialog(
            onDismissRequest = { pendingDelete = null },
            onConfirm = {
                if (activePlayingFile?.absolutePath == file.absolutePath) {
                    activePlayingFile = null
                }
                CallRecorder.delete(file)
                pendingDelete = null
                refreshKey++
            },
            title = stringResource(R.string.call_recordings_delete_title),
            message = stringResource(R.string.call_recordings_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }

    if (showDeleteAllConfirm) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            onConfirm = {
                activePlayingFile = null
                recordings.forEach { file -> CallRecorder.delete(file) }
                showDeleteAllConfirm = false
                refreshKey++
            },
            title = "Delete all recordings?",
            message = "All ${recordings.size} recordings will be permanently removed from your device. This action cannot be undone.",
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Outlined.DeleteSweep,
            isDestructive = true
        )
    }
}

/**
 * MD3 Expressive Call Record Entry Card & Inline Player Component
 */
@Composable
fun CallRecordEntryCard(
    file: File,
    isCurrentActive: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    playbackSpeed: Float,
    onCardClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val durationFormatted = remember(file, durationMs, isCurrentActive) {
        if (isCurrentActive && durationMs > 0) {
            formatTimeMs(durationMs)
        } else {
            val dur = getAudioDurationMs(context, file)
            if (dur > 0) formatTimeMs(dur.toInt()) else null
        }
    }
    val dateFormatted = remember(file) {
        SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(file.lastModified()))
    }
    val sizeFormatted = remember(file) {
        val kb = file.length() / 1024
        if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
    }

    RivoExpressiveCard(
        modifier = modifier,
        containerColor = if (isCurrentActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed Header: Leading icon + info + single Play/Pause button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onCardClick() }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivoLeadingIconTile(
                    icon = if (isCurrentActive && isPlaying) Icons.AutoMirrored.Outlined.VolumeUp else Icons.Outlined.MicNone,
                    selected = isCurrentActive,
                    containerColor = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isCurrentActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.nameWithoutExtension,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = buildString {
                            append(dateFormatted)
                            if (durationFormatted != null) append(" • $durationFormatted")
                            append(" • $sizeFormatted")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Single prominent Play/Pause button
                Surface(
                    onClick = onPlayPauseClick,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCurrentActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCurrentActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (isCurrentActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Expanded Player Section
            AnimatedVisibility(
                visible = isCurrentActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Time Labels + Wavy Progress Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimeMs(currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(38.dp),
                            textAlign = TextAlign.Start
                        )

                        WavyAudioSlider(
                            value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                            onValueChange = { fraction ->
                                onSeekTo((fraction * durationMs).toInt())
                            },
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )

                        Text(
                            text = formatTimeMs(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(38.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Transport Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Pill
                        AssistChip(
                            onClick = {
                                val newSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                onSpeedChange(newSpeed)
                            },
                            label = { Text("${playbackSpeed}x", style = MaterialTheme.typography.labelSmall) },
                            shape = CircleShape,
                            modifier = Modifier.height(34.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        // Rewind 10s
                        Surface(
                            onClick = onRewind10,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Replay10,
                                    contentDescription = "Rewind 10s",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Center Play/Pause Hero Button
                        Surface(
                            onClick = onPlayPauseClick,
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(width = 64.dp, height = 52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Forward 10s
                        Surface(
                            onClick = onForward10,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Forward10,
                                    contentDescription = "Forward 10s",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Bottom Action Bar: Share & Delete (separated from playback controls)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onShareClick,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom MD3 Expressive Swirly/Wavy Audio Progress Slider
 */
@Composable
fun WavyAudioSlider(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    handleColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyPhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyPhaseAnimation"
    )

    val density = LocalDensity.current
    val amplitudePx = with(density) { 3.5.dp.toPx() }
    val wavelengthPx = with(density) { 20.dp.toPx() }
    val strokeWidthPx = with(density) { 3.5.dp.toPx() }
    val handleHeightPx = with(density) { 18.dp.toPx() }

    Box(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val centerY = size.height / 2f
            val currentX = (value.coerceIn(0f, 1f) * width)

            // 1. Active Wavy Line (0 to currentX)
            if (currentX > 0f) {
                val path = Path()
                path.moveTo(0f, centerY)
                var x = 0f
                val step = 3.dp.toPx()
                while (x <= currentX) {
                    val y = centerY + amplitudePx * sin((x / wavelengthPx) * 2 * Math.PI + phase).toFloat()
                    path.lineTo(x, y)
                    x += step
                }
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // 2. Vertical Scrubber Handle | (at currentX)
            drawLine(
                color = handleColor,
                start = Offset(currentX, centerY - handleHeightPx / 2f),
                end = Offset(currentX, centerY + handleHeightPx / 2f),
                strokeWidth = strokeWidthPx * 1.15f,
                cap = StrokeCap.Round
            )

            // 3. Inactive Straight Line (currentX to width)
            if (currentX < width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(currentX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Full-width OEM action row with icon, title, subtitle, and trailing arrow.
 * Replaces squeezed horizontal button layout.
 */
@Composable
private fun OemActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getAudioDurationMs(context: Context, file: File): Long {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        durationStr?.toLongOrNull() ?: 0L
    }.getOrDefault(0L)
}

private fun formatTimeMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}

@Preview(showBackground = true)
@Composable
fun CallRecordEntryCardPreview() {
    CompositionLocalProvider(
        LocalRivoSurfaceStyle provides RivoSurfaceStyle(showCards = true, showDividers = true)
    ) {
        MaterialTheme {
            Box(modifier = Modifier.padding(16.dp)) {
                CallRecordEntryCard(
                    file = File("Call_John_Doe_2026.m4a"),
                    isCurrentActive = true,
                    isPlaying = true,
                    currentPositionMs = 45000,
                    durationMs = 180000,
                    playbackSpeed = 1.0f,
                    onCardClick = {},
                    onPlayPauseClick = {},
                    onSeekTo = {},
                    onRewind10 = {},
                    onForward10 = {},
                    onSpeedChange = {},
                    onShareClick = {},
                    onDeleteClick = {}
                )
            }
        }
    }
}
