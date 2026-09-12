package com.grinch.rivo4.controller.shizuku

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ShellAudioPipeline {

    private companion object {
        const val RELAY_BUFFER_SIZE = 32 * 1024
        const val PROCESS_STOP_GRACE_PERIOD_SEC = 2L
    }

    private val isRecordingActive = AtomicBoolean(false)

    private var scrcpyProcess: Process? = null
    private var serverSocket: LocalServerSocket? = null
    private var clientConnection: LocalSocket? = null
    private var audioWriteEnd: ParcelFileDescriptor? = null

    private var shellScope: CoroutineScope? = null
    private var audioPipeRelayJob: Job? = null

    fun startCapture(
        audioSource: String,
        audioCodec: String,
        audioBitRate: Int,
        serverPath: String,
        debug: Boolean
    ): ParcelFileDescriptor? {
        if (isRecordingActive.get()) {
            return null
        }

        try {
            val serverJarFile = File(serverPath)
            if (!serverJarFile.exists()) {
                return null
            }

            val pipe = ParcelFileDescriptor.createPipe()
            val pipeReadEnd = pipe[0]
            val pipeWriteEnd = pipe[1]
            audioWriteEnd = pipeWriteEnd

            val socketName = ScrcpyConfig.getRandomSocketName()
            val serverFullSocketName = ScrcpyConfig.SERVER_SOCKET_NAME_PREFIX + socketName
            serverSocket = LocalServerSocket(serverFullSocketName)

            shellScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            spawnAudioRelayCoroutine(debug)

            val serverArgs = ScrcpyConfig.buildServerArgs(
                socketName = socketName,
                audioSource = audioSource,
                audioCodec = audioCodec,
                audioBitRate = audioBitRate
            )

            val launchCommand = mutableListOf("app_process", "/", ScrcpyConfig.SERVER_MAIN_CLASS)
            launchCommand.addAll(serverArgs)

            val scrcpyBuilder = ProcessBuilder(launchCommand).apply {
                environment()["CLASSPATH"] = serverPath
                redirectErrorStream(true)
            }

            scrcpyProcess = scrcpyBuilder.start()
            isRecordingActive.set(true)

            spawnLogConsumerCoroutine(scrcpyProcess!!)
            spawnProcessMonitorCoroutine(scrcpyProcess!!)

            return pipeReadEnd

        } catch (e: Exception) {
            stopCapture()
            return null
        }
    }

    fun stopCapture() {
        if (!isRecordingActive.compareAndSet(true, false)) {
            return
        }

        runCatching { scrcpyProcess?.destroy() }

        try {
            scrcpyProcess?.waitFor(PROCESS_STOP_GRACE_PERIOD_SEC, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
        }

        runCatching {
            runBlocking {
                withTimeoutOrNull(2000L) {
                    audioPipeRelayJob?.join()
                }
            }
        }

        runCatching { shellScope?.cancel() }
        runCatching { clientConnection?.close() }
        runCatching { serverSocket?.close() }
        runCatching { audioWriteEnd?.close() }

        scrcpyProcess = null
        clientConnection = null
        serverSocket = null
        audioWriteEnd = null
        shellScope = null
        audioPipeRelayJob = null
    }

    private fun spawnAudioRelayCoroutine(debug: Boolean) {
        audioPipeRelayJob = shellScope?.launch(Dispatchers.IO) {
            try {
                val connection = serverSocket?.accept() ?: return@launch
                clientConnection = connection

                val sourceStream = connection.inputStream
                val destinationStream = ParcelFileDescriptor.AutoCloseOutputStream(audioWriteEnd)

                val buffer = ByteArray(RELAY_BUFFER_SIZE)

                while (isActive) {
                    val bytesRead = sourceStream.read(buffer)
                    if (bytesRead == -1) {
                        break
                    }
                    destinationStream.write(buffer, 0, bytesRead)
                }
            } catch (e: IOException) {
            } finally {
                stopCapture()
            }
        }
    }

    private fun spawnLogConsumerCoroutine(process: Process) {
        shellScope?.launch(Dispatchers.IO) {
            try {
                process.inputStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (isActive && line != null) {
                        android.util.Log.i("ShellAudioPipeline", "[scrcpy] $line")
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("ShellAudioPipeline", "Log consumer ended: ${e.message}")
            }
        }
    }

    private fun spawnProcessMonitorCoroutine(process: Process) {
        shellScope?.launch(Dispatchers.IO) {
            try {
                val exitCode = process.waitFor()
                android.util.Log.i("ShellAudioPipeline", "scrcpy-server exited with code $exitCode")
                if (exitCode != 0 && isRecordingActive.get()) {
                    stopCapture()
                }
            } catch (e: Exception) {
            }
        }
    }
}
