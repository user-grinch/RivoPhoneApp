package com.grinch.rivo4.view.screen.settings

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.AudioMetadataCache
import com.grinch.rivo4.controller.CallRecorder
import com.grinch.rivo4.controller.util.OemPermissionHelper
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Immutable model for call recording items to allow Compose smart recomposition skipping.
 */
@Immutable
data class CallRecordingItem(
    val file: File,
    val nameWithoutExtension: String,
    val callerLabel: String,
    val dateHeader: String,
    val dateFormatted: String,
    val sizeFormatted: String,
    val lastModified: Long,
    val durationMs: Long
)

/**
 * Converts a raw recording [File] into a pre-computed [CallRecordingItem].
 */
fun File.toCallRecordingItem(context: Context): CallRecordingItem {
    val lastMod = this.lastModified()
    val kb = this.length() / 1024
    val sizeStr = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
    val caller = this.nameWithoutExtension
        .substringBeforeLast('_')
        .substringBeforeLast('_')
    val duration = AudioMetadataCache.getDurationMsSync(context, this)

    return CallRecordingItem(
        file = this,
        nameWithoutExtension = this.nameWithoutExtension,
        callerLabel = caller,
        dateHeader = formatDateHeader(context, lastMod),
        dateFormatted = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(lastMod)),
        sizeFormatted = sizeStr,
        lastModified = lastMod,
        durationMs = duration
    )
}

private enum class DateFilterPreset {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

private fun formatDateHeader(context: Context, timestamp: Long): String {
    val cal = Calendar.getInstance()
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

    return when {
        timestamp >= todayStart -> context.getString(R.string.date_today)
        timestamp >= yesterdayStart -> context.getString(R.string.date_yesterday)
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Destination<RootGraph>
@Composable
fun CallRecordingsScreen(
    initialShowList: Boolean = false,
    navigator: DestinationsNavigator
) {
    CallRecordingsContent(
        initialShowList = initialShowList,
        onNavigateBack = { navigator.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRecordingsContent(
    initialShowList: Boolean = false,
    showTopBar: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }

    var refreshKey by remember { mutableIntStateOf(0) }
    var recordings by remember { mutableStateOf<List<CallRecordingItem>>(emptyList()) }
    var isLoadingRecordings by remember { mutableStateOf(true) }

    var showingRecordingsList by remember { mutableStateOf(initialShowList) }

    // Settings state
    var callRecordingEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_RECORDING, true))
    }
    var autoRecordEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_RECORDING_AUTO, false))
    }
    var autoRecordFilter by remember {
        mutableIntStateOf(
            prefs.getInt(
                PreferenceManager.KEY_CALL_RECORDING_FILTER,
                PreferenceManager.RECORD_FILTER_ALL
            )
        )
    }

    var bitrate by remember {
        mutableIntStateOf(prefs.getInt("call_recording_bitrate", 128000))
    }
    var minDurationFilter by remember {
        mutableIntStateOf(prefs.getInt("call_recording_min_duration", 0))
    }

    // Storage path state
    val customFolderName = remember(refreshKey) { prefs.getCustomRecordingFolderName() }
    val customFolderUri = remember(refreshKey) { prefs.getCustomRecordingFolderUri() }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            prefs.saveCustomRecordingFolder(uri.toString())
            refreshKey++
            android.widget.Toast.makeText(
                context,
                "Save folder updated",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Audio Playback state
    var activePlayingFile by remember { mutableStateOf<File?>(null) }
    var isRecordingPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var recordingDurationMs by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Dialog state
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterNumber by remember { mutableStateOf<String?>(null) }
    var datePreset by remember { mutableStateOf(DateFilterPreset.ALL) }
    var fromDateMillis by remember { mutableStateOf<Long?>(null) }
    var toDateMillis by remember { mutableStateOf<Long?>(null) }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    // Async recordings discovery off main thread
    LaunchedEffect(refreshKey) {
        isLoadingRecordings = true
        recordings = withContext(Dispatchers.IO) {
            val rawFiles = CallRecorder.listRecordings(context)
            val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            rawFiles.map { file ->
                val lastMod = file.lastModified()
                val kb = file.length() / 1024
                val sizeStr = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
                val caller = file.nameWithoutExtension
                    .substringBeforeLast('_')
                    .substringBeforeLast('_')
                val duration = AudioMetadataCache.getDurationMsSync(context, file)

                CallRecordingItem(
                    file = file,
                    nameWithoutExtension = file.nameWithoutExtension,
                    callerLabel = caller,
                    dateHeader = formatDateHeader(context, lastMod),
                    dateFormatted = dateFormat.format(Date(lastMod)),
                    sizeFormatted = sizeStr,
                    lastModified = lastMod,
                    durationMs = duration
                )
            }
        }
        isLoadingRecordings = false
    }

    // Managed MediaPlayer Lifecycle
    DisposableEffect(activePlayingFile) {
        if (activePlayingFile != null) {
            val mp = MediaPlayer().apply {
                try {
                    setDataSource(context, Uri.fromFile(activePlayingFile))
                    prepare()
                    start()
                    this@apply.playbackParams = this@apply.playbackParams.setSpeed(playbackSpeed)
                } catch (_: Exception) {
                }
            }
            mediaPlayer = mp
            isRecordingPlaying = mp.isPlaying
            recordingDurationMs = runCatching { mp.duration }.getOrDefault(0)

            onDispose {
                runCatching {
                    mp.stop()
                    mp.release()
                }
                mediaPlayer = null
                isRecordingPlaying = false
            }
        } else {
            onDispose { }
        }
    }

    // Playback Progress Poll Loop
    LaunchedEffect(isRecordingPlaying, activePlayingFile) {
        while (isRecordingPlaying && activePlayingFile != null) {
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

    val (effectiveFrom, effectiveTo) = remember(datePreset, fromDateMillis, toDateMillis) {
        val cal = Calendar.getInstance()
        when (datePreset) {
            DateFilterPreset.ALL -> null to null
            DateFilterPreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                start to cal.timeInMillis
            }

            DateFilterPreset.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                start to cal.timeInMillis
            }

            DateFilterPreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val now = System.currentTimeMillis()
                start to now
            }

            DateFilterPreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val now = System.currentTimeMillis()
                start to now
            }

            DateFilterPreset.CUSTOM -> fromDateMillis to toDateMillis
        }
    }

    val filteredRecordings = remember(recordings, searchQuery, effectiveFrom, effectiveTo, selectedFilterNumber) {
        recordings.filter { item ->
            val timestamp = item.lastModified
            val matchesFrom = effectiveFrom == null || timestamp >= effectiveFrom
            val matchesTo = effectiveTo == null || timestamp <= effectiveTo
            val matchesSearch =
                searchQuery.isBlank() || item.nameWithoutExtension.contains(searchQuery, ignoreCase = true)
            val matchesFilter =
                selectedFilterNumber == null || item.callerLabel.equals(selectedFilterNumber, ignoreCase = true)
            matchesFrom && matchesTo && matchesSearch && matchesFilter
        }
    }

    val uniqueCallerLabels = remember(recordings) {
        recordings.map { it.callerLabel }.distinct().sorted()
    }

    val groupedRecordings = remember(filteredRecordings) {
        filteredRecordings
            .sortedByDescending { it.lastModified }
            .groupBy { it.dateHeader }
    }

    val dateFormatPresetLabel: @Composable (DateFilterPreset) -> String = { preset ->
        when (preset) {
            DateFilterPreset.ALL -> stringResource(R.string.filter_all)
            DateFilterPreset.TODAY -> stringResource(R.string.date_today)
            DateFilterPreset.YESTERDAY -> stringResource(R.string.date_yesterday)
            DateFilterPreset.THIS_WEEK -> "This Week"
            DateFilterPreset.THIS_MONTH -> "This Month"
            DateFilterPreset.CUSTOM -> "Custom"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.call_recordings_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (showingRecordingsList && !initialShowList) {
                                    showingRecordingsList = false
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    actions = {
                        if (showingRecordingsList) {
                            if (filteredRecordings.isNotEmpty()) {
                                IconButton(onClick = { showDeleteAllConfirm = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = "Delete All",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { refreshKey++ }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (showingRecordingsList) {
            // VIEW MODE: Saved Call Recordings List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Modern MD3 Expressive Search Surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search recordings…") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true
                    )
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateFilterPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = datePreset == preset,
                            onClick = {
                                datePreset = preset
                                if (preset == DateFilterPreset.CUSTOM) {
                                    showFromDatePicker = true
                                }
                            },
                            label = { Text(dateFormatPresetLabel(preset)) },
                            leadingIcon = if (datePreset == preset) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                if (uniqueCallerLabels.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilterNumber == null,
                            onClick = { selectedFilterNumber = null },
                            label = { Text("All Contacts") }
                        )
                        uniqueCallerLabels.forEach { label ->
                            FilterChip(
                                selected = selectedFilterNumber == label,
                                onClick = {
                                    selectedFilterNumber = if (selectedFilterNumber == label) null else label
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (isLoadingRecordings) {
                    RecordingLoadingIndicator(modifier = Modifier.fillMaxSize())
                } else if (filteredRecordings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (recordings.isEmpty()) stringResource(R.string.call_recordings_empty) else "No recordings found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        groupedRecordings.entries.forEachIndexed { groupIndex, (header, itemsInGroup) ->
                            item(key = "header_${header}_$groupIndex") {
                                RivoSectionHeader(
                                    title = header,
                                    modifier = Modifier.padding(top = if (groupIndex == 0) 4.dp else 16.dp, bottom = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                )
                            }

                            itemsIndexed(
                                items = itemsInGroup,
                                key = { _, item -> item.file.absolutePath }
                            ) { index, item ->
                                val isCurrentActive = activePlayingFile?.absolutePath == item.file.absolutePath
                                val shape = rivoGroupedItemShape(index, itemsInGroup.size)

                                CallRecordEntryCard(
                                    item = item,
                                    shape = shape,
                                    isCurrentActive = isCurrentActive,
                                    isPlaying = isCurrentActive && isRecordingPlaying,
                                    currentPositionMs = if (isCurrentActive) currentPositionMs else 0,
                                    durationMs = if (isCurrentActive) recordingDurationMs else 0,
                                    playbackSpeed = playbackSpeed,
                                    onCardClick = {
                                        if (isCurrentActive) {
                                            mediaPlayer?.let { mp ->
                                                if (mp.isPlaying) {
                                                    mp.pause()
                                                    isRecordingPlaying = false
                                                } else {
                                                    mp.start()
                                                    isRecordingPlaying = true
                                                }
                                            }
                                        } else {
                                            activePlayingFile = item.file
                                        }
                                    },
                                    onPlayPauseClick = {
                                        if (isCurrentActive) {
                                            mediaPlayer?.let { mp ->
                                                if (mp.isPlaying) {
                                                    mp.pause()
                                                    isRecordingPlaying = false
                                                } else {
                                                    mp.start()
                                                    isRecordingPlaying = true
                                                }
                                            }
                                        } else {
                                            activePlayingFile = item.file
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
                                            val newPos = (currentPositionMs + 10000).coerceAtMost(recordingDurationMs)
                                            currentPositionMs = newPos
                                            mediaPlayer?.seekTo(newPos)
                                        }
                                    },
                                    onSpeedChange = { newSpeed ->
                                        playbackSpeed = newSpeed
                                        mediaPlayer?.let { mp ->
                                            runCatching {
                                                mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                                            }
                                        }
                                    },
                                    onShareClick = {
                                        CallRecorder.share(context, item.file, "Share Recording")
                                    },
                                    onDeleteClick = {
                                        pendingDelete = item.file
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // SETTINGS MODE: Full Expressive Settings Page
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Summary Header Card
                item {
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RivoLeadingIconTile(
                                icon = Icons.Outlined.Mic,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.call_recordings_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isLoadingRecordings) "Scanning recordings..." else "${recordings.size} saved recordings",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. OEM Device Optimization Warning Tile
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
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
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

                // 3. Saved Call Recordings Navigation Tile
                item {
                    RivoExpressiveGroup {
                        item {
                            RivoListItem(
                                headline = "Saved Call Recordings",
                                supporting = if (isLoadingRecordings) "Scanning recordings..." else "${recordings.size} recordings available",
                                leadingIcon = Icons.Outlined.LibraryMusic,
                                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = { showingRecordingsList = true }
                            )
                        }
                    }
                }

                // 4. Recording Controls Section
                item {
                    RivoExpressiveGroup(
                        title = "Recording Controls",
                        icon = Icons.Outlined.SettingsVoice
                    ) {
                        item {
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
                        }
                        item {
                            RivoSwitchListItem(
                                headline = "Auto-Record Calls",
                                supporting = "Automatically record calls as soon as they connect",
                                leadingIcon = Icons.Outlined.PlayCircleOutline,
                                checked = autoRecordEnabled,
                                onCheckedChange = {
                                    autoRecordEnabled = it
                                    prefs.setBoolean(PreferenceManager.KEY_CALL_RECORDING_AUTO, it)
                                }
                            )
                        }
                        if (autoRecordEnabled) {
                            item {
                                RivoSelectListItem(
                                    headline = "Auto-Record Filter",
                                    supporting = when (autoRecordFilter) {
                                        PreferenceManager.RECORD_FILTER_INCOMING_ONLY -> "Recording incoming calls only"
                                        PreferenceManager.RECORD_FILTER_OUTGOING_ONLY -> "Recording outgoing calls only"
                                        PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY -> "Recording unknown numbers only"
                                        PreferenceManager.RECORD_FILTER_CONTACTS_ONLY -> "Recording saved contacts only"
                                        else -> "Recording all calls"
                                    },
                                    leadingIcon = Icons.Outlined.FilterList,
                                    options = listOf(
                                        "All Calls" to PreferenceManager.RECORD_FILTER_ALL,
                                        "Incoming Calls Only" to PreferenceManager.RECORD_FILTER_INCOMING_ONLY,
                                        "Outgoing Calls Only" to PreferenceManager.RECORD_FILTER_OUTGOING_ONLY,
                                        "Unknown Numbers Only" to PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY,
                                        "Saved Contacts Only" to PreferenceManager.RECORD_FILTER_CONTACTS_ONLY
                                    ),
                                    selectedValue = autoRecordFilter,
                                    onValueChange = {
                                        autoRecordFilter = it
                                        prefs.setInt(PreferenceManager.KEY_CALL_RECORDING_FILTER, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Audio Quality & Filters Section
                item {
                    RivoExpressiveGroup(
                        title = "Audio Quality & Filters",
                        icon = Icons.Outlined.GraphicEq
                    ) {
                        item {
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
                        }
                        item {
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

                // 6. Storage & Location Section
                item {
                    RivoExpressiveGroup(
                        title = stringResource(R.string.settings_recording_storage_title),
                        icon = Icons.Outlined.Folder
                    ) {
                        val currentFolderText =
                            customFolderName ?: stringResource(R.string.settings_recording_save_folder_default)

                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_recording_save_folder),
                                supporting = currentFolderText,
                                leadingIcon = Icons.Outlined.FolderOpen,
                                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = { folderPickerLauncher.launch(null) }
                            )
                        }

                        if (!customFolderUri.isNullOrBlank()) {
                            item {
                                RivoListItem(
                                    headline = stringResource(R.string.settings_recording_reset_folder),
                                    supporting = stringResource(R.string.settings_recording_reset_folder_supporting),
                                    leadingIcon = Icons.Outlined.Restore,
                                    onClick = {
                                        prefs.resetCustomRecordingFolder()
                                        refreshKey++
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_recording_reset_folder_toast),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
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
                recordings.forEach { item -> CallRecorder.delete(item.file) }
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

    if (showFromDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fromDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            fromDateMillis = localCal.timeInMillis
                            datePreset = DateFilterPreset.CUSTOM
                        }
                        showFromDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Start Date (From)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                }
            )
        }
    }

    if (showToDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = toDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            toDateMillis = localCal.timeInMillis
                            datePreset = DateFilterPreset.CUSTOM
                        }
                        showToDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select End Date (To)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}

/**
 * MD3 Expressive Call Record Entry Card & Inline Player Component
 */
@Composable
fun CallRecordEntryCard(
    item: CallRecordingItem,
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
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)
) {
    val durationFormatted = remember(item.durationMs, durationMs, isCurrentActive) {
        val dur = if (isCurrentActive && durationMs > 0) durationMs.toLong() else item.durationMs
        if (dur > 0) formatTimeMs(dur.toInt()) else null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isCurrentActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Collapsed Header: Leading icon + info + single Play/Pause button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivoLeadingIconTile(
                    icon = if (isCurrentActive && isPlaying) Icons.AutoMirrored.Outlined.VolumeUp else Icons.Outlined.MicNone,
                    selected = isCurrentActive,
                    containerColor = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isCurrentActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nameWithoutExtension,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            append(item.dateFormatted)
                            if (durationFormatted != null) append(" • $durationFormatted")
                            append(" • ${item.sizeFormatted}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Single prominent Play/Pause button
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = if (isCurrentActive && isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCurrentActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (isCurrentActive && isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
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

                    // Bottom Action Bar: Share & Delete
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
 * Legacy overload for backward compatibility with [File].
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
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)
) {
    val context = LocalContext.current
    val item = remember(file) { file.toCallRecordingItem(context) }
    CallRecordEntryCard(
        item = item,
        isCurrentActive = isCurrentActive,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        playbackSpeed = playbackSpeed,
        onCardClick = onCardClick,
        onPlayPauseClick = onPlayPauseClick,
        onSeekTo = onSeekTo,
        onRewind10 = onRewind10,
        onForward10 = onForward10,
        onSpeedChange = onSpeedChange,
        onShareClick = onShareClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier,
        shape = shape
    )
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
                    val y = centerY + amplitudePx * kotlin.math.sin((x / wavelengthPx) * 2 * Math.PI + phase).toFloat()
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

private fun formatTimeMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}
