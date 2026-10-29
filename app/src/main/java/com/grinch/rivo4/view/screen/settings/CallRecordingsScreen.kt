package com.grinch.rivo4.view.screen.settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.grinch.rivo4.controller.permission.PermissionChecklistHelper
import com.grinch.rivo4.controller.shizuku.ShizukuConnectionManager
import com.grinch.rivo4.view.components.WavyAudioSlider
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.rivoCornerDp
import com.grinch.rivo4.view.theme.rivoCornerShape
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import com.grinch.rivo4.view.components.RivoResetButton
import com.grinch.rivo4.view.components.MenuTopAppBar

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
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Launch
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.SHIZU_CALL_RECORDER_URL
import com.grinch.rivo4.controller.AudioMetadataCache
import com.grinch.rivo4.controller.CallRecorder
import com.grinch.rivo4.controller.util.OemPermissionHelper
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.openLink
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

fun extractCallerLabel(nameWithoutExtension: String): String {
    val parts = nameWithoutExtension.split('_')
    return when {
        parts.size >= 3 -> {
            // Drop date and time parts at end (e.g. yyyyMMdd and HHmmss)
            parts.dropLast(2).joinToString("_").ifBlank { nameWithoutExtension }
        }
        parts.size == 2 -> {
            parts.first().ifBlank { nameWithoutExtension }
        }
        else -> nameWithoutExtension
    }
}

fun File.toCallRecordingItem(context: Context): CallRecordingItem {
    val lastMod = this.lastModified()
    val kb = this.length() / 1024
    val sizeStr = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
    val caller = extractCallerLabel(this.nameWithoutExtension)
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
    var showHowItWorksDialog by remember { mutableStateOf(false) }
    var shizukuCheckTrigger by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                shizukuCheckTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isShizukuInstalled = remember(shizukuCheckTrigger) {
        PermissionChecklistHelper.isShizukuInstalled(context)
    }
    val isShizukuRunning = remember(shizukuCheckTrigger) {
        PermissionChecklistHelper.isShizukuRunning()
    }
    val hasShizukuPermission = remember(shizukuCheckTrigger) {
        PermissionChecklistHelper.hasShizukuPermission(context)
    }

    // Settings state
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
            val displayName = CallRecorder.getFolderDisplayName(context, uri)
            prefs.saveCustomRecordingFolder(uri.toString(), displayName)
            refreshKey++
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.settings_recording_folder_changed, displayName),
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

    // Fast async recordings discovery with instant cache display and background duration resolution
    LaunchedEffect(refreshKey) {
        isLoadingRecordings = true
        val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        val rawFiles = withContext(Dispatchers.IO) {
            CallRecorder.listRecordings(context)
        }

        // 1. Immediately display the list with cached durations (or 0L if not yet cached)
        val initialItems = withContext(Dispatchers.Default) {
            rawFiles.map { file ->
                val lastMod = file.lastModified()
                val kb = file.length() / 1024
                val sizeStr = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
                val caller = extractCallerLabel(file.nameWithoutExtension)
                val duration = AudioMetadataCache.getCachedDurationMs(context, file) ?: 0L

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
        recordings = initialItems
        isLoadingRecordings = false

        // 2. Resolve any missing durations in background coroutines without blocking page load
        val unCached = initialItems.filter { it.durationMs <= 0L }
        if (unCached.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val updatedDurations = java.util.concurrent.ConcurrentHashMap<String, Long>()
                unCached.chunked(8).forEach { chunk ->
                    chunk.map { item ->
                        async {
                            val dur = AudioMetadataCache.getDurationMs(context, item.file)
                            if (dur > 0L) {
                                updatedDurations[item.file.absolutePath] = dur
                            }
                        }
                    }.awaitAll()
                }
                if (updatedDurations.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        recordings = recordings.map { item ->
                            val resolvedDur = updatedDurations[item.file.absolutePath]
                            if (resolvedDur != null && resolvedDur > 0L) {
                                item.copy(durationMs = resolvedDur)
                            } else {
                                item
                            }
                        }
                    }
                }
            }
        }
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
            delay(100)
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
        val query = searchQuery.trim()
        val filterNum = selectedFilterNumber?.trim()
        recordings.filter { item ->
            val timestamp = item.lastModified
            val matchesFrom = effectiveFrom == null || timestamp >= effectiveFrom
            val matchesTo = effectiveTo == null || timestamp <= effectiveTo
            val matchesSearch = query.isBlank() ||
                item.nameWithoutExtension.contains(query, ignoreCase = true) ||
                item.callerLabel.contains(query, ignoreCase = true)
            val matchesFilter = filterNum.isNullOrBlank() ||
                item.callerLabel.equals(filterNum, ignoreCase = true)
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
            DateFilterPreset.THIS_WEEK -> stringResource(R.string.analytics_time_this_week)
            DateFilterPreset.THIS_MONTH -> stringResource(R.string.analytics_time_this_month)
            DateFilterPreset.CUSTOM -> stringResource(R.string.ringtone_custom)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (showTopBar) {
                MenuTopAppBar(
                    text = stringResource(R.string.call_recordings_title),
                    onBackClick = {
                        if (showingRecordingsList && !initialShowList) {
                            showingRecordingsList = false
                        } else {
                            onNavigateBack()
                        }
                    },
                    actions = {
                        Surface(
                            onClick = { refreshKey++ },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = if (showingRecordingsList && filteredRecordings.isNotEmpty()) 8.dp else 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.loc_refresh),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (showingRecordingsList && filteredRecordings.isNotEmpty()) {
                            Surface(
                                onClick = { showDeleteAllConfirm = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = stringResource(R.string.loc_delete_all),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                        placeholder = { Text(stringResource(R.string.call_recordings_search)) },
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
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_clear_search))
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
                            label = { Text(stringResource(R.string.call_recordings_all_contacts)) }
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (recordings.isEmpty()) {
                                    stringResource(R.string.call_recordings_empty)
                                } else {
                                    stringResource(R.string.call_recordings_no_filter_matches)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (recordings.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.loc_recordings_size_total_recordings_are_saved_on_this_device, recordings.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(16.dp))
                                RivoResetButton(
                                    onClick = {
                                        searchQuery = ""
                                        datePreset = DateFilterPreset.ALL
                                        fromDateMillis = null
                                        toDateMillis = null
                                        selectedFilterNumber = null
                                    },
                                    text = stringResource(R.string.filter_reset_filters),
                                    icon = Icons.Default.FilterAltOff,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                )
                            }
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
                                val roundness = LocalCardRoundness.current
                                val shape = if (isCurrentActive) rivoCornerShape(20, roundness) else rivoGroupedItemShape(index, itemsInGroup.size)

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
                                        CallRecorder.share(context, item.file, context.getString(R.string.call_recordings_share))
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
                                    text = if (isLoadingRecordings) {
                                        stringResource(R.string.recordings_scanning)
                                    } else {
                                        stringResource(R.string.recordings_saved_count, recordings.size)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. How to Set Up Card (Compact & Modern MD3 Expressive)
                item {
                    Surface(
                        onClick = { showHowItWorksDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RivoLeadingIconTile(
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.call_recording_how_it_works_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = when {
                                                    hasShizukuPermission -> MaterialTheme.colorScheme.primary
                                                    isShizukuRunning -> MaterialTheme.colorScheme.tertiary
                                                    isShizukuInstalled -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.outline
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = when {
                                            hasShizukuPermission -> stringResource(R.string.call_recording_status_ready)
                                            isShizukuRunning -> stringResource(R.string.call_recording_status_needs_perm)
                                            isShizukuInstalled -> stringResource(R.string.call_recording_status_stopped)
                                            else -> stringResource(R.string.call_recording_status_not_installed)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
                                                text = stringResource(
                                                    R.string.loc_oempermissionhelper_getoembranddisplayname_optimization,
                                                    OemPermissionHelper.getOemBrandDisplayName()
                                                ),
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
                                                contentDescription = stringResource(R.string.loc_dismiss),
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
                                headline = stringResource(R.string.loc_saved_call_recordings),
                                supporting = if (isLoadingRecordings) {
                                    stringResource(R.string.recordings_scanning)
                                } else {
                                    stringResource(R.string.recordings_available_count, recordings.size)
                                },
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
                        title = stringResource(R.string.loc_recording_controls),
                        icon = Icons.Outlined.SettingsVoice
                    ) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.loc_auto_record_calls),
                                supporting = stringResource(R.string.loc_automatically_record_calls_as_soon_as_they_connect),
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
                                    headline = stringResource(R.string.loc_auto_record_filter),
                                    supporting = when (autoRecordFilter) {
                                        PreferenceManager.RECORD_FILTER_INCOMING_ONLY -> stringResource(R.string.call_recordings_filter_incoming_only_desc)
                                        PreferenceManager.RECORD_FILTER_OUTGOING_ONLY -> stringResource(R.string.call_recordings_filter_outgoing_only_desc)
                                        PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY -> stringResource(R.string.call_recordings_filter_unknown_only_desc)
                                        PreferenceManager.RECORD_FILTER_CONTACTS_ONLY -> stringResource(R.string.call_recordings_filter_contacts_only_desc)
                                        else -> stringResource(R.string.call_recordings_filter_all_desc)
                                    },
                                    leadingIcon = Icons.Outlined.FilterList,
                                    options = listOf(
                                        stringResource(R.string.call_recordings_filter_all) to PreferenceManager.RECORD_FILTER_ALL,
                                        stringResource(R.string.call_recordings_filter_incoming_only) to PreferenceManager.RECORD_FILTER_INCOMING_ONLY,
                                        stringResource(R.string.call_recordings_filter_outgoing_only) to PreferenceManager.RECORD_FILTER_OUTGOING_ONLY,
                                        stringResource(R.string.call_recordings_filter_unknown_only) to PreferenceManager.RECORD_FILTER_UNKNOWN_ONLY,
                                        stringResource(R.string.call_recordings_filter_contacts_only) to PreferenceManager.RECORD_FILTER_CONTACTS_ONLY
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

                // 5. Storage & Location Section
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
                CallRecorder.delete(context, file)
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
                recordings.forEach { item -> CallRecorder.delete(context, item.file) }
                showDeleteAllConfirm = false
                refreshKey++
            },
            title = stringResource(R.string.loc_delete_all_recordings),
            message = stringResource(
                R.string.loc_all_recordings_size_recordings_will_be_permanently_removed_from_your_d,
                recordings.size
            ),
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
                        text = stringResource(R.string.loc_select_start_date_from),
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
                        text = stringResource(R.string.loc_select_end_date_to),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                }
            )
        }
    }

    if (showHowItWorksDialog) {
        HowCallRecordingWorksDialog(
            onDismissRequest = { showHowItWorksDialog = false },
            isInstalled = isShizukuInstalled,
            isRunning = isShizukuRunning,
            hasPermission = hasShizukuPermission
        )
    }
}

@Composable
fun CallRecordEntryCard(
    item: CallRecordingItem,
    isCurrentActive: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    playbackSpeed: Float = 1.0f,
    onCardClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSpeedChange: (Float) -> Unit = {},
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape? = null
) {
    val roundness = LocalCardRoundness.current
    val effectiveCardShape = shape ?: rivoCornerShape(20, roundness)

    val iconTileShape = rivoCornerShape(14, roundness)
    val buttonShape = rivoCornerShape(14, roundness)
    val actionShape = rivoCornerShape(12, roundness)

    val durationFormatted = remember(item.durationMs, durationMs, isCurrentActive) {
        val dur = if (isCurrentActive && durationMs > 0) durationMs.toLong() else item.durationMs
        if (dur > 0) formatTimeMs(dur.toInt()) else null
    }

    val animatedCardBg by animateColorAsState(
        targetValue = if (isCurrentActive) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "CallRecordCardBg"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = effectiveCardShape,
        color = animatedCardBg,
        tonalElevation = if (isCurrentActive) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Line 1: Leading icon + Title/Date/Size + Action Icons (Share & Delete when active, or Play when collapsed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = iconTileShape,
                    color = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCurrentActive && isPlaying) Icons.AutoMirrored.Outlined.VolumeUp else Icons.Outlined.MicNone,
                            contentDescription = null,
                            tint = if (isCurrentActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nameWithoutExtension,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = buildString {
                            append(item.dateFormatted)
                            if (durationFormatted != null) append(" • $durationFormatted")
                            append(" • ${item.sizeFormatted}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (isCurrentActive) {
                    // Share icon button (follows container roundness, no border)
                    Surface(
                        onClick = onShareClick,
                        shape = actionShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.call_recordings_share),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Delete icon button (follows container roundness, no border)
                    Surface(
                        onClick = onDeleteClick,
                        shape = actionShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Collapsed: single play icon button
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        shape = buttonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.loc_play),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Line 2: Expanded Transport Controls (Rewind, Play/Pause, Forward) + Smooth Squiggly Progress
            AnimatedVisibility(
                visible = isCurrentActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s button (follows container roundness, no border)
                    Surface(
                        onClick = onRewind10,
                        shape = actionShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Replay10,
                                contentDescription = stringResource(R.string.loc_rewind_10s),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Hero Play/Pause button
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        shape = buttonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.call_recordings_pause else R.string.call_recordings_play),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Forward 10s button (follows container roundness, no border)
                    Surface(
                        onClick = onForward10,
                        shape = actionShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Forward10,
                                contentDescription = stringResource(R.string.loc_forward_10s),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Right side: Squiggly Progress slider with smooth continuous VSYNC interpolation
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val totalDur = if (durationMs > 0) durationMs else item.durationMs.toInt()

                        val currentPosState = rememberUpdatedState(currentPositionMs)
                        val playbackSpeedState = rememberUpdatedState(playbackSpeed)

                        var smoothProgress by remember {
                            mutableFloatStateOf(if (totalDur > 0) (currentPositionMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f)
                        }

                        // When paused or stopped: sync immediately if currentPositionMs or totalDur changes
                        LaunchedEffect(currentPositionMs, totalDur, isPlaying) {
                            if (!isPlaying || totalDur <= 0) {
                                smoothProgress = if (totalDur > 0) (currentPositionMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f
                            }
                        }

                        // During playback: glide smoothly on every display frame (60Hz / 120Hz VSYNC)
                        LaunchedEffect(isPlaying, totalDur) {
                            if (!isPlaying || totalDur <= 0) return@LaunchedEffect

                            var basePos = currentPosState.value.toFloat()
                            var lastElapsed = android.os.SystemClock.elapsedRealtime()

                            while (isActive && isPlaying) {
                                withFrameMillis {
                                    val now = android.os.SystemClock.elapsedRealtime()
                                    val deltaMs = (now - lastElapsed).toFloat()
                                    lastElapsed = now

                                    val polledMs = currentPosState.value.toFloat()
                                    // If user jumped (seek, rewind, fast forward)
                                    if (kotlin.math.abs(polledMs - basePos) > 300f) {
                                        basePos = polledMs
                                    } else {
                                        basePos = (basePos + deltaMs * playbackSpeedState.value).coerceAtMost(totalDur.toFloat())
                                    }

                                    smoothProgress = (basePos / totalDur.toFloat()).coerceIn(0f, 1f)
                                }
                                if (basePos >= totalDur) break
                            }
                        }

                        val displayTimeMs = if (isPlaying && totalDur > 0) {
                            (smoothProgress * totalDur).toInt()
                        } else {
                            currentPositionMs
                        }

                        WavyAudioSlider(
                            value = smoothProgress,
                            onValueChange = { fraction ->
                                onSeekTo((fraction * totalDur).toInt())
                            },
                            isPlaying = isPlaying,
                            sliderHeight = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(1.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeMs(displayTimeMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = formatTimeMs(totalDur),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    shape: androidx.compose.ui.graphics.Shape? = null
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

@Composable
fun HowCallRecordingWorksDialog(
    onDismissRequest: () -> Unit,
    isInstalled: Boolean,
    isRunning: Boolean,
    hasPermission: Boolean
) {
    val context = LocalContext.current
    val allReady = isInstalled && isRunning && hasPermission

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.call_recording_how_it_works_title),
        icon = Icons.Outlined.GraphicEq,
        confirmAction = RivoDialogAction(
            label = stringResource(R.string.call_recording_action_got_it),
            onClick = onDismissRequest
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Modern Explainer
            Text(
                text = stringResource(R.string.call_recording_compact_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status Bar / Card
            if (allReady) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.call_recording_ready_banner_desc),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactStatusChip(
                            label = stringResource(R.string.call_recording_status_app_installed),
                            isOk = isInstalled
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        CompactStatusChip(
                            label = stringResource(R.string.call_recording_status_service),
                            isOk = isRunning
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        CompactStatusChip(
                            label = stringResource(R.string.call_recording_status_permission),
                            isOk = hasPermission
                        )
                    }
                }
            }

            // Compact 3-Step Setup Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Step 1: Install
                    CompactStepRow(
                        stepNumber = "1",
                        title = stringResource(R.string.call_recording_step1_compact_title),
                        description = stringResource(R.string.call_recording_step1_compact_desc),
                        isComplete = isInstalled,
                        action = if (!isInstalled) {
                            {
                                FilledTonalButton(
                                    onClick = {
                                        try {
                                            val marketIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=${PermissionChecklistHelper.SHIZUKU_PACKAGE}")
                                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(marketIntent)
                                        } catch (_: Exception) {
                                            val webIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/apps/details?id=${PermissionChecklistHelper.SHIZUKU_PACKAGE}")
                                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(webIntent)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(stringResource(R.string.call_recording_action_install), fontSize = 12.sp)
                                }
                            }
                        } else null
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Step 2: Start Service
                    CompactStepRow(
                        stepNumber = "2",
                        title = stringResource(R.string.call_recording_step2_compact_title),
                        description = stringResource(R.string.call_recording_step2_compact_desc),
                        isComplete = isRunning,
                        action = if (!isRunning) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isInstalled) {
                                        FilledTonalButton(
                                            onClick = {
                                                try {
                                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(
                                                        PermissionChecklistHelper.SHIZUKU_PACKAGE
                                                    )?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                    if (launchIntent != null) context.startActivity(launchIntent)
                                                } catch (_: Exception) {}
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(stringResource(R.string.call_recording_action_open), fontSize = 12.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = { openLink(context, "https://shizuku.rikka.app/guide/setup/") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Launch,
                                            contentDescription = stringResource(R.string.call_recording_action_guide),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else null
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Step 3: Authorize
                    CompactStepRow(
                        stepNumber = "3",
                        title = stringResource(R.string.call_recording_step3_compact_title),
                        description = stringResource(R.string.call_recording_step3_compact_desc),
                        isComplete = hasPermission,
                        action = if (isRunning && !hasPermission) {
                            {
                                Button(
                                    onClick = {
                                        try {
                                            ShizukuConnectionManager.requestPermission()
                                        } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(stringResource(R.string.call_recording_action_grant), fontSize = 12.sp)
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStepRow(
    stepNumber: String,
    title: String,
    description: String,
    isComplete: Boolean,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isComplete) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (isComplete) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (action != null) {
            Spacer(Modifier.width(8.dp))
            action()
        }
    }
}

@Composable
private fun CompactStatusChip(label: String, isOk: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isOk) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
