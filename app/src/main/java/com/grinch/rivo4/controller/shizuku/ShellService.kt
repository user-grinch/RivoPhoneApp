package com.grinch.rivo4.controller.shizuku

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import com.grinch.rivo4.IShellService
import kotlin.system.exitProcess

@Keep
class ShellService : IShellService.Stub {

    private val pipeline by lazy { ShellAudioPipeline() }

    @Keep
    constructor() : this(null)

    @Keep
    constructor(context: Context?)

    override fun startCapture(
        audioSource: String?,
        audioCodec: String?,
        audioBitRate: Int,
        serverPath: String?,
        debug: Boolean
    ): ParcelFileDescriptor? {
        val source = audioSource ?: "voice-call"
        val codec = audioCodec ?: "aac"
        val path = serverPath ?: return null

        return pipeline.startCapture(
            audioSource = source,
            audioCodec = codec,
            audioBitRate = audioBitRate,
            serverPath = path,
            debug = debug
        )
    }

    override fun stopCapture() {
        pipeline.stopCapture()
    }

    override fun destroy() {
        stopCapture()
        exitProcess(0)
    }
}
