package com.grinch.rivo4;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;

public interface IShellService extends IInterface {
    String DESCRIPTOR = "com.grinch.rivo4.IShellService";

    int TRANSACTION_startCapture = IBinder.FIRST_CALL_TRANSACTION + 0;
    int TRANSACTION_stopCapture = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_destroy = 16777114;

    ParcelFileDescriptor startCapture(String audioSource, String audioCodec, int audioBitRate, String serverPath, boolean debug) throws RemoteException;
    void stopCapture() throws RemoteException;
    void destroy() throws RemoteException;

    abstract class Stub extends Binder implements IShellService {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IShellService asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IShellService) {
                return (IShellService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                case TRANSACTION_startCapture: {
                    data.enforceInterface(DESCRIPTOR);
                    String audioSource = data.readString();
                    String audioCodec = data.readString();
                    int audioBitRate = data.readInt();
                    String serverPath = data.readString();
                    boolean debug = data.readInt() != 0;
                    ParcelFileDescriptor result = this.startCapture(audioSource, audioCodec, audioBitRate, serverPath, debug);
                    reply.writeNoException();
                    if (result != null) {
                        reply.writeInt(1);
                        result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_stopCapture: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stopCapture();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_destroy: {
                    data.enforceInterface(DESCRIPTOR);
                    this.destroy();
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IShellService {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public ParcelFileDescriptor startCapture(String audioSource, String audioCodec, int audioBitRate, String serverPath, boolean debug) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                ParcelFileDescriptor _result = null;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(audioSource);
                    _data.writeString(audioCodec);
                    _data.writeInt(audioBitRate);
                    _data.writeString(serverPath);
                    _data.writeInt(debug ? 1 : 0);
                    mRemote.transact(TRANSACTION_startCapture, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void stopCapture() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_stopCapture, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void destroy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_destroy, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
