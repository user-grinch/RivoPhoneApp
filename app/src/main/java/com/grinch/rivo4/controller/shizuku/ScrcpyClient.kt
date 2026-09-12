package com.grinch.rivo4.controller.shizuku

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.EOFException
import java.io.FileInputStream

class ScrcpyClient(
    private val inputPfd: ParcelFileDescriptor,
    private val expectedCodec: ScrcpyAudioCodec,
    private val listener: AudioPacketListener
) : Closeable {

    companion object {
        private const val TAG = "ScrcpyClient"
        private const val MEDIA_PACKET_FLAG     = 1L shl 63
        private const val PACKET_FLAG_CONFIG    = 1L shl 62
        private const val PACKET_FLAG_KEY_FRAME = 1L shl 61
        private const val MAX_PACKET_SIZE = 1 * 1024 * 1024 // 1 MiB
    }

    interface AudioPacketListener {
        fun onMetadataReceived(codec: ScrcpyAudioCodec)
        fun onAudioPacket(packet: AudioPacket)
        fun onStreamEnd(error: String?)
    }

    data class AudioPacket(
        val pts: Long,
        val isConfigPacket: Boolean,
        val data: ByteArray
    )

    private data class AudioEnvelope(
        val rawPtsAndFlags: Long,
        val pts: Long,
        val isMedia: Boolean,
        val isConfig: Boolean,
        val isKeyFrame: Boolean,
        val payloadSize: Int
    )

    @Volatile
    private var running = false

    fun start() {
        running = true
        val inputStream = DataInputStream(
            BufferedInputStream(FileInputStream(inputPfd.fileDescriptor))
        )
        try {
            val receivedFourCC = inputStream.readInt()
            val resolvedCodec = ScrcpyAudioCodec.fromFourCC(receivedFourCC)
            Log.d(TAG, "Codec FourCC: received=0x${receivedFourCC.toString(16)} resolved=${resolvedCodec.cliKey}")

            listener.onMetadataReceived(resolvedCodec)

            var hasReceivedConfig = false
            while (running) {
                val header = readPacketHeader(inputStream)

                if (!hasReceivedConfig) {
                    if (!header.isConfig) {
                        Log.w(TAG, "First packet was not marked as config packet")
                    }
                    hasReceivedConfig = true
                }

                if (header.payloadSize <= 0 || header.payloadSize > MAX_PACKET_SIZE) {
                    throw java.io.IOException("Invalid packet size: ${header.payloadSize}")
                }

                val payloadBytes = ByteArray(header.payloadSize)
                inputStream.readFully(payloadBytes)

                listener.onAudioPacket(
                    AudioPacket(
                        pts = header.pts,
                        isConfigPacket = header.isConfig,
                        data = payloadBytes
                    )
                )
            }
        } catch (e: EOFException) {
            Log.d(TAG, "Stream reached EOF cleanly")
            listener.onStreamEnd(null)
        } catch (e: Exception) {
            Log.e(TAG, "Stream ended with exception: ${e.message}", e)
            listener.onStreamEnd(e.message)
        } finally {
            runCatching { inputStream.close() }
        }
    }

    fun stop() {
        running = false
    }

    override fun close() {
        stop()
        runCatching { inputPfd.close() }
    }

    private fun readPacketHeader(stream: DataInputStream): AudioEnvelope {
        val ptsAndFlags = stream.readLong()
        val payloadSize = stream.readInt()
        return AudioEnvelope(
            rawPtsAndFlags = ptsAndFlags,
            pts = ptsAndFlags and (MEDIA_PACKET_FLAG or PACKET_FLAG_CONFIG or PACKET_FLAG_KEY_FRAME).inv(),
            isMedia = (ptsAndFlags and MEDIA_PACKET_FLAG) == 0L,
            isConfig = (ptsAndFlags and PACKET_FLAG_CONFIG) != 0L,
            isKeyFrame = (ptsAndFlags and PACKET_FLAG_KEY_FRAME) != 0L,
            payloadSize = payloadSize
        )
    }
}
