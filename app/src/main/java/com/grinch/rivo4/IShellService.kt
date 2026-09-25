package com.grinch.rivo4

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.RemoteException
import androidx.annotation.Keep

@Keep
interface IShellService : IInterface {

    @Throws(RemoteException::class)
    fun startCapture(
        audioSource: String?,
        audioCodec: String?,
        audioBitRate: Int,
        serverPath: String?,
        debug: Boolean
    ): ParcelFileDescriptor?

    @Throws(RemoteException::class)
    fun stopCapture()

    @Throws(RemoteException::class)
    fun destroy()

    abstract class Stub : Binder(), IShellService {

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        @Throws(RemoteException::class)
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                IBinder.INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    return true
                }

                TRANSACTION_startCapture -> {
                    data.enforceInterface(DESCRIPTOR)
                    val audioSource = data.readString()
                    val audioCodec = data.readString()
                    val audioBitRate = data.readInt()
                    val serverPath = data.readString()
                    val debug = data.readInt() != 0
                    val result = startCapture(audioSource, audioCodec, audioBitRate, serverPath, debug)
                    reply?.writeNoException()
                    if (result != null) {
                        reply?.writeInt(1)
                        if (reply != null) {
                            result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                        }
                    } else {
                        reply?.writeInt(0)
                    }
                    return true
                }

                TRANSACTION_stopCapture -> {
                    data.enforceInterface(DESCRIPTOR)
                    stopCapture()
                    reply?.writeNoException()
                    return true
                }

                TRANSACTION_destroy -> {
                    data.enforceInterface(DESCRIPTOR)
                    destroy()
                    reply?.writeNoException()
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }

        private class Proxy(private val mRemote: IBinder) : IShellService {
            override fun asBinder(): IBinder = mRemote

            @Throws(RemoteException::class)
            override fun startCapture(
                audioSource: String?,
                audioCodec: String?,
                audioBitRate: Int,
                serverPath: String?,
                debug: Boolean
            ): ParcelFileDescriptor? {
                val parcelData = Parcel.obtain()
                val parcelReply = Parcel.obtain()
                var result: ParcelFileDescriptor? = null
                try {
                    parcelData.writeInterfaceToken(DESCRIPTOR)
                    parcelData.writeString(audioSource)
                    parcelData.writeString(audioCodec)
                    parcelData.writeInt(audioBitRate)
                    parcelData.writeString(serverPath)
                    parcelData.writeInt(if (debug) 1 else 0)
                    mRemote.transact(TRANSACTION_startCapture, parcelData, parcelReply, 0)
                    parcelReply.readException()
                    if (parcelReply.readInt() != 0) {
                        result = ParcelFileDescriptor.CREATOR.createFromParcel(parcelReply)
                    }
                } finally {
                    parcelReply.recycle()
                    parcelData.recycle()
                }
                return result
            }

            @Throws(RemoteException::class)
            override fun stopCapture() {
                val parcelData = Parcel.obtain()
                val parcelReply = Parcel.obtain()
                try {
                    parcelData.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_stopCapture, parcelData, parcelReply, 0)
                    parcelReply.readException()
                } finally {
                    parcelReply.recycle()
                    parcelData.recycle()
                }
            }

            @Throws(RemoteException::class)
            override fun destroy() {
                val parcelData = Parcel.obtain()
                val parcelReply = Parcel.obtain()
                try {
                    parcelData.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_destroy, parcelData, parcelReply, 0)
                    parcelReply.readException()
                } finally {
                    parcelReply.recycle()
                    parcelData.recycle()
                }
            }
        }

        companion object {
            const val DESCRIPTOR = "com.grinch.rivo4.IShellService"

            const val TRANSACTION_startCapture = IBinder.FIRST_CALL_TRANSACTION + 0
            const val TRANSACTION_stopCapture = IBinder.FIRST_CALL_TRANSACTION + 1
            const val TRANSACTION_destroy = 16777114

            @JvmStatic
            fun asInterface(obj: IBinder): IShellService {
                val iin = obj.queryLocalInterface(DESCRIPTOR)
                if (iin is IShellService) {
                    return iin
                }
                return Proxy(obj)
            }
        }
    }

    companion object {
        const val DESCRIPTOR = "com.grinch.rivo4.IShellService"
        const val TRANSACTION_startCapture = IBinder.FIRST_CALL_TRANSACTION + 0
        const val TRANSACTION_stopCapture = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_destroy = 16777114
    }
}
