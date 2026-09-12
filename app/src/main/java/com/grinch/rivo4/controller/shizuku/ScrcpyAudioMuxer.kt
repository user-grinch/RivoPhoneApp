package com.grinch.rivo4.controller.shizuku

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.nio.ByteBuffer

class ScrcpyAudioMuxer : Closeable {

    companion object {
        private const val TAG = "ScrcpyAudioMuxer"
        private const val GAP_THRESHOLD_NANOS = 400_000_000L // 400ms
        private const val GAP_SLACK_NANOS = 25_000_000L // 25ms
    }

    private var muxer: MediaMuxer? = null
    private val filePath: String?
    private val fileDescriptor: FileDescriptor?

    constructor(file: File) {
        this.filePath = file.absolutePath
        this.fileDescriptor = null
    }

    constructor(fd: FileDescriptor) {
        this.filePath = null
        this.fileDescriptor = fd
    }

    private var audioTrackIndex = -1
    private var isMuxerStarted = false
    private var firstPacketTimeNanos: Long = -1L
    private var lastWrittenPtsUs: Long = -1L
    private var lastPacketWallClockNanos: Long = -1L
    private var totalIgnoredGapNanos: Long = 0L

    fun initialize(codec: ScrcpyAudioCodec) {
        if (muxer != null) return
        muxer = if (filePath != null) {
            MediaMuxer(filePath, codec.outputFormat)
        } else if (fileDescriptor != null) {
            MediaMuxer(fileDescriptor, codec.outputFormat)
        } else {
            null
        }
    }

    fun writePacket(packet: ScrcpyClient.AudioPacket, codec: ScrcpyAudioCodec) {
        if (packet.isConfigPacket) {
            if (audioTrackIndex < 0) {
                addAudioTrack(packet.data, codec)
            }
            return
        }

        if (!isMuxerStarted || audioTrackIndex < 0) {
            return
        }

        val nowNanos = System.nanoTime()

        if (firstPacketTimeNanos == -1L) {
            firstPacketTimeNanos = nowNanos
            lastPacketWallClockNanos = nowNanos
        } else {
            val gapNanos = nowNanos - lastPacketWallClockNanos
            if (gapNanos > GAP_THRESHOLD_NANOS) {
                val ignoredNanos = gapNanos - GAP_SLACK_NANOS
                totalIgnoredGapNanos += ignoredNanos
            }
            lastPacketWallClockNanos = nowNanos
        }

        val wallClockPtsUs = (nowNanos - firstPacketTimeNanos - totalIgnoredGapNanos) / 1000L
        val normalizedPtsUs = if (wallClockPtsUs > lastWrittenPtsUs) wallClockPtsUs else lastWrittenPtsUs + 1L

        val bufferInfo = MediaCodec.BufferInfo().apply {
            offset = 0
            size = packet.data.size
            presentationTimeUs = normalizedPtsUs
        }
        lastWrittenPtsUs = normalizedPtsUs

        try {
            muxer?.writeSampleData(audioTrackIndex, ByteBuffer.wrap(packet.data), bufferInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sample data to muxer: ${e.message}")
        }
    }

    private fun addAudioTrack(configData: ByteArray, codec: ScrcpyAudioCodec) {
        val csdBytes = configData.takeIf { it.isNotEmpty() } ?: return

        val mediaFormat = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, codec.mimeType)
            setInteger(MediaFormat.KEY_SAMPLE_RATE, ScrcpyConfig.AUDIO_SAMPLE_RATE)
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, ScrcpyConfig.AUDIO_CHANNELS)
            setByteBuffer("csd-0", ByteBuffer.wrap(csdBytes))
        }

        audioTrackIndex = muxer?.addTrack(mediaFormat) ?: -1
        if (audioTrackIndex >= 0) {
            try {
                muxer?.start()
                isMuxerStarted = true
                Log.d(TAG, "MediaMuxer started with track: $audioTrackIndex (${codec.mimeType})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaMuxer: ${e.message}")
            }
        }
    }

    override fun close() {
        if (isMuxerStarted) {
            runCatching { muxer?.stop() }.onFailure {
                Log.w(TAG, "MediaMuxer stop warning: ${it.message}")
            }
        }
        runCatching { muxer?.release() }
        muxer = null
        isMuxerStarted = false
        audioTrackIndex = -1
        firstPacketTimeNanos = -1L
        lastWrittenPtsUs = -1L
        lastPacketWallClockNanos = -1L
        totalIgnoredGapNanos = 0L
    }
}
