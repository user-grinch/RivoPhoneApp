package com.grinch.rivo4.controller.shizuku

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object ServerExtractor {
    const val EXPECTED_SHA256 = "84924bd564a1eb6089c872c7521f968058977f91f5ff02514a8c74aff3210f3a"
    const val ASSET_NAME = "scrcpy-server"

    @WorkerThread
    fun ensureServerFile(context: Context, serverPath: String): Boolean {
        val file = File(serverPath)
        if (file.exists() && verifyServerHash(file)) {
            return true
        }
        return extractFromAssets(context, file)
    }

    private fun extractFromAssets(context: Context, destFile: File): Boolean {
        return try {
            val inputStream = try {
                context.assets.open(ASSET_NAME)
            } catch (e: Exception) {
                context.assets.open("scrcpy-server.jar")
            }
            inputStream.use { stream ->
                writeFile(destFile, stream)
            }
            verifyServerHash(destFile)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun writeFile(destFile: File, input: InputStream) {
        destFile.parentFile?.mkdirs()
        FileOutputStream(destFile).use { output ->
            val buffer = ByteArray(8 * 1024)
            var bytesRead = input.read(buffer)
            while (bytesRead > 0) {
                output.write(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        destFile.setReadable(true, false)
    }

    fun verifyServerHash(file: File): Boolean {
        if (!file.exists()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(EXPECTED_SHA256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
