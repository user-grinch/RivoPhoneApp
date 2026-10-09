package com.grinch.rivo4.controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.grinch.rivo4.IShellService
import com.grinch.rivo4.controller.shizuku.ScrcpyAudioCodec
import com.grinch.rivo4.controller.shizuku.ScrcpyAudioMuxer
import com.grinch.rivo4.controller.shizuku.ScrcpyClient
import com.grinch.rivo4.controller.shizuku.ScrcpyConfig
import com.grinch.rivo4.controller.shizuku.ShizukuConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object AudioMetadataCache {
    private val durationCache = ConcurrentHashMap<String, Long>()
    @Volatile
    private var prefs: android.content.SharedPreferences? = null

    private fun getPrefs(context: Context): android.content.SharedPreferences {
        var p = prefs
        if (p == null) {
            synchronized(this) {
                p = prefs
                if (p == null) {
                    p = context.applicationContext.getSharedPreferences("call_recording_durations_cache", Context.MODE_PRIVATE)
                    prefs = p
                }
            }
        }
        return p!!
    }

    fun getCachedDurationMs(file: File): Long? {
        val key = "${file.absolutePath}_${file.lastModified()}"
        return durationCache[key]
    }

    fun getCachedDurationMs(context: Context, file: File): Long? {
        val key = "${file.absolutePath}_${file.lastModified()}"
        durationCache[key]?.let { return it }
        try {
            val persisted = getPrefs(context).getLong(key, -1L)
            if (persisted >= 0L) {
                durationCache[key] = persisted
                return persisted
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun getDurationMs(context: Context, file: File): Long = withContext(Dispatchers.IO) {
        val key = "${file.absolutePath}_${file.lastModified()}"
        getCachedDurationMs(context, file)?.let { return@withContext it }
        val dur = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(file))
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        }.getOrDefault(0L)
        durationCache[key] = dur
        try {
            getPrefs(context).edit().putLong(key, dur).apply()
        } catch (_: Exception) {}
        dur
    }

    fun getDurationMsSync(context: Context, file: File): Long {
        val key = "${file.absolutePath}_${file.lastModified()}"
        getCachedDurationMs(context, file)?.let { return it }
        val dur = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(file))
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        }.getOrDefault(0L)
        durationCache[key] = dur
        try {
            getPrefs(context).edit().putLong(key, dur).apply()
        } catch (_: Exception) {}
        return dur
    }

    fun evict(file: File) {
        val key = "${file.absolutePath}_${file.lastModified()}"
        durationCache.remove(key)
    }

    fun clear() {
        durationCache.clear()
    }
}

object CallRecorder {

    private const val TAG = "CallRecorder"

    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startJob: Job? = null
    private var activeContext: Context? = null

    private val isStarting = AtomicBoolean(false)
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds = _durationSeconds.asStateFlow()

    @Volatile
    var lastRecordedFile: File? = null
        private set

    @Volatile
    var lastRecordedTimeMillis: Long = 0L
        private set

    fun clearLastRecordedFile() {
        lastRecordedFile = null
        lastRecordedTimeMillis = 0L
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    // Shizuku recording pipeline components
    // Note: The Shizuku call recording pipeline in this class is adapted from ShizuCallRecorder
    // (Copyright (C) 2026-present kitsumed (Med), licensed under GPLv3 with Section 7 additional terms).
    private var shizukuManager: ShizukuConnectionManager? = null
    private var shellService: IShellService? = null
    private var prewarmedShizukuManager: ShizukuConnectionManager? = null
    private var prewarmedShellService: IShellService? = null
    private var prewarmJob: Deferred<IShellService?>? = null
    private var scrcpyClient: ScrcpyClient? = null
    private var scrcpyMuxer: ScrcpyAudioMuxer? = null
    private var recordingScope: CoroutineScope? = null
    private var durationJob: Job? = null
    private var clientJob: Job? = null
    private var isUsingShizuku = false

    const val DIRECTORY_NAME = "Rivo Recordings"

    @Volatile
    private var recordingStartTimeMillis: Long = 0L

    @Volatile
    private var lastWorkingSource: Int = MediaRecorder.AudioSource.MIC

    private val audioSources = listOf(
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.DEFAULT,
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.VOICE_CALL
    )

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            false
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isWritableDirectory(dir: File): Boolean {
        return runCatching {
            if (!dir.exists() && !dir.mkdirs()) return false
            val testFile = File(dir, ".probe_${System.currentTimeMillis()}")
            val created = testFile.createNewFile()
            if (created) {
                testFile.delete()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun getCustomRecordingDirectory(context: Context): File? {
        val prefs = try {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        val uriStr =
            prefs?.getString(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_FOLDER_URI, null)
                ?: return null
        return runCatching {
            getFileFromTreeUri(Uri.parse(uriStr))
        }.getOrNull()
    }

    fun getFileFromTreeUri(treeUri: Uri): File? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            if (docId.startsWith("primary:")) {
                val relPath = docId.removePrefix("primary:").trim('/')
                if (relPath.isEmpty()) {
                    Environment.getExternalStorageDirectory()
                } else {
                    File(Environment.getExternalStorageDirectory(), relPath)
                }
            } else if (docId.contains(":")) {
                val parts = docId.split(":", limit = 2)
                val storageId = parts[0]
                val relPath = parts.getOrNull(1)?.trim('/') ?: ""
                val base = File("/storage/$storageId")
                if (base.exists()) {
                    if (relPath.isEmpty()) base else File(base, relPath)
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFolderDisplayName(context: Context, treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            if (docId.startsWith("primary:")) {
                val rel = docId.removePrefix("primary:").trim('/')
                if (rel.isEmpty()) "Internal Storage" else rel
            } else if (docId.contains(":")) {
                val parts = docId.split(":", limit = 2)
                val rel = parts.getOrNull(1)?.trim('/') ?: ""
                if (rel.isEmpty()) parts[0] else "${parts[0]}/$rel"
            } else {
                DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Custom Folder"
            }
        } catch (e: Exception) {
            DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Custom Folder"
        }
    }

    fun getStandardRecordingsDirectory(context: Context): File {
        // 1. If All Files Access is granted, allow direct root storage
        if (hasStoragePermission(context)) {
            val directInternal = File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME)
            if (isWritableDirectory(directInternal)) {
                return directInternal
            }
        }

        // 2. Standard public Recordings folder: /storage/emulated/0/Recordings/Rivo Recordings
        val pubRecordings = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS),
            DIRECTORY_NAME
        )
        if (isWritableDirectory(pubRecordings)) {
            return pubRecordings
        }

        // 3. Standard public Music folder: /storage/emulated/0/Music/Rivo Recordings
        val pubMusic = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            DIRECTORY_NAME
        )
        if (isWritableDirectory(pubMusic)) {
            return pubMusic
        }

        // 4. App-specific external storage (always writable)
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
            ?: context.getExternalFilesDir(null)
        if (extDir != null) {
            val dir = File(extDir, DIRECTORY_NAME)
            if (isWritableDirectory(dir)) {
                return dir
            }
        }

        // 5. Ultimate fallback: internal app sandbox
        val internalDir = File(context.filesDir, DIRECTORY_NAME)
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    fun getRecordingsDirectory(context: Context): File {
        // 0. Custom user-defined directory if directly writable
        getCustomRecordingDirectory(context)?.let { customDir ->
            if (isWritableDirectory(customDir)) {
                return customDir
            }
        }
        return getStandardRecordingsDirectory(context)
    }

    fun getAllRecordingDirectories(context: Context): List<File> {
        val dirs = mutableListOf<File>()
        getCustomRecordingDirectory(context)?.let { customDir ->
            if (customDir.exists() && customDir.isDirectory) {
                dirs.add(customDir)
            }
        }
        runCatching {
            val d = File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME)
            if (d.exists() && d.isDirectory) dirs.add(d)
        }
        runCatching {
            val d =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), DIRECTORY_NAME)
            if (d.exists() && d.isDirectory) dirs.add(d)
        }
        runCatching {
            val d = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), DIRECTORY_NAME)
            if (d.exists() && d.isDirectory) dirs.add(d)
        }
        // Known OEM call recording directories (including Vivo, Xiaomi, Samsung, Oppo, OnePlus)
        val oemPaths = listOf(
            "MIUI/sound_recorder/call_rec",
            "Sounds/CallRecord",
            "Record/Call",
            "Recordings/Call",
            "Record/Phone",
            "vivo/Record/Call",
            "vivo/Record",
            "Recordings/Call Recordings",
            "Recordings/Phone",
            "Recordings",
            "Record",
            "Audio/CallRecordings",
            "CallRecordings"
        )
        for (sub in oemPaths) {
            runCatching {
                val d = File(Environment.getExternalStorageDirectory(), sub)
                if (d.exists() && d.isDirectory) dirs.add(d)
            }
        }
        runCatching {
            val d =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "Voice Recorder")
            if (d.exists() && d.isDirectory) dirs.add(d)
        }
        runCatching {
            context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)?.let {
                val d = File(it, DIRECTORY_NAME)
                if (d.exists() && d.isDirectory) dirs.add(d)
            }
        }
        runCatching {
            context.getExternalFilesDir(null)?.let {
                val d = File(it, DIRECTORY_NAME)
                if (d.exists() && d.isDirectory) dirs.add(d)
            }
        }
        runCatching {
            val d = File(context.filesDir, DIRECTORY_NAME)
            if (d.exists() && d.isDirectory) dirs.add(d)
        }
        return dirs.distinctBy { it.absolutePath }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(context: Context, label: String) {
        if (_isRecording.value || !isStarting.compareAndSet(false, true)) {
            Log.d(TAG, "Recording start ignored: already running or initializing")
            return
        }

        val appContext = context.applicationContext
        activeContext = appContext

        // Instant UI reaction: start duration timer and mark recording active immediately
        _isRecording.value = true
        recordingStartTimeMillis = System.currentTimeMillis()
        startDurationTimer()
        CallRecordingService.start(appContext)

        startJob?.cancel()
        startJob = recorderScope.launch {
            try {
                val success = startInternal(appContext, label)
                if (!success) {
                    Log.w(TAG, "Recording start failed; rolling back recording state")
                    _isRecording.value = false
                    recordingStartTimeMillis = 0L
                    durationJob?.cancel()
                    CallRecordingService.stop(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting recording: ${e.message}", e)
                _isRecording.value = false
                recordingStartTimeMillis = 0L
                durationJob?.cancel()
                CallRecordingService.stop(appContext)
            } finally {
                isStarting.set(false)
            }
        }
    }

    fun prepare(context: Context) {
        val appContext = context.applicationContext
        recorderScope.launch(Dispatchers.IO) {
            try {
                getRecordingsDirectory(appContext)
                val prefs = try {
                    val deviceContext = appContext.createDeviceProtectedStorageContext()
                    deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
                } catch (e: Exception) {
                    null
                }
                val isShizukuEnabled = prefs?.getBoolean(
                    com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_SHIZUKU,
                    true
                ) ?: true
                if (isShizukuEnabled && ShizukuConnectionManager.isAvailable() && ShizukuConnectionManager.hasPermission(
                        appContext
                    )
                ) {
                    val serverPath = ScrcpyConfig.ensureServerJar(appContext)
                    if (serverPath != null) {
                        prewarmShizuku(appContext)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun prewarmShizuku(context: Context) {
        synchronized(this) {
            if (prewarmedShellService?.asBinder()?.isBinderAlive == true) {
                return
            }
            if (prewarmJob?.isActive == true) {
                return
            }
            val mgr = prewarmedShizukuManager ?: ShizukuConnectionManager(context.applicationContext) {
                synchronized(this@CallRecorder) {
                    prewarmedShellService = null
                    prewarmedShizukuManager = null
                }
            }
            prewarmedShizukuManager = mgr
            prewarmJob = recorderScope.async(Dispatchers.IO) {
                try {
                    val service = withTimeoutOrNull(7000L) {
                        mgr.getShellService()
                    }
                    synchronized(this@CallRecorder) {
                        prewarmedShellService = service
                    }
                    if (service != null) {
                        Log.i(TAG, "Shizuku ShellService pre-warmed and ready for 0ms recording")
                    }
                    service
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to pre-warm Shizuku service: ${e.message}")
                    null
                }
            }
        }
    }

    fun releasePrewarm() {
        synchronized(this) {
            prewarmJob?.cancel()
            prewarmJob = null
            prewarmedShellService = null
            runCatching { prewarmedShizukuManager?.unbind() }
            prewarmedShizukuManager = null
        }
    }

    private suspend fun startInternal(context: Context, label: String): Boolean {
        if (!isStarting.get() && _isRecording.value) return true
        activeContext = context

        val safeLabel = label
            .replace(Regex("[^\\p{L}\\p{N}+_-]"), "_")
            .take(40)
            .ifBlank { "call" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val targetFile = File(getRecordingsDirectory(context), "${safeLabel}_$stamp.m4a")

        val prefs = try {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        val isShizukuEnabled =
            prefs?.getBoolean(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_SHIZUKU, true)
                ?: true

        // Priority 1: Use Shizuku + scrcpy-server ONLY if enabled and available
        if (isShizukuEnabled && ShizukuConnectionManager.isAvailable() && ShizukuConnectionManager.hasPermission(context)) {
            val shizukuSuccess = startShizukuRecording(context, targetFile)
            if (shizukuSuccess) return true
        }

        // Priority 2: Standard MediaRecorder (immediate capture)
        return startMediaRecorder(context, targetFile)
    }

    private suspend fun startShizukuRecording(context: Context, targetFile: File): Boolean {
        return try {
            val serverPath = ScrcpyConfig.ensureServerJar(context) ?: run {
                Log.w(TAG, "scrcpy-server JAR not found or invalid hash")
                return false
            }

            var mgr: ShizukuConnectionManager? = null
            var service: IShellService? = null

            synchronized(this) {
                if (prewarmedShellService?.asBinder()?.isBinderAlive == true && prewarmedShizukuManager != null) {
                    mgr = prewarmedShizukuManager
                    service = prewarmedShellService
                    prewarmedShellService = null
                    prewarmedShizukuManager = null
                    prewarmJob = null
                }
            }

            if (service == null) {
                val pendingJob = prewarmJob
                if (pendingJob != null && pendingJob.isActive) {
                    service = withTimeoutOrNull(5000L) { pendingJob.await() }
                    if (service != null && service.asBinder().isBinderAlive) {
                        mgr = prewarmedShizukuManager
                        synchronized(this) {
                            prewarmedShellService = null
                            prewarmedShizukuManager = null
                            prewarmJob = null
                        }
                    }
                }
            }

            if (service == null || mgr == null || !service.asBinder().isBinderAlive) {
                Log.d(TAG, "No valid pre-warmed service; establishing fresh Shizuku connection")
                val freshMgr = ShizukuConnectionManager(context.applicationContext)
                val freshService = withTimeoutOrNull(8000L) {
                    freshMgr.getShellService()
                } ?: run {
                    Log.w(TAG, "Timed out waiting for Shizuku shell service")
                    freshMgr.unbind()
                    return false
                }
                mgr = freshMgr
                service = freshService
            }

            shizukuManager = mgr
            shellService = service

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            recordingScope = scope

            // Attempt voice-call first (captures uplink and downlink)
            var pipePfd = service.startCapture(
                "voice-call",
                "aac",
                ScrcpyConfig.DEFAULT_AUDIO_BIT_RATE,
                serverPath,
                false
            )

            // Fallback to mic-voice-communication if voice-call is not supported on this HAL
            if (pipePfd == null) {
                pipePfd = service.startCapture(
                    "mic-voice-communication",
                    "aac",
                    ScrcpyConfig.DEFAULT_AUDIO_BIT_RATE,
                    serverPath,
                    false
                )
            }

            val pfd = pipePfd ?: run {
                Log.e(TAG, "Shell service returned null audio pipe")
                mgr.unbind()
                return false
            }

            val stagingFile = File(context.cacheDir, "staging_${targetFile.nameWithoutExtension}.m4a").apply {
                if (exists()) delete()
                createNewFile()
            }
            val recordFile = stagingFile

            val muxer = ScrcpyAudioMuxer(recordFile)
            muxer.initialize(ScrcpyAudioCodec.AAC)
            scrcpyMuxer = muxer

            val client = ScrcpyClient(
                inputPfd = pfd,
                expectedCodec = ScrcpyAudioCodec.AAC,
                listener = object : ScrcpyClient.AudioPacketListener {
                    override fun onMetadataReceived(codec: ScrcpyAudioCodec) {
                        muxer.initialize(codec)
                    }

                    override fun onAudioPacket(packet: ScrcpyClient.AudioPacket) {
                        muxer.writePacket(packet, ScrcpyAudioCodec.AAC)
                    }

                    override fun onStreamEnd(error: String?) {
                        Log.d(TAG, "ScrcpyClient stream ended: $error")
                    }
                }
            )
            scrcpyClient = client

            clientJob = scope.launch(Dispatchers.IO) {
                client.start()
            }

            currentFile = recordFile
            isUsingShizuku = true
            _isRecording.value = true
            recordingStartTimeMillis = System.currentTimeMillis()
            CallRecordingService.start(context)
            startDurationTimer()

            Log.i(TAG, "Shizuku call recording started: ${recordFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Shizuku recording: ${e.message}", e)
            cleanupShizuku()
            runCatching { File(context.cacheDir, "staging_${targetFile.nameWithoutExtension}.m4a").delete() }
            if (targetFile.exists()) targetFile.delete()
            false
        }
    }

    data class RecordingFormatProfile(
        val outputFormat: Int,
        val audioEncoder: Int,
        val sampleRate: Int,
        val bitRate: Int,
        val extension: String
    )

    private fun getFormatProfiles(targetBitrate: Int): List<RecordingFormatProfile> = listOf(
        RecordingFormatProfile(
            outputFormat = MediaRecorder.OutputFormat.MPEG_4,
            audioEncoder = MediaRecorder.AudioEncoder.AAC,
            sampleRate = 44100,
            bitRate = targetBitrate,
            extension = "m4a"
        ),
        RecordingFormatProfile(
            outputFormat = MediaRecorder.OutputFormat.MPEG_4,
            audioEncoder = MediaRecorder.AudioEncoder.AAC,
            sampleRate = 16000,
            bitRate = targetBitrate.coerceAtMost(64000),
            extension = "m4a"
        ),
        RecordingFormatProfile(
            outputFormat = MediaRecorder.OutputFormat.THREE_GPP,
            audioEncoder = MediaRecorder.AudioEncoder.AMR_WB,
            sampleRate = 16000,
            bitRate = 23850,
            extension = "3gp"
        ),
        RecordingFormatProfile(
            outputFormat = MediaRecorder.OutputFormat.THREE_GPP,
            audioEncoder = MediaRecorder.AudioEncoder.AMR_NB,
            sampleRate = 8000,
            bitRate = 12200,
            extension = "3gp"
        )
    )

    private fun startMediaRecorder(context: Context, targetFile: File): Boolean {
        if (!hasAudioPermission(context)) {
            Log.w(TAG, "Cannot start MediaRecorder: RECORD_AUDIO permission not granted")
            return false
        }

        val prefs = try {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        val targetBitrate = prefs?.getInt("call_recording_bitrate", 128000) ?: 128000
        val profiles = getFormatProfiles(targetBitrate)

        val prioritizedSources = (listOf(lastWorkingSource) + audioSources).distinct()
        for (source in prioritizedSources) {
            for (profile in profiles) {
                val stagingName = "staging_${targetFile.nameWithoutExtension}.${profile.extension}"
                val stagingFile = File(context.cacheDir, stagingName)
                try {
                    if (stagingFile.exists()) stagingFile.delete()
                    stagingFile.createNewFile()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not prepare staging file: ${e.message}")
                }

                val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                try {
                    instance.setAudioSource(source)
                    instance.setOutputFormat(profile.outputFormat)
                    instance.setAudioEncoder(profile.audioEncoder)
                    instance.setAudioEncodingBitRate(profile.bitRate)
                    instance.setAudioSamplingRate(profile.sampleRate)
                    instance.setOutputFile(stagingFile.absolutePath)
                    instance.prepare()
                    instance.start()

                    recorder = instance
                    currentFile = stagingFile
                    isUsingShizuku = false
                    lastWorkingSource = source
                    _isRecording.value = true
                    recordingStartTimeMillis = System.currentTimeMillis()
                    CallRecordingService.start(context)
                    startDurationTimer()
                    Log.i(TAG, "MediaRecorder started: source $source, profile ${profile.extension}")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Source $source with ${profile.extension} failed: ${e.message}")
                    try {
                        instance.reset()
                    } catch (ignored: Exception) {
                    }
                    try {
                        instance.release()
                    } catch (ignored: Exception) {
                    }
                    if (stagingFile.exists()) stagingFile.delete()
                }
            }
        }
        Log.e(TAG, "All audio sources and format profiles failed for MediaRecorder")
        return false
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        _durationSeconds.value = 0L
        val start = System.currentTimeMillis()
        if (recordingStartTimeMillis == 0L) {
            recordingStartTimeMillis = start
        }
        durationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _isRecording.value) {
                _durationSeconds.value = (System.currentTimeMillis() - start) / 1000
                delay(1000)
            }
        }
    }

    fun stop(): File? {
        startJob?.cancel()
        startJob = null
        isStarting.set(false)

        if (!_isRecording.value && recorder == null && scrcpyMuxer == null) {
            _isRecording.value = false
            return null
        }

        val actualDuration = if (recordingStartTimeMillis > 0L) {
            (System.currentTimeMillis() - recordingStartTimeMillis) / 1000
        } else {
            _durationSeconds.value
        }
        recordingStartTimeMillis = 0L
        durationJob?.cancel()
        durationJob = null

        val saved = currentFile
        val ctx = activeContext

        if (isUsingShizuku) {
            // Stop order: shell capture -> drain pipe -> close client -> close muxer
            try {
                shellService?.stopCapture()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping shell capture: ${e.message}")
            }

            try {
                kotlinx.coroutines.runBlocking {
                    withTimeoutOrNull(2000L) {
                        clientJob?.join()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error waiting for client job drain: ${e.message}")
            }

            try {
                scrcpyClient?.stop()
                scrcpyClient?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing scrcpy client: ${e.message}")
            }

            try {
                scrcpyMuxer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing muxer: ${e.message}")
            }
            cleanupShizuku()
        } else {
            val instance = recorder
            try {
                if (actualDuration < 1) {
                    try {
                        Thread.sleep(600)
                    } catch (ignored: Exception) {
                    }
                }
                instance?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder.stop() exception: ${e.message}")
            } finally {
                try {
                    instance?.reset()
                } catch (ignored: Exception) {
                }
                try {
                    instance?.release()
                } catch (ignored: Exception) {
                }
                recorder = null
            }
        }

        _isRecording.value = false
        currentFile = null
        isUsingShizuku = false
        activeContext = null

        if (ctx != null) {
            CallRecordingService.stop(ctx)
        }

        if (saved == null || !saved.exists() || saved.length() == 0L) {
            Log.w(TAG, "Recording file empty or missing, discarding: ${saved?.absolutePath}")
            saved?.delete()
            return null
        }

        if (ctx == null) return saved

        // Check minimum duration filter setting
        val prefs = try {
            val deviceContext = ctx.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        val minDuration = prefs?.getInt("call_recording_min_duration", 0) ?: 0
        if (minDuration > 0 && actualDuration < minDuration) {
            Log.i(TAG, "Call duration ($actualDuration s) was below filter ($minDuration s), discarding.")
            saved.delete()
            return null
        }

        val cleanName = saved.name.removePrefix("staging_")
        val baseName = cleanName.substringBeforeLast(".")
        val extension = cleanName.substringAfterLast(".", "m4a")
        val mimeType = if (extension.equals("m4a", ignoreCase = true)) "audio/mp4" else "audio/*"

        val customUriStr =
            prefs?.getString(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_FOLDER_URI, null)
        val customDir = getCustomRecordingDirectory(ctx)
        var destinationFile: File? = null
        var wroteToCustom = false

        // Attempt 1: Direct File copy if customDir is a writable filesystem directory
        if (customDir != null && isWritableDirectory(customDir)) {
            try {
                if (!customDir.exists()) customDir.mkdirs()
                val target = File(customDir, cleanName)
                if (saved.canonicalPath != target.canonicalPath) {
                    saved.copyTo(target, overwrite = true)
                }
                if (target.exists() && target.length() > 0L) {
                    destinationFile = target
                    wroteToCustom = true
                    Log.i(TAG, "Recording saved directly to custom folder: ${target.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct copy to custom folder failed: ${e.message}")
            }
        }

        // Attempt 2: SAF DocumentFile write if direct copy failed or directory requires SAF
        if (!wroteToCustom && !customUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customUriStr)
                val docDir = DocumentFile.fromTreeUri(ctx, treeUri)
                if (docDir != null && docDir.canWrite()) {
                    docDir.findFile(cleanName)?.delete()
                    docDir.findFile("$baseName.$extension")?.delete()

                    // DocumentFile.createFile automatically appends the extension based on mimeType.
                    // Passing baseName without extension prevents creating double extensions like .m4a.m4a.
                    val newDocFile = docDir.createFile(mimeType, baseName)
                    if (newDocFile != null) {
                        var bytesWritten = 0L
                        ctx.contentResolver.openOutputStream(newDocFile.uri, "wt")?.use { out ->
                            saved.inputStream().use { input ->
                                bytesWritten = input.copyTo(out)
                            }
                        }
                        if (bytesWritten > 0L) {
                            wroteToCustom = true
                            Log.i(TAG, "Recording ($bytesWritten bytes) written via SAF DocumentFile to: ${newDocFile.uri}")
                            if (customDir != null) {
                                val target = File(customDir, newDocFile.name ?: cleanName)
                                if (target.exists() && target.length() > 0L) {
                                    destinationFile = target
                                }
                            }
                        } else {
                            newDocFile.delete()
                            Log.w(TAG, "DocumentFile write wrote 0 bytes, deleted empty document")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing to custom DocumentFile: ${e.message}", e)
            }
        }

        // Always ensure a valid local File exists in standard directory so MediaScanner, MediaPlayer,
        // and listRecordings() have a reliable zero-latency playback target
        val standardDir = getStandardRecordingsDirectory(ctx)
        val standardTargetFile = File(standardDir, cleanName)

        val effectiveFile: File = if (destinationFile != null && destinationFile.exists() && destinationFile.length() > 0L) {
            if (saved.canonicalPath != destinationFile.canonicalPath) {
                saved.delete()
            }
            destinationFile
        } else {
            try {
                standardDir.mkdirs()
                if (saved.canonicalPath != standardTargetFile.canonicalPath) {
                    saved.copyTo(standardTargetFile, overwrite = true)
                    saved.delete()
                }
                standardTargetFile
            } catch (e: Exception) {
                Log.w(TAG, "Could not move recording to $standardTargetFile: ${e.message}", e)
                saved
            }
        }

        // Notify MediaScanner once on the physical output path
        try {
            MediaScannerConnection.scanFile(
                ctx,
                arrayOf(effectiveFile.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Log.d(TAG, "Scanned $path: uri=$uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaScanner error: ${e.message}")
        }

        recorderScope.launch(Dispatchers.Main) {
            try {
                Toast.makeText(ctx, "Call recorded: ${effectiveFile.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
            }
        }

        lastRecordedFile = effectiveFile
        lastRecordedTimeMillis = System.currentTimeMillis()

        return effectiveFile
    }

    private fun cleanupShizuku() {
        scrcpyClient = null
        scrcpyMuxer = null
        shellService = null
        runCatching { shizukuManager?.unbind() }
        shizukuManager = null
        runCatching { clientJob?.cancel() }
        clientJob = null
        recordingScope = null
    }

    fun listRecordings(context: Context): List<File> {
        val destinationDir = getRecordingsDirectory(context)

        // 1. Recover any unmigrated staging files from cacheDir
        runCatching {
            context.cacheDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("staging_") && it.length() > 0 }
                ?.forEach { staging ->
                    try {
                        val recoveredName = staging.name.removePrefix("staging_")
                        val recoveredFile = File(destinationDir, recoveredName)
                        if (staging.canonicalPath != recoveredFile.canonicalPath) {
                            staging.copyTo(recoveredFile, overwrite = true)
                            staging.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed recovering staging file: ${staging.name}", e)
                    }
                }
        }

        // 2. Scan recording directories
        val dirs = getAllRecordingDirectories(context)
        val files = mutableListOf<File>()
        val supportedExts = listOf(".m4a", ".mp3", ".aac", ".3gp", ".wav")
        for (dir in dirs) {
            dir.listFiles()
                ?.filter { file ->
                    file.isFile && file.length() > 0 && supportedExts.any { ext ->
                        file.name.endsWith(
                            ext,
                            ignoreCase = true
                        )
                    }
                }
                ?.let { files.addAll(it) }
        }

        // 2.5 Scan custom SAF tree directory if set
        val prefs = try {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        val customUriStr = prefs?.getString(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_FOLDER_URI, null)
        if (!customUriStr.isNullOrBlank()) {
            runCatching {
                val treeUri = Uri.parse(customUriStr)
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                val customDir = getCustomRecordingDirectory(context)
                val standardDir = getStandardRecordingsDirectory(context)
                docDir?.listFiles()?.forEach { docFile ->
                    val name = docFile.name ?: return@forEach
                    if (docFile.isFile && docFile.length() > 0L && supportedExts.any { name.endsWith(it, ignoreCase = true) }) {
                        val directFile = if (customDir != null) File(customDir, name) else null
                        if (directFile != null && directFile.exists() && directFile.length() > 0L) {
                            files.add(directFile)
                        } else {
                            // Ensure mirror in standardDir exists so MediaPlayer & cache have direct access
                            val mirrorFile = File(standardDir, name)
                            if (!mirrorFile.exists() || mirrorFile.length() != docFile.length()) {
                                runCatching {
                                    standardDir.mkdirs()
                                    context.contentResolver.openInputStream(docFile.uri)?.use { inStream ->
                                        mirrorFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
                                    }
                                }
                            }
                            if (mirrorFile.exists() && mirrorFile.length() > 0L) {
                                files.add(mirrorFile)
                            }
                        }
                    }
                }
            }
        }

        // 3. Scan cacheDir for direct audio fallback files
        runCatching {
            context.cacheDir.listFiles()
                ?.filter { file ->
                    file.isFile && file.length() > 0 && !file.name.startsWith("staging_") && supportedExts.any { ext ->
                        file.name.endsWith(
                            ext,
                            ignoreCase = true
                        )
                    }
                }
                ?.let { files.addAll(it) }
        }

        // 4. Query MediaStore to discover recordings on modern Android (including Vivo & OEM dialer recordings)
        runCatching {
            val projection = arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_MODIFIED
            )
            val selection = buildString {
                append("(")
                val callKeywords = listOf("call", "record", "rec_", "phone", "vivo")
                callKeywords.forEachIndexed { i, kw ->
                    if (i > 0) append(" OR ")
                    append("${MediaStore.Audio.Media.DATA} LIKE '%$kw%'")
                    append(" OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%$kw%'")
                }
                append(")")
            }
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    if (dataIndex >= 0) {
                        val filePath = cursor.getString(dataIndex)
                        if (!filePath.isNullOrBlank()) {
                            val f = File(filePath)
                            if (f.exists() && f.isFile && f.length() > 0 && supportedExts.any { f.name.endsWith(it, ignoreCase = true) }) {
                                files.add(f)
                            }
                        }
                    }
                }
            }
        }

        // Return distinct files sorted newest to oldest
        return files
            .distinctBy { it.name }
            .sortedByDescending { it.lastModified() }
    }

    fun delete(context: Context, file: File): Boolean {
        AudioMetadataCache.evict(file)
        var deletedSaf = false
        val prefs = try {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        val customUriStr = prefs?.getString(com.grinch.rivo4.controller.util.PreferenceManager.KEY_CALL_RECORDING_FOLDER_URI, null)
        if (!customUriStr.isNullOrBlank()) {
            runCatching {
                val treeUri = Uri.parse(customUriStr)
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                val doc = docDir?.findFile(file.name)
                if (doc != null && doc.exists()) {
                    deletedSaf = doc.delete()
                }
            }
        }
        val deletedLocal = file.delete()
        return deletedLocal || deletedSaf
    }

    fun delete(file: File): Boolean {
        AudioMetadataCache.evict(file)
        val ctx = activeContext
        if (ctx != null) {
            return delete(ctx, file)
        }
        return file.delete()
    }

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, file: File, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(
                Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch share chooser: ${e.message}")
        }
    }
}
