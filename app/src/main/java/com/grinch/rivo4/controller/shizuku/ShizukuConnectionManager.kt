package com.grinch.rivo4.controller.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.grinch.rivo4.IShellService
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShizukuConnectionManager(
    private val context: Context,
    private val onBinderDied: () -> Unit = {}
) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 204846

        fun isAvailable(): Boolean {
            return try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
        }

        fun hasPermission(context: Context? = null): Boolean {
            return try {
                if (isAvailable()) {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } else if (context != null) {
                    context.checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        fun requestPermission() {
            if (!hasPermission()) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        }
    }

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (e: Exception) {
            1
        }

        Shizuku.UserServiceArgs(ComponentName(context.packageName, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("ElevatedShellService")
            .debuggable(true)
            .version(version)
    }

    private var serviceConnection: ServiceConnection? = null

    suspend fun getShellService(): IShellService = suspendCancellableCoroutine { continuation ->
        if (!isAvailable()) {
            continuation.resumeWithException(IllegalStateException("Shizuku is not running"))
            return@suspendCancellableCoroutine
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                if (binder != null) {
                    val proxy = IShellService.Stub.asInterface(binder)
                    if (continuation.isActive) {
                        continuation.resume(proxy)
                    }
                } else {
                    val e = IllegalStateException("Shizuku returned null binder")
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                unbind()
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException("Shizuku service disconnected"))
                } else {
                    onBinderDied()
                }
            }
        }

        this.serviceConnection = connection

        fun bindServiceInternal() {
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }

        val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == PERMISSION_REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        bindServiceInternal()
                    } else if (continuation.isActive) {
                        continuation.resumeWithException(SecurityException("Shizuku permission denied"))
                    }
                }
            }
        }

        if (hasPermission()) {
            bindServiceInternal()
        } else {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }

        continuation.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }

    fun unbind() {
        val serviceConn = serviceConnection
        if (serviceConn != null) {
            try {
                if (isAvailable()) {
                    Shizuku.unbindUserService(userServiceArgs, serviceConn, false)
                }
            } catch (e: Exception) {
            }
        }
        serviceConnection = null
    }
}
