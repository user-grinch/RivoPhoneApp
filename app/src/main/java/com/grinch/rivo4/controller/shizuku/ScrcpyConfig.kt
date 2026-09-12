package com.grinch.rivo4.controller.shizuku

import android.content.Context
import java.io.File
import java.security.SecureRandom

object ScrcpyConfig {
    const val SERVER_MAIN_CLASS = "com.genymobile.scrcpy.Server"
    const val SERVER_VERSION = "4.0"
    const val SERVER_SOCKET_NAME_PREFIX = "scrcpy_"
    const val DEFAULT_AUDIO_BIT_RATE = 128000
    const val AUDIO_SAMPLE_RATE = 48000
    const val AUDIO_CHANNELS = 2

    fun getServerPath(context: Context): String {
        val folder = context.getExternalFilesDir(null)
            ?: context.externalCacheDir
            ?: File("/storage/emulated/0/Android/data/${context.packageName}/files")
        return File(folder, "scrcpy-server.jar").absolutePath
    }

    fun ensureServerJar(context: Context): String? {
        val path = getServerPath(context)
        val ready = ServerExtractor.ensureServerFile(context, path)
        return if (ready) path else null
    }

    fun getRandomSocketName(): String {
        // Must be a positive 32-bit int hex string (at most 31 bits) so scrcpy-server's
        // Integer.parseInt(scid, 16) never throws NumberFormatException.
        return SecureRandom().nextInt(Int.MAX_VALUE).toString(16).padStart(8, '0')
    }

    fun buildServerArgs(
        socketName: String,
        audioSource: String = "voice-call",
        audioCodec: String = "aac",
        audioBitRate: Int = DEFAULT_AUDIO_BIT_RATE
    ): List<String> {
        return listOf(
            SERVER_VERSION,
            "log_level=info",
            "video=false",
            "audio=true",
            "control=false",
            "tunnel_forward=false",
            "send_dummy_byte=false",
            "scid=$socketName",
            "audio_source=$audioSource",
            "audio_codec=$audioCodec",
            "audio_bit_rate=$audioBitRate",
            "send_device_meta=false",
            "send_frame_meta=true",
            "send_stream_meta=true"
        )
    }
}
