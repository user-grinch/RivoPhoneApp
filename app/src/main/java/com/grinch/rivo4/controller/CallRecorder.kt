package com.grinch.rivo4.controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallRecorder {

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    private val audioSources = listOf(
        MediaRecorder.AudioSource.VOICE_CALL,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.MIC
    )

    fun getRecordingsDirectory(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val dir = File(base, "CallRecordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(context: Context, label: String): Boolean {
        if (_isRecording.value) return true
        if (!hasPermission(context)) return false

        val safeLabel = label
            .replace(Regex("[^\\p{L}\\p{N}+_-]"), "_")
            .take(40)
            .ifBlank { "call" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(getRecordingsDirectory(context), "${safeLabel}_$stamp.m4a")

        for (source in audioSources) {
            val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            try {
                instance.setAudioSource(source)
                instance.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                instance.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                instance.setAudioEncodingBitRate(128000)
                instance.setAudioSamplingRate(44100)
                instance.setOutputFile(file.absolutePath)
                instance.prepare()
                instance.start()

                recorder = instance
                currentFile = file
                _isRecording.value = true
                return true
            } catch (e: Exception) {
                try { instance.reset() } catch (ignored: Exception) {}
                try { instance.release() } catch (ignored: Exception) {}
                if (file.exists()) file.delete()
            }
        }
        return false
    }

    fun stop(): File? {
        val instance = recorder ?: run {
            _isRecording.value = false
            return null
        }
        var saved = currentFile
        try {
            instance.stop()
        } catch (e: Exception) {
            saved?.delete()
            saved = null
        } finally {
            try { instance.reset() } catch (ignored: Exception) {}
            try { instance.release() } catch (ignored: Exception) {}
            recorder = null
            currentFile = null
            _isRecording.value = false
        }
        if (saved != null && (!saved.exists() || saved.length() == 0L)) {
            saved.delete()
            return null
        }
        return saved
    }

    fun listRecordings(context: Context): List<File> {
        val dir = getRecordingsDirectory(context)
        return dir.listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun delete(file: File): Boolean = file.delete()

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun play(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
        }
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
        }
    }
}
